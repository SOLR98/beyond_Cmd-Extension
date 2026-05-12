package com.solr98.beyondcmdextension.handler;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.init.ModItems;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.AbstractUnorderedStackHandler.TypeBucket;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.item.NetedItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NetworkAmmoExtractor {

    public static int consumeAmmoDirectly(ItemStack gunStack, int neededAmount, ServerPlayer player) {
        DimensionsNet net = DimensionsNet.getPrimaryNetFromPlayer(player);
        if (net == null) return 0;
        return consumeAmmoDirectly(gunStack, neededAmount, net);
    }

    public static int consumeAmmoDirectly(ItemStack gunStack, int neededAmount, DimensionsNet net) {
        ItemStack reference = getAmmoReference(gunStack);
        if (reference != null) {
            ItemStackKey key = new ItemStackKey(reference);
            KeyAmount extracted = net.getUnifiedStorage().extract(key, neededAmount, false, false);
            if (extracted.amount() > 0) {
                net.setDirty();
                return (int) extracted.amount();
            }
        }
        if (hasCreativeAmmoBoxInNetwork(gunStack, net)) return neededAmount;
        return 0;
    }

    public static int extractAmmoFromNetwork(ItemStack gunStack, int neededAmount, ServerPlayer player) {
        DimensionsNet net = DimensionsNet.getPrimaryNetFromPlayer(player);
        if (net == null) return 0;

        ItemStack reference = getAmmoReference(gunStack);
        if (reference == null) return 0;

        ItemStackKey key = new ItemStackKey(reference);
        KeyAmount extracted = net.getUnifiedStorage().extract(key, neededAmount, false, false);
        if (extracted.amount() > 0) {
            net.setDirty();
            ItemStack ammo = key.copyStackWithCount(extracted.amount());
            if (!player.getInventory().add(ammo)) {
                player.drop(ammo, false);
            }
        }
        return (int) extracted.amount();
    }

    public static DimensionsNet findNetworkForPlayer(ServerPlayer player) {
        DimensionsNet net = DimensionsNet.getPrimaryNetFromPlayer(player);
        if (net != null) return net;
        net = findTerminalInInventory(player);
        return net;
    }

    public static DimensionsNet findTerminalInInventory(ServerPlayer player) {
        var capOpt = player.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
        if (capOpt.isEmpty()) return null;
        IItemHandler inv = capOpt.get();
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty()) {
                int netId = NetedItem.getNetId(stack);
                if (netId >= 0) {
                    DimensionsNet found = DimensionsNet.getNetFromId(netId);
                    if (found != null) return found;
                }
            }
        }
        return null;
    }

    public static List<DimensionsNet> findAllTerminalsInInventory(ServerPlayer player) {
        List<DimensionsNet> nets = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        var capOpt = player.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
        if (capOpt.isEmpty()) return nets;
        IItemHandler inv = capOpt.get();
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty()) {
                int netId = NetedItem.getNetId(stack);
                if (netId >= 0 && !seen.contains(netId)) {
                    seen.add(netId);
                    DimensionsNet net = DimensionsNet.getNetFromId(netId);
                    if (net != null) nets.add(net);
                }
            }
        }
        return nets;
    }

    public static int countAmmoInNetwork(ItemStack gunStack, ServerPlayer player) {
        DimensionsNet net = DimensionsNet.getPrimaryNetFromPlayer(player);
        if (net != null) {
            int count = countAmmoInNetwork(gunStack, net);
            if (count > 0) return count;
        }
        net = findTerminalInInventory(player);
        if (net == null) return 0;
        return countAmmoInNetwork(gunStack, net);
    }

    public static int countAmmoInNetwork(ItemStack gunStack, DimensionsNet net) {
        ItemStack reference = getAmmoReference(gunStack);
        if (reference != null) {
            ItemStackKey key = new ItemStackKey(reference);
            KeyAmount found = net.getUnifiedStorage().getStackByKey(key);
            long count = found.amount();
            if (count > 0) return (int) Math.min(count, Integer.MAX_VALUE);
        }
        if (hasCreativeAmmoBoxInNetwork(gunStack, net)) return Integer.MAX_VALUE;
        return 0;
    }

    public static int countAmmoInNetworkByAmmoId(ResourceLocation ammoId, DimensionsNet net) {
        if (ammoId == null || net == null) return 0;
        ItemStackKey key = buildAmmoKey(ammoId);
        if (key == null) return 0;
        KeyAmount found = net.getUnifiedStorage().getStackByKey(key);
        long count = found.amount();
        if (count > 0) return (int) Math.min(count, Integer.MAX_VALUE);
        if (hasCreativeAmmoBoxInNetwork(ammoId, net)) return Integer.MAX_VALUE;
        return 0;
    }

    public static int countAmmoInNetworkByAmmoId(ResourceLocation ammoId, ServerPlayer player) {
        DimensionsNet net = DimensionsNet.getPrimaryNetFromPlayer(player);
        if (net == null) return 0;
        return countAmmoInNetworkByAmmoId(ammoId, net);
    }

    public static boolean hasCreativeAmmoBoxInNetwork(ResourceLocation ammoId, DimensionsNet net) {
        if (ammoId == null) return false;
        UnifiedStorage storage = net.getUnifiedStorage();
        var opt = storage.getBucket(ItemStackKey.ID);
        if (opt.isEmpty()) return false;
        TypeBucket bucket = opt.get();
        for (int i = 0; i < bucket.size(); i++) {
            IStackKey<?> rawKey = bucket.get(i);
            if (!(rawKey instanceof ItemStackKey ik)) continue;
            ItemStack stack = ik.getReadOnlyStack();
            if (!(stack.getItem() instanceof IAmmoBox box)) continue;
            if (box.isAllTypeCreative(stack)) return true;
            if (box.isCreative(stack) && ammoId.equals(box.getAmmoId(stack))) return true;
        }
        return false;
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

    public static boolean hasCreativeAmmoBoxInNetwork(ItemStack gunStack, ServerPlayer player) {
        DimensionsNet net = DimensionsNet.getPrimaryNetFromPlayer(player);
        if (net == null) return false;
        return hasCreativeAmmoBoxInNetwork(gunStack, net);
    }

    public static boolean hasCreativeAmmoBoxInNetwork(ItemStack gunStack, DimensionsNet net) {
        ResourceLocation ammoId = getAmmoId(gunStack);
        if (ammoId == null) return false;
        UnifiedStorage storage = net.getUnifiedStorage();
        var opt = storage.getBucket(ItemStackKey.ID);
        if (opt.isEmpty()) return false;
        TypeBucket bucket = opt.get();
        for (int i = 0; i < bucket.size(); i++) {
            IStackKey<?> rawKey = bucket.get(i);
            if (!(rawKey instanceof ItemStackKey ik)) continue;
            ItemStack stack = ik.getReadOnlyStack();
            if (!(stack.getItem() instanceof IAmmoBox box)) continue;
            if (box.isAllTypeCreative(stack)) return true;
            if (box.isCreative(stack) && ammoId.equals(box.getAmmoId(stack))) return true;
        }
        return false;
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
