package com.sabcancode.truephysics.core.collapse;

import com.sabcancode.truephysics.config.PhysicsConfig;
import com.sabcancode.truephysics.config.PhysicsConfigHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

/**
 * Collapses unsupported blocks by converting them to vanilla
 * {@link FallingBlockEntity}s. Single responsibility: collapse mechanics.
 *
 * <p>Respects per-event block budget from config. All blocks — vanilla
 * and modded — are handled. Works with any block that has a valid
 * {@link BlockState}.</p>
 */
public final class FallingBlockCollapseHandler implements CollapseHandler {

    @Override
    public int collapse(ServerLevel level, Set<BlockPos> positions) {
        PhysicsConfig cfg = PhysicsConfigHolder.get();
        int collapsed = 0;

        for (BlockPos pos : positions) {
            if (collapsed >= cfg.caveinMaxBlocksPerEvent) break;

            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.getBlock() == Blocks.AIR) continue;

            // Remove the original block first, then spawn falling entity
            level.removeBlock(pos, false);
            FallingBlockEntity entity = FallingBlockEntity.fall(level, pos, state);
            level.addFreshEntity(entity);

            collapsed++;
        }

        return collapsed;
    }
}
