package com.won983212.schemimporter.client;

import com.won983212.schemimporter.SchematicImporterMod;
import net.minecraft.util.ResourceLocation;

public class ResourceUtil {
    public static ResourceLocation getResource(String path) {
        return new ResourceLocation(SchematicImporterMod.MODID + ":" + path);
    }
}
