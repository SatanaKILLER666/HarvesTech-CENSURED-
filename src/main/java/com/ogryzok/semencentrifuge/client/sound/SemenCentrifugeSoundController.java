package com.ogryzok.semencentrifuge.client.sound;

import com.ogryzok.semencentrifuge.sound.SemenCentrifugeSoundRegistry;
import com.ogryzok.semencentrifuge.tile.TileSemenCentrifugeBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SemenCentrifugeSoundController {
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final Map<BlockPos, Integer> MODES = new HashMap<BlockPos, Integer>();
    private static final Map<BlockPos, SemenCentrifugeLoopSound> LOOPS = new HashMap<BlockPos, SemenCentrifugeLoopSound>();

    private SemenCentrifugeSoundController() {
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
            if (!(te instanceof TileSemenCentrifugeBase)) {
                continue;
            }

            TileSemenCentrifugeBase tile = (TileSemenCentrifugeBase) te;
            if (tile.isInvalid() || tile.getPos() == null) {
                continue;
            }

            BlockPos pos = tile.getPos();
            seen.add(pos);

            int newMode = resolveMode(tile);
            int oldMode = MODES.containsKey(pos) ? MODES.get(pos) : 0;

            if (newMode != oldMode) {
                stopLoop(pos);

                if (oldMode != 0 && newMode == 0) {
                    playOneShot(SemenCentrifugeSoundRegistry.CENTRIFUGE_STOP, pos, 1.0F, 1.0F);
                }

                if (newMode != 0) {
                    startLoop(pos, tile, newMode);
                }

                MODES.put(pos, newMode);
            } else if (newMode != 0 && !isLoopAlive(pos)) {
                startLoop(pos, tile, newMode);
            }
        }

        Iterator<Map.Entry<BlockPos, Integer>> it = MODES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = it.next();
            if (!seen.contains(entry.getKey())) {
                stopLoop(entry.getKey());
                it.remove();
            }
        }
    }

    private static int resolveMode(TileSemenCentrifugeBase tile) {
        if (tile == null || !tile.isSessionActive()) {
            return 0;
        }
        return Math.max(1, Math.min(5, tile.getCurrentRound()));
    }

    private static void startLoop(BlockPos pos, TileSemenCentrifugeBase tile, int mode) {
        stopLoop(pos);

        SoundEvent sound = getSoundForMode(mode);
        if (sound == null) {
            return;
        }

        SemenCentrifugeLoopSound loop = new SemenCentrifugeLoopSound(sound, tile);
        LOOPS.put(pos, loop);
        MC.getSoundHandler().playSound(loop);
    }

    private static SoundEvent getSoundForMode(int mode) {
        switch (mode) {
            case 1:
                return SemenCentrifugeSoundRegistry.CENTRIFUGE_1SPEED;
            case 2:
                return SemenCentrifugeSoundRegistry.CENTRIFUGE_2SPEED;
            case 3:
                return SemenCentrifugeSoundRegistry.CENTRIFUGE_3SPEED;
            case 4:
                return SemenCentrifugeSoundRegistry.CENTRIFUGE_4SPEED;
            case 5:
                return SemenCentrifugeSoundRegistry.CENTRIFUGE_5SPEED;
            default:
                return null;
        }
    }

    private static boolean isLoopAlive(BlockPos pos) {
        SemenCentrifugeLoopSound loop = LOOPS.get(pos);
        return loop != null && MC.getSoundHandler().isSoundPlaying(loop);
    }

    private static void stopLoop(BlockPos pos) {
        SemenCentrifugeLoopSound loop = LOOPS.remove(pos);
        if (loop != null) {
            loop.stopLoop();
        }
    }

    private static void playOneShot(SoundEvent sound, BlockPos pos, float volume, float pitch) {
        if (sound == null || pos == null) {
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
        for (SemenCentrifugeLoopSound loop : LOOPS.values()) {
            if (loop != null) {
                loop.stopLoop();
            }
        }
        LOOPS.clear();
        MODES.clear();
    }
}
