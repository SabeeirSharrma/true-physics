package com.sabcancode.truephysics.fabric;

import com.sabcancode.truephysics.TruePhysics;
import com.sabcancode.truephysics.config.PhysicsConfig;
import com.sabcancode.truephysics.config.PhysicsConfigHolder;
import com.sabcancode.truephysics.core.TruePhysicsCore;
import com.sabcancode.truephysics.platform.true_physics.ConfigLoaderImpl;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;

import java.util.HashMap;
import java.util.Map;

public final class TruePhysicsFabric implements ModInitializer {

    /** One core per loaded dimension, created lazily on first block change. */
    private static final Map<ServerLevel, TruePhysicsCore> cores = new HashMap<>();

    @Override
    public void onInitialize() {
        // M0 — load config
        PhysicsConfig config = ConfigLoaderImpl.load(
                FabricLoader.getInstance().getConfigDir());
        TruePhysics.init(config);

        // M1 — detect player block breaks
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (level instanceof ServerLevel serverLevel) {
                onBlockChanged(serverLevel, pos);
            }
        });

        // M1 — detect block placement (player right-click)
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (level instanceof ServerLevel serverLevel) {
                BlockPos pos = hitResult.getBlockPos();
                onBlockChanged(serverLevel, pos);
            }
            return InteractionResult.PASS;
        });

        // M1 — tick all cores each server tick
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerLevel level : server.getAllLevels()) {
                TruePhysicsCore core = cores.get(level);
                if (core != null) core.tick();
            }
        });

        // Cleanup on server stop
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            cores.clear();
        });
    }

    /**
     * Called by block-change events. Creates a core for the level
     * on first use, then delegates.
     */
    static void onBlockChanged(ServerLevel level, BlockPos pos) {
        PhysicsConfig cfg = PhysicsConfigHolder.get();
        if (!cfg.structuralCollapseEnabled) return;

        TruePhysicsCore core = cores.computeIfAbsent(level, TruePhysicsCore::create);
        core.onBlockChanged(pos);
    }
}
