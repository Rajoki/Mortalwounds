package com.rajoki.injuryplugin.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.rajoki.injuryplugin.components.BodyPartComponent;
import com.rajoki.injuryplugin.components.npc.NPCBodyPartComponent;
import com.rajoki.injuryplugin.systems.bodypartsystems.BodyPart;
import com.rajoki.injuryplugin.systems.bodypartsystems.DirectionalDamageCalculator;

import javax.annotation.Nonnull;

//Multiplies damage depending on body part hit (ex. Headshots do bonus damage)

public class BodyPartMultiplierSystem extends EntityEventSystem<EntityStore, Damage> {

    private final ComponentType<EntityStore, BodyPartComponent> playerCompType;
    private final ComponentType<EntityStore, NPCBodyPartComponent> npcCompType;

    public BodyPartMultiplierSystem(ComponentType<EntityStore, BodyPartComponent> playerCompType,
                                    ComponentType<EntityStore, NPCBodyPartComponent> npcCompType) {
        super(Damage.class);
        this.playerCompType = playerCompType;
        this.npcCompType = npcCompType;
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

        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) {
            return;
        }

        Ref<EntityStore> targetRef = chunk.getReferenceTo(index);
        Ref<EntityStore> attackerRef = entitySource.getRef();

        // Check for both player and NPC components correctly
        BodyPartComponent playerComp = store.getComponent(targetRef, playerCompType);
        NPCBodyPartComponent npcComp = store.getComponent(targetRef, npcCompType);

        // If both are null, this entity doesn't use the injury system
        if (playerComp == null && npcComp == null) {
            return;
        }

        TransformComponent victimTransform = store.getComponent(targetRef, TransformComponent.getComponentType());
        TransformComponent attackerTransform = store.getComponent(attackerRef, TransformComponent.getComponentType());

        if (victimTransform == null || attackerTransform == null) {
            return;
        }

        Vector3d attackerPos = attackerTransform.getPosition();
        BodyPart hitPart = DirectionalDamageCalculator.getTargetBodyPart(victimTransform, attackerPos);

        if (hitPart == BodyPart.HEAD) {
            float originalDamage = damage.getAmount();
            float headMultiplier = 1.2f;

            damage.setAmount(originalDamage * headMultiplier);
        }
    }
}