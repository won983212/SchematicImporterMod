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
import java.util.function.Consumer;

public class ServerSchematicLoader extends SchematicFileNetwork implements Consumer<IMessage> {
    private final SchematicReceiver receiver;
    private ServerPlayerEntity player;

    public ServerSchematicLoader() {
        receiver = new SchematicReceiver(this, true);
    }

    public String getSchematicPath() {
        return "schematics/" + Settings.USER_SCHEMATIC_DIR_NAME;
    }

    public void tick() {
        receiver.tick();
    }

    public void shutdown() {
        receiver.shutdown();
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
        Logger.debug("New");
        this.player = player;

        Path playerSchematicsPath = Paths.get(getSchematicPath(), player.getGameProfile().getName()).toAbsolutePath();
        Path uploadPath = playerSchematicsPath.resolve(schematic.getName()).normalize();
        if (!uploadPath.startsWith(playerSchematicsPath)) {
            Logger.warn("Attempted Schematic Upload with directory escape: " + playerSchematicsPath);
            return false;
        }

        try {
            if (!receiver.handleNewUpload(getBasePath(), schematic, size)) {
                giveSchematicItem(player, schematic.getName());
            }
            return true;
        } catch (SchematicNetworkException e) {
            handleException(e);
        }
        return false;
    }

    public boolean handleWriteRequest(ServerPlayerEntity player, String schematic, byte[] data) {
        Logger.debug("write");
        this.player = player;
        try {
            receiver.handleWriteRequest(getBasePath() + "/" + schematic, schematic, data);
            return true;
        } catch (SchematicNetworkException e) {
            handleException(e);
        }
        return false;
    }

    public boolean handleFinishedUpload(ServerPlayerEntity player, String schematic) {
        Logger.debug("finish");
        this.player = player;
        try {
            if (receiver.handleFinishedUpload(getBasePath() + "/" + schematic)) {
                giveSchematicItem(player, schematic);
                return true;
            }
        } catch (SchematicNetworkException e) {
            handleException(e);
        }
        return false;
    }

    private String getBasePath() {
        return "schematics/" + Settings.USER_SCHEMATIC_DIR_NAME + "/" + player.getGameProfile().getName();
    }

    private void handleException(SchematicNetworkException e) {
        e.printStackTrace();
    }

    private void giveSchematicItem(ServerPlayerEntity player, String schematic) {
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
