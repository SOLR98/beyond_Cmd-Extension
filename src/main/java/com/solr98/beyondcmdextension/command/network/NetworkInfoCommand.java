package com.solr98.beyondcmdextension.command.network;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import com.solr98.beyondcmdextension.command.CommandLang;
import com.solr98.beyondcmdextension.command.util.*;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;

/**
 * 网络信息命令
 * 功能：显示网络详细信息
 */
public class NetworkInfoCommand {
    
    /**
     * 注册命令
     */
    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("info")
            // 默认：显示当前玩家的主要网络信息
            .executes(ctx -> execute(ctx, -1, null, false))
            // 指定网络ID，当前玩家查看
            .then(Commands.argument("netId", IntegerArgumentType.integer(0, 9999))
                .executes(ctx -> execute(ctx, IntegerArgumentType.getInteger(ctx, "netId"), null, false))
                // 指定网络ID和玩家
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> execute(ctx, IntegerArgumentType.getInteger(ctx, "netId"), 
                            EntityArgument.getPlayer(ctx, "player"), false))
                )
                // OP手动触发NBT计算
                .then(Commands.literal("nbt")
                    .requires(source -> CommandUtils.hasOpPermission(source))
                    .executes(ctx -> execute(ctx, IntegerArgumentType.getInteger(ctx, "netId"), null, true))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> execute(ctx, IntegerArgumentType.getInteger(ctx, "netId"), 
                                EntityArgument.getPlayer(ctx, "player"), true))
                    )
                )
            )
            // OP手动触发NBT计算（当前网络）
            .then(Commands.literal("nbt")
                .requires(source -> CommandUtils.hasOpPermission(source))
                .executes(ctx -> execute(ctx, -1, null, true))
            );
    }
    
    /**
     * 执行命令
     */
    public static int execute(CommandContext<CommandSourceStack> ctx, int netId, ServerPlayer targetPlayer) {
        return execute(ctx, netId, targetPlayer, false);
    }
    
    /**
     * 执行命令（带NBT计算标志）
     */
    public static int execute(CommandContext<CommandSourceStack> ctx, int netId, ServerPlayer targetPlayer, boolean calculateNbt) {
        CommandSourceStack source = ctx.getSource();
        
        // 检查服务器是否可用
        if (!PermissionChecker.checkServerAvailable(source)) {
            return 0;
        }
        
        // 获取服务器
        net.minecraft.server.MinecraftServer server = source.getServer();
        if (server == null) {
            source.sendFailure(OutputFormatter.createError("error.server_not_available"));
            return 0;
        }
        
        // 获取执行者玩家
        ServerPlayer executor = source.getPlayer();
        if (executor == null) {
            source.sendFailure(OutputFormatter.createError("error.player_required"));
            return 0;
        }
        
        // 如果未指定网络ID，使用玩家的主要网络
        if (netId == -1) {
            DimensionsNet primaryNet = DimensionsNet.getPrimaryNetFromPlayer(executor);
            if (primaryNet == null) {
                source.sendFailure(OutputFormatter.createError("error.not_in_network"));
                return 0;
            }
            netId = primaryNet.getId();
        }
        
        // 检查网络是否存在
        DimensionsNet net = PermissionChecker.checkNetworkExists(source, netId);
        if (net == null) {
            return 0;
        }
        
        // 检查权限（查看其他玩家需要OP权限）
        ServerPlayer playerToCheck = targetPlayer != null ? targetPlayer : executor;
        if (targetPlayer != null && !executor.getUUID().equals(targetPlayer.getUUID())) {
            if (!PermissionChecker.checkOpPermissionForOthers(source, targetPlayer)) {
                return 0;
            }
        }
        
        // 检查玩家是否有权限查看网络
        if (!PermissionChecker.checkNetworkAccessPermission(source, net, playerToCheck)) {
            return 0;
        }
        
        // 检查是否是OP手动触发NBT计算
        boolean shouldCalculateNbt = calculateNbt && CommandUtils.hasOpPermission(source);
        
        // 构建网络信息消息
        MutableComponent message = buildNetworkInfoMessage(net, playerToCheck, server, shouldCalculateNbt);
        
        source.sendSuccess(() -> message, false);
        return 1;
    }
    
    /**
     * 构建网络信息消息
     */
    public static MutableComponent buildNetworkInfoMessage(DimensionsNet net, ServerPlayer player, 
                                                          net.minecraft.server.MinecraftServer server) {
        return buildNetworkInfoMessage(net, player, server, false);
    }
    
    /**
     * 构建网络信息消息（带NBT计算标志）
     */
    public static MutableComponent buildNetworkInfoMessage(DimensionsNet net, ServerPlayer player, 
                                                          net.minecraft.server.MinecraftServer server, boolean calculateNbt) {
        MutableComponent message = Component.empty();
        
        // 添加标题
        message = message.append(OutputFormatter.createTitle("network.info.title", net.getId()))
                .append(Component.literal("\n"));
        
        // 获取网络统计信息
        NetworkUtils.NetworkStats stats = NetworkUtils.getNetworkStats(net);
        
        // 获取玩家权限级别
        String permissionLevel = NetworkUtils.getPlayerPermissionLevel(player, net);
        String permissionDisplay = NetworkUtils.getPermissionLevelDisplay(permissionLevel);
        
        // 获取网络所有者名称
        String ownerName = CommandUtils.getNetworkOwnerName(net, server);
        
        // 添加所有者信息和状态
        message = message.append(Component.literal(CommandLang.get("network.info.owner_label", ownerName)))
                .append(Component.literal(net.deleted ? 
                        CommandLang.get("network.info.status.deleted") : 
                        CommandLang.get("network.info.status.active"))
                        .withStyle(net.deleted ? ChatFormatting.RED : ChatFormatting.GREEN))
                .append(Component.literal(CommandLang.get("network.info.your_permission_label")))
                .append(Component.literal(permissionDisplay)
                        .withStyle(getPermissionColor(permissionLevel)))
                .append(Component.literal("\n"));
        
        // 添加结晶生成剩余时间
        int remainingTime = NetworkUtils.getCrystalRemainingTime(net);
        message = message.append(Component.literal(CommandLang.get("network.info.crystal_time")))
                .append(OutputFormatter.createHoverableTime(remainingTime))
                .append(Component.literal("\n"));
        
        // 添加槽位信息
        long slotCapacity = net.getUnifiedStorage().slotCapacity;
        int slotMaxSize = net.getUnifiedStorage().slotMaxSize;
        
        message = message.append(Component.literal(CommandLang.get("network.info.slot_capacity_label")))
                .append(OutputFormatter.createHoverableNumber(slotCapacity, 
                        CommandLang.get("network.info.slot_capacity_label")))
                .append(Component.literal(CommandLang.get("network.info.slot_count_label")))
                .append(OutputFormatter.createHoverableNumber(slotMaxSize, 
                        CommandLang.get("network.info.slot_count_label")))
                .append(Component.literal("\n"));
        
        // 添加存储统计标题
        message = message.append(Component.literal(CommandLang.get("network.info.storage_stats") + "\n"));
        
        // 添加物品统计
        if (stats.itemTypes > 0) {
            message = message.append(Component.literal(CommandLang.get("network.info.items_label")))
                    .append(OutputFormatter.createHoverableResourceType(stats.itemTypes, 
                            CommandLang.get("network.info.items_label").trim()))
                    .append(Component.literal(CommandLang.get("network.info.types_suffix")))
                    .append(OutputFormatter.createHoverableItemCount(stats.itemTotal))
                    .append(Component.literal("\n"));
        }
        
        // 添加流体统计
        if (stats.fluidTypes > 0) {
            message = message.append(Component.literal(CommandLang.get("network.info.fluids_label")))
                    .append(OutputFormatter.createHoverableResourceType(stats.fluidTypes, 
                            CommandLang.get("network.info.fluids_label").trim()))
                    .append(Component.literal(CommandLang.get("network.info.types_suffix")))
                    .append(OutputFormatter.createHoverableFluid(stats.fluidTotal))
                    .append(Component.literal(" mB\n"));
        }
        
        // 添加能量统计
        if (stats.energyTypes > 0) {
            message = message.append(Component.literal(CommandLang.get("network.info.energy_label")))
                    .append(OutputFormatter.createHoverableResourceType(stats.energyTypes, 
                            CommandLang.get("network.info.energy_label").trim()))
                    .append(Component.literal(CommandLang.get("network.info.types_suffix")))
                    .append(OutputFormatter.createHoverableEnergy(stats.energyTotal))
                    .append(Component.literal(" FE\n"));
        }
        
        // 如果没有资源
        if (stats.getTotalTypes() == 0) {
            message = message.append(Component.literal("  无资源")
                    .withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("\n"));
        }
        
        // 添加玩家统计
        int playerCount = net.getPlayers().size();
        int managerCount = net.getManagers().size();
        
        message = message.append(Component.literal(CommandLang.get("network.info.player_count_label")))
                .append(OutputFormatter.createHoverableNumber(playerCount, 
                        CommandLang.get("network.info.player_count_label")))
                .append(Component.literal(CommandLang.get("network.info.manager_count_label")))
                .append(OutputFormatter.createHoverableNumber(managerCount, 
                        CommandLang.get("network.info.manager_count_label")))
                .append(Component.literal("\n"));
        
        // 添加玩家列表
        message = message.append(Component.literal(CommandLang.get("network.info.player_list_label")));
        
        NetworkUtils.PlayerList playerList = NetworkUtils.getNetworkPlayerList(net, server);
        if (playerList.hasPlayers()) {
            message = message.append(OutputFormatter.createPlayerList(playerList));
        } else {
            message = message.append(Component.literal(CommandLang.get("network.info.no_players"))
                    .withStyle(ChatFormatting.GRAY));
        }
        
            // 如果需要计算NBT大小，显示NBT分析信息
            if (calculateNbt) {
                message = message.append(Component.literal("\n"));
                message = message.append(Component.literal(CommandLang.get("network.info.nbt_section_title"))
                        .withStyle(ChatFormatting.GOLD))
                        .append(Component.literal("\n"));
                
                // 计算NBT大小
                NbtSizeInfo nbtInfo = calculateNetworkNbtSize(net);
                
                if (nbtInfo.calculationFailed) {
                    message = message.append(Component.literal("  " + CommandLang.get("network.info.nbt_calculation_failed"))
                            .withStyle(ChatFormatting.RED))
                            .append(Component.literal("\n"));
                } else if (nbtInfo.hasNbtData()) {
                    String formattedSize = formatFileSize(nbtInfo.totalNbtSize);
                    String uniqueItemsCount = formatLargeNumber(nbtInfo.uniqueItemsWithNbt);
                    String totalItemsCount = formatLargeNumber(nbtInfo.itemsWithNbt);
                    String exactByteSize = formatLargeNumber(nbtInfo.totalNbtSize) + " bytes";
                    
                    // 创建带有hover text的NBT大小显示
                    MutableComponent nbtSizeLine = Component.literal("  " + 
                            CommandLang.get("network.info.nbt_size", uniqueItemsCount, formattedSize))
                            .withStyle(ChatFormatting.YELLOW);
                    
                    // 添加hover text显示详细信息
                    MutableComponent hoverText = Component.literal(CommandLang.get("network.info.nbt_hover_title") + "\n")
                            .withStyle(ChatFormatting.GOLD)
                            .append(Component.literal(CommandLang.get("network.info.nbt_hover_unique_types") + uniqueItemsCount + "\n")
                                    .withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(CommandLang.get("network.info.nbt_hover_total_items") + totalItemsCount + "\n")
                                    .withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal(CommandLang.get("network.info.nbt_hover_exact_size") + exactByteSize + "\n")
                                    .withStyle(ChatFormatting.GREEN))
                            .append(Component.literal(CommandLang.get("network.info.nbt_hover_formatted_size") + formattedSize)
                                    .withStyle(ChatFormatting.LIGHT_PURPLE))
                            .append(Component.literal(CommandLang.get("network.info.nbt_hover_note"))
                                    .withStyle(ChatFormatting.GRAY));
                    
                    nbtSizeLine = nbtSizeLine.withStyle(nbtSizeLine.getStyle()
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    hoverText)));
                    
                    message = message.append(nbtSizeLine)
                            .append(Component.literal("\n"));
                    
                    // 如果数据过大，显示警告
                    if (nbtInfo.isLargeData() || nbtInfo.largeDataWarning) {
                        message = message.append(Component.literal("  " + 
                                CommandLang.get("network.info.nbt_large_warning"))
                                .withStyle(ChatFormatting.RED))
                                .append(Component.literal("\n"));
                    }
                } else {
                    message = message.append(Component.literal("  " + CommandLang.get("network.info.no_nbt_data"))
                            .withStyle(ChatFormatting.GRAY))
                            .append(Component.literal("\n"));
                }
                
                // 添加说明：NBT计算只包括有NBT标签的物品
                message = message.append(Component.literal(CommandLang.get("network.info.nbt_note"))
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC))
                        .append(Component.literal("\n"));
            }
        
        return message;
    }
    
    /**
     * 计算网络存储中所有物品的NBT大小（按照种类计算）
     * 能够处理大量数据，包括1GB级别的NBT数据
     */
    private static NbtSizeInfo calculateNetworkNbtSize(DimensionsNet net) {
        NbtSizeInfo info = new NbtSizeInfo();
        
        try {
            if (net == null || net.deleted) {
                return info;
            }
            
            // 获取网络存储
            var storage = net.getUnifiedStorage();
            if (storage == null) {
                return info;
            }
            
            // 使用Map来按物品种类统计NBT
            Map<String, ItemNbtStats> itemStatsMap = new HashMap<>();
            
            // 遍历存储中的所有物品
            for (var keyAmount : storage.getStorage()) {
                var key = keyAmount.key();
                long amount = keyAmount.amount();
                
                // 只处理物品类型的键
                if (key instanceof ItemStackKey itemKey) {
                    ItemStack stack = itemKey.getReadOnlyStack();
                    
                    // 检查物品是否有NBT
                    if (!stack.isEmpty()) {
                        // 检查是否有NBT标签
                        boolean hasNbt = stack.hasTag();
                        CompoundTag tag = stack.getTag();
                        
                        // 检查是否有真正的NBT数据（不只是空标签）
                        boolean hasRealNbt = hasNbt && tag != null && !tag.isEmpty();
                        
                        if (hasRealNbt) {
                            // 生成物品种类标识（物品ID + NBT哈希）
                            String itemKeyId = generateItemKey(stack, tag);
                            
                            // 获取或创建该种类的统计
                            ItemNbtStats stats = itemStatsMap.get(itemKeyId);
                            if (stats == null) {
                                stats = new ItemNbtStats();
                                stats.itemName = stack.getDisplayName().getString();
                                stats.itemCount = 0;
                                stats.nbtSizePerItem = estimateNbtSize(tag);
                                stats.nbtHash = tag.hashCode();
                                itemStatsMap.put(itemKeyId, stats);
                                info.uniqueItemsWithNbt++;
                            }
                            
                            // 更新统计
                            stats.itemCount += amount;
                            
                            // NBT大小只计算一次（按物品种类），不乘以物品数量
                            // 因为相同种类的物品共享相同的NBT数据
                            if (stats.itemCount == amount) { // 第一次遇到这种物品
                                info.totalNbtSize += stats.nbtSizePerItem;
                            }
                            
                            // 检查是否超过1GB（仅用于警告）
                            if (!info.largeDataWarning && info.totalNbtSize > 1073741824L) { // 1GB
                                info.largeDataWarning = true;
                            }
                        }
                    }
                }
            }
            
            // 计算总物品数量（所有带有NBT的物品）
            for (ItemNbtStats stats : itemStatsMap.values()) {
                info.itemsWithNbt += stats.itemCount;
            }
            
            // 保存种类统计信息
            info.itemStatsMap = itemStatsMap;
            
        } catch (Exception e) {
            // 计算失败，返回空信息
            info.calculationFailed = true;
            info.debugInfo = "Calculation failed: " + e.getMessage();
            System.out.println("[DEBUG] NBT calculation error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return info;
    }
    
    /**
     * 生成物品种类标识
     */
    private static String generateItemKey(ItemStack stack, CompoundTag tag) {
        // 使用物品ID和NBT哈希来标识同一种物品
        return stack.getItem().toString() + ":" + tag.hashCode();
    }
    
    /**
     * 估算NBT标签的大小（字节）
     * 使用简化的估算方法
     */
    private static long estimateNbtSize(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return 0;
        }
        
        // 简化的估算方法
        // 对于测试环境，NBT可能包含额外数据，使用保守估算
        long size = 0;
        
        for (String key : tag.getAllKeys()) {
            var value = tag.get(key);
            
            // 基础开销：类型(1) + 名称长度(2) + 名称
            size += 1 + 2 + key.length();
            
            // 根据值类型估算大小
            if (value instanceof CompoundTag compound) {
                // 复合标签：递归计算 + 结束标签(1)
                size += estimateNbtSize(compound) + 1;
            } else if (value instanceof net.minecraft.nbt.ListTag list) {
                // 列表标签：类型(1) + 长度(4) + 元素
                size += 1 + 4;
                for (var element : list) {
                    if (element instanceof CompoundTag compound) {
                        size += estimateNbtSize(compound);
                    } else {
                        size += 8; // 基本类型
                    }
                }
            } else if (value instanceof net.minecraft.nbt.IntArrayTag) {
                size += 4 + (tag.getIntArray(key).length * 4L);
            } else if (value instanceof net.minecraft.nbt.LongArrayTag) {
                size += 4 + (tag.getLongArray(key).length * 8L);
            } else if (value instanceof net.minecraft.nbt.ByteArrayTag) {
                size += 4 + tag.getByteArray(key).length;
            } else if (value instanceof net.minecraft.nbt.StringTag) {
                String str = tag.getString(key);
                size += 2 + str.length(); // UTF-8，假设ASCII
            } else if (value instanceof net.minecraft.nbt.IntTag) {
                size += 4;
            } else if (value instanceof net.minecraft.nbt.LongTag) {
                size += 8;
            } else if (value instanceof net.minecraft.nbt.DoubleTag) {
                size += 8;
            } else if (value instanceof net.minecraft.nbt.FloatTag) {
                size += 4;
            } else if (value instanceof net.minecraft.nbt.ShortTag) {
                size += 2;
            } else if (value instanceof net.minecraft.nbt.ByteTag) {
                size += 1;
            }
        }
        
        return size;
    }
    
    /**
     * 格式化文件大小
     */
    private static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.1f %s", bytes / Math.pow(1024, exp), pre);
    }
    
    /**
     * 格式化大数字（添加千位分隔符）
     */
    private static String formatLargeNumber(long number) {
        if (number < 1000) {
            return String.valueOf(number);
        }
        
        // 使用千位分隔符
        return String.format("%,d", number);
    }
    
    /**
     * 物品NBT统计类
     */
    private static class ItemNbtStats {
        String itemName;
        long itemCount;
        long nbtSizePerItem;
        int nbtHash;
    }
    
    /**
     * NBT大小信息类
     */
    private static class NbtSizeInfo {
        long itemsWithNbt = 0; // 使用long以支持大量物品
        long totalNbtSize = 0; // 字节
        int uniqueItemsWithNbt = 0; // 不同种类的物品数量
        boolean largeDataWarning = false; // 数据过大警告
        boolean calculationFailed = false; // 计算失败标志
        Map<String, ItemNbtStats> itemStatsMap = new HashMap<>(); // 按种类统计
        String debugInfo = ""; // 调试信息
        
        boolean hasNbtData() {
            return itemsWithNbt > 0;
        }
        
        boolean isLargeData() {
            return totalNbtSize > 1073741824L; // 大于1GB
        }
    }
    
    /**
     * 获取权限颜色
     */
    public static ChatFormatting getPermissionColor(String permissionLevel) {
        switch (permissionLevel) {
            case "owner":
                return ChatFormatting.RED;
            case "manager":
                return ChatFormatting.BLUE;
            case "member":
                return ChatFormatting.GREEN;
            default:
                return ChatFormatting.GRAY;
        }
    }
}