package com.rajoki.injuryplugin.config;

import java.util.*;

public class MortalWoundsConfig {
    private static MortalWoundsConfig instance;

    // Fracture and Bleed chances
    public int bodyPartFractureChance = 10; // Base fracture chance on players
    public int bodyPartBleedChance = 10; // Base bleed chance on players
    public int heavyBleedChance = 10; // Base heavy bleed chance on players

    // Weapon/Creature modifiers
    public int slashmodifier = 15; // Slashing modifier on players, slashing weapons/types increase bleed chance and decrease fracture chance
    public int bludgeonmodifier = 15; // Bludgeon modifier on players, bludgeon weapons/types increase fracture chance and decrease bleed chance

    // NPC-specific weapon/creature modifiers
    public int npcSlashModifier = 15;      // NPCs are more affected by slashing
    public int npcBludgeonModifier = 15;   // NPCs are more affected by bludgeoning

    // Regular Bleed settings
    public float bleedDamageAmount = 5.0f; // Amount bleed deals
    public float bleedDamageInterval = 10.0f; // When and how often bleeds deal damage
    public int bleedDurationSeconds = 120; // How long bleed lasts for without being treated

    // Heavy Bleed settings
    public float heavyBleedDamageAmount = 10.0f; // Default: 2x regular bleed
                                                    // Amount heavy bleed deals
    public float heavyBleedDamageInterval = 10.0f; // How often heavy bleeds deal damage
    public int heavyBleedDurationSeconds = 180; // How long bleeds last without being treated

    // Arm Fracture damage reduction
    public float armFractureDamageReduction1Arm = 0.25f; // % reduction with 1 arm fractured
    public float armFractureDamageReduction2Arms = 0.50f; // % reduction with 2 arms fractured

    // Leg Fracture speed reduction
    public float legFractureSpeedReduction1Leg = 0.25f; // % speed reduction with 1 leg fractured
    public float legFractureSpeedReduction2Legs = 0.50f; // % speed reduction with 2 legs fractured

    // Fracture duration
    public boolean enableFractureDuration = false; // If true, fractures heal automatically
    public int fractureDurationSeconds = 120; // How long before fractures auto-heal (if enabled)

    public boolean treatMultipleWounds = true; //Keep treatment windows open to cure multiple wounds at once

    // === NPC-SPECIFIC INJURY THRESHOLDS ===
    public int npcBodyPartFractureChance = 15;  // % base chance for NPCs
    public int npcBodyPartBleedChance = 15;     // % base chance for NPCs
    public int npcHeavyBleedChance = 10;         // % base chance for NPCs

    // === HEAVY DAMAGE MODIFIERS (Both Players and NPCs) ===
    public float heavyDamageThreshold = 0.50f;  // How much % of a limbs max health must be dealt for a heavy hit, which increases injuries
    public int heavyDamageFractureBonus = 25;   // + fracture chance on heavy hit
    public int heavyDamageBleedBonus = 25;      // + bleed chance on heavy hit

    public float headshotDamageBonus = 1.2f; //How much damage is multiplied by for headshots.

    // === NPC LOOT TABLES ===
    public Map<String, Map<String, List<LootEntry>>> npcLootTables = new HashMap<>();

    public static class LootEntry {
        public String itemId = "";
        public int minAmount = 1;
        public int maxAmount = 1;
        public float dropChance = 0.5f;

        public LootEntry() {}

        public LootEntry(String itemId, int minAmount, int maxAmount, float dropChance) {
            this.itemId = itemId;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.dropChance = dropChance;
        }
    }

    // Debug
    public boolean enableDebugLogging = false;

    public MortalWoundsConfig() {
        initializeDefaultLoot();
    }

