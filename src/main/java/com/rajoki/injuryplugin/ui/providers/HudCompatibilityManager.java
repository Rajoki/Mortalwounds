package com.rajoki.injuryplugin.ui.providers;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.rajoki.injuryplugin.MortalWoundsPlugin;

import javax.annotation.Nonnull;

//Compatibility for multiplehud ,etc

public class HudCompatibilityManager implements HudProvider {
    private final HudProvider hudProvider;
    private final boolean multipleHudAvailable;
    private static final HudCompatibilityManager instance = new HudCompatibilityManager();

    private HudCompatibilityManager() {
        PluginBase multipleHudPlugin = PluginManager.get().getPlugin(PluginIdentifier.fromString("Buuz135:MultipleHUD"));

        if (multipleHudPlugin != null && multipleHudPlugin.isEnabled()) {
            this.hudProvider = new MultipleHudProvider();
            this.multipleHudAvailable = true;
            MortalWoundsPlugin.getPluginLogger().atInfo().log("MultipleHUD detected. Using separate HUDs.");
        } else {
            this.hudProvider = new DefaultHudProvider();
            this.multipleHudAvailable = false;
            MortalWoundsPlugin.getPluginLogger().atInfo().log("MultipleHUD not found. Using vanilla HUD (compatible with AutoMultiHud).");
        }
    }

    public boolean isMultipleHudAvailable() {
        return this.multipleHudAvailable;
    }

    @Override
    public void setCustomHud(@Nonnull Player player, @Nonnull PlayerRef playerRef, @Nonnull String identifier, @Nonnull CustomUIHud hud) {
        this.hudProvider.setCustomHud(player, playerRef, identifier, hud);
    }

    public static HudCompatibilityManager get() {
        return instance;
    }
}