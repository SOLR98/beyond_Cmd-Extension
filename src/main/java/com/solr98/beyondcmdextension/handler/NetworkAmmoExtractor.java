package com.solr98.beyondcmdextension.handler;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.init.ModItems;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class NetworkAmmoExtractor {

    public static int consumeAmmoDirectly(ItemStack gunStack, int neededAmount, ServerPlayer player) {
        DimensionsNet net = DimensionsNet.getPrimaryNetFromPlayer(player);
        if (net == null) return 0;

        ItemStack reference = getAmmoReference(gunStack);
        if (reference == null) return 0;

        ItemStackKey key = new ItemStackKey(reference);
        var storage = net.getUnifiedStorage();
        KeyAmount extracted = storage.extract(key, neededAmount, false, false);
        if (extracted.amount() > 0) {
            net.setDirty();
        }
        return (int) extracted.amount();
    }

    public static int extractAmmoFromNetwork(ItemStack gunStack, int neededAmount, ServerPlayer player) {
        DimensionsNet net = DimensionsNet.getPrimaryNetFromPlayer(player);
        if (net == null) return 0;

        ItemStack reference = getAmmoReference(gunStack);
        if (reference == null) return 0;

        ItemStackKey key = new ItemStackKey(reference);
        var storage = net.getUnifiedStorage();
        KeyAmount extracted = storage.extract(key, neededAmount, false, false);
        if (extracted.amount() > 0) {
            net.setDirty();
            ItemStack ammo = key.copyStackWithCount(extracted.amount());
            if (!player.getInventory().add(ammo)) {
                player.drop(ammo, false);
            }
        }
        return (int) extracted.amount();
    }

    public static int countAmmoInNetwork(ItemStack gunStack, ServerPlayer player) {
        DimensionsNet net = DimensionsNet.getPrimaryNetFromPlayer(player);
        if (net == null) return 0;

        ItemStack reference = getAmmoReference(gunStack);
        if (reference == null) return 0;

        ItemStackKey key = new ItemStackKey(reference);
        KeyAmount found = net.getUnifiedStorage().getStackByKey(key);
        return (int) Math.min(found.amount(), Integer.MAX_VALUE);
    }

    public static int countAmmoInNetworkByAmmoId(ResourceLocation ammoId, ServerPlayer player) {
        DimensionsNet net = DimensionsNet.getPrimaryNetFromPlayer(player);
        if (net == null) return 0;

        ItemStackKey key = buildAmmoKey(ammoId);
        if (key == null) return 0;

        KeyAmount found = net.getUnifiedStorage().getStackByKey(key);
        return (int) Math.min(found.amount(), Integer.MAX_VALUE);
    }

    public static ResourceLocation getAmmoId(ItemStack gunStack) {
        IGun iGun = IGun.getIGunOrNull(gunStack);
        if (iGun == null) return null;
        ResourceLocation gunId = iGun.getGunId(gunStack);
        if (gunId == null) return null;
        var opt = TimelessAPI.getCommonGunIndex(gunId);
        if (opt.isEmpty()) return null;
        return opt.get().getGunData().getAmmoId();
    }

    public static ResourceLocation getAmmoIdClient(ItemStack gunStack) {
        IGun iGun = IGun.getIGunOrNull(gunStack);
        if (iGun == null) return null;
        ResourceLocation gunId = iGun.getGunId(gunStack);
        if (gunId == null) return null;
        var opt = com.tacz.guns.api.TimelessAPI.getClientGunIndex(gunId);
        if (opt.isEmpty()) return null;
        return opt.get().getGunData().getAmmoId();
    }

    private static ItemStack getAmmoReference(ItemStack gunStack) {
        ResourceLocation ammoId = getAmmoId(gunStack);
        if (ammoId == null) return null;
        return buildAmmoStack(ammoId);
    }

    private static ItemStackKey buildAmmoKey(ResourceLocation ammoId) {
        ItemStack ref = buildAmmoStack(ammoId);
        return ref != null ? new ItemStackKey(ref) : null;
    }

    private static ItemStack buildAmmoStack(ResourceLocation ammoId) {
        ItemStack ref = new ItemStack(ModItems.AMMO.get());
        if (ref.getItem() instanceof IAmmo iAmmo) {
            iAmmo.setAmmoId(ref, ammoId);
        }
        return ref;
    }
}
