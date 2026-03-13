package com.rajoki.injuryplugin.systems.npc;

import com.rajoki.injuryplugin.config.MortalWoundsConfig;
import com.rajoki.injuryplugin.systems.bodypartsystems.BodyPart;

import java.util.*;

//Entries/config for loot tables for npcs dropping items based on body part destroyed

public class NPCLimbLootTable {

    public static class LootDrop {
        public final String itemId;
        public final int minAmount;
        public final int maxAmount;
        public final float dropChance;

        public LootDrop(String itemId, int minAmount, int maxAmount, float dropChance) {
            this.itemId = itemId;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.dropChance = dropChance;
        }

        public static LootDrop fromConfig(MortalWoundsConfig.LootEntry entry) {
            return new LootDrop(entry.itemId, entry.minAmount, entry.maxAmount, entry.dropChance);
        }
    }

    public static List<LootDrop> getLootForLimb(String npcTypeId, BodyPart bodyPart) {
        List<MortalWoundsConfig.LootEntry> configEntries =
                MortalWoundsConfig.get().getLootForLimb(npcTypeId, bodyPart.name());

        List<LootDrop> result = new ArrayList<>();
        for (MortalWoundsConfig.LootEntry entry : configEntries) {
            result.add(LootDrop.fromConfig(entry));
        }

        return result;
    }

    public static boolean hasLimbLoot(String npcTypeId) {
        return !MortalWoundsConfig.get().getLootForLimb(npcTypeId, "ANY").isEmpty();
    }
}