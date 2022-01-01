package com.won983212.schemimporter.network.packets;

import com.won983212.schemimporter.CommonMod;
import com.won983212.schemimporter.Logger;
import com.won983212.schemimporter.client.ClientMod;
import com.won983212.schemimporter.network.IMessage;
import com.won983212.schemimporter.network.NetworkDispatcher;
import com.won983212.schemimporter.network.loader.SchematicFileNetwork;
import com.won983212.schemimporter.network.loader.SchematicNetworkException;
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
    public static final int REQUEST = 4;

    private final int code;
    private final String owner;
    private final String schematicKey;
    @Nullable
    private final SchematicFile schematicFile;
    private long size;
    private byte[] data;

    private CSSchematicUpload(int code, String owner, String schematicKey, @Nullable SchematicFile schematicFile, long size, byte[] data) {
        this.code = code;
        this.owner = owner;
        this.schematicKey = schematicKey;
        this.schematicFile = schematicFile;
        this.size = size;
        this.data = data;
    }

    public static CSSchematicUpload begin(SchematicFile schematic, long size) {
        return new CSSchematicUpload(BEGIN, schematic.getOwner(), schematic.getName(), schematic, size, null);
    }

    public static CSSchematicUpload write(String schematicKey, byte[] data) {
        return new CSSchematicUpload(WRITE, "", schematicKey, null, 0, data);
    }

    public static CSSchematicUpload finish(String schematicKey) {
        return new CSSchematicUpload(FINISH, "", schematicKey, null, 0, null);
    }

    public static CSSchematicUpload request(String owner, String schematic) {
        return new CSSchematicUpload(REQUEST, owner, schematic, null, 0, null);
    }

    public CSSchematicUpload(PacketBuffer buffer) {
        code = buffer.readByte();
        owner = buffer.readUtf(256);
        schematicKey = buffer.readUtf(512);

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
        buffer.writeUtf(owner, 256);
        buffer.writeUtf(schematicKey, 512);

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
            SchematicFileNetwork schemNet;

            if (player == null) {
                schemNet = ClientMod.CLIENT_SCHEMATIC_LOADER;
                if (code == REQUEST) {
                    Logger.error("Invaild packet: Client can't handle request packet.");
                    return;
                }
            } else {
                CommonMod.SERVER_SCHEMATIC_LOADER.setPlayer(player);
                schemNet = CommonMod.SERVER_SCHEMATIC_LOADER;
            }

            try {
                if (code == REQUEST) {
                    schemNet.handleUploadRequest(owner, schematicKey);
                }
                if (code == BEGIN) {
                    if (schematicFile == null) {
                        throw new SchematicNetworkException("Schematic file must not be null at BEGIN!");
                    } else {
                        schemNet.handleNewUpload(schematicFile, size);
                    }
                }
                if (code == WRITE) {
                    schemNet.handleWriteRequest(schematicKey, data);
                }
                if (code == FINISH) {
                    schemNet.handleFinishedUpload(schematicKey);
                }
            } catch (SchematicNetworkException e) {
                schemNet.handleException(e);
                IMessage failPacket = CSSchematicReceivedProgress.fail(schematicKey);
                if (player == null) {
                    NetworkDispatcher.sendToServer(failPacket);
                } else {
                    NetworkDispatcher.send(PacketDistributor.PLAYER.with(() -> player), failPacket);
                }
            }
        });
        context.get().setPacketHandled(true);
    }
}
