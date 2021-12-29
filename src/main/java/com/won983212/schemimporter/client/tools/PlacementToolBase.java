package com.won983212.schemimporter.client.tools;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.won983212.schemimporter.client.render.SuperRenderTypeBuffer;
import net.minecraft.client.renderer.IRenderTypeBuffer;

public abstract class PlacementToolBase extends SchematicToolBase {

    @Override
    public void renderTool(MatrixStack ms, SuperRenderTypeBuffer buffer) {
        super.renderTool(ms, buffer);
    }

    @Override
    public void renderOverlay(MatrixStack ms, IRenderTypeBuffer buffer) {
        super.renderOverlay(ms, buffer);
    }

    @Override
    public boolean handleMouseWheel(double delta) {
        return false;
    }

    @Override
    public boolean handleRightClick() {
        return false;
    }

}
