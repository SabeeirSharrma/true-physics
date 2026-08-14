package com.sabcancode.truephysics.platform.true_physics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sabcancode.truephysics.TruePhysics;
import com.sabcancode.truephysics.config.PhysicsConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Fabric config loader — plain JSON file via Gson.
 * Lives in the platform.true_physics package so Architectury's
 * {@code @ExpectPlatform} transformer can discover it.
 */
public final class ConfigLoaderImpl {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static PhysicsConfig load(Path configDir) {
        Path file = configDir.resolve(TruePhysics.MOD_ID + ".json");

        if (Files.exists(file)) {
            try {
                String json = Files.readString(file);
                PhysicsConfig config = GSON.fromJson(json, PhysicsConfig.class);
                if (config != null) {
                    TruePhysics.LOGGER.info("Loaded config from {}", file);
                    return config;
                }
            } catch (Exception e) {
                TruePhysics.LOGGER.error("Failed to read config — using defaults", e);
            }
        }

        // First launch or corrupt file → write defaults
        PhysicsConfig defaults = new PhysicsConfig();
        save(configDir, defaults);
        return defaults;
    }

    public static void save(Path configDir, PhysicsConfig config) {
        Path file = configDir.resolve(TruePhysics.MOD_ID + ".json");
        try {
            Files.createDirectories(configDir);
            Files.writeString(file, GSON.toJson(config));
            TruePhysics.LOGGER.info("Wrote default config to {}", file);
        } catch (IOException e) {
            TruePhysics.LOGGER.error("Failed to write config", e);
        }
    }
}
