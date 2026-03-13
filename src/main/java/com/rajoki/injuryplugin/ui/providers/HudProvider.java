package com.rajoki.injuryplugin.ui.providers;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

public interface HudProvider {
    void setCustomHud(@Nonnull Player player, @Nonnull PlayerRef playerRef, @Nonnull String identifier, @Nonnull CustomUIHud hud);
}