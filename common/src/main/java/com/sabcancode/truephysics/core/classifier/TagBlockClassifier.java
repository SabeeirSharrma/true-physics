package com.sabcancode.truephysics.core.classifier;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

/**
 * Classifies blocks by checking custom {@code true_physics:structural} and
 * {@code true_physics:anchor} tags first, falling back to a sensible
 * hardcoded default set.
 *
 * <p>Tags are defined via datapacks — add a block to the tag and it becomes
 * structural/anchor. This class is the single place that knowledge lives.</p>
 */
public final class TagBlockClassifier implements BlockClassifier {

    private static final Identifier STRUCTURAL_ID =
            Identifier.fromNamespaceAndPath("true_physics", "structural");
    private static final Identifier ANCHOR_ID =
            Identifier.fromNamespaceAndPath("true_physics", "anchor");

    // ── Hardcoded fallbacks (used when tag datapacks are absent) ────
    private static final Set<Block> DEFAULT_ANCHORS = Set.of(
            Blocks.BEDROCK,
            Blocks.DEEPSLATE,
            Blocks.DEEPSLATE_BRICKS,
            Blocks.DEEPSLATE_TILES,
            Blocks.POLISHED_DEEPSLATE
    );

    private static final Set<Block> DEFAULT_STRUCTURAL = Set.of(
            Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE,
            Blocks.DEEPSLATE, Blocks.TUFF, Blocks.BRICKS, Blocks.STONE_BRICKS,
            Blocks.MOSSY_STONE_BRICKS, Blocks.NETHER_BRICKS,
            Blocks.PACKED_ICE, Blocks.BLUE_ICE,
            Blocks.IRON_BLOCK, Blocks.GOLD_BLOCK, Blocks.DIAMOND_BLOCK,
            Blocks.EMERALD_BLOCK, Blocks.NETHERITE_BLOCK
    );

    @Override
    public Classification classify(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getCollisionShape(level, pos).isEmpty()) {
            return Classification.NONE;
        }

        Block block = state.getBlock();

        // 1) Check custom tags (datapack-extensible)
        if (level instanceof net.minecraft.world.level.Level realLevel) {
            var tagKey = net.minecraft.tags.TagKey.create(
                    net.minecraft.core.registries.Registries.BLOCK, ANCHOR_ID);
            if (realLevel.isLoaded(pos) && state.is(tagKey)) {
                return Classification.ANCHOR;
            }
            tagKey = net.minecraft.tags.TagKey.create(
                    net.minecraft.core.registries.Registries.BLOCK, STRUCTURAL_ID);
            if (realLevel.isLoaded(pos) && state.is(tagKey)) {
                return Classification.STRUCTURAL;
            }
        }

        // 2) Hardcoded fallback
        if (DEFAULT_ANCHORS.contains(block))   return Classification.ANCHOR;
        if (DEFAULT_STRUCTURAL.contains(block)) return Classification.STRUCTURAL;

        return Classification.NONE;
    }
}
