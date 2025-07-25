package com.lostcitiesautoloader;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Manages custom player spawning and dimension configuration
 */
@EventBusSubscriber(modid = LostCitiesAutoloaderMod.MODID)
public class DimensionManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    
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
            
            // Apply Lost Cities dimension configuration
            updateLostCitiesDimensionConfig(lostCityDimension);
            
        } catch (Exception e) {
            LOGGER.error("Failed to apply dimension configuration", e);
        }
    }
    
    /**
     * Update Lost Cities configuration to generate in the specified dimension
     */
    private static void updateLostCitiesDimensionConfig(String targetDimension) {
        try {
            Path configPath = Paths.get("config/lostcities/common.toml");
            
            if (!Files.exists(configPath)) {
                LOGGER.warn("Lost Cities config file not found at: {}", configPath);
                return;
            }
            
            List<String> lines = Files.readAllLines(configPath);
            boolean updated = false;
            
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.trim().startsWith("dimensionsWithProfiles")) {
                    // Update the dimensionsWithProfiles setting
                    String newLine = "\tdimensionsWithProfiles = [\"" + targetDimension + "=default\"]";
                    lines.set(i, newLine);
                    updated = true;
                    LOGGER.info("Updated Lost Cities dimension configuration to: {}", targetDimension);
                    break;
                }
            }
            
            if (updated) {
                Files.write(configPath, lines);
                LOGGER.info("Lost Cities configuration file updated successfully");
            } else {
                LOGGER.warn("Could not find dimensionsWithProfiles setting in Lost Cities config");
            }
            
        } catch (IOException e) {
            LOGGER.error("Failed to update Lost Cities dimension configuration", e);
        }
    }
    
    /**
     * Handle player login to potentially teleport to custom spawn dimension
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        try {
            boolean enableCustomSpawn = AutoloaderConfigSimple.ENABLE_CUSTOM_SPAWN.get();
            
            if (!enableCustomSpawn) {
                LOGGER.debug("Custom spawn disabled, player will spawn in default dimension");
                return;
            }
            
            String playerSpawnDimension = AutoloaderConfigSimple.PLAYER_SPAWN_DIMENSION.get();
            String playerSpawnCoordinates = AutoloaderConfigSimple.PLAYER_SPAWN_COORDINATES.get();
            String playerSpawnFacing = AutoloaderConfigSimple.PLAYER_SPAWN_FACING.get();
            
            LOGGER.info("Custom spawn enabled - target dimension: {}, coordinates: '{}', facing: '{}'", 
                       playerSpawnDimension, playerSpawnCoordinates, playerSpawnFacing);
            
            // Teleport player to custom dimension with custom coordinates and facing
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                teleportPlayerToDimension(serverPlayer, playerSpawnDimension, playerSpawnCoordinates, playerSpawnFacing);
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to handle custom player spawn", e);
        }
    }
    
    /**
     * Teleport a player to the specified dimension with custom coordinates and facing
     */
    private static void teleportPlayerToDimension(ServerPlayer player, String dimensionName, String coordinates, String facing) {
        try {
            ResourceLocation dimensionLocation = ResourceLocation.parse(dimensionName);
            ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionLocation);
            
            ServerLevel targetLevel = player.getServer().getLevel(dimensionKey);
            
            if (targetLevel == null) {
                LOGGER.error("Target dimension does not exist: {}", dimensionName);
                return;
            }
            
            // Parse coordinates - use default spawn if empty or invalid
            double x, y, z;
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
                    x = targetLevel.getSharedSpawnPos().getX();
                    y = targetLevel.getSharedSpawnPos().getY();
                    z = targetLevel.getSharedSpawnPos().getZ();
                }
            } else {
                LOGGER.debug("No custom coordinates specified, using default spawn");
                x = targetLevel.getSharedSpawnPos().getX();
                y = targetLevel.getSharedSpawnPos().getY();
                z = targetLevel.getSharedSpawnPos().getZ();
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
            if (player.level().dimension().equals(dimensionKey) && useCustomCoordinates &&
                Math.abs(player.getX() - x) < 1.0 && 
                Math.abs(player.getY() - y) < 1.0 && 
                Math.abs(player.getZ() - z) < 1.0) {
                LOGGER.debug("Player is already at target location in dimension: {}", dimensionName);
                return;
            }
            
            String locationDesc = useCustomCoordinates ? 
                String.format("at coordinates (%.1f, %.1f, %.1f)", x, y, z) : 
                "at default spawn";
            String facingDesc = useCustomFacing ? 
                String.format("facing %.1f degrees", Double.parseDouble(facing)) : 
                "with default facing";
                
            LOGGER.info("Teleporting player {} to dimension: {} {} {}", 
                       player.getName().getString(), dimensionName, locationDesc, facingDesc);
            
            // Use vanilla teleportation method
            player.teleportTo(targetLevel, x, y, z, yRot, 0.0f);
                
            LOGGER.info("Successfully teleported player to {}", dimensionName);
            
        } catch (Exception e) {
            LOGGER.error("Failed to teleport player to dimension: {}", dimensionName, e);
        }
    }
    
    /**
     * Get the dimension configuration for Lost Cities
     */
    public static String getLostCityDimensionConfiguration() {
        return AutoloaderConfigSimple.LOST_CITY_DIMENSION.get();
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
