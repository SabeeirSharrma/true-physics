package com.sabcancode.truephysics.core.item;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Parsed runtime lookup for item physics config lists
 * (swimming items, undestroyable items).
 *
 * <p>Config strings are comma-separated item IDs. This class parses them
 * once at config load and provides O(1) lookup via {@link Set}.</p>
 */
public final class ItemPhysicsRegistry {

    private static volatile Set<Item> swimmingItems = Collections.emptySet();
    private static volatile Set<Item> undestroyableItems = Collections.emptySet();

    private ItemPhysicsRegistry() {}

    /**
     * Parse and apply swimming-item list from config string.
     * Called when config is loaded or changed.
     */
    public static void setSwimmingItems(String csv) {
        swimmingItems = parseItems(csv);
    }

    /**
     * Parse and apply undestroyable-item list from config string.
     * Called when config is loaded or changed.
     */
    public static void setUndestroyableItems(String csv) {
        undestroyableItems = parseItems(csv);
    }

    /** Can this item entity float/swim upward in water? */
    public static boolean canSwim(ItemEntity entity) {
        return swimmingItems.contains(entity.getItem().getItem());
    }

    /** Is this item immune to fire/lava damage? */
    public static boolean isUndestroyable(ItemEntity entity) {
        return undestroyableItems.contains(entity.getItem().getItem());
    }

    /** Parse a comma-separated string of item IDs into a set. */
    private static Set<Item> parseItems(String csv) {
        if (csv == null || csv.isBlank()) return Collections.emptySet();

        // Registries may not be available (e.g. unit tests) — gracefully degrade
        try {
            return parseItems0(csv);
        } catch (Throwable t) {
            return Collections.emptySet();
        }
    }

    private static Set<Item> parseItems0(String csv) {
        Set<Item> result = new HashSet<>();
        for (String entry : csv.split(",")) {
            String id = entry.trim();
            if (id.isEmpty()) continue;

            // Try as namespaced (mod:item) first, then minecraft:item
            Identifier loc;
            if (id.contains(":")) {
                loc = Identifier.tryParse(id);
            } else {
                loc = Identifier.tryBuild("minecraft", id);
            }
            if (loc == null) continue;

            // Look up in the item registry
            var opt = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(loc);
            Item item = opt.map(ref -> ref.value()).orElse(null);
            if (item != null && item != Items.AIR) {
                result.add(item);
            }
        }
        return Collections.unmodifiableSet(result);
    }
}
