package com.sabcancode.truephysics.neoforge;

import com.sabcancode.truephysics.TruePhysics;
import com.sabcancode.truephysics.config.PhysicsConfig;
import com.sabcancode.truephysics.config.PhysicsConfigHolder;
import com.sabcancode.truephysics.core.TruePhysicsCore;
import com.sabcancode.truephysics.platform.true_physics.ConfigLoaderImpl;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;

@Mod(TruePhysics.MOD_ID)
public final class TruePhysicsNeoForge {

    private static final Map<ServerLevel, TruePhysicsCore> cores = new HashMap<>();

    public TruePhysicsNeoForge(net.neoforged.fml.ModContainer modContainer) {
        // M0 — config
        modContainer.registerConfig(
                net.neoforged.fml.config.ModConfig.Type.COMMON,
                ConfigLoaderImpl.getSpec());
        PhysicsConfig config = ConfigLoaderImpl.load();
        TruePhysics.init(config);

        // M1 — register event listeners
        NeoForge.EVENT_BUS.register(this);
    }

    /** Called when a player breaks a block. */
    @SubscribeEvent
    public void onBlockBreak(BreakBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            onBlockChanged(level, event.getPos());
        }
    }

    /** Called when a block is placed by an entity. */
    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            onBlockChanged(level, event.getPos());
        }
    }

    /** Tick every core each server tick. */
    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            TruePhysicsCore core = cores.get(level);
            if (core != null) core.tick();
        }
    }

    private static void onBlockChanged(ServerLevel level, BlockPos pos) {
        PhysicsConfig cfg = PhysicsConfigHolder.get();
        if (!cfg.structuralCollapseEnabled) return;

        TruePhysicsCore core = cores.computeIfAbsent(level, TruePhysicsCore::create);
        core.onBlockChanged(level, pos);
    }
}
