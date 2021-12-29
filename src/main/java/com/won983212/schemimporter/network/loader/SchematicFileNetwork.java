package com.won983212.schemimporter.network.loader;

import com.won983212.schemimporter.SchematicImporterMod;
import com.won983212.schemimporter.Settings;
import net.minecraft.entity.player.PlayerEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SchematicFileNetwork {
    protected void createFolderIfMissing(String path) {
        try {
            Files.createDirectories(Paths.get(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static boolean isSchematicSizeTooBig(PlayerEntity player, long size) {
        if (size > Settings.MAX_TOTAL_SCHEMATIC_SIZE * 1000) {
            if (player != null) {
                player.sendMessage(SchematicImporterMod.translate("schematics.uploadTooLarge").append(" (" + size / 1000 + " KB)."), player.getUUID());
                player.sendMessage(SchematicImporterMod.translate("schematics.maxAllowedSize").append(" " + Settings.MAX_TOTAL_SCHEMATIC_SIZE + " KB"), player.getUUID());
            }
            return true;
        }
        return false;
    }
}
