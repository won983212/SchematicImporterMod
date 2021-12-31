package com.won983212.schemimporter;

import com.won983212.schemimporter.client.ClientMod;
import com.won983212.schemimporter.network.NetworkDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.LanguageMap;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SchematicImporterMod.MODID)
public class SchematicImporterMod {
    public static final String MODID = "schemimporter";
    private final CommonMod proxy;

    public SchematicImporterMod() {
        proxy = DistExecutor.safeRunForDist(() -> ClientMod::new, () -> com.won983212.schemimporter.server.ServerMod::new);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        NetworkDispatcher.initDispatcher();
        proxy.onCommonSetup(event);
    }

    public static float getPartialTicks() {
        return Minecraft.getInstance().getFrameTime();
    }

    public static TranslationTextComponent translate(String key, Object... args) {
        return new TranslationTextComponent(MODID + "." + key, args);
    }

    public static String translateAsString(String key, Object... args) {
        return String.format(LanguageMap.getInstance().getOrDefault(MODID + "." + key), args);
    }

    public static ResourceLocation getResource(String path) {
        return new ResourceLocation(SchematicImporterMod.MODID, path);
    }
}
