package com.rajoki.injuryplugin.utils;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.component.spatial.SpatialStructure;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.CombatTextUpdate;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems.EntityViewer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.rajoki.injuryplugin.systems.bodypartsystems.BodyPart;
import it.unimi.dsi.fastutil.objects.ObjectList;

import javax.annotation.Nonnull;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Helper class for displaying floating injury text above entities
 * With delays to prevent overlapping with damage numbers
 */
public final class InjuryTextHelper {
    private static final double VIEW_DISTANCE = 64.0;
    private static final long DEFAULT_DELAY_MS = 300; // 300ms delay for injury text

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private InjuryTextHelper() {
    }

    /**
     * Send combat text to all nearby players (immediate)
     */
    private static void sendCombatTextImmediate(@Nonnull Ref<EntityStore> entityRef,
                                                @Nonnull String text,
                                                float hitAngle,
                                                @Nonnull Vector3d position,
                                                @Nonnull Store<EntityStore> store,
                                                @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        CombatTextUpdate update = new CombatTextUpdate(hitAngle, text);

        SpatialResource<Ref<EntityStore>, EntityStore> playerSpatial =
                commandBuffer.getResource(EntityModule.get().getPlayerSpatialResourceType());
        SpatialStructure<Ref<EntityStore>> spatialStructure = playerSpatial.getSpatialStructure();

        ObjectList<Ref<EntityStore>> nearbyPlayers = SpatialResource.getThreadLocalReferenceList();
        spatialStructure.collect(position, VIEW_DISTANCE, nearbyPlayers);

        ComponentType<EntityStore, EntityViewer> viewerType = EntityViewer.getComponentType();

        for (int i = 0; i < nearbyPlayers.size(); ++i) {
            Ref<EntityStore> playerRef = nearbyPlayers.get(i);
            if (playerRef.isValid()) {
                EntityViewer viewer = commandBuffer.getComponent(playerRef, viewerType);
                if (viewer != null) {
                    viewer.queueUpdate(entityRef, update);
                }
            }
        }
    }

    /**
     * Send combat text with a delay
     */
    private static void sendCombatTextDelayed(@Nonnull Ref<EntityStore> entityRef,
                                              @Nonnull String text,
                                              float hitAngle,
                                              @Nonnull Vector3d position,
                                              @Nonnull Store<EntityStore> store,
                                              @Nonnull CommandBuffer<EntityStore> commandBuffer,
                                              long delayMs) {

        scheduler.schedule(() -> {
            sendCombatTextImmediate(entityRef, text, hitAngle, position, store, commandBuffer);
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Get abbreviated body part category for text display
     */
    private static String getBodyPartCategory(BodyPart bodyPart) {
        if (bodyPart.isLeg()) {
            return "LEG";
        } else if (bodyPart.isArm()) {
            return "ARM";
        } else if (bodyPart.isWing()) {
            return "WING";
        } else {
            // For specific parts like HEAD, TORSO, TAIL, NECK, ABDOMEN
            switch (bodyPart) {
                case HEAD -> { return "HEAD"; }
                case TORSO -> { return "TORSO"; }
                case TAIL -> { return "TAIL"; }
                case NECK -> { return "NECK"; }
                case ABDOMEN -> { return "ABDOMEN"; }
                default -> { return "LIMB"; }
            }
        }
    }

    /**
     * Display fracture injury text (with delay)
     */
    public static void sendFractureText(@Nonnull Ref<EntityStore> entityRef,
                                        @Nonnull BodyPart bodyPart,
                                        @Nonnull Vector3d position,
                                        @Nonnull Store<EntityStore> store,
                                        @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        String category = getBodyPartCategory(bodyPart);
        String text = category + " FRACTURE";

        sendCombatTextDelayed(entityRef, text, 0.0F, position, store, commandBuffer, DEFAULT_DELAY_MS);
    }

    /**
     * Display bleed injury text (with delay)
     */
    public static void sendBleedText(@Nonnull Ref<EntityStore> entityRef,
                                     @Nonnull BodyPart bodyPart,
                                     boolean isHeavy,
                                     @Nonnull Vector3d position,
                                     @Nonnull Store<EntityStore> store,
                                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        String category = getBodyPartCategory(bodyPart);
        String text;

        if (isHeavy) {
            text = category + " HVY BLD";  // "LEG HVY BLD", "ARM HVY BLD"
        } else {
            text = category + " BLEED";    // "LEG BLEED", "ARM BLEED"
        }

        sendCombatTextDelayed(entityRef, text, 0.0F, position, store, commandBuffer, DEFAULT_DELAY_MS);
    }

    /**
     * Display limb destroyed text (with delay)
     */
    public static void sendLimbDestroyedText(@Nonnull Ref<EntityStore> entityRef,
                                             @Nonnull BodyPart bodyPart,
                                             @Nonnull Vector3d position,
                                             @Nonnull Store<EntityStore> store,
                                             @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        String category = getBodyPartCategory(bodyPart);
        String text = category + " BROKEN";  // "LEG BROKEN", "ARM BROKEN", "WING BROKEN"

        sendCombatTextDelayed(entityRef, text, 0.0F, position, store, commandBuffer, DEFAULT_DELAY_MS);
    }

    /**
     * Display generic injury text (with delay)
     */
    public static void sendInjuryText(@Nonnull Ref<EntityStore> entityRef,
                                      @Nonnull String injuryText,
                                      @Nonnull Vector3d position,
                                      @Nonnull Store<EntityStore> store,
                                      @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        sendCombatTextDelayed(entityRef, injuryText, 0.0F, position, store, commandBuffer, DEFAULT_DELAY_MS);
    }

    /**
     * Display injury text with custom delay
     */
    public static void sendInjuryTextWithDelay(@Nonnull Ref<EntityStore> entityRef,
                                               @Nonnull String injuryText,
                                               @Nonnull Vector3d position,
                                               @Nonnull Store<EntityStore> store,
                                               @Nonnull CommandBuffer<EntityStore> commandBuffer,
                                               long delayMs) {

        sendCombatTextDelayed(entityRef, injuryText, 0.0F, position, store, commandBuffer, delayMs);
    }

    /**
     * Cleanup scheduler on plugin disable
     */
    public static void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}