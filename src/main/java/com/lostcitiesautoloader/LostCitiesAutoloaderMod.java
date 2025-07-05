package com.lostcitiesautoloader;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(LostCitiesAutoloaderMod.MODID)
public class LostCitiesAutoloaderMod {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "lostcitiesautoloader";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public LostCitiesAutoloaderMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Lost Cities Autoloader: Initializing mod");
        
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        
        // Register config event handlers on the mod bus (not the common bus)
        modEventBus.addListener(this::onConfigLoading);
        modEventBus.addListener(this::onConfigReloading);

        // Register ourselves for server and other game events we are interested in.
        NeoForge.EVENT_BUS.register(this);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, AutoloaderConfigSimple.SPEC);
        
        LOGGER.info("Lost Cities Autoloader: Mod initialization complete");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Lost Cities Autoloader - Common Setup");
        
        // Setup configuration directory
        setupConfigDirectory();
        
        // Initialize dimension management
        LOGGER.info("Initializing dimension management features");
        
        // DON'T access config values during setup - this causes circular dependency
        LOGGER.info("Common setup complete - config will be accessed later during server startup");
    }

    private void setupConfigDirectory() {
        try {
            Path configDir = Paths.get("config", "lost_cities_autoloader");
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
                LOGGER.info("Created Lost Cities Autoloader configuration directory: {}", configDir);
                
                // Create example configuration file
                createExampleConfig(configDir);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create configuration directory", e);
        }
    }

    private void createExampleConfig(Path configDir) {
        try {
            Path exampleFile = configDir.resolve("example_preset.json");
            if (!Files.exists(exampleFile)) {
                String exampleContent = """
                    {
                        "profile": "default",
                        "description": "Default Lost Cities profile - cities are generated normally",
                        "settings": {
                            "cityChance": 0.02,
                            "cityRadius": 128,
                            "generateLighting": true,
                            "generateSpawners": false,
                            "ruinChance": 0.1
                        }
                    }
                    """;
                Files.writeString(exampleFile, exampleContent);
                LOGGER.info("Created example configuration: {}", exampleFile);
            }
            
            // Create an example for custom dimensions
            Path dimensionExampleFile = configDir.resolve("custom_dimension_example.json");
            if (!Files.exists(dimensionExampleFile)) {
                String dimensionExampleContent = """
                    {
                        "profile": "tallbuildings",
                        "description": "Example configuration for custom dimensions - requires manual dimension config",
                        "settings": {
                            "cityChance": 0.1,
                            "cityRadius": 200,
                            "generateLighting": true,
                            "generateSpawners": true,
                            "ruinChance": 0.05,
                            "vineChance": 0.2
                        }
                    }
                    """;
                Files.writeString(dimensionExampleFile, dimensionExampleContent);
                LOGGER.info("Created custom dimension example configuration: {}", dimensionExampleFile);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create example configuration", e);
        }
    }

    @SubscribeEvent
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        LOGGER.info("Lost Cities Autoloader - Server About To Start (applying configuration early)");
        
        // This is the optimal time to apply Lost Cities configuration
        // It happens after configs are loaded but before world generation begins
        // At this point biome modifiers and world generation settings are being applied
        AutoloaderProfileManager.loadAndApplyConfiguration();
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Lost Cities Autoloader - Server Starting");
        
        // Additional safety check - apply configuration again if needed
        // This ensures we catch any cases where the early application didn't work
        AutoloaderProfileManager.loadAndApplyConfiguration();
    }

    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        // This gets called when a level is loaded - we can use this to ensure 
        // our profile is applied to new worlds, but only if config is ready
        if (event.getLevel().isClientSide()) {
            return; // Only process on server side
        }
        
        LOGGER.debug("Level loaded: {}", event.getLevel());
        
        // Try to apply the configuration when each world loads
        // This ensures we catch newly created worlds
        AutoloaderProfileManager.loadAndApplyConfiguration();
    }

    public void onConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == AutoloaderConfigSimple.SPEC) {
            LOGGER.info("Lost Cities Autoloader config loading detected");
            // Add targeted debugging for config values
            LOGGER.debug("Config loaded - enable_autoloader: {}", AutoloaderConfigSimple.ENABLE_AUTOLOADER.get());
            LOGGER.debug("Config loaded - config_file_name: {}", AutoloaderConfigSimple.CONFIG_FILE_NAME.get());
            LOGGER.debug("Config loaded - enable_custom_spawn: {}", AutoloaderConfigSimple.ENABLE_CUSTOM_SPAWN.get());
            LOGGER.debug("Config loaded - player_spawn_dimension: {}", AutoloaderConfigSimple.PLAYER_SPAWN_DIMENSION.get());
            LOGGER.debug("Config loaded - lost_city_dimension: {}", AutoloaderConfigSimple.LOST_CITY_DIMENSION.get());
        }
    }

    public void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == AutoloaderConfigSimple.SPEC) {
            LOGGER.info("Lost Cities Autoloader config reloading detected");
            // Add targeted debugging for config values
            LOGGER.debug("Config reloaded - enable_autoloader: {}", AutoloaderConfigSimple.ENABLE_AUTOLOADER.get());
            LOGGER.debug("Config reloaded - config_file_name: {}", AutoloaderConfigSimple.CONFIG_FILE_NAME.get());
            LOGGER.debug("Config reloaded - enable_custom_spawn: {}", AutoloaderConfigSimple.ENABLE_CUSTOM_SPAWN.get());
            LOGGER.debug("Config reloaded - player_spawn_dimension: {}", AutoloaderConfigSimple.PLAYER_SPAWN_DIMENSION.get());
            LOGGER.debug("Config reloaded - lost_city_dimension: {}", AutoloaderConfigSimple.LOST_CITY_DIMENSION.get());
        }
    }

}
