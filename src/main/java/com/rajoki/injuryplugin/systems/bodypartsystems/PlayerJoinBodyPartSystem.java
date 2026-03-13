package com.rajoki.injuryplugin.systems.bodypartsystems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.rajoki.injuryplugin.components.BodyPartComponent;
import com.rajoki.injuryplugin.ui.hud.BodyPartHudManager;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Adds and initializes BodyPartComponent for players when they join the server
 */
public class PlayerJoinBodyPartSystem extends RefSystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final ComponentType<EntityStore, BodyPartComponent> bodyPartComponentType;

    public PlayerJoinBodyPartSystem(ComponentType<EntityStore, BodyPartComponent> bodyPartComponentType) {
        this.bodyPartComponentType = bodyPartComponentType;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Override
    public void onEntityAdded(@Nonnull Ref<EntityStore> ref,
                              @Nonnull AddReason reason,
                              @Nonnull Store<EntityStore> store,
                              @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            LOGGER.atWarning().log("PlayerJoinBodyPartSystem triggered on NON-PLAYER entity!");
            return;
        }

        // Check if component already exists (loaded from persistence)
        BodyPartComponent existing = store.getComponent(ref, bodyPartComponentType);

        if (existing != null && existing.isInitialized()) {
            // Component exists from save data, just log it
            LOGGER.atInfo().log(String.format(
                    "[RETURNING PLAYER] %s - Loaded body parts from save data",
                    playerRef.getUsername()
            ));

//            LOGGER.atInfo().log(String.format(
//                    "  Head: %.1f/%.1f, Torso: %.1f/%.1f",
//                    existing.getBodyPartHealth(BodyPart.HEAD),
//                    existing.getBodyPartMaxHealth(BodyPart.HEAD),
//                    existing.getBodyPartHealth(BodyPart.TORSO),
//                    existing.getBodyPartMaxHealth(BodyPart.TORSO)
//            ));

            // Log effects if any
            for (BodyPart part : BodyPart.values()) {
                if (!existing.getBodyPartEffects(part).isEmpty()) {
                    LOGGER.atInfo().log(String.format(
                            "  %s has effects: %s",
                            part.getDisplayName(),
                            existing.getBodyPartEffects(part)
                    ));
                }
            }

            return; // Don't reinitialize!
        }

        // Component doesn't exist or isn't initialized = create new one
        LOGGER.atInfo().log(String.format(
                "[NEW PLAYER] %s - Initializing fresh body part data",
                playerRef.getUsername()
        ));

        EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
        if (stats == null) {
            LOGGER.atWarning().log("Could not get EntityStatMap for player");
            return;
        }

        int healthIdx = DefaultEntityStatTypes.getHealth();
        EntityStatValue healthValue = stats.get(healthIdx);
        if (healthValue == null) {
            LOGGER.atWarning().log("Could not get health value for player");
            return;
        }

        float playerMaxHealth = healthValue.getMax();
        float playerCurrentHealth = healthValue.get();

        // Create new component
        BodyPartComponent bodyPart = new BodyPartComponent();

        // Initialize all body parts
        for (BodyPart part : BodyPart.values()) {
            float partMaxHealth = part.getMaxHealth(playerMaxHealth);
            bodyPart.setBodyPartMaxHealth(part, partMaxHealth);

            float healthPercentage = playerMaxHealth > 0 ? (playerCurrentHealth / playerMaxHealth) : 1.0f;
            float partCurrentHealth = partMaxHealth * healthPercentage;
            bodyPart.setBodyPartHealth(part, partCurrentHealth);

            LOGGER.atInfo().log(String.format(
                    "  Initialized %s: %.1f / %.1f",
                    part.getDisplayName(), partCurrentHealth, partMaxHealth
            ));
        }

        bodyPart.setInitialized(true);

        // Use commandBuffer.addComponent() instead of store.putComponent()?
        commandBuffer.addComponent(ref, bodyPartComponentType, bodyPart);

//        LOGGER.atInfo().log(String.format(
//                "[INITIALIZED] %s - Body parts ready and will persist",
//                playerRef.getUsername()
//        ));
    }

    @Override
    public void onEntityRemove(@Nonnull Ref<EntityStore> ref,
                               @Nonnull RemoveReason reason,
                               @Nonnull Store<EntityStore> store,
                               @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        // Persistence via CODEC handles saving automatically
        try {
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef != null) {
                BodyPartComponent bodyPart = store.getComponent(ref, this.bodyPartComponentType);
                if (bodyPart != null && bodyPart.isInitialized()) {
                    LOGGER.atInfo().log(String.format(
                            "[PLAYER LOGOUT] %s - Body part data will auto-save via persistence",
                            playerRef.getUsername()
                    ));

                    // Remove HUD from registry on logout
                    UUID uuid = playerRef.getUuid();
                    BodyPartHudManager.removeHud(uuid);
                    LOGGER.atInfo().log(String.format(
                            "[PLAYER LOGOUT] %s - Removed HUD from registry",
                            playerRef.getUsername()
                    ));
                }
            }
        } catch (Exception e) {
            LOGGER.atWarning().log("Error in onEntityRemove: " + e.getMessage());
        }
    }
}