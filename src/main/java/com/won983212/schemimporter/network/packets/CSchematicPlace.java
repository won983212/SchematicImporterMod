package com.won983212.schemimporter.network.packets;

import com.won983212.schemimporter.CommonMod;
import com.won983212.schemimporter.Logger;
import com.won983212.schemimporter.SchematicImporterMod;
import com.won983212.schemimporter.network.IMessage;
import com.won983212.schemimporter.schematic.SchematicPrinter;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkEvent.Context;

import java.util.function.Supplier;

// TODO 이미 있는 파일 upload하면 error
public class CSchematicPlace implements IMessage {
    public final ItemStack stack;

    public CSchematicPlace(ItemStack stack) {
        this.stack = stack;
    }

    public CSchematicPlace(PacketBuffer buffer) {
        stack = buffer.readItem();
    }

    public void write(PacketBuffer buffer) {
        buffer.writeItem(stack);
    }

    public void handle(Supplier<Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayerEntity player = context.get().getSender();
            if (player == null) {
                return;
            }

            if (!player.canUseGameMasterBlocks()) {
                player.sendMessage(new TranslationTextComponent("message.adminonly")
                        .withStyle(TextFormatting.RED), player.getUUID());
            }

            boolean includeAir = false;
            String name = "unknown";
            if (stack.hasTag()) {
                includeAir = stack.getTag().getBoolean("IncludeAir");
                name = stack.getTag().getString("File");
            }

            final int[] percentIndex = {0};
            World world = player.getLevel();
            try {
                SchematicPrinter printer = SchematicPrinter.newPlacingSchematicTask(stack, world, (s, p) -> {
                    int percent = (int) Math.floor(p * 100);
                    if (percent >= percentIndex[0] * 10) {
                        percentIndex[0]++;
                        Logger.debug(s + ": " + percent + "%");
                        sendSchematicMessage(player, percent + "%");
                        if (percent == 100) {
                            sendSchematicMessageTranslate(player, "message.placecomplete");
                        }
                    }
                }).includeAir(includeAir).maxBatchPlacing(10000);

                CommonMod.SERVER_SCHEDULER.addAsyncTask(printer)
                        .name("print/" + name + "/" + player.getGameProfile().getName())
                        .exceptionally((e) -> handleException(player, e));
                sendSchematicMessageTranslate(player, "message.placestart");
            } catch (IllegalArgumentException e) {
                handleException(player, e);
            }
        });
        context.get().setPacketHandled(true);
    }

    private static void handleException(ServerPlayerEntity player, Exception e) {
        Logger.error(e);
        sendSchematicMessageTranslate(player, "message.exception");
    }

    private static void sendSchematicMessageTranslate(ServerPlayerEntity player, String messageId) {
        ITextComponent text = SchematicImporterMod.translate(messageId);
        text = new StringTextComponent(TextFormatting.GOLD + "[Schematic] " + TextFormatting.RESET).append(text);
        Logger.info(text.getString());
        player.sendMessage(text, player.getUUID());
    }

    private static void sendSchematicMessage(ServerPlayerEntity player, String message) {
        player.sendMessage(new StringTextComponent(TextFormatting.GOLD + "[Schematic] " + TextFormatting.RESET + message), player.getUUID());
    }
}
