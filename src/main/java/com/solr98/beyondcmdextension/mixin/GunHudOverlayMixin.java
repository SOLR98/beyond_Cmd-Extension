package com.solr98.beyondcmdextension.mixin;

import com.solr98.beyondcmdextension.client.AmmoCountCache;
import com.solr98.beyondcmdextension.handler.NetworkAmmoExtractor;
import com.tacz.guns.client.gui.overlay.GunHudOverlay;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GunHudOverlay.class, remap = false)
public class GunHudOverlayMixin {

    @Shadow
    private static int cacheInventoryAmmoCount;

    @Inject(method = "handleInventoryAmmo", at = @At("RETURN"))
    private static void onHandleInventoryAmmo(ItemStack stack, Inventory inventory, CallbackInfo ci) {
        if (cacheInventoryAmmoCount >= 9999) return;

        ResourceLocation ammoId = NetworkAmmoExtractor.getAmmoIdClient(stack);
        if (ammoId == null) return;

        int networkAmmo = AmmoCountCache.getCount(ammoId);
        if (networkAmmo > 0) {
            cacheInventoryAmmoCount = Math.min(9999, cacheInventoryAmmoCount + networkAmmo);
        }

        AmmoCountCache.requestAndGet(ammoId);
    }
}
