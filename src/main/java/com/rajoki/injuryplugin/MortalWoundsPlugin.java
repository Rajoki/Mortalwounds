package com.rajoki.injuryplugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.rajoki.injuryplugin.commands.*;
import com.rajoki.injuryplugin.components.BodyPartComponent;
import com.rajoki.injuryplugin.components.HitTrackerComponent;
import com.rajoki.injuryplugin.components.npc.NPCBodyPartComponent;
import com.rajoki.injuryplugin.config.MortalWoundsConfig;
import com.rajoki.injuryplugin.systems.BodyPartMultiplierSystem;
import com.rajoki.injuryplugin.systems.PlayerJoinHitTrackerSystem;
import com.rajoki.injuryplugin.systems.bodypartsystems.*;
import com.rajoki.injuryplugin.systems.npc.*;
import com.rajoki.injuryplugin.ui.firstaidui.*;
import com.rajoki.injuryplugin.ui.hud.BodyPartHudTickSystem;

import java.io.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MortalWoundsPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILENAME = "mortalwounds_config.json";

    private static MortalWoundsPlugin instance;
    private File configFile;

    private ComponentType<EntityStore, BodyPartComponent> bodyPartComponentType;
    // private BodyPartHudService bodyPartHudService;

    public MortalWoundsPlugin(JavaPluginInit init) {
        super(init);
        instance = this;
        LOGGER.atInfo().log("Hello from %s version %s", this.getName(), this.getManifest().getVersion().toString());
    }

    public static MortalWoundsPlugin getInstance() {
        return instance;
    }

    public static HytaleLogger getPluginLogger() {
        return LOGGER;
    }

    public ComponentType<EntityStore, BodyPartComponent> getBodyPartComponentType() {
        return bodyPartComponentType;
    }

