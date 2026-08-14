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
 * <h3>Adhesive blocks (M2)</h3>
 * <p>Adhesive blocks (slime, honey, etc.) transmit support at zero cost.
 * When the BFS encounters an adhesive block, its neighbors receive the
 * same distance instead of distance + 1. This means:</p>
 * <ul>
 *   <li>A structural block connected to an anchor via adhesive has the
 *       same support distance as one directly adjacent to the anchor.</li>
 *   <li>If the adhesive path breaks, all connected blocks in the group
 *       lose support and fall together.</li>
 * </ul>
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

        // Collect anchor, structural, and adhesive positions in this chunk
        List<BlockPos> anchors = new ArrayList<>();
        List<BlockPos> structural = new ArrayList<>();
        Set<BlockPos> adhesive = new HashSet<>();

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
                        classifyAndCollect(pos, anchors, structural, adhesive);
                    }
                }
            }
        }

        // BFS from anchors (with adhesive zero-cost edges)
        Map<BlockPos, Integer> localDist = new HashMap<>();
        bfsFromAnchors(anchors, structural, adhesive, localDist, cfg.structuralMaxBlocksPerBfs);

        // Merge into global state
        Set<BlockPos> localUnsupported = new HashSet<>();

        for (BlockPos pos : structural) {
            int dist = localDist.getOrDefault(pos, Integer.MAX_VALUE);
            distances.put(pos, dist);

            if (dist > cfg.structuralMaxBlocksPerBfs) {
                localUnsupported.add(pos);
            }
        }

        // Adhesive blocks also participate in the graph — they can fall
        for (BlockPos pos : adhesive) {
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
                                    List<BlockPos> structural,
                                    Set<BlockPos> adhesive) {
        Classification c = classifier.classify(level, pos);
        switch (c) {
            case ANCHOR     -> anchors.add(pos);
            case STRUCTURAL -> structural.add(pos);
            case ADHESIVE   -> adhesive.add(pos);
            case NONE       -> { /* skip */ }
        }
    }

    /**
     * BFS from all anchors simultaneously, with adhesive zero-cost edges.
     *
     * <p>When the current block is adhesive, its neighbors receive the
     * same distance (zero-cost edge). This means adhesive blocks
     * "conduct" support without adding to the distance.</p>
     *
     * <p>Example: Anchor(0) → Structural(1) → Adhesive(1) → Structural(1)
     * The last structural block has distance 1, not 3, because the
     * adhesive block doesn't add cost.</p>
     */
    private void bfsFromAnchors(List<BlockPos> anchors,
                                List<BlockPos> structural,
                                Set<BlockPos> adhesive,
                                Map<BlockPos, Integer> result,
                                int maxBlocks) {
        // All participating blocks (structural + adhesive)
        Set<BlockPos> allBlocks = new HashSet<>(structural);
        allBlocks.addAll(adhesive);

        Queue<BlockPos> queue = new ArrayDeque<>();
        int walked = 0;

        for (BlockPos anchor : anchors) {
            result.put(anchor, 0);
            queue.add(anchor);
        }

        while (!queue.isEmpty() && walked < maxBlocks) {
            BlockPos current = queue.poll();
            int currentDist = result.get(current);
            boolean currentIsAdhesive = adhesive.contains(current);

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (!allBlocks.contains(neighbor)) continue;
                if (result.containsKey(neighbor)) continue;
                // Don't walk into unloaded chunks
                if (!level.isLoaded(neighbor)) continue;

                // Zero-cost edge: adhesive blocks transmit support without adding distance
                int neighborDist = currentIsAdhesive ? currentDist : currentDist + 1;
                result.put(neighbor, neighborDist);
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
