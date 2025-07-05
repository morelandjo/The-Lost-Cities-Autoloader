package com.lostcitiesautoloader;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class AutoloaderConfigNew {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Configuration values - using separate sections to work around NeoForge bug
    public final ModConfigSpec.BooleanValue enableAutoloader;
    public final ModConfigSpec.ConfigValue<String> configFileName;
    public final ModConfigSpec.ConfigValue<String> lostCityDimension;
    public final ModConfigSpec.BooleanValue enableCustomSpawn;
    public final ModConfigSpec.ConfigValue<String> playerSpawnDimension;

    public static final AutoloaderConfigNew CONFIG;
    public static final ModConfigSpec SPEC;

    /**
     * Constructor that builds the configuration values using the provided builder
     * Uses separate sections to work around NeoForge TOML serialization bug
     */
    private AutoloaderConfigNew(ModConfigSpec.Builder builder) {
        LOGGER.info("=== AutoloaderConfigNew constructor START ===");
        LOGGER.info("Using split sections approach to work around NeoForge serialization bug");
        
        try {
            // === GENERAL SECTION ===
            LOGGER.info("Creating general section...");
            builder.push("general");
            builder.comment("Lost Cities Autoloader Configuration");

            enableAutoloader = builder
                    .comment("Enable the Lost Cities autoloader functionality")
                    .define("enableAutoloader", true);
            LOGGER.info("enableAutoloader defined: {} (path: {})", enableAutoloader, enableAutoloader.getPath());

            configFileName = builder
                    .comment("Name of the configuration file to load from config/lost_cities_autoloader/ directory (without .json extension)")
                    .define("configFileName", "survival_cities");
            LOGGER.info("configFileName defined: {} (path: {})", configFileName, configFileName.getPath());

            builder.pop();
            LOGGER.info("General section complete");

            // === DIMENSIONS SECTION ===
            LOGGER.info("Creating dimensions section...");
            builder.push("dimensions");
            builder.comment("Dimension Configuration Settings");
            
            lostCityDimension = builder
                    .comment("Dimension where Lost Cities should generate",
                            "Use 'minecraft:overworld' for overworld, 'minecraft:the_nether' for nether, 'minecraft:the_end' for end",
                            "Or specify a custom dimension like 'lostcities:lostcity'")
                    .define("lostCityDimension", "minecraft:overworld");
            LOGGER.info("lostCityDimension defined: {} (path: {})", lostCityDimension, lostCityDimension.getPath());

            builder.pop();
            LOGGER.info("Dimensions section complete");

            // === SPAWN SECTION ===
            LOGGER.info("Creating spawn section...");
            builder.push("spawn");
            builder.comment("Player Spawn Configuration Settings");

            enableCustomSpawn = builder
                    .comment("Enable custom player spawn dimension (separate from Lost Cities dimension)")
                    .define("enableCustomSpawn", false);
            LOGGER.info("enableCustomSpawn defined: {} (path: {})", enableCustomSpawn, enableCustomSpawn.getPath());

            playerSpawnDimension = builder
                    .comment("Dimension where players should spawn when joining the world",
                            "This can be different from the Lost Cities dimension",
                            "Only used if enableCustomSpawn is true")
                    .define("playerSpawnDimension", "minecraft:overworld");
            LOGGER.info("playerSpawnDimension defined: {} (path: {})", playerSpawnDimension, playerSpawnDimension.getPath());

            builder.pop();
            LOGGER.info("Spawn section complete");
            
        } catch (Exception e) {
            LOGGER.error("ERROR in AutoloaderConfigNew constructor", e);
            throw e;
        }
        
        LOGGER.info("=== AutoloaderConfigNew constructor COMPLETE ===");
    }

    // Static block that creates both the CONFIG and SPEC using the configure method
    static {
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        ClassLoader classLoader = AutoloaderConfigNew.class.getClassLoader();
        
        LOGGER.info("=== AutoloaderConfigNew static block START ===");
        LOGGER.info("Time: {}", startTime);
        LOGGER.info("Thread: {}", threadName);
        LOGGER.info("ClassLoader: {}", classLoader);
        
        try {
            LOGGER.info("Creating ModConfigSpec.Builder...");
            ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
            LOGGER.info("Builder created: {}", builder);
            
            LOGGER.info("Calling builder.configure(AutoloaderConfigNew::new)...");
            // Use the configure method to create both the config instance and the spec
            Pair<AutoloaderConfigNew, ModConfigSpec> pair = builder.configure(AutoloaderConfigNew::new);
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
        LOGGER.info("=== AutoloaderConfigNew static block COMPLETE ===");
        LOGGER.info("Duration: {} ms", endTime - startTime);
    }
}
