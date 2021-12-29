package com.won983212.schemimporter.server;

import com.mojang.brigadier.CommandDispatcher;
import com.won983212.schemimporter.server.command.SchematicCommand;
import net.minecraft.command.CommandSource;

import java.util.function.Consumer;

public enum Commands {
    OPEN_SCHEMATIC_GUI(SchematicCommand::register);

    private final Consumer<CommandDispatcher<CommandSource>> registerFunc;

    Commands(Consumer<CommandDispatcher<CommandSource>> registerFunc) {
        this.registerFunc = registerFunc;
    }

    public void reigster(CommandDispatcher<CommandSource> dispatcher) {
        this.registerFunc.accept(dispatcher);
    }
}
