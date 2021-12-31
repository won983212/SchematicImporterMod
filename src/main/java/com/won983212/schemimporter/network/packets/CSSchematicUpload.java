package com.won983212.schemimporter.network.packets;

import com.won983212.schemimporter.CommonMod;
import com.won983212.schemimporter.Logger;
import com.won983212.schemimporter.network.IMessage;
import com.won983212.schemimporter.network.NetworkDispatcher;
import com.won983212.schemimporter.schematic.SchematicFile;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent.Context;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class CSSchematicUpload implements IMessage {

    public static final int BEGIN = 0;
    public static final int WRITE = 1;
    public static final int FINISH = 2;

    private final int code;
    private final String schematic;
    @Nullable
    private final SchematicFile schematicFile;
    private long size;
    private byte[] data;

    private CSSchematicUpload(int code, String schematic, @Nullable SchematicFile schematicFile, long size, byte[] data) {
        this.code = code;
        this.schematic = schematic;
        this.schematicFile = schematicFile;
        this.size = size;
        this.data = data;
    }

    public static CSSchematicUpload begin(SchematicFile schematic, long size) {
        return new CSSchematicUpload(BEGIN, schematic.getName(), schematic, size, null);
    }

    public static CSSchematicUpload write(String schematic, byte[] data) {
        return new CSSchematicUpload(WRITE, schematic, null, 0, data);
    }

    public static CSSchematicUpload finish(String schematic) {
        return new CSSchematicUpload(FINISH, schematic, null, 0, null);
    }

    public CSSchematicUpload(PacketBuffer buffer) {
        code = buffer.readByte();
        schematic = buffer.readUtf(256);

        boolean hasFileInfo = buffer.readBoolean();
        if (hasFileInfo) {
            schematicFile = new SchematicFile(buffer);
        } else {
            schematicFile = null;
        }

        if (code == BEGIN) {
            size = buffer.readLong();
        }
        if (code == WRITE) {
            data = buffer.readByteArray();
        }
    }

    public void write(PacketBuffer buffer) {
        buffer.writeByte(code);
        buffer.writeUtf(schematic, 256);

        buffer.writeBoolean(schematicFile != null);
        if (schematicFile != null) {
            schematicFile.writeTo(buffer);
        }

        if (code == BEGIN) {
            buffer.writeLong(size);
        }
        if (code == WRITE) {
            buffer.writeByteArray(data);
        }
    }

    public void handle(Supplier<Context> context) {
        context.get().enqueueWork(() -> {
            Context ctx = context.get();
            ServerPlayerEntity player = ctx.getSender();
            if (player == null) {
                return;
            }

            boolean success = false;
            if (code == BEGIN) {
                if (schematicFile == null) {
                    Logger.error("Schematic file must not be null at BEGIN!");
                    success = false;
                } else {
                    success = CommonMod.SERVER_SCHEMATIC_LOADER.handleNewUpload(player, schematicFile, size);
                }
            }
            if (code == WRITE) {
                success = CommonMod.SERVER_SCHEMATIC_LOADER.handleWriteRequest(player, schematic, data);
            }
            if (code == FINISH) {
                success = CommonMod.SERVER_SCHEMATIC_LOADER.handleFinishedUpload(player, schematic);
            }
            if (!success) {
                NetworkDispatcher.send(PacketDistributor.PLAYER.with(() -> player),
                        CSSchematicReceivedProgress.fail(schematic));
            }
        });
        context.get().setPacketHandled(true);
    }

}
