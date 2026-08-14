package com.sabcancode.truephysics.core.classifier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

/**
 * Determines whether a block at a given position participates in the
 * structural support graph. Single responsibility: classification only.
 *
 * <p>Implementations may use tags, block properties, NBT, or any other
 * heuristic — the graph doesn't care how the decision is made.</p>
 */
public interface BlockClassifier {

    /** Block-tag key under {@code true_physics:} for opt-in structural blocks. */
    String STRUCTURAL_TAG = "structural";
    /** Block-tag key for blocks that act as anchors (bedrock, deep terrain, etc.). */
    String ANCHOR_TAG     = "anchor";

    enum Classification {
        /** Not part of the support graph. */
        NONE,
        /** Participates in support-distance BFS but is not an anchor. */
        STRUCTURAL,
        /** Treated as distance-0 anchor — structural blocks connect toward these. */
        ANCHOR
    }

    /**
     * Classify a block for the support graph.
     *
     * @param level  block getter (may be a chunk section — don't load chunks)
     * @param pos    block position
     * @return classification
     */
    Classification classify(BlockGetter level, BlockPos pos);
}
