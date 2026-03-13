package com.rajoki.injuryplugin.systems.bodypartsystems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.rajoki.injuryplugin.MortalWoundsPlugin;
import com.rajoki.injuryplugin.components.BodyPartComponent;
import com.rajoki.injuryplugin.config.MortalWoundsConfig;

import javax.annotation.Nonnull;


// Reduces damage dealt when players have arm fractures

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

        Damage.Source source = event.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) {
            return;
        }

        Ref<EntityStore> attackerRef = entitySource.getRef();

        BodyPartComponent bodyPartComp = store.getComponent(attackerRef,
                MortalWoundsPlugin.getInstance().getBodyPartComponentType());

        if (bodyPartComp == null) {
            return;
        }

        // Check how many arms are fractured
        boolean leftArmFractured = bodyPartComp.hasBodyPartEffect(BodyPart.LEFTARM, "FRACTURE");
        boolean rightArmFractured = bodyPartComp.hasBodyPartEffect(BodyPart.RIGHTARM, "FRACTURE");

        float damageReduction = 0f;

        if (leftArmFractured && rightArmFractured) {
            // Both arms fractured, use config value
            damageReduction = MortalWoundsConfig.get().armFractureDamageReduction2Arms;
//            LOGGER.atInfo().log("Both arms fractured - " + (damageReduction * 100) + "% damage reduction");
        } else if (leftArmFractured || rightArmFractured) {
            // One arm fractured, use config value
            damageReduction = MortalWoundsConfig.get().armFractureDamageReduction1Arm;
//            LOGGER.atInfo().log("One arm fractured - " + (damageReduction * 100) + "% damage reduction");
        }

        if (damageReduction > 0f) {
            float originalDamage = event.getAmount();
            float damageMultiplier = 1.0f - damageReduction;
            float newDamage = originalDamage * damageMultiplier;
            event.setAmount(newDamage);
//            LOGGER.atInfo().log("Arm fracture reduced damage: " + originalDamage + " -> " + newDamage);
        }
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getGatherDamageGroup();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }
}