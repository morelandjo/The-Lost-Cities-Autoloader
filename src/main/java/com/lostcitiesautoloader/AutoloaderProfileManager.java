package com.lostcitiesautoloader;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Manages the automatic profile loading and application for Lost Cities
 */
public class AutoloaderProfileManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    
    private static boolean lostCitiesLoaded = false;
    private static Class<?> configClass = null;
    private static Object profileFromClientField = null;
    private static Object jsonFromClientField = null;
    private static Class<?> profileSetupClass = null;
    private static Object standardProfilesField = null;
    
    static {
        checkLostCitiesAvailability();
    }
    
    private static void checkLostCitiesAvailability() {
        lostCitiesLoaded = ModList.get().isLoaded("lostcities");
        
        if (lostCitiesLoaded) {
            try {
                // Get Lost Cities Config class
                configClass = Class.forName("mcjty.lostcities.setup.Config");
                
                // Get the profile fields
                Field profileField = configClass.getDeclaredField("profileFromClient");
                profileField.setAccessible(true);
                profileFromClientField = profileField;
                
                Field jsonField = configClass.getDeclaredField("jsonFromClient");
                jsonField.setAccessible(true);
                jsonFromClientField = jsonField;
                
                // Get ProfileSetup class
                profileSetupClass = Class.forName("mcjty.lostcities.config.ProfileSetup");
                Field standardField = profileSetupClass.getDeclaredField("STANDARD_PROFILES");
                standardField.setAccessible(true);
                standardProfilesField = standardField;
                
                LOGGER.info("Successfully connected to Lost Cities mod");
                
            } catch (Exception e) {
                LOGGER.error("Failed to connect to Lost Cities mod", e);
                lostCitiesLoaded = false;
            }
        } else {
            LOGGER.warn("Lost Cities mod not found - autoloader will not function");
        }
    }
    
    public static void loadAndApplyConfiguration() {
        LOGGER.info("Loading and applying Lost Cities Autoloader configuration");
        
        if (!lostCitiesLoaded) {
            LOGGER.warn("Lost Cities mod not available - cannot apply autoloader configuration");
            return;
        }

        // Check if the config spec is loaded
        if (!AutoloaderConfigSimple.SPEC.isLoaded()) {
            LOGGER.info("Config not loaded yet, will retry later");
            return;
        }
        
        // Check if autoloader is enabled
        try {
            boolean enabledValue = AutoloaderConfigSimple.ENABLE_AUTOLOADER.get();
            LOGGER.debug("Autoloader enabled: {}", enabledValue);
            
            if (!enabledValue) {
                LOGGER.info("Lost Cities Autoloader is disabled in configuration");
                return;
            }
        } catch (Exception e) {
            LOGGER.warn("Config value not ready yet: {}", e.getMessage());
            return;
        }
        
        // Check if Lost Cities profiles are loaded yet
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> standardProfiles = (Map<String, Object>) ((Field)standardProfilesField).get(null);
            if (standardProfiles == null || standardProfiles.isEmpty()) {
                LOGGER.info("Lost Cities profiles not loaded yet, will retry later");
                return;
            }
            LOGGER.debug("Lost Cities has {} profiles available", standardProfiles.size());
        } catch (Exception e) {
            LOGGER.warn("Cannot access Lost Cities profiles yet: {}", e.getMessage());
            return;
        }

        try {
            Path configDir = Paths.get("config", "lost_cities_autoloader");
            if (!Files.exists(configDir)) {
                LOGGER.info("No autoloader configuration directory found - no profile will be applied");
                return;
            }
            
            // Load the specific configuration file specified in config
            String configFileName;
            try {
                configFileName = AutoloaderConfigSimple.CONFIG_FILE_NAME.get() + ".json";
                LOGGER.debug("Using config file: {}", configFileName);
            } catch (Exception e) {
                LOGGER.warn("Cannot access config file name setting, using default");
                configFileName = "survival_cities.json";
            }
            
            Path configFile = configDir.resolve(configFileName);
            
            if (Files.exists(configFile)) {
                LOGGER.info("Loading configuration file: {}", configFileName);
                loadConfigurationFile(configFile);
            } else {
                LOGGER.warn("Configuration file not found: {} - no profile will be applied", configFileName);
            }
            
            // Apply dimension configuration
            applyDimensionConfiguration();
                    
        } catch (Exception e) {
            LOGGER.error("Failed to load autoloader configuration", e);
        }
    }
    
    private static void applyDimensionConfiguration() {
        try {
            boolean enableCustomSpawn = AutoloaderConfigSimple.ENABLE_CUSTOM_SPAWN.get();
            String playerSpawnDimension = AutoloaderConfigSimple.PLAYER_SPAWN_DIMENSION.get();
            String lostCityDimension = AutoloaderConfigSimple.LOST_CITY_DIMENSION.get();
            
            LOGGER.info("Profile Manager - Dimension configuration:");
            LOGGER.info("  Enable custom spawn: {}", enableCustomSpawn);
            LOGGER.info("  Player spawn dimension: {}", playerSpawnDimension);
            LOGGER.info("  Lost city dimension: {}", lostCityDimension);
            
            // Delegate to DimensionManager for actual implementation
            DimensionManager.applyDimensionConfiguration();
            
        } catch (Exception e) {
            LOGGER.error("Failed to apply dimension configuration", e);
        }
    }
    
    private static void loadConfigurationFile(Path configFile) {
        try {
            String content = Files.readString(configFile);
            JsonObject config = JsonParser.parseString(content).getAsJsonObject();
            
            String profileName = config.get("profile").getAsString();
            LOGGER.info("Loading autoloader configuration: {} -> {}", configFile.getFileName(), profileName);
            
            // Apply the profile to Lost Cities
            applyProfileToLostCities(profileName, config);
            
        } catch (Exception e) {
            LOGGER.error("Failed to load configuration file: {}", configFile, e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private static void applyProfileToLostCities(String profileName, JsonObject config) {
        try {
            // Check if the profile exists in Lost Cities
            Map<String, Object> standardProfiles = (Map<String, Object>) ((Field)standardProfilesField).get(null);
            
            LOGGER.info("Attempting to apply profile '{}' to Lost Cities", profileName);
            LOGGER.info("Available Lost Cities profiles: {}", standardProfiles.keySet());
            
            if (standardProfiles.containsKey(profileName)) {
                // Set the client profile - this is how Lost Cities knows what profile to use
                String currentProfile = (String) ((Field)profileFromClientField).get(null);
                LOGGER.info("Current Lost Cities profile: '{}', changing to: '{}'", currentProfile, profileName);
                
                ((Field)profileFromClientField).set(null, profileName);
                
                // Verify the change took effect
                String newProfile = (String) ((Field)profileFromClientField).get(null);
                LOGGER.info("Profile change verification: new profile is '{}'", newProfile);
                
                // If there are custom settings, we could create a custom JSON
                if (config.has("settings")) {
                    String customJson = GSON.toJson(config.get("settings"));
                    String currentJson = (String) ((Field)jsonFromClientField).get(null);
                    LOGGER.info("Current custom JSON: {}", currentJson);
                    
                    ((Field)jsonFromClientField).set(null, customJson);
                    LOGGER.info("Applied custom settings for profile: {}", profileName);
                    LOGGER.info("Custom settings JSON: {}", customJson);
                    
                    // Verify JSON change
                    String newJson = (String) ((Field)jsonFromClientField).get(null);
                    LOGGER.info("JSON change verification: new JSON is: {}", newJson);
                }
                
                // Try to refresh the Lost Cities configuration
                refreshLostCitiesConfig();
                
                // Apply dimension-specific configuration if needed
                applyDimensionSpecificConfiguration(profileName);
                
                LOGGER.info("✓ Successfully applied Lost Cities profile: '{}'", profileName);
                
            } else {
                LOGGER.warn("✗ Profile '{}' not found in Lost Cities - available profiles: {}", 
                           profileName, standardProfiles.keySet());
            }
            
        } catch (Exception e) {
            LOGGER.error("✗ Failed to apply profile to Lost Cities", e);
        }
    }
    
    private static void refreshLostCitiesConfig() {
        try {
            // Try to call resetProfileCache if it exists
            Method resetMethod = configClass.getDeclaredMethod("resetProfileCache");
            resetMethod.setAccessible(true);
            resetMethod.invoke(null);
            
            // Also try to increment the dirty counter to force cache refresh
            try {
                Class<?> featureClass = Class.forName("mcjty.lostcities.worldgen.LostCityFeature");
                Field dirtyCounterField = featureClass.getDeclaredField("globalDimensionInfoDirtyCounter");
                dirtyCounterField.setAccessible(true);
                int currentValue = dirtyCounterField.getInt(null);
                dirtyCounterField.setInt(null, currentValue + 1);
            } catch (Exception e) {
                // Not critical if this fails
                LOGGER.debug("Could not increment dirty counter", e);
            }
            
        } catch (Exception e) {
            LOGGER.debug("Could not refresh Lost Cities config", e);
        }
    }
    
    public static void applyToNewWorld() {
        // Check if config is available and enabled before proceeding
        try {
            if (!AutoloaderConfig.CONFIG.enableAutoloader.get()) {
                return;
            }
        } catch (Exception e) {
            LOGGER.debug("Config not available yet, skipping autoloader");
            return;
        }
        
        if (!lostCitiesLoaded) {
            return;
        }
        
        // This method is called when a new world/level is loaded
        // We can re-apply our configuration to ensure it takes effect
        loadAndApplyConfiguration();
    }
    
    public static boolean isLostCitiesAvailable() {
        return lostCitiesLoaded;
    }
    
    public static void listAvailableProfiles() {
        if (!lostCitiesLoaded) {
            LOGGER.info("Lost Cities not available");
            return;
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> standardProfiles = (Map<String, Object>) ((Field)standardProfilesField).get(null);
            LOGGER.info("Available Lost Cities profiles: {}", standardProfiles.keySet());
        } catch (Exception e) {
            LOGGER.error("Failed to list available profiles", e);
        }
    }
    
    private static void applyDimensionSpecificConfiguration(String profileName) {
        try {
            String lostCityDimension = DimensionManager.getLostCityDimensionConfiguration();
            
            if (!"minecraft:overworld".equals(lostCityDimension)) {
                LOGGER.info("Configuring Lost Cities profile '{}' for dimension: {}", profileName, lostCityDimension);
                
                // Try to access and modify Lost Cities dimension configuration
                try {
                    // Look for the Lost Cities config class that handles dimension profiles
                    Class<?> lostCitiesConfig = Class.forName("mcjty.lostcities.setup.Config");
                    
                    // Try to find the dimensionsWithProfiles field
                    Field dimensionsField = lostCitiesConfig.getDeclaredField("dimensionsWithProfiles");
                    dimensionsField.setAccessible(true);
                    
                    @SuppressWarnings("unchecked")
                    java.util.List<String> dimensionProfiles = (java.util.List<String>) dimensionsField.get(null);
                    
                    if (dimensionProfiles != null) {
                        // Create the dimension profile entry
                        String dimensionProfileEntry = lostCityDimension + "=" + profileName;
                        
                        // Check if this dimension is already configured
                        boolean found = false;
                        for (int i = 0; i < dimensionProfiles.size(); i++) {
                            String entry = dimensionProfiles.get(i);
                            if (entry.startsWith(lostCityDimension + "=")) {
                                // Update existing entry
                                dimensionProfiles.set(i, dimensionProfileEntry);
                                found = true;
                                LOGGER.info("Updated Lost Cities dimension profile: {}", dimensionProfileEntry);
                                break;
                            }
                        }
                        
                        if (!found) {
                            // Add new entry
                            dimensionProfiles.add(dimensionProfileEntry);
                            LOGGER.info("Added Lost Cities dimension profile: {}", dimensionProfileEntry);
                        }
                        
                        LOGGER.info("Lost Cities dimensions with profiles: {}", dimensionProfiles);
                    }
                    
                } catch (ClassNotFoundException e) {
                    LOGGER.warn("Lost Cities config class not found - dimension configuration not applied");
                } catch (NoSuchFieldException e) {
                    LOGGER.warn("Lost Cities dimensionsWithProfiles field not found - dimension configuration not applied");
                } catch (Exception e) {
                    LOGGER.warn("Failed to modify Lost Cities dimension configuration: {}", e.getMessage());
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to apply dimension-specific configuration", e);
        }
    }
}
