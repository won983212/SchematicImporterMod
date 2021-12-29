package com.won983212.schemimporter;

import com.won983212.schemimporter.client.ClientMod;
import com.won983212.schemimporter.network.NetworkDispatcher;
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
}
