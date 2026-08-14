package com.sabcancode.truephysics.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Support Beam — a craftable block that acts as an anchor in the
 * structural-integrity system.
 *
 * <p>Players place support beams to prevent collapse. Any structural
 * block connected to a support beam (directly or via adhesive) stays
 * supported. Breaking a support beam triggers re-evaluation of all
 * connected blocks.</p>
 *
 * <p>The 3D model will be designed in Blockbench — this class uses
 * the default cube shape until the custom model is imported.</p>
 */
public final class SupportBeamBlock extends Block {

    public SupportBeamBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .requiresCorrectToolForDrops()
                .strength(5.0F, 6.0F)
                .sound(SoundType.NETHERITE_BLOCK)
                .lightLevel(state -> 7)
        );
    }
}
