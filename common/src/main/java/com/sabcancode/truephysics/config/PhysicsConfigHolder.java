package com.sabcancode.truephysics.config;

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
    }

    public static boolean isLoaded() {
        return config != null;
    }
}
