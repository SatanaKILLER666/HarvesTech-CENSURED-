package com.ogryzok.semendestiller.client.sound;

import com.ogryzok.semendestiller.sound.SemenDestillerSoundRegistry;
import com.ogryzok.semendestiller.tile.TileSemenDestillerBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SemenDestillerSoundController {
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final Map<BlockPos, Boolean> STATES = new HashMap<BlockPos, Boolean>();
    private static final Map<BlockPos, SemenDestillerLoopSound> LOOPS = new HashMap<BlockPos, SemenDestillerLoopSound>();

    private SemenDestillerSoundController() {
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
            if (!(te instanceof TileSemenDestillerBase)) {
                continue;
            }

            TileSemenDestillerBase tile = (TileSemenDestillerBase) te;
            if (tile.isInvalid() || tile.getPos() == null) {
                continue;
            }

            BlockPos pos = tile.getPos();
            seen.add(pos);

            boolean newState = tile.isDistillingNow();
            boolean oldState = STATES.containsKey(pos) && STATES.get(pos);

            if (newState != oldState) {
                if (oldState) {
                    stopLoop(pos);
                    playOneShot(pos, SemenDestillerSoundRegistry.DISTILLER_STOP, 1.0F, 1.0F);
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

    private static void startLoop(BlockPos pos, TileSemenDestillerBase tile) {
        stopLoop(pos);
        if (SemenDestillerSoundRegistry.DISTILLER_WORK == null) {
            return;
        }

        SemenDestillerLoopSound loop = new SemenDestillerLoopSound(SemenDestillerSoundRegistry.DISTILLER_WORK, tile);
        LOOPS.put(pos, loop);
        MC.getSoundHandler().playSound(loop);
    }


    private static boolean isLoopAlive(BlockPos pos) {
        SemenDestillerLoopSound loop = LOOPS.get(pos);
        return loop != null && MC.getSoundHandler().isSoundPlaying(loop);
    }

    private static void stopLoop(BlockPos pos) {
        SemenDestillerLoopSound loop = LOOPS.remove(pos);
        if (loop != null) {
            loop.stopLoop();
        }
    }

    private static void playOneShot(BlockPos pos, net.minecraft.util.SoundEvent sound, float volume, float pitch) {
        if (pos == null || sound == null) {
            return;
        }

        ISound oneShot = new PositionedSoundRecord(
                sound.getSoundName(),
                SoundCategory.BLOCKS,
                volume,
                pitch,
                false,
                0,
                ISound.AttenuationType.LINEAR,
                pos.getX() + 0.5F,
                pos.getY() + 1.0F,
                pos.getZ() + 0.5F
        );
        MC.getSoundHandler().playSound(oneShot);
    }

    private static void clearAll() {
        for (SemenDestillerLoopSound loop : LOOPS.values()) {
            if (loop != null) {
                loop.stopLoop();
            }
        }
        LOOPS.clear();
        STATES.clear();
    }
}
