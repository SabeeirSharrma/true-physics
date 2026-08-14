package com.sabcancode.truephysics.registry;

import com.sabcancode.truephysics.TruePhysics;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

/**
 * Item registration for True Physics.
 * Uses Architectury's {@link DeferredRegister} for cross-platform compat.
 */
public final class ModItems {

    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(TruePhysics.MOD_ID, Registries.ITEM);

    // ─── M2: Support Beam ──────────────────────────────────────────
    public static final RegistrySupplier<Item> SUPPORT_BEAM =
            ITEMS.register("support_beam", () -> new BlockItem(
                    ModBlocks.SUPPORT_BEAM.get(),
                    new Item.Properties()
                            .setId(ResourceKey.create(Registries.ITEM,
                                    Identifier.tryBuild(TruePhysics.MOD_ID, "support_beam")))
            ));

    /** Call from each platform entrypoint to register items. */
    public static void register() {
        ITEMS.register();
    }
}
