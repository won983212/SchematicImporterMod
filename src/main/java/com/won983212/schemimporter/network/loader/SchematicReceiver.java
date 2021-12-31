package com.won983212.schemimporter.network.loader;

import com.won983212.schemimporter.Logger;
import com.won983212.schemimporter.SchematicImporterMod;
import com.won983212.schemimporter.Settings;
import com.won983212.schemimporter.network.IMessage;
import com.won983212.schemimporter.network.packets.CSSchematicReceivedProgress;
import com.won983212.schemimporter.schematic.IProgressEntryProducer;
import com.won983212.schemimporter.schematic.SchematicFile;
import com.won983212.schemimporter.schematic.parser.SchematicFileParser;
import net.minecraft.entity.player.ServerPlayerEntity;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class SchematicReceiver implements IProgressEntryProducer {
    private final Consumer<IMessage> packetSender;
    private final Map<String, SchematicNetworkProgress<OutputStream>> activeUploads;
    private final boolean hasFileCountLimit;

    protected SchematicReceiver(Consumer<IMessage> packetSender, boolean hasFileCountLimit) {
        this.activeUploads = new HashMap<>();
        this.packetSender = packetSender;
        this.hasFileCountLimit = hasFileCountLimit;
    }

    public void tick() {
        Set<String> deadEntries = new HashSet<>();
        for (String upload : activeUploads.keySet()) {
            SchematicNetworkProgress<OutputStream> entry = activeUploads.get(upload);

            if (!entry.increaseIdleTime()) {
                Logger.warn("Schematic Upload timed out: " + upload);
                deadEntries.add(upload);
            }
        }
        deadEntries.forEach(this::cancelUpload);
    }

    public void shutdown() {
        new HashSet<>(activeUploads.keySet()).forEach(this::cancelUpload);
    }

    /**
     * @param targetPath as relative path. ex) uploaded/Dev
     * @return return false if already exists.
     */
    public boolean handleNewUpload(String targetPath, SchematicFile schematic, long size) {
        String name = schematic.getName();
        String schematicPath = targetPath + "/" + name;

        SchematicFileNetwork.createFolderIfMissing(targetPath);

        if (SchematicFileParser.isUnsupportedExtension(name)) {
            throw new SchematicNetworkException(SchematicImporterMod.translateAsString("message.unsupportedextension", schematicPath));
        }

        Path uploadPath = Paths.get(targetPath, name).normalize();
        if (schematic.equals(uploadPath)) {
            throw new SchematicNetworkException(SchematicImporterMod.translateAsString("message.alreadyexists", schematicPath));
        }

        if (SchematicFileNetwork.isSchematicSizeTooBig(size)) {
            throw new SchematicNetworkException(SchematicFileNetwork.getTooBigSizeMessage(name, size / 1000));
        }

        if (activeUploads.containsKey(schematicPath)) {
            return false;
        }

        try {
            Files.deleteIfExists(uploadPath);

            if (hasFileCountLimit) {
                long count;
                try (Stream<Path> list = Files.list(Paths.get(targetPath))) {
                    count = list.count();
                }

                // Too many Schematics
                if (count >= Settings.MAX_SCHEMATICS) {
                    Stream<Path> list2 = Files.list(Paths.get(targetPath));
                    Optional<Path> lastFilePath = list2.filter(f -> !Files.isDirectory(f))
                            .min(Comparator.comparingLong(f -> f.toFile()
                                    .lastModified()));
                    list2.close();
                    if (lastFilePath.isPresent()) {
                        Files.deleteIfExists(lastFilePath.get());
                    }
                }
            }

            // Open Stream
            OutputStream writer = Files.newOutputStream(uploadPath);
            activeUploads.put(schematicPath, new SchematicNetworkProgress<>(schematicPath, writer, size));
            return true;
        } catch (IOException e) {
            throw new SchematicNetworkException(SchematicImporterMod.translateAsString("message.exceptionmessage", schematicPath), e);
        }
    }

    public void handleWriteRequest(String schematicPath, String schematicName, byte[] data) {
        if (activeUploads.containsKey(schematicPath)) {
            SchematicNetworkProgress<OutputStream> entry = activeUploads.get(schematicPath);

            // Size Validations
            if (data.length > Settings.SCHEMATIC_PACKET_SIZE) {
                cancelUpload(schematicPath);
                throw new SchematicNetworkException(SchematicImporterMod.translateAsString("message.oversizedpacket", schematicPath));
            }

            if (!entry.addUploadedBytes(data.length)) {
                cancelUpload(schematicPath);
                throw new SchematicNetworkException(SchematicImporterMod.translateAsString("message.receiveexceed", schematicPath));
            }

            try {
                entry.getValue().write(data);
                entry.resetIdleTime();
                packetSender.accept(CSSchematicReceivedProgress.success(schematicName, entry.getUploadedBytes()));
                return;
            } catch (IOException e) {
                cancelUpload(schematicPath);
                throw new SchematicNetworkException(SchematicImporterMod.translateAsString("message.exceptionmessage", schematicPath), e);
            }
        }
        throw new SchematicNetworkException(SchematicImporterMod.translateAsString("message.notactiveschem", schematicPath));
    }

    public boolean handleFinishedUpload(String schematicPath) {
        if (activeUploads.containsKey(schematicPath)) {
            try {
                SchematicNetworkProgress<OutputStream> entry = activeUploads.get(schematicPath);
                entry.getValue().close();
                activeUploads.remove(schematicPath);

                if (entry.getUploadedBytes() != entry.getTotalBytes()) {
                    Files.deleteIfExists(Paths.get(schematicPath));
                    throw new SchematicNetworkException(SchematicImporterMod.translateAsString("message.leakfile",
                            entry.getUploadedBytes(), entry.getTotalBytes()));
                }

                Logger.info("New Schematic Uploaded: " + schematicPath);
                return true;
            } catch (IOException e) {
                throw new SchematicNetworkException(SchematicImporterMod.translateAsString("message.exceptionmessage", schematicPath), e);
            }
        }
        return false;
    }

    protected void cancelUpload(String schematicPath) {
        if (!activeUploads.containsKey(schematicPath)) {
            return;
        }

        SchematicNetworkProgress<OutputStream> entry = activeUploads.remove(schematicPath);
        try {
            entry.getValue().close();
            Files.deleteIfExists(Paths.get(schematicPath));
            Logger.warn("Cancelled Schematic Upload: " + schematicPath);
        } catch (IOException e) {
            throw new SchematicNetworkException(SchematicImporterMod.translateAsString("message.exceptionmessage", schematicPath), e);
        }
    }

    @Override
    public Iterable<SchematicNetworkProgress<OutputStream>> getProgressEntries() {
        return activeUploads.values();
    }

    @Override
    public int size() {
        return activeUploads.size();
    }
}
