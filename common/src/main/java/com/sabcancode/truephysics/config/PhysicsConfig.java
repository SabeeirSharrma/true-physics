package com.sabcancode.truephysics.config;

/**
 * M0 — Config & Performance Safety Rails.
 *
 * Every field has a sane low-end default. Server owners raise caps;
 * power users tune knobs. Nothing is hardcoded in the systems that follow —
 * they all read from this object.
 */
public final class PhysicsConfig {

    // ─── Master toggles ────────────────────────────────────────────
    public boolean structuralCollapseEnabled = true;
    public boolean adhesivesEnabled          = true;
    public boolean itemPhysicsEnabled        = true;
    public boolean oceanPhysicsEnabled       = true;
    public boolean caveInsEnabled            = true;
    public boolean particlesEnabled          = true;

    // ─── Structural collapse budgets (M1) ──────────────────────────
    /** Max blocks walked per single support-graph BFS event. */
    public int structuralMaxBlocksPerBfs   = 256;
    /** Max BFS events processed per server tick. */
    public int structuralMaxBfsPerTick     = 4;

    // ─── Item physics budgets (M4) ─────────────────────────────────
    /** Max items being physics-simulated at once. */
    public int itemPhysicsMaxActive        = 64;
    /** Ticks an item must be nearly stationary before sleep. */
    public int itemPhysicsRestThreshold    = 40;

    // ─── Cave-in budgets (M7) ──────────────────────────────────────
    /** Ticks between cave-in scans for the same chunk region. */
    public int caveinScanCooldown          = 200;
    /** Max blocks destroyed in a single cave-in event. */
    public int caveinMaxBlocksPerEvent     = 128;
    /** Radius (blocks) of a cave-in collapse. */
    public int caveinMaxRadius             = 8;

    // ─── Diagnostics ───────────────────────────────────────────────
    /** Log physics-system load every N ticks (0 = off). */
    public int diagnosticsLogInterval      = 0;
}
