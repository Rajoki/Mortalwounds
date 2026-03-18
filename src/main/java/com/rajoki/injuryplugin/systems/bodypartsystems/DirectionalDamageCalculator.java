package com.rajoki.injuryplugin.systems.bodypartsystems;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.rajoki.injuryplugin.components.npc.Anatomy;

//Determines hit body part by direction, height, if crouching or not, etc

public class DirectionalDamageCalculator {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Main entry point. Determines hit body part based on anatomy type
     */
    public static BodyPart getTargetBodyPart(TransformComponent victimTransform, Vector3d attackerPosition, Anatomy anatomy) {
        return getTargetBodyPart(victimTransform, attackerPosition, anatomy, null);
    }

    /**
     * Main entry point with crouching/low attacks, determines hit body part based on anatomy type
     */
    public static BodyPart getTargetBodyPart(TransformComponent victimTransform, Vector3d attackerPosition,
                                             Anatomy anatomy, MovementStatesComponent attackerMovement) {
        // Check if attacker is crouching
        boolean isCrouching = false;
        if (attackerMovement != null && attackerMovement.getMovementStates() != null) {
            isCrouching = attackerMovement.getMovementStates().crouching;
        }

        return switch (anatomy) {
            case HUMANOID -> getHumanoidBodyPart(victimTransform, attackerPosition, isCrouching);
            case QUADRUPED -> getQuadrupedBodyPart(victimTransform, attackerPosition, isCrouching);
            case FLYING -> getFlyingBodyPart(victimTransform, attackerPosition, isCrouching);
            case SERPENT -> getSerpentBodyPart(victimTransform, attackerPosition);
            case DRAGON -> getDragonBodyPart(victimTransform, attackerPosition, isCrouching);
//            case SPIDER -> getSpiderBodyPart(victimTransform, attackerPosition, isCrouching);
        };
    }

    /**
     * Overload for backward compatibility (assumes HUMANOID)
     */
    public static BodyPart getTargetBodyPart(TransformComponent victimTransform, Vector3d attackerPosition) {
        return getHumanoidBodyPart(victimTransform, attackerPosition, false);
    }

