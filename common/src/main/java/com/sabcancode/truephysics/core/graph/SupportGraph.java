package com.sabcancode.truephysics.core.graph;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import java.util.Set;

/**
 * Tracks structural-support distances from every structural block to its
 * nearest anchor. Single responsibility: distance queries + dirty tracking.
 *
 * <p>The graph is recomputed lazily — only when a block break/place event
 * marks a chunk dirty. BFS budget is enforced by the implementation (see
 * {@code PhysicsConfig}).</p>
 */
public interface SupportGraph {

    /**
     * Returns the computed support distance for a block, or
     * {@link Integer#MAX_VALUE} if no path to an anchor exists.
     */
    int getSupportDistance(BlockPos pos);

    /**
     * Marks a chunk as needing recompute on the next scheduled tick.
     * Multiple calls per tick are deduplicated.
     */
    void markDirty(ChunkPos chunk);

    /**
     * Process pending dirty chunks up to the per-tick budget.
     * Called once per server tick from the event coordinator.
     */
    void tick();

    /**
     * @return set of positions currently flagged as unsupported (distance
     *         exceeds max), ready for collapse handling
     */
    Set<BlockPos> getUnsupportedBlocks();
}
