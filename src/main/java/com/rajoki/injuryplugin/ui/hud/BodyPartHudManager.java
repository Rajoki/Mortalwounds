package com.rajoki.injuryplugin.ui.hud;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.rajoki.injuryplugin.MortalWoundsPlugin;
import com.rajoki.injuryplugin.components.BodyPartComponent;
import com.rajoki.injuryplugin.ui.providers.HudCompatibilityManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BodyPartHudManager {
    private static final Map<UUID, BodyPartHud> activeHuds = new ConcurrentHashMap<>();

    public static void showHud(Player player, Store<EntityStore> store, Ref<EntityStore> ref) {
        UUID uuid = player.getUuid();

        //System.out.println("[HUD MANAGER] showHud() called for " + player.getPlayerRef().getUsername() + " (" + uuid + ")");

        // Remove existing HUD if present
        if (activeHuds.containsKey(uuid)) {
            //System.out.println("[HUD MANAGER] Removing existing HUD first");
            removeHud(uuid);
        }

        // Get body part data
        BodyPartComponent bodyPartData = store.getComponent(ref,
                MortalWoundsPlugin.getInstance().getBodyPartComponentType());

        if (bodyPartData == null) {
            System.out.println("[HUD MANAGER] ERROR: BodyPartComponent is null!");
            return;
        }

        if (!bodyPartData.isInitialized()) {
            System.out.println("[HUD MANAGER] ERROR: BodyPartComponent not initialized!");
            return;
        }

        // Create new HUD
        //System.out.println("[HUD MANAGER] Creating new BodyPartHud...");
        PlayerRef playerRef = player.getPlayerRef();
        BodyPartHud hud = new BodyPartHud(playerRef);
        hud.setBodyPartData(bodyPartData);

        // Use compatibility manager to add HUD (works with MultipleHUD/AutoMultiHud)
        System.out.println("[HUD MANAGER] Registering with HudCompatibilityManager...");
        HudCompatibilityManager.get().setCustomHud(player, playerRef, "MortalWounds_BodyPartHud", hud);

        activeHuds.put(uuid, hud);
        System.out.println("[HUD MANAGER] HUD registered successfully. Active HUDs: " + activeHuds.size());

        MortalWoundsPlugin.getPluginLogger().atInfo().log("Registered HUD for " + playerRef.getUsername());
    }

    public static void removeHud(UUID uuid) {
        System.out.println("[HUD MANAGER] removeHud() called for " + uuid);
        BodyPartHud hud = activeHuds.remove(uuid);
        if (hud != null) {
            System.out.println("[HUD MANAGER] Removed HUD from registry");
            hud.show();
            BodyPartHud.removeFromRegistry(uuid);
        } else {
            System.out.println("[HUD MANAGER] No HUD found to remove");
        }
    }

    public static void removeHud(PlayerRef playerRef) {
        removeHud(playerRef.getUuid());
    }

    public static BodyPartHud getHud(UUID uuid) {
        BodyPartHud hud = activeHuds.get(uuid);
        // System.out.println("[HUD MANAGER] getHud(" + uuid + ") = " + (hud != null ? "found" : "null"));
        return hud;
    }

    public static boolean hasHud(UUID uuid) {
        return activeHuds.containsKey(uuid);
    }

    public static void cleanup() {
        activeHuds.values().forEach(BodyPartHud::show);
        activeHuds.clear();
    }

    public static void refreshHud(Player player, Store<EntityStore> store, Ref<EntityStore> ref) {
        BodyPartHud hud = activeHuds.get(player.getUuid());
        if (hud != null) {
            BodyPartComponent bodyPartData = store.getComponent(ref,
                    MortalWoundsPlugin.getInstance().getBodyPartComponentType());
            if (bodyPartData != null) {
                hud.refresh(bodyPartData);
            }
        }
    }
}