package com.sabcancode.truephysics.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor("stuckSpeedMultiplier")
    Vec3 getStuckSpeedMultiplier();

    @Invoker
    BlockPos callGetBlockPosBelowThatAffectsMyMovement();
}
