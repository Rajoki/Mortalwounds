package com.rajoki.injuryplugin.systems.npc;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.rajoki.injuryplugin.components.npc.NPCBodyPartComponent;
import com.rajoki.injuryplugin.config.MortalWoundsConfig;
import com.rajoki.injuryplugin.components.npc.Anatomy;
import com.rajoki.injuryplugin.systems.bodypartsystems.BodyPart;

import javax.annotation.Nonnull;

/**
 * Reduces damage dealt by NPCs with fractured arms/front legs
 * Handles both humanoid (arms) and quadruped (front legs) anatomies
 */
public class NPCArmFractureDamageSystem extends EntityEventSystem<EntityStore, Damage> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final ComponentType<EntityStore, NPCBodyPartComponent> npcBodyPartType;

    // HUMANOID arm penalties (config-based will be added)
    // QUADRUPED front leg penalties (slightly less severe)
    private static final float QUAD_ONE_FRONT_LEG_REDUCTION = 0.15f;  // 15% damage reduction
    private static final float QUAD_TWO_FRONT_LEG_REDUCTION = 0.35f;  // 35% damage reduction

    public NPCArmFractureDamageSystem(ComponentType<EntityStore, NPCBodyPartComponent> npcBodyPartType) {
        super(Damage.class);
        this.npcBodyPartType = npcBodyPartType;
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getGatherDamageGroup();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {

        // Get damage source
        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) {
            return;
        }

        // Get attacker's NPC component
        Ref<EntityStore> attackerRef = entitySource.getRef();
        NPCBodyPartComponent npcBodyPart = store.getComponent(attackerRef, npcBodyPartType);

        if (npcBodyPart == null || !npcBodyPart.isInitialized()) {
            return;
        }

        // Calculate damage reduction based on anatomy
        float reduction = calculateDamageReduction(npcBodyPart);

        // Apply reduction
        if (reduction > 0f) {
            float originalDamage = damage.getAmount();
            float multiplier = Math.max(0, 1.0f - reduction);
            float newDamage = originalDamage * multiplier;

            damage.setAmount(newDamage);

            // Optional: Log for debugging
            /*
            LOGGER.atInfo().log(String.format(
                "[NPC ARM/LEG INJURY] Damage reduced: %.1f -> %.1f (%.0f%% penalty)",
                originalDamage, newDamage, reduction * 100
            ));
            */
        }
    }

    /**
     * Calculate damage reduction based on anatomy and limb fractures
     */
    private float calculateDamageReduction(NPCBodyPartComponent npcBodyPart) {
        Anatomy anatomy = npcBodyPart.getAnatomy();

        switch (anatomy) {
            case HUMANOID -> {
                boolean leftArm = npcBodyPart.hasEffect(BodyPart.LEFTARM, "FRACTURE");
                boolean rightArm = npcBodyPart.hasEffect(BodyPart.RIGHTARM, "FRACTURE");

                if (leftArm && rightArm) {
                    return MortalWoundsConfig.get().armFractureDamageReduction2Arms;
                } else if (leftArm || rightArm) {
                    return MortalWoundsConfig.get().armFractureDamageReduction1Arm;
                }
                return 0f;
            }

            case QUADRUPED, DRAGON -> {
                // Front legs are used for attacks (swipes, claws, etc.)
                boolean frontLeft = npcBodyPart.hasEffect(BodyPart.FRONT_LEFT_LEG, "FRACTURE");
                boolean frontRight = npcBodyPart.hasEffect(BodyPart.FRONT_RIGHT_LEG, "FRACTURE");

                if (frontLeft && frontRight) {
                    return QUAD_TWO_FRONT_LEG_REDUCTION;
                } else if (frontLeft || frontRight) {
                    return QUAD_ONE_FRONT_LEG_REDUCTION;
                }
                return 0f;
            }

//            case SPIDER -> {
//                // Count fractured front legs (legs 1, 2, 7, 8)
//                int frontLegsFractured = 0;
//                if (npcBodyPart.hasEffect(BodyPart.LEG_1, "FRACTURE")) frontLegsFractured++;
//                if (npcBodyPart.hasEffect(BodyPart.LEG_2, "FRACTURE")) frontLegsFractured++;
//                if (npcBodyPart.hasEffect(BodyPart.LEG_7, "FRACTURE")) frontLegsFractured++;
//                if (npcBodyPart.hasEffect(BodyPart.LEG_8, "FRACTURE")) frontLegsFractured++;
//
//                // Each front leg = 10% reduction
//                return frontLegsFractured * 0.10f;
//            }

            default -> {
                return 0f; // No damage penalty for other anatomies
            }
        }
    }
}