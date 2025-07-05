
# Lost Cities Autoloader

A NeoForge mod for Minecraft 1.21.1 that automatically applies Lost Cities profiles to new worlds without requiring manual configuration in the world creation screen.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.186+
- [The Lost Cities](https://www.curseforge.com/minecraft/mc-mods/the-lost-cities) mod


## How It Works

This mod automatically detects when The Lost Cities mod is present and applies configured profiles to all new worlds. Instead of having to manually select Lost Cities settings every time you create a new world, the autoloader reads configuration files and applies them automatically.

## Configuration

### Basic Setup

1. Start Minecraft once with the mod installed
2. Navigate to `/config/lost_cities_autoloader/` directory
3. Create or copy a JSON configuration file (see example below)
4. Change the config to specify the filename you want to autoload

### Configuration Selection

The mod uses a specific configuration file based on the `configFileName` setting in the mod's config. By default, it looks for `survival_cities.json`. You can change this in the NeoForge config system or by editing the config file directly.

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
- `enableCustomSpawn` - Enable custom player spawn dimension (default: false)
- `playerSpawnDimension` - Dimension where players spawn (default: "minecraft:overworld")

## Note

### No Effect on Existing Worlds
- The autoloader only affects newly created worlds
- Existing worlds keep their original generation settings

### Dimension Requirements
- Target dimensions must exist (provided by mods or vanilla)
- Lost Cities must support the target dimension type
- Custom dimensions may require additional configuration






