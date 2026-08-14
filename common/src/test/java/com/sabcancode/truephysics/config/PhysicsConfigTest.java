package com.sabcancode.truephysics.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PhysicsConfig} — verifies default values
 * and that all fields are properly initialized.
 */
class PhysicsConfigTest {

    @Test
    void defaultMasterToggles() {
        PhysicsConfig cfg = new PhysicsConfig();
        assertTrue(cfg.structuralCollapseEnabled, "structural collapse should be enabled by default");
        assertTrue(cfg.adhesivesEnabled, "adhesives should be enabled by default");
        assertTrue(cfg.itemPhysicsEnabled, "item physics should be enabled by default");
        assertTrue(cfg.oceanPhysicsEnabled, "ocean physics should be enabled by default");
        assertTrue(cfg.caveInsEnabled, "cave-ins should be enabled by default");
        assertTrue(cfg.particlesEnabled, "particles should be enabled by default");
    }

    @Test
    void defaultStructuralBudgets() {
        PhysicsConfig cfg = new PhysicsConfig();
        assertEquals(256, cfg.structuralMaxBlocksPerBfs, "default BFS max blocks");
        assertEquals(4, cfg.structuralMaxBfsPerTick, "default BFS per tick");
    }

    @Test
    void defaultItemPhysicsBudgets() {
        PhysicsConfig cfg = new PhysicsConfig();
        assertEquals(64, cfg.itemPhysicsMaxActive, "default max active items");
        assertEquals(40, cfg.itemPhysicsRestThreshold, "default rest threshold");
    }

    @Test
    void defaultCaveInBudgets() {
        PhysicsConfig cfg = new PhysicsConfig();
        assertEquals(200, cfg.caveinScanCooldown, "default cave-in cooldown");
        assertEquals(128, cfg.caveinMaxBlocksPerEvent, "default cave-in max blocks");
        assertEquals(8, cfg.caveinMaxRadius, "default cave-in radius");
    }

    @Test
    void defaultDiagnostics() {
        PhysicsConfig cfg = new PhysicsConfig();
        assertEquals(0, cfg.diagnosticsLogInterval, "diagnostics should be off by default");
    }

    @Test
    void fieldsAreMutable() {
        PhysicsConfig cfg = new PhysicsConfig();
        cfg.structuralCollapseEnabled = false;
        cfg.structuralMaxBlocksPerBfs = 512;
        cfg.diagnosticsLogInterval = 100;

        assertFalse(cfg.structuralCollapseEnabled);
        assertEquals(512, cfg.structuralMaxBlocksPerBfs);
        assertEquals(100, cfg.diagnosticsLogInterval);
    }
}
