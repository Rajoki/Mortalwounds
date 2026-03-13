package com.rajoki.injuryplugin.ui.gui;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.rajoki.injuryplugin.MortalWoundsPlugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BodyPartUIRegistry {
    private static final Map<PlayerRef, BodyPartStatsUI> activeUIs = new ConcurrentHashMap<>();

    public static void register(PlayerRef playerRef, BodyPartStatsUI ui) {
        activeUIs.put(playerRef, ui);
        MortalWoundsPlugin.getPluginLogger().atInfo().log("Registered BodyPartStatsUI for player: " + playerRef.getUuid());
    }

    public static void unregister(PlayerRef playerRef) {
        activeUIs.remove(playerRef);
        MortalWoundsPlugin.getPluginLogger().atInfo().log("Unregistered BodyPartStatsUI for player: " + playerRef.getUuid());
    }

    public static BodyPartStatsUI get(PlayerRef playerRef) {
        return activeUIs.get(playerRef);
    }

    public static Map<PlayerRef, BodyPartStatsUI> getAll() {
        return activeUIs;
    }
}