package com.rajoki.injuryplugin.systems.bodypartsystems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.protocol.MovementSettings;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.rajoki.injuryplugin.components.BodyPartComponent;
import com.rajoki.injuryplugin.config.MortalWoundsConfig;

import javax.annotation.Nonnull;

public class LegFractureMovementSystem extends DelayedEntitySystem<EntityStore> {

   //System to reduce movement speed depending on leg fracture and how many legs are fractured for players.

    private final ComponentType<EntityStore, BodyPartComponent> bodyPartComponentType;

    public LegFractureMovementSystem(ComponentType<EntityStore, BodyPartComponent> bodyPartComponentType) {
        super(0.2f);
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

        Ref<EntityStore> ref = chunk.getReferenceTo(index);

        PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null) return;

        BodyPartComponent bodyPartComp = chunk.getComponent(index, this.bodyPartComponentType);
        if (bodyPartComp == null) return;

        MovementManager movementManager = store.getComponent(ref, MovementManager.getComponentType());
        if (movementManager == null) return;

        // Check how many legs are fractured
        boolean leftLegFractured = bodyPartComp.hasBodyPartEffect(BodyPart.LEFTLEG, "FRACTURE");
        boolean rightLegFractured = bodyPartComp.hasBodyPartEffect(BodyPart.RIGHTLEG, "FRACTURE");

        MovementSettings settings = movementManager.getSettings();
        MovementSettings defaults = movementManager.getDefaultSettings();

        float speedReduction = 0f;

        if (leftLegFractured && rightLegFractured) {
            // Both legs fractured / use config value
            speedReduction = MortalWoundsConfig.get().legFractureSpeedReduction2Legs;
        } else if (leftLegFractured || rightLegFractured) {
            // One leg fractured / use config value
            speedReduction = MortalWoundsConfig.get().legFractureSpeedReduction1Leg;
        }

        float multiplier = 1.0f - speedReduction;

        // Apply speed multiplier
        settings.baseSpeed = defaults.baseSpeed * multiplier;
        settings.forwardWalkSpeedMultiplier = defaults.forwardWalkSpeedMultiplier * multiplier;
        settings.backwardWalkSpeedMultiplier = defaults.backwardWalkSpeedMultiplier * multiplier;
        settings.strafeWalkSpeedMultiplier = defaults.strafeWalkSpeedMultiplier * multiplier;
        settings.forwardRunSpeedMultiplier = defaults.forwardRunSpeedMultiplier * multiplier;
        settings.backwardRunSpeedMultiplier = defaults.backwardRunSpeedMultiplier * multiplier;
        settings.strafeRunSpeedMultiplier = defaults.strafeRunSpeedMultiplier * multiplier;
        settings.forwardSprintSpeedMultiplier = defaults.forwardSprintSpeedMultiplier * multiplier;
        settings.forwardCrouchSpeedMultiplier = defaults.forwardCrouchSpeedMultiplier * multiplier;
        settings.backwardCrouchSpeedMultiplier = defaults.backwardCrouchSpeedMultiplier * multiplier;
        settings.strafeCrouchSpeedMultiplier = defaults.strafeCrouchSpeedMultiplier * multiplier;

        movementManager.update(playerRef.getPacketHandler());
    }
}