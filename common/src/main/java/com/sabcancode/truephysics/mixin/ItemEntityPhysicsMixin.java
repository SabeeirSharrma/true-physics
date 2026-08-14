package com.sabcancode.truephysics.mixin;

import com.sabcancode.truephysics.config.PhysicsConfigHolder;
import com.sabcancode.truephysics.core.item.ItemEntityExtender;
import com.sabcancode.truephysics.core.item.ItemPhysicsEngine;
import com.sabcancode.truephysics.core.item.ItemPhysicsRegistry;
import net.minecraft.world.damagesource.DamageSource;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into {@link ItemEntity} — the heart of True Physics item physics.
 *
 * <p>Disables vanilla's water/lava/gravity handling and replaces it with
 * our custom physics engine. Pattern adapted from CreativeMD's ItemPhysic.</p>
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityPhysicsMixin extends Entity implements ItemEntityExtender {

    @Unique
    private Fluid truePhysics$fluid;

    @Unique
    private boolean truePhysics$swim;

    @Unique
    private boolean truePhysics$burn;

    /** Whether this entity was admitted to the active cap this tick. */
    @Unique
    private boolean truePhysics$active;

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

    // ─── Swim/burn flag updates ──────────────────────────────────────

    /**
     * Refresh swim/burn flags on every server tick.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void truePhysics$refreshFlags(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        Level level = self.level();
        if (level.isClientSide()) return;
        if (!PhysicsConfigHolder.isLoaded()) return;
        if (!PhysicsConfigHolder.get().itemPhysicsEnabled) return;

        truePhysics$swim = ItemPhysicsRegistry.canSwim(self);
        truePhysics$burn = ItemPhysicsRegistry.isUndestroyable(self);
        truePhysics$active = false;
    }

    // ─── Pre-tick: gravity + buoyancy ────────────────────────────────

    @Inject(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/item/ItemEntity;isInWater()Z"))
    private void truePhysics$updatePre(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        Level level = self.level();
        if (level.isClientSide()) return;
        if (!PhysicsConfigHolder.isLoaded()) return;
        if (!PhysicsConfigHolder.get().itemPhysicsEnabled) return;

        truePhysics$active = ItemPhysicsEngine.preTick(self);
    }

    // ─── Disable vanilla water/lava/gravity ──────────────────────────

    @Redirect(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/item/ItemEntity;isInWater()Z"))
    private boolean truePhysics$isInWaterRedirect(ItemEntity entity) {
        return false;
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/item/ItemEntity;isInLava()Z"))
    private boolean truePhysics$isInLavaRedirect(ItemEntity entity) {
        return false;
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/item/ItemEntity;applyGravity()V"))
    private void truePhysics$isNoGravityRedirect(ItemEntity entity) {
        // No-op: gravity handled in updatePre
    }

    // ─── Post-tick: friction + bounce + slope + rest ─────────────────

    @Inject(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/item/ItemEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
        ordinal = 0))
    private void truePhysics$update(CallbackInfo ci) {
        if (!truePhysics$active) return; // Over budget — skip post-tick

        ItemEntity self = (ItemEntity) (Object) this;
        Level level = self.level();
        if (level.isClientSide()) return;

        ItemPhysicsEngine.postTick(self);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/item/ItemEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private void truePhysics$setDeltaMovementRedirect(ItemEntity entity, Vec3 vec) {
        // No-op
    }

    // ─── Undestroyable items: fire/lava/damage immunity ──────────────

    @Inject(method = "fireImmune()Z", at = @At("HEAD"), cancellable = true)
    private void truePhysics$fireImmuneInject(CallbackInfoReturnable<Boolean> cir) {
        if (truePhysics$burn) {
            cir.setReturnValue(true);
        }
    }

    // Note: lavaHurt() lives on Entity, not ItemEntity — but Entity.lavaHurt()
    // already calls fireImmune() first, so our fireImmune override above handles it.

    @Inject(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At("HEAD"), cancellable = true)
    private void truePhysics$hurtServerInject(net.minecraft.server.level.ServerLevel level,
                                              DamageSource source, float amount,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (truePhysics$burn) {
            cir.setReturnValue(false);
        }
    }
}
