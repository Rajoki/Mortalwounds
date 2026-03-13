package com.rajoki.injuryplugin.ui.firstaidui;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class SurvKitPageSupplier implements OpenCustomUIInteraction.CustomPageSupplier {

    @Override
    public CustomUIPage tryCreate(
            Ref<EntityStore> store,
            ComponentAccessor<EntityStore> accessor,
            PlayerRef player,
            InteractionContext context
    ) {
        return new SurvKitSelectionPageNew(player);
    }
}
