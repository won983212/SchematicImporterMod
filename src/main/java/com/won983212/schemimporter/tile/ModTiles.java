package com.won983212.schemimporter.tile;

import com.won983212.schemimporter.SchematicImporterMod;
import com.won983212.schemimporter.block.ModBlocks;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SchematicImporterMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModTiles {
    public static final TileEntityType<TileEntityIndustrialAlarm> tileEntityIndustrialAlarm =
            TileEntityType.Builder.of(TileEntityIndustrialAlarm::new, ModBlocks.blockIndustrialAlarm).build(null);

    @SubscribeEvent
    public static void onTileEntityTypeRegistration(final RegistryEvent.Register<TileEntityType<?>> event) {
        tileEntityIndustrialAlarm.setRegistryName(SchematicImporterMod.MODID + ":tile_entity_type_industrial_alarm");
        event.getRegistry().register(tileEntityIndustrialAlarm);
    }
}
