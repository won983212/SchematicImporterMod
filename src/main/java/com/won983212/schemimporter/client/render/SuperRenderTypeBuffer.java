package com.won983212.schemimporter.client.render;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.util.Util;

import java.util.SortedMap;

public class SuperRenderTypeBuffer implements IRenderTypeBuffer {

    static SuperRenderTypeBuffer instance;

    public static SuperRenderTypeBuffer getInstance() {
        if (instance == null) {
            instance = new SuperRenderTypeBuffer();
        }
        return instance;
    }

    final SuperRenderTypeBufferPhase earlyBuffer;
    final SuperRenderTypeBufferPhase defaultBuffer;
    final SuperRenderTypeBufferPhase lateBuffer;

    public SuperRenderTypeBuffer() {
        earlyBuffer = new SuperRenderTypeBufferPhase();
        defaultBuffer = new SuperRenderTypeBufferPhase();
        lateBuffer = new SuperRenderTypeBufferPhase();
    }

    public IVertexBuilder getEarlyBuffer(RenderType type) {
        return earlyBuffer.getBuffer(type);
    }

    @Override
    public IVertexBuilder getBuffer(RenderType type) {
        return defaultBuffer.getBuffer(type);
    }

    public IVertexBuilder getLateBuffer(RenderType type) {
        return lateBuffer.getBuffer(type);
    }

    public void draw() {
        earlyBuffer.endBatch();
        defaultBuffer.endBatch();
        lateBuffer.endBatch();
    }

    private static class SuperRenderTypeBufferPhase extends Impl {

        // Visible clones from net.minecraft.client.renderer.RenderTypeBuffers
        static final RegionRenderCacheBuilder blockBuilders = new RegionRenderCacheBuilder();

        static SortedMap<RenderType, BufferBuilder> createEntityBuilders() {
            return Util.make(new Object2ObjectLinkedOpenHashMap<>(), (map) -> {
                map.put(Atlases.solidBlockSheet(), blockBuilders.builder(RenderType.solid()));
                assign(map, RenderTypes.getOutlineSolid());
                map.put(Atlases.cutoutBlockSheet(), blockBuilders.builder(RenderType.cutout()));
                map.put(Atlases.bannerSheet(), blockBuilders.builder(RenderType.cutoutMipped()));
                map.put(Atlases.translucentCullBlockSheet(), blockBuilders.builder(RenderType.translucent()));
                assign(map, Atlases.shieldSheet());
                assign(map, Atlases.bedSheet());
                assign(map, Atlases.shulkerBoxSheet());
                assign(map, Atlases.signSheet());
                assign(map, Atlases.chestSheet());
                assign(map, RenderType.translucentNoCrumbling());
                assign(map, RenderType.glint());
                assign(map, RenderType.entityGlint());
                assign(map, RenderType.waterMask());
                ModelBakery.DESTROY_TYPES.forEach((p_228488_1_) -> assign(map, p_228488_1_));
            });
        }

        private static void assign(Object2ObjectLinkedOpenHashMap<RenderType, BufferBuilder> map, RenderType type) {
            map.put(type, new BufferBuilder(type.bufferSize()));
        }

        protected SuperRenderTypeBufferPhase() {
            super(new BufferBuilder(256), createEntityBuilders());
        }

    }

}
