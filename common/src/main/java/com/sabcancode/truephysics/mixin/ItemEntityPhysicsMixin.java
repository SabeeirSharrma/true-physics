package com.sabcancode.truephysics.mixin;

import com.sabcancode.truephysics.config.PhysicsConfigHolder;
import com.sabcancode.truephysics.core.item.ItemEntityExtender;
import com.sabcancode.truephysics.core.item.ItemPhysicsEngine;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link ItemEntity} — the heart of True Physics item physics.
 *
 * <p>Disables vanilla's water/lava/gravity handling and replaces it with
 * our custom physics engine. Pattern adapted from CreativeMD's ItemPhysic.</p>
 *
 * <p>Injection points:</p>
 * <ul>
 *   <li>{@code updatePre} — HEAD of tick, before vanilla movement</li>
 *   <li>{@code update} — before first setDeltaMovement in tick</li>
 *   <li>{@code isInWaterRedirect} — returns false to disable vanilla water</li>
 *   <li>{@code isInLavaRedirect} — returns false to disable vanilla lava</li>
 *   <li>{@code isNoGravityRedirect} — no-ops vanilla gravity</li>
 *   <li>{@code setDeltaMovementRedirect} — prevents vanilla from overwriting our movement</li>
 * </ul>
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityPhysicsMixin extends Entity implements ItemEntityExtender {

    @Unique
    private Fluid truePhysics$fluid;

    @Unique
    private boolean truePhysics$swim;

    @Unique
    private boolean truePhysics$burn;

    @Unique
    private int truePhysics$ticksSinceRest = 0;

    private ItemEntityPhysicsMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    // ─── Duck interface implementation ───────────────────────────────

    @Override
    public Fluid truePhysics$getFluid() {
        return truePhysics$fluid;
    }

    @Override
    public void truePhysics$setFluid(Fluid fluid) {
        this.truePhysics$fluid = fluid;
    }

    @Override
    public boolean truePhysics$canSwim() {
        return truePhysics$swim;
    }

    @Override
    public boolean truePhysics$canBurn() {
        return truePhysics$burn;
    }

    // ─── Pre-tick: gravity + buoyancy ────────────────────────────────

    /**
     * Inject before vanilla's water check to apply our gravity/buoyancy.
     */
    @Inject(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/item/ItemEntity;isInWater()Z"))
    private void truePhysics$updatePre(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        Level level = self.level();
        if (level.isClientSide()) return;
        if (!PhysicsConfigHolder.isLoaded()) return;
        if (!PhysicsConfigHolder.get().itemPhysicsEnabled) return;

        ItemPhysicsEngine.preTick(self);
    }

    // ─── Disable vanilla water/lava/gravity ──────────────────────────

    /**
     * Redirect isInWater() to always return false — we handle water ourselves.
     */
    @Redirect(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/item/ItemEntity;isInWater()Z"))
    private boolean truePhysics$isInWaterRedirect(ItemEntity entity) {
        return false;
    }

    /**
     * Redirect isInLava() to always return false — we handle lava ourselves.
     */
    @Redirect(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/item/ItemEntity;isInLava()Z"))
    private boolean truePhysics$isInLavaRedirect(ItemEntity entity) {
        return false;
    }

    /**
     * Redirect applyGravity() to no-op — we handle gravity ourselves.
     */
    @Redirect(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/item/ItemEntity;applyGravity()V"))
    private void truePhysics$isNoGravityRedirect(ItemEntity entity) {
        // No-op: gravity handled in updatePre
    }

    // ─── Post-tick: friction + bounce + slope + rest ─────────────────

    /**
     * Inject before the first setDeltaMovement to apply our friction/drag.
     */
    @Inject(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/item/ItemEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
        ordinal = 0))
    private void truePhysics$update(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        Level level = self.level();
        if (level.isClientSide()) return;
        if (!PhysicsConfigHolder.isLoaded()) return;
        if (!PhysicsConfigHolder.get().itemPhysicsEnabled) return;

        ItemPhysicsEngine.postTick(self);
    }

    /**
     * Redirect all setDeltaMovement calls to prevent vanilla from overwriting
     * our physics calculations.
     */
    @Redirect(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/item/ItemEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private void truePhysics$setDeltaMovementRedirect(ItemEntity entity, Vec3 vec) {
        // No-op: vanilla's movement is already handled by our engine
    }
}
