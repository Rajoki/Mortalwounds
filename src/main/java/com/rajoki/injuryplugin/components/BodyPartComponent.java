package com.rajoki.injuryplugin.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.rajoki.injuryplugin.systems.bodypartsystems.BodyPart;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Tracks damage to individual body parts.
 * Each body part has a max health (based on player's max health) and current health.
 */
public class BodyPartComponent implements Component<EntityStore> {
    public static final BuilderCodec<BodyPartComponent> CODEC;

    // Current health for each body part
    private float headHealth = -1.0f;
    private float torsoHealth = -1.0f;
    private float leftArmHealth = -1.0f;
    private float rightArmHealth = -1.0f;
    private float leftLegHealth = -1.0f;
    private float rightLegHealth = -1.0f;

    // Max health for each body part
    private float headMaxHealth = -1.0f;
    private float torsoMaxHealth = -1.0f;
    private float leftArmMaxHealth = -1.0f;
    private float rightArmMaxHealth = -1.0f;
    private float leftLegMaxHealth = -1.0f;
    private float rightLegMaxHealth = -1.0f;

    // Broken status
    private boolean headBroken = false;
    private boolean torsoBroken = false;
    private boolean leftArmBroken = false;
    private boolean rightArmBroken = false;
    private boolean leftLegBroken = false;
    private boolean rightLegBroken = false;

    // Health stored to clamp it for broken/destroyed body parts
    private float originalMaxHealth;
    private boolean hasStoredOriginalMaxHealth = false;

    private boolean initialized = false;
    private boolean dirty = false;

    // Effects stored as comma-separated strings for persistence
    private String headEffects = "";
    private String torsoEffects = "";
    private String leftArmEffects = "";
    private String rightArmEffects = "";
    private String leftLegEffects = "";
    private String rightLegEffects = "";

    public BodyPartComponent() {
    }



    // Dirty tracking
    public boolean isDirty() {
        return dirty;
    }

    public void clearDirty() {
        this.dirty = false;
    }

    public void markDirty() {
        this.dirty = true;
    }

    // Current health getters and setters
    public float getHeadHealth() {
        return headHealth;
    }

    public void setHeadHealth(float headHealth) {
        float clamped = Math.max(0f, headHealth);
        if (this.headHealth != clamped) {
            this.headHealth = clamped;
            markDirty();
        }
    }

    public float getTorsoHealth() {
        return torsoHealth;
    }

    public void setTorsoHealth(float torsoHealth) {
        float clamped = Math.max(0f, torsoHealth);
        if (this.torsoHealth != clamped) {
            this.torsoHealth = clamped;
            markDirty();
        }
    }

    public float getLeftArmHealth() {
        return leftArmHealth;
    }

    public void setLeftArmHealth(float leftArmHealth) {
        float clamped = Math.max(0f, leftArmHealth);
        if (this.leftArmHealth != clamped) {
            this.leftArmHealth = clamped;
            markDirty();
        }
    }

    public float getRightArmHealth() {
        return rightArmHealth;
    }

    public void setRightArmHealth(float rightArmHealth) {
        float clamped = Math.max(0f, rightArmHealth);
        if (this.rightArmHealth != clamped) {
            this.rightArmHealth = clamped;
            markDirty();
        }
    }

    public float getLeftLegHealth() {
        return leftLegHealth;
    }

    public void setLeftLegHealth(float leftLegHealth) {
        float clamped = Math.max(0f, leftLegHealth);
        if (this.leftLegHealth != clamped) {
            this.leftLegHealth = clamped;
            markDirty();
        }
    }

    public float getRightLegHealth() {
        return rightLegHealth;
    }

    public void setRightLegHealth(float rightLegHealth) {
        float clamped = Math.max(0f, rightLegHealth);
        if (this.rightLegHealth != clamped) {
            this.rightLegHealth = clamped;
            markDirty();
        }
    }

    // Max health getters and setters
    public float getHeadMaxHealth() {
        return headMaxHealth;
    }

    public void setHeadMaxHealth(float headMaxHealth) {
        if (this.headMaxHealth != headMaxHealth) {
            this.headMaxHealth = headMaxHealth;
            markDirty();
        }
    }

    public float getTorsoMaxHealth() {
        return torsoMaxHealth;
    }

    public void setTorsoMaxHealth(float torsoMaxHealth) {
        if (this.torsoMaxHealth != torsoMaxHealth) {
            this.torsoMaxHealth = torsoMaxHealth;
            markDirty();
        }
    }

    public float getLeftArmMaxHealth() {
        return leftArmMaxHealth;
    }

    public void setLeftArmMaxHealth(float leftArmMaxHealth) {
        if (this.leftArmMaxHealth != leftArmMaxHealth) {
            this.leftArmMaxHealth = leftArmMaxHealth;
            markDirty();
        }
    }

    public float getRightArmMaxHealth() {
        return rightArmMaxHealth;
    }

    public void setRightArmMaxHealth(float rightArmMaxHealth) {
        if (this.rightArmMaxHealth != rightArmMaxHealth) {
            this.rightArmMaxHealth = rightArmMaxHealth;
            markDirty();
        }
    }

    public float getLeftLegMaxHealth() {
        return leftLegMaxHealth;
    }

    public void setLeftLegMaxHealth(float leftLegMaxHealth) {
        if (this.leftLegMaxHealth != leftLegMaxHealth) {
            this.leftLegMaxHealth = leftLegMaxHealth;
            markDirty();
        }
    }

    public float getRightLegMaxHealth() {
        return rightLegMaxHealth;
    }

    public void setRightLegMaxHealth(float rightLegMaxHealth) {
        if (this.rightLegMaxHealth != rightLegMaxHealth) {
            this.rightLegMaxHealth = rightLegMaxHealth;
            markDirty();
        }
    }

    // Broken status getters and setters
    public boolean isHeadBroken() {
        return headBroken;
    }

    public void setHeadBroken(boolean broken) {
        this.headBroken = broken;
    }

    public boolean isTorsoBroken() {
        return torsoBroken;
    }

    public void setTorsoBroken(boolean broken) {
        this.torsoBroken = broken;
    }

    public boolean isLeftArmBroken() {
        return leftArmBroken;
    }

    public void setLeftArmBroken(boolean broken) {
        this.leftArmBroken = broken;
    }

    public boolean isRightArmBroken() {
        return rightArmBroken;
    }

    public void setRightArmBroken(boolean broken) {
        this.rightArmBroken = broken;
    }

    public boolean isLeftLegBroken() {
        return leftLegBroken;
    }

    public void setLeftLegBroken(boolean broken) {
        this.leftLegBroken = broken;
    }

    public boolean isRightLegBroken() {
        return rightLegBroken;
    }

    public void setRightLegBroken(boolean broken) {
        this.rightLegBroken = broken;
    }


    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        if (this.initialized != initialized) {
            this.initialized = initialized;
            markDirty();
        }
    }

    // Effect string getters/setters for CODEC
    public String getHeadEffects() {
        return headEffects;
    }

    public void setHeadEffects(String effects) {
        this.headEffects = effects != null ? effects : "";
    }

    public String getTorsoEffects() {
        return torsoEffects;
    }

    public void setTorsoEffects(String effects) {
        this.torsoEffects = effects != null ? effects : "";
    }

    public String getLeftArmEffects() {
        return leftArmEffects;
    }

    public void setLeftArmEffects(String effects) {
        this.leftArmEffects = effects != null ? effects : "";
    }

    public String getRightArmEffects() {
        return rightArmEffects;
    }

    public void setRightArmEffects(String effects) {
        this.rightArmEffects = effects != null ? effects : "";
    }

    public String getLeftLegEffects() {
        return leftLegEffects;
    }

    public void setLeftLegEffects(String effects) {
        this.leftLegEffects = effects != null ? effects : "";
    }

    public String getRightLegEffects() {
        return rightLegEffects;
    }

    public void setRightLegEffects(String effects) {
        this.rightLegEffects = effects != null ? effects : "";
    }

    // Helper to get the effects string for a body part
    private String getEffectsString(BodyPart part) {
        return switch (part) {
            case HEAD -> headEffects;
            case TORSO -> torsoEffects;
            case LEFTARM -> leftArmEffects;
            case RIGHTARM -> rightArmEffects;
            case LEFTLEG -> leftLegEffects;
            case RIGHTLEG -> rightLegEffects;
            default -> ""; // Non-humanoid parts don't have effects in BodyPartComponent
        };
    }

    // Helper to set the effects string for a body part
    private void setEffectsString(BodyPart part, String effects) {
        switch (part) {
            case HEAD -> headEffects = effects;
            case TORSO -> torsoEffects = effects;
            case LEFTARM -> leftArmEffects = effects;
            case RIGHTARM -> rightArmEffects = effects;
            case LEFTLEG -> leftLegEffects = effects;
            case RIGHTLEG -> rightLegEffects = effects;
            default -> {} // Non-humanoid parts don't have effects in BodyPartComponent
        }
    }

    // Helper to convert string to set
    private Set<String> stringToSet(String str) {
        Set<String> set = new HashSet<>();
        if (str != null && !str.isEmpty()) {
            for (String effect : str.split(",")) {
                if (!effect.isEmpty()) {
                    set.add(effect.trim());
                }
            }
        }
        return set;
    }

    // Helper to convert set to string
    private String setToString(Set<String> set) {
        if (set.isEmpty()) {
            return "";
        }
        return String.join(",", set);
    }

    // Helper methods for body parts
    public float getBodyPartHealth(BodyPart part) {
        return switch (part) {
            case HEAD -> headHealth;
            case TORSO -> torsoHealth;
            case LEFTARM -> leftArmHealth;
            case RIGHTARM -> rightArmHealth;
            case LEFTLEG -> leftLegHealth;
            case RIGHTLEG -> rightLegHealth;
            default -> 0f; // Non-humanoid parts return 0
        };
    }

    public void setBodyPartHealth(BodyPart part, float health) {
        switch (part) {
            case HEAD -> setHeadHealth(health);
            case TORSO -> setTorsoHealth(health);
            case LEFTARM -> setLeftArmHealth(health);
            case RIGHTARM -> setRightArmHealth(health);
            case LEFTLEG -> setLeftLegHealth(health);
            case RIGHTLEG -> setRightLegHealth(health);
            default -> {} // Non-humanoid parts ignored
        }
    }

    public float getBodyPartMaxHealth(BodyPart part) {
        return switch (part) {
            case HEAD -> headMaxHealth;
            case TORSO -> torsoMaxHealth;
            case LEFTARM -> leftArmMaxHealth;
            case RIGHTARM -> rightArmMaxHealth;
            case LEFTLEG -> leftLegMaxHealth;
            case RIGHTLEG -> rightLegMaxHealth;
            default -> 0f; // Non-humanoid parts return 0
        };
    }

    public void setBodyPartMaxHealth(BodyPart part, float maxHealth) {
        switch (part) {
            case HEAD -> setHeadMaxHealth(maxHealth);
            case TORSO -> setTorsoMaxHealth(maxHealth);
            case LEFTARM -> setLeftArmMaxHealth(maxHealth);
            case RIGHTARM -> setRightArmMaxHealth(maxHealth);
            case LEFTLEG -> setLeftLegMaxHealth(maxHealth);
            case RIGHTLEG -> setRightLegMaxHealth(maxHealth);
            default -> {} // Non-humanoid parts ignored
        }
    }

    public boolean isBodyPartBroken(BodyPart part) {
        return switch (part) {
            case HEAD -> headBroken;
            case TORSO -> torsoBroken;
            case LEFTARM -> leftArmBroken;
            case RIGHTARM -> rightArmBroken;
            case LEFTLEG -> leftLegBroken;
            case RIGHTLEG -> rightLegBroken;
            default -> false; // Non-humanoid parts always return false
        };
    }

    public void setBodyPartBroken(BodyPart part, boolean broken) {
        switch (part) {
            case HEAD -> setHeadBroken(broken);
            case TORSO -> setTorsoBroken(broken);
            case LEFTARM -> setLeftArmBroken(broken);
            case RIGHTARM -> setRightArmBroken(broken);
            case LEFTLEG -> setLeftLegBroken(broken);
            case RIGHTLEG -> setRightLegBroken(broken);
            default -> {} // Non-humanoid parts ignored
        }
    }

    public void damageBodyPart(BodyPart part, float damage) {
        float currentHealth = getBodyPartHealth(part);
        setBodyPartHealth(part, currentHealth - damage);
    }

    // Effect management methods
    public void addBodyPartEffect(BodyPart part, String effectName) {
        Set<String> effects = stringToSet(getEffectsString(part));
        effects.add(effectName);
        setEffectsString(part, setToString(effects));
        markDirty();
    }

    public void removeBodyPartEffect(BodyPart part, String effectName) {
        Set<String> effects = stringToSet(getEffectsString(part));
        effects.remove(effectName);
        setEffectsString(part, setToString(effects));
        markDirty();
    }

    public boolean hasBodyPartEffect(BodyPart part, String effectName) {
        return stringToSet(getEffectsString(part)).contains(effectName);
    }

    public Set<String> getBodyPartEffects(BodyPart part) {
        return stringToSet(getEffectsString(part));
    }

    @Nonnull
    @Override
    public BodyPartComponent clone() {
        BodyPartComponent cloned = new BodyPartComponent();

        cloned.headHealth = this.headHealth;
        cloned.torsoHealth = this.torsoHealth;
        cloned.leftArmHealth = this.leftArmHealth;
        cloned.rightArmHealth = this.rightArmHealth;
        cloned.leftLegHealth = this.leftLegHealth;
        cloned.rightLegHealth = this.rightLegHealth;

        cloned.headMaxHealth = this.headMaxHealth;
        cloned.torsoMaxHealth = this.torsoMaxHealth;
        cloned.leftArmMaxHealth = this.leftArmMaxHealth;
        cloned.rightArmMaxHealth = this.rightArmMaxHealth;
        cloned.leftLegMaxHealth = this.leftLegMaxHealth;
        cloned.rightLegMaxHealth = this.rightLegMaxHealth;

        cloned.headBroken = this.headBroken;
        cloned.torsoBroken = this.torsoBroken;
        cloned.leftArmBroken = this.leftArmBroken;
        cloned.rightArmBroken = this.rightArmBroken;
        cloned.leftLegBroken = this.leftLegBroken;
        cloned.rightLegBroken = this.rightLegBroken;

        cloned.initialized = this.initialized;

        // Clone effects
        cloned.headEffects = this.headEffects;
        cloned.torsoEffects = this.torsoEffects;
        cloned.leftArmEffects = this.leftArmEffects;
        cloned.rightArmEffects = this.rightArmEffects;
        cloned.leftLegEffects = this.leftLegEffects;
        cloned.rightLegEffects = this.rightLegEffects;

        return cloned;
    }

    static {
        CODEC = BuilderCodec.builder(BodyPartComponent.class, BodyPartComponent::new)
                // Current health fields
                .addField(new KeyedCodec<>("HeadHealth", Codec.FLOAT),
                        BodyPartComponent::setHeadHealth,
                        BodyPartComponent::getHeadHealth)
                .addField(new KeyedCodec<>("TorsoHealth", Codec.FLOAT),
                        BodyPartComponent::setTorsoHealth,
                        BodyPartComponent::getTorsoHealth)
                .addField(new KeyedCodec<>("LeftArmHealth", Codec.FLOAT),
                        BodyPartComponent::setLeftArmHealth,
                        BodyPartComponent::getLeftArmHealth)
                .addField(new KeyedCodec<>("RightArmHealth", Codec.FLOAT),
                        BodyPartComponent::setRightArmHealth,
                        BodyPartComponent::getRightArmHealth)
                .addField(new KeyedCodec<>("LeftLegHealth", Codec.FLOAT),
                        BodyPartComponent::setLeftLegHealth,
                        BodyPartComponent::getLeftLegHealth)
                .addField(new KeyedCodec<>("RightLegHealth", Codec.FLOAT),
                        BodyPartComponent::setRightLegHealth,
                        BodyPartComponent::getRightLegHealth)

                // Max health fields
                .addField(new KeyedCodec<>("HeadMaxHealth", Codec.FLOAT),
                        BodyPartComponent::setHeadMaxHealth,
                        BodyPartComponent::getHeadMaxHealth)
                .addField(new KeyedCodec<>("TorsoMaxHealth", Codec.FLOAT),
                        BodyPartComponent::setTorsoMaxHealth,
                        BodyPartComponent::getTorsoMaxHealth)
                .addField(new KeyedCodec<>("LeftArmMaxHealth", Codec.FLOAT),
                        BodyPartComponent::setLeftArmMaxHealth,
                        BodyPartComponent::getLeftArmMaxHealth)
                .addField(new KeyedCodec<>("RightArmMaxHealth", Codec.FLOAT),
                        BodyPartComponent::setRightArmMaxHealth,
                        BodyPartComponent::getRightArmMaxHealth)
                .addField(new KeyedCodec<>("LeftLegMaxHealth", Codec.FLOAT),
                        BodyPartComponent::setLeftLegMaxHealth,
                        BodyPartComponent::getLeftLegMaxHealth)
                .addField(new KeyedCodec<>("RightLegMaxHealth", Codec.FLOAT),
                        BodyPartComponent::setRightLegMaxHealth,
                        BodyPartComponent::getRightLegMaxHealth)

                // Broken status fields
                .addField(new KeyedCodec<>("HeadBroken", Codec.BOOLEAN),
                        BodyPartComponent::setHeadBroken,
                        BodyPartComponent::isHeadBroken)
                .addField(new KeyedCodec<>("TorsoBroken", Codec.BOOLEAN),
                        BodyPartComponent::setTorsoBroken,
                        BodyPartComponent::isTorsoBroken)
                .addField(new KeyedCodec<>("LeftArmBroken", Codec.BOOLEAN),
                        BodyPartComponent::setLeftArmBroken,
                        BodyPartComponent::isLeftArmBroken)
                .addField(new KeyedCodec<>("RightArmBroken", Codec.BOOLEAN),
                        BodyPartComponent::setRightArmBroken,
                        BodyPartComponent::isRightArmBroken)
                .addField(new KeyedCodec<>("LeftLegBroken", Codec.BOOLEAN),
                        BodyPartComponent::setLeftLegBroken,
                        BodyPartComponent::isLeftLegBroken)
                .addField(new KeyedCodec<>("RightLegBroken", Codec.BOOLEAN),
                        BodyPartComponent::setRightLegBroken,
                        BodyPartComponent::isRightLegBroken)

                // Effect fields - stored as comma-separated strings
                .addField(new KeyedCodec<>("HeadEffects", Codec.STRING),
                        BodyPartComponent::setHeadEffects,
                        BodyPartComponent::getHeadEffects)
                .addField(new KeyedCodec<>("TorsoEffects", Codec.STRING),
                        BodyPartComponent::setTorsoEffects,
                        BodyPartComponent::getTorsoEffects)
                .addField(new KeyedCodec<>("LeftArmEffects", Codec.STRING),
                        BodyPartComponent::setLeftArmEffects,
                        BodyPartComponent::getLeftArmEffects)
                .addField(new KeyedCodec<>("RightArmEffects", Codec.STRING),
                        BodyPartComponent::setRightArmEffects,
                        BodyPartComponent::getRightArmEffects)
                .addField(new KeyedCodec<>("LeftLegEffects", Codec.STRING),
                        BodyPartComponent::setLeftLegEffects,
                        BodyPartComponent::getLeftLegEffects)
                .addField(new KeyedCodec<>("RightLegEffects", Codec.STRING),
                        BodyPartComponent::setRightLegEffects,
                        BodyPartComponent::getRightLegEffects)

                .append(new KeyedCodec<>("OriginalMaxHealth", Codec.FLOAT),
                        (component, value, context) -> component.originalMaxHealth = value,
                        (component, context) -> component.originalMaxHealth)
                .add()

                .append(new KeyedCodec<>("HasStoredOriginalMaxHealth", Codec.BOOLEAN),
                        (component, value, context) -> component.hasStoredOriginalMaxHealth = value,
                        (component, context) -> component.hasStoredOriginalMaxHealth)
                .add()

                // Initialized field
                .addField(new KeyedCodec<>("Initialized", Codec.BOOLEAN),
                        BodyPartComponent::setInitialized,
                        BodyPartComponent::isInitialized)
                .build();
    }

    // Getters and setters for destroyed body parts
    public float getOriginalMaxHealth() {
        return originalMaxHealth;
    }

    public void setOriginalMaxHealth(float maxHealth) {
        this.originalMaxHealth = maxHealth;
    }

    public boolean hasStoredOriginalMaxHealth() {
        return hasStoredOriginalMaxHealth;
    }

    public void setStoredOriginalMaxHealth(boolean stored) {
        this.hasStoredOriginalMaxHealth = stored;
    }

    private final Map<String, Float> customFloats = new HashMap<>();

    public void setCustomFloat(String key, float value) {
        customFloats.put(key, value);
    }

    public Float getCustomFloat(String key) {
        return customFloats.get(key);
    }

    public void removeCustomFloat(String key) {
        customFloats.remove(key);
    }

}