package com.rajoki.injuryplugin.systems.npc;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.rajoki.injuryplugin.components.npc.NPCBodyPartComponent;
import com.rajoki.injuryplugin.config.MortalWoundsConfig;
import com.rajoki.injuryplugin.systems.CreatureType;
import com.rajoki.injuryplugin.systems.bodypartsystems.BodyPart;
import com.rajoki.injuryplugin.systems.bodypartsystems.DirectionalDamageCalculator;
import com.rajoki.injuryplugin.utils.InjuryTextHelper;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Applies body part damage and effects to NPCs with limb health tracking
 */
public class NPCBodyPartDamageSystem extends EntityEventSystem<EntityStore, Damage> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final ComponentType<EntityStore, NPCBodyPartComponent> npcBodyPartType;

    public NPCBodyPartDamageSystem(ComponentType<EntityStore, NPCBodyPartComponent> npcBodyPartType) {
        super(Damage.class);
        this.npcBodyPartType = npcBodyPartType;
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getInspectDamageGroup();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return this.npcBodyPartType;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {

        if (damage.getAmount() <= 0) {
            return;
        }

        Ref<EntityStore> targetRef = chunk.getReferenceTo(index);

        if (!targetRef.isValid()) {
            return;
        }

// STOP processing if NPC already dead
        if (chunk.getArchetype().contains(com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent.getComponentType())) {
            return;
        }

        EntityStatMap stats = store.getComponent(targetRef, EntityStatMap.getComponentType());
        if (stats != null) {
            int healthIdx = DefaultEntityStatTypes.getHealth();
            EntityStatValue healthValue = stats.get(healthIdx);

            if (healthValue != null && healthValue.get() <= 0) {
                return;
            }
        }

        NPCBodyPartComponent npcBodyPart = chunk.getComponent(index, this.npcBodyPartType);

        if (npcBodyPart == null || !npcBodyPart.isInitialized()) {
            return;
        }

        Damage.Source source = damage.getSource();

        // Only apply effects if damage came from a player/entity
        if (!(source instanceof Damage.EntitySource entitySource)) {
            return;
        }

        // Determine which body part was hit
        TransformComponent npcTransform = store.getComponent(targetRef, TransformComponent.getComponentType());
        if (npcTransform == null) {
            return;
        }

        Ref<EntityStore> attackerRef = entitySource.getRef();
        TransformComponent attackerTransform = store.getComponent(attackerRef, TransformComponent.getComponentType());

        BodyPart hitPart;
        if (attackerTransform != null) {
            Vector3d attackerPos = attackerTransform.getPosition();
            // Get attacker's movement component
            MovementStatesComponent attackerMovement = store.getComponent(attackerRef, MovementStatesComponent.getComponentType());
            // Pass Anatomy to calculator
            hitPart = DirectionalDamageCalculator.getTargetBodyPart(
                    npcTransform,
                    attackerPos,
                    npcBodyPart.getAnatomy(),
                    attackerMovement
            );
        } else {
            hitPart = BodyPart.TORSO;
        }

        // Get NPC name for logging
        NPCEntity npc = chunk.getComponent(index, NPCEntity.getComponentType());
        String npcName = (npc != null) ? npc.getRoleName() : "NPC";

        PlayerRef attackerPlayer = store.getComponent(attackerRef, PlayerRef.getComponentType());

        // === APPLY DAMAGE TO LIMB ===
        float damageAmount = damage.getAmount();
        float healthBefore = npcBodyPart.getBodyPartHealth(hitPart);
        boolean limbJustDestroyed = npcBodyPart.damageBodyPart(hitPart, damageAmount);
        float healthAfter = npcBodyPart.getBodyPartHealth(hitPart);

        //  Check if NPC died from THIS damage before applying effects
        if (stats != null) {
            int healthIdx = DefaultEntityStatTypes.getHealth();
            EntityStatValue healthValue = stats.get(healthIdx);

            if (healthValue != null && healthValue.get() <= 0) {
                //LOGGER.atInfo().log("[NPC DAMAGE] " + npcName + " died from this hit - skipping injury effects");
                return; // NPC just died - don't apply fractures/bleeds/loot
            }
        }

//        if (npcBodyPart.isBodyPartDestroyed(hitPart)) {
//            return;
//        }
//
//        if (npcBodyPart.isBodyPartDestroyed(BodyPart.TORSO) ||
//                npcBodyPart.isBodyPartDestroyed(BodyPart.HEAD)) {
//            return;
//        }

        // DETAILED LOGGING
//        LOGGER.atInfo().log("========================================");
//        LOGGER.atInfo().log(String.format("[NPC DAMAGE] %s - %s", npcName, hitPart.getDisplayName()));
//        LOGGER.atInfo().log(String.format("  Damage: %.1f", damageAmount));
//        LOGGER.atInfo().log(String.format("  Health: %.1f → %.1f / %.1f",
//                healthBefore, healthAfter, npcBodyPart.getBodyPartMaxHealth(hitPart)));
//
//        if (limbJustDestroyed) {
//            LOGGER.atInfo().log("  >>> LIMB DESTROYED! <<<");
//        }

        // === LIMB DESTRUCTION = GUARANTEED FRACTURE ===
        if (limbJustDestroyed) {
            handleLimbDestruction(targetRef, store, commandBuffer, npcBodyPart, hitPart, attackerRef, npcName);
        }
        // === NORMAL RANDOM EFFECTS (only if limb not destroyed) ===
        else if (!npcBodyPart.isBodyPartDestroyed(hitPart)) {
            applyRandomEffects(targetRef, store, commandBuffer, npcBodyPart, hitPart, attackerRef, attackerPlayer, damageAmount);
        }

        //LOGGER.atInfo().log("========================================");
    }

    /**
     * Handle limb destruction / can override resistances but not immunities
     */
    private void handleLimbDestruction(Ref<EntityStore> targetRef,
                                       Store<EntityStore> store,
                                       CommandBuffer<EntityStore> commandBuffer,
                                       NPCBodyPartComponent npcBodyPart,
                                       BodyPart hitPart,
                                       Ref<EntityStore> attackerRef,
                                       String npcName) {

        CreatureType creatureType = npcBodyPart.getCreatureType();
        TransformComponent transform = store.getComponent(targetRef, TransformComponent.getComponentType());

        // Get NPC type for loot
        NPCEntity npc = store.getComponent(targetRef, NPCEntity.getComponentType());
        String npcTypeId = (npc != null) ? npc.getNPCTypeId() : null;

        // === ALWAYS PLAY SOUND + TEXT WHEN LIMB DESTROYED ===
        playSoundEffect(attackerRef, store, "SFX_Bone_Break", 3.0f, 0.8f);
        // Display limb destroyed text
        if (transform != null) {
            InjuryTextHelper.sendLimbDestroyedText(targetRef, hitPart, transform.getPosition(), store, commandBuffer);
        }

        // Always try to fracture the destroyed limb (unless IMMUNE)
        if (!npcBodyPart.hasEffect(hitPart, "FRACTURE")) {
            if (creatureType.isImmuneTo("FRACTURE")) {
//                LOGGER.atInfo().log(String.format(
//                        "[LIMB DESTROYED] %s's %s destroyed but CANNOT fracture (immune)",
//                        npcName, hitPart.getDisplayName()
//                ));
            } else {
                // Even resistant creatures can fracture when limb is destroyed
                npcBodyPart.addEffect(hitPart, "FRACTURE");
                // playSoundEffect(attackerRef, store, "SFX_Bone_Break", 3.0f, 0.8f);



//                LOGGER.atInfo().log(String.format(
//                        "[LIMB DESTROYED] %s's %s FRACTURED!%s",
//                        npcName, hitPart.getDisplayName(),
//                        creatureType.isResistantTo("FRACTURE") ? " (overcame resistance)" : ""
//                ));
            }
        }

        // === DROP LOOT FOR BROKEN LIMB ===
        if (transform != null && NPCLimbLootTable.hasLimbLoot(npcTypeId)) {
            dropLimbLoot(targetRef, store, commandBuffer, npcTypeId, hitPart, transform.getPosition());
        }

        // Notify attacking player
        PlayerRef attackerPlayer = store.getComponent(attackerRef, PlayerRef.getComponentType());
//        if (attackerPlayer != null) {
//            attackerPlayer.sendMessage(Message.raw(String.format(
//                    "§c§l✖ Enemy %s DESTROYED!",
//                    hitPart.getDisplayName().toUpperCase()
//            )));
//        }
    }

    /**
     * Apply random effects (only called if limb not destroyed)
     */
    private void applyRandomEffects(Ref<EntityStore> targetRef,
                                    Store<EntityStore> store,
                                    CommandBuffer<EntityStore> commandBuffer,
                                    NPCBodyPartComponent npcBodyPart,
                                    BodyPart hitPart,
                                    Ref<EntityStore> attackerRef,
                                    PlayerRef attackerPlayer, float damageAmount) {

        CreatureType creatureType = npcBodyPart.getCreatureType();
        TransformComponent transform = store.getComponent(targetRef, TransformComponent.getComponentType());

        // Get weapon/creature modifiers
        int[] modifiers = getWeaponModifiers(attackerRef, store);
        int fractureModifier = modifiers[0];
        int bleedModifier = modifiers[1];

        // Calculate heavy damage bonus
        float limbMaxHealth = npcBodyPart.getBodyPartMaxHealth(hitPart);
        float damagePercent = limbMaxHealth > 0 ? (damageAmount / limbMaxHealth) : 0;
        boolean isHeavyDamage = damagePercent >= MortalWoundsConfig.get().heavyDamageThreshold;

        int heavyDamageFractureBonus = isHeavyDamage ? MortalWoundsConfig.get().heavyDamageFractureBonus : 0;
        int heavyDamageBleedBonus = isHeavyDamage ? MortalWoundsConfig.get().heavyDamageBleedBonus : 0;

        // === FRACTURE CHECK ===
        // Check immunity first
        if (creatureType.isImmuneTo("FRACTURE")) {
//            LOGGER.atInfo().log(String.format(
//                    "  [FRACTURE IMMUNE] %s cannot be fractured",
//                    creatureType.name()
//            ));

//            if (attackerPlayer != null) {
//                attackerPlayer.sendMessage(Message.raw(
//                        "§7Cannot fracture this creature!"
//                ));
//            }
        }
        // Check resistance (can only fracture through part destruction, not random)
        else if (creatureType.isResistantTo("FRACTURE")) {
//            LOGGER.atInfo().log(String.format(
//                    "  [FRACTURE RESISTANT] %s - can only fracture via part destruction",
//                    creatureType.name()
//            ));
        }
        // Normal random fracture check
        else if (!npcBodyPart.hasEffect(hitPart, "FRACTURE")) {
            int baseFractureRoll = ThreadLocalRandom.current().nextInt(1, 101);

            // Apply Weapon modifier
            int adjustedFractureRoll = Math.max(1, baseFractureRoll + fractureModifier);

            // Apply creature-specific threshold multiplier
            int baseFractureThreshold = MortalWoundsConfig.get().npcBodyPartFractureChance;
            int adjustedThreshold = npcBodyPart.getAdjustedFractureThreshold(baseFractureThreshold);

            adjustedThreshold += heavyDamageFractureBonus;

//            LOGGER.atInfo().log(String.format(
//                    "  [FRACTURE CHECK] Roll: %d vs Threshold: %d (base: %d, multiplier: %.1fx)",
//                    adjustedFractureRoll, adjustedThreshold, baseFractureThreshold,
//                    creatureType.getFractureThresholdMultiplier()
//            ));

            if (adjustedFractureRoll <= adjustedThreshold) {
                npcBodyPart.addEffect(hitPart, "FRACTURE");
                playSoundEffect(attackerRef, store, "SFX_Bone_Break", 1.8f, 0.7f);

                // Display floating text
                if (transform != null) {
                    InjuryTextHelper.sendFractureText(targetRef, hitPart, transform.getPosition(), store, commandBuffer);
                }

//                LOGGER.atInfo().log(String.format(
//                        "  >>> [FRACTURE] %s FRACTURED! <<<",
//                        hitPart.getDisplayName()
//                ));
            }
        }

        // === BLEED CHECK ===
        // Check immunity first
        if (creatureType.isImmuneTo("BLEED")) {
//            LOGGER.atInfo().log(String.format(
//                    "  [BLEED IMMUNE] %s cannot bleed",
//                    creatureType.name()
//            ));

//            if (attackerPlayer != null) {
//                attackerPlayer.sendMessage(Message.raw(
//                        "§7This creature cannot bleed!"
//                ));
//            }
        }
        // Check resistance (harder to bleed, but not impossible)
        else if (!npcBodyPart.hasEffect(hitPart, "BLEED") &&
                !npcBodyPart.hasEffect(hitPart, "HEAVY_BLEED")) {

            int baseBleedRoll = ThreadLocalRandom.current().nextInt(1, 101);

            // Apply weapon modifier
            int adjustedBleedRoll = Math.max(1, baseBleedRoll + bleedModifier);

            // Apply resistance if applicable
            int baseBleedThreshold = MortalWoundsConfig.get().npcBodyPartBleedChance;
            int adjustedThreshold = baseBleedThreshold;

            if (creatureType.isResistantTo("BLEED")) {
                // Resistant creatures have reduced chance (divide threshold by multiplier)
                adjustedThreshold = (int) (baseBleedThreshold / creatureType.getFractureThresholdMultiplier());

//                LOGGER.atInfo().log(String.format(
//                        "  [BLEED RESISTANT] Threshold reduced: %d → %d",
//                        baseBleedThreshold, adjustedThreshold
//                ));
            }

            adjustedThreshold += heavyDamageBleedBonus;

//            LOGGER.atInfo().log(String.format(
//                    "  [BLEED CHECK] Roll: %d vs Threshold: %d",
//                    adjustedBleedRoll, adjustedThreshold
//            ));

            if (adjustedBleedRoll <= adjustedThreshold) {
                int heavyBleedRoll = ThreadLocalRandom.current().nextInt(1, 101);
                int heavyBleedThreshold = MortalWoundsConfig.get().npcHeavyBleedChance + heavyDamageBleedBonus;

                if (heavyBleedRoll <= heavyBleedThreshold) {
                    //direct heavy bleed
                    npcBodyPart.addEffect(hitPart, "HEAVY_BLEED");
                    playSoundEffect(attackerRef, store, "SFX_Bone_Break", 2.0f, 1.5f);

                    if (transform != null) {
                        InjuryTextHelper.sendBleedText(targetRef, hitPart, true, transform.getPosition(), store,
                                commandBuffer);
                    }

//                    LOGGER.atInfo().log(String.format(
//                            " >>> [HEAVY BLEED] %s HEAVY BLEEDING! <<<",
//                            hitPart.getDisplayName()
//                    ));
                } else {
                    // Regular bleed
                    npcBodyPart.addEffect(hitPart, "BLEED");
                    playSoundEffect(attackerRef, store, "SFX_Bone_Break", 1.5f, 1.5f);

                    // Display floating text
                    if (transform != null) {
                        InjuryTextHelper.sendBleedText(targetRef, hitPart, false, transform.getPosition(), store, commandBuffer);
                    }

//                    LOGGER.atInfo().log(String.format(
//                            "  >>> [BLEED] %s BLEEDING! <<<",
//                            hitPart.getDisplayName()
//                    ));
                }
            }
        }

    }

    private void playSoundEffect(Ref<EntityStore> targetRef, Store<EntityStore> store,
                                 String soundEventId, float volume, float pitch) {
        try {
            int soundIndex = SoundEvent.getAssetMap().getIndex(soundEventId);
            if (soundIndex > 0) {
                SoundUtil.playSoundEvent2d(targetRef, soundIndex, SoundCategory.SFX, volume, pitch, store);
            }
        } catch (Exception e) {
            // Sound not found - ignore
        }
    }

    /**
     * Drop loot items for a broken limb
     */
    private void dropLimbLoot(Ref<EntityStore> targetRef,
                              Store<EntityStore> store,
                              CommandBuffer<EntityStore> commandBuffer,
                              String npcTypeId,
                              BodyPart bodyPart,
                              Vector3d position) {

        List<NPCLimbLootTable.LootDrop> possibleLoot = NPCLimbLootTable.getLootForLimb(npcTypeId, bodyPart);

        for (NPCLimbLootTable.LootDrop lootDrop : possibleLoot) {
            // Roll for drop chance
            float roll = ThreadLocalRandom.current().nextFloat();
            if (roll > lootDrop.dropChance) {
                continue; // Didn't pass drop chance
            }

            // Determine amount
            int amount = ThreadLocalRandom.current().nextInt(
                    lootDrop.minAmount,
                    lootDrop.maxAmount + 1
            );

            try {
                // Create item stack using item ID string
                ItemStack itemStack = new ItemStack(lootDrop.itemId, amount);

                // Spawn item drop at position with slight random offset
                Vector3d dropPos = new Vector3d(
                        position.getX() + (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.5,
                        position.getY() + 0.5, // Slightly above ground
                        position.getZ() + (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.5
                );

                // Use the same method as the arrow drop mod
                dropItemAtPosition(targetRef, itemStack, dropPos, commandBuffer);

//                LOGGER.atInfo().log(String.format(
//                        "[LIMB LOOT] Dropped %dx %s from %s",
//                        amount, lootDrop.itemId, bodyPart.getDisplayName()
//                ));

            } catch (Exception e) {
                LOGGER.atWarning().log("[LIMB LOOT] Failed to spawn item " + lootDrop.itemId + ": " + e.getMessage());
            }
        }
    }

    /**
     * Drop an item at a position near npc
     */
    private void dropItemAtPosition(Ref<EntityStore> sourceRef,
                                    ItemStack itemStack,
                                    Vector3d position,
                                    CommandBuffer<EntityStore> commandBuffer) {

        if (itemStack.isEmpty() || !itemStack.isValid()) {
            return;
        }

        // Use ItemComponent to generate the item drop
        Holder<EntityStore> itemEntityHolder = ItemComponent.generateItemDrop(
                commandBuffer,
                itemStack,
                position,
                Vector3f.ZERO,  // No initial velocity
                0.0f,           // No upward throw
                0.0f,           // No horizontal throw
                0.0f            // No rotation
        );

        if (itemEntityHolder == null) {
            return;
        }

        // Set pickup delay so item doesn't immediately get picked up
        ItemComponent itemComponent = itemEntityHolder.getComponent(ItemComponent.getComponentType());
        if (itemComponent != null) {
            itemComponent.setPickupDelay(1.0f); // 1 second delay before can be picked up
        }

        // Add the item entity to the world
        commandBuffer.addEntity(itemEntityHolder, AddReason.SPAWN);
    }

    /**
     * Determine weapon/creature type modifiers for injury chances
     * Returns [fractureModifier, bleedModifier]
     */
    private int[] getWeaponModifiers(Ref<EntityStore> attackerRef, Store<EntityStore> store) {
        int fractureModifier = 0;
        int bleedModifier = 0;

        if (!attackerRef.isValid()) {
            return new int[]{fractureModifier, bleedModifier};
        }

        Entity attackerEntity = EntityUtils.getEntity(attackerRef, store);

        if (!(attackerEntity instanceof LivingEntity livingAttacker)) {
            return new int[]{fractureModifier, bleedModifier};
        }

        Inventory inventory = livingAttacker.getInventory();

        if (inventory != null) {
            ItemStack itemInHand = inventory.getItemInHand();

            // Check if holding a weapon
            if (itemInHand != null && !itemInHand.isEmpty()) {
                Item item = itemInHand.getItem();

                if (item != null) {
                    String itemId = item.getId().toLowerCase();

                    // Slashing weapons (easier to bleed, harder to fracture)
                    if (itemId.contains("sword") || itemId.contains("axe") ||
                            itemId.contains("dagger") || itemId.contains("blade") ||
                            itemId.contains("scythe")) {

                        bleedModifier = -MortalWoundsConfig.get().npcSlashModifier;
                        fractureModifier = MortalWoundsConfig.get().npcBludgeonModifier;

//                        LOGGER.atInfo().log("[NPC - SLASHING WEAPON] Bleed modifier: " + bleedModifier +
//                                ", Fracture modifier: +" + fractureModifier);
                    }
                    // Piercing weapons (easier to bleed, normal fracture)
                    else if (itemId.contains("bow") || itemId.contains("spear") ||
                            itemId.contains("arrow")) {

                        bleedModifier = -MortalWoundsConfig.get().npcSlashModifier;
                        fractureModifier = MortalWoundsConfig.get().npcBludgeonModifier;

//                        LOGGER.atInfo().log("[NPC - PIERCING WEAPON] Bleed modifier: " + bleedModifier +
//                                ", Fracture modifier: +" + fractureModifier);
                    }
                    // Bludgeoning weapons (easier to fracture, harder to bleed)
                    else if (itemId.contains("mace") || itemId.contains("hammer") ||
                            itemId.contains("club") || itemId.contains("maul") ||
                            itemId.contains("staff")) {

                        fractureModifier = -MortalWoundsConfig.get().npcBludgeonModifier;
                        bleedModifier = MortalWoundsConfig.get().npcSlashModifier;

//                        LOGGER.atInfo().log("[NPC - BLUDGEONING WEAPON] Fracture modifier: " + fractureModifier +
//                                ", Bleed modifier: +" + bleedModifier);
                    }

                    return new int[]{fractureModifier, bleedModifier};
                }
            }
        }

        // No weapon, check creature type
        String entityName = null;

        if (attackerEntity instanceof NPCEntity npc) {
            entityName = npc.getNPCTypeId().toLowerCase();
        } else {
            DisplayNameComponent displayName = store.getComponent(attackerRef, DisplayNameComponent.getComponentType());
            if (displayName != null && displayName.getDisplayName() != null) {
                entityName = displayName.getDisplayName().toString().toLowerCase();
            }
        }

        if (entityName == null || entityName.isEmpty()) {
            entityName = attackerEntity.getClass().getSimpleName().toLowerCase();
        }

        if (entityName != null) {
            // Slashing creatures (claws, teeth)
            if (entityName.contains("bear") || entityName.contains("wolf") ||
                    entityName.contains("tiger") || entityName.contains("cact") ||
                    entityName.contains("raptor") || entityName.contains("goblin") ||
                    entityName.contains("hyena") || entityName.contains("leopard") ||
                    entityName.contains("scarak") || entityName.contains("scorpion") ||
                    entityName.contains("zombie")) {

                bleedModifier = -MortalWoundsConfig.get().npcSlashModifier;
                fractureModifier = MortalWoundsConfig.get().npcBludgeonModifier;

//                LOGGER.atInfo().log("[NPC - SLASHING CREATURE] Bleed modifier: " + bleedModifier +
//                        ", Fracture modifier: +" + fractureModifier);
            }
            // Bludgeoning creatures (stone, bone)
            else if (entityName.contains("golem") || entityName.contains("ogre") ||
                    entityName.contains("skeleton") || entityName.contains("rock") ||
                    entityName.contains("yeti") || entityName.contains("trok")) {

                fractureModifier = -MortalWoundsConfig.get().npcBludgeonModifier;
                bleedModifier = MortalWoundsConfig.get().npcSlashModifier;

//                LOGGER.atInfo().log("[NPC - BLUDGEONING CREATURE] Fracture modifier: " + fractureModifier +
//                        ", Bleed modifier: +" + bleedModifier);
            }
        }

        return new int[]{fractureModifier, bleedModifier};
    }
}