package com.won983212.schemimporter.block;

import com.won983212.schemimporter.SchematicImporterMod;
import net.minecraft.block.Block;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SchematicImporterMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModBlocks {
    public static final BlockIndustrialAlarm blockIndustrialAlarm =
            (BlockIndustrialAlarm) (new BlockIndustrialAlarm().setRegistryName(SchematicImporterMod.MODID, "block_industrial_alarm"));

    @SubscribeEvent
    public static void onBlocksRegistration(final RegistryEvent.Register<Block> blockRegisterEvent) {
        blockRegisterEvent.getRegistry().register(blockIndustrialAlarm);
    }
}
