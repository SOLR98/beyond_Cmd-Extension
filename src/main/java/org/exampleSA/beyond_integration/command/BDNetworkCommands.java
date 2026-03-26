package org.exampleSA.beyond_integration.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.util.PlayerNameHelper;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.exampleSA.beyond_integration.Beyond_integration;
import org.exampleSA.beyond_integration.command.CommandLang;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = Beyond_integration.MODID)
public final class BDNetworkCommands
{
    private BDNetworkCommands()
    {
    }

    private static final int OP_LEVEL = 2;

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        CommandBuildContext context = event.getBuildContext();
        event.getDispatcher().register(
                Commands.literal("bdtools")
                        .requires(src -> src.hasPermission(OP_LEVEL))
                        .then(Commands.literal("network")
                                .then(Commands.literal("list")
                                        .executes(ctx -> listNets(ctx.getSource(), false, 1))
                                        .then(Commands.literal("all")
                                                .executes(ctx -> listNets(ctx.getSource(), true, 1))
                                        )
                                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                .executes(ctx -> listNets(ctx.getSource(), false, IntegerArgumentType.getInteger(ctx, "page")))
                                                .then(Commands.literal("all")
                                                        .executes(ctx -> listNets(ctx.getSource(), true, IntegerArgumentType.getInteger(ctx, "page")))
                                                )
                                        )
                                )
                                .then(Commands.literal("info")
                                        .then(Commands.argument("netId", IntegerArgumentType.integer(0))
                                                .executes(ctx -> infoNet(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId")))
                                        )
                                )
                                .then(Commands.literal("restore")
                                        .then(Commands.argument("netId", IntegerArgumentType.integer(0))
                                                .executes(ctx -> restoreNet(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId")))
                                        )
                                )
                                .then(Commands.literal("insert")
                                        .then(Commands.argument("netId", IntegerArgumentType.integer(0))
                                                .then(Commands.argument("item", ItemArgument.item(context))
                                                        .executes(ctx -> insertItem(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), ItemArgument.getItem(ctx, "item").createItemStack(1, false), 1L))
                                                        .then(Commands.argument("count", LongArgumentType.longArg(1, Long.MAX_VALUE))
                                                                .executes(ctx -> insertItem(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), ItemArgument.getItem(ctx, "item").createItemStack(1, false), LongArgumentType.getLong(ctx, "count")))
                                                        )
                                                )
                                        )
                                )
                                .then(Commands.literal("giveTerminal")
                                        .then(Commands.argument("netId", IntegerArgumentType.integer(0))
                                                .executes(ctx -> giveTerminal(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), 1))
                                                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> giveTerminal(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), IntegerArgumentType.getInteger(ctx, "count")))
                                                )
                                        )
                                )
                                .then(Commands.literal("generateItems")
                                        .then(Commands.argument("netId", IntegerArgumentType.integer(0))
                                                .executes(ctx -> generateItems(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), 100, 100, 300, false, false))
                                                .then(Commands.argument("typeCount", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> generateItems(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), IntegerArgumentType.getInteger(ctx, "typeCount"), 100, 300, false, false))
                                                        .then(Commands.argument("minAmount", IntegerArgumentType.integer(1))
                                                                .executes(ctx -> generateItems(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), IntegerArgumentType.getInteger(ctx, "typeCount"), IntegerArgumentType.getInteger(ctx, "minAmount"), 300, false, false))
                                                                .then(Commands.argument("maxAmount", IntegerArgumentType.integer(1))
                                                                        .executes(ctx -> generateItems(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), IntegerArgumentType.getInteger(ctx, "typeCount"), IntegerArgumentType.getInteger(ctx, "minAmount"), IntegerArgumentType.getInteger(ctx, "maxAmount"), false, false))
                                                                        .then(Commands.argument("withEnchantments", BoolArgumentType.bool())
                                                                                .executes(ctx -> generateItems(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), IntegerArgumentType.getInteger(ctx, "typeCount"), IntegerArgumentType.getInteger(ctx, "minAmount"), IntegerArgumentType.getInteger(ctx, "maxAmount"), BoolArgumentType.getBool(ctx, "withEnchantments"), false))
                                                                                .then(Commands.argument("withNbt", BoolArgumentType.bool())
                                                                                        .executes(ctx -> generateItems(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), IntegerArgumentType.getInteger(ctx, "typeCount"), IntegerArgumentType.getInteger(ctx, "minAmount"), IntegerArgumentType.getInteger(ctx, "maxAmount"), BoolArgumentType.getBool(ctx, "withEnchantments"), BoolArgumentType.getBool(ctx, "withNbt")))
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                                .then(Commands.literal("batchCreate")
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                .executes(ctx -> batchCreateNets(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count"), Long.MAX_VALUE, Integer.MAX_VALUE))
                                                .then(Commands.argument("slotCapacity", LongArgumentType.longArg(1))
                                                        .executes(ctx -> batchCreateNets(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count"), LongArgumentType.getLong(ctx, "slotCapacity"), Integer.MAX_VALUE))
                                                        .then(Commands.argument("slotMaxSize", IntegerArgumentType.integer(1))
                                                                .executes(ctx -> batchCreateNets(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count"), LongArgumentType.getLong(ctx, "slotCapacity"), IntegerArgumentType.getInteger(ctx, "slotMaxSize")))
                                                        )
                                                )
                                        )
                                )
                        )
        );
    }

    private static DimensionsNet getNetOrFail(CommandSourceStack source, int netId)
    {
        DimensionsNet net = DimensionsNet.getNetFromId(netId);
        if (net == null)
        {
            source.sendFailure(Component.literal("ID does not correspond to any network (or it was deleted)."));
        }
        return net;
    }

    private static int listNets(CommandSourceStack source, boolean showAll, int page)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
        {
            source.sendFailure(CommandLang.component("error.server_not_available"));
            return 0;
        }

        int maxPerPage = org.exampleSA.beyond_integration.CommandConfig.SERVER.maxNetworksPerPage.get();
        int startIndex = (page - 1) * maxPerPage;
        int endIndex = startIndex + maxPerPage;
        
        String titleKey = showAll ? "network.list.all_title" : "network.list.title";
        StringBuilder message = new StringBuilder(CommandLang.get(titleKey) + " (Page " + page + ")\n");
        int count = 0;
        int totalCount = 0;
        int displayedCount = 0;

        for (int netId = 0; netId < 10000; netId++)
        {
            DimensionsNet net = server.overworld().getDataStorage().get(DimensionsNet::load, "BDNet_" + netId);
            if (net != null)
            {
                if (!showAll && net.deleted) continue;
                
                totalCount++;
                
                if (totalCount > startIndex && displayedCount < maxPerPage)
                {
                    String ownerName = CommandLang.get("network.list.owner");
                    if (net.getOwner() != null)
                    {
                        ownerName = PlayerNameHelper.getPlayerNameByUUID(net.getOwner(), server);
                    }
                    message.append("Net ID: ").append(netId)
                            .append(" | ").append(CommandLang.get("network.list.owner")).append(": ").append(ownerName)
                            .append(" | ").append(CommandLang.get("network.list.players")).append(": ").append(net.getPlayers().size())
                            .append(" | ").append(CommandLang.get("network.list.managers")).append(": ").append(net.getManagers().size())
                            .append(net.deleted ? " | " + CommandLang.get("network.list.deleted_mark") + "\n" : "\n");
                    displayedCount++;
                }
                count++;
            }
        }

        if (displayedCount == 0)
        {
            source.sendSuccess(() -> Component.literal(showAll ? CommandLang.get("network.list.none_all") : CommandLang.get("network.list.none")), true);
        }
        else
        {
            int totalPages = (int) Math.ceil((double) totalCount / maxPerPage);
            
            MutableComponent pageInfo = Component.literal(CommandLang.get("network.list.page_info", page, totalPages, totalCount));
            
            MutableComponent navigation = Component.empty();
            
            if (page > 1)
            {
                navigation = navigation.append(
                    Component.literal("[◀ Previous]")
                        .withStyle(Style.EMPTY
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                                "/bdtools network list " + (page - 1) + (showAll ? " all" : "")))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                                Component.literal("Click to go to page " + (page - 1))))
                            .withColor(net.minecraft.ChatFormatting.GREEN)
                        )
                ).append(Component.literal(" "));
            }
            
            navigation = navigation.append(
                Component.literal("[Page " + page + "/" + totalPages + "]")
                    .withStyle(Style.EMPTY
                        .withColor(net.minecraft.ChatFormatting.YELLOW)
                    )
            );
            
            if (page < totalPages)
            {
                navigation = navigation.append(Component.literal(" ")).append(
                    Component.literal("[Next ▶]")
                        .withStyle(Style.EMPTY
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                                "/bdtools network list " + (page + 1) + (showAll ? " all" : "")))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                                Component.literal("Click to go to page " + (page + 1))))
                            .withColor(net.minecraft.ChatFormatting.GREEN)
                        )
                );
            }
            
            MutableComponent finalMessage = Component.literal(message.toString())
                .append(Component.literal("\n"))
                .append(pageInfo)
                .append(Component.literal("\n"))
                .append(navigation);
            
            source.sendSuccess(() -> finalMessage, true);
        }
        return count;
    }

    private static int infoNet(CommandSourceStack source, int netId)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
        {
            source.sendFailure(CommandLang.component("error.server_not_available"));
            return 0;
        }

        DimensionsNet net = server.overworld().getDataStorage().get(DimensionsNet::load, "BDNet_" + netId);
        if (net == null)
        {
            source.sendFailure(CommandLang.component("network.info.not_exist", netId));
            return 0;
        }

        String ownerName;
        if (net.getOwner() != null)
        {
            ownerName = PlayerNameHelper.getPlayerNameByUUID(net.getOwner(), server);
        }
        else
        {
            ownerName = CommandLang.get("network.info.unknown");
        }

        long slotCapacity = net.getUnifiedStorage().slotCapacity;
        int slotMaxSize = net.getUnifiedStorage().slotMaxSize;
        int currentTime = 0;
        try
        {
            java.lang.reflect.Field field = DimensionsNet.class.getDeclaredField("currentTime");
            field.setAccessible(true);
            currentTime = field.getInt(net);
        }
        catch (Exception e)
        {
            currentTime = -1;
        }

        StringBuilder message = new StringBuilder();
        message.append(CommandLang.get("network.info.title", netId)).append("\n");
        message.append(CommandLang.get("network.info.owner")).append(": ").append(ownerName).append("\n");
        message.append(CommandLang.get("network.info.status")).append(": ").append(
                net.deleted ? CommandLang.get("network.info.status.deleted") : CommandLang.get("network.info.status.active")
        ).append("\n");
        message.append(CommandLang.get("network.info.players")).append(": ").append(net.getPlayers().size()).append("\n");
        message.append(CommandLang.get("network.info.managers")).append(": ").append(net.getManagers().size()).append("\n");
        message.append(CommandLang.get("network.info.slot_capacity")).append(": ").append(slotCapacity).append("\n");
        message.append(CommandLang.get("network.info.slot_max_size")).append(": ").append(slotMaxSize).append("\n");
        message.append(CommandLang.get("network.info.current_time")).append(": ").append(currentTime).append("\n");

        source.sendSuccess(() -> Component.literal(message.toString()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int restoreNet(CommandSourceStack source, int netId)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
        {
            source.sendFailure(CommandLang.component("error.server_not_available"));
            return 0;
        }

        DimensionsNet net = server.overworld().getDataStorage().get(DimensionsNet::load, "BDNet_" + netId);
        if (net == null)
        {
            source.sendFailure(CommandLang.component("network.restore.not_exist", netId));
            return 0;
        }

        if (!net.deleted)
        {
            source.sendFailure(CommandLang.component("network.restore.not_deleted", netId));
            return 0;
        }

        net.setOwner(null);
        net.getPlayers().clear();
        net.getManagers().clear();
        net.deleted = false;
        net.setDirty();

        source.sendSuccess(
                () -> Component.literal(CommandLang.get("network.restore.success", netId)),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int insertItem(CommandSourceStack source, int netId, ItemStack itemStack, long count)
    {
        DimensionsNet net = getNetOrFail(source, netId);
        if (net == null) return 0;

        ItemStackKey key = new ItemStackKey(itemStack);
        var remainder = net.getUnifiedStorage().insert(key, count, false);

        long inserted = count - remainder.amount();

        source.sendSuccess(
                () -> Component.literal(CommandLang.get("network.insert.success", inserted, itemStack.getHoverName().getString(), netId)),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int giveTerminal(CommandSourceStack source, int netId, int count)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
        {
            source.sendFailure(CommandLang.component("error.server_not_available"));
            return 0;
        }

        DimensionsNet net = server.overworld().getDataStorage().get(DimensionsNet::load, "BDNet_" + netId);
        if (net == null)
        {
            source.sendFailure(CommandLang.component("network.giveTerminal.not_exist", netId));
            return 0;
        }

        ServerPlayer player = source.getPlayer();
        if (player == null)
        {
            source.sendFailure(CommandLang.component("error.player_required"));
            return 0;
        }

        Item portableTerminal = ForgeRegistries.ITEMS.getValue(
                new ResourceLocation(BDConstants.MODID, "net_terminal_item")
        );
        if (portableTerminal == null)
        {
            source.sendFailure(CommandLang.component("error.item_not_found"));
            return 0;
        }

        ItemStack terminalStack = new ItemStack(portableTerminal, count);
        com.wintercogs.beyonddimensions.common.item.NetedItem.setNetId(terminalStack, netId);

        final String ownerName;
        if (net.getOwner() != null)
        {
            ownerName = PlayerNameHelper.getPlayerNameByUUID(net.getOwner(), server);
        }
        else
        {
            ownerName = "Unknown";
        }

        terminalStack.setHoverName(Component.literal(ownerName + " " + CommandLang.get("network.list.owner")));

        if (!player.getInventory().add(terminalStack))
        {
            player.drop(terminalStack, false);
        }

        source.sendSuccess(
                () -> Component.literal(CommandLang.get("network.giveTerminal.success", netId, ownerName, count)),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int generateItems(CommandSourceStack source, int netId, int typeCount, int minAmount, int maxAmount, boolean withEnchantments, boolean withNbt)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
        {
            source.sendFailure(CommandLang.component("error.server_not_available"));
            return 0;
        }

        DimensionsNet net = server.overworld().getDataStorage().get(DimensionsNet::load, "BDNet_" + netId);
        if (net == null)
        {
            source.sendFailure(CommandLang.component("network.info.not_exist", netId));
            return 0;
        }

        List<Item> allItems = BuiltInRegistries.ITEM.stream()
                .filter(item -> item != Items.AIR)
                .collect(Collectors.toList());

        List<Enchantment> allEnchantments = BuiltInRegistries.ENCHANTMENT.stream()
                .collect(Collectors.toList());

        Random random = new Random();
        Collections.shuffle(allItems, random);

        int count = Math.min(typeCount, allItems.size());
        int totalInserted = 0;

        for (int i = 0; i < count; i++)
        {
            Item item = allItems.get(i);
            int amount = minAmount + random.nextInt(maxAmount - minAmount + 1);

            ItemStack itemStack = new ItemStack(item, amount);

            if (withEnchantments)
            {
                addRandomEnchantments(itemStack, allEnchantments, random);
            }

            if (withNbt)
            {
                addRandomNbt(itemStack, random);
            }

            ItemStackKey stack = new ItemStackKey(itemStack);
            var remainder = net.getUnifiedStorage().insert(stack, amount, false);
            totalInserted += (amount - remainder.amount());
        }

        final int finalTotalInserted = totalInserted;
        source.sendSuccess(
                () -> Component.literal(CommandLang.get("network.generateItems.success", count, finalTotalInserted, netId)),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private static void addRandomEnchantments(ItemStack itemStack, List<Enchantment> allEnchantments, Random random)
    {
        CompoundTag tag = itemStack.getOrCreateTag();
        ListTag enchantmentsList = new ListTag();

        int enchantmentCount = 1 + random.nextInt(4);

        for (int i = 0; i < enchantmentCount; i++)
        {
            Enchantment enchantment = allEnchantments.get(random.nextInt(allEnchantments.size()));
            int level = 1 + random.nextInt(enchantment.getMaxLevel());

            CompoundTag enchantmentTag = new CompoundTag();
            enchantmentTag.putString("id", BuiltInRegistries.ENCHANTMENT.getKey(enchantment).toString());
            enchantmentTag.putShort("lvl", (short) level);
            enchantmentsList.add(enchantmentTag);
        }

        tag.put("Enchantments", enchantmentsList);
        itemStack.setTag(tag);
    }

    private static void addRandomNbt(ItemStack itemStack, Random random)
    {
        CompoundTag tag = itemStack.getOrCreateTag();

        if (random.nextBoolean())
        {
            tag.putString("CustomName", "{\"text\":\"Random Item " + random.nextInt(1000) + "\"}");
        }

        if (random.nextBoolean())
        {
            tag.putBoolean("Unbreakable", true);
        }

        if (random.nextDouble() < 0.3)
        {
            CompoundTag display = tag.getCompound("display");
            int color = random.nextInt(16777216);
            display.putInt("color", color);
            tag.put("display", display);
        }

        if (random.nextDouble() < 0.2)
        {
            ListTag lore = new ListTag();
            lore.add(StringTag.valueOf("Lore line " + random.nextInt(10)));
            tag.put("Lore", lore);
        }

        itemStack.setTag(tag);
    }

    private static int batchCreateNets(CommandSourceStack source, int count, long slotCapacity, int slotMaxSize)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
        {
            source.sendFailure(CommandLang.component("error.server_not_available"));
            return 0;
        }

        int createdCount = 0;
        List<Integer> createdNetIds = new java.util.ArrayList<>();

        for (int i = 0; i < count; i++)
        {
            String netIdStr = DimensionsNet.buildNewNetName(server);
            int netId = Integer.parseInt(netIdStr.replace("BDNet_", ""));

            DimensionsNet newNet = server.overworld().getDataStorage().computeIfAbsent(DimensionsNet::load, DimensionsNet::create, netIdStr);
            newNet.setId(netId);
            newNet.setOwner(null);
            newNet.getPlayers().clear();
            newNet.getManagers().clear();
            newNet.getUnifiedStorage().setSlotCapacity(slotCapacity);
            newNet.getUnifiedStorage().setSlotMaxSize(slotMaxSize);
            newNet.setDirty();

            createdCount++;
            createdNetIds.add(netId);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(CommandLang.get("network.batchCreate.success", createdCount));
        if (createdCount <= 10)
        {
            sb.append(" (");
            for (int i = 0; i < createdNetIds.size(); i++)
            {
                if (i > 0) sb.append(", ");
                sb.append(createdNetIds.get(i));
            }
            sb.append(")");
        }
        else
        {
            sb.append(" (").append(createdNetIds.get(0)).append(" - ").append(createdNetIds.get(createdNetIds.size() - 1)).append(")");
        }

        source.sendSuccess(() -> Component.literal(sb.toString()), true);
        return createdCount;
    }
}
