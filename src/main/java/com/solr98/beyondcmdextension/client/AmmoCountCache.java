package com.solr98.beyondcmdextension.client;

import com.solr98.beyondcmdextension.network.PacketHandler;
import com.solr98.beyondcmdextension.network.RequestAmmoCountPacket;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class AmmoCountCache {

    private static final Map<String, Integer> cache = new HashMap<>();
    private static String pendingAmmoId = null;

    public static boolean hasData(ResourceLocation ammoId) {
        return cache.containsKey(ammoId.toString());
    }

    public static int getCount(ResourceLocation ammoId) {
        return cache.getOrDefault(ammoId.toString(), 0);
    }

    public static void requestAndGet(ResourceLocation ammoId) {
        String id = ammoId.toString();
        if (id.equals(pendingAmmoId)) return;
        pendingAmmoId = id;
        PacketHandler.sendToServer(new RequestAmmoCountPacket(ammoId));
    }

    public static void update(ResourceLocation ammoId, int count) {
        cache.put(ammoId.toString(), count);
        pendingAmmoId = null;
    }

    public static void clear() {
        cache.clear();
        pendingAmmoId = null;
    }
}
