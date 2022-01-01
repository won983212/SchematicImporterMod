package com.won983212.schemimporter.network.loader;

import com.won983212.schemimporter.Logger;
import com.won983212.schemimporter.SchematicImporterMod;
import com.won983212.schemimporter.Settings;
import com.won983212.schemimporter.network.IMessage;
import com.won983212.schemimporter.network.packets.CSSchematicUpload;
import com.won983212.schemimporter.schematic.IProgressEntryProducer;
import com.won983212.schemimporter.schematic.SchematicFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Consumer;

public class SchematicSender implements IProgressEntryProducer {
    private final Consumer<IMessage> packetSender;
    private final Map<String, SchematicNetworkProgress<InputStream>> activeUploads;
    private int packetCycle;


    public SchematicSender(Consumer<IMessage> packetSender) {
        this.activeUploads = new HashMap<>();
        this.packetSender = packetSender;
    }


    public void tick() {
        if (activeUploads.isEmpty()) {
            return;
        }
        if (packetCycle-- > 0) {
            return;
        }
        packetCycle = Settings.PACKET_DELAY;

        for (String schematic : new HashSet<>(activeUploads.keySet())) {
            continueUpload(schematic);
        }
    }

    public void shutdown() {
        activeUploads.clear();
    }

    public void startNewUpload(String basePath, SchematicFile schematicFile) {
        String name = schematicFile.getOwner() + "/" + schematicFile.getName();
        if (activeUploads.containsKey(name)) {
            throw new SchematicNetworkException(SchematicImporterMod.translateAsString("message.uploadalready"));
        }

        Path path = Paths.get(basePath, schematicFile.getName());

        if (!Files.exists(path)) {
            throw new SchematicNetworkException(SchematicImporterMod.translateAsString("message.missingschematic", path.toString()));
        }

        try {
            long size = Files.size(path);
            if (SchematicFileNetwork.isSchematicSizeTooBig(size)) {
                throw new SchematicNetworkException(SchematicFileNetwork.getTooBigSizeMessage(name, size / 1000));
            }

            InputStream in = Files.newInputStream(path, StandardOpenOption.READ);
            SchematicNetworkProgress<InputStream> ent = new SchematicNetworkProgress<>(name, path, in, size);
            packetSender.accept(CSSchematicUpload.begin(schematicFile, size));
            activeUploads.put(name, ent);
        } catch (IOException e) {
            throw new SchematicNetworkException(SchematicImporterMod.translateAsString("message.exception"), e);
        }
    }

    private void continueUpload(String schematicKey) {
        if (activeUploads.containsKey(schematicKey)) {
            final int maxPacketSize = Settings.SCHEMATIC_PACKET_SIZE;
            byte[] data = new byte[maxPacketSize];
            try {
                SchematicNetworkProgress<InputStream> ent = activeUploads.get(schematicKey);
                int len = ent.getValue().read(data);

                if (len != -1) {
                    if (len < maxPacketSize) {
                        data = Arrays.copyOf(data, len);
                    }
                    packetSender.accept(CSSchematicUpload.write(schematicKey, data));
                }

                if (len < maxPacketSize) {
                    finishUpload(schematicKey);
                }
            } catch (Exception e) {
                activeUploads.remove(schematicKey);
                throw new SchematicNetworkException(SchematicImporterMod.translateAsString("message.exception"), e);
            }
        }
    }

    private void finishUpload(String schematicKey) {
        SchematicNetworkProgress<InputStream> ent = activeUploads.remove(schematicKey);
        if (ent != null) {
            packetSender.accept(CSSchematicUpload.finish(schematicKey));
        }
    }

    public void handleProgress(String schematicKey, long uploaded) {
        SchematicNetworkProgress<InputStream> ent = activeUploads.get(schematicKey);
        if (ent != null) {
            ent.setUploaded(uploaded);
            Logger.debug(String.format("Uploaded %s %.2f%%", schematicKey, ent.getProgress()));
        }
    }

    public void cancelUpload(String schematicKey) {
        activeUploads.remove(schematicKey);
    }

    @Override
    public Iterable<SchematicNetworkProgress<InputStream>> getProgressEntries() {
        return activeUploads.values();
    }

    @Override
    public int size() {
        return activeUploads.size();
    }
}