    /**
     * HUMANOID targeting (players, goblins, skeletons, etc.)
     * Probabilistic head targeting based on height, deterministic for other parts
     */
    private static BodyPart getHumanoidBodyPart(TransformComponent victimTransform, Vector3d attackerPosition,
                                                boolean attackerIsCrouching) {
        Vector3d victimPos = copyVector(victimTransform.getPosition());
        Vector3d attackerPos = copyVector(attackerPosition);

        double dx = attackerPos.getX() - victimPos.getX();
        double dy = attackerPos.getY() - victimPos.getY();
        double dz = attackerPos.getZ() - victimPos.getZ();

        // Get relative angle
        double relativeAngle = getRelativeAngle(victimTransform, attackerPos, victimPos);

        // Determine attack direction
        boolean isFront = Math.abs(relativeAngle) <= 45;
        boolean isBack = Math.abs(relativeAngle) >= 135;
        boolean isLeftSide = relativeAngle > 45 && relativeAngle <= 135;
        boolean isRightSide = relativeAngle < -45 && relativeAngle >= -135;

        // === CROUCHING ATTACK LOGIC ===
        if (attackerIsCrouching) {
            // LOGGER.atInfo().log(String.format("Attacker is crouching!"));
            // Crouching one level below (dy < -0.5) = ALWAYS LEGS
            if (dy < -0.5) {
                if (isFront || isBack) {
                    return Math.random() < 0.5 ? BodyPart.LEFTLEG : BodyPart.RIGHTLEG;
                } else if (isLeftSide) {
                    return BodyPart.LEFTLEG;
                } else { // isRightSide
                    return BodyPart.RIGHTLEG;
                }
            }
            // Crouching on same level OR slightly below (-0.5 to 0.2) = 50% legs, 50% torso/arms
            else if (dy >= -0.5 && dy <= 0.2) {
                double roll = Math.random();

                if (isFront || isBack) {
                    // Front/Back: 50% legs, 50% torso
                    if (roll < 0.5) {
                        return Math.random() < 0.5 ? BodyPart.LEFTLEG : BodyPart.RIGHTLEG;
                    } else {
                        return BodyPart.TORSO;
                    }
                } else if (isLeftSide) {
                    // Left side: 50% left leg, 50% left arm
                    if (roll < 0.5) {
                        return BodyPart.LEFTLEG;
                    } else {
                        return BodyPart.LEFTARM;
                    }
                } else { // isRightSide
                    // Right side: 50% right leg, 50% right arm
                    if (roll < 0.5) {
                        return BodyPart.RIGHTLEG;
                    } else {
                        return BodyPart.RIGHTARM;
                    }
                }
            }
            // Crouching but attacking upward (dy > 0.2) = normal targeting logic applies below
        }

        // === STANDING ONE LEVEL BELOW (not crouching, dy between -0.5 and -1.0) ===
        // 50% legs, 50% torso/arms
        if (!attackerIsCrouching && dy < -0.5 && dy >= -1.0) {
            double roll = Math.random();

            if (isFront || isBack) {
                // Front/Back: 50% legs, 50% torso
                if (roll < 0.5) {
                    return Math.random() < 0.5 ? BodyPart.LEFTLEG : BodyPart.RIGHTLEG;
                } else {
                    return BodyPart.TORSO;
                }
            } else if (isLeftSide) {
                // Left side: 50% left leg, 50% left arm
                if (roll < 0.5) {
                    return BodyPart.LEFTLEG;
                } else {
                    return BodyPart.LEFTARM;
                }
            } else { // isRightSide
                // Right side: 50% right leg, 50% right arm
                if (roll < 0.5) {
                    return BodyPart.RIGHTLEG;
                } else {
                    return BodyPart.RIGHTARM;
                }
            }
        }

        // === VERY LOW ATTACKS (standing, more than one level below) = ALWAYS LEGS ===
        if (dy < -1.0) {
            if (isFront || isBack) {
                return Math.random() < 0.5 ? BodyPart.LEFTLEG : BodyPart.RIGHTLEG;
            } else if (isLeftSide) {
                return BodyPart.LEFTLEG;
            } else if (isRightSide) {
                return BodyPart.RIGHTLEG;
            } else {
                return Math.random() < 0.5 ? BodyPart.LEFTLEG : BodyPart.RIGHTLEG;
            }
        }

        // === HIGH ATTACKS - Probabilistic head vs body ===
        if (dy > 2.5) {
            // Very high - always head
            return BodyPart.HEAD;
        }
        else if (dy > 1.5) {
            // High - 80% head, 20% body part based on angle
            double roll = Math.random();
            if (roll < 0.8) {
                return BodyPart.HEAD;
            }
            // Fall through to determine body part based on angle
        }
        else if (dy > 0.5) {
            // Moderately high = 50% head, 50% body part based on angle
            double roll = Math.random();
            if (roll < 0.5) {
                return BodyPart.HEAD;
            }
            // Fall through to determine body part based on angle
        }

        // === SAME LEVEL OR DIDN'T HIT HEAD = Determine by angle ===
        // From sides = always arms
        if (isLeftSide) {
            return BodyPart.LEFTARM;
        } else if (isRightSide) {
            return BodyPart.RIGHTARM;
        }
        // From front or back = always torso
        else {
            return BodyPart.TORSO;
        }
    }

