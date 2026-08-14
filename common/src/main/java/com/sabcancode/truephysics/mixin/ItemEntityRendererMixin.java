package com.sabcancode.truephysics.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sabcancode.truephysics.client.ClientPhysic;
import com.sabcancode.truephysics.client.ItemEntityRenderStateExtender;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link ItemEntityRenderer} to inject physics-based rotation
 * and custom rendering during the submit phase. Pattern adapted from
 * CreativeMD's ItemPhysic.
 */
@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin extends EntityRenderer<ItemEntity, ItemEntityRenderState> {

    @Shadow
    @Final
    private RandomSource random;

    protected ItemEntityRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    /**
     * Intercept the submit() method to apply physics-based rendering.
     * If our custom renderer handles it, cancel vanilla rendering.
     */
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void truePhysics$submit(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        if (ClientPhysic.submit(state, poseStack, collector, camera, this.random)) {
            super.submit(state, poseStack, collector, camera);
            ci.cancel();
        }
    }

    /**
     * After vanilla extracts render state, inject our physics rotation.
     */
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V",
            at = @At("TAIL"), require = 1)
    private void truePhysics$injectExtract(ItemEntity item, ItemEntityRenderState state, float partialTicks, CallbackInfo info) {
        ((ItemEntityRenderStateExtender) state).extractPhysic(item);
    }
}
