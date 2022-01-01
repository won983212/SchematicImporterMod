package com.won983212.schemimporter;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public enum ModTextures {
    BLANK("blank.png", 0, 0, 16, 16),
    CHECKERED("checkerboard.png", 0, 0, 16, 16),
    OVERLAY("overlay.png", 0, 0, 16, 16),
    HIGHLIGHT_CHECKERED("highlighted_checkerboard.png", 0, 0, 16, 16),
    SCHEMATIC_SELECT_BACKGROUND("schematics.png", 0, 0, 206, 137);

    private final ResourceLocation location;
    public final int width, height;
    public final int startX, startY;

    ModTextures(String filename, int x, int y, int width, int height) {
        this.location = SchematicImporterMod.getResource("textures/gui/" + filename);
        this.startX = x;
        this.startY = y;
        this.width = width;
        this.height = height;
    }

    @OnlyIn(Dist.CLIENT)
    public void bind() {
        Minecraft.getInstance()
                .getTextureManager()
                .bind(location);
    }

    @OnlyIn(Dist.CLIENT)
    public void draw(MatrixStack matrixStack, int x, int y, int w, int h) {
        bind();
        AbstractGui.blit(matrixStack, x, y, startX, startY, w, h, width, height);
    }

    @OnlyIn(Dist.CLIENT)
    public void draw(MatrixStack matrixStack, Screen screen, int x, int y) {
        bind();
        screen.blit(matrixStack, x, y, startX, startY, width, height);
    }

    public ResourceLocation getLocation() {
        return location;
    }

}