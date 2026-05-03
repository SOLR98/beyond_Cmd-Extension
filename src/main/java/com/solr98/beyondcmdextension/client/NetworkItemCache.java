package com.solr98.beyondcmdextension.client;

import java.util.HashMap;
import java.util.Map;

public class NetworkItemCache {
    private static Map<String, Long> counts = new HashMap<>();
    private static boolean hasNetwork = true;

    public static void set(Map<String, Long> data) {
        counts.putAll(data);
    }

    public static void setAll(Map<String, Long> data, boolean hasNet) {
        counts = new HashMap<>(data);
        hasNetwork = hasNet;
    }

    public static boolean hasNetwork() {
        return hasNetwork;
    }

    public static long getCount(String itemKey) {
        return counts.getOrDefault(itemKey, 0L);
    }

    public static void clear() {
        counts.clear();
        hasNetwork = true;
    }

    public static boolean isEmpty() {
        return counts.isEmpty();
    }
}
