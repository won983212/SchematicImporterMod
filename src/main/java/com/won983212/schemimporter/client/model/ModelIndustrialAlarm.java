package com.won983212.schemimporter.client.model;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.won983212.schemimporter.client.ResourceUtil;
import com.won983212.schemimporter.client.render.ModRenderType;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.Model;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

public class ModelIndustrialAlarm extends Model {
    private static final int FULL_LIGHT = 0xF000F0;
    private static final ResourceLocation TEXTURE = ResourceUtil.getResource("render/industrial_alarm.png");
    private static final ResourceLocation TEXTURE_ACTIVE = ResourceUtil.getResource("render/industrial_alarm_active.png");
    private final RenderType RENDER_TYPE = ModRenderType.standard(TEXTURE);
    private final RenderType RENDER_TYPE_ACTIVE = ModRenderType.standard(TEXTURE_ACTIVE);

    private final ModelRenderer base;
    private final ModelRenderer bulb;
    private final ModelRenderer light_box;
    private final ModelRenderer aura;

    public ModelIndustrialAlarm() {
        super(RenderType::entitySolid);
        texWidth = 64;
        texHeight = 64;

        base = new ModelRenderer(this, 0, 9);
        base.addBox(-3F, 0F, -3F, 6, 1, 6);
        base.setPos(0F, 0F, 0F);
        base.setTexSize(64, 64);
        setRotation(base, 0F, 0F, 0F);
        bulb = new ModelRenderer(this, 16, 0);
        bulb.addBox(-1F, 1F, -1F, 2, 3, 2);
        bulb.setPos(0F, 0F, 0F);
        bulb.setTexSize(64, 64);
        setRotation(bulb, 0F, 0F, 0F);
        light_box = new ModelRenderer(this, 0, 0);
        light_box.addBox(-2F, 1F, -2F, 4, 4, 4);
        light_box.setPos(0F, 0F, 0F);
        light_box.setTexSize(64, 64);
        setRotation(light_box, 0F, 0F, 0F);
        aura = new ModelRenderer(this, 0, 16);
        aura.addBox(-6F, 2F, -1F, 12, 1, 2);
        aura.setPos(0F, 0F, 0F);
        aura.setTexSize(64, 64);
        setRotation(aura, 0F, 0F, 0F);
    }

    private IVertexBuilder getVertexBuilder(@Nonnull IRenderTypeBuffer renderer, @Nonnull RenderType renderType, boolean hasEffect) {
        return ItemRenderer.getFoilBufferDirect(renderer, renderType, false, hasEffect);
    }

    private void setRotation(ModelRenderer model, float x, float y, float z) {
        model.xRot = x;
        model.yRot = y;
        model.zRot = z;
    }

    public void render(@Nonnull MatrixStack matrix, @Nonnull IRenderTypeBuffer renderer, int light, int overlayLight, boolean active, float rotation, boolean renderBase,
                       boolean hasEffect) {
        render(matrix, getVertexBuilder(renderer, active ? RENDER_TYPE_ACTIVE : RENDER_TYPE, hasEffect), light, overlayLight, 1, 1, 1, 1,
                active, rotation, renderBase);
    }

    @Override
    public void renderToBuffer(@Nonnull MatrixStack matrix, @Nonnull IVertexBuilder vertexBuilder, int light, int overlayLight, float red, float green, float blue,
                               float alpha) {
        render(matrix, vertexBuilder, light, overlayLight, red, green, blue, alpha, false, 0, false);
    }

    private void render(@Nonnull MatrixStack matrix, @Nonnull IVertexBuilder vertexBuilder, int light, int overlayLight, float red, float green, float blue, float alpha,
                        boolean active, float rotation, boolean renderBase) {
        if (renderBase) {
            base.render(matrix, vertexBuilder, light, overlayLight, red, green, blue, alpha);
        }
        if (active) {
            setRotation(aura, 0, (float) Math.toRadians(rotation), 0);
            setRotation(bulb, 0, (float) Math.toRadians(rotation), 0);
        } else {
            setRotation(aura, 0, 0, 0);
            setRotation(bulb, 0, 0, 0);
        }
        float bulbAlpha = 0.3F + (Math.abs(((rotation * 2) % 360) - 180F) / 180F) * 0.7F;
        bulb.render(matrix, vertexBuilder, active ? FULL_LIGHT : light, overlayLight, red, green, blue, bulbAlpha);
        light_box.render(matrix, vertexBuilder, active ? FULL_LIGHT : light, overlayLight, red, green, blue, alpha);
        if (!renderBase) {
            aura.render(matrix, vertexBuilder, FULL_LIGHT, overlayLight, red, green, blue, bulbAlpha);
        }
    }
}