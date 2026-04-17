package com.ogryzok.separator.client.sound;

import com.ogryzok.separator.sound.SeparatorSoundRegistry;
import com.ogryzok.separator.tile.TileSeparator;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SeparatorSoundController {
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final Map<BlockPos, Boolean> STATES = new HashMap<BlockPos, Boolean>();
    private static final Map<BlockPos, SeparatorLoopSound> LOOPS = new HashMap<BlockPos, SeparatorLoopSound>();

    private SeparatorSoundController() {
    }

    public static void onClientTick() {
        if (MC == null || MC.world == null || MC.player == null) {
            clearAll();
            return;
        }

        Set<BlockPos> seen = new HashSet<BlockPos>();
        List<TileEntity> loaded = MC.world.loadedTileEntityList;
        if (loaded == null || loaded.isEmpty()) {
            clearAll();
            return;
        }

        for (TileEntity te : loaded) {
            if (!(te instanceof TileSeparator)) {
                continue;
            }

            TileSeparator tile = (TileSeparator) te;
            if (tile.isInvalid() || tile.getPos() == null) {
                continue;
            }

            BlockPos pos = tile.getPos();
            seen.add(pos);

            boolean newState = tile.isActiveNow();
            boolean oldState = STATES.containsKey(pos) && STATES.get(pos);

            if (newState != oldState) {
                if (oldState) {
                    stopLoop(pos);
                }

                if (newState) {
                    startLoop(pos, tile);
                }

                STATES.put(pos, newState);
            } else if (newState && !isLoopAlive(pos)) {
                startLoop(pos, tile);
            }
        }

        Iterator<Map.Entry<BlockPos, Boolean>> it = STATES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Boolean> entry = it.next();
            if (!seen.contains(entry.getKey())) {
                stopLoop(entry.getKey());
                it.remove();
            }
        }
    }

    private static void startLoop(BlockPos pos, TileSeparator tile) {
        stopLoop(pos);
        if (SeparatorSoundRegistry.SEPARATOR == null) {
            return;
        }

        SeparatorLoopSound loop = new SeparatorLoopSound(SeparatorSoundRegistry.SEPARATOR, tile);
        LOOPS.put(pos, loop);
        MC.getSoundHandler().playSound(loop);
    }

    private static boolean isLoopAlive(BlockPos pos) {
        SeparatorLoopSound loop = LOOPS.get(pos);
        return loop != null && MC.getSoundHandler().isSoundPlaying(loop);
    }

    private static void stopLoop(BlockPos pos) {
        SeparatorLoopSound loop = LOOPS.remove(pos);
        if (loop != null) {
            loop.stopLoop();
        }
    }

    private static void clearAll() {
        for (SeparatorLoopSound loop : LOOPS.values()) {
            if (loop != null) {
                loop.stopLoop();
            }
        }
        LOOPS.clear();
        STATES.clear();
    }
}
