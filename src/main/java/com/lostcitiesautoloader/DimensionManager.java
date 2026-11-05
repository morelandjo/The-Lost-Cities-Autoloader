package com.lostcitiesautoloader;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
//? >1.18.2 {
import net.minecraft.core.registries.Registries;
//?} <=1.18.2 {
/*import net.minecraft.core.Registry;
*///?}
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
//? neoforge {
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
//?} forge {
/*import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
*///?}
import org.slf4j.Logger;

/**
 * Manages custom player spawning and dimension configuration
 */
//? neoforge {
@EventBusSubscriber(modid = LostCitiesAutoloaderMod.MODID)
//?} forge {
/*@Mod.EventBusSubscriber(modid = LostCitiesAutoloaderMod.MODID)
*///?}
public class DimensionManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CUSTOM_SPAWN_TAG = "lostcitiesautoloader_custom_spawned";
    /** How far from the shared spawn, in blocks along X and Z, to look for a safe spot */
    private static final int SAFE_SPAWN_SEARCH_RADIUS = 24;
    /** How far above and below the shared spawn Y to look before accepting any height in the column */
    private static final int SAFE_SPAWN_PREFERRED_Y_RANGE = 8;
    private static String reportedInvalidDimension = null;
    
    /**
     * Apply dimension configuration to Lost Cities
     */
    public static void applyDimensionConfiguration() {
        try {
            boolean enableCustomSpawn = AutoloaderConfigSimple.ENABLE_CUSTOM_SPAWN.get();
            String playerSpawnDimension = AutoloaderConfigSimple.PLAYER_SPAWN_DIMENSION.get();
            String lostCityDimension = AutoloaderConfigSimple.LOST_CITY_DIMENSION.get();
            
            LOGGER.info("Applying dimension configuration:");
            LOGGER.info("  Custom spawn enabled: {}", enableCustomSpawn);
            LOGGER.info("  Player spawn dimension: {}", playerSpawnDimension);
            LOGGER.info("  Lost city dimension: {}", lostCityDimension);
            
        } catch (Exception e) {
            LOGGER.error("Failed to apply dimension configuration", e);
        }
    }
    
    /**
     * Report a lost_city_dimension target that no level exists for. Call once the levels are created.
     */
    public static void verifyLostCityDimension(MinecraftServer server) {
        String target = AutoloaderConfigSimple.LOST_CITY_DIMENSION.get();
        ResourceKey<Level> key = getLostCityDimensionKey();
        if (key == null) {
            return; // Already reported by getLostCityDimensionKey
        }
        if (server.getLevel(key) == null) {
            LOGGER.error("Lost Cities target dimension '{}' does not exist in this world - no cities will generate there", target);
        }
    }
    
    /**
     * Handle player login to potentially teleport to custom spawn dimension (only on first spawn)
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        try {
            boolean enableCustomSpawn = AutoloaderConfigSimple.ENABLE_CUSTOM_SPAWN.get();
            
            if (!enableCustomSpawn) {
                LOGGER.debug("Custom spawn disabled, player will spawn in default dimension");
                return;
            }
            
            if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
                return;
            }
            
            // Check if player has already been teleported to custom spawn
            CompoundTag persistentData = serverPlayer.getPersistentData();
            // Tag lives under PERSISTED_NBT_TAG so it survives respawn; root-level tag is the legacy location
            CompoundTag persisted = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
            if (persisted.getBoolean(CUSTOM_SPAWN_TAG) || persistentData.getBoolean(CUSTOM_SPAWN_TAG)) {
                LOGGER.debug("Player {} has already been teleported to custom spawn, skipping", 
                           serverPlayer.getName().getString());
                return;
            }
            
            String playerSpawnDimension = AutoloaderConfigSimple.PLAYER_SPAWN_DIMENSION.get();
            String playerSpawnCoordinates = AutoloaderConfigSimple.PLAYER_SPAWN_COORDINATES.get();
            String playerSpawnFacing = AutoloaderConfigSimple.PLAYER_SPAWN_FACING.get();
            
            LOGGER.info("Custom spawn enabled - target dimension: {}, coordinates: '{}', facing: '{}'", 
                       playerSpawnDimension, playerSpawnCoordinates, playerSpawnFacing);
            
            // Teleport player to custom dimension with custom coordinates and facing
            boolean teleported = teleportPlayerToDimension(serverPlayer, playerSpawnDimension, playerSpawnCoordinates, playerSpawnFacing);
            
            if (teleported) {
                // Mark player as having been teleported to custom spawn
                persisted.putBoolean(CUSTOM_SPAWN_TAG, true);
                persistentData.put(Player.PERSISTED_NBT_TAG, persisted);
                LOGGER.debug("Marked player {} as having been teleported to custom spawn", 
                           serverPlayer.getName().getString());
            } else {
                LOGGER.warn("Custom spawn teleport failed for player {}, will be retried on next login",
                           serverPlayer.getName().getString());
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to handle custom player spawn", e);
        }
    }
    
    /**
     * Teleport a player to the specified dimension with custom coordinates and facing
     */
    private static boolean teleportPlayerToDimension(ServerPlayer player, String dimensionName, String coordinates, String facing) {
        try {
            //? >1.20.1 {
            ResourceLocation dimensionLocation = ResourceLocation.parse(dimensionName);
            //?} <=1.20.1 {
            /*ResourceLocation dimensionLocation = new ResourceLocation(dimensionName);
            *///?}
            //? >1.18.2 {
            ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionLocation);
            //?} <=1.18.2 {
            /*ResourceKey<Level> dimensionKey = ResourceKey.create(Registry.DIMENSION_REGISTRY, dimensionLocation);
            *///?}
            
            ServerLevel targetLevel = player.getServer().getLevel(dimensionKey);
            
            if (targetLevel == null) {
                LOGGER.error("Target dimension does not exist: {}", dimensionName);
                return false;
            }
            
            //? >1.18.2 {
            ResourceKey<Level> currentDimension = player.level().dimension();
            //?} <=1.18.2 {
            /*ResourceKey<Level> currentDimension = player.getLevel().dimension();
            *///?}
            
            // Parse coordinates - use a safe spot near the default spawn if empty or invalid
            double x = 0, y = 0, z = 0;
            boolean useCustomCoordinates = false;
            
            if (coordinates != null && !coordinates.trim().isEmpty()) {
                try {
                    String[] parts = coordinates.split(",");
                    if (parts.length != 3) {
                        throw new IllegalArgumentException("Coordinates must be in format 'x,y,z'");
                    }
                    x = Double.parseDouble(parts[0].trim());
                    y = Double.parseDouble(parts[1].trim());
                    z = Double.parseDouble(parts[2].trim());
                    useCustomCoordinates = true;
                    LOGGER.debug("Using custom coordinates: ({}, {}, {})", x, y, z);
                } catch (Exception e) {
                    LOGGER.warn("Invalid coordinates format '{}', using default spawn", coordinates);
                }
            } else {
                LOGGER.debug("No custom coordinates specified, using default spawn");
            }
            
            if (!useCustomCoordinates) {
                if (currentDimension.equals(dimensionKey)) {
                    LOGGER.debug("Player is already in target dimension {} and no coordinates are set, not teleporting", dimensionName);
                    return true;
                }
                
                BlockPos safeSpawn = findSafeSpawnPos(targetLevel);
                if (safeSpawn == null) {
                    LOGGER.warn("No safe spawn found within {} blocks of {} in dimension {}, not teleporting",
                               SAFE_SPAWN_SEARCH_RADIUS, targetLevel.getSharedSpawnPos(), dimensionName);
                    return false;
                }
                x = safeSpawn.getX() + 0.5;
                y = safeSpawn.getY();
                z = safeSpawn.getZ() + 0.5;
            }
            
            // Parse facing - use default if empty or invalid
            float yRot = player.getYRot(); // Keep current facing as default
            boolean useCustomFacing = false;
            
            if (facing != null && !facing.trim().isEmpty()) {
                try {
                    double facingDegrees = Double.parseDouble(facing.trim());
                    // Convert facing to Minecraft's rotation system (0=South, 90=West, 180=North, 270=East)
                    yRot = (float) (facingDegrees - 180.0f);
                    if (yRot < 0) yRot += 360;
                    useCustomFacing = true;
                    LOGGER.debug("Using custom facing: {} degrees", facingDegrees);
                } catch (Exception e) {
                    LOGGER.warn("Invalid facing format '{}', using default facing", facing);
                }
            } else {
                LOGGER.debug("No custom facing specified, using default facing");
            }
            
            // Only teleport if player is not already in the target dimension or at the correct position
            if (currentDimension.equals(dimensionKey) && useCustomCoordinates &&
                Math.abs(player.getX() - x) < 1.0 && 
                Math.abs(player.getY() - y) < 1.0 && 
                Math.abs(player.getZ() - z) < 1.0) {
                LOGGER.debug("Player is already at target location in dimension: {}", dimensionName);
                return true;
            }
            
            String locationDesc = useCustomCoordinates ? 
                String.format("at coordinates (%.1f, %.1f, %.1f)", x, y, z) : 
                String.format("at safe spot (%.1f, %.1f, %.1f) near default spawn", x, y, z);
            String facingDesc = useCustomFacing ? 
                String.format("facing %.1f degrees", Double.parseDouble(facing)) : 
                "with default facing";
                
            LOGGER.info("Teleporting player {} to dimension: {} {} {}", 
                       player.getName().getString(), dimensionName, locationDesc, facingDesc);
            
            // Use vanilla teleportation method
            player.teleportTo(targetLevel, x, y, z, yRot, 0.0f);
                
            LOGGER.info("Successfully teleported player to {}", dimensionName);
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Failed to teleport player to dimension: {}", dimensionName, e);
            return false;
        }
    }
    
    /**
     * Find where a player can safely stand near a level's shared spawn. Looks at the blocks themselves
     * because a surface heightmap gives the roof in dimensions with a solid ceiling.
     *
     * @return the position of the player's feet, or null if there is no safe spot in range
     */
    static BlockPos findSafeSpawnPos(ServerLevel level) {
        BlockPos spawn = level.getSharedSpawnPos();
        int minFeetY = level.getMinBuildHeight() + 1;
        int maxFeetY = level.getMaxBuildHeight() - 2;
        
        BlockPos found = null;
        if (spawn.getY() >= minFeetY && spawn.getY() <= maxFeetY) {
            found = searchColumns(level, spawn, spawn.getY(), minFeetY, maxFeetY, SAFE_SPAWN_PREFERRED_Y_RANGE);
        }
        if (found == null) {
            // The spawn Y can be outside the build range, so fall back to the sea level as the starting height
            int startY = spawn.getY() >= minFeetY && spawn.getY() <= maxFeetY ? spawn.getY()
                    : Math.max(minFeetY, Math.min(maxFeetY, level.getSeaLevel()));
            found = searchColumns(level, spawn, startY, minFeetY, maxFeetY, maxFeetY - minFeetY);
        }
        return found;
    }
    
    /**
     * Check columns in growing squares around the spawn, moving out from startY in each column
     */
    private static BlockPos searchColumns(ServerLevel level, BlockPos spawn, int startY, int minFeetY, int maxFeetY, int yRange) {
        BlockPos.MutableBlockPos feet = new BlockPos.MutableBlockPos();
        for (int radius = 0; radius <= SAFE_SPAWN_SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    int columnX = spawn.getX() + dx;
                    int columnZ = spawn.getZ() + dz;
                    if (!level.getWorldBorder().isWithinBounds(feet.set(columnX, startY, columnZ))) {
                        continue;
                    }
                    for (int dy = 0; dy <= yRange; dy++) {
                        int above = startY + dy;
                        if (above <= maxFeetY && isSafeToStand(level, feet.set(columnX, above, columnZ))) {
                            return feet.immutable();
                        }
                        int below = startY - dy;
                        if (dy > 0 && below >= minFeetY && isSafeToStand(level, feet.set(columnX, below, columnZ))) {
                            return feet.immutable();
                        }
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * A sturdy, harmless floor with two blocks of air above it
     */
    private static boolean isSafeToStand(ServerLevel level, BlockPos feet) {
        if (!level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir()) {
            return false;
        }
        BlockPos floorPos = feet.below();
        BlockState floor = level.getBlockState(floorPos);
        return floor.isFaceSturdy(level, floorPos, Direction.UP)
                && floor.getFluidState().isEmpty()
                && !floor.is(Blocks.MAGMA_BLOCK);
    }
    
    /**
     * Get the dimension configuration for Lost Cities
     */
    public static String getLostCityDimensionConfiguration() {
        return AutoloaderConfigSimple.LOST_CITY_DIMENSION.get();
    }
    
    /**
     * Whether Lost Cities targets the overworld, the case Lost Cities' own profile selection handles
     */
    public static boolean isOverworldTarget() {
        return Level.OVERWORLD.equals(getLostCityDimensionKey());
    }
    
    /**
     * Get the configured Lost Cities dimension as a key
     *
     * @return the key, or null (after logging an error) if the configured value is not a valid ID
     */
    public static ResourceKey<Level> getLostCityDimensionKey() {
        String target = getLostCityDimensionConfiguration();
        ResourceLocation location = ResourceLocation.tryParse(target);
        if (location == null) {
            // Called once per biome by the biome modifier, so only report each bad value once
            if (target.equals(reportedInvalidDimension)) {
                return null;
            }
            reportedInvalidDimension = target;
            LOGGER.error("lost_city_dimension '{}' is not a valid dimension ID - Lost Cities will not be configured for it", target);
            return null;
        }
        //? >1.18.2 {
        return ResourceKey.create(Registries.DIMENSION, location);
        //?} <=1.18.2 {
        /*return ResourceKey.create(Registry.DIMENSION_REGISTRY, location);
        *///?}
    }
    
    /**
     * Get the player spawn dimension configuration
     */
    public static String getPlayerSpawnDimensionConfiguration() {
        return AutoloaderConfigSimple.PLAYER_SPAWN_DIMENSION.get();
    }
    
    /**
     * Check if custom spawn is enabled
     */
    public static boolean isCustomSpawnEnabled() {
        return AutoloaderConfigSimple.ENABLE_CUSTOM_SPAWN.get();
    }
}
