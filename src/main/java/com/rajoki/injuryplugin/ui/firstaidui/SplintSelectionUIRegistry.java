package com.rajoki.injuryplugin.ui.firstaidui;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SplintSelectionUIRegistry {
    private static final ConcurrentHashMap<UUID, SplintSelectionPageNew> activePages = new ConcurrentHashMap<>();

    public static void register(PlayerRef playerRef, SplintSelectionPageNew page) {
        activePages.put(playerRef.getUuid(), page);
    }

    public static void unregister(PlayerRef playerRef) {
        activePages.remove(playerRef.getUuid());
    }

    public static SplintSelectionPageNew get(PlayerRef playerRef) {
        return activePages.get(playerRef.getUuid());
    }
}