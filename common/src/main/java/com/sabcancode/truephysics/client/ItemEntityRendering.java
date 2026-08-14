package com.sabcancode.truephysics.client;

/**
 * Interface injected onto ItemEntity via mixin to control vanilla rendering bypass.
 */
public interface ItemEntityRendering {
    boolean skipRendering();
}
