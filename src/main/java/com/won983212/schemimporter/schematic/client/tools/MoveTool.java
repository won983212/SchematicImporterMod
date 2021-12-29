package com.won983212.schemimporter.schematic.client.tools;

import com.won983212.schemimporter.schematic.client.SchematicTransformation;
import com.won983212.schemimporter.utility.VecHelper;
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

        SchematicTransformation transformation = schematicHandler.getTransformation();
        Vector3d vec = Vector3d.atLowerCornerOf(selectedFace.getNormal()).scale(-Math.signum(delta));
        vec = vec.multiply(transformation.getMirrorModifier(Axis.X), 1, transformation.getMirrorModifier(Axis.Z));
        vec = VecHelper.rotate(vec, transformation.getRotationTarget(), Axis.Y);

        transformation.move((float) vec.x, 0, (float) vec.z);
        schematicHandler.markDirty();
        return true;
    }

}
