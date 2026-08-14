package com.sabcancode.truephysics.fabric;

import com.sabcancode.truephysics.TruePhysics;
import com.sabcancode.truephysics.config.PhysicsConfig;
import com.sabcancode.truephysics.platform.true_physics.ConfigLoaderImpl;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class TruePhysicsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        PhysicsConfig config = ConfigLoaderImpl.load(FabricLoader.getInstance().getConfigDir());
        TruePhysics.init(config);
    }
}
