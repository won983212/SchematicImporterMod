package com.won983212.schemimporter.client.tools;

import com.won983212.schemimporter.SchematicImporterMod;
import com.won983212.schemimporter.client.SchematicTransformation;
import com.won983212.schemimporter.utility.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.math.vector.Vector3d;

public class MoveTool extends PlacementToolBase {

    public void init() {
        super.init();
        renderSelectedFace = true;
    }

    @Override
    public boolean handleMouseWheel(double delta) {
        if (!schematicSelected || !selectedFace.getAxis().isHorizontal()) {
            return true;
        }

        int scale = -1;
        float pt = SchematicImporterMod.getPartialTicks();
        SchematicTransformation transformation = schematicHandler.getTransformation();
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player != null) {
            if (player.getViewVector(pt).dot(Vector3d.atLowerCornerOf(selectedFace.getNormal())) < 0) {
                scale = 1;
            }
        }

        Vector3d vec = Vector3d.atLowerCornerOf(selectedFace.getNormal()).scale(-Math.signum(delta) * scale);
        vec = vec.multiply(transformation.getMirrorModifier(Axis.X), 1, transformation.getMirrorModifier(Axis.Z));
        vec = VecHelper.rotate(vec, transformation.getRotationTarget(), Axis.Y);

        transformation.move((float) vec.x, 0, (float) vec.z);
        schematicHandler.markDirty();
        return true;
    }

}
