package com.won983212.schemimporter.network.packets;

import com.won983212.schemimporter.CommonMod;
import com.won983212.schemimporter.network.IMessage;
import com.won983212.schemimporter.network.NetworkDispatcher;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.function.Supplier;

public class CSchematicFileDelete implements IMessage {
    protected final String schematicFileName;

    public CSchematicFileDelete(String schematicFileName) {
        this.schematicFileName = schematicFileName;
        if (schematicFileName == null) {
            throw new NullPointerException("schematicFileName");
        }
    }

    public CSchematicFileDelete(PacketBuffer buffer) {
        schematicFileName = buffer.readUtf();
    }

    public void write(PacketBuffer buffer) {
        buffer.writeUtf(schematicFileName);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayerEntity player = context.get().getSender();
            if (player == null) {
                return;
            }
            if (CommonMod.SERVER_SCHEMATIC_LOADER.deleteSchematic(player, schematicFileName)) {
                SSchematicDeleteResponse packet = new SSchematicDeleteResponse(schematicFileName);
                NetworkDispatcher.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }
        });
        ctx.setPacketHandled(true);
    }
}
