package com.sabcancode.truephysics.neoforge;

import com.sabcancode.truephysics.TruePhysics;
import com.sabcancode.truephysics.config.PhysicsConfig;
import com.sabcancode.truephysics.platform.true_physics.ConfigLoaderImpl;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(TruePhysics.MOD_ID)
public final class TruePhysicsNeoForge {
    public TruePhysicsNeoForge(ModContainer modContainer) {
        // Register the config spec so NeoForge loads/saves the file
        modContainer.registerConfig(
                net.neoforged.fml.config.ModConfig.Type.COMMON,
                ConfigLoaderImpl.getSpec()
        );
        // Build our data object from the spec values
        PhysicsConfig config = ConfigLoaderImpl.load();
        TruePhysics.init(config);
    }
}
