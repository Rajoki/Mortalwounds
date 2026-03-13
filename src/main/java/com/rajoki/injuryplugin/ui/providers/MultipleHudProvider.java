package com.rajoki.injuryplugin.ui.providers;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.rajoki.injuryplugin.MortalWoundsPlugin;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;

//Dependency for multiplehud to load IF multiplehud is present

public class MultipleHudProvider implements HudProvider {
    private Object multipleHudInstance;
    private Method setCustomHudMethod;

    public MultipleHudProvider() {
        try {
            // Dynamically load MultipleHUD if present
            Class<?> multipleHudClass = Class.forName("com.buuz135.mhud.MultipleHUD");
            Method getInstanceMethod = multipleHudClass.getMethod("getInstance");
            multipleHudInstance = getInstanceMethod.invoke(null);

            setCustomHudMethod = multipleHudClass.getMethod("setCustomHud",
                    Player.class, PlayerRef.class, String.class, CustomUIHud.class);

            MortalWoundsPlugin.getPluginLogger().atInfo().log("MultipleHUD integration loaded successfully");
        } catch (Exception e) {
            MortalWoundsPlugin.getPluginLogger().atWarning().log("MultipleHUD not available: " + e.getMessage());
        }
    }

    @Override
    public void setCustomHud(@Nonnull Player player, @Nonnull PlayerRef playerRef,
                             @Nonnull String identifier, @Nonnull CustomUIHud hud) {
        try {
            if (multipleHudInstance != null && setCustomHudMethod != null) {
                setCustomHudMethod.invoke(multipleHudInstance, player, playerRef, identifier, hud);
            }
        } catch (Exception e) {
            MortalWoundsPlugin.getPluginLogger().atSevere().log("Failed to set HUD via MultipleHUD: " + e.getMessage());
        }
    }
}