    /**
     * QUADRUPED targeting (wolves, bears, tigers, etc.)
     * Crouching targets legs
     */
    private static BodyPart getQuadrupedBodyPart(TransformComponent victimTransform, Vector3d attackerPosition,
                                                 boolean attackerIsCrouching) {
        Vector3d victimPos = copyVector(victimTransform.getPosition());
        Vector3d attackerPos = copyVector(attackerPosition);

        double dy = attackerPos.getY() - victimPos.getY();
        double relativeAngle = getRelativeAngle(victimTransform, attackerPos, victimPos);

        // Determine if attack is from front, back, or sides
        boolean isFront = Math.abs(relativeAngle) <= 60;
        boolean isLeft = relativeAngle > 0;

        // === CROUCHING LOGIC (same pattern as humanoid) ===
        if (attackerIsCrouching) {

//            LOGGER.atInfo().log(String.format("Quadruped: Attacker crouching. dy=%.2f", dy));

            // Crouching below target -> ALWAYS LEGS
            if (dy < -0.5) {

//                LOGGER.atInfo().log("Quadruped crouch: Below target → Legs");

                if (isFront && isLeft) return BodyPart.FRONT_LEFT_LEG;
                if (isFront && !isLeft) return BodyPart.FRONT_RIGHT_LEG;
                if (!isFront && isLeft) return BodyPart.BACK_LEFT_LEG;
                return BodyPart.BACK_RIGHT_LEG;
            }

            // Same level crouch → RANDOM roll
            else if (dy >= -0.5 && dy <= 0.2) {

//                LOGGER.atInfo().log("Quadruped crouch: Same level → Random legs/torso");

                double roll = Math.random();

                if (roll < 0.5) {

//                    LOGGER.atInfo().log("Quadruped crouch roll → Legs");

                    if (isFront && isLeft) return BodyPart.FRONT_LEFT_LEG;
                    if (isFront && !isLeft) return BodyPart.FRONT_RIGHT_LEG;
                    if (!isFront && isLeft) return BodyPart.BACK_LEFT_LEG;
                    return BodyPart.BACK_RIGHT_LEG;

                } else {

//                    LOGGER.atInfo().log("Quadruped crouch roll → Torso");

                    return BodyPart.TORSO;

                }
            }

//            LOGGER.atInfo().log("Quadruped crouch attacking upward → normal logic");

        }

        // === ABOVE ATTACKS (dy > 0.3) ===
        if (dy > 0.3) {
            if (isFront) {
                return BodyPart.HEAD;
            } else {
                return BodyPart.TORSO;
            }
        }

        // === BELOW ATTACKS (dy < -0.2) ===
        else if (dy < -0.2) {
            if (isFront && isLeft) {
                return BodyPart.FRONT_LEFT_LEG;
            } else if (isFront && !isLeft) {
                return BodyPart.FRONT_RIGHT_LEG;
            } else if (!isFront && isLeft) {
                return BodyPart.BACK_LEFT_LEG;
            } else {
                return BodyPart.BACK_RIGHT_LEG;
            }
        }

        // === SAME LEVEL ATTACKS (-0.2 <= dy <= 0.3) ===
        else {
            if (isFront) {
                return BodyPart.HEAD;
            } else {
                return BodyPart.TORSO;
            }
        }
    }

    /**
     * FLYING creature targeting (birds, bats, dragons with wings)
     * crouching targets legs
     */
    private static BodyPart getFlyingBodyPart(TransformComponent victimTransform, Vector3d attackerPosition,
                                              boolean attackerIsCrouching) {
        Vector3d victimPos = copyVector(victimTransform.getPosition());
        Vector3d attackerPos = copyVector(attackerPosition);

        double dy = attackerPos.getY() - victimPos.getY();
        double relativeAngle = getRelativeAngle(victimTransform, attackerPos, victimPos);

        if (attackerIsCrouching) {

//            LOGGER.atInfo().log(String.format("Flying: Attacker crouching. dy=%.2f", dy));

            // Crouching below target
            if (dy < -0.5) {

//                LOGGER.atInfo().log("Flying crouch: Below target → Legs");
                return Math.random() < 0.5 ? BodyPart.LEFTLEG : BodyPart.RIGHTLEG;

            }

            // Same level crouch
            else if (dy >= -0.5 && dy <= 0.2) {

//                LOGGER.atInfo().log("Flying crouch: Same level → Random legs/torso");

                if (Math.random() < 0.5) {
                    return Math.random() < 0.5 ? BodyPart.LEFTLEG : BodyPart.RIGHTLEG;
                } else {
                    return BodyPart.TORSO;
                }

            }

//            LOGGER.atInfo().log("Flying crouch attacking upward → normal logic");
        }

        // Very high attacks = head
        if (dy > 0.5) {
            return BodyPart.HEAD;
        }

        // Side attacks = wings
        if (relativeAngle > 45 && relativeAngle <= 135) {
            return BodyPart.LEFT_WING;
        } else if (relativeAngle < -45 && relativeAngle >= -135) {
            return BodyPart.RIGHT_WING;
        }

        // Low attacks = legs
        if (dy < -0.2) {
            return Math.random() < 0.5 ? BodyPart.LEFTLEG : BodyPart.RIGHTLEG;
        }

        // Default = torso
        return BodyPart.TORSO;
    }

