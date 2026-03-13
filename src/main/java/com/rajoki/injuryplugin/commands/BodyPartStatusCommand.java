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
import com.rajoki.injuryplugin.MortalWoundsPlugin;
import com.rajoki.injuryplugin.systems.bodypartsystems.BodyPart;
import com.rajoki.injuryplugin.components.BodyPartComponent;

import javax.annotation.Nonnull;

public class BodyPartStatusCommand extends AbstractPlayerCommand {

    // /mwtextstats to show body parts in text form
    // Right now displays ALL parts even from other anatomies, bugged

    public BodyPartStatusCommand() {
        super("mwtextstats", "Shows detailed body part status and current multipliers");
        this.setPermissionGroup(GameMode.Adventure);
    }

    @Override
    protected void execute(@Nonnull CommandContext commandContext, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {

        Player player = commandContext.senderAs(Player.class);

        BodyPartComponent bodyPartComp = store.getComponent(ref,
                MortalWoundsPlugin.getInstance().getBodyPartComponentType());

        if (bodyPartComp == null || !bodyPartComp.isInitialized()) {
            commandContext.sendMessage(Message.raw("§cBody part system not initialized!"));
            return;
        }

        // Header
        commandContext.sendMessage(Message.raw("§6========== Body Part Status =========="));

        // Check each body part for fractures
        for (BodyPart part : BodyPart.values()) {
            boolean hasFracture = bodyPartComp.hasBodyPartEffect(part, "FRACTURE");
            boolean hasBleed = bodyPartComp.hasBodyPartEffect(part, "BLEED");
            float current = bodyPartComp.getBodyPartHealth(part);
            float max = bodyPartComp.getBodyPartMaxHealth(part);

            String status = "";
            if (hasFracture) status += "[FRACTURED]";
            if (hasBleed) status += "[BLEEDING]";
            if (status.isEmpty()) status = "[OK]";

            String health = String.format("(%.0f/%.0f HP)", current, max);

            commandContext.sendMessage(Message.raw(
                    String.format("  %s %s %s", part.getDisplayName(), status, health)
            ));
        }

        commandContext.sendMessage(Message.raw("======================================"));

        // Calculate arm fracture multiplier
        boolean leftArmFractured = bodyPartComp.hasBodyPartEffect(BodyPart.LEFTARM, "FRACTURE");
        boolean rightArmFractured = bodyPartComp.hasBodyPartEffect(BodyPart.RIGHTARM, "FRACTURE");

        float armDamageMultiplier = 1.0f;
        String armStatus;

        if (leftArmFractured && rightArmFractured) {
            armDamageMultiplier = 0.5f;
            armStatus = "2 Arms Fractured";
        } else if (leftArmFractured || rightArmFractured) {
            armDamageMultiplier = 0.75f;
            armStatus = "1 Arm Fractured";
        } else {
            armStatus = "No Arm Fractures";
        }

        commandContext.sendMessage(Message.raw(
                String.format("Damage Output: §f%.0f%% §7(%s)", armDamageMultiplier * 100, armStatus)
        ));

        // Calculate leg fracture multiplier
        boolean leftLegFractured = bodyPartComp.hasBodyPartEffect(BodyPart.LEFTLEG, "FRACTURE");
        boolean rightLegFractured = bodyPartComp.hasBodyPartEffect(BodyPart.RIGHTLEG, "FRACTURE");

        float legSpeedMultiplier = 1.0f;
        String legStatus;

        if (leftLegFractured && rightLegFractured) {
            legSpeedMultiplier = 0.50f;
            legStatus = "2 Legs Fractured";
        } else if (leftLegFractured || rightLegFractured) {
            legSpeedMultiplier = 0.25f;
            legStatus = "1 Leg Fractured";
        } else {
            legStatus = "No Leg Fractures";
        }

        commandContext.sendMessage(Message.raw(
                String.format("Movement Speed: §f%.0f%% §7(%s)", legSpeedMultiplier * 100, legStatus)
        ));

        commandContext.sendMessage(Message.raw("======================================"));
    }
}