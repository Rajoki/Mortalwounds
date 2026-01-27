package com.rajoki.injuryplugin;

public class InjuryConfig {
    private static InjuryConfig instance;

    // Chance thresholds (roll must be < this value)
    public int bleedChance = 25;
    public int fractureChance = 10;

    // Message settings
    public boolean showMessages = false;

    // Arm fracture damage reduction (0.5 = 50% damage)
    public float armFractureDamageMultiplier = 0.5f;

    public static void set(InjuryConfig config) {
        instance = config;
    }

    public static InjuryConfig get() {
        return instance;
    }
}