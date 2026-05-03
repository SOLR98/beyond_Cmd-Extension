package com.solr98.beyondcmdextension.mixin;

import com.solr98.beyondcmdextension.client.AmmoCountCache;
import com.solr98.beyondcmdextension.handler.NetworkAmmoExtractor;
import com.tacz.guns.client.animation.statemachine.GunAnimationStateContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GunAnimationStateContext.class, remap = false)
public class GunAnimationStateContextMixin {

    @Shadow
    private ItemStack currentGunItem;

    @Inject(method = "hasAmmoToConsume", at = @At("RETURN"), cancellable = true)
    private void onHasAmmoToConsume(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        ResourceLocation ammoId = NetworkAmmoExtractor.getAmmoIdClient(currentGunItem);
        if (ammoId == null) return;
        if (AmmoCountCache.getCount(ammoId) > 0) {
            cir.setReturnValue(true);
        }
    }
}
