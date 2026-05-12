package com.solr98.beyondcmdextension;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.Arrays;
import java.util.List;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = com.solr98.beyondcmdextension.Beyond_cmd_extension.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER.comment("Whether to log the dirt block on common setup").define("logDirtBlock", true);

    private static final ForgeConfigSpec.IntValue MAGIC_NUMBER = BUILDER.comment("A magic number").defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER.comment("What you want the introduction message to be for the magic number").define("magicNumberIntroduction", "The magic number is... ");

    private static final ForgeConfigSpec.BooleanValue ENABLE_ENCHANTMENT_SEPARATION = BUILDER
            .comment("Enable automatic enchantment book separation when inserted into dimension network")
            .define("enableEnchantmentSeparation", true);

    private static final ForgeConfigSpec.IntValue ENCHANTMENT_SEPARATION_BASE_COST = BUILDER
            .comment("Base experience cost for separating one enchantment from a book")
            .defineInRange("enchantmentSeparationBaseCost", 10, 0, 1000);

    private static final ForgeConfigSpec.IntValue ENCHANTMENT_SEPARATION_LEVEL_MULTIPLIER = BUILDER
            .comment("Experience cost multiplier per enchantment level")
            .defineInRange("enchantmentSeparationLevelMultiplier", 5, 0, 100);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> HIGH_COST_ENCHANTMENTS = BUILDER
            .comment("List of enchantments with higher separation cost (format: 'modid:enchantment_id:multiplier')")
            .defineList("highCostEnchantments", 
                Arrays.asList(
                    // === 原版Minecraft附魔默认成本列表 ===
                    
                    // 1. 仅限宝藏附魔 (Treasure-Only) - 最高成本 (3.0x)
                    // 这些附魔只能通过战利品箱、交易或钓鱼获得
                    "minecraft:mending:3.0",           // 经验修补
                    "minecraft:frost_walker:3.0",      // 冰霜行者
                    "minecraft:soul_speed:3.0",        // 灵魂疾行
                    "minecraft:swift_sneak:3.0",       // 迅捷潜行
                    
                    // 2. 宝藏附魔 (Treasure) - 高成本 (2.0x)
                    // 可以通过附魔台获得，但比较稀有
                    "minecraft:loyalty:2.0",           // 忠诚
                    "minecraft:impaling:2.0",          // 穿刺
                    "minecraft:riptide:2.0",           // 激流
                    "minecraft:channeling:2.0",        // 引雷
                    
                    // 3. 诅咒附魔 (Curse) - 中高成本 (2.0x)
                    // 负面效果的附魔
                    "minecraft:binding_curse:2.0",     // 绑定诅咒
                    "minecraft:vanishing_curse:2.0",   // 消失诅咒
                    
                    // 4. 稀有普通附魔 - 中成本 (1.5x)
                    // 非常有用且相对稀有的附魔
                    "minecraft:silk_touch:1.5",        // 精准采集
                    "minecraft:infinity:1.5",          // 无限
                    "minecraft:multishot:1.5",         // 多重射击
                    
                    // 5. 有用但常见的附魔 - 轻微额外成本 (1.2x)
                    // 大多数玩家常用的附魔
                    "minecraft:sharpness:1.2",         // 锋利
                    "minecraft:efficiency:1.2",        // 效率
                    "minecraft:unbreaking:1.2",        // 耐久
                    "minecraft:fortune:1.2",           // 时运
                    "minecraft:looting:1.2",           // 抢夺
                    "minecraft:protection:1.2",        // 保护
                    "minecraft:feather_falling:1.2",   // 摔落保护
                    "minecraft:depth_strider:1.2",     // 深海探索者
                    "minecraft:aqua_affinity:1.2",     // 水下速掘
                    "minecraft:respiration:1.2",       // 水下呼吸
                    "minecraft:thorns:1.2",            // 荆棘
                    "minecraft:fire_aspect:1.2",       // 火焰附加
                    "minecraft:knockback:1.2",         // 击退
                    "minecraft:fire_protection:1.2",   // 火焰保护
                    "minecraft:blast_protection:1.2",  // 爆炸保护
                    "minecraft:projectile_protection:1.2", // 弹射物保护
                    "minecraft:power:1.2",             // 力量
                    "minecraft:punch:1.2",             // 冲击
                    "minecraft:flame:1.2",             // 火矢
                    "minecraft:luck_of_the_sea:1.2",   // 海之眷顾
                    "minecraft:lure:1.2",              // 饵钓
                    "minecraft:quick_charge:1.2",      // 快速装填
                    "minecraft:piercing:1.2",          // 穿透
                    "minecraft:sweeping:1.2",          // 横扫之刃
                    
                    // 6. 特殊效果附魔 - 最低额外成本 (1.1x)
                    // 针对特定生物类型的附魔
                    "minecraft:smite:1.1",             // 亡灵杀手
                    "minecraft:bane_of_arthropods:1.1" // 节肢杀手
                ), 
                obj -> obj instanceof String);

    private static final ForgeConfigSpec.DoubleValue DEFAULT_ENCHANTMENT_MULTIPLIER = BUILDER
            .comment("Default experience cost multiplier for enchantments not in the high cost list")
            .defineInRange("defaultEnchantmentMultiplier", 1.0, 0.1, 10.0);

    private static final ForgeConfigSpec.ConfigValue<String> COST_FORMULA = BUILDER
            .comment("Cost calculation formula. Available variables: base, level, multiplier, books")
            .define("costFormula", "base + (level - 1) * multiplier");

    private static final ForgeConfigSpec.BooleanValue USE_FORMULA = BUILDER
            .comment("Use custom formula for cost calculation (if false, uses default formula)")
            .define("useFormula", false);

    private static final ForgeConfigSpec.BooleanValue ENABLE_NETWORK_TRANSFER = BUILDER
            .comment("Enable network-to-network item transfer feature (requires restart)")
            .define("enableNetworkTransfer", false);

    // ========== 物品黑名单配置 ==========

    private static final ForgeConfigSpec.BooleanValue ENABLE_ITEM_BLACKLIST = BUILDER
            .comment("Enable item blacklist to prevent specific items from being inserted into networks")
            .define("enableItemBlacklist", false);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_BLACKLIST = BUILDER
            .comment("List of items blocked from network insertion (format: 'modid:item_id')")
            .defineList("itemBlacklist",
                Arrays.asList(
                    "minecraft:barrier",
                    "minecraft:command_block",
                    "minecraft:structure_block",
                    "minecraft:structure_void",
                    "minecraft:jigsaw",
                    "minecraft:bedrock",
                    "minecraft:end_portal_frame",
                    "minecraft:spawner"
                ),
                obj -> obj instanceof String);

    // ========== 网络合成频率限制 ==========

    private static final ForgeConfigSpec.IntValue CRAFT_COOLDOWN_MS = BUILDER
            .comment("Minimum interval in milliseconds between craft requests from the same player (0 to disable)")
            .defineInRange("craftCooldownMs", 1000, 0, 60000);

    // ========== 配方树深度限制 ==========

    private static final ForgeConfigSpec.IntValue CRAFT_MAX_DEPTH = BUILDER
            .comment("Maximum recursion depth for recipe tree resolution")
            .defineInRange("craftMaxDepth", 6, 1, 20);

    private static final ForgeConfigSpec.BooleanValue BLOCK_BD_CONTAINER_READER = BUILDER
            .comment("Block TacZAddon from reading BeyondDimensions storage blocks as surrounding containers")
            .define("blockBdContainerReader", true);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean logDirtBlock;
    public static int magicNumber;
    public static String magicNumberIntroduction;
    public static boolean enableEnchantmentSeparation;
    public static int enchantmentSeparationBaseCost;
    public static int enchantmentSeparationLevelMultiplier;
    public static List<? extends String> highCostEnchantments;
    public static double defaultEnchantmentMultiplier;
    public static String costFormula;
    public static boolean useFormula;
    public static boolean enableNetworkTransfer;
    public static boolean enableItemBlacklist;
    public static List<? extends String> itemBlacklist;
    public static int craftCooldownMs;
    public static int craftMaxDepth;
    public static boolean blockBdContainerReader;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        logDirtBlock = LOG_DIRT_BLOCK.get();
        magicNumber = MAGIC_NUMBER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();
        enableEnchantmentSeparation = ENABLE_ENCHANTMENT_SEPARATION.get();
        enchantmentSeparationBaseCost = ENCHANTMENT_SEPARATION_BASE_COST.get();
        enchantmentSeparationLevelMultiplier = ENCHANTMENT_SEPARATION_LEVEL_MULTIPLIER.get();
        highCostEnchantments = HIGH_COST_ENCHANTMENTS.get();
        defaultEnchantmentMultiplier = DEFAULT_ENCHANTMENT_MULTIPLIER.get();
        costFormula = COST_FORMULA.get();
        useFormula = USE_FORMULA.get();
        enableNetworkTransfer = ENABLE_NETWORK_TRANSFER.get();
        enableItemBlacklist = ENABLE_ITEM_BLACKLIST.get();
        itemBlacklist = ITEM_BLACKLIST.get();
        craftCooldownMs = CRAFT_COOLDOWN_MS.get();
        craftMaxDepth = CRAFT_MAX_DEPTH.get();
        blockBdContainerReader = BLOCK_BD_CONTAINER_READER.get();
    }
}
