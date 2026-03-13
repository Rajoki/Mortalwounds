package com.rajoki.injuryplugin.systems.bodypartsystems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.rajoki.injuryplugin.components.BodyPartComponent;

import javax.annotation.Nonnull;

/**
 * Clamps player max health based on destroyed (broken) body parts.
 * Runs every 0.5 seconds to check and enforce health limits.
 */
public class BodyPartDestroyedHealthSystem extends DelayedEntitySystem<EntityStore> {

    private final ComponentType<EntityStore, BodyPartComponent> bodyPartType;

    public BodyPartDestroyedHealthSystem(ComponentType<EntityStore, BodyPartComponent> bodyPartType) {
        super(0.5f); // Run every 0.5 seconds
        this.bodyPartType = bodyPartType;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), this.bodyPartType);
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        EntityStatMap stats = chunk.getComponent(index, EntityStatMap.getComponentType());
        BodyPartComponent bodyPartData = chunk.getComponent(index, this.bodyPartType);

        if (playerRef == null || stats == null || bodyPartData == null || !bodyPartData.isInitialized()) {
            return;
        }

        // Get player's health stat
        int healthIdx = DefaultEntityStatTypes.getHealth();
        EntityStatValue healthValue = stats.get(healthIdx);

        if (healthValue == null) {
            return;
        }

        float engineMaxHealth = healthValue.getMax();

        // First-time initialization = store original max health
        if (!bodyPartData.hasStoredOriginalMaxHealth()) {
            bodyPartData.setOriginalMaxHealth(engineMaxHealth);
            bodyPartData.setStoredOriginalMaxHealth(true);
        } else {
            // Check if engine max changed (e.g., from armor/buffs)
            float storedOriginal = bodyPartData.getOriginalMaxHealth();

            if (engineMaxHealth != storedOriginal) {
                float delta = engineMaxHealth - storedOriginal;
                bodyPartData.setOriginalMaxHealth(engineMaxHealth);


            }
        }

        // Calculate total health lost from destroyed (broken) limbs
        float destroyedHealthLoss = 0f;

        for (BodyPart part : BodyPart.values()) {
            if (bodyPartData.isBodyPartBroken(part)) {
                destroyedHealthLoss += bodyPartData.getBodyPartMaxHealth(part);
            }
        }

        // Calculate effective max health (original - destroyed limbs)
        float originalMaxHealth = bodyPartData.getOriginalMaxHealth();
        float effectiveMaxHealth = originalMaxHealth - destroyedHealthLoss;

        // Clamp to at least 1 HP (can't go below 1)
        effectiveMaxHealth = Math.max(1f, effectiveMaxHealth);

        // Get current health
        float currentHealth = healthValue.get();

        // CLAMP: If current health exceeds effective max, push it back down
        if (currentHealth > effectiveMaxHealth) {
            stats.setStatValue(healthIdx, effectiveMaxHealth);

            //  Notify player they hit the limit
            // playerRef.sendMessage(Message.raw("§cYour destroyed limbs limit your max health!"));
        }
    }
}