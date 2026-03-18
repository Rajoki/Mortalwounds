package com.rajoki.injuryplugin.ui;

public class TutorialPageData {

    public static final String[] TUTORIAL_PAGES = {
            // Page 1
            "Welcome to Mortal Wounds!\n\n" +
                    "Your body is no longer a single health bar.\n" +
                    "Each limb has its own health and can suffer injuries.\n\n" +
                    "Click 'Next' to learn more!",

            // Page 2
            "Each body part has separate health based on your current max health:\n" +
                    "• Head\n" +
                    "• Torso\n" +
                    "• Left Arm / Right Arm\n" +
                    "• Left Leg / Right Leg\n\n" +
                    "Damage is applied to specific limbs based on\n" +
                    "where and how you're hit! Depends on height difference and which side you're hit from\n" +
                    "Increase limb health with armor/food bonuses!",

            // Page 3
            "Injuries have gameplay effects:\n\n" +
                    "BLEED - Damage over time\n" +
                    "HEAVY BLEED - Higher damage over time, and more deadly\n" +
                    "FRACTURE - Reduces effectiveness\n" +
                    "BROKEN - Limb reaches 0 hp/cannot be healed until survival kit is used\n" +
                    "• Fractured legs = slower movement\n" +
                    "• Fractured arms = less damage dealt\n" +
                    "• Fractured torso = higher stamina consumption",

            // Page 4
            "Use medical items to heal:\n" +
                    "Bandages - Stop bleeding\n" +
                    "Splints - Heal fractures\n" +
                    "Survival Kits - Repair destroyed limbs\n\n" +
                    "When a limb reaches 0 hp, it is considered destroyed and\n " +
                    "cannot be healed until a survival kit is used\n" +
                    "Craft them in your inventory or at a workbench\n" +
                    "using basic materials (fiber, sticks, rubble)",

            // Page 5
            "Type /mwstats to open this UI anytime!\n" +
                    "Hover over injury icons to see details:\n" +
                    "• What the injury does\n" +
                    "• How to heal it\n\n" +
                    "Try it now to see!\n" +
                    "The color of each limb shows its health:\n" +
                    "Green = Healthy  Yellow = Injured  Red = Critical\n" +
                    "You can also see your current condition on the on-screen HUD, with injury icons",

            // Page 6
            "NPCs use the SAME system!\n\n" +
                    "Attack enemies strategically:\n" +
                    "• Jump or attack from above = aims for HEAD - headshots do extra damage\n" +
                    "• Attack from below or CROUCH = aims for LEGS\n" +
                    "• Attack from sides = hits ARMS\n\n" +
                    "Break their legs to slow them down!\n" +
                    "Break their arms to reduce their damage!\n" +
                    "Cause bleeds to damage them over time!",

            // Page 7
            "Breaking enemy limbs drops extra loot!\n\n" +
                    "Examples:\n" +
                    "• Bears - Break limbs for more hide\n" +
                    "• Skeletons - Break bones for fragments\n" +
                    "• Wolves - Break limbs for more hide\n\n" +
                    "\"Breaking\" = depleting a limb to 0 HP\n",

            // Page 8
            "Certain enemies may be resistant to different injuries or immune to them\n" +
            "Skeletons = Immune to bleeding, but can be fractured\n" +
            "Golems = Immune to bleeding, and resistant to fractures. (Can be fractured only by\n" +
            "breaking the limb, and not by random chance\n\n" +
            "Different weapon or creature types also cause injuries at different rates\n" +
            "Swords/daggers/slashing weapons = More chance for bleeds to occur\n" +
            "Maces/bludgeon weapons = More chance for fractures to occur",


            // Page 9
            "You're ready to play! Many of these systems can be changed to your liking in the\n" + "" +
                    "mortalwounds_config file!\n" +
                    "Useful Commands:\n" +
                    "/mwstats - View detailed limb status\n" +
                    "/mwtextstats - Quick text summary\n" +
                    "/mwtutorial - Show this tutorial again\n" +
                    "/mwheal - Admin only, removes all current injuries\n" +
                    "Good luck, and watch your limbs!"
    };

    public static int getTotalPages() {
        return TUTORIAL_PAGES.length;
    }

    public static String getPageText(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= TUTORIAL_PAGES.length) {
            return "Invalid page";
        }
        return TUTORIAL_PAGES[pageIndex];
    }
}