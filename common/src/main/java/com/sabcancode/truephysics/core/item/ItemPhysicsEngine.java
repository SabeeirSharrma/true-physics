package com.sabcancode.truephysics.core.item;

import com.sabcancode.truephysics.config.PhysicsConfig;
import com.sabcancode.truephysics.config.PhysicsConfigHolder;
import com.sabcancode.truephysics.mixin.EntityAccessor;
import com.sabcancode.truephysics.mixin.ItemEntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import team.creative.creativecore.CreativeCore;

/**
 * Centralized item physics engine — the single source of truth for
 * all item physics calculations. Called by the Mixin.
 *
 * <p>Split into two phases (matching ItemPhysic's architecture):</p>
 * <ul>
 *   <li><b>preTick</b> — gravity + fluid buoyancy (before vanilla movement)</li>
 *   <li><b>postTick</b> — friction + bounce + slope + rest detection (after vanilla movement)</li>
 * </ul>
 *
 * <p>Delegates friction and viscosity to CreativeCore's APIs for
 * full compatibility with modded blocks and fluids.</p>
 */
public final class ItemPhysicsEngine {

    private ItemPhysicsEngine() {}

    // ─── Pre-tick: gravity + buoyancy ───────────────────────────────

    /**
     * Called before vanilla's tick logic. Applies gravity and fluid forces.
     */
    public static void preTick(ItemEntity item) {
        PhysicsConfig cfg = PhysicsConfigHolder.get();

        // Detect fluid
        Fluid fluid = detectFluid(item);
        ((ItemEntityExtender) item).truePhysics$setFluid(fluid);

        if (fluid == null) {
            // No fluid — apply normal gravity
            if (!item.isNoGravity()) {
                item.setDeltaMovement(
                    item.getDeltaMovement().add(0, -item.getGravity(), 0)
                );
            }
            return;
        }

        // In fluid — apply buoyancy
        float viscosity = CreativeCore.loader().getFluidViscosityMultiplier(fluid, item.level());
        double force = -0.02 / Math.max(1, viscosity);

        boolean isSwimming = ((ItemEntityExtender) item).truePhysics$canSwim();

        if (isSwimming && !fluid.is(FluidTags.LAVA)) {
            // Swimming items float upward (wood, boats, etc.)
            double maxSpeed = 0.1;
            if (item.getDeltaMovement().y < maxSpeed) {
                force = Math.min(item.getGravity(), maxSpeed - item.getDeltaMovement().y);
            }
        } else if (item.getDeltaMovement().y < -0.1) {
            // Non-swimming items moving down fast — apply drag
            force = 0;
            item.setDeltaMovement(
                item.getDeltaMovement().multiply(1, 0.8, 1)
            );
        }

        item.setDeltaMovement(item.getDeltaMovement().add(0, force, 0));
    }

    // ─── Post-tick: friction + bounce + slope + rest ─────────────────

