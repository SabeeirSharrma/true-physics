package com.sabcancode.truephysics.registry;

import com.sabcancode.truephysics.TruePhysics;
import com.sabcancode.truephysics.block.SupportBeamBlock;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;

/**
 * Block and item registration for True Physics.
 * Uses Architectury's {@link DeferredRegister} for cross-platform compat.
 */
public final class ModBlocks {

    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(TruePhysics.MOD_ID, Registries.BLOCK);

    // ─── M2: Support Beam ──────────────────────────────────────────
    public static final RegistrySupplier<Block> SUPPORT_BEAM =
            BLOCKS.register("support_beam", SupportBeamBlock::new);

    /** Call from each platform entrypoint to register blocks. */
    public static void register() {
        BLOCKS.register();
    }
}