//    public BodyPartHudService getBodyPartHudService() {
//        return bodyPartHudService;
//    }

    private final Map<UUID, BodyPartComponent> savedBodyParts = new ConcurrentHashMap<>();

    @Override
    protected void setup() {
        // Load config first
        loadConfig();


        // Register commands
        this.getCommandRegistry().registerCommand(new InjuryTestCommand());
        this.getCommandRegistry().registerCommand(new RandomHitCommandTest());
        this.getCommandRegistry().registerCommand(new BodyPartStatsCommand());
        this.getCommandRegistry().registerCommand(new BodyPartStatusCommand());
        this.getCommandRegistry().registerCommand(new HealBodyPartsCommand());



        // Register HitTracker component (existing injury roll system)
        ComponentType<EntityStore, HitTrackerComponent> hitTrackerComponentType =
                this.getEntityStoreRegistry().registerComponent(HitTrackerComponent.class, HitTrackerComponent::new);

        // Register BodyPart component with CODEC for persistence
        this.bodyPartComponentType = this.getEntityStoreRegistry().registerComponent(
                BodyPartComponent.class,
                "BodyPartComponent",  // Important: unique name
                BodyPartComponent.CODEC  // This enables persistence
        );

        // Register NPC body part component
        ComponentType<EntityStore, NPCBodyPartComponent> npcBodyPartType =
                this.getEntityStoreRegistry().registerComponent(
                        NPCBodyPartComponent.class,
                        NPCBodyPartComponent::new
                );

        // Register HitTracker systems
        this.getEntityStoreRegistry().registerSystem(new PlayerJoinHitTrackerSystem(hitTrackerComponentType));
        // this.getEntityStoreRegistry().registerSystem(new InjuryRollSystem(hitTrackerComponentType));

        // Register BodyPart systems (using the SAME bodyPartComponentType)
        this.getEntityStoreRegistry().registerSystem(new PlayerJoinBodyPartSystem(bodyPartComponentType));
        this.getEntityStoreRegistry().registerSystem(new BodyPartDamageSystem(bodyPartComponentType));
        this.getEntityStoreRegistry().registerSystem(new BodyPartRecoverySystem(bodyPartComponentType));
        this.getEntityStoreRegistry().registerSystem(new LegFractureMovementSystem(bodyPartComponentType));
        this.getEntityStoreRegistry().registerSystem(new BodyPartHudTickSystem(bodyPartComponentType));
        this.getEntityStoreRegistry().registerSystem(new BodyPartBleedSystem(bodyPartComponentType));
        //Disabled TorsoFracture for now because of bug
        // this.getEntityStoreRegistry().registerSystem(new TorsoFractureStaminaSystem(bodyPartComponentType));
        this.getEntityStoreRegistry().registerSystem(new HeadFractureEffectSystem(bodyPartComponentType));
        this.getEntityStoreRegistry().registerSystem(new OnDeathInjuryResetSystem());
        this.getEntityStoreRegistry().registerSystem(new FractureDurationSystem(bodyPartComponentType));
        this.getEntityStoreRegistry().registerSystem(new BodyPartDestroyedHealthSystem(bodyPartComponentType));

        // Register NPC systems
        this.getEntityStoreRegistry().registerSystem(new NPCJoinBodyPartSystem(npcBodyPartType));
        this.getEntityStoreRegistry().registerSystem(new NPCBodyPartDamageSystem(npcBodyPartType));
        this.getEntityStoreRegistry().registerSystem(new NPCLegFractureMovementSystem(npcBodyPartType));
        this.getEntityStoreRegistry().registerSystem(new NPCArmFractureDamageSystem(npcBodyPartType));
        this.getEntityStoreRegistry().registerSystem(new NPCBodyPartBleedSystem(npcBodyPartType));

        this.getEntityStoreRegistry().registerSystem(new BodyPartMultiplierSystem(bodyPartComponentType, npcBodyPartType));

        // Register arm fracture damage reduction system
        this.getEntityStoreRegistry().registerSystem(new ArmFractureDamageSystem());

        // Load saved data
//        loadSavedBodyParts(); - old, keeping for now just in case

        // Initialize BodyPartHudService
       // this.bodyPartHudService = new BodyPartHudService();

        OpenCustomUIInteraction.registerCustomPageSupplier(
                this,
                BandageSelectionPageNew.class,
                "BandageSelectionNew",
                new BandagePageNewSupplier()
        );


        OpenCustomUIInteraction.registerCustomPageSupplier(
                this,
                SplintSelectionPageNew.class,
                "SplintSelectionNew",
                new SplintPageNewSupplier()
        );

        OpenCustomUIInteraction.registerCustomPageSupplier(
                this,
                SurvKitSelectionPageNew.class,
                "SurvKitSelection",
                new SurvKitPageSupplier()
        );




        LOGGER.atInfo().log("MortalWoundsPlugin setup complete!");
        // LOGGER.atInfo().log("BodyPartHudService initialized");
    }

    @Override
    public void start() {
        EventBus bus = HytaleServer.get().getEventBus();

        // Old for ticking/updating on screen hud, keeping for now
        // Initialize BodyPartHudService
//        this.bodyPartHudService = new BodyPartHudService();
//
//        // Register event handlers in the service (for HUD management)
//        bus.registerGlobal(PlayerReadyEvent.class, bodyPartHudService::handlePlayerReady);
//        bus.registerGlobal(PlayerDisconnectEvent.class, bodyPartHudService::handlePlayerDisconnect);
//
//        // Start the service (handles ticking/updating)
//        bodyPartHudService.start();

        LOGGER.atInfo().log("MortalWoundsPlugin started: BodyPart HUD always on.");
    }






    private void loadConfig() {
        this.configFile = new File(CONFIG_FILENAME);
        File configFile = this.configFile;
        MortalWoundsConfig defaults = createDefaults();

        if (!configFile.exists()) {
            // Create new config with defaults
            MortalWoundsConfig.set(defaults);
            saveConfig(configFile, defaults);
            LOGGER.atInfo().log("Created default config file: " + CONFIG_FILENAME);
        } else {
            // Load and merge existing config
            MortalWoundsConfig config = loadAndMergeConfig(configFile, defaults);
            MortalWoundsConfig.set(config);
            LOGGER.atInfo().log("Loaded config from: " + CONFIG_FILENAME);
        }
    }

    private MortalWoundsConfig createDefaults() {
        return new MortalWoundsConfig();
    }

    private MortalWoundsConfig loadAndMergeConfig(File configFile, MortalWoundsConfig defaults) {
        try (FileReader reader = new FileReader(configFile)) {
            JsonObject userJson = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject defaultsJson = GSON.toJsonTree(defaults).getAsJsonObject();
            boolean updated = false;

            // Add any missing keys from defaults
            for (String key : defaultsJson.keySet()) {
                if (!userJson.has(key)) {
                    userJson.add(key, defaultsJson.get(key));
                    updated = true;
                }
            }

            MortalWoundsConfig config = GSON.fromJson(userJson, MortalWoundsConfig.class);

            // Save if we added new fields
            if (updated) {
                saveConfig(configFile, config);
                LOGGER.atInfo().log("Updated config with new fields");
            }

            return config;
        } catch (Exception e) {
            LOGGER.atInfo().log("Failed to load config, using defaults: " + e.getMessage());
            return defaults;
        }
    }

    public void saveConfig() {
        saveConfig(configFile, MortalWoundsConfig.get());
    }

    private void saveConfig(File file, MortalWoundsConfig config) {
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(config, writer);
            LOGGER.atInfo().log("Saved config to: " + file.getName());
        } catch (IOException e) {
            LOGGER.atInfo().log("Failed to save config: " + e.getMessage());
        }
    }
//       Old system for saving data
//    private static final String BODYPART_SAVE_FILE = "bodyparts.json";
//
//    private void loadSavedBodyParts() {
//        File file = new File(BODYPART_SAVE_FILE);
//        if (!file.exists()) return;
//
//        try (FileReader reader = new FileReader(file)) {
//            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
//            for (String uuidStr : json.keySet()) {
//                UUID uuid = UUID.fromString(uuidStr);
//                BodyPartComponent component = GSON.fromJson(json.get(uuidStr), BodyPartComponent.class);
//                savedBodyParts.put(uuid, component);
//            }
//            LOGGER.atInfo().log("Loaded saved body part data for " + savedBodyParts.size() + " players");
//        } catch (Exception e) {
//            LOGGER.atWarning().log("Failed to load saved body parts: " + e.getMessage());
//        }
//    }
//
//    public void savePlayerBodyPart(PlayerRef ref, BodyPartComponent component) {
//        savedBodyParts.put(ref.getUuid(), component);
//        saveAllBodyParts();
//    }
//
//    private void saveAllBodyParts() {
//        File file = new File(BODYPART_SAVE_FILE);
//        try (FileWriter writer = new FileWriter(file)) {
//            GSON.toJson(savedBodyParts, writer);
//        } catch (Exception e) {
//            LOGGER.atWarning().log("Failed to save body parts: " + e.getMessage());
//        }
//    }



}