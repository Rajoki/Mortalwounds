package com.rajoki.injuryplugin.ui.hud;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.rajoki.injuryplugin.ui.firstaidui.*;
import com.rajoki.injuryplugin.ui.gui.BodyPartStatsUI;
import com.rajoki.injuryplugin.ui.gui.BodyPartUIRegistry;
import com.rajoki.injuryplugin.components.BodyPartComponent;

import javax.annotation.Nonnull;

//System for the HUD to constantly update in real time

public class BodyPartHudTickSystem extends EntityTickingSystem<EntityStore> {

    private final ComponentType<EntityStore, BodyPartComponent> bodyPartType;

    public BodyPartHudTickSystem(ComponentType<EntityStore, BodyPartComponent> bodyPartType) {
        this.bodyPartType = bodyPartType;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), this.bodyPartType);
    }

    @Override
    public void tick(float dt,
                     int index,
                     @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);

        // Get PlayerRef first to ensure this is a player entity
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        // Get the Player entity using EntityUtils
        Player player = (Player) EntityUtils.getEntity(ref, store);
        if (player == null || player.wasRemoved()) {
            return;
        }

        BodyPartComponent bodyPart = store.getComponent(ref, bodyPartType);
        if (bodyPart == null || !bodyPart.isInitialized()) {
            return;
        }

        // Get HUD from the manager's registry (not from hudManager)
        BodyPartHud hud = BodyPartHudManager.getHud(player.getUuid());

        // If null, create it
        if (hud == null) {
            // System.out.println("[HUD TICK] HUD is null for " + playerRef.getUsername() + " (" + player.getUuid() + "), creating...");
            BodyPartHudManager.showHud(player, store, ref);
            hud = BodyPartHudManager.getHud(player.getUuid());

            if (hud == null) {
               // System.out.println("[HUD TICK] ERROR: HUD still null after showHud() for " + playerRef.getUsername());
            } else {
               // System.out.println("[HUD TICK] HUD created successfully for " + playerRef.getUsername());
            }
        }

        // Get stats UI
        BodyPartStatsUI ui = BodyPartUIRegistry.get(playerRef);

        // Get splint UI (if open)
        SplintSelectionPageNew splintUI = SplintSelectionUIRegistry.get(playerRef);

        // Get bandage UI (if open)
        BandageSelectionPageNew bandageUI = BandageSelectionUIRegistry.get(playerRef);

        // Get Surgery UI (if open)
        SurvKitSelectionPageNew surgeryUI = SurvKitUIRegistry.get(playerRef);

        // Update ALL UIs BEFORE clearing dirty
        if (bodyPart.isDirty()) {
            // System.out.println("[HUD TICK] Body part is dirty, updating HUD for " + playerRef.getUsername());

            if (ui != null) {
                ui.refresh(ref, store);
            }

            if (splintUI != null) {
                splintUI.refresh(ref, store);
            }

            if (bandageUI != null) {
                bandageUI.refresh(ref, store);
            }

            if (surgeryUI != null) {
                surgeryUI.refresh(ref, store);
            }

            // USE STATIC METHOD like EasyHunger does
            BodyPartHud.updatePlayerBodyParts(player.getUuid(), bodyPart);

            bodyPart.clearDirty();
        }
    }
}