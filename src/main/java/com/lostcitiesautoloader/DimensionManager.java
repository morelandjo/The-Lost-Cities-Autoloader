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
            LOGGER.info("Custom spawn enabled - target dimension: {}", playerSpawnDimension);
            
            // Teleport player to custom dimension
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                teleportPlayerToDimension(serverPlayer, playerSpawnDimension);
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to handle custom player spawn", e);
        }
    }
    
    /**
     * Teleport a player to the specified dimension
     */
    private static void teleportPlayerToDimension(ServerPlayer player, String dimensionName) {
        try {
            ResourceLocation dimensionLocation = ResourceLocation.parse(dimensionName);
            ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionLocation);
            
            ServerLevel targetLevel = player.getServer().getLevel(dimensionKey);
            
            if (targetLevel == null) {
                LOGGER.error("Target dimension does not exist: {}", dimensionName);
                return;
            }
            
            // Only teleport if player is not already in the target dimension
            if (player.level().dimension().equals(dimensionKey)) {
                LOGGER.debug("Player is already in target dimension: {}", dimensionName);
                return;
            }
            
            LOGGER.info("Teleporting player {} to dimension: {}", player.getName().getString(), dimensionName);
            
            // Use vanilla teleportation method
            player.teleportTo(targetLevel, 
                targetLevel.getSharedSpawnPos().getX(), 
                targetLevel.getSharedSpawnPos().getY(), 
                targetLevel.getSharedSpawnPos().getZ(), 
                player.getYRot(), 
                player.getXRot());
                
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
