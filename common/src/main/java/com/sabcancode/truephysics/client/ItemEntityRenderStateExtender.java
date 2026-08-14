package com.sabcancode.truephysics.client;

import net.minecraft.world.entity.item.ItemEntity;

/**
 * Interface injected onto ItemEntityRenderState via mixin
 * to carry rotation/physics data from entity to renderer.
 */
public interface ItemEntityRenderStateExtender {
    float getXRot();
    float getYRot();
    boolean skipRendering();
    boolean isBlock();
    boolean hasAdditionalOffset();
    void extractPhysic(ItemEntity entity);
}
