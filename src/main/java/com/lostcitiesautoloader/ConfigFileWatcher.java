package com.lostcitiesautoloader;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Watches the config directory for file changes to debug the .bak file creation issue
 */
public class ConfigFileWatcher {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean watching = new AtomicBoolean(false);
    private static WatchService watchService;
    private static Thread watchThread;

    public static void startWatching() {
        if (watching.compareAndSet(false, true)) {
            try {
                Path configDir = Paths.get("config");
                if (!Files.exists(configDir)) {
                    LOGGER.info("Config directory doesn't exist yet, creating it");
                    Files.createDirectories(configDir);
                }

                LOGGER.info("=== Starting config file watcher on: {} ===", configDir.toAbsolutePath());
                watchService = FileSystems.getDefault().newWatchService();
                
                // Register the config directory for all types of events
                configDir.register(watchService, 
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);

                watchThread = new Thread(() -> {
                    LOGGER.info("Config file watcher thread started");
                    try {
                        while (!Thread.currentThread().isInterrupted()) {
                            WatchKey key = watchService.take();
                            
                            for (WatchEvent<?> event : key.pollEvents()) {
                                WatchEvent.Kind<?> kind = event.kind();
                                
                                if (kind == StandardWatchEventKinds.OVERFLOW) {
                                    continue;
                                }
                                
                                @SuppressWarnings("unchecked")
                                WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                                Path fileName = pathEvent.context();
                                
                                // Only log our config files
                                if (fileName.toString().contains("lostcitiesautoloader")) {
                                    LOGGER.info("*** CONFIG FILE EVENT: {} - {} ***", kind.name(), fileName);
                                    LOGGER.info("    Event count: {}", event.count());
                                    LOGGER.info("    Full path: {}", configDir.resolve(fileName));
                                    LOGGER.info("    Timestamp: {}", java.time.Instant.now());
                                    LOGGER.info("    Thread: {}", Thread.currentThread().getName());
                                    
                                    // Print stack trace to see what's causing the file operation
                                    LOGGER.info("    Stack trace:");
                                    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                                    for (int i = 0; i < Math.min(15, stackTrace.length); i++) {
                                        LOGGER.info("      [{}] {}", i, stackTrace[i]);
                                    }
                                }
                            }
                            
                            boolean valid = key.reset();
                            if (!valid) {
                                LOGGER.warn("Config watcher key became invalid");
                                break;
                            }
                        }
                    } catch (InterruptedException e) {
                        LOGGER.info("Config file watcher interrupted");
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        LOGGER.error("Error in config file watcher", e);
                    } finally {
                        LOGGER.info("Config file watcher thread ending");
                    }
                }, "ConfigFileWatcher");
                
                watchThread.setDaemon(true);
                watchThread.start();
                LOGGER.info("Config file watcher started successfully");
                
            } catch (IOException e) {
                LOGGER.error("Failed to start config file watcher", e);
                watching.set(false);
            }
        }
    }

    public static void stopWatching() {
        if (watching.compareAndSet(true, false)) {
            LOGGER.info("Stopping config file watcher");
            try {
                if (watchService != null) {
                    watchService.close();
                }
                if (watchThread != null) {
                    watchThread.interrupt();
                }
            } catch (IOException e) {
                LOGGER.error("Error stopping config file watcher", e);
            }
        }
    }
}
