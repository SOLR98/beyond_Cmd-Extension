package com.solr98.beyondcmdextension.client;

import com.solr98.beyondcmdextension.network.PacketHandler;
import com.solr98.beyondcmdextension.network.RequestAmmoCountPacket;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class AmmoCountCache {

    private static final Map<String, Map<Integer, Integer>> cache = new HashMap<>();
    private static final Set<String> responded = new HashSet<>();
    private static String pendingQuickId = null;
    private static String pendingFullId = null;
    private static long lastFullUpdateMs = 0;
    private static final long FULL_INTERVAL_MS = 6000;

    public static boolean hasData(ResourceLocation ammoId) {
        return responded.contains(ammoId.toString());
    }

    public static int getCount(ResourceLocation ammoId) {
        Map<Integer, Integer> nets = cache.get(ammoId.toString());
        if (nets == null) return 0;
        return nets.values().stream().mapToInt(Integer::intValue).sum();
    }

    public static Map<Integer, Integer> getAllNetworks(ResourceLocation ammoId) {
        return cache.getOrDefault(ammoId.toString(), Collections.emptyMap());
    }

    public static void requestQuick(ResourceLocation ammoId) {
        String id = ammoId.toString();
        if (id.equals(pendingQuickId)) return;
        pendingQuickId = id;
        PacketHandler.sendToServer(new RequestAmmoCountPacket(ammoId, true));
    }

    public static void requestFull(ResourceLocation ammoId) {
        long now = System.currentTimeMillis();
        if (now - lastFullUpdateMs < FULL_INTERVAL_MS) return;
        String id = ammoId.toString();
        if (id.equals(pendingFullId)) return;
        pendingFullId = id;
        lastFullUpdateMs = now;
        PacketHandler.sendToServer(new RequestAmmoCountPacket(ammoId, false));
    }

    public static void update(ResourceLocation ammoId, Map<Integer, Integer> networkCounts, boolean quick) {
        cache.put(ammoId.toString(), new LinkedHashMap<>(networkCounts));
        if (quick) {
            pendingQuickId = null;
        } else {
            pendingFullId = null;
        }
        responded.add(ammoId.toString());
    }

    public static void clear() {
        cache.clear();
        responded.clear();
        pendingQuickId = null;
        pendingFullId = null;
        lastFullUpdateMs = 0;
    }
}
