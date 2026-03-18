package com.rajoki.injuryplugin.systems.bodypartsystems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.protocol.GameMode;
import com.rajoki.injuryplugin.components.BodyPartComponent;

import javax.annotation.Nonnull;


//Torso fractured = stamina drains at x speed when using abilities
// This gives you effectively 50% stamina without breaking the guard break system
// There was a bug with clamping stamina with guard break and multi-hit attacks
public class TorsoFractureStaminaSystem extends DelayedEntitySystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final ComponentType<EntityStore, BodyPartComponent> bodyPartComponentType;

    private static final float DRAIN_MULTIPLIER = 1.5f; // x stamina drain when fractured

    public TorsoFractureStaminaSystem(ComponentType<EntityStore, BodyPartComponent> bodyPartComponentType) {
        super(0.05f); // Tick 20 times per second for smooth drain
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
        try {
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
            Player player = chunk.getComponent(index, Player.getComponentType());
            EntityStatMap stats = chunk.getComponent(index, EntityStatMap.getComponentType());
            BodyPartComponent bodyPartComp = chunk.getComponent(index, this.bodyPartComponentType);

            if (playerRef == null || player == null || stats == null || bodyPartComp == null) {
                return;
            }

            // Skip in Creative mode
            GameMode gameMode = player.getGameMode();
            if (gameMode == GameMode.Creative) {
                return;
            }

            int staminaIdx = DefaultEntityStatTypes.getStamina();
            EntityStatValue staminaValue = stats.get(staminaIdx);

            if (staminaValue == null) {
                return;
            }

            boolean hasTorsoFracture = bodyPartComp.hasBodyPartEffect(BodyPart.TORSO, "FRACTURE");
            float currentStamina = staminaValue.get();
            float maxStamina = staminaValue.getMax();

            if (hasTorsoFracture) {
                // Check if stamina is actively draining (being used)
                // If stamina is below max and not at zero, it's either draining or regenerating
                // We detect usage by checking if it decreased since last tick

                // Store previous stamina value in component
                Float previousStamina = bodyPartComp.getCustomFloat("PREV_STAMINA");

                if (previousStamina != null && previousStamina > currentStamina && currentStamina > 0) {
                    // Stamina decreased = player is using it
                    // Apply additional drain (multiply the drain)
                    float drainAmount = previousStamina - currentStamina;
                    float extraDrain = drainAmount * (DRAIN_MULTIPLIER - 1.0f); // Additional drain

                    float newStamina = Math.max(0, currentStamina - extraDrain);
                    stats.setStatValue(staminaIdx, newStamina);
                }

                // Store current stamina for next tick
                bodyPartComp.setCustomFloat("PREV_STAMINA", currentStamina);

                // Mark that torso is fractured
                if (!bodyPartComp.hasBodyPartEffect(BodyPart.TORSO, "STAMINA_PENALTY")) {
                    bodyPartComp.addBodyPartEffect(BodyPart.TORSO, "STAMINA_PENALTY");
                }

            } else {
                // No torso fracture = clean up
                if (bodyPartComp.hasBodyPartEffect(BodyPart.TORSO, "STAMINA_PENALTY")) {
                    bodyPartComp.removeBodyPartEffect(BodyPart.TORSO, "STAMINA_PENALTY");
                }
                bodyPartComp.removeCustomFloat("PREV_STAMINA");
            }

        } catch (Exception e) {
            LOGGER.atWarning().log("Error in TorsoFractureStaminaSystem: " + e.getMessage());
        }
    }
}