    /**
     * Called after vanilla's tick logic. Applies friction, bouncing,
     * slope sliding, and rest detection.
     */
    public static void postTick(ItemEntity item) {
        PhysicsConfig cfg = PhysicsConfigHolder.get();
        Fluid fluid = ((ItemEntityExtender) item).truePhysics$getFluid();

        if (fluid == null) {
            // On land — apply friction via CreativeCore
            float friction = 0.98F;
            if (item.onGround()) {
                BlockPos posBelow = ((EntityAccessor) item).callGetBlockPosBelowThatAffectsMyMovement();
                float blockFriction = CreativeCore.loader().getFriction(item.level(), posBelow, item);
                friction = blockFriction * 0.98F;
            }

            // Apply friction scale from config
            friction = 1.0F - ((1.0F - friction) * cfg.itemPhysicsFrictionScale);

            item.setDeltaMovement(
                item.getDeltaMovement().multiply(friction, 0.98, friction)
            );

            // Bounce on landing
            if (item.onGround() && item.getDeltaMovement().y < 0) {
                float bounceFactor = getBounceFactor(item);
                float finalBounce = bounceFactor * cfg.itemPhysicsBounceFactor;
                item.setDeltaMovement(
                    item.getDeltaMovement().multiply(1.0, -finalBounce, 1.0)
                );
            }

            // Slope sliding
            applySlopeSliding(item);

        } else {
            // In fluid — apply drag via CreativeCore
            if (cfg.itemPhysicsVanillaFlow) {
                // Use vanilla flow behavior
                if (item.isInWater() && item.getFluidHeight(FluidTags.WATER) > 0.1F) {
                    ((ItemEntityAccessor) item).callSetUnderwaterMovement();
                } else if (item.isInLava() && item.getFluidHeight(FluidTags.LAVA) > 0.1F) {
                    ((ItemEntityAccessor) item).callSetUnderLavaMovement();
                }
            } else {
                float viscosity = CreativeCore.loader().getFluidViscosityMultiplier(fluid, item.level());
                item.setDeltaMovement(
                    item.getDeltaMovement().multiply(
                        1.0 / (1.2 * viscosity), 1.0, 1.0 / (1.2 * viscosity)
                    )
                );
            }
        }

        // Rest detection
        applyRestDetection(item, cfg);
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    /**
     * Detect which fluid the item is in (if any).
     */
    private static Fluid detectFluid(ItemEntity item) {
        Level level = item.level();
        if (level == null) return null;

        BlockPos pos = item.blockPosition();
        FluidState state = level.getFluidState(pos);
        Fluid fluid = state.getType();

        if (state.isEmpty() || fluid == null) return null;

        // Check if item is near the fluid surface
        double filled = state.getHeight(level, pos);
        double itemY = item.getY() - pos.getY();

        if (itemY - 0.2 <= filled) {
            return fluid;
        }
        return null;
    }

    /**
     * Get bounce factor based on the block below.
     */
    private static float getBounceFactor(ItemEntity item) {
        BlockPos pos = ((EntityAccessor) item).callGetBlockPosBelowThatAffectsMyMovement();
        var block = item.level().getBlockState(pos).getBlock();

        if (block == Blocks.SLIME_BLOCK) return 0.8F;
        if (block == Blocks.HONEY_BLOCK) return 0.1F;
        if (block == Blocks.BEDROCK) return 0.0F;
        return 0.3F; // Default bounce
    }

    /**
     * Apply slope sliding on stairs and slabs.
     */
    private static void applySlopeSliding(ItemEntity entity) {
        BlockPos pos = ((EntityAccessor) entity).callGetBlockPosBelowThatAffectsMyMovement();
        BlockState blockBelow = entity.level().getBlockState(pos);

        boolean isStair = blockBelow.getBlock() instanceof StairBlock;
        boolean isSlab = false;

        if (blockBelow.getBlock() instanceof SlabBlock slab) {
            var typeProp = slab.getStateDefinition().getProperty("type");
            if (typeProp != null) {
                var typeVal = blockBelow.getValue(typeProp);
                isSlab = !typeVal.toString().equals("double");
            }
        }

        if (isStair || isSlab) {
            double vy = entity.getDeltaMovement().y;
            if (vy > -0.15) {
                entity.setDeltaMovement(
                    entity.getDeltaMovement().x,
                    vy - 0.04,
                    entity.getDeltaMovement().z
                );
            }
        }
    }

    /**
     * Apply rest detection — items with near-zero velocity go to sleep.
     */
    private static void applyRestDetection(ItemEntity item, PhysicsConfig cfg) {
        double horizontalSpeed = Math.abs(item.getDeltaMovement().x)
                               + Math.abs(item.getDeltaMovement().z);
        double verticalSpeed = Math.abs(item.getDeltaMovement().y);

        if (horizontalSpeed < 0.01 && verticalSpeed < 0.01 && item.onGround()) {
            if (horizontalSpeed < 0.005 && verticalSpeed < 0.005) {
                item.setDeltaMovement(0, 0, 0);
            }
        }
    }
}
