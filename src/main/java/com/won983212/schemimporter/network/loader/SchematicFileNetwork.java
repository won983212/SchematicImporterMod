package com.won983212.schemimporter.network.loader;

import com.won983212.schemimporter.SchematicImporterMod;
import com.won983212.schemimporter.Settings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SchematicFileNetwork {
    protected static void createFolderIfMissing(String path) {
        try {
            Files.createDirectories(Paths.get(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static boolean isSchematicSizeTooBig(long size) {
        return size > Settings.MAX_TOTAL_SCHEMATIC_SIZE * 1000;
    }

    protected static String getTooBigSizeMessage(String name, long size) {
        return SchematicImporterMod.translateAsString("message.uploadTooLarge", name, size / 1000) + '\n'
                + SchematicImporterMod.translateAsString("message.maxAllowedSize", Settings.MAX_TOTAL_SCHEMATIC_SIZE);
    }
}
