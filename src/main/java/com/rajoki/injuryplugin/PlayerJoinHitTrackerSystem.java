package com.rajoki.injuryplugin;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class PlayerJoinHitTrackerSystem extends RefSystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final ComponentType<EntityStore, HitTrackerComponent> hitTrackerComponentType;

    public PlayerJoinHitTrackerSystem(ComponentType<EntityStore, HitTrackerComponent> hitTrackerComponentType) {
        this.hitTrackerComponentType = hitTrackerComponentType;
    }

    @Nonnull
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason reason,
                              @Nonnull Store<EntityStore> store,
                              @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        HitTrackerComponent existing = commandBuffer.getComponent(ref, this.hitTrackerComponentType);
        if (existing == null) {
            commandBuffer.addComponent(ref, this.hitTrackerComponentType, new HitTrackerComponent());
            LOGGER.atInfo().log("Added HitTrackerComponent to joining player");
        }
    }

    public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason,
                               @Nonnull Store<EntityStore> store,
                               @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        // No cleanup needed
    }
}