package com.rajoki.injuryplugin.ui.firstaidui;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BandageSelectionUIRegistry {
    private static final ConcurrentHashMap<UUID, BandageSelectionPageNew> activePages = new ConcurrentHashMap<>();

    public static void register(PlayerRef playerRef, BandageSelectionPageNew page) {
        activePages.put(playerRef.getUuid(), page);
    }

    public static void unregister(PlayerRef playerRef) {
        activePages.remove(playerRef.getUuid());
    }

    public static BandageSelectionPageNew get(PlayerRef playerRef) {
        return activePages.get(playerRef.getUuid());
    }
}

// Class used to update UI elements while open