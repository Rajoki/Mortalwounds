package com.rajoki.injuryplugin.components.npc;

import com.rajoki.injuryplugin.systems.bodypartsystems.BodyPart;

import java.util.Arrays;
import java.util.List;

/**
 * Defines different anatomical structures for various creature types
 */
public enum Anatomy {
    HUMANOID(Arrays.asList(
            BodyPart.HEAD,
            BodyPart.TORSO,
            BodyPart.LEFTARM,
            BodyPart.RIGHTARM,
            BodyPart.LEFTLEG,
            BodyPart.RIGHTLEG
    )),

    QUADRUPED(Arrays.asList(
            BodyPart.HEAD,
            BodyPart.TORSO,
            BodyPart.FRONT_LEFT_LEG,
            BodyPart.FRONT_RIGHT_LEG,
            BodyPart.BACK_LEFT_LEG,
            BodyPart.BACK_RIGHT_LEG
    )),

    FLYING(Arrays.asList(
            BodyPart.HEAD,
            BodyPart.TORSO,
            BodyPart.LEFT_WING,
            BodyPart.RIGHT_WING,
            BodyPart.LEFTLEG,
            BodyPart.RIGHTLEG
    )),

    SERPENT(Arrays.asList(
            BodyPart.HEAD,
            BodyPart.TORSO,
            BodyPart.TAIL
    )),

    // Boss-specific anatomies, Dragon as example
    DRAGON(Arrays.asList(
            BodyPart.HEAD,
            BodyPart.NECK,
            BodyPart.TORSO,
            BodyPart.LEFT_WING,
            BodyPart.RIGHT_WING,
            BodyPart.FRONT_LEFT_LEG,
            BodyPart.FRONT_RIGHT_LEG,
            BodyPart.BACK_LEFT_LEG,
            BodyPart.BACK_RIGHT_LEG,
            BodyPart.TAIL
    ));

// Spider was for testing, probably won't use.

    //    SPIDER(Arrays.asList(
//            BodyPart.HEAD,
//            BodyPart.ABDOMEN,
//            BodyPart.LEG_1,
//            BodyPart.LEG_2,
//            BodyPart.LEG_3,
//            BodyPart.LEG_4,
//            BodyPart.LEG_5,
//            BodyPart.LEG_6,
//            BodyPart.LEG_7,
//            BodyPart.LEG_8
//    ));

    private final List<BodyPart> bodyParts;

    Anatomy(List<BodyPart> bodyParts) {
        this.bodyParts = bodyParts;
    }

    public List<BodyPart> getBodyParts() {
        return bodyParts;
    }

    /**
     * Determine anatomy from NPC type
     */
    public static Anatomy fromNPCType(String npcTypeId) {
        if (npcTypeId == null) return HUMANOID;

        String lower = npcTypeId.toLowerCase();

        //Defines NPCs into different anatomies based on name

        // Quadrupeds
        if (lower.contains("wolf") || lower.contains("bear") ||
                lower.contains("cat") || lower.contains("dog") ||
                lower.contains("tiger") || lower.contains("lion") || lower.contains("spider")) {
            return QUADRUPED;
        }

        // Flying creatures
        if (lower.contains("bird") || lower.contains("eagle") ||
                lower.contains("hawk") || lower.contains("bat")) {
            return FLYING;
        }

        // Serpents
        if (lower.contains("snake") || lower.contains("serpent") ||
                lower.contains("worm")) {
            return SERPENT;
        }

        // Bosses
        if (lower.contains("dragon")) {
            return DRAGON;
        }

//        if (lower.contains("spider")) {
//            return SPIDER;
//        }

        // Default to humanoid
        return HUMANOID;
    }
}