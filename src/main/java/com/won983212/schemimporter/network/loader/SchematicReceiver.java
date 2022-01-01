package com.won983212.schemimporter.network.loader;

import com.won983212.schemimporter.Logger;
import com.won983212.schemimporter.SchematicImporterMod;
import com.won983212.schemimporter.Settings;
import com.won983212.schemimporter.network.IMessage;
import com.won983212.schemimporter.network.packets.CSSchematicReceivedProgress;
import com.won983212.schemimporter.schematic.IProgressEntryProducer;
import com.won983212.schemimporter.schematic.SchematicFile;
import com.won983212.schemimporter.schematic.parser.SchematicFileParser;

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

    public boolean isUploading(String schematic) {
        return activeUploads.containsKey(schematic);
    }

    /**
     * @param basePath as relative path. ex) schematics/uploaded/Dev
     * @return return false if already exists.
     */
    public boolean newDownloadRequest(String basePath, SchematicFile schematic, long size) {
        String name = schematic.getName();
        String key = schematic.getOwner() + "/" + name;
        Path uploadPath = Paths.get(basePath, name).normalize();

        SchematicFileNetwork.createFolderIfMissing(basePath);

        if (SchematicFileParser.isUnsupportedExtension(name)) {
            throw new SchematicNetworkException(SchematicImporterMod.translateAsString("message.unsupportedextension", name));
        }

        if (schematic.isSameHash(uploadPath)) {
            throw new SchematicNetworkException(SchematicImporterMod.translateAsString("message.alreadyexists", uploadPath.toString()));
        }

        if (SchematicFileNetwork.isSchematicSizeTooBig(size)) {
            throw new SchematicNetworkException(SchematicFileNetwork.getTooBigSizeMessage(name, size / 1000));
        }

        if (activeUploads.containsKey(key)) {
            return false;
        }

        try {
            Files.deleteIfExists(uploadPath);

            if (hasFileCountLimit) {
                long count;
                try (Stream<Path> list = Files.list(Paths.get(basePath))) {
                    count = list.count();
                }

                // Too many Schematics
                if (count >= Settings.MAX_SCHEMATICS) {
                    Stream<Path> list2 = Files.list(Paths.get(basePath));
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
            activeUploads.put(key, new SchematicNetworkProgress<>(key, uploadPath, writer, size));
            return true;
        } catch (IOException e) {
            throw new SchematicNetworkException(SchematicImporterMod.translateAsString("message.exceptionmessage", uploadPath), e);
        }
    }

    public void handleWriteRequest(String schematicKey, byte[] data) {
        if (activeUploads.containsKey(schematicKey)) {
            SchematicNetworkProgress<OutputStream> entry = activeUploads.get(schematicKey);

            // Size Validations
            if (data.length > Settings.SCHEMATIC_PACKET_SIZE) {
                cancelUpload(schematicKey);
                throw new SchematicNetworkException(SchematicImporterMod.translateAsString("message.oversizedpacket", schematicKey));
            }

            if (!entry.addUploadedBytes(data.length)) {
                cancelUpload(schematicKey);
                throw new SchematicNetworkException(SchematicImporterMod.translateAsString("message.receiveexceed", schematicKey));
            }

            try {
                entry.getValue().write(data);
                entry.resetIdleTime();
                packetSender.accept(CSSchematicReceivedProgress.success(schematicKey, entry.getUploadedBytes()));
                return;
            } catch (IOException e) {
                cancelUpload(schematicKey);
                throw new SchematicNetworkException(SchematicImporterMod.translateAsString("message.exceptionmessage", schematicKey), e);
            }
        }
        throw new SchematicNetworkException(SchematicImporterMod.translateAsString("message.notactiveschem", schematicKey));
    }

    /**
     * @return return false if <code>schematicKey</code> is not found in <code>activeUploads</code>.
     */
    public boolean handleFinishedUpload(String schematicKey) {
        if (activeUploads.containsKey(schematicKey)) {
            try {
                SchematicNetworkProgress<OutputStream> entry = activeUploads.remove(schematicKey);
                entry.getValue().close();

                if (entry.getUploadedBytes() != entry.getTotalBytes()) {
                    Files.deleteIfExists(entry.getFilePath());
                    throw new SchematicNetworkException(SchematicImporterMod.translateAsString("message.leakfile",
                            entry.getUploadedBytes(), entry.getTotalBytes()));
                }

                Logger.info("New Schematic Uploaded: " + schematicKey);
                return true;
            } catch (IOException e) {
                throw new SchematicNetworkException(SchematicImporterMod.translateAsString("message.exceptionmessage", schematicKey), e);
            }
        }
        return false;
    }

    public void cancelUpload(String schematicKey) {
        if (!activeUploads.containsKey(schematicKey)) {
            return;
        }

        SchematicNetworkProgress<OutputStream> entry = activeUploads.remove(schematicKey);
        try {
            entry.getValue().close();
            Files.deleteIfExists(entry.getFilePath());
            Logger.warn("Cancelled Schematic Upload: " + schematicKey);
        } catch (IOException e) {
            throw new SchematicNetworkException(SchematicImporterMod.translateAsString("message.exceptionmessage", schematicKey), e);
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
