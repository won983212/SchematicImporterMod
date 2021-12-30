package com.won983212.schemimporter.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.won983212.schemimporter.CommonMod;
import com.won983212.schemimporter.SchematicImporterMod;
import com.won983212.schemimporter.network.NetworkDispatcher;
import com.won983212.schemimporter.network.packets.SOpenSchematicMenu;
import com.won983212.schemimporter.schematic.SchematicFile;
import com.won983212.schemimporter.server.ModCommands;
import com.won983212.schemimporter.task.QueuedAsyncTask;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.List;

public class SchematicCommand {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> schematicCommand
                = Commands.literal("scm")
                .requires((source) -> source.hasPermission(2))
                .executes(SchematicCommand::openSchematicDialog)
                .then(Commands.literal("task")
                        .then(Commands.literal("clear").executes(SchematicCommand::clearSchematicTasks))
                        .then(Commands.literal("list").executes(SchematicCommand::listSchematicTasks)));
        dispatcher.register(schematicCommand);
    }

    private static int openSchematicDialog(CommandContext<CommandSource> context) {
        ServerPlayerEntity player = ModCommands.getServerPlayer(context.getSource());
        if (player == null) {
            return 0;
        }

        SOpenSchematicMenu packet = new SOpenSchematicMenu(SchematicFile.getFileList(player.getGameProfile().getName()));
        NetworkDispatcher.send(PacketDistributor.PLAYER.with(() -> player), packet);
        return 1;
    }

    private static int clearSchematicTasks(CommandContext<CommandSource> context) {
        CommonMod.SERVER_SCHEDULER.cancelAllTask();
        context.getSource().sendSuccess(SchematicImporterMod.translate("message.clearedalltasks"), true);
        return 1;
    }

    private static int listSchematicTasks(CommandContext<CommandSource> context) {
        int i = 0;
        List<QueuedAsyncTask<?>> tasks = CommonMod.SERVER_SCHEDULER.getActiveTasks();
        if (tasks.isEmpty()) {
            context.getSource().sendSuccess(SchematicImporterMod.translate("message.notasks"), true);
            return 1;
        }
        context.getSource().sendSuccess(new StringTextComponent("===== Server Side Tasks ====="), true);
        for (QueuedAsyncTask<?> task : tasks) {
            String colorCode = task.isActive() ? TextFormatting.GOLD.toString() : TextFormatting.DARK_GRAY.toString();
            String message = String.format(TextFormatting.GRAY + "[%d] %s%s §r- %.2fs", i++, colorCode, task.getName(), task.getRunningTime() / 1000.0);
            StringTextComponent messageComp = new StringTextComponent(message);
            context.getSource().sendSuccess(messageComp, true);
        }
        return 1;
    }
}
