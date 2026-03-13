package com.rajoki.injuryplugin.systems.bodypartsystems;

/**
 * Universal body parts for all creature types
 */
public enum BodyPart {
    // Humanoid parts
    HEAD(0.16f),
    TORSO(0.20f),
    LEFTARM(0.16f),
    RIGHTARM(0.16f),
    LEFTLEG(0.16f),
    RIGHTLEG(0.16f),

    // Quadruped parts
    FRONT_LEFT_LEG(0.15f),
    FRONT_RIGHT_LEG(0.15f),
    BACK_LEFT_LEG(0.15f),
    BACK_RIGHT_LEG(0.15f),

    // Flying creature parts
    LEFT_WING(0.12f),
    RIGHT_WING(0.12f),

    // Dragon/boss? specific
    NECK(0.10f),
    TAIL(0.15f),
    ABDOMEN(0.20f),

    // Spider legs, for testing, probably won't use
    LEG_1(0.08f),
    LEG_2(0.08f),
    LEG_3(0.08f),
    LEG_4(0.08f),
    LEG_5(0.08f),
    LEG_6(0.08f),
    LEG_7(0.08f),
    LEG_8(0.08f);



    private final float healthPercentage;



    BodyPart(float healthPercentage) {
        this.healthPercentage = healthPercentage;
    }

    public float getHealthPercentage() {
        return healthPercentage;
    }

    public float getMaxHealth(float entityMaxHealth) {
        return entityMaxHealth * healthPercentage;
    }

    public String getDisplayName() {
        switch (this) {
            // Humanoid
            case HEAD: return "Head";
            case TORSO: return "Torso";
            case LEFTARM: return "Left Arm";
            case RIGHTARM: return "Right Arm";
            case LEFTLEG: return "Left Leg";
            case RIGHTLEG: return "Right Leg";

            // Quadruped
            case FRONT_LEFT_LEG: return "Front Left Leg";
            case FRONT_RIGHT_LEG: return "Front Right Leg";
            case BACK_LEFT_LEG: return "Back Left Leg";
            case BACK_RIGHT_LEG: return "Back Right Leg";

            // Flying
            case LEFT_WING: return "Left Wing";
            case RIGHT_WING: return "Right Wing";

            // Dragon/Boss
            case NECK: return "Neck";
            case TAIL: return "Tail";
            case ABDOMEN: return "Abdomen";

            // Spider
            case LEG_1: return "Leg 1";
            case LEG_2: return "Leg 2";
            case LEG_3: return "Leg 3";
            case LEG_4: return "Leg 4";
            case LEG_5: return "Leg 5";
            case LEG_6: return "Leg 6";
            case LEG_7: return "Leg 7";
            case LEG_8: return "Leg 8";

            default: return name();
        }
    }

    /**
     * Check if this is a leg-type part (for movement penalties)
     */
    public boolean isLeg() {
        return this == LEFTLEG || this == RIGHTLEG ||
                this == FRONT_LEFT_LEG || this == FRONT_RIGHT_LEG ||
                this == BACK_LEFT_LEG || this == BACK_RIGHT_LEG ||
                this == LEG_1 || this == LEG_2 || this == LEG_3 || this == LEG_4 ||
                this == LEG_5 || this == LEG_6 || this == LEG_7 || this == LEG_8;
    }

    /**
     * Check if this is a wing-type part (for flying penalties, possibly in future)
     */
    public boolean isWing() {
        return this == LEFT_WING || this == RIGHT_WING;
    }

    /**
     * Check if this is an arm-type part (for damage penalties)
     */
    public boolean isArm() {
        return this == LEFTARM || this == RIGHTARM;
    }


}

