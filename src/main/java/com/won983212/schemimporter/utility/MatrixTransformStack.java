package com.won983212.schemimporter.utility;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.math.vector.Vector3i;

public class MatrixTransformStack {

    private final MatrixStack internal;

    public MatrixTransformStack(MatrixStack internal) {
        this.internal = internal;
    }

    public static MatrixTransformStack of(MatrixStack ms) {
        return new MatrixTransformStack(ms);
    }

    public MatrixTransformStack translate(double x, double y, double z) {
        internal.translate(x, y, z);
        return this;
    }

    public MatrixTransformStack multiply(Quaternion quaternion) {
        internal.mulPose(quaternion);
        return this;
    }

    public void rotateX(double angle) {
        multiply(Vector3f.XP, angle);
    }

    public MatrixTransformStack rotateY(double angle) {
        return multiply(Vector3f.YP, angle);
    }

    public MatrixTransformStack rotateZ(double angle) {
        return multiply(Vector3f.ZP, angle);
    }

    public void translate(Vector3i vec) {
        translate(vec.getX(), vec.getY(), vec.getZ());
    }

    public MatrixTransformStack translate(Vector3d vec) {
        return translate(vec.x, vec.y, vec.z);
    }

    public MatrixTransformStack translateBack(Vector3d vec) {
        return translate(-vec.x, -vec.y, -vec.z);
    }

    public MatrixTransformStack multiply(Vector3f axis, double angle) {
        if (angle == 0) {
            return this;
        }
        return multiply(axis.rotationDegrees((float) angle));
    }
}