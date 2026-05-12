package com.solr98.beyondcmdextension.mixin;

import com.wintercogs.beyonddimensions.common.item.NetedItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

@Pseudo
@Mixin(targets = "euphy.upo.sentrymechanicalarm.content.SentryArmBlockEntity", remap = false)
public class SentryFakePlayerSyncMixin {

    @Inject(method = "fireGun", at = @At(value = "INVOKE",
            target = "Leuphy/upo/sentrymechanicalarm/util/SentryFakePlayer;sync(Lnet/minecraftforge/common/util/FakePlayer;Leuphy/upo/sentrymechanicalarm/content/SentryArmBlockEntity;FFLnet/minecraft/world/item/ItemStack;)V",
            shift = At.Shift.AFTER), remap = false)
    private void onAfterSync(CallbackInfo ci) {
        try {
            BlockEntity be = (BlockEntity) (Object) this;
            if (be.getLevel() == null || be.getLevel().isClientSide) return;

            Method getFake = be.getClass().getClassLoader()
                    .loadClass("euphy.upo.sentrymechanicalarm.util.SentryFakePlayer")
                    .getMethod("get", be.getClass());
            FakePlayer fp = (FakePlayer) getFake.invoke(null, be);
            if (fp == null) return;

            fp.getInventory().setItem(8, ItemStack.EMPTY);

            var opt = be.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
            if (opt.isEmpty()) return;
            IItemHandler inv = opt.get();
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (!stack.isEmpty() && NetedItem.getNetId(stack) >= 0) {
                    fp.getInventory().setItem(8, stack.copy());
                    break;
                }
            }
        } catch (Exception ignored) {}
    }
}
