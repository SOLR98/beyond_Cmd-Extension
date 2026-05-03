package com.solr98.beyondcmdextension;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

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
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock) LOGGER.info("DIRT BLOCK >> {}");

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);
        
        // Register enchantment book separator handler
        registerEnchantmentBookSeparator();
        // Register item blacklist handler
        registerItemBlacklistHandler();
        // Register ammo box extract handler (only if TACZ is loaded)
        registerAmmoBoxExtractHandler();
        // Register network packets
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
}
