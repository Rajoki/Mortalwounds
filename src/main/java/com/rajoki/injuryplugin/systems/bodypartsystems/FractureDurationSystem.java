package com.rajoki.injuryplugin.systems.bodypartsystems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.rajoki.injuryplugin.components.BodyPartComponent;
import com.rajoki.injuryplugin.config.MortalWoundsConfig;

import javax.annotation.Nonnull;
import java.util.*;

public class FractureDurationSystem extends DelayedEntitySystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final ComponentType<EntityStore, BodyPartComponent> bodyPartComponentType;

    // Track fracture timers per player per body part

    private final Map<UUID, Map<BodyPart, Float>> playerFractureTimers = new HashMap<>();

    public FractureDurationSystem(ComponentType<EntityStore, BodyPartComponent> bodyPartComponentType) {
        super(1.0f); // Tick every second
        this.bodyPartComponentType = bodyPartComponentType;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), this.bodyPartComponentType);
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        // Skip if fracture duration is disabled
        if (!MortalWoundsConfig.get().enableFractureDuration) {
            return;
        }

        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        BodyPartComponent bodyPartComp = chunk.getComponent(index, this.bodyPartComponentType);

        if (playerRef == null || bodyPartComp == null || !bodyPartComp.isInitialized()) {
            return;
        }

        // Skip if player is dead
        if (chunk.getArchetype().contains(DeathComponent.getComponentType())) {
            return;
        }

        UUID playerId = playerRef.getUuid();
        Map<BodyPart, Float> playerFractures = playerFractureTimers
                .computeIfAbsent(playerId, k -> new EnumMap<>(BodyPart.class));

        // Process each body part
        for (BodyPart part : BodyPart.values()) {
            boolean hasFracture = bodyPartComp.hasBodyPartEffect(part, "FRACTURE");
            Float fractureTime = playerFractures.get(part);

            if (hasFracture) {
                // Initialize timer if new fracture
                if (fractureTime == null) {
                    playerFractures.put(part, 0f);
//                    LOGGER.atInfo().log(String.format("[FRACTURE STARTED] %s will heal in %d seconds",
//                            part.getDisplayName(), MortalWoundsConfig.get().fractureDurationSeconds));
                } else {
                    // Increment timer
                    float newTime = fractureTime + dt;
                    playerFractures.put(part, newTime);

                    // Check if fracture should auto-heal
                    if (newTime >= MortalWoundsConfig.get().fractureDurationSeconds) {
                        bodyPartComp.removeBodyPartEffect(part, "FRACTURE");
                        playerFractures.remove(part);

//                        playerRef.sendMessage(Message.raw(
//                                String.format("%s fracture healed!", part.getDisplayName())
//                        ));

//                        LOGGER.atInfo().log(String.format("[FRACTURE HEALED] %s auto-healed after %.1f seconds",
//                                part.getDisplayName(), newTime));
                    }
                }
            } else {
                // Clean up timer if fracture was removed externally (healed by splint)
                if (fractureTime != null) {
                    playerFractures.remove(part);
//                    LOGGER.atInfo().log(String.format("[FRACTURE CLEARED] %s fracture removed externally",
//                            part.getDisplayName()));
                }
            }
        }

        // Clean up empty player entries
        if (playerFractures.isEmpty()) {
            playerFractureTimers.remove(playerId);
        }
    }
}