    /**
     * SERPENT targeting (snakes, worms)
     * Only head, torso, tail = no limbs (crouching doesn't change targeting)
     */
    private static BodyPart getSerpentBodyPart(TransformComponent victimTransform, Vector3d attackerPosition) {
        Vector3d victimPos = copyVector(victimTransform.getPosition());
        Vector3d attackerPos = copyVector(attackerPosition);

        double relativeAngle = getRelativeAngle(victimTransform, attackerPos, victimPos);

        // Front attacks = head
        if (Math.abs(relativeAngle) <= 60) {
            return BodyPart.HEAD;
        }
        // Back attacks = tail
        else if (Math.abs(relativeAngle) >= 120) {
            return BodyPart.TAIL;
        }
        // Side/middle = torso
        else {
            return BodyPart.TORSO;
        }
    }

    /**
     * DRAGON targeting (complex anatomy with wings, tail, neck)
     * Crouching targets legs
     */
    private static BodyPart getDragonBodyPart(TransformComponent victimTransform, Vector3d attackerPosition,
                                              boolean attackerIsCrouching) {
        Vector3d victimPos = copyVector(victimTransform.getPosition());
        Vector3d attackerPos = copyVector(attackerPosition);

        double dy = attackerPos.getY() - victimPos.getY();
        double relativeAngle = getRelativeAngle(victimTransform, attackerPos, victimPos);

        // === CROUCHING ALWAYS TARGETS LEGS ===
        if (attackerIsCrouching) {

//            LOGGER.atInfo().log(String.format("Dragon: Attacker crouching. dy=%.2f", dy));

            boolean isFront = Math.abs(relativeAngle) <= 90;
            boolean isLeft = relativeAngle > 0;

            // Below target → legs
            if (dy < -0.5) {

//                LOGGER.atInfo().log("Dragon crouch: Below target → Legs");

                if (isFront && isLeft) return BodyPart.FRONT_LEFT_LEG;
                if (isFront && !isLeft) return BodyPart.FRONT_RIGHT_LEG;
                if (!isFront && isLeft) return BodyPart.BACK_LEFT_LEG;
                return BodyPart.BACK_RIGHT_LEG;

            }

            // Same level → random
            else if (dy >= -0.5 && dy <= 0.2) {

//                LOGGER.atInfo().log("Dragon crouch: Same level → Random legs/torso");

                if (Math.random() < 0.5) {

                    if (isFront && isLeft) return BodyPart.FRONT_LEFT_LEG;
                    if (isFront && !isLeft) return BodyPart.FRONT_RIGHT_LEG;
                    if (!isFront && isLeft) return BodyPart.BACK_LEFT_LEG;
                    return BodyPart.BACK_RIGHT_LEG;

                } else {

                    return BodyPart.TORSO;

                }
            }

//            LOGGER.atInfo().log("Dragon crouch attacking upward → normal logic");
        }

        // Very high attacks = head or neck
        if (dy > 0.8) {
            return BodyPart.HEAD;
        } else if (dy > 0.4) {
            return BodyPart.NECK;
        }

        // Wing attacks from sides
        if (relativeAngle > 45 && relativeAngle <= 135) {
            return BodyPart.LEFT_WING;
        } else if (relativeAngle < -45 && relativeAngle >= -135) {
            return BodyPart.RIGHT_WING;
        }

        // Tail from behind
        if (Math.abs(relativeAngle) >= 120) {
            return BodyPart.TAIL;
        }

        // Legs from below
        if (dy < -0.2) {
            boolean isFront = Math.abs(relativeAngle) <= 90;
            boolean isLeft = relativeAngle > 0;

            if (isFront && isLeft) return BodyPart.FRONT_LEFT_LEG;
            if (isFront && !isLeft) return BodyPart.FRONT_RIGHT_LEG;
            if (!isFront && isLeft) return BodyPart.BACK_LEFT_LEG;
            return BodyPart.BACK_RIGHT_LEG;
        }

        // Default = torso
        return BodyPart.TORSO;
    }

