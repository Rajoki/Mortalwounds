package com.rajoki.injuryplugin.systems.npc;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.rajoki.injuryplugin.components.npc.NPCBodyPartComponent;
import com.rajoki.injuryplugin.systems.bodypartsystems.BodyPart;

import javax.annotation.Nonnull;


//  NPCs with fractured torsos take increased damage
//  Damage multiplier is configurable

public class NPCTorsoFractureDamageSystem extends EntityEventSystem<EntityStore, Damage> {

    private final ComponentType<EntityStore, NPCBodyPartComponent> npcCompType;
    private static final float TORSO_FRACTURE_DAMAGE_MULTIPLIER = 1.2f;

    public NPCTorsoFractureDamageSystem(ComponentType<EntityStore, NPCBodyPartComponent> npcCompType) {
        super(Damage.class);
        this.npcCompType = npcCompType;
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getGatherDamageGroup();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return this.npcCompType;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {

        Ref<EntityStore> targetRef = chunk.getReferenceTo(index);
        NPCBodyPartComponent npcComp = chunk.getComponent(index, npcCompType);

        if (npcComp == null) {
            return;
        }

        // Check if NPC has a torso fracture
        boolean hasTorsoFracture = npcComp.hasEffect(BodyPart.TORSO, "FRACTURE");

        if (hasTorsoFracture) {
            // Apply damage multiplier
            float originalDamage = damage.getAmount();
            float newDamage = originalDamage * TORSO_FRACTURE_DAMAGE_MULTIPLIER;

            damage.setAmount(newDamage);
        }
    }
}