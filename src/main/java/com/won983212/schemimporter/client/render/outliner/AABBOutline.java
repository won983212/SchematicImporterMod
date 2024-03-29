package com.won983212.schemimporter.client.render.outliner;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.won983212.schemimporter.client.render.RenderTypes;
import com.won983212.schemimporter.client.render.SuperRenderTypeBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;

public class AABBOutline extends Outline {

    private AxisAlignedBB bb;
    private BlockPos anchor;

    public AABBOutline(AxisAlignedBB bb) {
        this.setBounds(bb);
        this.anchor = BlockPos.ZERO;
    }

    @Override
    public void render(MatrixStack ms, SuperRenderTypeBuffer buffer, float pt) {
        Vector3d projectedView = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        boolean noCull = bb.move(anchor).contains(projectedView);

        AxisAlignedBB bounds = bb.inflate(noCull ? -1 / 128d : 1 / 128d);
        noCull |= params.disableCull;

        Vector3d xyz = new Vector3d(bounds.minX, bounds.minY, bounds.minZ);
        Vector3d Xyz = new Vector3d(bounds.maxX, bounds.minY, bounds.minZ);
        Vector3d xYz = new Vector3d(bounds.minX, bounds.maxY, bounds.minZ);
        Vector3d XYz = new Vector3d(bounds.maxX, bounds.maxY, bounds.minZ);
        Vector3d xyZ = new Vector3d(bounds.minX, bounds.minY, bounds.maxZ);
        Vector3d XyZ = new Vector3d(bounds.maxX, bounds.minY, bounds.maxZ);
        Vector3d xYZ = new Vector3d(bounds.minX, bounds.maxY, bounds.maxZ);
        Vector3d XYZ = new Vector3d(bounds.maxX, bounds.maxY, bounds.maxZ);

        Vector3d start = xyz;
        renderAACuboidLine(ms, buffer, start, Xyz);
        renderAACuboidLine(ms, buffer, start, xYz);
        renderAACuboidLine(ms, buffer, start, xyZ);

        start = XyZ;
        renderAACuboidLine(ms, buffer, start, xyZ);
        renderAACuboidLine(ms, buffer, start, XYZ);
        renderAACuboidLine(ms, buffer, start, Xyz);

        start = XYz;
        renderAACuboidLine(ms, buffer, start, xYz);
        renderAACuboidLine(ms, buffer, start, Xyz);
        renderAACuboidLine(ms, buffer, start, XYZ);

        start = xYZ;
        renderAACuboidLine(ms, buffer, start, XYZ);
        renderAACuboidLine(ms, buffer, start, xyZ);
        renderAACuboidLine(ms, buffer, start, xYz);

        renderFace(ms, buffer, Direction.NORTH, xYz, XYz, Xyz, xyz, noCull);
        renderFace(ms, buffer, Direction.SOUTH, XYZ, xYZ, xyZ, XyZ, noCull);
        renderFace(ms, buffer, Direction.WEST, xYZ, xYz, xyz, xyZ, noCull);
        renderFace(ms, buffer, Direction.EAST, XYz, XYZ, XyZ, Xyz, noCull);
        renderFace(ms, buffer, Direction.UP, xYZ, XYZ, XYz, xYz, noCull);
        renderFace(ms, buffer, Direction.DOWN, xyz, Xyz, XyZ, xyZ, noCull);
    }

    protected void renderFace(MatrixStack ms, SuperRenderTypeBuffer buffer, Direction direction, Vector3d p1, Vector3d p2,
                              Vector3d p3, Vector3d p4, boolean noCull) {
        if (!params.faceTexture.isPresent()) {
            return;
        }

        ResourceLocation faceTexture = params.faceTexture.get().getLocation();
        float alphaBefore = params.alpha;
        params.alpha = (direction == params.getHighlightedFace() && params.hightlightedFaceTexture.isPresent()) ? 1 : 0.5f;

        RenderType translucentType = RenderTypes.getOutlineTranslucent(faceTexture, !noCull);
        IVertexBuilder builder = buffer.getLateBuffer(translucentType);

        Axis axis = direction.getAxis();
        Vector3d uDiff = p2.subtract(p1);
        Vector3d vDiff = p4.subtract(p1);
        float maxU = (float) Math.abs(axis == Axis.X ? uDiff.z : uDiff.x);
        float maxV = (float) Math.abs(axis == Axis.Y ? vDiff.z : vDiff.y);
        putQuadUV(ms, builder, p1, p2, p3, p4, 0, 0, maxU, maxV, Direction.UP);
        params.alpha = alphaBefore;
    }

    public void setBounds(AxisAlignedBB bb) {
        this.bb = bb;
    }

    public void setAnchor(BlockPos anchor) {
        this.anchor = anchor;
    }

}
