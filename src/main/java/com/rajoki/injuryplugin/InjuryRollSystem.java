package com.rajoki.injuryplugin;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.*;
import com.hypixel.hytale.server.core.universe.world.PlayerUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ThreadLocalRandom;

public class InjuryRollSystem extends DamageEventSystem {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final ComponentType<EntityStore, HitTrackerComponent> hitTrackerComponentType;
    private final InjuryEffectManager effectManager;

    public InjuryRollSystem(ComponentType<EntityStore, HitTrackerComponent> hitTrackerComponentType) {
        this.hitTrackerComponentType = hitTrackerComponentType;
        this.effectManager = new InjuryEffectManager();
    }

    @Nullable
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getInspectDamageGroup();
    }

    @Nonnull
    public Query<EntityStore> getQuery() {
        return this.hitTrackerComponentType;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {


        // SKIP damage with zero amount
        if (damage.getAmount() <= 0) {
            return;
        }

        Damage.Source source = damage.getSource();

        if (source instanceof Damage.EntitySource entitySource) {
            int randomNumber = ThreadLocalRandom.current().nextInt(1, 101);

            var victimRef = chunk.getReferenceTo(index);

            // Determine which effect to apply based on roll and existing effects
            InjuryEffectManager.InjuryResult result = effectManager.determineEffect(damage, randomNumber, victimRef, store);

            // Apply the effect if one was selected
            if (result.hasEffect()) {
                EffectControllerComponent effectController = store.getComponent(victimRef, EffectControllerComponent.getComponentType());

                if (effectController != null) {
                    // Remove old effect if needed (e.g., remove Bleed when applying HeavyBleed)
                    if (result.shouldRemoveEffect()) {
                        int effectToRemoveIndex = EntityEffect.getAssetMap().getIndex(result.getEffectToRemove());
                        if (effectToRemoveIndex != Integer.MIN_VALUE) {
                            effectController.removeEffect(victimRef, effectToRemoveIndex,
                                    com.hypixel.hytale.server.core.asset.type.entityeffect.config.RemovalBehavior.COMPLETE,
                                    store);
                        }
                    }

                    // Apply new effect
                    boolean success = effectController.addEffect(victimRef, result.getEffect(), store);


//                    if (success) {
//                        // Send message only to the victim
//                        com.hypixel.hytale.server.core.entity.entities.Player victim =
//                                store.getComponent(victimRef, com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
//
//                        if (victim != null) {
//                            victim.sendMessage(Message.raw(result.getMessage()));
//                        }
//                    }

                }
            }
        }
    }
}