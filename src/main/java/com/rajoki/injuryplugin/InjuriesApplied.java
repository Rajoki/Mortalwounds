package com.rajoki.injuryplugin;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.damage.*;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.none.simple.ApplyEffectInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class InjuriesApplied extends ApplyEffectInteraction {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final ComponentType<EntityStore, HitTrackerComponent> hitTrackerComponentType;
    // private DamageCause cachedPoisonCause;

    public InjuriesApplied(ComponentType<EntityStore, HitTrackerComponent> hitTrackerComponentType) {
        this.hitTrackerComponentType = hitTrackerComponentType;
    }


        }