    /**
     * SPIDER targeting (8 legs distributed around body) - probably won't use but could be useful?
     * Crouching targets legs
     */
    private static BodyPart getSpiderBodyPart(TransformComponent victimTransform, Vector3d attackerPosition,
                                              boolean attackerIsCrouching) {
        Vector3d victimPos = copyVector(victimTransform.getPosition());
        Vector3d attackerPos = copyVector(attackerPosition);

        double dy = attackerPos.getY() - victimPos.getY();
        double relativeAngle = getRelativeAngle(victimTransform, attackerPos, victimPos);

        // === CROUCHING ALWAYS TARGETS LEGS (choose based on angle) ===
        if (attackerIsCrouching) {

//            LOGGER.atInfo().log(String.format("Spider: Attacker crouching. dy=%.2f", dy));

            double normalizedAngle = relativeAngle;
            while (normalizedAngle < 0) normalizedAngle += 360;

            // Below target → always legs
            if (dy < -0.5) {

//                LOGGER.atInfo().log("Spider crouch: Below target → Legs");

            }
            // Same level → random legs or abdomen
            else if (dy >= -0.5 && dy <= 0.2) {

//                LOGGER.atInfo().log("Spider crouch: Same level → Random legs/abdomen");

                if (Math.random() > 0.5) {
                    return BodyPart.ABDOMEN;
                }

            }

            if (normalizedAngle < 22.5 || normalizedAngle >= 337.5) return BodyPart.LEG_1;
            else if (normalizedAngle < 67.5) return BodyPart.LEG_2;
            else if (normalizedAngle < 112.5) return BodyPart.LEG_3;
            else if (normalizedAngle < 157.5) return BodyPart.LEG_4;
            else if (normalizedAngle < 202.5) return BodyPart.LEG_5;
            else if (normalizedAngle < 247.5) return BodyPart.LEG_6;
            else if (normalizedAngle < 292.5) return BodyPart.LEG_7;
            else return BodyPart.LEG_8;
        }

        // High attack = head
        if (dy > 0.3) {
            return BodyPart.HEAD;
        }

        // Very low attack = abdomen
        if (dy < -0.3) {
            return BodyPart.ABDOMEN;
        }

        // Divide 360 degrees into 8 segments (45 degrees each)
        double normalizedAngle = relativeAngle;
        while (normalizedAngle < 0) normalizedAngle += 360;

        if (normalizedAngle < 22.5 || normalizedAngle >= 337.5) return BodyPart.LEG_1;
        else if (normalizedAngle < 67.5) return BodyPart.LEG_2;
        else if (normalizedAngle < 112.5) return BodyPart.LEG_3;
        else if (normalizedAngle < 157.5) return BodyPart.LEG_4;
        else if (normalizedAngle < 202.5) return BodyPart.LEG_5;
        else if (normalizedAngle < 247.5) return BodyPart.LEG_6;
        else if (normalizedAngle < 292.5) return BodyPart.LEG_7;
        else return BodyPart.LEG_8;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Create defensive copy of vector
     */
    private static Vector3d copyVector(Vector3d original) {
        return new Vector3d(original.getX(), original.getY(), original.getZ());
    }

    /**
     * Calculate relative angle between attacker and victim's facing direction
     */
    private static double getRelativeAngle(TransformComponent victimTransform, Vector3d attackerPos, Vector3d victimPos) {
        double dx = attackerPos.getX() - victimPos.getX();
        double dz = attackerPos.getZ() - victimPos.getZ();

        double attackAngle = Math.toDegrees(Math.atan2(dx, dz));

        Vector3f rotation = victimTransform.getRotation();
        float yaw = rotation.getYaw();
        float pitch = rotation.getPitch();

        Vector3f lookDir = new Vector3f();
        lookDir.assign(yaw, pitch);

        double playerFacingAngle = Math.toDegrees(Math.atan2(lookDir.getX(), lookDir.getZ()));
        double relativeAngle = attackAngle - playerFacingAngle;

        // Normalize to -180 to 180
        while (relativeAngle > 180) relativeAngle -= 360;
        while (relativeAngle < -180) relativeAngle += 360;

        return relativeAngle;
    }

    /**
     * Alternative: Height-based targeting (simple fallback)
     */
    public static BodyPart getTargetBodyPartByHeight(Vector3d victimPos, Vector3d attackerPos) {
        double heightDiff = attackerPos.getY() - victimPos.getY();

        if (heightDiff > 1.4) {
            return BodyPart.HEAD;
        } else if (heightDiff > 0.6) {
            return BodyPart.TORSO;
        } else {
            return Math.random() < 0.5 ? BodyPart.LEFTLEG : BodyPart.RIGHTLEG;
        }
    }
}