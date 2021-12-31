package com.won983212.schemimporter.network.loader;

import com.won983212.schemimporter.client.ClientMod;
import com.won983212.schemimporter.network.IMessage;
import com.won983212.schemimporter.network.NetworkDispatcher;
import com.won983212.schemimporter.schematic.SchematicFile;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class ClientSchematicLoader extends SchematicFileNetwork implements Consumer<IMessage> {
    private final SchematicSender sender;
    private final SchematicReceiver receiver;

    public ClientSchematicLoader() {
        this.sender = new SchematicSender(this);
        this.receiver = new SchematicReceiver(this, false);
        createFolderIfMissing("schematics");
    }

    public void tick() {
        try {
            sender.tick();
        } catch (SchematicNetworkException e) {
            handleException(e);
        }
    }

    public void shutdown(){
        sender.shutdown();
    }

    public void startNewUpload(SchematicFile schematic) {
        try {
            sender.startNewUpload(schematic);
        } catch (SchematicNetworkException e) {
            handleException(e);
        }
    }

    private void handleException(SchematicNetworkException e) {
        Minecraft.getInstance().gui.getChat().addMessage(new StringTextComponent(e.getMessage()));
        e.printStackTrace();
    }

    public void handleFailState(String schematic) {
        sender.cancelUpload(schematic);
    }

    public void handleServerProgress(String schematic, long uploaded) {
        sender.setUploaded(schematic, uploaded);
    }

    public void registerProgressProducers() {
        ClientMod.SCHEMATIC_UPLOAD_SCREEN.registerProgressProducer(sender);
        //TODO receiver도 설치
    }

    @Override
    public void accept(IMessage message) {
        NetworkDispatcher.sendToServer(message);
    }
}