    private void initializeDefaultLoot() {
        // SKELETON
        Map<String, List<LootEntry>> skeletonLoot = new HashMap<>();
        skeletonLoot.put("ANY", Arrays.asList(
                new LootEntry("Ingredient_Bone_Fragment", 1, 1, 0.50f)
        ));
        skeletonLoot.put("HEAD", Arrays.asList(
                new LootEntry("Ingredient_Bone_Fragment", 1, 1, 0.20f)
        ));
        npcLootTables.put("skeleton", skeletonLoot);

        // BEAR
        Map<String, List<LootEntry>> bearLoot = new HashMap<>();
        bearLoot.put("LEGS", Arrays.asList(
                new LootEntry("Ingredient_Hide_Heavy", 1, 1, 0.20f)
        ));
        bearLoot.put("ANY", Arrays.asList(
                new LootEntry("Ingredient_Hide_Heavy", 1, 1, 0.20f)
        ));
        bearLoot.put("FRONT_LEGS", Arrays.asList(
                new LootEntry("Ingredient_Hide_Heavy", 1, 1, 0.20f)
        ));
        bearLoot.put("TORSO", Arrays.asList(
                new LootEntry("Ingredient_Hide_Heavy", 1, 1, 0.30f)
        ));
        npcLootTables.put("bear", bearLoot);

        // WOLF
        Map<String, List<LootEntry>> wolfLoot = new HashMap<>();
        wolfLoot.put("ANY", Arrays.asList(
                new LootEntry("Ingredient_Hide_Medium", 1, 1, 0.25f)
        ));
        wolfLoot.put("TORSO", Arrays.asList(
                new LootEntry("Ingredient_Hide_Medium", 1, 1, 0.25f)
        ));
        npcLootTables.put("wolf", wolfLoot);

        // GOLEMS
//        Map<String, List<LootEntry>> golemLoot = new HashMap<>();
//        golemLoot.put("ANY", Arrays.asList(
//                new LootEntry("Rock_Stone_Cobble", 1, 1, 0.25f)
//        ));
//        golemLoot.put("CORE", Arrays.asList(
//                new LootEntry("Ingredient_Bar_Iron", 1, 1, 0.50f)
//        ));
//        npcLootTables.put("golem", golemLoot);

        // THUNDER GOLEM
        Map<String, List<LootEntry>> golemThunderLoot = new HashMap<>();
        golemThunderLoot.put("ANY", Arrays.asList(
                new LootEntry("Ingredient_Crystal_Yellow", 1, 1, 0.25f)
        ));
        golemThunderLoot.put("CORE", Arrays.asList(
                new LootEntry("Rock_Gem_Topaz", 1, 1, 0.50f)
        ));
        npcLootTables.put("Golem_Crystal_Thunder", golemThunderLoot);

        // SANDSWEPT GOLEM
        Map<String, List<LootEntry>> golemSandLoot = new HashMap<>();
        golemSandLoot.put("ANY", Arrays.asList(
                new LootEntry("Ingredient_Crystal_Cyan", 1, 1, 0.25f)
        ));
        golemSandLoot.put("CORE", Arrays.asList(
                new LootEntry("Rock_Gem_Zephyr", 1, 1, 0.50f)
        ));
        npcLootTables.put("Golem_Crystal_Sand", golemSandLoot);

        // EMBER GOLEM
        Map<String, List<LootEntry>> golemFlameLoot = new HashMap<>();
        golemFlameLoot.put("ANY", Arrays.asList(
                new LootEntry("Ingredient_Fabric_Scrap_Cindercloth", 1, 1, 0.25f)
        ));
        golemFlameLoot.put("CORE", Arrays.asList(
                new LootEntry("Rock_Gem_Ruby", 1, 1, 0.50f)
        ));
        npcLootTables.put("Golem_Crystal_Flame", golemFlameLoot);

        // FROST GOLEM
        Map<String, List<LootEntry>> golemFrostLoot = new HashMap<>();
        golemFrostLoot.put("ANY", Arrays.asList(
                new LootEntry("Ingredient_Crystal_Blue", 1, 1, 0.25f)
        ));
        golemFrostLoot.put("CORE", Arrays.asList(
                new LootEntry("Rock_Gem_Sapphire", 1, 1, 0.50f)
        ));
        npcLootTables.put("Golem_Crystal_Frost", golemFrostLoot);

        // FIRESTEEL GOLEM
        Map<String, List<LootEntry>> golemFiresteelLoot = new HashMap<>();
        golemFiresteelLoot.put("ANY", Arrays.asList(
                new LootEntry("Ingredient_Fire_Essence", 1, 1, 0.25f)
        ));
        golemFiresteelLoot.put("CORE", Arrays.asList(
                new LootEntry("Ingredient_Bar_Iron", 1, 1, 0.50f)
        ));
        npcLootTables.put("Golem_Firesteel", golemFiresteelLoot);

        // EARTHEN GOLEM
        Map<String, List<LootEntry>> golemEarthenLoot = new HashMap<>();
        golemEarthenLoot.put("ANY", Arrays.asList(
                new LootEntry("Ingredient_Crystal_Green", 1, 1, 0.25f)
        ));
        golemEarthenLoot.put("CORE", Arrays.asList(
                new LootEntry("Rock_Gem_Emerald", 1, 1, 0.50f)
        ));
        npcLootTables.put("Golem_Crystal_Earth", golemEarthenLoot);





        // SPIDER
        Map<String, List<LootEntry>> spiderLoot = new HashMap<>();
        spiderLoot.put("ANY", Arrays.asList(
                new LootEntry("Ingredient_Sac_Venom", 1, 1, 0.20f)
        ));
        spiderLoot.put("LEGS", Arrays.asList(
                new LootEntry("Ingredient_Fabric_Scrap_Silk", 1, 1, 0.10f)
        ));
        spiderLoot.put("ABDOMEN", Arrays.asList(
                new LootEntry("Ingredient_Fabric_Scrap_Silk", 1, 1, 0.10f)
        ));
        spiderLoot.put("HEAD", Arrays.asList(
                new LootEntry("Ingredient_Sac_Venom", 1, 1, 0.10f)
        ));
        npcLootTables.put("spider", spiderLoot);

        // ZOMBIE
        Map<String, List<LootEntry>> zombieLoot = new HashMap<>();
        zombieLoot.put("ANY", Arrays.asList(
                new LootEntry("Ingredient_Fabric_Scrap_Linen", 1, 1, 0.20f)
        ));
        npcLootTables.put("zombie", zombieLoot);

        // GOBLIN
        Map<String, List<LootEntry>> goblinLoot = new HashMap<>();
        goblinLoot.put("LEFTARM", Arrays.asList(
                new LootEntry("Ingredient_Fabric_Scrap_Linen", 1, 1, 0.20f)
        ));
        goblinLoot.put("RIGHTARM", Arrays.asList(
                new LootEntry("Crude_Dagger", 1, 1, 0.20f)
        ));
        npcLootTables.put("goblin", goblinLoot);

        // DRAGON (example for future)
        Map<String, List<LootEntry>> dragonLoot = new HashMap<>();
        dragonLoot.put("LEFT_WING", Arrays.asList(
                new LootEntry("Ingredient_Hide_Scaled", 1, 1, 0.20f)
        ));
        dragonLoot.put("RIGHT_WING", Arrays.asList(
                new LootEntry("Ingredient_Hide_Scaled", 1, 1, 0.20f)
        ));
        dragonLoot.put("TAIL", Arrays.asList(
                new LootEntry("Ingredient_Hide_Scaled", 1, 1, 0.20f)
        ));
        dragonLoot.put("HEAD", Arrays.asList(
                new LootEntry("Ingredient_Hide_Scaled", 1, 1, 0.20f),
                new LootEntry("Ingredient_Hide_Scaled", 1, 1, 0.20f)
        ));
        npcLootTables.put("dragon", dragonLoot);
    }

