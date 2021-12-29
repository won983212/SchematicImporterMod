package com.won983212.schemimporter.mixin;

import com.won983212.schemimporter.LegacyMapper;
import net.minecraft.util.datafix.fixes.BlockStateFlatteningMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockStateFlatteningMap.class)
public class MixinBlockStateFlattening {
    @Inject(method = "register(ILjava/lang/String;[Ljava/lang/String;)V", at = @At("HEAD"))
    private static void onAddEntry(int idMeta, String fixedNBT, String[] sourceNBTs, CallbackInfo ci) {
        LegacyMapper.registerBlockState(idMeta, fixedNBT, sourceNBTs);
    }
}
