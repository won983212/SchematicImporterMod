package com.won983212.schemimporter.network.loader;

import com.won983212.schemimporter.Logger;
import com.won983212.schemimporter.SchematicImporterMod;
import com.won983212.schemimporter.Settings;
import com.won983212.schemimporter.network.IMessage;
import com.won983212.schemimporter.schematic.SchematicFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Consumer;

public interface SchematicFileNetwork extends Consumer<IMessage> {

    void handleUploadRequest(String owner, String schematic) throws SchematicNetworkException;

    void handleNewUpload(SchematicFile schematic, long size) throws SchematicNetworkException;

    void handleWriteRequest(String schematicKey, byte[] data) throws SchematicNetworkException;

    void handleFinishedUpload(String schematicKey) throws SchematicNetworkException;

    void handleFailState(String schematicKey);

    void handleProgress(String schematicKey, long uploaded);

    @Override
    void accept(IMessage message);

    default void handleException(SchematicNetworkException e) {
        if (!e.getMessage().isEmpty()) {
            Logger.error(e);
        }
    }

    static void createFolderIfMissing(String path) {
        try {
            Files.createDirectories(Paths.get(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static boolean isSchematicSizeTooBig(long size) {
        return size > Settings.MAX_TOTAL_SCHEMATIC_SIZE * 1000;
    }

    static String getTooBigSizeMessage(String name, long size) {
        return SchematicImporterMod.translateAsString("message.uploadTooLarge", name, size / 1000) + '\n'
                + SchematicImporterMod.translateAsString("message.maxAllowedSize", Settings.MAX_TOTAL_SCHEMATIC_SIZE);
    }
}
