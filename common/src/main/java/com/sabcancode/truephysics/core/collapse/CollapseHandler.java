package com.sabcancode.truephysics.core.collapse;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Set;

/**
 * Handles the actual collapse of unsupported blocks.
 * Single responsibility: decide what happens when blocks lose support.
 *
 * <p>v1 default: convert to vanilla {@code FallingBlockEntity}.
 * Future: cave-in events (M7), particle effects (M6), custom entities.</p>
 */
public interface CollapseHandler {

    /**
     * Collapse all provided positions. Implementations respect per-event
     * budgets from {@link com.sabcancode.truephysics.config.PhysicsConfig}.
     *
     * @param level     the server level
     * @param positions blocks that have exceeded their max support distance
     * @return number of blocks actually collapsed (may be less than input)
     */
    int collapse(ServerLevel level, Set<BlockPos> positions);
}
