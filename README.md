# Survival Overhaul - Mortal Wounds

1.4.0 - Crouching now effects damage direction! Crouch while attacking for "low" attacks. If on the same level as the target, have a chance to hit either torso or legs. If you're on lower ground and crouching, you'll have a higher chance to hit legs.
1.3.1 + 1.3.2 - Removed many server/debug logs I had on. I think I got them all this time, sorry about that!
1.3.0 Update
Major update expanding the injury system beyond players.

NPCs now use the same body part damage and injury system, meaning enemies can receive fractures, bleeds, and heavy bleeds on specific limbs just like players.

Examples

• Breaking a skeleton’s head may cause bone fragments to drop
• Breaking a wolf’s torso can drop hide
• Breaking a goblin’s arm may cause it to drop its weapon

This is currently a basic implementation, but the system is configurable. Players and server owners can add any mobs, and item drops through the config.

Other additions and improvements:

• NPC body part injuries (bleeds, heavy bleeds, fractures)

Creature-specific traits (skeletons can't bleed, golems resist fractures)
• Body-part-based NPC loot drops (configurable)
Weapon modifiers now affect both player and NPC injuries
• Damage popup text showing injuries inflicted on enemies
• Headshots now deal bonus damage
• Improved body part hit detection
Hits from above are more likely to strike the head
Hits from below are more likely to strike legs
Side hits distribute damage between torso and limbs
Smart hit detection adapts to creature anatomy (humanoids, quadrupeds, spiders)
• Heavy damage bonus system - larger/stronger hits have higher chances to cause injuries. Doing over 50% of a limbs damage in one strike will have a higher chance for bleed/fractures.
• Improved combat feedback when damaging NPC limbs - sound effects played, damage text displayed.
Balance / System Changes

• Torso fracture stamina reduction temporarily removed while the system is redesigned
• Torso fractures currently have no gameplay effect
• Several internal improvements to the injury and hit detection systems

In Progress

• Visual effects and particles for injuries are being implemented
• A small bug currently causes a delay in the effects, so they are temporarily disabled while being fixed

Hardcore Injury System for Hytale
Mortal Wounds transforms combat into a true survival experience. Inspired by hardcore survival games like Outward and Escape from Tarkov, damage now has lasting consequences. You are no longer a single health bar.

Features
Body Part and Locational Damage
Your character now has individual body parts:

Head
Torso
Left Arm
Right Arm
Left Leg
Right Leg

Each limb has its own health pool based on a percentage of your maximum health. Increase your max health or armor and each limb becomes stronger as well.

Damage is locational. The height and direction of the hit determines which body part takes damage.

A strike from above may hit the head.
A low swing may hit the legs.
Positioning now matters.

Falling from a height will hurt your legs first.

If a body part reaches 0 health and is struck again, excess damage will transfer to the closest or a random remaining body part.

Your HUD displays each limb’s health in real time:

Green = Healthy
Yellow = Injured
Red = Critically Damaged
Black = Destroyed





Your HUD will also show status icons next to that limb if you have any bleeds or fractures.

If you have the AutoMultiHud mod installed, the limb HUD can be moved and configured freely using the in-game /amh command.

Use /mwstats to open a detailed UI of your limb health.

Use /mwtextstats to display your condition in chat.

Injury System
Taking damage has a configurable chance to apply injuries to the specific body part that was hit.

Fall damage damages legs first and has a heightened chance to cause fractures.

All chances and values listed below are configurable.



Bleed
Damage over time
Moderate severity

Configurable damage amount, interval, and duration.

If you receive another bleed on the same limb that already has one, it becomes a heavy bleed.

Heavy Bleed
Higher damage over time
Lasts longer
Much more dangerous if untreated

Fractures
Fractures apply unique effects depending on the limb.

Arm Fracture
Reduces damage dealt.

One arm fractured reduces damage.
Both arms fractured reduces it further.

Leg Fracture
Reduces movement speed.

One leg fractured reduces speed.
Both legs reduce it further.

Torso Fracture
Currently has no gameplay effect while the system is being redesigned.

Head Fracture
Applies a visual impairment effect.

Fracture duration can be enabled or disabled in the config.

NPC Injury System
Enemies now use the same body part damage and injury system.

NPCs can suffer:

• Bleeds
• Heavy Bleeds
• Fractures
• Destroyed limbs

This allows combat to have visible consequences, such as enemies walking slower, forced to crouch when legs broken, suffering damage over time, or dropping items from specific body parts.

Damage numbers and injury notifications now appear when damaging NPCs to show what effects were applied.

NPC Body Part Loot System
Enemies can drop items depending on which body part is destroyed.

Examples:

Breaking a skeleton head → bone fragments
Breaking a goblin arm → weapon drop
Breaking a spider abdomen → silk

The system is fully configurable, allowing players and servers to add:

• new mobs
• custom item drops

through the config file.

Medical Items and Treatment
Using a Crude Bandage, Crude Splint, or Crude Survival Kit now opens a body part selection UI.






You choose which limb to treat.

Bandages remove bleeds and heavy bleeds.
Splints heal fractures.
Survival Kits repair destroyed limbs.

Each item heals one condition at a time.

Commands
/mwstats
Opens a detailed limb status UI

/mwtextstats
Shows limb health and injuries in chat

/mwheal
Admin command that removes injuries

/injury
Tests if the injury system is working

/randomhit
Tests the injury roll system

Built as Part of a Larger Survival Vision
Mortal Wounds is part of a planned full survival overhaul for Hytale.

Future systems will expand combat, injuries, treatment, and survival mechanics.

Planned features include:

• More injury types (burns, magic damage)
• Damage scaling for injury chances
• Higher tier medical items
• Temporary painkillers and quick treatment items

Configurable Options
Many aspects of Mortal Wounds can be customized in the mortalwounds_config file located in your world folder.

Injury Chances
bodyPartFractureChance
Base chance for fractures

bodyPartBleedChance
Base chance for bleeds

heavyBleedChance
Chance a bleed becomes a heavy bleed

Weapon Modifiers
slashmodifier
Extra injury chance from slashing weapons

bludgeonmodifier
Extra injury chance from blunt weapons

npcSlashModifier
Modifier for NPC attacks that count as slashing

npcBludgeonModifier
Modifier for NPC attacks that count as blunt damage

Bleed Settings
bleedDamageAmount
Damage per bleed tick

bleedDamageInterval
Time between bleed damage ticks

bleedDurationSeconds
Total bleed duration

Heavy Bleeds
heavyBleedDamageAmount
Damage per tick

heavyBleedDamageInterval

heavyBleedDurationSeconds

Fractures
armFractureDamageReduction1Arm
Damage reduction when one arm is fractured

armFractureDamageReduction2Arms
Damage reduction when both arms are fractured

legFractureSpeedReduction1Leg
Movement reduction for one fractured leg

legFractureSpeedReduction2Legs

enableFractureDuration
Enables timed fracture healing

fractureDurationSeconds

Treatment Settings
treatMultipleWounds
Keeps the treatment UI open to treat multiple injuries

NPC Injury Chances
npcBodyPartFractureChance

npcBodyPartBleedChance

npcHeavyBleedChance

Heavy Damage Bonus System
heavyDamageThreshold
Damage percentage that counts as a heavy hit

heavyDamageFractureBonus
Extra fracture chance for heavy hits

heavyDamageBleedBonus
Extra bleed chance for heavy hits

NPC Loot Table Configuration
The npcLootTables section allows you to define drops based on mob type and body part.

Example:


"skeleton": {
"HEAD": [
{
"itemId": "Ingredient_Bone_Fragment",
"minAmount": 1,
"maxAmount": 1,
"dropChance": 0.2
}
]
}

This means:

If a skeleton's head is destroyed, it has a 20% chance to drop a bone fragment.

Special Key: Use "ANY" for that mob to drop items from any part of it that is destroyed.

You can add your own mobs, body parts, and items to customize drops.

Known Issues
I haven’t been able to test it extensively on multiplayer servers yet, so feedback is appreciated.

Dynamic HUD updates can sometimes conflict with other UI mods.

If you die from a bleed or other injury, the death message may display incorrectly.

Changelog
1.2.2
Bug fix for torso fracture stamina clamp issue.

1.2.1
Reduced time to use medical items.

1.2.0
Added head concussion visual effect for head fractures.

Also fixed a bug where player HUDs sometimes did not appear after rejoining a server.

