package com.solr98.beyondcmdextension.mixin;

import com.solr98.beyondcmdextension.handler.NetworkAmmoExtractor;
import com.solr98.beyondcmdextension.maid.MaidNetworkHelper;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.common.item.NetedItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ModernKineticGunScriptAPI.class, remap = false)
public class ModernKineticGunScriptAPIMixin {

    @Shadow
    private LivingEntity shooter;

    @Shadow
    private ItemStack itemStack;

    @Shadow
    private AbstractGunItem abstractGunItem;

    @Inject(method = "hasAmmoToConsume", at = @At("RETURN"), cancellable = true)
    private void onHasAmmoToConsume(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        if (abstractGunItem.useDummyAmmo(itemStack)) return;

        if (shooter instanceof ServerPlayer player) {
            if (NetworkAmmoExtractor.countAmmoInNetwork(itemStack, player) > 0) {
                cir.setReturnValue(true);
            }
        } else if (shooter.level().isClientSide) {
            cir.setReturnValue(true);
        } else if (ModList.get().isLoaded("touhou_little_maid")) {
            DimensionsNet net = MaidNetworkHelper.findTerminal(shooter);
            if (net != null && NetworkAmmoExtractor.countAmmoInNetwork(itemStack, net) > 0) {
                cir.setReturnValue(true);
            }
        }
        // 通用终端扫描（哨兵 FakePlayer 等）
        if (!cir.getReturnValue()) {
            DimensionsNet net = findTerminalOnShooter(shooter);
            if (net != null && NetworkAmmoExtractor.countAmmoInNetwork(itemStack, net) > 0) {
                cir.setReturnValue(true);
            }
        }
    }

    private static DimensionsNet findTerminalOnShooter(LivingEntity shooter) {
        var capOpt = shooter.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).resolve();
        if (capOpt.isEmpty()) return null;
        IItemHandler inv = capOpt.get();
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
}
