package com.solr98.beyondcmdextension;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.common.item.NetedItem;
import org.slf4j.Logger;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Beyond_cmd_extension.MODID)
public class Beyond_cmd_extension {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "beyond_cmd_extension";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings("removal")
    public Beyond_cmd_extension() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        
        // Register command language config
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, CommandConfig.SERVER_SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        registerEnchantmentBookSeparator();
        registerItemBlacklistHandler();
        registerAmmoBoxExtractHandler();
        com.solr98.beyondcmdextension.network.PacketHandler.register();
    }

    private void registerEnchantmentBookSeparator() {
        com.wintercogs.beyonddimensions.api.dimensionnet.helper.UnifiedStorageBeforeInsertHandler
                .addHandler(new com.solr98.beyondcmdextension.handler.EnchantmentBookSeparatorHandler());
        LOGGER.info("Registered EnchantmentBookSeparatorHandler");
    }

    private void registerItemBlacklistHandler() {
        com.wintercogs.beyonddimensions.api.dimensionnet.helper.UnifiedStorageBeforeInsertHandler
                .addHandler(new com.solr98.beyondcmdextension.handler.ItemBlacklistHandler());
        LOGGER.info("Registered ItemBlacklistHandler");
    }

    private void registerAmmoBoxExtractHandler() {
        if (ModList.get().isLoaded("tacz")) {
            com.wintercogs.beyonddimensions.api.dimensionnet.helper.UnifiedStorageBeforeInsertHandler
                    .addHandler(new com.solr98.beyondcmdextension.handler.AmmoBoxExtractHandler());
            LOGGER.info("Registered AmmoBoxExtractHandler");
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (blockId == null || !"sentrymechanicalarm".equals(blockId.getNamespace())) return;

        Player player = event.getEntity();
        ItemStack stack = player.getItemInHand(event.getHand());
        if (stack.isEmpty() || NetedItem.getNetId(stack) < 0) return;

        event.setCanceled(true);

        if (event.getSide().isClient()) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return;

        try {
            Method getHeld = be.getClass().getMethod("getHeldItem");
            ItemStack held = (ItemStack) getHeld.invoke(be);
            if (held.isEmpty()) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.beyond_cmd_extension.sentry_no_gun"), true);
                return;
            }

            Method addBox = be.getClass().getMethod("addAmmoBox", ItemStack.class);
            boolean ok = (boolean) addBox.invoke(be, stack);
            if (ok) {
                if (!player.isCreative()) stack.shrink(1);
            } else {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("sentry.tooltip.ammobox_1"), true);
            }
        } catch (Exception ignored) {}
    }
}
