package com.rajoki.injuryplugin.components.npc;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.rajoki.injuryplugin.systems.CreatureType;
import com.rajoki.injuryplugin.systems.bodypartsystems.BodyPart;

import javax.annotation.Nullable;
import java.util.*;

/**
 * NPC body part tracking with flexible anatomy support
 */
public class NPCBodyPartComponent implements Component<EntityStore> {

    // The anatomy type for this NPC
    private Anatomy anatomy = Anatomy.HUMANOID;
    private CreatureType creatureType = CreatureType.NORMAL;

    // Only track body parts that exist for this anatomy
    private final Map<String, Float> bodyPartHealth = new HashMap<>();
    private final Map<String, Float> bodyPartMaxHealth = new HashMap<>();
    private final Map<String, Set<String>> bodyPartEffects = new HashMap<>();

    private boolean initialized = false;
    private float npcMaxHealth = 100f;

    public NPCBodyPartComponent() {
        // Will be initialized with specific anatomy later
    }

    public NPCBodyPartComponent(NPCBodyPartComponent other) {
        this.initialized = other.initialized;
        this.npcMaxHealth = other.npcMaxHealth;
        this.anatomy = other.anatomy;

        for (Map.Entry<String, Float> entry : other.bodyPartHealth.entrySet()) {
            this.bodyPartHealth.put(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Float> entry : other.bodyPartMaxHealth.entrySet()) {
            this.bodyPartMaxHealth.put(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Set<String>> entry : other.bodyPartEffects.entrySet()) {
            this.bodyPartEffects.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
    }

    @Nullable
    @Override
    public NPCBodyPartComponent clone() {
        return new NPCBodyPartComponent(this);
    }

    /**
     * Initialize body part health based on NPC's max health and anatomy
     */
    public void initializeBodyParts(float npcMaxHealth, Anatomy anatomy) {
        this.npcMaxHealth = npcMaxHealth;
        this.anatomy = anatomy;

        List<BodyPart> parts = anatomy.getBodyParts();

        for (BodyPart part : parts) {
            float partMaxHealth = part.getMaxHealth(npcMaxHealth);
            bodyPartMaxHealth.put(part.name(), partMaxHealth);
            bodyPartHealth.put(part.name(), partMaxHealth);
            bodyPartEffects.put(part.name(), new HashSet<>());
        }

        this.initialized = true;
    }

    /**
     * Set the creature type (after initialization)
     */
    public void setCreatureType(CreatureType creatureType) {
        this.creatureType = creatureType;
    }

    public Anatomy getAnatomy() {
        return anatomy;
    }

    public CreatureType getCreatureType() {
        return creatureType;
    }

    /**
     * Check if this creature is immune to an effect
     */
    public boolean isImmuneTo(String effect) {
        return creatureType.isImmuneTo(effect);
    }

    /**
     * Check if this creature is resistant to an effect
     */
    public boolean isResistantTo(String effect) {
        return creatureType.isResistantTo(effect);
    }

    /**
     * Get adjusted fracture threshold for this creature type
     */
    public int getAdjustedFractureThreshold(int baseThreshold) {
        return (int)(baseThreshold * creatureType.getFractureThresholdMultiplier());
    }

    /**
     * Get all valid body parts for this NPC's anatomy
     */
    public List<BodyPart> getValidBodyParts() {
        return anatomy.getBodyParts();
    }

    /**
     * Check if this body part exists on this NPC
     */
    public boolean hasBodyPart(BodyPart part) {
        return bodyPartHealth.containsKey(part.name());
    }



    public float getBodyPartHealth(BodyPart part) {
        return bodyPartHealth.getOrDefault(part.name(), 0f);
    }

    public void setBodyPartHealth(BodyPart part, float health) {
        if (!hasBodyPart(part)) return; // Ignore if this part doesn't exist

        float maxHealth = getBodyPartMaxHealth(part);
        bodyPartHealth.put(part.name(), Math.max(0f, Math.min(health, maxHealth)));
    }

    public boolean damageBodyPart(BodyPart part, float damage) {
        if (!hasBodyPart(part)) return false; // Can't damage part that doesn't exist

        float currentHealth = getBodyPartHealth(part);
        float newHealth = Math.max(0f, currentHealth - damage);
        setBodyPartHealth(part, newHealth);

        return newHealth <= 0f && currentHealth > 0f;
    }

    public float getBodyPartMaxHealth(BodyPart part) {
        return bodyPartMaxHealth.getOrDefault(part.name(), 0f);
    }

    public boolean isBodyPartDestroyed(BodyPart part) {
        return hasBodyPart(part) && getBodyPartHealth(part) <= 0f;
    }

    public void addEffect(BodyPart part, String effectName) {
        if (!hasBodyPart(part)) return;

        Set<String> effects = bodyPartEffects.get(part.name());
        if (effects != null) {
            effects.add(effectName);
        }
    }

    public void removeEffect(BodyPart part, String effectName) {
        if (!hasBodyPart(part)) return;

        Set<String> effects = bodyPartEffects.get(part.name());
        if (effects != null) {
            effects.remove(effectName);
        }
    }

    public boolean hasEffect(BodyPart part, String effectName) {
        if (!hasBodyPart(part)) return false;

        Set<String> effects = bodyPartEffects.get(part.name());
        return effects != null && effects.contains(effectName);
    }

    public Set<String> getEffects(BodyPart part) {
        return bodyPartEffects.getOrDefault(part.name(), Collections.emptySet());
    }

    /**
     * Check if any leg-type part has a fracture
     */
    public boolean hasLegFracture() {
        for (BodyPart part : getValidBodyParts()) {
            if (part.isLeg() && hasEffect(part, "FRACTURE")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if any wing is fractured
     */
    public boolean hasWingFracture() {
        for (BodyPart part : getValidBodyParts()) {
            if (part.isWing() && hasEffect(part, "FRACTURE")) {
                return true;
            }
        }
        return false;
    }

    public boolean hasArmFracture() {
        for (BodyPart part : getValidBodyParts()) {
            if (part.isArm() && hasEffect(part, "FRACTURE")) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAnyBleed() {
        for (BodyPart part : getValidBodyParts()) {
            if (hasEffect(part, "BLEED") || hasEffect(part, "HEAVY_BLEED")) {
                return true;
            }
        }
        return false;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public float getNpcMaxHealth() {
        return npcMaxHealth;
    }
}