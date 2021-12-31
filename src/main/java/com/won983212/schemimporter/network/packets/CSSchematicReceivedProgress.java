package com.won983212.schemimporter.network.packets;

import com.won983212.schemimporter.client.ClientMod;
import com.won983212.schemimporter.network.IMessage;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class CSSchematicReceivedProgress implements IMessage {

    public static final int SUCCESS = 1;
    public static final int FAIL = 2;

    private final int code;
    private final String schematic;
    private final long receivedBytes;

    private CSSchematicReceivedProgress(int code, String schematic, long receivedBytes) {
        this.code = code;
        this.schematic = schematic;
        this.receivedBytes = receivedBytes;
    }

    public static CSSchematicReceivedProgress success(String schematic, long receivedBytes) {
        return new CSSchematicReceivedProgress(SUCCESS, schematic, receivedBytes);
    }

    public static CSSchematicReceivedProgress fail(String schematic) {
        return new CSSchematicReceivedProgress(FAIL, schematic, 0);
    }

    public CSSchematicReceivedProgress(PacketBuffer buf) {
        code = buf.readByte();
        schematic = buf.readUtf(256);
        receivedBytes = buf.readLong();
    }

    @Override
    public void write(PacketBuffer buf) {
        buf.writeByte(code);
        buf.writeUtf(schematic, 256);
        buf.writeLong(receivedBytes);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (code == SUCCESS) {
                ClientMod.CLIENT_SCHEMATIC_LOADER.handleServerProgress(schematic, receivedBytes);
            }
            if (code == FAIL) {
                ClientMod.CLIENT_SCHEMATIC_LOADER.handleFailState(schematic);
            }
        });
        context.get().setPacketHandled(true);
    }
}
