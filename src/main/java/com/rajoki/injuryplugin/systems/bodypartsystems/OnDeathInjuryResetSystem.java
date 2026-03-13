package com.rajoki.injuryplugin.systems.bodypartsystems;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.rajoki.injuryplugin.MortalWoundsPlugin;
import com.rajoki.injuryplugin.components.BodyPartComponent;
import org.jspecify.annotations.NonNull;

public class OnDeathInjuryResetSystem extends DeathSystems.OnDeathSystem {

    //Resets injuries to none after death, to prevent respawning with prior injuries.

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
                MortalWoundsPlugin.getInstance().getBodyPartComponentType(),
                PlayerRef.getComponentType()
        );
    }

    @Override
    public void onComponentAdded(@NonNull Ref<EntityStore> ref, @NonNull DeathComponent deathComponent, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {

    }

    @Override
    public void onComponentRemoved(
            Ref<EntityStore> ref,
            DeathComponent death,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer
    ) {

        BodyPartComponent comp = store.getComponent(
                ref,
                MortalWoundsPlugin.getInstance().getBodyPartComponentType()
        );

        if (comp == null) return;

        // Clear all effects + fractures
        for (BodyPart part : BodyPart.values()) {

            // Remove effects
            for (String effect : comp.getBodyPartEffects(part)) {
                comp.removeBodyPartEffect(part, effect);
            }

            // Reset fracture state
            comp.setBodyPartBroken(part, false);


        }

        MortalWoundsPlugin.getPluginLogger().atInfo().log(
                "[RESPAWN] Cleared injuries and restored body parts"
        );
    }
}