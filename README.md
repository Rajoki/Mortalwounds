Survival Overhaul - Mortal Wounds
Version 1.6.0 - NPC injury systems expanded! Torso fractures now cause NPCs to take increased damage. Head fractures cause NPCs to miss attacks 50% of the time with "MISS" displayed instead of damage.

Player torso fractures now increase stamina consumption by 1.5x.

1.5.0 - Tutorial UI and welcome message. Golem loot drops. Config rebalance. /mwstats reminder text in HUD.

Getting Started
Your body is now 6 limbs: head, torso, left/right arms, left/right legs. Each has separate health shown in your HUD (green = healthy, yellow = injured, red = critical, black = destroyed).

Damage is directional. Attacks from above/jumping hit the head. Crouch attacks/being below the target hit legs. Position matters.

Injuries happen when hit: Bleeds (damage over time), Fractures (reduce effectiveness), Destroyed limbs (need survival kit).

Effects vary by limb:

Arm fractures = less damage dealt (per each arm)
Leg fractures = slower movement (per each leg)
Torso fractures = 1.5x stamina drain
Head fractures = vision impaired
Healing items:

Bandages stop bleeding
Splints heal fractures
Survival Kits repair destroyed limbs
Craft these in your inventory using basic materials (fiber, sticks, rubble).

NPCs use the same system. Break their legs to slow them. Break their head to make them miss attacks. Break their arms to reduce their damage. Destroy limbs for extra loot drops.

Different types/anatomies can have different effects! Bears are quadrupeds - fracture their front legs to reduce damage they deal AND movement speed.

Commands: /mwstats (detailed UI), /mwtextstats (chat summary), /mwtutorial (interactive guide)

Hardcore Injury System for Hytale
Mortal Wounds transforms combat into a survival experience. Inspired by Outward and Escape from Tarkov, damage has lasting consequences. You are no longer a single health bar.

Body Part Damage
Six body parts with individual health:

Head
Torso
Left/Right Arms
Left/Right Legs
Each limb's health scales with your max health and armor. Better armor/food buffs = stronger limbs.

Directional damage. Height and angle determine what gets hit:

Strikes from above/jumping hit the head
Low swings hit legs
Side attacks hit torso/arms/legs
Crouching while attacking targets legs
Fall damage hits legs first with increased fracture chance.

Excess damage from a destroyed limb transfers to other parts.

Real-time HUD shows limb health and status icons for bleeds/fractures. Works with AutoMultiHud for custom positioning.

Injury System
Configurable chance to apply injuries to the struck body part.



Bleeds

Damage over time
Two bleeds on same limb = heavy bleed
Heavy bleeds last longer and deal more damage
Lower chance for direct heavy bleeds
Fractures Apply unique penalties per limb:

Arms: Reduced damage (one arm = minor penalty, both arms = major)
Legs: Reduced movement (one leg = minor penalty, both legs = major)
Torso: 1.5x stamina consumption rate
Head: Visual impairment effect
Fracture duration is configurable.

NPC Injury System
Enemies suffer the same injuries as players:

Bleeds (damage over time)
Heavy bleeds
Fractures (movement/attack penalties)
Destroyed limbs (loot drops)
NPC-specific injury effects:

Torso fractures: Take more damage
Head fractures: % chance to miss attacks (displays "MISS" instead of damage)
Leg fractures: Slower movement, forced to crouch when both legs broken
Combat has visible consequences. Damage numbers and injury text appear when hitting NPCs for feedback.

NPC Loot System
Destroying specific body parts can drop unique items:

Skeleton head → bone fragments
Goblin arm → weapon drop
Spider abdomen → silk
Golem torso → gems/ores
Golem limbs → shards
Fully configurable through the config file. Add custom mobs and drops as you desire.

Medical Treatment
Three healing items:

Crude Bandages: Remove bleeds/heavy bleeds

Crude Splints: Heal fractures 
Crude Survival Kits: Repair destroyed limbs
Using an item opens a body part selection UI. Choose which limb to treat. Each item heals one condition.

Craft all items in your inventory or at workbenches using fiber, sticks, and rubble.

Commands
/mwtutorial - Interactive tutorial UI

/mwstats - Detailed limb status UI

/mwtextstats - Limb health/injuries in chat

/mwheal - Admin: remove all injuries

Configuration
Customize nearly everything in mortalwounds_config (located in your world folder).

Injury Chances

bodyPartFractureChance - Base fracture chance
bodyPartBleedChance - Base bleed chance
heavyBleedChance - Bleed upgrade chance
Weapon Modifiers

slashmodifier - Slash weapon injury bonus
bludgeonmodifier - Blunt weapon injury bonus
npcSlashModifier / npcBludgeonModifier - NPC attack modifiers
Bleed Settings

bleedDamageAmount - Damage per tick
bleedDamageInterval - Time between ticks
bleedDurationSeconds - Total duration
Heavy bleed variants available
Fractures

armFractureDamageReduction1Arm / 2Arms
legFractureSpeedReduction1Leg / 2Legs
enableFractureDuration - Timed healing on/off
fractureDurationSeconds
Heavy Damage System

heavyDamageThreshold - Damage % for heavy hit
heavyDamageFractureBonus - Extra fracture chance
heavyDamageBleedBonus - Extra bleed chance
NPC Settings

npcBodyPartFractureChance
npcBodyPartBleedChance
npcHeavyBleedChance
headshotDamageBonus - How much damage is multiplied for with headshots - effective on both NPCs and players
Treatment

treatMultipleWounds - Keep UI open for batch healing
Loot Tables Configure drops per mob and body part:

 
 
json
"skeleton": {
  "HEAD": [{
    "itemId": "Ingredient_Bone_Fragment",
    "minAmount": 1,
    "maxAmount": 1,
    "dropChance": 0.2
  }]
}
Use "ANY" as body part key for drops from any destroyed limb.

Part of a Larger Vision
Mortal Wounds is the foundation of a full survival overhaul for Hytale.

Planned features:

More injury types (burns, magic damage)
Higher tier medical items
Temporary painkillers and quick treatment
Known Issues
Limited multiplayer testing. Feedback appreciated.

HUD updates may conflict with some UI mods.

Death from injuries may show incorrect death messages.

Changelog
1.6.0 - Player torso fractures now increase stamina drain (1.5x). NPC torso fractures increase damage taken (1.5x). NPC head fractures cause 50% miss chance. Tutorial system added. Golem loot tables added. Injury chances rebalanced.

1.5.0 - Tutorial UI and welcome message. Golem loot drops. Config rebalance. /mwstats reminder text in HUD.

1.4.0 - Crouching affects directional damage.

1.3.0 - NPC injury system. Body part loot drops. Headshot damage bonus. Improved hit detection. Heavy damage bonuses.

1.2.0 - Head fracture visual effect.
