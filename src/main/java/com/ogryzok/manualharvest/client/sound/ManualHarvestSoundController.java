package com.ogryzok.manualharvest.client.sound;

import com.ogryzok.manualharvest.ManualHarvestLogic;
import com.ogryzok.manualharvest.sound.ManualHarvestSoundRegistry;
import com.ogryzok.player.semen.ISemenStorage;
import com.ogryzok.player.semen.SemenProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class ManualHarvestSoundController {
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final Map<Integer, ManualHarvestPlayerLoopSound> MANUAL_LOOPS = new HashMap<Integer, ManualHarvestPlayerLoopSound>();
    private static final Map<Integer, ManualHarvestPlayerLoopSound> USUAL_BEAM_LOOPS = new HashMap<Integer, ManualHarvestPlayerLoopSound>();
    private static final Map<Integer, ManualHarvestPlayerLoopSound> MEGA_LOOPS = new HashMap<Integer, ManualHarvestPlayerLoopSound>();

    private ManualHarvestSoundController() {
    }

    public static void onClientTick() {
        if (MC == null || MC.world == null || MC.player == null) {
            clearAll();
            return;
        }

        java.util.Set<Integer> seen = new java.util.HashSet<Integer>();
        for (EntityPlayer player : MC.world.playerEntities) {
            if (player == null || player.isDead) {
                continue;
            }

            seen.add(player.getEntityId());
            ISemenStorage storage = player.getCapability(SemenProvider.SEMEN_CAP, null);
            boolean manualStage = shouldPlayManualHarvest(storage);
            boolean usualBeamStage = shouldPlayUsualBeam(storage);
            boolean megaStage = shouldPlayMegaBeam(storage);

            if (manualStage) {
                ensureManualLoop(player);
            } else {
                stopManualLoop(player.getEntityId());
            }

            if (usualBeamStage) {
                ensureUsualBeamLoop(player);
            } else {
                stopUsualBeamLoop(player.getEntityId());
            }

            if (megaStage) {
                ensureMegaLoop(player);
            } else {
                stopMegaLoop(player.getEntityId());
            }
        }

        cleanupMissing(seen, MANUAL_LOOPS);
        cleanupMissing(seen, USUAL_BEAM_LOOPS);
        cleanupMissing(seen, MEGA_LOOPS);
    }

    private static void ensureManualLoop(EntityPlayer player) {
        int id = player.getEntityId();
        ManualHarvestPlayerLoopSound loop = MANUAL_LOOPS.get(id);
        if (loop != null && MC.getSoundHandler().isSoundPlaying(loop)) {
            return;
        }
        stopManualLoop(id);
        if (ManualHarvestSoundRegistry.MANUAL_HARVEST == null) {
            return;
        }
        loop = new ManualHarvestPlayerLoopSound(ManualHarvestSoundRegistry.MANUAL_HARVEST, player, p -> {
            ISemenStorage storage = p.getCapability(SemenProvider.SEMEN_CAP, null);
            return shouldPlayManualHarvest(storage);
        });
        MANUAL_LOOPS.put(id, loop);
        MC.getSoundHandler().playSound(loop);
    }


    private static void ensureUsualBeamLoop(EntityPlayer player) {
        int id = player.getEntityId();
        ManualHarvestPlayerLoopSound loop = USUAL_BEAM_LOOPS.get(id);
        if (loop != null && MC.getSoundHandler().isSoundPlaying(loop)) {
            return;
        }
        stopUsualBeamLoop(id);
        if (ManualHarvestSoundRegistry.USUAL_LUCH == null) {
            return;
        }
        loop = new ManualHarvestPlayerLoopSound(ManualHarvestSoundRegistry.USUAL_LUCH, player, p -> {
            ISemenStorage storage = p.getCapability(SemenProvider.SEMEN_CAP, null);
            return shouldPlayUsualBeam(storage);
        });
        USUAL_BEAM_LOOPS.put(id, loop);
        MC.getSoundHandler().playSound(loop);
    }

    private static void ensureMegaLoop(EntityPlayer player) {
        int id = player.getEntityId();
        ManualHarvestPlayerLoopSound loop = MEGA_LOOPS.get(id);
        if (loop != null && MC.getSoundHandler().isSoundPlaying(loop)) {
            return;
        }
        stopMegaLoop(id);
        if (ManualHarvestSoundRegistry.MEGA_LUCH == null) {
            return;
        }
        loop = new ManualHarvestPlayerLoopSound(ManualHarvestSoundRegistry.MEGA_LUCH, player, p -> {
            ISemenStorage storage = p.getCapability(SemenProvider.SEMEN_CAP, null);
            return shouldPlayMegaBeam(storage);
        });
        MEGA_LOOPS.put(id, loop);
        MC.getSoundHandler().playSound(loop);
    }

    private static boolean shouldPlayManualHarvest(ISemenStorage storage) {
        if (storage == null || !storage.isManualHarvesting()) {
            return false;
        }
        return !isBeamActive(storage);
    }

    private static boolean shouldPlayUsualBeam(ISemenStorage storage) {
        if (storage == null || !storage.isManualHarvesting()) {
            return false;
        }
        if (storage.getAbstinenceStage() >= 4) {
            return false;
        }
        return isBeamActive(storage);
    }

    private static boolean shouldPlayMegaBeam(ISemenStorage storage) {
        if (storage == null || !storage.isManualHarvesting()) {
            return false;
        }
        if (storage.getAbstinenceStage() < 4) {
            return false;
        }
        return isBeamActive(storage);
    }

    private static boolean isBeamActive(ISemenStorage storage) {
        if (storage == null || !storage.isManualHarvesting()) {
            return false;
        }

        int totalTicks = getClientTotalTicks(storage);
        int duration = getClientBeamDuration(storage);
        int beamStartTick = Math.max(1, totalTicks - duration);
        int ticks = storage.getManualHarvestTicks();
        return ticks >= beamStartTick && ticks < totalTicks;
    }

    private static int getClientTotalTicks(ISemenStorage storage) {
        int duration = getClientBeamDuration(storage);
        return ManualHarvestLogic.HARVEST_TICKS_TOTAL + Math.max(0, duration - ManualHarvestLogic.BASE_BEAM_TICKS);
    }

    private static int getClientBeamDuration(ISemenStorage storage) {
        int stage = storage != null ? storage.getAbstinenceStage() : 0;
        switch (stage) {
            case 1:
                return 70;
            case 2:
                return 80;
            case 3:
                return 90;
            case 4:
                return 100;
            default:
                return ManualHarvestLogic.BASE_BEAM_TICKS;
        }
    }

    private static void stopManualLoop(int entityId) {
        ManualHarvestPlayerLoopSound loop = MANUAL_LOOPS.remove(entityId);
        if (loop != null) {
            loop.stopLoop();
        }
    }

    private static void stopUsualBeamLoop(int entityId) {
        ManualHarvestPlayerLoopSound loop = USUAL_BEAM_LOOPS.remove(entityId);
        if (loop != null) {
            loop.stopLoop();
        }
    }

    private static void stopMegaLoop(int entityId) {
        ManualHarvestPlayerLoopSound loop = MEGA_LOOPS.remove(entityId);
        if (loop != null) {
            loop.stopLoop();
        }
    }

    private static void cleanupMissing(java.util.Set<Integer> seen, Map<Integer, ManualHarvestPlayerLoopSound> loops) {
        Iterator<Map.Entry<Integer, ManualHarvestPlayerLoopSound>> it = loops.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, ManualHarvestPlayerLoopSound> entry = it.next();
            if (!seen.contains(entry.getKey())) {
                if (entry.getValue() != null) {
                    entry.getValue().stopLoop();
                }
                it.remove();
            }
        }
    }

    private static void clearAll() {
        for (ManualHarvestPlayerLoopSound loop : MANUAL_LOOPS.values()) {
            if (loop != null) {
                loop.stopLoop();
            }
        }
        MANUAL_LOOPS.clear();

        for (ManualHarvestPlayerLoopSound loop : USUAL_BEAM_LOOPS.values()) {
            if (loop != null) {
                loop.stopLoop();
            }
        }
        USUAL_BEAM_LOOPS.clear();

        for (ManualHarvestPlayerLoopSound loop : MEGA_LOOPS.values()) {
            if (loop != null) {
                loop.stopLoop();
            }
        }
        MEGA_LOOPS.clear();
    }
}
