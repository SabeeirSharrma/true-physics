package com.sabcancode.truephysics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sabcancode.truephysics.config.PhysicsConfigHolder;
import com.sabcancode.truephysics.mixin.EntityAccessor;
import com.sabcancode.truephysics.mixin.ItemStackRenderStateAccessor;
import com.sabcancode.truephysics.mixin.LayerRenderStateAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import team.creative.creativecore.CreativeCore;

/**
 * Client-side physics calculations and rendering for item entities.
 * Handles both rotation calculations and the custom submit() pipeline.
 * Adapted from CreativeMD's ItemPhysic.
 */
public final class ClientPhysic {

    private static final float BASE_MULTIPLIER = 0.25F;
    private static final Minecraft MC = Minecraft.getInstance();
    private static final double RANDOM_Y_OFFSET_SCALE = 0.05 / (Math.PI * 2);

    private ClientPhysic() {}

    /**
     * Calculate and apply rotation to an item entity for visual rendering.
     * Called during extractRenderState to feed data into the render pipeline.
     */
    public static void calculateRotation(ItemEntity entity, ItemEntityRenderState state) {
        if (!PhysicsConfigHolder.isLoaded() || !PhysicsConfigHolder.get().itemPhysicsEnabled) return;

        float rotateBy = MC.getDeltaTracker().getRealtimeDeltaTicks() * BASE_MULTIPLIER;
        if (MC.isPaused()) rotateBy = 0;

        // Slow down in cobwebs/powder snow
        Vec3 stuck = ((EntityAccessor) entity).getStuckSpeedMultiplier();
        if (stuck != null && stuck.lengthSqr() > 0) {
            rotateBy *= (float) stuck.x * 0.2F;
        }

        boolean isBlock = ((ItemEntityRenderStateExtender) state).isBlock();

        if (!entity.onGround()) {
            // In air — tumble
            rotateBy *= 2;
            Fluid fluid = detectFluid(entity);
            if (fluid != null) {
                float viscosity = CreativeCore.loader().getFluidViscosityMultiplier(fluid, entity.level());
                rotateBy /= (1 + viscosity);
            }
            entity.setXRot(entity.getXRot() + rotateBy);
        } else {
            if (isBlock) {
                // Block items on ground — snap to nearest 90°
                float current = entity.getXRot();
                float diff0 = Math.abs(current);
                float diff90 = Math.abs(current - 90);
                float diff180 = Math.abs(current - 180);
                float diff270 = Math.abs(current + 90);

                float nearest = 0;
                if (diff90 < diff0 && diff90 <= diff180 && diff90 <= diff270) nearest = 90;
                else if (diff180 < diff90 && diff180 <= diff270) nearest = 180;
                else if (diff270 < diff180 && diff270 < diff0) nearest = -90;

                float diff = nearest - current;
                if (Math.abs(diff) > 0.5F) {
                    entity.setXRot(current + diff * 0.2F);
                } else {
                    entity.setXRot(nearest);
                }
            } else {
                // Non-block items on ground — lay flat
                float current = entity.getXRot();
                if (Math.abs(current) > 0.5F) {
                    entity.setXRot(current * 0.8F);
                } else {
                    entity.setXRot(0);
                }
            }
        }
    }

    /**
     * Custom submit() implementation that renders items with physics-based rotation.
     * Returns true if custom rendering was applied, false to fall back to vanilla.
     */
    public static boolean submit(ItemEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera, RandomSource rand) {
        if (!PhysicsConfigHolder.isLoaded() || !PhysicsConfigHolder.get().itemPhysicsEnabled) return false;
        if (state.ageInTicks < 1) return false;
        if (((ItemEntityRenderStateExtender) state).skipRendering()) return false;

        pose.pushPose();

        rand.setSeed(state.seed);
        int j = getModelCount(state.count);
        boolean gui3d = ((ItemEntityRenderStateExtender) state).isBlock();

        var transform = ((LayerRenderStateAccessor) ((ItemStackRenderStateAccessor) state.item).callFirstLayer()).getItemTransform();

        // Rotate to lie flat (XP rotation by PI/2)
        pose.mulPose(com.mojang.math.Axis.XP.rotation((float) Math.PI / 2));
        // Apply Y rotation
        pose.mulPose(com.mojang.math.Axis.ZP.rotation(((ItemEntityRenderStateExtender) state).getYRot()));

        var mc = Minecraft.getInstance();

        if (state.ageInTicks != 0 && (gui3d || mc.options != null)) {
            if (gui3d) {
                pose.translate(0, -0.2, -0.08);
            } else if (((ItemEntityRenderStateExtender) state).hasAdditionalOffset()) {
                pose.translate(0, 0.0, -0.14 - state.bobOffset * RANDOM_Y_OFFSET_SCALE);
            } else {
                pose.translate(0, 0, -0.04 - state.bobOffset * RANDOM_Y_OFFSET_SCALE);
            }

            double height = transform.scale().y();
            if (gui3d) {
                pose.translate(0, height, 0);
            }
            pose.mulPose(com.mojang.math.Axis.YP.rotation(((ItemEntityRenderStateExtender) state).getXRot()));
            if (gui3d) {
                pose.translate(0, -height, 0);
            }
        }

        if (!gui3d) {
            float f7 = -0.0F * (j - 1) * 0.5F;
            float f8 = -0.0F * (j - 1) * 0.5F;
            float f9 = -0.09375F * (j - 1) * 0.5F;
            pose.translate(f7, f8, f9);
        }

        float f = transform.scale().x();
        float f1 = transform.scale().y();
        float f2 = transform.scale().z();

        for (int k = 0; k < j; ++k) {
            pose.pushPose();
            if (k > 0) {
                if (gui3d) {
                    float f11 = (rand.nextFloat() * 2.0F - 1.0F) * f;
                    float f13 = (rand.nextFloat() * 2.0F - 1.0F) * f1;
                    float f10 = (rand.nextFloat() * 2.0F - 1.0F) * f2;
                    pose.translate(f11, f13, f10);
                }
            }

            state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            pose.popPose();
            if (!gui3d) {
                pose.translate(0.0F * f, 0.0F * f1, 0.09375F * f2);
            }
        }

        pose.popPose();
        return true;
    }

    /**
     * Get model count based on item stack count (for rendering multiple items).
     */
    public static int getModelCount(int count) {
        if (count > 48) return 5;
        if (count > 32) return 4;
        if (count > 16) return 3;
        if (count > 1) return 2;
        return 1;
    }

    /**
     * Detect which fluid the item is in (if any).
     */
    private static Fluid detectFluid(ItemEntity item) {
        if (item.level() == null) return null;

        var state = item.level().getFluidState(item.blockPosition());
        var fluid = state.getType();
        if (state.isEmpty() || fluid == null) return null;

        double filled = state.getHeight(item.level(), item.blockPosition());
        double itemY = item.getY() - item.blockPosition().getY();
        if (itemY - 0.2 <= filled) return fluid;
        return null;
    }
}
