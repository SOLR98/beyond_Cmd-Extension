package com.solr98.beyondcmdextension.mixin;

import com.solr98.beyondcmdextension.client.AmmoCountCache;
import com.solr98.beyondcmdextension.handler.NetworkAmmoExtractor;
import com.solr98.beyondcmdextension.maid.MaidNetworkCache;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.common.item.NetedItem;
import net.minecraft.network.chat.Component;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mixin(value = AbstractGunItem.class, remap = false)
public abstract class AbstractGunItemMixin {

    private static final Field CURIOS_PLAYER_FIELD;
    private static final Map<UUID, Long> lastNoAmmoNotify = new HashMap<>();
    private static final long NOTIFY_COOLDOWN_MS = 5000;
    private static final Map<UUID, Long> maidBubbleCooldown = new HashMap<>();
    private static final long MAID_BUBBLE_COOLDOWN_MS = 10000;

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
            } else {
                sendNoAmmoNotify(player, null);
            }
        } else if (shooter.level().isClientSide) {
            ResourceLocation ammoId = NetworkAmmoExtractor.getAmmoIdClient(gunItem);
            if (ammoId != null) {
                AmmoCountCache.requestQuick(ammoId);
                if (!AmmoCountCache.hasData(ammoId) || AmmoCountCache.getCount(ammoId) > 0) {
                    cir.setReturnValue(true);
                }
            }
        } else if (ModList.get().isLoaded("touhou_little_maid")) {
            MaidCompat.handleCanReload(shooter, gunItem, cir);
        }
    }

    @Inject(method = "hasInventoryAmmo", at = @At("RETURN"), cancellable = true)
    private void onHasInventoryAmmo(LivingEntity shooter, ItemStack gun, boolean needCheckAmmo, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        IGun iGun = IGun.getIGunOrNull(gun);
        if (iGun == null || !iGun.useInventoryAmmo(gun)) return;

        if (shooter instanceof ServerPlayer) {
            if (NetworkAmmoExtractor.countAmmoInNetwork(gun, (ServerPlayer) shooter) > 0) {
                cir.setReturnValue(true);
            }
        } else if (shooter.level().isClientSide) {
            ResourceLocation ammoId = NetworkAmmoExtractor.getAmmoIdClient(gun);
            if (ammoId != null) {
                AmmoCountCache.requestQuick(ammoId);
            }
            cir.setReturnValue(true);
        } else if (ModList.get().isLoaded("touhou_little_maid")) {
            DimensionsNet net = MaidCompat.getTerminalNetwork(shooter);
            if (net != null && NetworkAmmoExtractor.countAmmoInNetwork(gun, net) > 0) {
                cir.setReturnValue(true);
            }
        }
    }

    private static DimensionsNet findTerminalOnShooter(LivingEntity shooter) {
        var capOpt = shooter.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).resolve();
        if (capOpt.isEmpty()) return null;
        return findTerminalInHandler(capOpt.get());
    }

    @Inject(method = "findAndExtractInventoryAmmo", at = @At("RETURN"), cancellable = true)
    private void onFindAndExtract(IItemHandler itemHandler, ItemStack gunItem, int needAmmoCount, CallbackInfoReturnable<Integer> cir) {
        int found = cir.getReturnValue();
        if (found >= needAmmoCount) return;
        int stillNeed = needAmmoCount - found;

        if (stillNeed <= 0) return;

        ServerPlayer player = getPlayer(itemHandler);
        if (player != null) {
            int fromNet = NetworkAmmoExtractor.consumeAmmoDirectly(gunItem, stillNeed, player);
            if (fromNet > 0) { cir.setReturnValue(found + fromNet); return; }
        }

        // 女仆支持
        DimensionsNet net;
        if (ModList.get().isLoaded("touhou_little_maid") && (net = MaidCompat.getNetworkFromHandler(itemHandler)) != null) {
            int fromNet = NetworkAmmoExtractor.consumeAmmoDirectly(gunItem, stillNeed, net);
            if (fromNet > 0) { cir.setReturnValue(found + fromNet); return; }
        }

        // 通用终端扫描（哨兵 FakePlayer / 其他实体 / 装备了终端的任意生物）
        net = findTerminalInHandler(itemHandler);
        if (net != null) {
            int fromNet = NetworkAmmoExtractor.consumeAmmoDirectly(gunItem, stillNeed, net);
            if (fromNet > 0) cir.setReturnValue(found + fromNet);
        }
    }

    private static DimensionsNet findTerminalInHandler(IItemHandler inv) {
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            int netId = NetedItem.getNetId(stack);
            if (netId >= 0) {
                DimensionsNet net = DimensionsNet.getNetFromId(netId);
                if (net != null) {
                    return net;
                }
            }
        }
        return null;
    }

    private static ServerPlayer getPlayer(IItemHandler handler) {
        if (handler instanceof PlayerMainInvWrapper w) {
            var p = w.getInventoryPlayer().player;
            return p instanceof ServerPlayer sp ? sp : null;
        }
        if (handler instanceof CombinedInvWrapper) {
            for (String fieldName : new String[]{"itemHandler", "children", "handlers"}) {
                try {
                    var field = CombinedInvWrapper.class.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object raw = field.get(handler);
                    IItemHandlerModifiable[] arr;
                    if (raw instanceof IItemHandlerModifiable[] iArr) {
                        arr = iArr;
                    } else if (raw instanceof List<?> list) {
                        arr = list.toArray(new IItemHandlerModifiable[0]);
                    } else {
                        continue;
                    }
                    for (var sub : arr) {
                        var result = getPlayer(sub);
                        if (result != null) return result;
                    }
                } catch (Exception ignored) {}
            }
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

    private static void sendNoAmmoNotify(LivingEntity target, DimensionsNet net) {
        if (!(target instanceof ServerPlayer player)) return;
        if (net == null) {
            net = DimensionsNet.getPrimaryNetFromPlayer(player);
            if (net == null) return;
        }
        long now = System.currentTimeMillis();
        if (now - lastNoAmmoNotify.getOrDefault(player.getUUID(), 0L) < NOTIFY_COOLDOWN_MS) return;
        lastNoAmmoNotify.put(player.getUUID(), now);
        player.sendSystemMessage(Component.translatable("message.beyond_cmd_extension.network_no_ammo", net.getId()));
    }

    public static class MaidCompat {

        static DimensionsNet getTerminalNetwork(LivingEntity entity) {
            if (!(entity instanceof com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid maid)) return null;
            DimensionsNet net = findInHandler(maid.getAvailableInv(false));
            if (net != null) { MaidNetworkCache.put(maid.getUUID(), net.getId()); return net; }
            net = findInCurios(maid);
            if (net != null) { MaidNetworkCache.put(maid.getUUID(), net.getId()); return net; }
            net = findInHandler(maid.getMaidBauble());
            if (net != null) { MaidNetworkCache.put(maid.getUUID(), net.getId()); return net; }
            MaidNetworkCache.remove(maid.getUUID());
            return null;
        }

        static DimensionsNet getNetworkFromHandler(IItemHandler handler) {
            if (!(handler instanceof com.github.tartaricacid.touhoulittlemaid.inventory.handler.MaidInvWrapper maidInv)) return null;
            return getTerminalNetwork(maidInv.getMaid());
        }

        static void handleCanReload(LivingEntity shooter, ItemStack gunItem, CallbackInfoReturnable<Boolean> cir) {
            if (!(shooter instanceof com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid maid)) return;
            DimensionsNet net = getTerminalNetwork(maid);
            if (net == null) return;
            int count = NetworkAmmoExtractor.countAmmoInNetwork(gunItem, net);
            if (count > 0) {
                cir.setReturnValue(true);
            } else {
                UUID maidId = maid.getUUID();
                long now = System.currentTimeMillis();
                if (now - maidBubbleCooldown.getOrDefault(maidId, 0L) < MAID_BUBBLE_COOLDOWN_MS) return;
                maidBubbleCooldown.put(maidId, now);
                maid.getChatBubbleManager().addChatBubble(
                        com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.implement.TextChatBubbleData.type2(
                                Component.translatable("message.beyond_cmd_extension.network_no_ammo", net.getId())));
            }
        }

        private static DimensionsNet findInHandler(IItemHandler inv) {
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                int netId = NetedItem.getNetId(stack);
                if (netId >= 0) {
                    DimensionsNet net = DimensionsNet.getNetFromId(netId);
                    if (net != null) return net;
                }
            }
            return null;
        }

        private static DimensionsNet findInCurios(com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid maid) {
            try {
                DimensionsNet[] result = {null};
                top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(maid).ifPresent(handler -> {
                    for (var entry : handler.getCurios().entrySet()) {
                        var stacksHandler = entry.getValue();
                        var stacks = stacksHandler.getStacks();
                        for (int i = 0; i < stacks.getSlots(); i++) {
                            ItemStack stack = stacks.getStackInSlot(i);
                            if (stack.isEmpty()) continue;
                            int netId = NetedItem.getNetId(stack);
                            if (netId >= 0) {
                                DimensionsNet net = DimensionsNet.getNetFromId(netId);
                                if (net != null) { result[0] = net; return; }
                            }
                        }
                    }
                });
                return result[0];
            } catch (NoClassDefFoundError | Exception ignored) {}
            return null;
        }
    }
}
