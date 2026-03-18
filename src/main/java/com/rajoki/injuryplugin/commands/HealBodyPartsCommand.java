package com.rajoki.injuryplugin.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.rajoki.injuryplugin.MortalWoundsPlugin;
import com.rajoki.injuryplugin.components.BodyPartComponent;
import com.rajoki.injuryplugin.systems.bodypartsystems.BodyPart;

import javax.annotation.Nonnull;

public class HealBodyPartsCommand extends AbstractPlayerCommand {

    // /mwheal to heal injuries, etc

    public HealBodyPartsCommand() {
        super("mwheal", "Removes all fractures, bleeds, and repairs broken limbs (for testing)");
        this.setPermissionGroup(GameMode.Creative);
    }

    @Override
    protected void execute(@Nonnull CommandContext commandContext, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {

        Player player = commandContext.senderAs(Player.class);

        BodyPartComponent bodyPartComp = store.getComponent(ref,
                MortalWoundsPlugin.getInstance().getBodyPartComponentType());

        if (bodyPartComp == null || !bodyPartComp.isInitialized()) {
            commandContext.sendMessage(Message.raw("Body part system not initialized!"));
            return;
        }

        int fracturesRemoved = 0;
        int bleedsRemoved = 0;
        int heavyBleedsRemoved = 0;
        int brokenPartsRestored = 0;

        for (BodyPart part : BodyPart.values()) {
            // Remove FRACTURE effect
            if (bodyPartComp.hasBodyPartEffect(part, "FRACTURE")) {
                bodyPartComp.removeBodyPartEffect(part, "FRACTURE");
                fracturesRemoved++;
            }

            // Remove BLEED effect
            if (bodyPartComp.hasBodyPartEffect(part, "BLEED")) {
                bodyPartComp.removeBodyPartEffect(part, "BLEED");
                bleedsRemoved++;
            }

            // Remove HEAVY_BLEED effect
            if (bodyPartComp.hasBodyPartEffect(part, "HEAVY_BLEED")) {
                bodyPartComp.removeBodyPartEffect(part, "HEAVY_BLEED");
                heavyBleedsRemoved++;
            }

            // Remove DESTROYED effect
            if (bodyPartComp.hasBodyPartEffect(part, "DESTROYED")) {
                bodyPartComp.removeBodyPartEffect(part, "DESTROYED");
            }

            // IMPORTANT: Remove STAMINA_CLAMPED marker (added by TorsoFractureStaminaSystem)
            if (bodyPartComp.hasBodyPartEffect(part, "STAMINA_CLAMPED")) {
                bodyPartComp.removeBodyPartEffect(part, "STAMINA_CLAMPED");
            }

            // Restore broken limbs
            if (bodyPartComp.isBodyPartBroken(part)) {
                bodyPartComp.setBodyPartBroken(part, false);
                bodyPartComp.setBodyPartHealth(part, 1.0f); // Restore to 1 HP
                brokenPartsRestored++;
            }
        }

        // CRITICAL: Also clear the guard break effect from the player
        clearGuardBreakEffect(ref, store);

        String message = String.format("Removed %d fractures, %d bleeds, %d heavy bleeds, and restored %d broken limbs!",
                fracturesRemoved, bleedsRemoved, heavyBleedsRemoved, brokenPartsRestored);
        commandContext.sendMessage(Message.raw(message));

        // Mark component as dirty so systems update
        bodyPartComp.markDirty();
    }

    /**
     * Removes the Stamina_Broken (guard break) effect if active.
     * Important for clean healing during testing.
     */
    private void clearGuardBreakEffect(Ref<EntityStore> ref, Store<EntityStore> store) {
        try {
            EffectControllerComponent effectController = store.getComponent(
                    ref, EffectControllerComponent.getComponentType());
            if (effectController == null) return;

            int effectIndex = EntityEffect.getAssetMap().getIndex("Stamina_Broken");
            if (effectIndex == Integer.MIN_VALUE) return;

            if (effectController.getActiveEffects().containsKey(effectIndex)) {
                effectController.removeEffect(ref, effectIndex, store);
            }
        } catch (Exception e) {
            // Silently fail - not critical
        }
    }
}