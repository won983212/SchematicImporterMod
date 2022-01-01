package com.won983212.schemimporter.network.packets;

import com.won983212.schemimporter.CommonMod;
import com.won983212.schemimporter.client.ClientMod;
import com.won983212.schemimporter.network.IMessage;
import com.won983212.schemimporter.network.loader.SchematicFileNetwork;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class CSSchematicReceivedProgress implements IMessage {

    public static final int SUCCESS = 1;
    public static final int FAIL = 2;

    private final int code;
    private final String schematicKey;
    private final long receivedBytes;

    private CSSchematicReceivedProgress(int code, String schematicKey, long receivedBytes) {
        this.code = code;
        this.schematicKey = schematicKey;
        this.receivedBytes = receivedBytes;
    }

    public static CSSchematicReceivedProgress success(String schematicKey, long receivedBytes) {
        return new CSSchematicReceivedProgress(SUCCESS, schematicKey, receivedBytes);
    }

    public static CSSchematicReceivedProgress fail(String schematic) {
        return new CSSchematicReceivedProgress(FAIL, schematic, 0);
    }

    public CSSchematicReceivedProgress(PacketBuffer buf) {
        code = buf.readByte();
        schematicKey = buf.readUtf(256);
        receivedBytes = buf.readLong();
    }

    @Override
    public void write(PacketBuffer buf) {
        buf.writeByte(code);
        buf.writeUtf(schematicKey, 256);
        buf.writeLong(receivedBytes);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            NetworkEvent.Context ctx = context.get();
            ServerPlayerEntity player = ctx.getSender();
            SchematicFileNetwork schemNet;

            if (player == null) {
                schemNet = ClientMod.CLIENT_SCHEMATIC_LOADER;
            } else {
                CommonMod.SERVER_SCHEMATIC_LOADER.setPlayer(player);
                schemNet = CommonMod.SERVER_SCHEMATIC_LOADER;
            }

            if (code == SUCCESS) {
                schemNet.handleProgress(schematicKey, receivedBytes);
            }
            if (code == FAIL) {
                schemNet.handleFailState(schematicKey);
            }
        });
        context.get().setPacketHandled(true);
    }
}
