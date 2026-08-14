package com.sabcancode.truephysics.core.graph;

import com.sabcancode.truephysics.TruePhysics;
import com.sabcancode.truephysics.config.PhysicsConfig;
import com.sabcancode.truephysics.config.PhysicsConfigHolder;
import com.sabcancode.truephysics.core.classifier.BlockClassifier;
import com.sabcancode.truephysics.core.classifier.BlockClassifier.Classification;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.*;

/**
 * BFS-based support graph. Single responsibility: compute and cache
 * support distances, track dirty chunks, enforce traversal budgets.
 *
 * <p>Cross-chunk propagation is handled by marking neighboring chunks
 * dirty when a block changes — the next tick recomputes them.</p>
 */
public final class BfsSupportGraph implements SupportGraph {

    private final ServerLevel level;
    private final BlockClassifier classifier;

    /** Block → distance-to-anchor.  {@code MAX_VALUE} = no anchor path. */
    private final Map<BlockPos, Integer> distances = new HashMap<>();
    /** Set of blocks currently exceeding their max support distance. */
    private final Set<BlockPos> unsupported = new HashSet<>();
    /** Chunks needing recomputation on the next tick. */
    private final Set<Long> dirtyChunks = new LinkedHashSet<>();

    public BfsSupportGraph(ServerLevel level, BlockClassifier classifier) {
        this.level = level;
        this.classifier = classifier;
    }

    // ── SupportGraph interface ──────────────────────────────────────

    @Override
    public int getSupportDistance(BlockPos pos) {
        return distances.getOrDefault(pos, Integer.MAX_VALUE);
    }

    @Override
    public void markDirty(ChunkPos chunk) {
        dirtyChunks.add(chunk.pack());
    }

    @Override
    public void tick() {
        PhysicsConfig cfg = PhysicsConfigHolder.get();
        int processed = 0;

        var iterator = dirtyChunks.iterator();
        while (iterator.hasNext() && processed < cfg.structuralMaxBfsPerTick) {
            long packed = iterator.next();
            iterator.remove();
            recomputeChunk(ChunkPos.unpack(packed));
            processed++;
        }
    }

    @Override
    public Set<BlockPos> getUnsupportedBlocks() {
        return Collections.unmodifiableSet(unsupported);
    }

    // ── Internal ───────────────────────────────────────────────────

    /**
     * Full recomputation of support distances within one chunk.
     * Treats chunk boundaries as implicit anchors (configurable later).
     */
    private void recomputeChunk(ChunkPos chunk) {
        if (!level.isLoaded(chunk.getWorldPosition())) return;

        PhysicsConfig cfg = PhysicsConfigHolder.get();
        LevelChunk levelChunk = (LevelChunk) level.getChunk(chunk.getWorldPosition());

        // Collect anchor + structural positions in this chunk
        List<BlockPos> anchors = new ArrayList<>();
        List<BlockPos> structural = new ArrayList<>();

        LevelChunkSection[] sections = levelChunk.getSections();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section.hasOnlyAir()) continue;

            int sectionY = levelChunk.getSectionYFromSectionIndex(i);

            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockPos pos = new BlockPos(
                                chunk.getMinBlockX() + x,
                                sectionY * 16 + y,
                                chunk.getMinBlockZ() + z);
                        classifyAndCollect(pos, anchors, structural);
                    }
                }
            }
        }

        // BFS from anchors
        Map<BlockPos, Integer> localDist = new HashMap<>();
        bfsFromAnchors(anchors, structural, localDist, cfg.structuralMaxBlocksPerBfs);

        // Merge into global state
        Set<BlockPos> localUnsupported = new HashSet<>();

        for (BlockPos pos : structural) {
            int dist = localDist.getOrDefault(pos, Integer.MAX_VALUE);
            distances.put(pos, dist);

            if (dist > cfg.structuralMaxBlocksPerBfs) {
                localUnsupported.add(pos);
            }
        }

        // Also mark neighbors in adjacent chunks as needing recompute
        // (cross-chunk propagation)
        markNeighborChunksDirty(chunk);

        unsupported.clear();
        // Rebuild unsupported from all stored distances
        for (var entry : distances.entrySet()) {
            if (entry.getValue() > cfg.structuralMaxBlocksPerBfs) {
                unsupported.add(entry.getKey());
            }
        }
    }

    private void classifyAndCollect(BlockPos pos,
                                    List<BlockPos> anchors,
                                    List<BlockPos> structural) {
        Classification c = classifier.classify(level, pos);
        switch (c) {
            case ANCHOR     -> anchors.add(pos);
            case STRUCTURAL -> structural.add(pos);
            case NONE       -> { /* skip */ }
        }
    }

    /**
     * Standard BFS from all anchors simultaneously.
     * Stops when budget is exhausted.
     */
    private void bfsFromAnchors(List<BlockPos> anchors,
                                List<BlockPos> structural,
                                Map<BlockPos, Integer> result,
                                int maxBlocks) {
        Set<BlockPos> structuralSet = new HashSet<>(structural);
        Queue<BlockPos> queue = new ArrayDeque<>();
        int walked = 0;

        for (BlockPos anchor : anchors) {
            result.put(anchor, 0);
            queue.add(anchor);
        }

        while (!queue.isEmpty() && walked < maxBlocks) {
            BlockPos current = queue.poll();
            int currentDist = result.get(current);

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (!structuralSet.contains(neighbor)) continue;
                if (result.containsKey(neighbor)) continue;
                // Don't walk into unloaded chunks
                if (!level.isLoaded(neighbor)) continue;

                result.put(neighbor, currentDist + 1);
                queue.add(neighbor);
                walked++;
            }
        }
    }

    private void markNeighborChunksDirty(ChunkPos center) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                dirtyChunks.add(new ChunkPos(center.x() + dx, center.z() + dz).pack());
            }
        }
    }
}
