package com.won983212.schemimporter.network.loader;

import com.won983212.schemimporter.Logger;
import com.won983212.schemimporter.SchematicImporterMod;
import com.won983212.schemimporter.Settings;
import com.won983212.schemimporter.network.IMessage;
import com.won983212.schemimporter.network.packets.CSSchematicUpload;
import com.won983212.schemimporter.schematic.IProgressEntryProducer;
import com.won983212.schemimporter.schematic.SchematicFile;
import net.minecraft.client.Minecraft;

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

    public void startNewUpload(SchematicFile schematic) {
        String name = schematic.getName();
        if (activeUploads.containsKey(name)) {
            throw new SchematicNetworkException(SchematicImporterMod.translateAsString("message.uploadalready"));
        }

        Path path = Paths.get("schematics", name);

        if (!Files.exists(path)) {
            throw new SchematicNetworkException(SchematicImporterMod.translateAsString("message.missingschematic", path.toString()));
        }

        try {
            long size = Files.size(path);

            // Too big
            if (!Minecraft.getInstance().hasSingleplayerServer() &&
                    SchematicFileNetwork.isSchematicSizeTooBig(size)) {
                throw new SchematicNetworkException(SchematicFileNetwork.getTooBigSizeMessage(name, size / 1000));
            }

            InputStream in = Files.newInputStream(path, StandardOpenOption.READ);
            SchematicNetworkProgress<InputStream> ent = new SchematicNetworkProgress<>(name, in, size);
            packetSender.accept(CSSchematicUpload.begin(schematic, size));
            activeUploads.put(name, ent);
        } catch (IOException e) {
            throw new SchematicNetworkException(SchematicImporterMod.translateAsString("message.exception"), e);
        }
    }

    private void continueUpload(String schematic) {
        if (activeUploads.containsKey(schematic)) {
            final int maxPacketSize = Settings.SCHEMATIC_PACKET_SIZE;
            byte[] data = new byte[maxPacketSize];
            try {
                SchematicNetworkProgress<InputStream> ent = activeUploads.get(schematic);
                int len = ent.getValue().read(data);

                if (len != -1) {
                    if (len < maxPacketSize) {
                        data = Arrays.copyOf(data, len);
                    }
                    packetSender.accept(CSSchematicUpload.write(schematic, data));
                }

                if (len < maxPacketSize) {
                    finishUpload(schematic);
                }
            } catch (Exception e) {
                activeUploads.remove(schematic);
                throw new SchematicNetworkException(SchematicImporterMod.translateAsString("message.exception"), e);
            }
        }
    }

    private void finishUpload(String schematic) {
        SchematicNetworkProgress<InputStream> ent = activeUploads.remove(schematic);
        if (ent != null) {
            packetSender.accept(CSSchematicUpload.finish(schematic));
        }
    }

    public void cancelUpload(String schematic) {
        activeUploads.remove(schematic);
    }

    public void setUploaded(String schematic, long uploaded) {
        SchematicNetworkProgress<InputStream> ent = activeUploads.get(schematic);
        if (ent != null) {
            ent.setUploaded(uploaded);
            Logger.debug(String.format("Uploaded %s %.2f%%", schematic, ent.getProgress()));
        }
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
