
# Lost Cities Autoloader

A Minecraft mod that automatically applies Lost Cities profiles to new worlds without requiring manual configuration in the world creation screen.

Normally to get Lost Cities content in a new world you have to go through a separate screen with many different settings. This mod is ideal for mod packs that want to have Lost Cities in the world by default without the player setting them up.

## Requirements

### Supported Versions

| Minecraft | Loader | Java |
|-----------|--------|------|
| 1.21.1 | NeoForge 21.1.186+ | 21 |
| 1.20.1 | Forge 47+ | 17 |
| 1.18.2 | Forge 40+ | 17 |

Requirements:

- One of the Minecraft/loader combinations above
- [The Lost Cities](https://www.curseforge.com/minecraft/mc-mods/the-lost-cities) mod (required on every supported version)


## How It Works

This mod automatically detects when The Lost Cities mod is present and applies configured profiles to all new worlds. Instead of having to manually select Lost Cities settings every time you create a new world, the autoloader reads configuration files and applies them automatically.

## Configuration

### Basic Setup

1. Start Minecraft once with the mod installed
2. Navigate to `/config/lost_cities_autoloader/` directory
3. Create or copy a JSON configuration file (see example below)
4. Change the config to specify the filename you want to autoload

### Configuration Selection

The mod uses a specific configuration file based on the `configFileName` setting in the mod's config. By default, it looks for `survival_cities.json`.

### Configuration Files

Create JSON files in the `config/lost_cities_autoloader/` directory. The mod will load the file specified in its configuration. 

**Example:** To use `config/lost_cities_autoloader/ruins.json`, set `configFileName` to `"ruins"` in the config.

Example:
```json
{
    "profile": "tallbuildings",
    "description": "Massive cities",
    "settings": {
        "cityChance": 0.15,
        "cityRadius": 200,
        "generateLighting": true,
        "generateSpawners": false,
        "ruinChance": 0.0,
        "vineChance": 0.0
    }
}
```


### Available Profiles

The `profile` field should match one of the built-in Lost Cities profiles:

- `default` - Standard Lost Cities generation
- `nodamage` - Like default but no explosion damage
- `floating` - Cities on floating islands  
- `space` - Cities in floating glass bubbles
- `cavern` - Cities in dark caverns
- `biosphere` - Jungles in glass bubbles on barren landscape
- `ancient` - Ancient jungle cities with vines and ruins
- `wasteland` - Wasteland with no water
- `atlantis` - Drowned cities with raised water level
- `safe` - No spawners, lighting but no loot
- `tallbuildings` - Very tall buildings
- `rarecities` - Cities are rare
- `onlycities` - The entire world is a city

### Custom Settings (Optional)

You can override specific settings by including a `settings` object. Common settings include:

- `cityChance` - Probability of city generation (0.0 to 1.0)
- `cityRadius` - Maximum radius of cities in blocks
- `generateLighting` - Whether to add lighting to buildings (true/false)
- `generateSpawners` - Whether to add monster spawners (true/false) 
- `generateLoot` - Whether to add loot chests (true/false)
- `ruinChance` - Probability of ruins/damage (0.0 to 1.0)
- `vineChance` - Probability of vines on buildings (0.0 to 1.0)



## Mod Configuration

- `enableAutoloader` - Enable/disable the autoloader functionality (default: true)
- `configFileName` - Name of the JSON configuration file to load (default: "survival_cities")

### Dimension Configuration
- `lostCityDimension` - Dimension where Lost Cities should generate (default: "minecraft:overworld")
  - Use `"minecraft:overworld"` for overworld
  - Use `"minecraft:the_nether"` for nether
  - Use `"minecraft:the_end"` for end  
  - Use custom dimension IDs like `"lostcities:lostcity"` for mod dimensions
  - Any dimension in the world works: Lost Cities is added to its biomes automatically, and it uses the profile from your JSON file (or `default` if there is none)
  - When this is not the overworld, the profile is not applied to the overworld, so it gets no cities. This also clears any profile stored with an existing world
  - `config/lostcities/common.toml` is not changed
- `enableCustomSpawn` - Enable custom player spawn dimension (default: false)
- `playerSpawnDimension` - Dimension where players spawn (default: "minecraft:overworld")

## Note

### No Effect on Existing Worlds
- The autoloader only affects newly created worlds
- Existing worlds keep their original generation settings

### Dimension Requirements
- This is a fun little addition allowing you to start a world right in a specific dimension, such as Nether with cities.
- Target dimensions must exist (provided by mods or vanilla)
- A missing or invalid target dimension is logged as an error and gets no cities
- Cities replace the target dimension's own terrain where they generate, so how they look depends on that dimension's generator

## Building

The project uses [Stonecutter](https://stonecutter.kikugie.dev/) to build all supported versions from one source tree. You need JDK 21 to run Gradle; the Java 17 and 21 toolchains for each version are downloaded automatically.

```
./gradlew buildAll
```

Jars are written to `versions/<node>/build/libs/lostcitiesautoloader-<mod version>-<minecraft version>.jar`, where `<node>` is `1.21.1-neoforge`, `1.20.1-forge` or `1.18.2-forge`. Jars in `devlibs` are development-only; use the ones directly in `libs`.

To build a single version, run e.g. `./gradlew :1.20.1-forge:build`.

## Version-Specific Code

All versions share `src/main`. Code that differs between Minecraft versions or loaders is wrapped in Stonecutter comment guards (for example `//? if forge {` ... `//?}` or version-range conditions), and Stonecutter enables or disables the matching blocks for each node when it builds. The `forge` and `neoforge` constants are derived from the node name.

The active version (`.sc_active_version`, currently `1.21.1-neoforge`) is the one whose code is shown uncommented in `src/main` for your IDE. Switching it rewrites source files in `src/main`, so only do it deliberately and do not commit the rewritten sources or a changed `.sc_active_version` by accident; switch back to `1.21.1-neoforge` before committing.
