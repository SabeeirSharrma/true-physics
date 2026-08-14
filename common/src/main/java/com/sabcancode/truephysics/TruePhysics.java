package com.sabcancode.truephysics;

import com.sabcancode.truephysics.config.PhysicsConfig;
import com.sabcancode.truephysics.config.PhysicsConfigHolder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TruePhysics {
    public static final String MOD_ID = "true_physics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * Common init — called by each platform entrypoint after loading config.
     */
    public static void init(PhysicsConfig config) {
        PhysicsConfigHolder.set(config);
        logEnabledSystems(config);
    }

    private static void logEnabledSystems(PhysicsConfig c) {
        LOGGER.info("True Physics {} | systems: collapse={} adhesives={} items={} ocean={} caveins={} particles={}",
                "v1.0.0",
                c.structuralCollapseEnabled,
                c.adhesivesEnabled,
                c.itemPhysicsEnabled,
                c.oceanPhysicsEnabled,
                c.caveInsEnabled,
                c.particlesEnabled);
        LOGGER.info("  budgets: bfs_blocks={} bfs_tick={} item_active={} item_rest={} "
                + "cave_cooldown={} cave_blocks={} cave_radius={}",
                c.structuralMaxBlocksPerBfs,
                c.structuralMaxBfsPerTick,
                c.itemPhysicsMaxActive,
                c.itemPhysicsRestThreshold,
                c.caveinScanCooldown,
                c.caveinMaxBlocksPerEvent,
                c.caveinMaxRadius);
    }
}
