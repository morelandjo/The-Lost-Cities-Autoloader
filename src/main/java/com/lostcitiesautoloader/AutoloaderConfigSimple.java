package com.lostcitiesautoloader;

import net.neoforged.neoforge.common.ModConfigSpec;

public class AutoloaderConfigSimple {
    
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;
    
    // All config values in one section with descriptive names
    public static final ModConfigSpec.BooleanValue ENABLE_AUTOLOADER;
    public static final ModConfigSpec.ConfigValue<String> CONFIG_FILE_NAME;
    public static final ModConfigSpec.ConfigValue<String> LOST_CITY_DIMENSION;
    public static final ModConfigSpec.BooleanValue ENABLE_CUSTOM_SPAWN;
    public static final ModConfigSpec.ConfigValue<String> PLAYER_SPAWN_DIMENSION;
    
    static {
        BUILDER.comment("Lost Cities Autoloader Configuration");
        
        ENABLE_AUTOLOADER = BUILDER
            .comment("Enable the Lost Cities autoloader functionality")
            .define("enable_autoloader", true);
            
        CONFIG_FILE_NAME = BUILDER
            .comment("Name of the configuration file to load from config/lost_cities_autoloader/ directory (without .json extension)")
            .define("config_file_name", "survival_cities");
            
        LOST_CITY_DIMENSION = BUILDER
            .comment("Dimension where Lost Cities should generate",
                    "Use 'minecraft:overworld' for overworld, 'minecraft:the_nether' for nether, 'minecraft:the_end' for end",
                    "Or specify a custom dimension like 'lostcities:lostcity'")
            .define("lost_city_dimension", "minecraft:overworld");
            
        ENABLE_CUSTOM_SPAWN = BUILDER
            .comment("Enable custom player spawn dimension (separate from Lost Cities dimension)")
            .define("enable_custom_spawn", false);
            
        PLAYER_SPAWN_DIMENSION = BUILDER
            .comment("Dimension where players should spawn when joining the world",
                    "This can be different from the Lost Cities dimension",
                    "Only used if enable_custom_spawn is true")
            .define("player_spawn_dimension", "minecraft:overworld");
        
        SPEC = BUILDER.build();
    }
}
