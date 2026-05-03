package com.solr98.beyondcmdextension.mixin;

import com.solr98.beyondcmdextension.client.NetworkItemCache;
import com.solr98.beyondcmdextension.network.PacketHandler;
import com.solr98.beyondcmdextension.network.RequestNetworkItemsPacket;
import com.solr98.beyondcmdextension.network.TaczCraftPacket;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GunSmithTableScreen.class)
public class GunSmithTableScreenMixin {

    @Shadow(remap = false)
    private Int2IntArrayMap playerIngredientCount;

    @Shadow(remap = false)
    private GunSmithTableRecipe selectedRecipe;

    @Inject(method = "init", at = @At("TAIL"), remap = false)
    private void onInit(CallbackInfo ci) {
        var screen = (GunSmithTableScreen) (Object) this;
        if (NetworkItemCache.isEmpty()) {
            PacketHandler.sendToServer(new RequestNetworkItemsPacket());
        }

        ResourceLocation taczTex = ResourceLocation.tryBuild("tacz", "textures/gui/gun_smith_table.png");
        var btn = new Button(screen.getGuiLeft() + 267, screen.getGuiTop() + 162, 18, 18,
                Component.empty(), b -> {
            if (selectedRecipe != null && NetworkItemCache.hasNetwork()) {
                int count = 1;
                if (Screen.hasShiftDown()) {
                    int max = Integer.MAX_VALUE;
                    var ingr = selectedRecipe.getInputs();
                    for (int i = 0; i < ingr.size(); i++) {
                        int need = ingr.get(i).getCount();
                        if (need <= 0) continue;
                        int have = playerIngredientCount.get(i);
                        max = Math.min(max, have / need);
                    }
                    count = Math.max(1, max);
                } else if (Screen.hasControlDown()) {
                    count = 10;
                }
                PacketHandler.sendToServer(new TaczCraftPacket(selectedRecipe.getId(), count));
            }
        }, s -> Component.empty()) {
            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                boolean hasNet = NetworkItemCache.hasNetwork();
                int v = (isHoveredOrFocused() && hasNet) ? 229 : 211;
                g.blit(taczTex, getX(), getY(), 18, 18, 149, v, 18, 18, 256, 256);
                if (!hasNet) {
                    g.fill(getX(), getY(), getX() + 18, getY() + 18, 0xAA000000);
                }
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                        ResourceLocation.tryBuild("beyonddimensions", "net_creater"));
                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                    g.pose().pushPose();
                    g.pose().translate(getX() + 1, getY() + 1, 0);
                    g.renderItem(new net.minecraft.world.item.ItemStack(item), 0, 0);
                    g.pose().popPose();
                }
                if (hasNet && isHoveredOrFocused()) {
                    String tip;
                    if (Screen.hasShiftDown()) {
                        tip = "Shift: 最大合成";
                    } else if (Screen.hasControlDown()) {
                        tip = "Ctrl: 合成×10";
                    } else {
                        tip = "网络合成";
                    }
                    g.renderTooltip(Minecraft.getInstance().font, Component.literal(tip), mx, my);
                }
                if (!hasNet && isHoveredOrFocused()) {
                    g.renderTooltip(Minecraft.getInstance().font,
                            Component.literal("§c无网络"),
                            mx, my);
                }
            }
        };

        try {
            var addM = net.minecraft.client.gui.screens.Screen.class.getDeclaredMethod("addRenderableWidget", net.minecraft.client.gui.components.AbstractWidget.class);
            addM.setAccessible(true);
            addM.invoke(screen, btn);
        } catch (Exception e) {
            try {
                var rField = net.minecraft.client.gui.screens.Screen.class.getDeclaredField("renderables");
                rField.setAccessible(true);
                var renderables = (java.util.List) rField.get(screen);
                renderables.add(btn);
                var cField = net.minecraft.client.gui.screens.Screen.class.getDeclaredField("children");
                cField.setAccessible(true);
                var children = (java.util.List) cField.get(screen);
                children.add(btn);
            } catch (Exception ignored) {}
        }
    }

    @Inject(method = "getPlayerIngredientCount", at = @At("RETURN"), remap = false)
    private void addNetworkCounts(GunSmithTableRecipe recipe, CallbackInfo ci) {
        if (NetworkItemCache.isEmpty()) return;
        String recipeKey = recipe.getId().toString();
        var ingredients = recipe.getInputs();
        for (int i = 0; i < ingredients.size(); i++) {
            var ingredient = ingredients.get(i).getIngredient();
            String exactKey = recipeKey + "|" + i;
            long exact = NetworkItemCache.getCount(exactKey);
            if (exact > 0) {
                long total = (long) playerIngredientCount.get(i) + exact;
                playerIngredientCount.put(i, (int) Math.min(total, Integer.MAX_VALUE));
                continue;
            }

            boolean hasNbt = false;
            for (ItemStack m : ingredient.getItems()) {
                if (!m.isEmpty() && m.hasTag() && !m.getTag().isEmpty()) {
                    hasNbt = true;
                    break;
                }
            }
            if (hasNbt) continue;

            long extra = 0;
            for (ItemStack match : ingredient.getItems()) {
                if (match.isEmpty()) continue;
                extra += NetworkItemCache.getCount(match.getItem().toString());
            }
            if (extra > 0) {
                long total = (long) playerIngredientCount.get(i) + extra;
                playerIngredientCount.put(i, (int) Math.min(total, Integer.MAX_VALUE));
            }
        }
    }
}
