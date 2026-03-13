package com.rajoki.injuryplugin.systems.bodypartsystems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.rajoki.injuryplugin.components.BodyPartComponent;

import javax.annotation.Nonnull;

/**
 * Clamps stamina to 50% when torso is fractured.
 *
 * Also clears the Stamina_Broken (guard break) effect when stamina reaches
 * the clamped ceiling. Without this, guard break can never clear because its
 * recovery condition requires full stamina, which the clamp prevents. (Seems to have bugs
 * as sometimes guard break wasn't clearing and would have to wait till fracture was healed and
 * stamina went back to 100%. Removed it for now from plugin setup and may return later.
 */
public class TorsoFractureStaminaSystem extends DelayedEntitySystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final ComponentType<EntityStore, BodyPartComponent> bodyPartComponentType;

    private static final String GUARD_BREAK_EFFECT = "Stamina_Broken";

    public TorsoFractureStaminaSystem(ComponentType<EntityStore, BodyPartComponent> bodyPartComponentType) {
        super(0.1f); // Check every 0.1 seconds
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
            EntityStatMap stats = chunk.getComponent(index, EntityStatMap.getComponentType());
            BodyPartComponent bodyPartComp = chunk.getComponent(index, this.bodyPartComponentType);

            if (playerRef == null || stats == null || bodyPartComp == null) {
                return;
            }

            int staminaIdx = DefaultEntityStatTypes.getStamina();
            EntityStatValue staminaValue = stats.get(staminaIdx);

            if (staminaValue == null) {
                return;
            }

            boolean hasTorsoFracture = bodyPartComp.hasBodyPartEffect(BodyPart.TORSO, "FRACTURE");
            boolean isTorsoBroken = bodyPartComp.isBodyPartBroken(BodyPart.TORSO);
            float originalMaxStamina = staminaValue.getMax();
            float currentStamina = staminaValue.get();

            if (hasTorsoFracture || isTorsoBroken) {
                float clampedMax = originalMaxStamina * 0.5f;

                // Only clear guard break when stamina is POSITIVE and recovering
                // If stamina is negative, the guard break is legitimate (you're actually broken)
                // Once stamina regenerates back to positive, then we clear it
                if (currentStamina > 1) {


                    clearGuardBreakEffect(ref, store);

//                    {
//                        LOGGER.atInfo().log("[TORSO FRACTURE] Cleared guard break effect. " +
//                                "Stamina: " + currentStamina + "/" + clampedMax +
//                                " (Torso broken: " + isTorsoBroken + ")");
//                    }
                }

                // Clamp stamina if it's above the limit
                if (currentStamina > clampedMax) {
                    stats.setStatValue(staminaIdx, clampedMax);
                }

                if (!bodyPartComp.hasBodyPartEffect(BodyPart.TORSO, "STAMINA_CLAMPED")) {
                    bodyPartComp.addBodyPartEffect(BodyPart.TORSO, "STAMINA_CLAMPED");
                }

            } else {
                // No torso fracture/broken, remove clamp marker if it exists
                if (bodyPartComp.hasBodyPartEffect(BodyPart.TORSO, "STAMINA_CLAMPED")) {
                    bodyPartComp.removeBodyPartEffect(BodyPart.TORSO, "STAMINA_CLAMPED");
                }
            }

        } catch (Exception e) {
            LOGGER.atWarning().log("Error in TorsoFractureStaminaSystem: " + e.getMessage());
        }
    }

    /**
     * Removes the Stamina_Broken (guard break) effect if it is currently active.
     * Uses the same EffectControllerComponent pattern as HeadFractureEffectSystem.
     */
    private void clearGuardBreakEffect(Ref<EntityStore> ref, Store<EntityStore> store) {
        try {
            EffectControllerComponent effectController = store.getComponent(
                    ref, EffectControllerComponent.getComponentType());
            if (effectController == null) return;

            int effectIndex = EntityEffect.getAssetMap().getIndex(GUARD_BREAK_EFFECT);
            if (effectIndex == Integer.MIN_VALUE) return;

            if (effectController.getActiveEffects().containsKey(effectIndex)) {
                effectController.removeEffect(ref, effectIndex, store);
            }
        } catch (Exception e) {
            LOGGER.atWarning().log("Error clearing guard break effect: " + e.getMessage());
        }
    }
}