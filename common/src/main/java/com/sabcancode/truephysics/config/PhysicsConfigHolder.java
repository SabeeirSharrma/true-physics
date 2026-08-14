package com.sabcancode.truephysics.config;

import com.sabcancode.truephysics.core.item.ItemPhysicsRegistry;

/**
 * Static holder for the active {@link PhysicsConfig}.
 * Set once during mod init, read from anywhere in common code.
 */
public final class PhysicsConfigHolder {
    private static PhysicsConfig config;

    private PhysicsConfigHolder() {}

    public static PhysicsConfig get() {
        if (config == null) {
            throw new IllegalStateException("PhysicsConfig not yet loaded — called too early?");
        }
        return config;
    }

    public static void set(PhysicsConfig cfg) {
        if (config != null) {
            throw new IllegalStateException("PhysicsConfig already set");
        }
        config = cfg;

        // Initialize item physics registries from config
        ItemPhysicsRegistry.setSwimmingItems(cfg.itemPhysicsSwimmingItems);
        ItemPhysicsRegistry.setUndestroyableItems(cfg.itemPhysicsUndestroyableItems);
    }

    public static boolean isLoaded() {
        return config != null;
    }
}
