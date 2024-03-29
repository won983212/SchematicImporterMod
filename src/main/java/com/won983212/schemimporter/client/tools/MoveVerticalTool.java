package com.won983212.schemimporter.client.tools;

public class MoveVerticalTool extends PlacementToolBase {

    @Override
    public boolean handleMouseWheel(double delta) {
        if (schematicHandler.isDeployed()) {
            schematicHandler.getTransformation().move(0, (float) Math.signum(delta), 0);
            schematicHandler.markDirty();
        }
        return true;
    }

}
