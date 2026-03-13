package com.rajoki.injuryplugin.systems.npc;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.rajoki.injuryplugin.components.npc.Anatomy;
import com.rajoki.injuryplugin.components.npc.NPCBodyPartComponent;
import com.rajoki.injuryplugin.systems.CreatureType;
import com.rajoki.injuryplugin.systems.bodypartsystems.BodyPart;

import javax.annotation.Nonnull;

/**
 * Adds NPCBodyPartComponent to NPCs when they spawn and initializes limb health
 */
public class NPCJoinBodyPartSystem extends RefSystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final ComponentType<EntityStore, NPCBodyPartComponent> npcBodyPartType;

    public NPCJoinBodyPartSystem(ComponentType<EntityStore, NPCBodyPartComponent> npcBodyPartType) {
        this.npcBodyPartType = npcBodyPartType;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        ComponentType<EntityStore, NPCEntity> npcType = NPCEntity.getComponentType();

        if (npcType == null) {
            return Query.not(Query.any());
        }

        return npcType;
    }

  @Override
    public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason reason,
                              @Nonnull Store<EntityStore> store,
                              @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        NPCBodyPartComponent existing = store.getComponent(ref, npcBodyPartType);

        if (existing != null && existing.isInitialized()) {
            return;
        }

        NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
        String npcTypeId = (npc != null) ? npc.getNPCTypeId() : null;
        String npcName = (npc != null) ? npc.getRoleName() : "Unknown NPC";

        // Determine anatomy and creature type
        Anatomy anatomy = Anatomy.fromNPCType(npcTypeId);
        CreatureType creatureType = CreatureType.fromNPCType(npcTypeId);

        EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
        if (stats == null) {
            LOGGER.atWarning().log("[NPC BODYPART] Could not get EntityStatMap for " + npcName);
            return;
        }

        int healthIdx = DefaultEntityStatTypes.getHealth();
        EntityStatValue healthValue = stats.get(healthIdx);
        if (healthValue == null) {
            LOGGER.atWarning().log("[NPC BODYPART] Could not get health value for " + npcName);
            return;
        }

        float npcMaxHealth = healthValue.getMax();

        // Create component with detected anatomy
        NPCBodyPartComponent npcBodyPart = new NPCBodyPartComponent();
        npcBodyPart.initializeBodyParts(npcMaxHealth, anatomy);  // ONLY 2 ARGS
        npcBodyPart.setCreatureType(creatureType);  // SET CREATURE TYPE SEPARATELY

        // Detailed logging
//        LOGGER.atInfo().log("========================================");
//        LOGGER.atInfo().log(String.format("[NPC BODYPART INIT] %s (%s, %s, %.0f HP)",
//                npcName, anatomy.name(), creatureType.name(), npcMaxHealth));

        // Log immunities if any
//        if (creatureType != CreatureType.NORMAL) {
//            LOGGER.atInfo().log(String.format("  Creature Type: %s", creatureType.name()));
//            if (creatureType.isImmuneTo("BLEED")) {
//                LOGGER.atInfo().log("  - IMMUNE to bleeding");
//            }
//            if (creatureType.isResistantTo("FRACTURE")) {
//                LOGGER.atInfo().log(String.format("  - RESISTANT to fractures (%.1fx threshold)",
//                        creatureType.getFractureThresholdMultiplier()));
//            }
//        }

//        for (BodyPart part : npcBodyPart.getValidBodyParts()) {
//            LOGGER.atInfo().log(String.format(
//                    "  %s: %.1f / %.1f HP",
//                    part.getDisplayName(),
//                    npcBodyPart.getBodyPartHealth(part),
//                    npcBodyPart.getBodyPartMaxHealth(part)
//            ));
//        }
//        LOGGER.atInfo().log("========================================");

        commandBuffer.addComponent(ref, npcBodyPartType, npcBodyPart);
    }

    @Override
    public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason,
                               @Nonnull Store<EntityStore> store,
                               @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        // Cleanup if needed
    }
}