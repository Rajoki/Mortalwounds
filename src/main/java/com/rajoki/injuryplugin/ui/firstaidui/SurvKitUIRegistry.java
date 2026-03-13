package com.rajoki.injuryplugin.ui.firstaidui;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SurvKitUIRegistry {
    private static final ConcurrentHashMap<UUID, SurvKitSelectionPageNew> activePages = new ConcurrentHashMap<>();

    public static void register(PlayerRef playerRef,SurvKitSelectionPageNew page) {
        activePages.put(playerRef.getUuid(), page);
    }

    public static void unregister(PlayerRef playerRef) {
        activePages.remove(playerRef.getUuid());
    }

    public static SurvKitSelectionPageNew get(PlayerRef playerRef) {
        return activePages.get(playerRef.getUuid());
    }
}