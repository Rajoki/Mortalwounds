package com.rajoki.injuryplugin.systems.npc;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.rajoki.injuryplugin.components.npc.NPCBodyPartComponent;
import com.rajoki.injuryplugin.systems.bodypartsystems.BodyPart;
import com.rajoki.injuryplugin.utils.InjuryTextHelper;

import javax.annotation.Nonnull;
import java.util.concurrent.ThreadLocalRandom;


 // NPCs with fractured heads have a chance to miss their attacks completely.

public class NPCHeadFractureMissSystem extends EntityEventSystem<EntityStore, Damage> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final ComponentType<EntityStore, NPCBodyPartComponent> npcCompType;
    private static final float HEAD_FRACTURE_MISS_CHANCE = 0.5f; // % miss chance

    public NPCHeadFractureMissSystem(ComponentType<EntityStore, NPCBodyPartComponent> npcCompType) {
        super(Damage.class);
        this.npcCompType = npcCompType;
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        // Use FilterDamageGroup to cancel damage BEFORE it's applied
        return DamageModule.get().getFilterDamageGroup();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.any(); // Check all damage events
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {

        // Skip if damage already cancelled or zero
        if (damage.getAmount() <= 0 || damage.isCancelled()) {
            return;
        }

        // Get the attacker (the NPC)
        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) {
            return;
        }

        Ref<EntityStore> attackerRef = entitySource.getRef();
        NPCBodyPartComponent npcComp = store.getComponent(attackerRef, npcCompType);

        // Only process if attacker is an NPC with the component
        if (npcComp == null) {
            return;
        }

        // Check if NPC has a head fracture
        boolean hasHeadFracture = npcComp.hasEffect(BodyPart.HEAD, "FRACTURE");

        if (hasHeadFracture) {
            // Roll for miss chance
            float roll = ThreadLocalRandom.current().nextFloat();

            if (roll < HEAD_FRACTURE_MISS_CHANCE) {

                // Cancel damage, no knockback or red flash
                damage.setAmount(0);          // Set damage to 0 first
                damage.setCancelled(true);     // Then cancel the event

                // Get target position for displaying text
                Ref<EntityStore> targetRef = chunk.getReferenceTo(index);
                TransformComponent targetTransform = store.getComponent(targetRef,
                        TransformComponent.getComponentType());

                if (targetTransform != null) {
                    Vector3d targetPosition = targetTransform.getPosition();

                    // Display "MISS" text above the attacker (NPC)
                    InjuryTextHelper.sendInjuryTextWithDelay(
                            attackerRef,            // Show text above the attacker (so players will see it)
                            "MISS",              // Text to display
                            targetPosition,      // Target's position
                            store,
                            commandBuffer,
                            0L                   // No delay (immediate)
                    );
                }

                //LOGGER.atInfo().log("[HEAD FRACTURE] NPC missed attack due to head fracture");
            }
        }
    }
}