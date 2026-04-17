package com.ogryzok.lifecrusher.client.sound;

import com.ogryzok.lifecrusher.sound.LifeCrusherSoundRegistry;
import com.ogryzok.lifecrusher.tile.CrusherState;
import com.ogryzok.lifecrusher.tile.TileLifeCrusher;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;

public final class LifeCrusherSoundController {
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final Map<BlockPos, Mode> MODES = new HashMap<BlockPos, Mode>();
    private static final Map<BlockPos, LifeCrusherLoopSound> LOOPS = new HashMap<BlockPos, LifeCrusherLoopSound>();

    private enum Mode {
        NONE,
        WORK,
        BERSERK
    }

    private LifeCrusherSoundController() {
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
            if (!(te instanceof TileLifeCrusher)) {
                continue;
            }

            TileLifeCrusher tile = (TileLifeCrusher) te;
            if (tile.isInvalid() || tile.getPos() == null) {
                continue;
            }

            BlockPos pos = tile.getPos();
            seen.add(pos);

            Mode newMode = resolveMode(tile);
            Mode oldMode = MODES.containsKey(pos) ? MODES.get(pos) : Mode.NONE;

            if (newMode != oldMode) {
                stopLoop(pos);

                if ((oldMode == Mode.WORK || oldMode == Mode.BERSERK) && newMode == Mode.NONE) {
                    playOneShot(LifeCrusherSoundRegistry.STOP, pos, 1.0F, 1.0F);
                }

                if (newMode == Mode.WORK) {
                    startLoop(pos, tile, LifeCrusherSoundRegistry.WORK);
                } else if (newMode == Mode.BERSERK) {
                    startLoop(pos, tile, LifeCrusherSoundRegistry.BERSERK);
                }

                MODES.put(pos, newMode);
            } else if (newMode != Mode.NONE && !LOOPS.containsKey(pos)) {
                if (newMode == Mode.WORK) {
                    startLoop(pos, tile, LifeCrusherSoundRegistry.WORK);
                } else {
                    startLoop(pos, tile, LifeCrusherSoundRegistry.BERSERK);
                }
            }
        }

        Iterator<Map.Entry<BlockPos, Mode>> it = MODES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Mode> entry = it.next();
            if (!seen.contains(entry.getKey())) {
                stopLoop(entry.getKey());
                it.remove();
            }
        }
    }

    private static Mode resolveMode(TileLifeCrusher tile) {
        if (tile == null) {
            return Mode.NONE;
        }

        if (tile.getState() == CrusherState.FAIL || tile.getState() == CrusherState.IDLE) {
            return Mode.NONE;
        }

        if (tile.getWorkSpeed() <= 0.01F) {
            return Mode.NONE;
        }

        return tile.isBerserk() ? Mode.BERSERK : Mode.WORK;
    }

    private static void startLoop(BlockPos pos, TileLifeCrusher tile, SoundEvent sound) {
        if (sound == null) {
            return;
        }

        LifeCrusherLoopSound loop = new LifeCrusherLoopSound(sound, tile);
        LOOPS.put(pos, loop);
        MC.getSoundHandler().playSound(loop);
    }

    private static void stopLoop(BlockPos pos) {
        LifeCrusherLoopSound loop = LOOPS.remove(pos);
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
                pos.getY() + 0.85F,
                pos.getZ() + 0.5F
        );
        MC.getSoundHandler().playSound(oneShot);
    }

    private static void clearAll() {
        for (LifeCrusherLoopSound loop : LOOPS.values()) {
            if (loop != null) {
                loop.stopLoop();
            }
        }
        LOOPS.clear();
        MODES.clear();
    }
}
