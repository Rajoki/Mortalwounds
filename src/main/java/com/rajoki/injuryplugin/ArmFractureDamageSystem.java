package com.rajoki.injuryplugin;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class ArmFractureDamageSystem extends EntityEventSystem<EntityStore, Damage> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public ArmFractureDamageSystem() {
        super(Damage.class);
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage event) {

        // Check if the damage source is an entity (player attacking something)
        Damage.Source source = event.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) {
            return;
        }

        // Get the attacker (the entity dealing damage)
        Ref<EntityStore> attackerRef = entitySource.getRef();

        // Check if attacker has the arm fracture effect
        EffectControllerComponent effectController = store.getComponent(attackerRef, EffectControllerComponent.getComponentType());

        if (effectController == null) {
            return;
        }

        // Check for Fracture_Arm effect
        int armFractureIndex = EntityEffect.getAssetMap().getIndex("Fracture_Arm");

        if (armFractureIndex == Integer.MIN_VALUE) {
            return;
        }

        // If attacker has arm fracture, reduce their damage
        if (effectController.getActiveEffects().containsKey(armFractureIndex)) {
            float originalDamage = event.getAmount();

            // Get reduction from config (default 50% reduction)
            float reductionMultiplier = InjuryConfig.get().armFractureDamageMultiplier;

            float newDamage = originalDamage * reductionMultiplier;
            event.setAmount(newDamage);

            LOGGER.atInfo().log("Arm fracture reduced damage: " + originalDamage + " -> " + newDamage);
        }
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getGatherDamageGroup();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any(); // Process all damage events
    }
}