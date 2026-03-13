package com.rajoki.injuryplugin;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class HitTrackerComponent implements Component<EntityStore> {
    // Empty component, just used to track entities that can receive hit notifications

    public HitTrackerComponent() {
    }

    @Nonnull
    @Override
    public HitTrackerComponent clone() {
        return new HitTrackerComponent();
    }
}