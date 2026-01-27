package com.rajoki.injuryplugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.io.*;

public class InjuryPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILENAME = "injury_config.json";

    public InjuryPlugin(JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Hello from %s version %s", this.getName(), this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        // Load config first
        loadConfig();

        this.getCommandRegistry().registerCommand(new InjuryTestCommand());
        this.getCommandRegistry().registerCommand(new RandomHitCommandTest());

        // Register custom component
        ComponentType<EntityStore, HitTrackerComponent> hitTrackerComponentType =
                this.getEntityStoreRegistry().registerComponent(HitTrackerComponent.class, HitTrackerComponent::new);

        // Register systems
        this.getEntityStoreRegistry().registerSystem(new PlayerJoinHitTrackerSystem(hitTrackerComponentType));
        this.getEntityStoreRegistry().registerSystem(new InjuryRollSystem(hitTrackerComponentType));

        // Register arm fracture damage reduction system
        this.getEntityStoreRegistry().registerSystem(new ArmFractureDamageSystem());

        LOGGER.atInfo().log("Plugin setup complete!");
    }

    private void loadConfig() {
        File configFile = new File(CONFIG_FILENAME);
        InjuryConfig defaults = createDefaults();

        if (!configFile.exists()) {
            // Create new config with defaults
            InjuryConfig.set(defaults);
            saveConfig(configFile, defaults);
            LOGGER.atInfo().log("Created default config file: " + CONFIG_FILENAME);
        } else {
            // Load and merge existing config
            InjuryConfig config = loadAndMergeConfig(configFile, defaults);
            InjuryConfig.set(config);
            LOGGER.atInfo().log("Loaded config from: " + CONFIG_FILENAME);
        }
    }

    private InjuryConfig createDefaults() {
        return new InjuryConfig(); // Uses default values from class
    }

    private InjuryConfig loadAndMergeConfig(File configFile, InjuryConfig defaults) {
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

            InjuryConfig config = GSON.fromJson(userJson, InjuryConfig.class);

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

    private void saveConfig(File file, InjuryConfig config) {
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(config, writer);
            LOGGER.atInfo().log("Saved config to: " + file.getName());
        } catch (IOException e) {
            LOGGER.atInfo().log("Failed to save config: " + e.getMessage());
        }
    }
}