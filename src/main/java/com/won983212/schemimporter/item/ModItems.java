package com.won983212.schemimporter.item;

import com.won983212.schemimporter.SchematicImporterMod;
import com.won983212.schemimporter.block.ModBlocks;
import com.won983212.schemimporter.client.render.item.RenderIndustrialAlarmItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SchematicImporterMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModItems {
    public static final BlockItem itemIndustrialAlarm = (BlockItem) new BlockItem(ModBlocks.blockIndustrialAlarm, new Item.Properties()
            .setISTER(() -> RenderIndustrialAlarmItem::new)
            .stacksTo(64)
            .tab(ItemGroup.TAB_DECORATIONS))
            .setRegistryName("block_industrial_alarm");

    public static final SchematicItem itemSchematic = (SchematicItem) new SchematicItem(new Item.Properties()
            .stacksTo(1)
            .tab(ItemGroup.TAB_TOOLS))
            .setRegistryName("schematic");

    @SubscribeEvent
    public static void onItemsRegistration(final RegistryEvent.Register<Item> itemRegisterEvent) {
        itemRegisterEvent.getRegistry().registerAll(itemIndustrialAlarm, itemSchematic);
    }
}