package com.sabcancode.truephysics.core.item;

import net.minecraft.world.level.material.Fluid;

/**
 * Duck interface injected onto {@link net.minecraft.world.entity.item.ItemEntity}
 * via Mixin. Stores the fluid state for the current tick so physics logic
 * can reference it without re-querying the level.
 */
public interface ItemEntityExtender {

    Fluid truePhysics$getFluid();

    void truePhysics$setFluid(Fluid fluid);

    boolean truePhysics$canSwim();

    boolean truePhysics$canBurn();
}
