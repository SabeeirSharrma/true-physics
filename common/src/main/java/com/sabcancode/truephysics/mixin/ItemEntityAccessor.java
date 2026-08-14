package com.sabcancode.truephysics.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemEntity.class)
public interface ItemEntityAccessor {
    @Invoker
    void callSetUnderwaterMovement();

    @Invoker
    void callSetUnderLavaMovement();
}
