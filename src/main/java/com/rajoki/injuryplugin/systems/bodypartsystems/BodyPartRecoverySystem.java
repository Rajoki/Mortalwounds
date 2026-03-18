package com.rajoki.injuryplugin.systems.bodypartsystems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.rajoki.injuryplugin.components.BodyPartComponent;
import com.rajoki.injuryplugin.config.MortalWoundsConfig;
import com.rajoki.injuryplugin.MortalWoundsPlugin;
import com.rajoki.injuryplugin.ui.gui.BodyPartStatsUI;
import com.rajoki.injuryplugin.ui.gui.BodyPartUIRegistry;

import javax.annotation.Nonnull;

/**
 * Runs periodically to:
 * 1. Update body part max health when player's max health changes (armor equip/unequip)
 * 2. Heal body parts proportionally when player's health increases
 */
public class BodyPartRecoverySystem extends DelayedEntitySystem<EntityStore> {

    private final ComponentType<EntityStore, BodyPartComponent> bodyPartComponentType;

    public BodyPartRecoverySystem(ComponentType<EntityStore, BodyPartComponent> bodyPartComponentType) {
        super(0.5f); // Run every 0.5 seconds
        this.bodyPartComponentType = bodyPartComponentType;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(this.bodyPartComponentType);
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        try {
            EntityStatMap stats = chunk.getComponent(index, EntityStatMap.getComponentType());
            BodyPartComponent bodyPartComp = chunk.getComponent(index, this.bodyPartComponentType);

            if (stats == null || bodyPartComp == null || !bodyPartComp.isInitialized()) {
                return;
            }

            int healthIdx = DefaultEntityStatTypes.getHealth();
            EntityStatValue healthValue = stats.get(healthIdx);
            if (healthValue == null) return;

            float currentMaxHealth = healthValue.getMax();
            float currentHealth = healthValue.get();

            // Update body part max health if needed
            updateBodyPartMaxHealth(bodyPartComp, currentMaxHealth);

            // Heal body parts proportionally
            boolean healed = healBodyParts(bodyPartComp, currentHealth, currentMaxHealth, dt);

            // If healing occurred, update the BodyPartStatsUI immediately
            if (healed) {
                PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
                if (playerRef != null) {
                    BodyPartStatsUI ui = BodyPartUIRegistry.get(playerRef);
                    if (ui != null) {
                        Ref<EntityStore> ref = chunk.getReferenceTo(index);
                        ui.refresh(ref, store);
                    }
                }
            }

        } catch (Exception e) {
            MortalWoundsPlugin.getPluginLogger().atWarning().log("Error in BodyPartRecoverySystem: " + e.getMessage());
        }
    }

    /**
     * Update body part max health when player's overall max health changes
     * (e.g., from equipping/unequipping armor)
     */
    private void updateBodyPartMaxHealth(BodyPartComponent bodyPartComp, float newMaxHealth) {
        // Calculate what the total max health SHOULD be based on body part percentages
        float expectedTotal = 0f;
        for (BodyPart part : BodyPart.values()) {
            expectedTotal += bodyPartComp.getBodyPartMaxHealth(part);
        }

        // If there's a significant difference, recalculate all body part max healths
        if (Math.abs(expectedTotal - newMaxHealth) > 0.1f) {
            if (MortalWoundsConfig.get().enableDebugLogging) {
                MortalWoundsPlugin.getPluginLogger().atInfo().log(
                        String.format("Max health changed from %.1f to %.1f - updating body parts",
                                expectedTotal, newMaxHealth)
                );
            }

            boolean maxHealthIncreased = newMaxHealth > expectedTotal;

            // Recalculate max health for each body part based on new total
            for (BodyPart part : BodyPart.values()) {
                float oldMax = bodyPartComp.getBodyPartMaxHealth(part);
                float newMax = part.getMaxHealth(newMaxHealth);
                float currentHealth = bodyPartComp.getBodyPartHealth(part);

                // Set new max
                bodyPartComp.setBodyPartMaxHealth(part, newMax);

                // ONLY scale current health DOWN when max decreases (armor removed)
                // When max INCREASES (armor equipped), keep current health the same
                if (!maxHealthIncreased) {
                    // Max health decreased = scale current health down proportionally
                    float damageRatio = (oldMax > 0) ? (currentHealth / oldMax) : 1.0f;
                    float newHealth = Math.min(newMax, newMax * damageRatio);
                    bodyPartComp.setBodyPartHealth(part, newHealth);
                } else {
                    // Max health increased = just clamp to new max (don't scale up)
                    float newHealth = Math.min(newMax, currentHealth);
                    bodyPartComp.setBodyPartHealth(part, newHealth);
                }

                if (MortalWoundsConfig.get().enableDebugLogging) {
                    MortalWoundsPlugin.getPluginLogger().atInfo().log(
                            String.format("%s: %.1f/%.1f -> %.1f/%.1f",
                                    part.getDisplayName(), currentHealth, oldMax,
                                    bodyPartComp.getBodyPartHealth(part), newMax)
                    );
                }
            }
        }
    }

    /**
     * Heal body parts proportionally when player's health increases
     * BROKEN parts cannot heal naturally. They require repair items
     */
    private boolean healBodyParts(BodyPartComponent bodyPartComp, float currentHealth, float maxHealth, float dt) {
        boolean healedSomething = false;

        // Total current health of ALL parts
        float totalCurrentHealth = 0f;
        for (BodyPart part : BodyPart.values()) {
            totalCurrentHealth += bodyPartComp.getBodyPartHealth(part);
        }

        float healthDifference = currentHealth - totalCurrentHealth;

        // Only heal if player health is HIGHER than body part total
        // This prevents "fixing" desyncs caused by bleed damage
        if (healthDifference <= 0.01f) return false;

        // Calculate total missing health across ALL parts (excluding broken parts)
        float totalMissingHealth = 0f;
        for (BodyPart part : BodyPart.values()) {
            // SKIP broken/destroyed limbs - they can't heal naturally
            if (bodyPartComp.isBodyPartBroken(part)) {
                continue;
            }

            float missing = bodyPartComp.getBodyPartMaxHealth(part) - bodyPartComp.getBodyPartHealth(part);
            if (missing > 0) totalMissingHealth += missing;
        }

        if (totalMissingHealth <= 0.01f) return false;

        float healingToDistribute = Math.min(healthDifference, totalMissingHealth);

        // Heal all NON-BROKEN parts proportionally
        for (BodyPart part : BodyPart.values()) {
            // SKIP broken/destroyed limbs, they can't heal naturally
            if (bodyPartComp.isBodyPartBroken(part)) {
                continue;
            }

            float partMax = bodyPartComp.getBodyPartMaxHealth(part);
            float partCurrent = bodyPartComp.getBodyPartHealth(part);
            float missing = partMax - partCurrent;

            if (missing > 0) {
                float healAmount = (missing / totalMissingHealth) * healingToDistribute;
                float newHealth = Math.min(partMax, partCurrent + healAmount);
                bodyPartComp.setBodyPartHealth(part, newHealth);

                if (newHealth > partCurrent) healedSomething = true;

                if (MortalWoundsConfig.get().enableDebugLogging) {
                    MortalWoundsPlugin.getPluginLogger().atInfo().log(
                            String.format("%s healed: %.1f -> %.1f (max: %.1f)",
                                    part.getDisplayName(), partCurrent, newHealth, partMax)
                    );
                }
            }
        }

        return healedSomething;
    }
}