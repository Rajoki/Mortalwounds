package com.rajoki.injuryplugin.systems.npc;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.rajoki.injuryplugin.components.npc.NPCBodyPartComponent;
import com.rajoki.injuryplugin.config.MortalWoundsConfig;
import com.rajoki.injuryplugin.systems.bodypartsystems.BodyPart;

import javax.annotation.Nonnull;
import java.util.EnumMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Ported bleed system specifically for NPCs.
 * Handles damage over time and effect expiration for NPCs
 */
public class NPCBodyPartBleedSystem extends DelayedEntitySystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final ComponentType<EntityStore, NPCBodyPartComponent> npcBodyPartType;

    // Use WeakHashMap to track timers
    private final Map<NPCEntity, Map<BodyPart, BleedData>> npcBleedTimers = new WeakHashMap<>();

    public NPCBodyPartBleedSystem(ComponentType<EntityStore, NPCBodyPartComponent> npcBodyPartType) {
        super(1.0f); // Ticks once every second for performance
        this.npcBodyPartType = npcBodyPartType;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {

        return this.npcBodyPartType;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        Ref<EntityStore> entityRef = chunk.getReferenceTo(index);
        NPCEntity npc = chunk.getComponent(index, NPCEntity.getComponentType());
        NPCBodyPartComponent npcBodyPart = chunk.getComponent(index, this.npcBodyPartType);

        if (npc == null || npcBodyPart == null || !npcBodyPart.isInitialized()) return;

        // Stop processing if the NPC is already dead
        if (chunk.getArchetype().contains(DeathComponent.getComponentType())) {
            npcBleedTimers.remove(npc);
            return;
        }

        Map<BodyPart, BleedData> activeBleeds = npcBleedTimers
                .computeIfAbsent(npc, k -> new EnumMap<>(BodyPart.class));

        for (BodyPart part : BodyPart.values()) {
            boolean hasBleed = npcBodyPart.hasEffect(part, "BLEED");
            boolean hasHeavyBleed = npcBodyPart.hasEffect(part, "HEAVY_BLEED");
            BleedData data = activeBleeds.get(part);

            if (hasBleed || hasHeavyBleed) {
                // Initialize if new bleed detected
                if (data == null) {
                    data = new BleedData();
                    data.isHeavy = hasHeavyBleed;
                    activeBleeds.put(part, data);
                }

                // Handle upgrade from regular to heavy
                if (hasHeavyBleed && !data.isHeavy) {
                    data.isHeavy = true;
                    data.totalBleedTime = 0f; // Reset duration for the new heavy bleed
                }

                data.timeSinceLastDamage += dt;
                data.totalBleedTime += dt;

                // Determine logic based on severity
                float interval = data.isHeavy ?
                        MortalWoundsConfig.get().heavyBleedDamageInterval :
                        MortalWoundsConfig.get().bleedDamageInterval;

                int duration = data.isHeavy ?
                        MortalWoundsConfig.get().heavyBleedDurationSeconds :
                        MortalWoundsConfig.get().bleedDurationSeconds;

                // Apply Damage Tick
                if (data.timeSinceLastDamage >= interval) {
                    applyNpcBleedDamage(entityRef, commandBuffer, data.isHeavy);
                    data.timeSinceLastDamage = 0f;
                }

                // Check for Expiration
                if (data.totalBleedTime >= duration) {
                    npcBodyPart.removeEffect(part, hasHeavyBleed ? "HEAVY_BLEED" : "BLEED");
                    activeBleeds.remove(part);
                }
            } else {
                // Clean up tracking if effect was removed (ex. via a bandage system)
                activeBleeds.remove(part);
            }
        }
    }

    private void applyNpcBleedDamage(Ref<EntityStore> entityRef, CommandBuffer<EntityStore> commandBuffer, boolean isHeavy) {
        float damageAmount = isHeavy ?
                MortalWoundsConfig.get().heavyBleedDamageAmount :
                MortalWoundsConfig.get().bleedDamageAmount;

        // Create the damage event
        Damage bleedDamage = new Damage(
                Damage.NULL_SOURCE,
                new DamageCause("Bleed", "Bleed", false, false, true),
                damageAmount
        );

        // This applies the damage to the NPC's actual health pool
        DamageSystems.executeDamage(entityRef, commandBuffer, bleedDamage);
    }

    private static class BleedData {
        float timeSinceLastDamage = 0f;
        float totalBleedTime = 0f;
        boolean isHeavy = false;
    }
}