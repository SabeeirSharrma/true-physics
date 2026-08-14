package com.sabcancode.truephysics.mixin;

import com.sabcancode.truephysics.client.ItemEntityRenderStateExtender;
import com.sabcancode.truephysics.client.ClientPhysic;
import com.sabcancode.truephysics.config.PhysicsConfigHolder;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Mixin into {@link ItemEntityRenderState} to carry physics rotation data.
 * Implements {@link ItemEntityRenderStateExtender} so the renderer can
 * read rotation values that were computed during extractRenderState.
 */
@Mixin(ItemEntityRenderState.class)
public class ItemEntityRenderStateMixin implements ItemEntityRenderStateExtender {

    @Unique
    private float truePhysics$rotX;

    @Unique
    private float truePhysics$rotY;

    @Unique
    private boolean truePhysics$isBlock;

    @Unique
    private boolean truePhysics$additionalOffset;

    @Override
    public float getXRot() {
        return truePhysics$rotX;
    }

    @Override
    public float getYRot() {
        return truePhysics$rotY;
    }

    @Override
    public boolean skipRendering() {
        return false;
    }

    @Override
    public boolean isBlock() {
        return truePhysics$isBlock;
    }

    @Override
    public boolean hasAdditionalOffset() {
        return truePhysics$additionalOffset;
    }

    @Override
    public void extractPhysic(ItemEntity entity) {
        ItemEntityRenderState state = (ItemEntityRenderState) (Object) this;
        truePhysics$isBlock = state.item.usesBlockLight();

        // Calculate rotation based on physics
        ClientPhysic.calculateRotation(entity, state);

        // Check if block below requires additional offset
        truePhysics$additionalOffset = false;
        if (entity.level() != null) {
            var blockState = entity.level().getBlockState(entity.blockPosition());
            if (blockState.is(net.minecraft.world.level.block.Blocks.SNOW) ||
                blockState.is(net.minecraft.world.level.block.Blocks.SOUL_SAND) ||
                blockState.is(net.minecraft.world.level.block.Blocks.MUD)) {
                truePhysics$additionalOffset = true;
            }
        }

        // Store computed rotations
        truePhysics$rotX = entity.getXRot();
        truePhysics$rotY = entity.getYRot();
    }
}
