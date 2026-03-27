package org.exampleSA.beyond_integration.command;

import net.minecraft.network.chat.Component;
import org.exampleSA.beyond_integration.CommandConfig;

import java.util.HashMap;
import java.util.Map;

public class CommandLang
{
    private static final Map<String, Map<CommandConfig.Language, String>> TRANSLATIONS = new HashMap<>();

    static
    {
        register("network.list.title",
                CommandConfig.Language.EN_US, "=== Dimension Networks ===",
                CommandConfig.Language.ZH_CN, "=== 维度网络列表 ===");

        register("network.list.deleted_title",
                CommandConfig.Language.EN_US, "=== Deleted Dimension Networks ===",
                CommandConfig.Language.ZH_CN, "=== 已删除的维度网络 ===");

        register("network.list.owner",
                CommandConfig.Language.EN_US, "Owner",
                CommandConfig.Language.ZH_CN, "所有者");

        register("network.list.players",
                CommandConfig.Language.EN_US, "Players",
                CommandConfig.Language.ZH_CN, "玩家数");

        register("network.list.managers",
                CommandConfig.Language.EN_US, "Managers",
                CommandConfig.Language.ZH_CN, "管理员数");

        register("network.list.deleted_mark",
                CommandConfig.Language.EN_US, "[DELETED]",
                CommandConfig.Language.ZH_CN, "[已删除]");

        register("network.list.none",
                CommandConfig.Language.EN_US, "No networks found.",
                CommandConfig.Language.ZH_CN, "未找到网络。");

        register("network.list.none_all",
                CommandConfig.Language.EN_US, "No networks found.",
                CommandConfig.Language.ZH_CN, "未找到网络。");

        register("network.info.title",
                CommandConfig.Language.EN_US, "=== Network Info (ID: %s) ===",
                CommandConfig.Language.ZH_CN, "=== 网络信息 (ID: %s) ===");

        register("network.info.owner",
                CommandConfig.Language.EN_US, "Owner",
                CommandConfig.Language.ZH_CN, "所有者");

        register("network.info.unknown",
                CommandConfig.Language.EN_US, "Unknown",
                CommandConfig.Language.ZH_CN, "未知");

        register("network.info.status",
                CommandConfig.Language.EN_US, "Status",
                CommandConfig.Language.ZH_CN, "状态");

        register("network.info.status.active",
                CommandConfig.Language.EN_US, "Active",
                CommandConfig.Language.ZH_CN, "活跃");

        register("network.info.status.deleted",
                CommandConfig.Language.EN_US, "Deleted",
                CommandConfig.Language.ZH_CN, "已删除");

        register("network.info.players",
                CommandConfig.Language.EN_US, "Players",
                CommandConfig.Language.ZH_CN, "玩家数");

        register("network.info.managers",
                CommandConfig.Language.EN_US, "Managers",
                CommandConfig.Language.ZH_CN, "管理员数");

        register("network.info.slot_capacity",
                CommandConfig.Language.EN_US, "Slot Capacity (per type)",
                CommandConfig.Language.ZH_CN, "槽位容量 (每种)");

        register("network.info.slot_max_size",
                CommandConfig.Language.EN_US, "Slot Max Size (types)",
                CommandConfig.Language.ZH_CN, "槽位数量 (种类)");

        register("network.info.current_time",
                CommandConfig.Language.EN_US, "Crystal Remaining Time",
                CommandConfig.Language.ZH_CN, "结晶剩余生成时间");

        register("network.info.item_types",
                CommandConfig.Language.EN_US, "Item Types",
                CommandConfig.Language.ZH_CN, "物品种类");

        register("network.info.total_items",
                CommandConfig.Language.EN_US, "Total Items",
                CommandConfig.Language.ZH_CN, "物品总数");

        register("network.info.no_players",
                CommandConfig.Language.EN_US, "No players",
                CommandConfig.Language.ZH_CN, "无玩家");

        register("network.info.not_exist",
                CommandConfig.Language.EN_US, "Network does not exist: netId=%s",
                CommandConfig.Language.ZH_CN, "网络不存在：netId=%s");

        register("network.generateItems.success",
                CommandConfig.Language.EN_US, "Generated %s types of items, total %s items inserted into network %s",
                CommandConfig.Language.ZH_CN, "已生成 %s 种物品，共 %s 个物品插入到网络 %s");

        register("network.generateItems.with_enchantments",
                CommandConfig.Language.EN_US, "With enchantments",
                CommandConfig.Language.ZH_CN, "含附魔");

        register("network.generateItems.with_nbt",
                CommandConfig.Language.EN_US, "With custom NBT",
                CommandConfig.Language.ZH_CN, "含自定义 NBT");

        register("network.batchCreate.success",
                CommandConfig.Language.EN_US, "Successfully created %s networks",
                CommandConfig.Language.ZH_CN, "成功创建 %s 个网络");

        register("network.restore.success",
                CommandConfig.Language.EN_US, "Restored network: netId=%s",
                CommandConfig.Language.ZH_CN, "已恢复网络：netId=%s");

        register("network.restore.not_deleted",
                CommandConfig.Language.EN_US, "Network is not deleted: netId=%s",
                CommandConfig.Language.ZH_CN, "网络未被删除：netId=%s");

        register("network.restore.not_exist",
                CommandConfig.Language.EN_US, "Network does not exist: netId=%s",
                CommandConfig.Language.ZH_CN, "网络不存在：netId=%s");

        register("network.insert.success",
                CommandConfig.Language.EN_US, "Inserted %s x %s into network %s",
                CommandConfig.Language.ZH_CN, "已插入 %s x %s 到网络 %s");

        register("network.giveTerminal.success",
                CommandConfig.Language.EN_US, "Given portable network terminal (bound to netId=%s, owner=%s) x%s",
                CommandConfig.Language.ZH_CN, "已给予便携网络终端 (绑定到 netId=%s, 所有者=%s) x%s");

        register("network.giveTerminal.not_exist",
                CommandConfig.Language.EN_US, "Network does not exist: netId=%s",
                CommandConfig.Language.ZH_CN, "网络不存在：netId=%s");

        register("error.server_not_available",
                CommandConfig.Language.EN_US, "Server not available.",
                CommandConfig.Language.ZH_CN, "服务器不可用。");

        register("error.player_required",
                CommandConfig.Language.EN_US, "This command must be run by a player.",
                CommandConfig.Language.ZH_CN, "此命令必须由玩家执行。");

        register("error.item_not_found",
                CommandConfig.Language.EN_US, "Portable network terminal item not found.",
                CommandConfig.Language.ZH_CN, "未找到便携网络终端物品。");
                
        register("network.list.all_title",
                CommandConfig.Language.EN_US, "=== All Dimension Networks ===",
                CommandConfig.Language.ZH_CN, "=== 所有维度网络 ===");
                
        register("network.list.page",
                CommandConfig.Language.EN_US, "Page %s",
                CommandConfig.Language.ZH_CN, "第 %s 页");
                
        register("network.list.previous",
                CommandConfig.Language.EN_US, "◀ Previous",
                CommandConfig.Language.ZH_CN, "◀ 上一页");
                
        register("network.list.next",
                CommandConfig.Language.EN_US, "Next ▶",
                CommandConfig.Language.ZH_CN, "下一页 ▶");
                
        register("network.list.page_with_total",
                CommandConfig.Language.EN_US, "Page %s/%s (Total: %s networks)",
                CommandConfig.Language.ZH_CN, "第 %s/%s 页 (总计: %s 个网络)");
    }

    private static void register(String key, CommandConfig.Language lang1, String text1, CommandConfig.Language lang2, String text2)
    {
        TRANSLATIONS.computeIfAbsent(key, k -> new HashMap<>()).put(lang1, text1);
        TRANSLATIONS.computeIfAbsent(key, k -> new HashMap<>()).put(lang2, text2);
    }

    public static String get(String key)
    {
        CommandConfig.Language lang = CommandConfig.getCommandLanguage();
        Map<CommandConfig.Language, String> langMap = TRANSLATIONS.get(key);
        if (langMap == null)
        {
            return key;
        }
        String text = langMap.get(lang);
        return text != null ? text : langMap.get(CommandConfig.Language.EN_US);
    }

    public static String get(String key, Object... args)
    {
        return String.format(get(key), args);
    }

    public static Component component(String key)
    {
        return Component.literal(get(key));
    }

    public static Component component(String key, Object... args)
    {
        return Component.literal(get(key, args));
    }
}