    /**
     * Get loot for a specific NPC type and body part
     */
    public List<LootEntry> getLootForLimb(String npcTypeId, String bodyPartName) {
        if (npcTypeId == null) return Collections.emptyList();

        String lowerNpcType = npcTypeId.toLowerCase();
        Map<String, List<LootEntry>> npcLoot = null;

        // Find matching NPC type (case insensitive, contains match)
        for (Map.Entry<String, Map<String, List<LootEntry>>> entry : npcLootTables.entrySet()) {
            if (lowerNpcType.contains(entry.getKey().toLowerCase())) {
                npcLoot = entry.getValue();
                break;
            }
        }

        if (npcLoot == null) return Collections.emptyList();

        List<LootEntry> result = new ArrayList<>();

        // Add "ANY" drops (apply to all body parts)
        if (npcLoot.containsKey("ANY")) {
            result.addAll(npcLoot.get("ANY"));
        }

        // Add specific body part drops
        if (npcLoot.containsKey(bodyPartName)) {
            result.addAll(npcLoot.get(bodyPartName));
        }

        // Handle special cases for legs
        if (bodyPartName.contains("LEG") && npcLoot.containsKey("LEGS")) {
            result.addAll(npcLoot.get("LEGS"));
        }
        if (bodyPartName.contains("FRONT") && bodyPartName.contains("LEG") && npcLoot.containsKey("FRONT_LEGS")) {
            result.addAll(npcLoot.get("FRONT_LEGS"));
        }

        // Handle core drops (torso/head for golems)
        if ((bodyPartName.equals("TORSO") || bodyPartName.equals("HEAD")) && npcLoot.containsKey("CORE")) {
            result.addAll(npcLoot.get("CORE"));
        }

        return result;
    }

    public static MortalWoundsConfig get() {
        if (instance == null) {
            instance = new MortalWoundsConfig();
        }
        return instance;
    }

    public static void set(MortalWoundsConfig config) {
        instance = config;
    }
}