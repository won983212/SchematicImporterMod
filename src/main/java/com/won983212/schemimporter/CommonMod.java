package com.won983212.schemimporter;

import com.mojang.brigadier.CommandDispatcher;
import com.won983212.schemimporter.network.loader.ServerSchematicLoader;
import com.won983212.schemimporter.server.ModCommands;
import com.won983212.schemimporter.task.TaskScheduler;
import net.minecraft.command.CommandSource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;

@Mod.EventBusSubscriber
public class CommonMod {
    public static final ServerSchematicLoader SERVER_SCHEMATIC_LOADER = new ServerSchematicLoader();
    public static final TaskScheduler SERVER_SCHEDULER = new TaskScheduler();

    public void onCommonSetup(FMLCommonSetupEvent event) {
        SERVER_SCHEMATIC_LOADER.tick();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            return;
        }

        CommonMod.SERVER_SCHEMATIC_LOADER.tick();
        CommonMod.SERVER_SCHEDULER.tick();
    }

    @SubscribeEvent
    public static void onRegisterCommand(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSource> dispater = e.getDispatcher();
        for (ModCommands command : ModCommands.values()) {
            command.reigster(dispater);
        }
    }

    @SubscribeEvent
    public static void serverStopped(FMLServerStoppingEvent event) {
        CommonMod.SERVER_SCHEMATIC_LOADER.shutdown();
    }

}
