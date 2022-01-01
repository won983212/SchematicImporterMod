package com.won983212.schemimporter.utility;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SChunkDataPacket;
import net.minecraft.network.play.server.SUpdateLightPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.AbstractChunkProvider;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ServerWorld;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

public class FastBlockPlacer {
    private final World world;
    private final boolean isPlainWorld;
    private final HashSet<ChunkPos> chunks = new HashSet<>();
    private Queue<ChunkPos> chunkQueue;


    public FastBlockPlacer(World world) {
        this.world = world;
        this.isPlainWorld = !(world instanceof ServerWorld);
    }

    public boolean setBlock(BlockPos pos, BlockState state, int flag) {
        ChunkPos chunkPos = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
        chunks.add(chunkPos);
        return world.setBlock(pos, state, flag & -3);
    }

    public boolean update() {
        if (isPlainWorld) {
            return false;
        }
        if (chunkQueue == null) {
            AbstractChunkProvider provider = world.getChunkSource();
            chunkQueue = chunks.stream()
                    .filter((pos) -> provider.hasChunk(pos.x, pos.z))
                    .collect(Collectors.toCollection(LinkedList::new));
            chunks.clear();
        }
        if (chunkQueue.isEmpty()) {
            chunkQueue = null;
            return false;
        }
        sendChunkDataTo(chunkQueue.poll());
        return true;
    }

    private List<ServerPlayerEntity> getPlayers(ChunkPos pos) {
        ServerWorld sWorld = (ServerWorld) world;
        int viewDist = sWorld.getServer().getPlayerList().getViewDistance();
        return sWorld.getPlayers((p) -> pos.x >= p.xChunk - viewDist && pos.x <= p.xChunk + viewDist
                && pos.z >= p.zChunk - viewDist && pos.z <= p.zChunk + viewDist);
    }

    private void sendChunkDataTo(ChunkPos pos) {
        getPlayers(pos).forEach((player) -> {
            Chunk chunk = world.getChunk(pos.x, pos.z);
            SChunkDataPacket chunkDataPacket = new SChunkDataPacket(chunk, 65535);
            SUpdateLightPacket updateLightPacket = new SUpdateLightPacket(chunk.getPos(), world.getLightEngine(), true);
            player.trackChunk(chunk.getPos(), chunkDataPacket, updateLightPacket);
        });
    }
}
