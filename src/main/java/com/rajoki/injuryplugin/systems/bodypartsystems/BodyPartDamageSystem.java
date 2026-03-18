package com.rajoki.injuryplugin.systems.bodypartsystems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.rajoki.injuryplugin.components.BodyPartComponent;
import com.rajoki.injuryplugin.components.npc.Anatomy;
import com.rajoki.injuryplugin.config.MortalWoundsConfig;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.rajoki.injuryplugin.utils.InjuryTextHelper;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

// Damage system for body parts/limbs on players

public class BodyPartDamageSystem extends EntityEventSystem<EntityStore, Damage> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final ComponentType<EntityStore, BodyPartComponent> bodyPartComponentType;

    public BodyPartDamageSystem(ComponentType<EntityStore, BodyPartComponent> bodyPartComponentType) {
        super(Damage.class);
        this.bodyPartComponentType = bodyPartComponentType;
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getInspectDamageGroup();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), this.bodyPartComponentType);
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {

        if (damage.getAmount() <= 0) {
            return;
        }

        // Skip bleed damage, it's already handled by BodyPartBleedSystem
        DamageCause damageCause = damage.getCause();
        if (damageCause != null && "Bleed".equals(damageCause.getId())) {
            return; // BodyPartBleedSystem already applied the damage to the correct body part
        }

        Ref<EntityStore> targetRef = chunk.getReferenceTo(index);
        PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        BodyPartComponent bodyPartComponent = chunk.getComponent(index, this.bodyPartComponentType);

        if (bodyPartComponent == null || !bodyPartComponent.isInitialized()) {
            return;
        }

        Damage.Source source = damage.getSource();
        damageCause = damage.getCause();
        BodyPart hitPart;
        boolean isFallDamage = false;
        boolean isEnvironmental = false;

        // Check if it's fall damage
        if (damageCause != null && damageCause.getId().toLowerCase().contains("fall")) {
            isFallDamage = true;
            hitPart = ThreadLocalRandom.current().nextBoolean() ? BodyPart.LEFTLEG : BodyPart.RIGHTLEG;

//            LOGGER.atInfo().log(String.format("[FALL DAMAGE] %.1f damage to %s",
//                    damage.getAmount(), hitPart.getDisplayName()));
        }
        // Determine hit part from entity source
        else if (source instanceof Damage.EntitySource entitySource) {
            TransformComponent victimTransform = store.getComponent(targetRef, TransformComponent.getComponentType());
            if (victimTransform == null) {
                return;
            }

            Ref<EntityStore> attackerRef = entitySource.getRef();
            TransformComponent attackerTransform = store.getComponent(attackerRef, TransformComponent.getComponentType());
            // Get attacker's movement component, for crouching/low attacks
            MovementStatesComponent attackerMovement = store.getComponent(attackerRef, MovementStatesComponent.getComponentType());


            if (attackerTransform != null) {
                Vector3d attackerPos = attackerTransform.getPosition();
                hitPart = DirectionalDamageCalculator.getTargetBodyPart(victimTransform, attackerPos, Anatomy.fromNPCType("Humanoid"),
                        attackerMovement);
            } else {
                hitPart = BodyPart.TORSO;
            }
        } else {
            // Other environmental damage
            isEnvironmental = true;
            BodyPart[] parts = BodyPart.values();
            hitPart = parts[ThreadLocalRandom.current().nextInt(parts.length)];

            String causeId = damageCause != null ? damageCause.getId() : "Unknown";
            //LOGGER.atInfo().log(String.format("[ENVIRONMENTAL] %s damage to %s", causeId, hitPart.getDisplayName()));
        }

        float damageAmount = damage.getAmount();

        // Apply damage with overflow handling
        float overflow = applyDamageWithOverflow(bodyPartComponent, hitPart, damageAmount, playerRef);

        // Check if body part broke
        float newHealth = bodyPartComponent.getBodyPartHealth(hitPart);
        if (newHealth <= 0f && !bodyPartComponent.isBodyPartBroken(hitPart)) {
            bodyPartComponent.setBodyPartBroken(hitPart, true);
            bodyPartComponent.setBodyPartHealth(hitPart, 0f);
            // Play bone break sound very loud for destroyed limb
            playSoundEffect(targetRef, store, "SFX_Bone_Break", 1.5f, 0.7f); // Loudest, lowest pitch

            // Display floating text
            TransformComponent transform = store.getComponent(targetRef, TransformComponent.getComponentType());
            if (transform != null) {
                InjuryTextHelper.sendFractureText(targetRef, hitPart, transform.getPosition(), store, commandBuffer);
            }

            // Display limb destroyed text
            transform = store.getComponent(targetRef, TransformComponent.getComponentType());
            if (transform != null) {
                InjuryTextHelper.sendLimbDestroyedText(targetRef, hitPart, transform.getPosition(), store, commandBuffer);
            }


//            if (playerRef != null) {
//                playerRef.sendMessage(Message.raw(
//                        String.format("%s DESTROYED!", hitPart.getDisplayName().toUpperCase())
//                ));
//            }
        }

        // === WEAPON/CREATURE TYPE MODIFIERS ===
        int fractureModifier = 0;
        int bleedModifier = 0;

        if (source instanceof Damage.EntitySource entitySource) {
            Ref<EntityStore> attackerRef = entitySource.getRef();

            if (attackerRef.isValid()) {
                Entity attackerEntity = EntityUtils.getEntity(attackerRef, store);

                if (attackerEntity instanceof LivingEntity livingAttacker) {
                    Inventory inventory = livingAttacker.getInventory();

                    if (inventory != null) {
                        ItemStack itemInHand = inventory.getItemInHand();

                        // Check if holding a weapon
                        if (itemInHand != null && !itemInHand.isEmpty()) {
                            Item item = itemInHand.getItem();

                            if (item != null) {
                                String itemId = item.getId().toLowerCase();
//                                LOGGER.atInfo().log("========================================");
//                                LOGGER.atInfo().log("[WEAPON DETECTED] " + itemId);

                                // Slashing weapons
                                if (itemId.contains("sword") || itemId.contains("axe") ||
                                        itemId.contains("dagger") || itemId.contains("blade") ||
                                        itemId.contains("scythe") || itemId.contains("bow") ||
                                        itemId.contains("spear")) {

                                    bleedModifier = -MortalWoundsConfig.get().slashmodifier; // Negative = easier to bleed
                                    fractureModifier = MortalWoundsConfig.get().bludgeonmodifier; // Positive = harder to fracture
//                                    LOGGER.atInfo().log("[SLASHING WEAPON] Bleed modifier: " + bleedModifier +
//                                            ", Fracture modifier: +" + fractureModifier);

                                    // Bludgeoning weapons
                                } else if (itemId.contains("mace") || itemId.contains("hammer") ||
                                        itemId.contains("club") || itemId.contains("maul") ||
                                        itemId.contains("staff")) {

                                    fractureModifier = -MortalWoundsConfig.get().bludgeonmodifier; // Negative = easier to fracture
                                    bleedModifier = MortalWoundsConfig.get().slashmodifier; // Positive = harder to bleed
//                                    LOGGER.atInfo().log("[BLUDGEONING WEAPON] Fracture modifier: " + fractureModifier +
//                                            ", Bleed modifier: +" + bleedModifier);
                                }
                            }
                        } else {
                            // No weapon = check creature type
//                            LOGGER.atInfo().log("========================================");
//                            LOGGER.atInfo().log("[NO WEAPON] Checking creature type...");

                            String entityName = null;

                            if (attackerEntity instanceof NPCEntity npc) {
                                entityName = npc.getNPCTypeId().toLowerCase();
                                //LOGGER.atInfo().log("[NPC TYPE] " + entityName);
                            } else {
                                DisplayNameComponent displayName = store.getComponent(attackerRef, DisplayNameComponent.getComponentType());
                                if (displayName != null && displayName.getDisplayName() != null) {
                                    entityName = displayName.getDisplayName().toString().toLowerCase();
                                    // LOGGER.atInfo().log("[DISPLAY NAME] " + entityName);
                                }
                            }

                            if (entityName == null || entityName.isEmpty()) {
                                entityName = attackerEntity.getClass().getSimpleName().toLowerCase();
                                // LOGGER.atInfo().log("[CLASS NAME FALLBACK] " + entityName);
                            }

                            if (entityName != null) {
                                // Slashing creatures
                                if (entityName.contains("bear") || entityName.contains("wolf") ||
                                        entityName.contains("tiger") || entityName.contains("cact") ||
                                        entityName.contains("raptor") || entityName.contains("goblin") ||
                                        entityName.contains("hyena") || entityName.contains("leopard") ||
                                        entityName.contains("scarak") || entityName.contains("scorpion") ||
                                        entityName.contains("zombie")) {

                                    bleedModifier = -MortalWoundsConfig.get().slashmodifier;
                                    fractureModifier = MortalWoundsConfig.get().bludgeonmodifier;
//                                    LOGGER.atInfo().log("[SLASHING CREATURE] Bleed modifier: " + bleedModifier +
//                                            ", Fracture modifier: +" + fractureModifier);

                                    // Bludgeoning creatures
                                } else if (entityName.contains("golem") || entityName.contains("ogre") ||
                                        entityName.contains("skeleton") || entityName.contains("rock") ||
                                        entityName.contains("yeti") || entityName.contains("trok")) {

                                    fractureModifier = -MortalWoundsConfig.get().bludgeonmodifier;
                                    bleedModifier = MortalWoundsConfig.get().slashmodifier;
//                                    LOGGER.atInfo().log("[BLUDGEONING CREATURE] Fracture modifier: " + fractureModifier +
//                                            ", Bleed modifier: +" + bleedModifier);
                                } else {
                                    LOGGER.atInfo().log("[UNKNOWN CREATURE] No modifiers applied");
                                }
                            }
                        }
                    }
                }
            }
        }

        // === FRACTURE CHECK ===
        if ((isFallDamage || source instanceof Damage.EntitySource) &&
                !bodyPartComponent.hasBodyPartEffect(hitPart, "FRACTURE")) {

            int baseFractureRoll = ThreadLocalRandom.current().nextInt(1, 101);
            int adjustedFractureRoll = Math.max(1, baseFractureRoll + fractureModifier);

            int fractureThreshold = MortalWoundsConfig.get().bodyPartFractureChance;

            // Heavy damage bonus
            float limbMaxHealth = bodyPartComponent.getBodyPartMaxHealth(hitPart);
            float damagePercent = limbMaxHealth > 0 ? (damageAmount / limbMaxHealth) : 0;
            boolean isHeavyDamage = damagePercent >= MortalWoundsConfig.get().heavyDamageThreshold;

            if (isHeavyDamage) {
                fractureThreshold += MortalWoundsConfig.get().heavyDamageFractureBonus;
            }

            if (isFallDamage && damageAmount > 5f) {
                fractureThreshold = (int) (fractureThreshold * 1.5f);
                fractureThreshold = Math.min(fractureThreshold, 100);
            }

            if (adjustedFractureRoll <= fractureThreshold) {
                bodyPartComponent.addBodyPartEffect(hitPart, "FRACTURE");
                playSoundEffect(targetRef, store, "SFX_Bone_Break", 1.0f, 1.0f);

                TransformComponent transform = store.getComponent(targetRef, TransformComponent.getComponentType());
                if (transform != null) {
                    InjuryTextHelper.sendFractureText(targetRef, hitPart, transform.getPosition(), store, commandBuffer);
                }
            }
        }

        // === BLEED CHECK ===
        if (!isEnvironmental && !isFallDamage) {
            int baseBleedRoll = ThreadLocalRandom.current().nextInt(1, 101);
            int adjustedBleedRoll = Math.max(1, baseBleedRoll + bleedModifier);

            int bleedThreshold = MortalWoundsConfig.get().bodyPartBleedChance;

            // Heavy damage bonus
            float limbMaxHealth = bodyPartComponent.getBodyPartMaxHealth(hitPart);
            float damagePercent = limbMaxHealth > 0 ? (damageAmount / limbMaxHealth) : 0;
            boolean isHeavyDamage = damagePercent >= MortalWoundsConfig.get().heavyDamageThreshold;

            if (isHeavyDamage) {
                bleedThreshold += MortalWoundsConfig.get().heavyDamageBleedBonus;
            }

            boolean hasBleed = bodyPartComponent.hasBodyPartEffect(hitPart, "BLEED");
            boolean hasHeavyBleed = bodyPartComponent.hasBodyPartEffect(hitPart, "HEAVY_BLEED");

            if (adjustedBleedRoll <= bleedThreshold) {
                TransformComponent transform = store.getComponent(targetRef, TransformComponent.getComponentType());

                if (hasHeavyBleed) {
                    // Already heavy bleeding
                } else if (hasBleed) {
                    // Upgrade to heavy bleed
                    bodyPartComponent.removeBodyPartEffect(hitPart, "BLEED");
                    bodyPartComponent.addBodyPartEffect(hitPart, "HEAVY_BLEED");
                    playSoundEffect(targetRef, store, "SFX_Bone_Break", 1.2f, 5.0f);

                    if (transform != null) {
                        InjuryTextHelper.sendBleedText(targetRef, hitPart, true, transform.getPosition(), store, commandBuffer);
                    }
                } else {
                    // Check for heavy bleed first
                    int heavyBleedRoll = ThreadLocalRandom.current().nextInt(1, 101);
                    int heavyBleedThreshold = MortalWoundsConfig.get().heavyBleedChance;

                    // Add heavy damage bonus to heavy bleed chance too
                    if (isHeavyDamage) {
                        heavyBleedThreshold += MortalWoundsConfig.get().heavyDamageBleedBonus;
                    }

                    if (heavyBleedRoll <= heavyBleedThreshold) {
                        // Direct heavy bleed
                        bodyPartComponent.addBodyPartEffect(hitPart, "HEAVY_BLEED");
                        playSoundEffect(targetRef, store, "SFX_Bone_Break", 2.0f, 5.0f);

                        if (transform != null) {
                            InjuryTextHelper.sendBleedText(targetRef, hitPart, true, transform.getPosition(), store, commandBuffer);
                        }
                    } else {
                        // Regular bleed
                        bodyPartComponent.addBodyPartEffect(hitPart, "BLEED");
                        playSoundEffect(targetRef, store, "SFX_Bone_Break", 0.8f, 3.0f);

                        if (transform != null) {
                            InjuryTextHelper.sendBleedText(targetRef, hitPart, false, transform.getPosition(), store, commandBuffer);
                        }
                    }
                }
            }
        }

    }

    private float applyDamageWithOverflow(BodyPartComponent bodyPartComp, BodyPart hitPart,
                                          float damage, PlayerRef playerRef) {
        float currentHealth = bodyPartComp.getBodyPartHealth(hitPart);

        if (currentHealth >= damage) {
            bodyPartComp.damageBodyPart(hitPart, damage);
            return 0f;
        }

        float overflow = damage - currentHealth;
        bodyPartComp.setBodyPartHealth(hitPart, 0f);

//        LOGGER.atInfo().log(String.format("[OVERFLOW] %s destroyed with %.1f overflow damage",
//                hitPart.getDisplayName(), overflow));

        overflow = distributeOverflowDamage(bodyPartComp, hitPart, overflow, playerRef);
        return overflow;
    }

    private float distributeOverflowDamage(BodyPartComponent bodyPartComp, BodyPart hitPart,
                                           float overflow, PlayerRef playerRef) {
        List<BodyPart> targetPriority = getOverflowPriority(hitPart);

        for (BodyPart target : targetPriority) {
            if (overflow <= 0) break;

            float targetHealth = bodyPartComp.getBodyPartHealth(target);

            if (targetHealth > 0) {
                if (targetHealth >= overflow) {
                    bodyPartComp.damageBodyPart(target, overflow);
//                    LOGGER.atInfo().log(String.format("[OVERFLOW] %.1f damage to %s",
//                            overflow, target.getDisplayName()));
                    overflow = 0;
                } else {
                    bodyPartComp.setBodyPartHealth(target, 0f);
                    overflow -= targetHealth;

//                    LOGGER.atInfo().log(String.format("[OVERFLOW] %s destroyed, %.1f remaining",
//                            target.getDisplayName(), overflow));

                    if (!bodyPartComp.isBodyPartBroken(target)) {
                        bodyPartComp.setBodyPartBroken(target, true);
//                        if (playerRef != null) {
//                            playerRef.sendMessage(Message.raw(
//                                    String.format("%s DESTROYED (overflow)!", target.getDisplayName().toUpperCase())
//                            ));
//                        }
                    }
                }
            }
        }

        return overflow;
    }

    private List<BodyPart> getOverflowPriority(BodyPart hitPart) {
        List<BodyPart> priority = new ArrayList<>();

        switch (hitPart) {
            case HEAD -> {
                priority.add(BodyPart.TORSO);
                addRandomParts(priority, BodyPart.HEAD, BodyPart.TORSO);
            }
            case LEFTARM -> {
                priority.add(BodyPart.TORSO);
                priority.add(BodyPart.RIGHTARM);
                addRandomParts(priority, BodyPart.LEFTARM, BodyPart.TORSO, BodyPart.RIGHTARM);
            }
            case RIGHTARM -> {
                priority.add(BodyPart.TORSO);
                priority.add(BodyPart.LEFTARM);
                addRandomParts(priority, BodyPart.RIGHTARM, BodyPart.TORSO, BodyPart.LEFTARM);
            }
            case LEFTLEG -> {
                priority.add(BodyPart.RIGHTLEG);
                priority.add(BodyPart.TORSO);
                addRandomParts(priority, BodyPart.LEFTLEG, BodyPart.RIGHTLEG, BodyPart.TORSO);
            }
            case RIGHTLEG -> {
                priority.add(BodyPart.LEFTLEG);
                priority.add(BodyPart.TORSO);
                addRandomParts(priority, BodyPart.RIGHTLEG, BodyPart.LEFTLEG, BodyPart.TORSO);
            }
            case TORSO -> {
                addRandomParts(priority, BodyPart.TORSO);
            }
        }

        return priority;
    }

    private void addRandomParts(List<BodyPart> priority, BodyPart... exclude) {
        List<BodyPart> remaining = new ArrayList<>();

        for (BodyPart part : BodyPart.values()) {
            boolean isExcluded = false;
            for (BodyPart ex : exclude) {
                if (part == ex) {
                    isExcluded = true;
                    break;
                }
            }
            if (!isExcluded) {
                remaining.add(part);
            }
        }

        while (!remaining.isEmpty()) {
            int index = ThreadLocalRandom.current().nextInt(remaining.size());
            priority.add(remaining.remove(index));
        }
    }

    /**
     * Play a sound effect for the player
     */
    private void playSoundEffect(Ref<EntityStore> targetRef, Store<EntityStore> store,
                                 String soundEventId, float volume, float pitch) {
        try {
            int soundIndex = SoundEvent.getAssetMap().getIndex(soundEventId);
            if (soundIndex > 0) {
                SoundUtil.playSoundEvent2d(targetRef, soundIndex, SoundCategory.SFX, volume, pitch, store);
            }
        } catch (Exception e) {
            // Sound not found or error playing, ignore silently
        }
    }
}