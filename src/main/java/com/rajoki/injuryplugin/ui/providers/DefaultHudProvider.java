package com.rajoki.injuryplugin.ui.providers;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

//Default provider if other Hud managers aren't used or if no other mod uses huds

class DefaultHudProvider implements HudProvider {
    @Override
    public void setCustomHud(@Nonnull Player player, @Nonnull PlayerRef playerRef, @Nonnull String identifier, @Nonnull CustomUIHud hud) {
        player.getHudManager().setCustomHud(playerRef, hud);
    }
}