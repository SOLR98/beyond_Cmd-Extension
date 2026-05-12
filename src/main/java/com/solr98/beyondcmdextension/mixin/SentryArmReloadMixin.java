package com.solr98.beyondcmdextension.mixin;

import com.solr98.beyondcmdextension.handler.NetworkAmmoExtractor;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.common.item.NetedItem;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "euphy.upo.sentrymechanicalarm.content.SentryArmBlockEntity", remap = false)
public class SentryArmReloadMixin {

    @Inject(method = "performInstantReload", at = @At("HEAD"), cancellable = true)
    private void onReload(net.minecraftforge.common.util.FakePlayer fakePlayer,
                           com.tacz.guns.api.item.IGun iGun,
                           net.minecraft.world.item.ItemStack gunStack,
                           CallbackInfoReturnable<Boolean> cir) {
        try {
            if (iGun.useInventoryAmmo(gunStack)) return;
            DimensionsNet net = getTerminalNetwork();
            if (net == null) return;

            var gunIndexOpt = TimelessAPI.getCommonGunIndex(iGun.getGunId(gunStack));
            if (gunIndexOpt.isEmpty()) return;

            int maxAmmo = gunIndexOpt.get().getGunData().getAmmoAmount();
            int currentAmmo = iGun.getCurrentAmmoCount(gunStack);
            int need = maxAmmo - currentAmmo;
            if (need <= 0) return;

            int networkCount = NetworkAmmoExtractor.countAmmoInNetwork(gunStack, net);
            if (networkCount == Integer.MAX_VALUE) need = Math.min(need, 9000);
            else if (networkCount > 0) need = Math.min(need, Math.min(networkCount, 9000));
            else return;

            int fromNet = NetworkAmmoExtractor.consumeAmmoDirectly(gunStack, need, net);
            if (fromNet <= 0) return;

            iGun.setCurrentAmmoCount(gunStack, currentAmmo + fromNet);
            Bolt bolt = gunIndexOpt.get().getGunData().getBolt();
            if (bolt != Bolt.OPEN_BOLT && !iGun.hasBulletInBarrel(gunStack) && iGun.getCurrentAmmoCount(gunStack) > 0) {
                iGun.reduceCurrentAmmoCount(gunStack);
                iGun.setBulletInBarrel(gunStack, true);
            }
            cir.setReturnValue(true);
        } catch (Exception ignored) {}
    }

    @Unique
    private DimensionsNet getTerminalNetwork() {
        net.minecraft.world.level.block.entity.BlockEntity be =
                (net.minecraft.world.level.block.entity.BlockEntity) (Object) this;
        if (be.getLevel() == null || be.getLevel().isClientSide) return null;
        var opt = be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).resolve();
        if (opt.isEmpty()) return null;
        IItemHandler inv = opt.get();
        for (int i = 0; i < inv.getSlots(); i++) {
            net.minecraft.world.item.ItemStack stack = inv.getStackInSlot(i);
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
