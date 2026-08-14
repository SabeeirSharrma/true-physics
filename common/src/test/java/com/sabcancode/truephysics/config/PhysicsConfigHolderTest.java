package com.sabcancode.truephysics.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PhysicsConfigHolder} — verifies set/get lifecycle
 * and error handling.
 */
class PhysicsConfigHolderTest {

    @AfterEach
    void resetHolder() {
        // Reset the static holder between tests via reflection
        try {
            var field = PhysicsConfigHolder.class.getDeclaredField("config");
            field.setAccessible(true);
            field.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reset PhysicsConfigHolder", e);
        }
    }

    @Test
    void initiallyNotLoaded() {
        assertFalse(PhysicsConfigHolder.isLoaded());
    }

    @Test
    void setAndGetConfig() {
        PhysicsConfig cfg = new PhysicsConfig();
        PhysicsConfigHolder.set(cfg);

        assertTrue(PhysicsConfigHolder.isLoaded());
        assertSame(cfg, PhysicsConfigHolder.get());
    }

    @Test
    void getThrowsBeforeSet() {
        assertThrows(IllegalStateException.class, PhysicsConfigHolder::get,
                "get() before set() should throw");
    }

    @Test
    void setThrowsOnDoubleSet() {
        PhysicsConfigHolder.set(new PhysicsConfig());
        assertThrows(IllegalStateException.class, () -> PhysicsConfigHolder.set(new PhysicsConfig()),
                "double set() should throw");
    }

    @Test
    void configValuesAreLive() {
        PhysicsConfig cfg = new PhysicsConfig();
        cfg.structuralCollapseEnabled = false;
        PhysicsConfigHolder.set(cfg);

        assertFalse(PhysicsConfigHolder.get().structuralCollapseEnabled);
    }
}
