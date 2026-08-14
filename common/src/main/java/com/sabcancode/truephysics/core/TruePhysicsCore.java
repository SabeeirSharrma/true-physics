package com.sabcancode.truephysics.core;

import com.sabcancode.truephysics.TruePhysics;
import com.sabcancode.truephysics.config.PhysicsConfig;
import com.sabcancode.truephysics.config.PhysicsConfigHolder;
import com.sabcancode.truephysics.core.classifier.BlockClassifier;
import com.sabcancode.truephysics.core.classifier.TagBlockClassifier;
import com.sabcancode.truephysics.core.collapse.CollapseHandler;
import com.sabcancode.truephysics.core.collapse.FallingBlockCollapseHandler;
import com.sabcancode.truephysics.core.graph.BfsSupportGraph;
import com.sabcancode.truephysics.core.graph.SupportGraph;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * Central coordinator for the structural-integrity system.
 *
 * <p>Owns the lifecycle of the three core subsystems (classifier, graph,
 * collapse) and exposes a single {@link #onBlockChanged} entry point
 * that platform event listeners call.</p>
 *
 * <p>SOLID notes:
 * <ul>
 *   <li><b>SRP</b> — wiring + event dispatch only; algorithms live in the subsystems.</li>
 *   <li><b>DIP</b> — depends on the three interfaces, not concrete classes
 *       (constructors accept implementations).</li>
 *   <li><b>OCP</b> — new classifiers, graph strategies, or collapse handlers
 *       can be swapped without touching this class.</li>
 * </ul></p>
 */
public final class TruePhysicsCore {

    private final BlockClassifier classifier;
    private final SupportGraph graph;
    private final CollapseHandler collapse;

    /** The server level this core is bound to. */
    private final ServerLevel level;

    public TruePhysicsCore(ServerLevel level,
                           BlockClassifier classifier,
                           SupportGraph graph,
                           CollapseHandler collapse) {
        this.level      = level;
        this.classifier = classifier;
        this.graph      = graph;
        this.collapse   = collapse;
    }

    /**
     * Convenience factory that wires the default implementations
     * (tag classifier, BFS graph, falling-block collapse).
     *
     * <p>All blocks — vanilla and modded — are handled via datapack tags
     * ({@code true_physics:structural}, {@code true_physics:anchor}).
     * Any block added to these tags (by any mod or modpack) will be
     * classified automatically.</p>
     */
    public static TruePhysicsCore create(ServerLevel level) {
        BlockClassifier classifier = new TagBlockClassifier();
        SupportGraph graph         = new BfsSupportGraph(level, classifier);
        CollapseHandler collapse   = new FallingBlockCollapseHandler();
        return new TruePhysicsCore(level, classifier, graph, collapse);
    }

    // ── Accessors ──────────────────────────────────────────────────

    public ServerLevel    level()      { return level; }
    public BlockClassifier classifier() { return classifier; }
    public SupportGraph    graph()      { return graph; }
    public CollapseHandler collapse()   { return collapse; }

    // ── Event dispatch ──────────────────────────────────────────────

    /**
     * Called by platform event listeners when a block is placed or broken.
     * Marks the affected chunk (and neighbours) dirty for recompute.
     *
     * @param pos the position that changed
     */
    public void onBlockChanged(BlockPos pos) {
        PhysicsConfig cfg = PhysicsConfigHolder.get();
        if (!cfg.structuralCollapseEnabled) return;

        ChunkPos chunk = ChunkPos.containing(pos);
        graph.markDirty(chunk);
    }

    /**
     * Called once per server tick. Processes dirty chunks, detects
     * unsupported blocks, triggers collapse.
     */
    public void tick() {
        PhysicsConfig cfg = PhysicsConfigHolder.get();
        if (!cfg.structuralCollapseEnabled) return;

        // 1) Recompute dirty chunks (respects bfs/tick budget)
        graph.tick();

        // 2) Collapse any newly-unsupported blocks
        var unsupported = graph.getUnsupportedBlocks();
        if (!unsupported.isEmpty()) {
            int collapsed = collapse.collapse(level, unsupported);
            if (collapsed > 0) {
                TruePhysics.LOGGER.debug("Collapsed {} unsupported blocks", collapsed);
            }
        }

        // 3) Diagnostics
        if (cfg.diagnosticsLogInterval > 0) {
            logDiagnostics(cfg);
        }
    }

    private int tickCounter = 0;

    private void logDiagnostics(PhysicsConfig cfg) {
        tickCounter++;
        if (tickCounter % cfg.diagnosticsLogInterval == 0) {
            TruePhysics.LOGGER.info("Physics load — unsupported: {}, dirty chunks pending",
                    graph.getUnsupportedBlocks().size());
        }
    }
}
