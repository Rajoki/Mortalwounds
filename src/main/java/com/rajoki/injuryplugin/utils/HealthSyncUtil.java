package com.rajoki.injuryplugin.utils;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.rajoki.injuryplugin.MortalWoundsPlugin;
import com.rajoki.injuryplugin.components.BodyPartComponent;
import com.rajoki.injuryplugin.config.MortalWoundsConfig;
import com.rajoki.injuryplugin.systems.bodypartsystems.BodyPart;

// Helps with syncing between body part health and player actual health in Hytale

public class HealthSyncUtil {

    /**
     * Heal a broken limb by removing broken status and adding 1 HP to player
     * The BodyPartRecoverySystem will automatically sync body parts to match player health
     */
    public static void healBrokenLimb(Ref<EntityStore> ref, Store<EntityStore> store,
                                      BodyPartComponent bodyPartComp, BodyPart part) {
        // Get EntityStatMap to access actual player health
        EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
        if (stats == null) return;

        int healthIdx = DefaultEntityStatTypes.getHealth();
        EntityStatValue healthValue = stats.get(healthIdx);
        if (healthValue == null) return;

        float currentPlayerHealth = healthValue.get();
        float maxHealth = healthValue.getMax();

        // Remove broken status (allows natural healing)
        bodyPartComp.setBodyPartBroken(part, false);

        // Remove DESTROYED effect if present
        bodyPartComp.removeBodyPartEffect(part, "DESTROYED");

        // Set limb to 0 HP (still broken but repairable)
        // BodyPartRecoverySystem will heal it when player health increases
        bodyPartComp.setBodyPartHealth(part, 0f);

        // Add 1 HP to player's actual health
        float newPlayerHealth = Math.min(currentPlayerHealth + 1.0f, maxHealth);
        stats.setStatValue(healthIdx, newPlayerHealth);

        // Mark component dirty so recovery system processes it
        bodyPartComp.markDirty();

        if (MortalWoundsConfig.get().enableDebugLogging) {
            MortalWoundsPlugin.getPluginLogger().atInfo().log(
                    String.format("[HEAL] Repaired %s, added 1 HP to player: %.1f -> %.1f",
                            part.getDisplayName(), currentPlayerHealth, newPlayerHealth)
            );
        }
    }
}