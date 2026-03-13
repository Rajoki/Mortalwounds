package com.rajoki.injuryplugin.systems.npc;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.MovementSettings;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.movement.controllers.MotionController;
import com.rajoki.injuryplugin.components.npc.NPCBodyPartComponent;
import com.rajoki.injuryplugin.components.npc.Anatomy;
import com.rajoki.injuryplugin.systems.bodypartsystems.BodyPart;
import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Movement penalty system for NPCs with leg fractures
 * Handles both humanoid and quadruped anatomies differently
 */
public class NPCLegFractureMovementSystem extends EntityTickingSystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final ComponentType<EntityStore, NPCBodyPartComponent> npcBodyPartType;

    private Field settingsField;
    private Field moveSpeedField;
    private boolean reflectionInitialized = false;

    private final Map<NPCEntity, Float> originalBaseSpeeds = new WeakHashMap<>();

    // HUMANOID multipliers (2 legs)
    private static final float HUMANOID_ONE_LEG_MUL = 0.50f;  // 50% speed
    private static final float HUMANOID_TWO_LEG_MUL = 0.25f;  // 25% speed (crawl)

    // QUADRUPED multipliers (4 legs) = more gradual degradation
    private static final float QUAD_PER_LEG_PENALTY = 0.15f;  // 15% reduction per leg
    // 1 leg broken = 85% speed
    // 2 legs broken = 70% speed
    // 3 legs broken = 55% speed
    // 4 legs broken = 40% speed

    public NPCLegFractureMovementSystem(ComponentType<EntityStore, NPCBodyPartComponent> npcBodyPartType) {
        this.npcBodyPartType = npcBodyPartType;
    }

    private void tryInitReflection(Object controller) {
        if (reflectionInitialized) return;
        try {
            Class<?> baseClass = Class.forName("com.hypixel.hytale.server.npc.movement.controllers.MotionControllerBase");

            settingsField = baseClass.getDeclaredField("movementSettings");
            settingsField.setAccessible(true);

            moveSpeedField = baseClass.getDeclaredField("moveSpeed");
            moveSpeedField.setAccessible(true);

            reflectionInitialized = true;
//            LOGGER.atInfo().log("[NPC INJURY] Movement system initialized.");
        } catch (Exception e) {
            LOGGER.atWarning().log("[NPC INJURY] Reflection Error: " + e.getMessage());
        }
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return this.npcBodyPartType;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        NPCBodyPartComponent npcBodyPart = chunk.getComponent(index, this.npcBodyPartType);
        NPCEntity npc = chunk.getComponent(index, NPCEntity.getComponentType());
        MovementStatesComponent statesComp = chunk.getComponent(index, MovementStatesComponent.getComponentType());

        if (npc == null || npcBodyPart == null || statesComp == null) return;

        try {
            MotionController mc = npc.getRole().getActiveMotionController();
            if (mc == null) return;

            tryInitReflection(mc);
            if (!reflectionInitialized) return;

            MovementSettings settings = (MovementSettings) settingsField.get(mc);
            if (settings == null) return;

            // Capture original speed
            if (!originalBaseSpeeds.containsKey(npc)) {
                originalBaseSpeeds.put(npc, settings.baseSpeed);
            }

            float originalBase = originalBaseSpeeds.get(npc);
            Anatomy anatomy = npcBodyPart.getAnatomy();

            // Calculate speed based on anatomy type
            float speedMultiplier = calculateSpeedMultiplier(npcBodyPart, anatomy);

            if (speedMultiplier < 1.0f) {
                // Apply speed penalty
                float targetSpeed = originalBase * speedMultiplier;

                settings.baseSpeed = targetSpeed;
                settings.forwardRunSpeedMultiplier = 1.0f;
                settings.forwardSprintSpeedMultiplier = 1.0f;
                moveSpeedField.setDouble(mc, (double) targetSpeed);

                // Update animation states based on severity
                updateAnimationStates(statesComp, speedMultiplier, anatomy);

                npc.invalidateCachedHorizontalSpeedMultiplier();

            } else if (originalBaseSpeeds.containsKey(npc)) {
                // Fully healed - restore defaults
                settings.baseSpeed = originalBase;
                settings.forwardRunSpeedMultiplier = 1.65f;
                npc.invalidateCachedHorizontalSpeedMultiplier();
                originalBaseSpeeds.remove(npc);
            }

        } catch (Exception e) {
            // Silently catch reflection errors
        }
    }

    /**
     * Calculate speed multiplier based on anatomy and leg fractures
     */
    private float calculateSpeedMultiplier(NPCBodyPartComponent npcBodyPart, Anatomy anatomy) {
        switch (anatomy) {
            case HUMANOID -> {
                boolean leftLeg = npcBodyPart.hasEffect(BodyPart.LEFTLEG, "FRACTURE");
                boolean rightLeg = npcBodyPart.hasEffect(BodyPart.RIGHTLEG, "FRACTURE");

                if (leftLeg && rightLeg) {
                    return HUMANOID_TWO_LEG_MUL;
                } else if (leftLeg || rightLeg) {
                    return HUMANOID_ONE_LEG_MUL;
                }
                return 1.0f;
            }

            case QUADRUPED -> {
                int fracturedLegs = 0;

                if (npcBodyPart.hasEffect(BodyPart.FRONT_LEFT_LEG, "FRACTURE")) fracturedLegs++;
                if (npcBodyPart.hasEffect(BodyPart.FRONT_RIGHT_LEG, "FRACTURE")) fracturedLegs++;
                if (npcBodyPart.hasEffect(BodyPart.BACK_LEFT_LEG, "FRACTURE")) fracturedLegs++;
                if (npcBodyPart.hasEffect(BodyPart.BACK_RIGHT_LEG, "FRACTURE")) fracturedLegs++;

                if (fracturedLegs == 0) {
                    return 1.0f;
                }

                // Each leg reduces speed by 15%
                float penalty = fracturedLegs * QUAD_PER_LEG_PENALTY;
                return Math.max(0.4f, 1.0f - penalty); // Minimum 40% speed
            }

//            case SPIDER -> {
//                // Count fractured legs (spider has 8)
//                int fracturedLegs = 0;
//                for (BodyPart part : BodyPart.values()) {
//                    if (part.isLeg() && npcBodyPart.hasBodyPart(part) &&
//                            npcBodyPart.hasEffect(part, "FRACTURE")) {
//                        fracturedLegs++;
//                    }
//                }
//
//                if (fracturedLegs == 0) return 1.0f;
//
//                // Spider: Each leg = 8% reduction (max 64% at 8 legs)
//                float penalty = fracturedLegs * 0.08f;
//                return Math.max(0.36f, 1.0f - penalty);
//            }

            case FLYING -> {
                // Flying creatures slow if legs broken
                boolean leftLeg = npcBodyPart.hasEffect(BodyPart.LEFTLEG, "FRACTURE");
                boolean rightLeg = npcBodyPart.hasEffect(BodyPart.RIGHTLEG, "FRACTURE");

                if (leftLeg && rightLeg) return 0.60f;
                if (leftLeg || rightLeg) return 0.80f;
                return 1.0f;
            }

            case SERPENT -> {
                // Serpents don't have legs, no penalty
                return 1.0f;
            }

            case DRAGON -> {
                // Dragons have 4 legs like quadrupeds
                int fracturedLegs = 0;

                if (npcBodyPart.hasEffect(BodyPart.FRONT_LEFT_LEG, "FRACTURE")) fracturedLegs++;
                if (npcBodyPart.hasEffect(BodyPart.FRONT_RIGHT_LEG, "FRACTURE")) fracturedLegs++;
                if (npcBodyPart.hasEffect(BodyPart.BACK_LEFT_LEG, "FRACTURE")) fracturedLegs++;
                if (npcBodyPart.hasEffect(BodyPart.BACK_RIGHT_LEG, "FRACTURE")) fracturedLegs++;

                if (fracturedLegs == 0) return 1.0f;

                float penalty = fracturedLegs * QUAD_PER_LEG_PENALTY;
                return Math.max(0.4f, 1.0f - penalty);
            }
        }

        return 1.0f;
    }

    /**
     * Update animation states based on injury severity
     */
    private void updateAnimationStates(MovementStatesComponent statesComp,
                                       float speedMultiplier, Anatomy anatomy) {
        // Severe injury (< 50% speed) = crouching/crawling
        if (speedMultiplier < 0.5f) {
            statesComp.getMovementStates().crouching = true;
            statesComp.getMovementStates().running = false;
            statesComp.getMovementStates().sprinting = false;
        }
        // Moderate injury (50-75% speed) - walking/limping
        else if (speedMultiplier < 0.75f) {
            statesComp.getMovementStates().walking = true;
            statesComp.getMovementStates().running = false;
            statesComp.getMovementStates().sprinting = false;
        }
    }
}