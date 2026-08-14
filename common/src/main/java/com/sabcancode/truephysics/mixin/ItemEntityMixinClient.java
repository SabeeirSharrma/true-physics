package com.sabcancode.truephysics.mixin;

import com.sabcancode.truephysics.client.ItemEntityRendering;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Client-side mixin into {@link ItemEntity} to control vanilla rendering bypass.
 * Adds the skipRendering flag used by ItemEntityRendering interface.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixinClient extends Entity implements ItemEntityRendering {

    @Unique
    public boolean truePhysics$skipPhysicRenderer;

    protected ItemEntityMixinClient(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean skipRendering() {
        return truePhysics$skipPhysicRenderer;
    }
}
