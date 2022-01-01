package com.won983212.schemimporter.network.loader;

import com.won983212.schemimporter.Logger;
import com.won983212.schemimporter.Settings;
import com.won983212.schemimporter.item.SchematicItem;
import com.won983212.schemimporter.network.IMessage;
import com.won983212.schemimporter.network.NetworkDispatcher;
import com.won983212.schemimporter.schematic.SchematicFile;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.network.PacketDistributor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ServerSchematicLoader implements SchematicFileNetwork {
    private final SchematicSender sender;
    private final SchematicReceiver receiver;
    private ServerPlayerEntity player;


    public ServerSchematicLoader() {
        this.sender = new SchematicSender(this);
        this.receiver = new SchematicReceiver(this, true);
        SchematicFileNetwork.createFolderIfMissing(Settings.SCHEMATIC_DIR_NAME);
    }

    public void tick() {
        try {
            sender.tick();
            receiver.tick();
        } catch (SchematicNetworkException e) {
            Logger.error(e);
        }
    }

    public void shutdown() {
        sender.shutdown();
        receiver.shutdown();
    }

    public void setPlayer(ServerPlayerEntity player) {
        this.player = player;
    }

    public boolean deleteSchematic(ServerPlayerEntity player, String schematic) {
        this.player = player;
        Path schematicPath = tryGetUploadPath(schematic, false);
        try {
            Files.deleteIfExists(schematicPath);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void handleUploadRequest(String owner, String schematic) throws SchematicNetworkException {
        tryGetUploadPath(owner + "/" + schematic, true);
        Path fileDirectory = Paths.get(Settings.SCHEMATIC_DIR_NAME, Settings.UPLOADED_SCHEMATIC_DIR_NAME, owner);
        SchematicFile file = new SchematicFile(owner, fileDirectory.resolve(schematic).toFile());
        sender.startNewUpload(fileDirectory.toString(), file);
    }

    public void handleNewUpload(SchematicFile schematic, long size) throws SchematicNetworkException {
        tryGetUploadPath(schematic.getName(), false);
        if (!receiver.newDownloadRequest(getBasePath(), schematic, size)) {
            giveSchematicItem(schematic.getName());
            throw new SchematicNetworkException("");
        }
    }

    public void handleWriteRequest(String schematicKey, byte[] data) throws SchematicNetworkException {
        receiver.handleWriteRequest(schematicKey, data);
    }

    public void handleFinishedUpload(String schematicKey) throws SchematicNetworkException {
        if (receiver.handleFinishedUpload(schematicKey)) {
            giveSchematicItem(SchematicFile.keyToName(schematicKey));
        }
    }

    public void handleFailState(String schematicKey) {
        sender.cancelUpload(schematicKey);
    }

    public void handleProgress(String schematicKey, long uploaded) {
        sender.handleProgress(schematicKey, uploaded);
    }

    private Path tryGetUploadPath(String schematicName, boolean allowOtherPlayerAccess) {
        Path basePath = Paths.get(Settings.SCHEMATIC_DIR_NAME, Settings.UPLOADED_SCHEMATIC_DIR_NAME).toAbsolutePath();
        if (!allowOtherPlayerAccess) {
            basePath = basePath.resolve(player.getGameProfile().getName());
        }
        Path uploadPath = basePath.resolve(schematicName).normalize();
        if (!uploadPath.startsWith(basePath)) {
            throw new SchematicNetworkException("Attempted Schematic Upload with directory escape: " + schematicName);
        }
        return uploadPath;
    }

    private String getBasePath() {
        return Settings.SCHEMATIC_DIR_NAME + "/" + Settings.UPLOADED_SCHEMATIC_DIR_NAME
                + "/" + player.getGameProfile().getName();
    }

    private void giveSchematicItem(String schematic) {
        ItemStack item = SchematicItem.create(schematic, player.getGameProfile().getName());
        player.inventory.add(item);
    }

    @Override
    public void accept(IMessage message) {
        if (player == null) {
            Logger.warn("Can't send packet because player is null. It is a bug!");
        }
        NetworkDispatcher.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
