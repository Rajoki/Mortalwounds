package com.rajoki.injuryplugin.systems;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Defines creature types and their effect immunities/resistances
 */
public enum CreatureType {
    // Undead - can't bleed, harder to fracture
    SKELETON(
            new HashSet<>(Arrays.asList("BLEED", "HEAVY_BLEED")),  // Immune to bleeding
            new HashSet<>(),               // Resistant to fractures
            1.0f  // Fracture threshold multiplier (2x harder to fracture)
    ),

    ZOMBIE(
            new HashSet<>(Arrays.asList("BLEED", "HEAVY_BLEED")),  // Can't bleed (already dead)
            new HashSet<>(),
            1.0f
    ),

    GHOST(
            new HashSet<>(Arrays.asList("BLEED", "HEAVY_BLEED", "FRACTURE")),  // Immune to physical damage effects
            new HashSet<>(),
            1.0f
    ),

    // Constructs/golems,  can't bleed, very hard to fracture
    GOLEM(
            new HashSet<>(Arrays.asList("BLEED", "HEAVY_BLEED")),  // Stone/metal can't bleed
            new HashSet<>(Arrays.asList("FRACTURE")),               // Very resistant to fractures
            3.0f  // 3x harder to fracture (basically requires part destruction)
    ),

    ANIMATED_ARMOR(
            new HashSet<>(Arrays.asList("BLEED", "HEAVY_BLEED")),
            new HashSet<>(Arrays.asList("FRACTURE")),
            2.5f
    ),

    // Elemental, no physical effects
    FIRE_ELEMENTAL(
            new HashSet<>(Arrays.asList("BLEED", "HEAVY_BLEED", "FRACTURE")),
            new HashSet<>(),
            1.0f
    ),

    WATER_ELEMENTAL(
            new HashSet<>(Arrays.asList("BLEED", "HEAVY_BLEED", "FRACTURE")),
            new HashSet<>(),
            1.0f
    ),

    // Slimes/Oozes = can't fracture bones (no bones?)
    SLIME(
            new HashSet<>(Arrays.asList("FRACTURE")),  // No bones to break
            new HashSet<>(Arrays.asList("BLEED")),      // Resistant but not immune to bleeding
            1.5f  // Harder to make them bleed
    ),

    // Plants - can't bleed blood, but can be "damaged"
    PLANT(
            new HashSet<>(Arrays.asList("BLEED", "HEAVY_BLEED")),  // No blood
            new HashSet<>(),
            1.0f
    ),

    // Mechanical = can't bleed, very hard to fracture
    MECHANICAL(
            new HashSet<>(Arrays.asList("BLEED", "HEAVY_BLEED")),
            new HashSet<>(Arrays.asList("FRACTURE")),
            4.0f  // Extremely hard to "fracture" metal
    ),

    // Normal creatures = no immunities
    NORMAL(
            new HashSet<>(),
            new HashSet<>(),
            1.0f
    );

    private final Set<String> immuneEffects;           // Complete immunity
    private final Set<String> resistantEffects;        // Can only get through part destruction
    private final float fractureThresholdMultiplier;   // Multiply threshold (higher = harder)

    CreatureType(Set<String> immuneEffects, Set<String> resistantEffects, float fractureThresholdMultiplier) {
        this.immuneEffects = immuneEffects;
        this.resistantEffects = resistantEffects;
        this.fractureThresholdMultiplier = fractureThresholdMultiplier;
    }

    /**
     * Check if this creature type is completely immune to an effect
     */
    public boolean isImmuneTo(String effect) {
        return immuneEffects.contains(effect);
    }

    /**
     * Check if this creature type is resistant to an effect
     * (can only get it through part destruction, not random chance)
     */
    public boolean isResistantTo(String effect) {
        return resistantEffects.contains(effect);
    }

    /**
     * Get the fracture threshold multiplier for this creature type
     */
    public float getFractureThresholdMultiplier() {
        return fractureThresholdMultiplier;
    }

    /**
     * Determine creature type from NPC type ID
     */
    public static CreatureType fromNPCType(String npcTypeId) {
        if (npcTypeId == null) return NORMAL;

        String lower = npcTypeId.toLowerCase();

        // Undead
        if (lower.contains("skeleton")) return SKELETON;
        if (lower.contains("zombie")) return ZOMBIE;
        if (lower.contains("ghost") || lower.contains("spirit") || lower.contains("wraith")) return GHOST;

        // Constructs
        if (lower.contains("golem")) return GOLEM;
        if (lower.contains("armor") && lower.contains("animated")) return ANIMATED_ARMOR;

        // Elementals
        if (lower.contains("fire") && lower.contains("elemental")) return FIRE_ELEMENTAL;
        if (lower.contains("water") && lower.contains("elemental")) return WATER_ELEMENTAL;

        // Slimes
        if (lower.contains("slime") || lower.contains("ooze")) return SLIME;

        // Plants
        if (lower.contains("treant") || lower.contains("vine") || lower.contains("plant")) return PLANT;

        // Mechanical
        if (lower.contains("robot") || lower.contains("automaton") || lower.contains("mechanical")) return MECHANICAL;

        return NORMAL;
    }
}