package com.lostcitiesautoloader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
//? neoforge {
import net.neoforged.fml.ModList;
//?} forge {
/*import net.minecraftforge.fml.ModList;
*///?}
//? >1.20.1 {
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
//?}
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manages the automatic profile loading and application for Lost Cities
 */
public class AutoloaderProfileManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean lostCitiesLoaded = false;
    private static Class<?> configClass = null;
    private static Object profileFromClientField = null;
    private static Object jsonFromClientField = null;
    private static Class<?> profileSetupClass = null;
    private static Object standardProfilesField = null;
    // The profile the lost_city_dimension target should use; "default" unless the JSON config names another
    private static String targetProfile = "default";
    
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
            targetProfile = "default";
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
            applyDimensionProfiles();
            
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
                // Determine whether the config provides any custom setting overrides.
                JsonObject settings = (config.has("settings") && config.get("settings").isJsonObject())
                        ? config.getAsJsonObject("settings") : null;
                boolean hasSettings = settings != null && settings.size() > 0;

                // The profile name we hand to Lost Cities. With no overrides this is the base
                // profile. With overrides we must use Lost Cities' special "customized" profile -
                // that is the ONLY profile name through which custom settings are honoured. Lost
                // Cities builds the "customized" profile from the JSON we provide and maps the
                // dimension to it; pointing the dimension at the base profile name leaves any
                // custom JSON parsed-but-orphaned (which is why settings like generateLighting
                // previously had no effect).
                String effectiveProfile = profileName;
                String customJson = "";

                if (hasSettings) {
                    // Build the FULL, category-nested profile JSON (base profile serialised with
                    // toJson) and merge the user's overrides into it. Lost Cities reads each
                    // setting from its specific category, so a flat {"generateLighting":false}
                    // blob is never matched - the value has to live under the category that
                    // declares it (e.g. "lostcity"). Starting from the base profile's own JSON
                    // also preserves every setting the user did not override.
                    String built = buildCustomizedJson(standardProfiles, profileName, settings);
                    if (built != null) {
                        effectiveProfile = "customized";
                        customJson = built;
                    } else {
                        LOGGER.warn("Falling back to base profile '{}' without custom settings", profileName);
                    }
                }

                targetProfile = effectiveProfile;

                if (DimensionManager.isOverworldTarget()) {
                    String currentProfile = (String) ((Field)profileFromClientField).get(null);
                    LOGGER.info("Current Lost Cities profile: '{}', changing to: '{}'", currentProfile, effectiveProfile);

                    ((Field)profileFromClientField).set(null, effectiveProfile);
                    ((Field)jsonFromClientField).set(null, customJson);

                    LOGGER.info("Profile change verification: new profile is '{}'",
                            (String) ((Field)profileFromClientField).get(null));
                } else if (!customJson.isEmpty()) {
                    registerCustomizedProfile(standardProfiles, customJson);
                }
                if (hasSettings && "customized".equals(effectiveProfile)) {
                    LOGGER.info("Applied custom settings for base profile '{}' via 'customized' profile", profileName);
                    LOGGER.info("Customized profile JSON: {}", customJson);
                }

                LOGGER.info("✓ Successfully applied Lost Cities profile: '{}'", effectiveProfile);

            } else {
                LOGGER.warn("✗ Profile '{}' not found in Lost Cities - available profiles: {}", 
                           profileName, standardProfiles.keySet());
            }
            
        } catch (Exception e) {
            LOGGER.error("✗ Failed to apply profile to Lost Cities", e);
        }
    }
    
    /**
     * Builds the full Lost Cities profile JSON for the "customized" profile by taking the base
     * profile's own serialised settings and overlaying the user's overrides.
     *
     * <p>Lost Cities serialises a profile as a category-nested object
     * ({@code {"lostcity":{"generateLighting":...}, "explosions":{...}, ...}}) and reads each
     * setting back from the category that declares it. The autoloader config lists overrides as a
     * flat map, so each override is matched to whichever category already contains that key. Keys
     * with no matching option are reported and skipped rather than silently dropped.
     *
     * @return the merged JSON string, or {@code null} if the base profile could not be serialised.
     */
    private static String buildCustomizedJson(Map<String, Object> standardProfiles, String baseProfileName,
                                              JsonObject userSettings) {
        try {
            Object baseProfile = standardProfiles.get(baseProfileName);
            if (baseProfile == null) {
                LOGGER.warn("Base profile '{}' not found - cannot build customized settings", baseProfileName);
                return null;
            }

            // Serialise the base profile to its full, category-nested JSON. The boolean argument is
            // Lost Cities' "isCustom" flag; false mirrors what the in-game config GUI passes.
            Method toJson = baseProfile.getClass().getMethod("toJson", boolean.class);
            Object result = toJson.invoke(baseProfile, false);
            if (!(result instanceof JsonObject)) {
                LOGGER.warn("Unexpected toJson result for profile '{}': {}", baseProfileName, result);
                return null;
            }
            JsonObject fullJson = (JsonObject) result;

            for (Map.Entry<String, JsonElement> entry : userSettings.entrySet()) {
                String key = entry.getKey();
                boolean applied = false;
                for (Map.Entry<String, JsonElement> category : fullJson.entrySet()) {
                    if (!category.getValue().isJsonObject()) {
                        continue;
                    }
                    JsonObject categoryObj = category.getValue().getAsJsonObject();
                    if (categoryObj.has(key)) {
                        categoryObj.add(key, entry.getValue());
                        applied = true;
                        LOGGER.info("Applied custom setting '{}' = {} (category '{}')",
                                key, entry.getValue(), category.getKey());
                        break;
                    }
                }
                if (!applied) {
                    LOGGER.warn("Ignoring unknown Lost Cities setting '{}' - no such option in profile '{}'",
                            key, baseProfileName);
                }
            }

            return fullJson.toString();
        } catch (Exception e) {
            LOGGER.error("Failed to build customized profile JSON for base profile '{}'", baseProfileName, e);
            return null;
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
    
    /**
     * Makes the "customized" profile available by name. Lost Cities only creates it for the profile
     * selected for the overworld, and rejects list entries naming a profile that does not exist yet.
     */
    private static void registerCustomizedProfile(Map<String, Object> standardProfiles, String json) throws Exception {
        Class<?> profileClass = Class.forName("mcjty.lostcities.config.LostCityProfile");
        if (!standardProfiles.containsKey("customized")) {
            standardProfiles.put("customized",
                    profileClass.getConstructor(String.class, boolean.class).newInstance("customized", false));
        }
        Object parsed = profileClass.getConstructor(String.class, String.class).newInstance("customized", json);
        profileClass.getMethod("copyFrom", profileClass).invoke(standardProfiles.get("customized"), parsed);
    }

    /**
     * Maps the target dimension to its profile in Lost Cities' dimensionsWithProfiles value, in memory
     * only: Lost Cities rebuilds its profile map from that value whenever its caches are cleaned, so
     * entries added to the map itself do not last, and common.toml stays the user's. Other entries are kept.
     * A non-overworld target also takes the profile away from the overworld.
     */
    private static void applyDimensionProfiles() {
        if (DimensionManager.getLostCityDimensionKey() == null) {
            return; // Invalid ID, already logged. Lost Cities would throw parsing it, so never hand it over.
        }
        try {
            String target = DimensionManager.getLostCityDimensionConfiguration();
            boolean overworldTarget = DimensionManager.isOverworldTarget();

            Field profilesField = configClass.getDeclaredField("DIMENSION_PROFILES");
            profilesField.setAccessible(true);
            Object profilesValue = profilesField.get(null);
            // Drop any earlier override so the entries below are the ones in common.toml
            profilesValue.getClass().getMethod("clearCache").invoke(profilesValue);
            List<?> configured = (List<?>) profilesValue.getClass().getMethod("get").invoke(profilesValue);

            List<String> merged = new ArrayList<>();
            for (Object entry : configured) {
                String dimension = entry.toString().split("=", 2)[0];
                if (!overworldTarget && dimension.equals("minecraft:overworld")) {
                    LOGGER.info("Ignoring Lost Cities entry '{}': lost_city_dimension is {}", entry, target);
                } else if (!dimension.equals(target)) {
                    merged.add(entry.toString());
                }
            }
            // For the overworld, the JSON's profile goes through Lost Cities' own selection (profileFromClient)
            merged.add(target + "=" + (overworldTarget ? "default" : targetProfile));

            Field cachedValue = profilesValue.getClass().getDeclaredField("cachedValue");
            cachedValue.setAccessible(true);
            cachedValue.set(profilesValue, merged);
            LOGGER.info("Lost Cities dimensions with profiles: {}", merged);

            if (!overworldTarget) {
                ((Field)profileFromClientField).set(null, null);
                ((Field)jsonFromClientField).set(null, null);
                clearStoredOverworldSelection();
            }
            refreshLostCitiesConfig();
        } catch (Exception e) {
            LOGGER.error("Failed to configure Lost Cities for dimension {}", DimensionManager.getLostCityDimensionConfiguration(), e);
        }
    }

    /**
     * A selection stored with the world would give the overworld a profile again and overwrite the
     * "customized" profile with its own JSON, so it is cleared while another dimension is the target.
     */
    private static void clearStoredOverworldSelection() throws Exception {
        //? >1.20.1 {
        // Stored in the world's lostcities_data, reachable from the first level load onwards (retried on each).
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerLevel overworld = server == null ? null : server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return;
        }
        Class<?> lostDataClass = Class.forName("mcjty.lostcities.data.LostData");
        Object data = lostDataClass.getMethod("getData", Level.class).invoke(null, overworld);
        if (!"".equals(lostDataClass.getMethod("getSelectedProfile").invoke(data))) {
            LOGGER.info("Clearing the Lost Cities overworld profile stored with this world");
            lostDataClass.getMethod("setProfile", String.class, String.class).invoke(data, "", "");
        }
        //?} <=1.20.1 {
        /*// Stored in the world's server config, which is loaded before ServerAboutToStartEvent.
        Object selected = configClass.getField("SELECTED_PROFILE").get(null);
        Method get = selected.getClass().getMethod("get");
        Method set = selected.getClass().getMethod("set", Object.class);
        Object current = get.invoke(selected);
        if (!"".equals(current) && !"<CHECK>".equals(current)) {
            LOGGER.info("Clearing the Lost Cities overworld profile stored with this world");
            set.invoke(selected, "");
            set.invoke(configClass.getField("SELECTED_CUSTOM_JSON").get(null), "");
        }
        *///?}
    }
}
