package com.sabcancode.truephysics.platform.true_physics;

import com.sabcancode.truephysics.TruePhysics;
import com.sabcancode.truephysics.config.PhysicsConfig;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * NeoForge config loader — ModConfigSpec.
 * Lives in the platform.true_physics package so Architectury's
 * {@code @ExpectPlatform} transformer can discover it.
 */
public final class ConfigLoaderImpl {

    // ─── Spec (built once, read forever) ────────────────────────────
    private static ModConfigSpec SPEC;
    private static ModConfigSpec.BooleanValue
            cfgStructural, cfgAdhesives, cfgItems, cfgOcean, cfgCaveins, cfgParticles;
    private static ModConfigSpec.IntValue
            cfgBfsBlocks, cfgBfsTick,
            cfgAdhesiveMaxDist,
            cfgItemActive, cfgItemRest,
            cfgCaveCooldown, cfgCaveBlocks, cfgCaveRadius,
            cfgDiagInterval;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        // Master toggles
        cfgStructural  = b.comment("Enable structural-collapse system")
                          .define("structuralCollapseEnabled", true);
        cfgAdhesives   = b.comment("Enable adhesive/glue mechanic")
                          .define("adhesivesEnabled", true);
        cfgItems       = b.comment("Enable item physics")
                          .define("itemPhysicsEnabled", true);
        cfgOcean       = b.comment("Enable ocean/buoyancy physics")
                          .define("oceanPhysicsEnabled", true);
        cfgCaveins     = b.comment("Enable cave-in events")
                          .define("caveInsEnabled", true);
        cfgParticles   = b.comment("Enable stress/debris particles")
                          .define("particlesEnabled", true);

        b.comment("Structural collapse budgets").push("structural");
        cfgBfsBlocks = b.comment("Max blocks walked per BFS event")
                        .defineInRange("maxBlocksPerBfs", 256, 32, 4096);
        cfgBfsTick   = b.comment("Max BFS events processed per tick")
                        .defineInRange("maxBfsPerTick", 4, 1, 64);
        b.pop();

        b.comment("Adhesive budgets").push("adhesives");
        cfgAdhesiveMaxDist = b.comment("Max distance adhesive can transmit support (0 = unlimited)")
                              .defineInRange("maxDistance", 0, 0, 256);
        b.pop();

        b.comment("Item physics budgets").push("itemPhysics");
        cfgItemActive = b.comment("Max concurrently physics-active items")
                         .defineInRange("maxActive", 64, 8, 1024);
        cfgItemRest   = b.comment("Ticks before a near-stationary item sleeps")
                         .defineInRange("restThreshold", 40, 5, 200);
        b.pop();

        b.comment("Cave-in budgets").push("caveins");
        cfgCaveCooldown = b.comment("Ticks between scans for same chunk region")
                           .defineInRange("scanCooldown", 200, 20, 2000);
        cfgCaveBlocks   = b.comment("Max blocks destroyed per cave-in event")
                           .defineInRange("maxBlocksPerEvent", 128, 16, 1024);
        cfgCaveRadius   = b.comment("Collapse radius in blocks")
                           .defineInRange("maxRadius", 8, 2, 32);
        b.pop();

        b.comment("Diagnostics").push("diagnostics");
        cfgDiagInterval = b.comment("Log physics load every N ticks (0 = off)")
                           .defineInRange("logInterval", 0, 0, 1200);
        b.pop();

        SPEC = b.build();
    }

    // ─── Public API ─────────────────────────────────────────────────

    /** Call from your @Mod constructor to register the config. */
    public static ModConfigSpec getSpec() {
        return SPEC;
    }

    /** Build a PhysicsConfig from the current spec values. */
    public static PhysicsConfig load() {
        PhysicsConfig c = new PhysicsConfig();
        c.structuralCollapseEnabled = cfgStructural.get();
        c.adhesivesEnabled          = cfgAdhesives.get();
        c.itemPhysicsEnabled        = cfgItems.get();
        c.oceanPhysicsEnabled       = cfgOcean.get();
        c.caveInsEnabled            = cfgCaveins.get();
        c.particlesEnabled          = cfgParticles.get();

        c.structuralMaxBlocksPerBfs = cfgBfsBlocks.get();
        c.structuralMaxBfsPerTick   = cfgBfsTick.get();

        c.adhesiveMaxDistance       = cfgAdhesiveMaxDist.get();

        c.itemPhysicsMaxActive      = cfgItemActive.get();
        c.itemPhysicsRestThreshold  = cfgItemRest.get();

        c.caveinScanCooldown        = cfgCaveCooldown.get();
        c.caveinMaxBlocksPerEvent   = cfgCaveBlocks.get();
        c.caveinMaxRadius           = cfgCaveRadius.get();

        c.diagnosticsLogInterval    = cfgDiagInterval.get();

        TruePhysics.LOGGER.info("Loaded NeoForge ModConfigSpec config");
        return c;
    }
}
