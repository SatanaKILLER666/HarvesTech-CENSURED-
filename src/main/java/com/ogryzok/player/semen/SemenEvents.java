package com.ogryzok.player.semen;

import com.ogryzok.harvestech;
import com.ogryzok.network.ModNetwork;
import com.ogryzok.player.semen.packet.PacketSyncSemen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

public class SemenEvents {

    private static final ResourceLocation KEY = new ResourceLocation(harvestech.MODID, "semen_storage");

    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof EntityPlayer) {
            event.addCapability(KEY, new SemenProvider());
        }
    }

    @SubscribeEvent
    public void onPlayerClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        if (event.getEntityPlayer() == null) return;

        ISemenStorage newStorage = event.getEntityPlayer().getCapability(SemenProvider.SEMEN_CAP, null);
        if (newStorage == null) return;

        if (event.isWasDeath()) {
            newStorage.clear();
            return;
        }

        ISemenStorage oldStorage = event.getOriginal().getCapability(SemenProvider.SEMEN_CAP, null);
        if (oldStorage == null) return;

        newStorage.setAmount(oldStorage.getAmount());
        newStorage.setTickCounter(oldStorage.getTickCounter());
        newStorage.setManualHarvesting(oldStorage.isManualHarvesting());
        newStorage.setManualHarvestTicks(oldStorage.getManualHarvestTicks());
        newStorage.setManualHarvestCooldownTicks(oldStorage.getManualHarvestCooldownTicks());
        newStorage.setSteroidHarvest(oldStorage.isSteroidHarvest());
        newStorage.setSteroidLoadedShot(oldStorage.isSteroidLoadedShot());
        newStorage.setSteroidHarvestLevel(oldStorage.getSteroidHarvestLevel());
        newStorage.setAbstinenceTicks(oldStorage.getAbstinenceTicks());
        newStorage.setAbstinenceStage(oldStorage.getAbstinenceStage());
        newStorage.setSeedKeeper(oldStorage.hasSeedKeeper());
        newStorage.setManualHarvestStartY(oldStorage.getManualHarvestStartY());
        newStorage.setWeaknessLockTicks(oldStorage.getWeaknessLockTicks());
        newStorage.setKeeperHoverTicks(oldStorage.getKeeperHoverTicks());
        newStorage.setKeeperGlideTicks(oldStorage.getKeeperGlideTicks());
        newStorage.setUnstableBeamTicks(oldStorage.getUnstableBeamTicks());
        newStorage.setUnstableBeamStage(oldStorage.getUnstableBeamStage());
        newStorage.setLastBurstRollMinute(oldStorage.getLastBurstRollMinute());
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        sync(event.player);
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        sync(event.player);
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        sync(event.player);
    }

    private void sync(EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP)) return;

        ISemenStorage storage = player.getCapability(SemenProvider.SEMEN_CAP, null);
        if (storage == null) return;

        ModNetwork.CHANNEL.sendTo(
                new PacketSyncSemen(player.getEntityId(), storage.getAmount(), storage.getTickCounter(),
                        storage.isManualHarvesting(), storage.getManualHarvestTicks(),
                        storage.getAbstinenceTicks(), storage.getAbstinenceStage(), storage.hasSeedKeeper()),
                (EntityPlayerMP) player
        );
    }
}
