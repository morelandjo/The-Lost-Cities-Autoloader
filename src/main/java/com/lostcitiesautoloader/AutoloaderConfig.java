package com.lostcitiesautoloader;

import com.mojang.logging.LogUtils;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

/**
 * Configuration for the Lost Cities Autoloader mod
 * 
 * This class follows the NeoForge configuration pattern using ModConfigSpec.Builder#configure()
 * as documented at: https://docs.neoforged.net/docs/misc/config/
 */
public class AutoloaderConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Config values - these will be populated by the builder
    public final ModConfigSpec.BooleanValue enableAutoloader;
    public final ModConfigSpec.ConfigValue<String> configFileName;
    public final ModConfigSpec.ConfigValue<String> lostCityDimension;
    public final ModConfigSpec.BooleanValue enableCustomSpawn;
    public final ModConfigSpec.ConfigValue<String> playerSpawnDimension;

    // Static instances - CONFIG holds the config values, SPEC holds the specification
    public static final AutoloaderConfig CONFIG;
    public static final ModConfigSpec SPEC;

    /**
     * Constructor that builds the configuration values using the provided builder
     */
    private AutoloaderConfig(ModConfigSpec.Builder builder) {
        LOGGER.info("=== AutoloaderConfig constructor START ===");
        LOGGER.info("Builder: {}", builder);
        LOGGER.info("Builder class: {}", builder.getClass());
        LOGGER.info("Builder hashCode: {}", System.identityHashCode(builder));
        
        try {
            // Push the "general" section FIRST, then add comment inside
            LOGGER.info("STEP 1: Pushing 'general' section...");
            builder.push("general");
            LOGGER.info("Builder state after push: {}", builder.toString());
            
            // Add section comment AFTER pushing the section
            LOGGER.info("STEP 2: Adding section comment...");
            builder.comment("Lost Cities Autoloader Configuration");
            LOGGER.info("Builder state after comment: {}", builder.toString());

            // Define general settings
            LOGGER.info("STEP 3: Defining enableAutoloader...");
            LOGGER.info("Builder before enableAutoloader: hashCode={}", System.identityHashCode(builder));
            ModConfigSpec.Builder tempBuilder1 = builder.comment("Enable the Lost Cities autoloader functionality");
            LOGGER.info("Builder after comment: hashCode={}", System.identityHashCode(tempBuilder1));
            enableAutoloader = tempBuilder1.define("enableAutoloader", true);
            LOGGER.info("enableAutoloader defined: {}", enableAutoloader);
            LOGGER.info("enableAutoloader class: {}", enableAutoloader.getClass());
            LOGGER.info("enableAutoloader path: {}", enableAutoloader.getPath());
            LOGGER.info("Builder state after enableAutoloader: {}", builder.toString());

            LOGGER.info("STEP 4: Defining configFileName...");
            LOGGER.info("Builder before configFileName: hashCode={}", System.identityHashCode(builder));
            ModConfigSpec.Builder tempBuilder2 = builder.comment("Name of the configuration file to load from config/lost_cities_autoloader/ directory (without .json extension)");
            LOGGER.info("Builder after comment: hashCode={}", System.identityHashCode(tempBuilder2));
            configFileName = tempBuilder2.define("configFileName", "survival_cities");
            LOGGER.info("configFileName defined: {}", configFileName);
            LOGGER.info("configFileName class: {}", configFileName.getClass());
            LOGGER.info("configFileName path: {}", configFileName.getPath());
            LOGGER.info("Builder state after configFileName: {}", builder.toString());

            // Add dimension settings
            LOGGER.info("STEP 5: Adding dimension comment...");
            builder.comment("Dimension Configuration Settings");
            LOGGER.info("Builder state after dimension comment: {}", builder.toString());
            
            LOGGER.info("STEP 6: Defining lostCityDimension...");
            LOGGER.info("Builder before lostCityDimension: hashCode={}", System.identityHashCode(builder));
            ModConfigSpec.Builder tempBuilder3 = builder.comment("Dimension where Lost Cities should generate",
                            "Use 'minecraft:overworld' for overworld, 'minecraft:the_nether' for nether, 'minecraft:the_end' for end",
                            "Or specify a custom dimension like 'lostcities:lostcity'");
            LOGGER.info("Builder after comment: hashCode={}", System.identityHashCode(tempBuilder3));
            lostCityDimension = tempBuilder3.define("lostCityDimension", "minecraft:overworld");
            LOGGER.info("lostCityDimension defined: {}", lostCityDimension);
            LOGGER.info("lostCityDimension class: {}", lostCityDimension.getClass());
            LOGGER.info("lostCityDimension path: {}", lostCityDimension.getPath());
            LOGGER.info("Builder state after lostCityDimension: {}", builder.toString());

            LOGGER.info("STEP 7: Defining enableCustomSpawn...");
            LOGGER.info("Builder before enableCustomSpawn: hashCode={}", System.identityHashCode(builder));
            ModConfigSpec.Builder tempBuilder4 = builder.comment("Enable custom player spawn dimension (separate from Lost Cities dimension)");
            LOGGER.info("Builder after comment: hashCode={}", System.identityHashCode(tempBuilder4));
            enableCustomSpawn = tempBuilder4.define("enableCustomSpawn", false);
            LOGGER.info("enableCustomSpawn defined: {}", enableCustomSpawn);
            LOGGER.info("enableCustomSpawn class: {}", enableCustomSpawn.getClass());
            LOGGER.info("enableCustomSpawn path: {}", enableCustomSpawn.getPath());
            LOGGER.info("Builder state after enableCustomSpawn: {}", builder.toString());

            LOGGER.info("STEP 8: Defining playerSpawnDimension...");
            LOGGER.info("Builder before playerSpawnDimension: hashCode={}", System.identityHashCode(builder));
            ModConfigSpec.Builder tempBuilder5 = builder.comment("Dimension where players should spawn when joining the world",
                            "This can be different from the Lost Cities dimension",
                            "Only used if enableCustomSpawn is true");
            LOGGER.info("Builder after comment: hashCode={}", System.identityHashCode(tempBuilder5));
            playerSpawnDimension = tempBuilder5.define("playerSpawnDimension", "minecraft:overworld");
            LOGGER.info("playerSpawnDimension defined: {}", playerSpawnDimension);
            LOGGER.info("playerSpawnDimension class: {}", playerSpawnDimension.getClass());
            LOGGER.info("playerSpawnDimension path: {}", playerSpawnDimension.getPath());
            LOGGER.info("Builder state after playerSpawnDimension: {}", builder.toString());

            LOGGER.info("STEP 9: Popping 'general' section...");
            builder.pop(); // Exit the "general" section
            LOGGER.info("Builder state after pop: {}", builder.toString());
            
        } catch (Exception e) {
            LOGGER.error("ERROR in AutoloaderConfig constructor", e);
            LOGGER.error("Exception occurred at step: checking builder state...");
            LOGGER.error("Builder state during error: {}", builder.toString());
            throw e;
        }
        
        LOGGER.info("=== AutoloaderConfig constructor COMPLETE ===");
        LOGGER.info("Final constructor state - Builder: {}", builder.toString());
    }

    // Static block that creates both the CONFIG and SPEC using the configure method
    static {
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        ClassLoader classLoader = AutoloaderConfig.class.getClassLoader();
        
        LOGGER.info("=== AutoloaderConfig static block START ===");
        LOGGER.info("Time: {}", startTime);
        LOGGER.info("Thread: {}", threadName);
        LOGGER.info("ClassLoader: {}", classLoader);
        LOGGER.info("Stack trace:");
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 0; i < Math.min(15, stackTrace.length); i++) {
            LOGGER.info("  [{}] {}", i, stackTrace[i]);
        }
        
        try {
            LOGGER.info("Creating ModConfigSpec.Builder...");
            ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
            LOGGER.info("Builder created: {}", builder);
            
            LOGGER.info("Calling builder.configure(AutoloaderConfig::new)...");
            // Use the configure method to create both the config instance and the spec
            Pair<AutoloaderConfig, ModConfigSpec> pair = builder.configure(AutoloaderConfig::new);
            LOGGER.info("Configure method returned pair: {}", pair);
            
            // Store the resulting values
            CONFIG = pair.getLeft();
            SPEC = pair.getRight();
            
            LOGGER.info("CONFIG instance: {}", CONFIG);
            LOGGER.info("SPEC instance: {}", SPEC);
            
            int valueCount = SPEC.getValues().size();
            LOGGER.info("Config spec contains {} value definitions", valueCount);
            
            // Debug: Let's see what values are actually in the spec
            LOGGER.info("Values in spec:");
            SPEC.getValues().entrySet().forEach(entry -> {
                LOGGER.info("  {} -> {}", entry.getKey(), entry.getValue());
            });
            
            // Test field access
            LOGGER.info("Testing field access:");
            LOGGER.info("  CONFIG.enableAutoloader: {}", CONFIG.enableAutoloader);
            LOGGER.info("  CONFIG.configFileName: {}", CONFIG.configFileName);
            LOGGER.info("  CONFIG.lostCityDimension: {}", CONFIG.lostCityDimension);
            LOGGER.info("  CONFIG.enableCustomSpawn: {}", CONFIG.enableCustomSpawn);
            LOGGER.info("  CONFIG.playerSpawnDimension: {}", CONFIG.playerSpawnDimension);
            
        } catch (Exception e) {
            LOGGER.error("FATAL ERROR during static initialization", e);
            throw new RuntimeException("Config initialization failed", e);
        }
        
        long endTime = System.currentTimeMillis();
        LOGGER.info("=== AutoloaderConfig static block COMPLETE ===");
        LOGGER.info("Duration: {} ms", endTime - startTime);
    }
}
