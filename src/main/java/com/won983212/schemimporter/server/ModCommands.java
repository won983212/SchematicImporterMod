package com.won983212.schemimporter.server;

import com.mojang.brigadier.CommandDispatcher;
import com.won983212.schemimporter.SchematicImporterMod;
import com.won983212.schemimporter.server.command.SchematicCommand;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;

import java.util.function.Consumer;

public enum ModCommands {
    OPEN_SCHEMATIC_GUI(SchematicCommand::register);

    private final Consumer<CommandDispatcher<CommandSource>> registerFunc;

    ModCommands(Consumer<CommandDispatcher<CommandSource>> registerFunc) {
        this.registerFunc = registerFunc;
    }

    public void reigster(CommandDispatcher<CommandSource> dispatcher) {
        this.registerFunc.accept(dispatcher);
    }

    public static ServerPlayerEntity getServerPlayer(CommandSource source) {
        Entity e = source.getEntity();
        if (!(e instanceof ServerPlayerEntity)) {
            source.sendFailure(SchematicImporterMod.translate("command.error.cantexecuteentity"));
            return null;
        }
        return (ServerPlayerEntity) e;
    }
}
