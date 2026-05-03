package com.solr98.beyondcmdextension.mixin;

import com.solr98.beyondcmdextension.client.AmmoCountCache;
import com.solr98.beyondcmdextension.handler.NetworkAmmoExtractor;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

@Mixin(value = AbstractGunItem.class, remap = false)
public abstract class AbstractGunItemMixin {

    private static final Field CURIOS_PLAYER_FIELD;

    static {
        Field f = null;
        if (ModList.get().isLoaded("curios_for_ammo_box")) {
            try {
                Class<?> clazz = Class.forName("com.nanaios.curios_for_ammo_box.util.ItemHandlerWithCurios");
                f = clazz.getDeclaredField("player");
                if (!Player.class.isAssignableFrom(f.getType())) f = null;
                else f.setAccessible(true);
            } catch (Exception ignored) {}
        }
        CURIOS_PLAYER_FIELD = f;
    }

    @Inject(method = "canReload", at = @At("RETURN"), cancellable = true)
    private void onCanReload(LivingEntity shooter, ItemStack gunItem, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null || iGun.useInventoryAmmo(gunItem)) return;

        if (shooter instanceof ServerPlayer player) {
            if (NetworkAmmoExtractor.countAmmoInNetwork(gunItem, player) > 0) {
                cir.setReturnValue(true);
            }
        } else if (shooter.level().isClientSide) {
            ResourceLocation ammoId = NetworkAmmoExtractor.getAmmoIdClient(gunItem);
            if (ammoId != null) {
                AmmoCountCache.requestAndGet(ammoId);
                if (!AmmoCountCache.hasData(ammoId) || AmmoCountCache.getCount(ammoId) > 0) {
                    cir.setReturnValue(true);
                }
            }
        }
    }

    @Inject(method = "hasInventoryAmmo", at = @At("RETURN"), cancellable = true)
    private void onHasInventoryAmmo(LivingEntity shooter, ItemStack gun, boolean needCheckAmmo, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        IGun iGun = IGun.getIGunOrNull(gun);
        if (iGun == null || !iGun.useInventoryAmmo(gun)) return;

        if (shooter instanceof ServerPlayer player) {
            if (NetworkAmmoExtractor.countAmmoInNetwork(gun, player) > 0) {
                cir.setReturnValue(true);
            }
        } else if (shooter.level().isClientSide) {
            ResourceLocation ammoId = NetworkAmmoExtractor.getAmmoIdClient(gun);
            if (ammoId != null) {
                AmmoCountCache.requestAndGet(ammoId);
                if (!AmmoCountCache.hasData(ammoId) || AmmoCountCache.getCount(ammoId) > 0) {
                    cir.setReturnValue(true);
                }
            }
        }
    }

    @Inject(method = "findAndExtractInventoryAmmo", at = @At("RETURN"), cancellable = true)
    private void onFindAndExtract(IItemHandler itemHandler, ItemStack gunItem, int needAmmoCount, CallbackInfoReturnable<Integer> cir) {
        int found = cir.getReturnValue();
        if (found >= needAmmoCount) return;
        ServerPlayer player = getPlayer(itemHandler);
        if (player == null) return;
        int stillNeed = needAmmoCount - found;
        int fromNet = NetworkAmmoExtractor.consumeAmmoDirectly(gunItem, stillNeed, player);
        if (fromNet > 0) {
            cir.setReturnValue(found + fromNet);
        }
    }

    private static ServerPlayer getPlayer(IItemHandler handler) {
        if (handler instanceof PlayerMainInvWrapper w) {
            var p = w.getInventoryPlayer().player;
            return p instanceof ServerPlayer sp ? sp : null;
        }
        if (handler instanceof CombinedInvWrapper) {
            try {
                var field = CombinedInvWrapper.class.getDeclaredField("itemHandler");
                field.setAccessible(true);
                var arr = (IItemHandlerModifiable[]) field.get(handler);
                for (var sub : arr) {
                    var result = getPlayer(sub);
                    if (result != null) return result;
                }
            } catch (Exception ignored) {}
        }
        Field f = CURIOS_PLAYER_FIELD;
        if (f != null) {
            try {
                var p = f.get(handler);
                return p instanceof ServerPlayer sp ? sp : null;
            } catch (Exception ignored) {}
        }
        return null;
    }
}
