package com.won983212.schemimporter.network.loader;

import com.won983212.schemimporter.Logger;
import com.won983212.schemimporter.Settings;
import com.won983212.schemimporter.item.SchematicItem;
import com.won983212.schemimporter.network.IMessage;
import com.won983212.schemimporter.network.NetworkDispatcher;
import com.won983212.schemimporter.network.packets.SSchematicReceivedProgress;
import com.won983212.schemimporter.schematic.SchematicFile;
import com.won983212.schemimporter.schematic.parser.SchematicFileParser;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.network.PacketDistributor;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class ServerSchematicLoader extends SchematicFileNetwork {
    private final Map<String, SchematicDownloadEntry> activeUploads;


    public ServerSchematicLoader() {
        activeUploads = new HashMap<>();
    }

    public String getSchematicPath() {
        return "schematics/" + Settings.USER_SCHEMATIC_DIR_NAME;
    }

    public void tick() {
        // Detect Timed out Uploads
        Set<String> deadEntries = new HashSet<>();
        for (String upload : activeUploads.keySet()) {
            SchematicDownloadEntry entry = activeUploads.get(upload);

            if (entry.idleTime++ > Settings.SCHEMATIC_IDLE_TIMEOUT) {
                Logger.warn("Schematic Upload timed out: " + upload);
                deadEntries.add(upload);
            }

        }

        // Remove Timed out Uploads
        deadEntries.forEach(this::cancelUpload);
    }

    public void shutdown() {
        // Close open streams
        new HashSet<>(activeUploads.keySet()).forEach(this::cancelUpload);
    }

    public boolean deleteSchematic(ServerPlayerEntity player, String schematic) {
        Path playerSchematicsPath = Paths.get(getSchematicPath(), player.getGameProfile().getName()).toAbsolutePath();
        Path path = playerSchematicsPath.resolve(schematic).normalize();
        if (!path.startsWith(playerSchematicsPath)) {
            Logger.warn("Attempted Schematic Upload with directory escape: " + schematic);
            return false;
        }

        try {
            Files.deleteIfExists(path);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean handleNewUpload(ServerPlayerEntity player, SchematicFile schematic, long size) {
        String name = schematic.getName();
        String playerPath = getSchematicPath() + "/" + player.getGameProfile().getName();
        String playerSchematicId = player.getGameProfile().getName() + "/" + name;

        createFolderIfMissing(playerPath);

        // Unsupported Format
        if (SchematicFileParser.isUnsupportedExtension(name)) {
            Logger.warn("Attempted Schematic Upload with non-supported Format: " + playerSchematicId);
            return false;
        }

        Path playerSchematicsPath = Paths.get(getSchematicPath(), player.getGameProfile().getName()).toAbsolutePath();

        Path uploadPath = playerSchematicsPath.resolve(name).normalize();
        if (!uploadPath.startsWith(playerSchematicsPath)) {
            Logger.warn("Attempted Schematic Upload with directory escape: " + playerSchematicId);
            return false;
        }

        if (schematic.equals(uploadPath)) {
            Logger.warn("Already exists file: " + playerSchematicId + ". So, it will use that cached file");
            giveSchematicItem(player, name);
            return false;
        }

        // Too big
        if (isSchematicSizeTooBig(player, size)) {
            return false;
        }

        // Skip existing Uploads
        if (activeUploads.containsKey(playerSchematicId)) {
            return false;
        }

        try {
            // Delete schematic with same name
            Files.deleteIfExists(uploadPath);

            // Too many Schematics
            long count;
            try (Stream<Path> list = Files.list(Paths.get(playerPath))) {
                count = list.count();
            }

            if (count >= Settings.MAX_SCHEMATICS) {
                Stream<Path> list2 = Files.list(Paths.get(playerPath));
                Optional<Path> lastFilePath = list2.filter(f -> !Files.isDirectory(f))
                        .min(Comparator.comparingLong(f -> f.toFile()
                                .lastModified()));
                list2.close();
                if (lastFilePath.isPresent()) {
                    Files.deleteIfExists(lastFilePath.get());
                }
            }

            // Open Stream
            OutputStream writer = Files.newOutputStream(uploadPath);
            activeUploads.put(playerSchematicId, new SchematicDownloadEntry(writer, size));

            return true;
        } catch (IOException e) {
            Logger.error("Exception Thrown when starting Upload: " + playerSchematicId);
            e.printStackTrace();
        }
        return false;
    }

    public boolean handleWriteRequest(ServerPlayerEntity player, String schematic, byte[] data) {
        String playerSchematicId = player.getGameProfile().getName() + "/" + schematic;

        if (activeUploads.containsKey(playerSchematicId)) {
            SchematicDownloadEntry entry = activeUploads.get(playerSchematicId);
            entry.bytesUploaded += data.length;

            // Size Validations
            if (data.length > Settings.SCHEMATIC_PACKET_SIZE) {
                Logger.warn("Oversized Upload Packet received: " + playerSchematicId);
                cancelUpload(playerSchematicId);
                return false;
            }

            if (entry.bytesUploaded > entry.totalBytes) {
                Logger.warn("Received more data than Expected: " + playerSchematicId);
                cancelUpload(playerSchematicId);
                return false;
            }

            try {
                entry.stream.write(data);
                entry.idleTime = 0;

                float progress = (float) ((double) entry.bytesUploaded / entry.totalBytes);
                IMessage packet = SSchematicReceivedProgress.success(schematic, progress);
                NetworkDispatcher.send(PacketDistributor.PLAYER.with(() -> player), packet);
                return true;
            } catch (IOException e) {
                Logger.error("Exception Thrown when uploading Schematic: " + playerSchematicId);
                e.printStackTrace();
                cancelUpload(playerSchematicId);
            }
        }
        return false;
    }

    protected void cancelUpload(String playerSchematicId) {
        if (!activeUploads.containsKey(playerSchematicId)) {
            return;
        }

        SchematicDownloadEntry entry = activeUploads.remove(playerSchematicId);
        try {
            entry.stream.close();
            Files.deleteIfExists(Paths.get(getSchematicPath(), playerSchematicId));
            Logger.warn("Cancelled Schematic Upload: " + playerSchematicId);

        } catch (IOException e) {
            Logger.error("Exception Thrown when cancelling Upload: " + playerSchematicId);
            e.printStackTrace();
        }
    }

    public boolean handleFinishedUpload(ServerPlayerEntity player, String schematic) {
        String playerSchematicId = player.getGameProfile().getName() + "/" + schematic;

        if (activeUploads.containsKey(playerSchematicId)) {
            try {
                activeUploads.get(playerSchematicId).stream.close();
                activeUploads.remove(playerSchematicId);

                Logger.info("New Schematic Uploaded: " + playerSchematicId);
                giveSchematicItem(player, schematic);

                return true;
            } catch (IOException e) {
                Logger.error("Exception Thrown when finishing Upload: " + playerSchematicId);
                e.printStackTrace();
            }
        }
        return false;
    }

    private void giveSchematicItem(ServerPlayerEntity player, String schematic) {
        ItemStack item = SchematicItem.create(schematic, player.getGameProfile().getName());
        player.inventory.add(item);
    }

    public static class SchematicDownloadEntry {
        public final OutputStream stream;
        public long bytesUploaded;
        public final long totalBytes;
        public int idleTime;

        public SchematicDownloadEntry(OutputStream stream, long totalBytes) {
            this.stream = stream;
            this.totalBytes = totalBytes;
            this.bytesUploaded = 0;
            this.idleTime = 0;
        }
    }
}
