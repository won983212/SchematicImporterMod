package com.won983212.schemimporter;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

public class ModKeys {
    public static final KeyBinding KEY_TOOL_MENU;
    public static final KeyBinding KEY_ACTIVATE_TOOL;

    public static void registerKeys() {
    }

    static {
        KEY_TOOL_MENU = new KeyBinding("key.toolmenu.desc", KeyConflictContext.IN_GAME, KeyModifier.NONE,
                InputMappings.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, "key." + SchematicImporterMod.MODID + ".category");
        KEY_ACTIVATE_TOOL = new KeyBinding("key.activatetool.desc", KeyConflictContext.IN_GAME, KeyModifier.NONE,
                InputMappings.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_CONTROL, "key." + SchematicImporterMod.MODID + ".category");
    }
}