package com.sabcancode.truephysics.core.classifier;

import com.sabcancode.truephysics.config.PhysicsConfig;
import com.sabcancode.truephysics.config.PhysicsConfigHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

/**
 * Classifies blocks by checking custom {@code true_physics:structural},
 * {@code true_physics:anchor}, and {@code true_physics:adhesive} tags
 * first, falling back to a sensible hardcoded default set.
 *
 * <p><b>Modded block support:</b> Any mod's blocks can be made structural,
 * anchor, or adhesive by adding them to the datapack tags:
 * <ul>
 *   <li>{@code data/true_physics/tags/blocks/structural.json}</li>
 *   <li>{@code data/true_physics/tags/blocks/anchor.json}</li>
 *   <li>{@code data/true_physics/tags/blocks/adhesive.json}</li>
 * </ul>
 * Common convention tags ({@code c:stone}, {@code c:deepslate}) are also
 * checked for structural blocks.</p>
 */
public final class TagBlockClassifier implements BlockClassifier {

    private static final Identifier STRUCTURAL_ID =
            Identifier.fromNamespaceAndPath("true_physics", "structural");
    private static final Identifier ANCHOR_ID =
            Identifier.fromNamespaceAndPath("true_physics", "anchor");
    private static final Identifier ADHESIVE_ID =
            Identifier.fromNamespaceAndPath("true_physics", "adhesive");

    // Common convention tags for modded block compat
    private static final Identifier C_STONE_ID =
            Identifier.fromNamespaceAndPath("c", "stone");
    private static final Identifier C_DEEPSLATE_ID =
            Identifier.fromNamespaceAndPath("c", "deepslate");

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

    private static final Set<Block> DEFAULT_ADHESIVES = Set.of(
            Blocks.SLIME_BLOCK,
            Blocks.HONEY_BLOCK
    );

    @Override
    public Classification classify(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getCollisionShape(level, pos).isEmpty()) {
            return Classification.NONE;
        }

        // 1) Check custom tags (datapack-extensible — covers ALL blocks, vanilla + modded)
        if (level instanceof Level realLevel) {
            if (realLevel.isLoaded(pos)) {
                // Anchor tags (checked first — highest priority)
                TagKey<Block> anchorKey = TagKey.create(Registries.BLOCK, ANCHOR_ID);
                if (state.is(anchorKey)) return Classification.ANCHOR;

                // Adhesive tags (checked before structural — adhesive is more specific)
                TagKey<Block> adhesiveKey = TagKey.create(Registries.BLOCK, ADHESIVE_ID);
                if (state.is(adhesiveKey)) {
                    // If adhesives are disabled, treat as structural instead
                    PhysicsConfig cfg = PhysicsConfigHolder.isLoaded() ? PhysicsConfigHolder.get() : null;
                    if (cfg != null && cfg.adhesivesEnabled) {
                        return Classification.ADHESIVE;
                    } else {
                        return Classification.STRUCTURAL;
                    }
                }

                // Structural tags
                TagKey<Block> structuralKey = TagKey.create(Registries.BLOCK, STRUCTURAL_ID);
                if (state.is(structuralKey)) return Classification.STRUCTURAL;

                // Common convention tags (modded block compat)
                TagKey<Block> cStoneKey = TagKey.create(Registries.BLOCK, C_STONE_ID);
                if (state.is(cStoneKey)) return Classification.STRUCTURAL;

                TagKey<Block> cDeepslateKey = TagKey.create(Registries.BLOCK, C_DEEPSLATE_ID);
                if (state.is(cDeepslateKey)) return Classification.STRUCTURAL;
            }
        }

        // 2) Hardcoded fallback (only when tags aren't loaded yet)
        Block block = state.getBlock();
        if (DEFAULT_ANCHORS.contains(block))   return Classification.ANCHOR;
        if (DEFAULT_ADHESIVES.contains(block)) {
            PhysicsConfig cfg = PhysicsConfigHolder.isLoaded() ? PhysicsConfigHolder.get() : null;
            if (cfg != null && cfg.adhesivesEnabled) {
                return Classification.ADHESIVE;
            } else {
                return Classification.STRUCTURAL;
            }
        }
        if (DEFAULT_STRUCTURAL.contains(block)) return Classification.STRUCTURAL;

        return Classification.NONE;
    }
}
