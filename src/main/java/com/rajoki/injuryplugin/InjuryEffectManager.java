package com.rajoki.injuryplugin;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.ActiveEntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;
import java.util.concurrent.ThreadLocalRandom;

public class InjuryEffectManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // Cached effects
    private EntityEffect cachedBleedEffect;
    private EntityEffect cachedHeavyBleedEffect;
    private EntityEffect cachedFractureEffect;
    private EntityEffect cachedArmFractureEffect;

    /**
     * Determines which effect to apply based on the roll value and existing effects
     * @param roll The random number rolled (1-100)
     * @param victimRef Reference to the victim entity
     * @param componentAccessor Component accessor to check existing effects
     * @return InjuryResult containing the effect to apply and the message
     */
    public InjuryResult determineEffect(Damage damage, int roll, Ref<EntityStore> victimRef, ComponentAccessor<EntityStore> componentAccessor) {
        InjuryConfig config = InjuryConfig.get();


        if (roll < config.fractureChance) {
            int fractureRoll = ThreadLocalRandom.current().nextInt(1, 101);
            if (fractureRoll < 50) {
                return new InjuryResult(
                        getArmFractureEffect(),
                        config.showMessages ? " - ARM FRACTURE!" : "",
                        null
                );
            }
            else {
            return new InjuryResult(
                    getFractureEffect(),
                    config.showMessages ? " - LEG FRACTURE!" : "",
                    null
            ); }
        } else if (roll < config.bleedChance) {
            // Check if player already has Bleed effect
            if (hasEffect(victimRef, componentAccessor, "Bleed")) {
                // Upgrade to HeavyBleed and remove regular Bleed
                return new InjuryResult(
                        getHeavyBleedEffect(),
                        config.showMessages ? " - HEAVY BLEED!" : "",
                        "Bleed"
                );
            } else {
                return new InjuryResult(
                        getBleedEffect(),
                        config.showMessages ? " - BLEEDING!" : "",
                        null
                );
            }
        }






        return new InjuryResult(null, "", null);
    }

    /**
     * Checks if an entity has a specific effect active
     * @param entityRef Reference to the entity
     * @param componentAccessor Component accessor
     * @param effectId The effect ID to check for
     * @return true if the entity has the effect, false otherwise
     */
    private boolean hasEffect(Ref<EntityStore> entityRef, ComponentAccessor<EntityStore> componentAccessor, String effectId) {
        EffectControllerComponent effectController = componentAccessor.getComponent(entityRef, EffectControllerComponent.getComponentType());

        if (effectController == null) {
            return false;
        }

        int effectIndex = EntityEffect.getAssetMap().getIndex(effectId);
        if (effectIndex == Integer.MIN_VALUE) {
            return false;
        }

        ActiveEntityEffect activeEffect = effectController.getActiveEffects().get(effectIndex);
        return activeEffect != null;
    }

    // Loading getters for each effect
    private EntityEffect getBleedEffect() {
        if (cachedBleedEffect == null) {
            cachedBleedEffect = EntityEffect.getAssetMap().getAsset("Bleed");
            if (cachedBleedEffect == null) {
                LOGGER.atInfo().log("Bleed effect not found in asset map!");
            }
        }
        return cachedBleedEffect;
    }

    private EntityEffect getHeavyBleedEffect() {
        if (cachedHeavyBleedEffect == null) {
            cachedHeavyBleedEffect = EntityEffect.getAssetMap().getAsset("HeavyBleed");
            if (cachedHeavyBleedEffect == null) {
                LOGGER.atInfo().log("HeavyBleed effect not found in asset map!");
            }
        }
        return cachedHeavyBleedEffect;
    }

    private EntityEffect getFractureEffect() {
        if (cachedFractureEffect == null) {
            cachedFractureEffect = EntityEffect.getAssetMap().getAsset("Fracture");
            if (cachedFractureEffect == null) {
                LOGGER.atInfo().log("Fracture effect not found in asset map!");
            }
        }
        return cachedFractureEffect;
    }

    private EntityEffect getArmFractureEffect() {
        if (cachedArmFractureEffect == null) {
            cachedArmFractureEffect = EntityEffect.getAssetMap().getAsset("Fracture_Arm");
            if (cachedArmFractureEffect == null) {
                LOGGER.atInfo().log("Arm Fracture effect not found in asset map!");
            }
        }
        return cachedArmFractureEffect;
    }

    /**
     * Result class to hold the effect to apply, message, and effect to remove
     */
    public static class InjuryResult {
        @Nullable
        private final EntityEffect effect;
        private final String message;
        @Nullable
        private final String effectToRemove;

        public InjuryResult(@Nullable EntityEffect effect, String message, @Nullable String effectToRemove) {
            this.effect = effect;
            this.message = message;
            this.effectToRemove = effectToRemove;
        }

        @Nullable
        public EntityEffect getEffect() {
            return effect;
        }

        public String getMessage() {
            return message;
        }

        @Nullable
        public String getEffectToRemove() {
            return effectToRemove;
        }

        public boolean hasEffect() {
            return effect != null;
        }

        public boolean shouldRemoveEffect() {
            return effectToRemove != null;
        }
    }





}