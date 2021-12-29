package com.won983212.schemimporter;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.glfw.GLFW;

public class ModKeys {
    public static final KeyBinding KEY_CLEAR_CACHE;
    public static final KeyBinding KEY_TOOL_MENU;
    public static final KeyBinding KEY_ACTIVATE_TOOL;

    public static void registerKeys() {
        ClientRegistry.registerKeyBinding(KEY_CLEAR_CACHE);
    }

    static {
        KEY_CLEAR_CACHE = new KeyBinding("key.clearcache.desc", KeyConflictContext.IN_GAME, KeyModifier.CONTROL,
                InputMappings.Type.KEYSYM, GLFW.GLFW_KEY_R, "key." + SchematicImporterMod.MODID + ".category");
        KEY_TOOL_MENU = new KeyBinding("key.toolmenu.desc", KeyConflictContext.IN_GAME, KeyModifier.NONE,
                InputMappings.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, "key." + SchematicImporterMod.MODID + ".category");
        KEY_ACTIVATE_TOOL = new KeyBinding("key.activatetool.desc", KeyConflictContext.IN_GAME, KeyModifier.NONE,
                InputMappings.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_CONTROL, "key." + SchematicImporterMod.MODID + ".category");
    }
}