package com.won983212.schemimporter.network.loader;

import com.won983212.schemimporter.Logger;
import com.won983212.schemimporter.Settings;
import com.won983212.schemimporter.client.ClientMod;
import com.won983212.schemimporter.network.IMessage;
import com.won983212.schemimporter.network.NetworkDispatcher;
import com.won983212.schemimporter.network.packets.CSSchematicUpload;
import com.won983212.schemimporter.schematic.SchematicFile;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class ClientSchematicLoader implements SchematicFileNetwork {
    private final SchematicSender sender;
    private final SchematicReceiver receiver;
    private final ArrayList<Consumer<String>> uploadRequestCallbacks;


    public ClientSchematicLoader() {
        this.sender = new SchematicSender(this);
        this.receiver = new SchematicReceiver(this, false);
        this.uploadRequestCallbacks = new ArrayList<>();
        SchematicFileNetwork.createFolderIfMissing(Settings.SCHEMATIC_DIR_NAME);
    }

    public void registerRequestFinishCallback(Consumer<String> callback) {
        this.uploadRequestCallbacks.add(callback);
    }

    public void tick() {
        try {
            sender.tick();
            receiver.tick();
        } catch (SchematicNetworkException e) {
            handleException(e);
        }
    }

    public void shutdown() {
        sender.shutdown();
        receiver.shutdown();
    }

    public void startNewUpload(SchematicFile schematic) {
        try {
            sender.startNewUpload(Settings.SCHEMATIC_DIR_NAME, schematic);
        } catch (SchematicNetworkException e) {
            handleException(e);
        }
    }

    public void requestUpload(ItemStack stack) {
        String owner = stack.getTag().getString("Owner");
        String schematic = stack.getTag().getString("File");
        Path key = Paths.get(Settings.SCHEMATIC_DIR_NAME, schematic);
        if (!receiver.isUploading(key.toString())) {
            NetworkDispatcher.sendToServer(CSSchematicUpload.request(owner, schematic));
        }
    }

    public void handleUploadRequest(String owner, String schematic) {
        throw new SchematicNetworkException("Client can't handle upload request.");
    }

    public void handleNewUpload(SchematicFile schematic, long size) {
        Path basePath = Paths.get(Settings.SCHEMATIC_DIR_NAME).toAbsolutePath();
        Path uploadPath = basePath.resolve(schematic.getName()).normalize();
        if (!uploadPath.startsWith(basePath)) {
            throw new SchematicNetworkException("Attempted Schematic Upload with directory escape: " + basePath);
        }
        receiver.newDownloadRequest(Settings.SCHEMATIC_DIR_NAME, schematic, size);
    }

    public void handleWriteRequest(String schematicKey, byte[] data) {
        receiver.handleWriteRequest(schematicKey, data);
    }

    public void handleFinishedUpload(String schematicKey) throws SchematicNetworkException {
        if (receiver.handleFinishedUpload(schematicKey)) {
            uploadRequestCallbacks.forEach((c) -> c.accept(SchematicFile.keyToName(schematicKey)));
        }
    }

    public void handleFailState(String schematicKey) {
        sender.cancelUpload(schematicKey);
    }

    public void handleProgress(String schematicKey, long uploaded) {
        sender.handleProgress(schematicKey, uploaded);
    }

    @Override
    public void handleException(SchematicNetworkException e) {
        Minecraft.getInstance().gui.getChat().addMessage(new StringTextComponent(e.getMessage()));
        Logger.error(e);
    }

    public void registerProgressProducers() {
        ClientMod.SCHEMATIC_UPLOAD_SCREEN.registerProgressProducer(sender);
        ClientMod.SCHEMATIC_UPLOAD_SCREEN.registerProgressProducer(receiver);
    }

    @Override
    public void accept(IMessage message) {
        NetworkDispatcher.sendToServer(message);
    }
}
