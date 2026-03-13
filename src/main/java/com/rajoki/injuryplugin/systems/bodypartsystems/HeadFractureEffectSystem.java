package com.rajoki.injuryplugin.systems.bodypartsystems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.rajoki.injuryplugin.MortalWoundsPlugin;
import com.rajoki.injuryplugin.components.BodyPartComponent;

import javax.annotation.Nonnull;

/**
 * Applies Head_Fracture_Effect (tunnel vision) when head is fractured
 * Similar to TorsoFractureStaminaSystem but for visual effects
 */
public class HeadFractureEffectSystem extends DelayedEntitySystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final ComponentType<EntityStore, BodyPartComponent> bodyPartComponentType;

    private static final String EFFECT_NAME = "Head_Fracture_Effect";

    public HeadFractureEffectSystem(ComponentType<EntityStore, BodyPartComponent> bodyPartComponentType) {
        super(1.0f); // Check every 0.1 seconds
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
            BodyPartComponent bodyPartComp = chunk.getComponent(index, this.bodyPartComponentType);

            if (playerRef == null || bodyPartComp == null) {
                return;
            }

            boolean hasHeadFracture = bodyPartComp.hasBodyPartEffect(BodyPart.HEAD, "FRACTURE");

            // Get effect controller
            EffectControllerComponent effectController = store.getComponent(ref,
                    EffectControllerComponent.getComponentType());

            if (effectController == null) {
                return;
            }

            // Get effect index
            int effectIndex = EntityEffect.getAssetMap().getIndex(EFFECT_NAME);
            if (effectIndex == Integer.MIN_VALUE) {
                // Effect not found in asset map - only log once
                return;
            }

            boolean hasEffect = effectController.getActiveEffects().containsKey(effectIndex);

            if (hasHeadFracture) {
                // HEAD has fracture = ensure effect is applied
                if (!hasEffect) {
                    EntityEffect headFractureEffect = EntityEffect.getAssetMap().getAsset(effectIndex);
                    if (headFractureEffect != null) {
                        effectController.addEffect(ref, headFractureEffect, store);

//                        LOGGER.atInfo().log(String.format(
//                                "[HEAD FRACTURE] Applied visual effect to %s",
//                                playerRef.getUsername()
//                        ));
                    }
                }

            } else {
                // No HEAD fracture = ensure effect is removed
                if (hasEffect) {
                    effectController.removeEffect(ref, effectIndex, store);

//                    LOGGER.atInfo().log(String.format(
//                            "[HEAD FRACTURE] Removed visual effect from %s",
//                            playerRef.getUsername()
//                    ));
                }
            }

        } catch (Exception e) {
            LOGGER.atWarning().log("Error in HeadFractureEffectSystem: " + e.getMessage());
        }
    }
}