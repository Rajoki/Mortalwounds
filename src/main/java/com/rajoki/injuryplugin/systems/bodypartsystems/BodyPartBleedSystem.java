package com.rajoki.injuryplugin.systems.bodypartsystems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.rajoki.injuryplugin.components.BodyPartComponent;
import com.rajoki.injuryplugin.config.MortalWoundsConfig;

import javax.annotation.Nonnull;
import java.util.*;

// Bleed damage system for players

public class BodyPartBleedSystem extends DelayedEntitySystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final ComponentType<EntityStore, BodyPartComponent> bodyPartComponentType;

    // Track bleed timers per player per body part
    private final Map<UUID, Map<BodyPart, BleedData>> playerBleedTimers = new HashMap<>();

    public BodyPartBleedSystem(ComponentType<EntityStore, BodyPartComponent> bodyPartComponentType) {
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
        Map<BodyPart, BleedData> playerBleeds = playerBleedTimers
                .computeIfAbsent(playerId, k -> new EnumMap<>(BodyPart.class));

        // Process each body part
        for (BodyPart part : BodyPart.values()) {
            boolean hasBleed = bodyPartComp.hasBodyPartEffect(part, "BLEED");
            boolean hasHeavyBleed = bodyPartComp.hasBodyPartEffect(part, "HEAVY_BLEED");
            BleedData bleedData = playerBleeds.get(part);

            if (hasBleed || hasHeavyBleed) {
                // Initialize bleed data if new bleed
                if (bleedData == null) {
                    bleedData = new BleedData();
                    bleedData.isHeavy = hasHeavyBleed;
                    playerBleeds.put(part, bleedData);

//                    LOGGER.atInfo().log(String.format("[%s STARTED] %s will bleed for %d seconds",
//                            hasHeavyBleed ? "HEAVY BLEED" : "BLEED",
//                            part.getDisplayName(),
//                            MortalWoundsConfig.get().bleedDurationSeconds));
                }

                // If upgraded to heavy bleed, update the flag and reset timer
                if (hasHeavyBleed && !bleedData.isHeavy) {
                    bleedData.isHeavy = true;
                    bleedData.totalBleedTime = 0f; // Reset duration for heavy bleed
                    bleedData.timeSinceLastDamage = 0f;
//                    LOGGER.atInfo().log(String.format("[UPGRADED TO HEAVY] %s timer reset", part.getDisplayName()));
                }

                // Increment timers
                bleedData.timeSinceLastDamage += dt;
                bleedData.totalBleedTime += dt;

                // Use different intervals for heavy vs regular bleed
                float damageInterval = bleedData.isHeavy ?
                        MortalWoundsConfig.get().heavyBleedDamageInterval :
                        MortalWoundsConfig.get().bleedDamageInterval;

                // Apply damage every interval
                if (bleedData.timeSinceLastDamage >= MortalWoundsConfig.get().bleedDamageInterval) {
                    applyBleedDamage(bodyPartComp, part, playerRef, ref, commandBuffer, bleedData.isHeavy);
                    bleedData.timeSinceLastDamage = 0f;
                }

                // Use different durations for heavy vs regular bleed
                int bleedDuration = bleedData.isHeavy ?
                        MortalWoundsConfig.get().heavyBleedDurationSeconds :
                        MortalWoundsConfig.get().bleedDurationSeconds;

                // Check if bleed expired
                if (bleedData.totalBleedTime >= bleedDuration) {
                    if (hasHeavyBleed) {
                        bodyPartComp.removeBodyPartEffect(part, "HEAVY_BLEED");
                    } else {
                        bodyPartComp.removeBodyPartEffect(part, "BLEED");
                    }
                    playerBleeds.remove(part);

//                    playerRef.sendMessage(Message.raw(
//                            String.format("%s stopped bleeding.", part.getDisplayName())
//                    ));
                }
            } else {
                // Clean up timer if bleed was removed externally (healed)
                if (bleedData != null) {
                    playerBleeds.remove(part);
                }
            }
        }

        // Clean up empty player entries
        if (playerBleeds.isEmpty()) {
            playerBleedTimers.remove(playerId);
        }
    }

    private void applyBleedDamage(BodyPartComponent bodyPartComp, BodyPart part,
                                  PlayerRef playerRef, Ref<EntityStore> ref,
                                  CommandBuffer<EntityStore> commandBuffer,
                                  boolean isHeavyBleed) {

        // Use separate config values for heavy bleed vs regular bleed
        float bleedDamage = isHeavyBleed ?
                MortalWoundsConfig.get().heavyBleedDamageAmount :
                MortalWoundsConfig.get().bleedDamageAmount;

        float currentHealth = bodyPartComp.getBodyPartHealth(part);



//        LOGGER.atInfo().log(String.format("[%s TICK] %s taking %.1f bleed damage (current HP: %.1f)",
//                isHeavyBleed ? "HEAVY BLEED" : "BLEED",
//                part.getDisplayName(), bleedDamage, currentHealth));

        // Apply damage to the bleeding body part with overflow
        if (currentHealth > 0) {
            if (currentHealth >= bleedDamage) {
                // Part can absorb all the damage
                bodyPartComp.damageBodyPart(part, bleedDamage);
            } else {
                // Part runs out of health = overflow to other parts
                float overflow = bleedDamage - currentHealth;
                bodyPartComp.setBodyPartHealth(part, 0f);

                if (!bodyPartComp.isBodyPartBroken(part)) {
                    bodyPartComp.setBodyPartBroken(part, true);
//                    playerRef.sendMessage(Message.raw(
//                            String.format("§4%s DESTROYED by bleeding!", part.getDisplayName().toUpperCase())
//                    ));
                }

                // Distribute overflow damage
                if (overflow > 0) {
                    distributeOverflowBleedDamage(bodyPartComp, part, overflow, playerRef);
                }
            }
        } else {
            // Part already at 0 = all damage becomes overflow
            distributeOverflowBleedDamage(bodyPartComp, part, bleedDamage, playerRef);
        }

        // Deal REAL damage to player health (bypasses armor)
        Damage bleedDamageEvent = new Damage(
                Damage.NULL_SOURCE,
                getBleedDamageCause(),
                bleedDamage
        );

        DamageSystems.executeDamage(ref, commandBuffer, bleedDamageEvent);

        // Notify player
//        String bleedType = isHeavyBleed ? "§lHEAVILY BLEEDING" : "bleeding";
//        playerRef.sendMessage(Message.raw(
//                String.format("%s is %s! (-%.0f HP§4)",
//                        part.getDisplayName(), bleedType, bleedDamage)
//        ));
    }

    private static DamageCause getBleedDamageCause() {
        return new DamageCause("Bleed", "Bleed", false, false, true);
    }

    private void distributeOverflowBleedDamage(BodyPartComponent bodyPartComp, BodyPart sourcePart,
                                               float overflow, PlayerRef playerRef) {
        List<BodyPart> targetPriority = getOverflowPriority(sourcePart);

        for (BodyPart target : targetPriority) {
            if (overflow <= 0) break;

            float targetHealth = bodyPartComp.getBodyPartHealth(target);

            if (targetHealth > 0) {
                if (targetHealth >= overflow) {
                    bodyPartComp.damageBodyPart(target, overflow);
//                    LOGGER.atInfo().log(String.format("[BLEED OVERFLOW] %.1f to %s", overflow, target.getDisplayName()));
                    overflow = 0;
                } else {
                    bodyPartComp.setBodyPartHealth(target, 0f);
                    overflow -= targetHealth;

//                    LOGGER.atInfo().log(String.format("[BLEED OVERFLOW] %s destroyed, %.1f remaining",
//                            target.getDisplayName(), overflow));

                    if (!bodyPartComp.isBodyPartBroken(target)) {
                        bodyPartComp.setBodyPartBroken(target, true);
//                        playerRef.sendMessage(Message.raw(
//                                String.format("%s DESTROYED (bleed overflow)!", target.getDisplayName().toUpperCase())
//                        ));
                    }
                }
            }
        }
    }

    private List<BodyPart> getOverflowPriority(BodyPart hitPart) {
        List<BodyPart> priority = new ArrayList<>();

        switch (hitPart) {
            case HEAD -> {
                priority.add(BodyPart.TORSO);
                addRandomParts(priority, BodyPart.HEAD, BodyPart.TORSO);
            }
            case LEFTARM -> {
                priority.add(BodyPart.TORSO);
                priority.add(BodyPart.RIGHTARM);
                addRandomParts(priority, BodyPart.LEFTARM, BodyPart.TORSO, BodyPart.RIGHTARM);
            }
            case RIGHTARM -> {
                priority.add(BodyPart.TORSO);
                priority.add(BodyPart.LEFTARM);
                addRandomParts(priority, BodyPart.RIGHTARM, BodyPart.TORSO, BodyPart.LEFTARM);
            }
            case LEFTLEG -> {
                priority.add(BodyPart.RIGHTLEG);
                priority.add(BodyPart.TORSO);
                addRandomParts(priority, BodyPart.LEFTLEG, BodyPart.RIGHTLEG, BodyPart.TORSO);
            }
            case RIGHTLEG -> {
                priority.add(BodyPart.LEFTLEG);
                priority.add(BodyPart.TORSO);
                addRandomParts(priority, BodyPart.RIGHTLEG, BodyPart.LEFTLEG, BodyPart.TORSO);
            }
            case TORSO -> {
                addRandomParts(priority, BodyPart.TORSO);
            }
        }

        return priority;
    }

    private void addRandomParts(List<BodyPart> priority, BodyPart... exclude) {
        List<BodyPart> remaining = new ArrayList<>();

        for (BodyPart part : BodyPart.values()) {
            boolean isExcluded = false;
            for (BodyPart ex : exclude) {
                if (part == ex) {
                    isExcluded = true;
                    break;
                }
            }
            if (!isExcluded) {
                remaining.add(part);
            }
        }

        while (!remaining.isEmpty()) {
            int idx = new Random().nextInt(remaining.size());
            priority.add(remaining.remove(idx));
        }
    }

    private static class BleedData {
        float timeSinceLastDamage = 0f;
        float totalBleedTime = 0f;
        boolean isHeavy = false;
    }
}