
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
import com.rajoki.injuryplugin.ui.gui.BodyPartStatsUI;
import com.rajoki.injuryplugin.components.BodyPartComponent;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public class BodyPartStatsCommand extends AbstractPlayerCommand {

    // type /mwstats to display Body part health in text

    public BodyPartStatsCommand() {
        super("mwstats", "View your body part health");
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

        CompletableFuture.runAsync(() -> {
            player.getPageManager().openCustomPage(ref, store, new BodyPartStatsUI(playerRef));
            playerRef.sendMessage(Message.raw("Body Part Stats Shown").color("green"));
        }, world);
    }
}