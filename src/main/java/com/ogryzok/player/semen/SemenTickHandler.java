package com.ogryzok.player.semen;

import com.ogryzok.disease.DiseaseHandler;
import com.ogryzok.disease.DiseaseRegistry;
import com.ogryzok.manualharvest.ManualHarvestLogic;
import com.ogryzok.network.ModNetwork;
import com.ogryzok.player.abstinence.AbstinenceData;
import com.ogryzok.player.semen.packet.PacketSyncSemen;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SemenTickHandler {

    public static final int FILL_STEP_MB = 100;
    public static final int TICKS_PER_STEP = 2400;
    public static final UUID ABSTINENCE_HEALTH_UUID = UUID.fromString("7df07e89-cabd-443f-ac83-3d9b0ba9b511");

    private final Map<UUID, String> lastSyncedState = new HashMap<>();

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        EntityPlayer player = event.player;
        if (player == null || player.world.isRemote) return;

        ISemenStorage storage = player.getCapability(SemenProvider.SEMEN_CAP, null);
        if (storage == null) return;

        if (storage.getManualHarvestCooldownTicks() > 0) {
            storage.setManualHarvestCooldownTicks(storage.getManualHarvestCooldownTicks() - 1);
        }

        if (storage.getWeaknessLockTicks() > 0) {
            int ticksLeft = storage.getWeaknessLockTicks() - 1;
            storage.setWeaknessLockTicks(ticksLeft);
            PotionEffect weakness = player.getActivePotionEffect(MobEffects.WEAKNESS);
            if (weakness == null || weakness.getDuration() < ticksLeft - 5 || weakness.getAmplifier() < 1) {
                player.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, Math.max(20, ticksLeft), 1, false, false));
            }
        }

        if (DiseaseHandler.isDiseased(player)) {
            storage.setAmount(0);
            storage.setTickCounter(0);
            storage.setManualHarvesting(false);
            storage.setManualHarvestTicks(0);
            storage.setSteroidHarvest(false);
            storage.setSteroidLoadedShot(false);
            storage.setSteroidHarvestLevel(0);
            storage.setWeaknessLockTicks(0);
            storage.setKeeperHoverTicks(0);
            storage.setKeeperGlideTicks(0);
            storage.setUnstableBeamTicks(0);
            storage.resetAbstinence();
        } else if (storage.getUnstableBeamTicks() > 0) {
            ManualHarvestLogic.tickUnstableBurst(player, storage);
        } else if (storage.isManualHarvesting()) {
            ManualHarvestLogic.serverTick(player, storage);
        } else if (!storage.isFull()) {
            storage.addTickCounter(1);

            while (storage.getTickCounter() >= TICKS_PER_STEP && !storage.isFull()) {
                storage.setTickCounter(storage.getTickCounter() - TICKS_PER_STEP);
                storage.fill(FILL_STEP_MB);
            }

            if (storage.isFull()) {
                storage.setTickCounter(0);
            }
        }

        ManualHarvestLogic.tickKeeperAfterglow(player, storage);

        updateAbstinence(player, storage);
        updateSeedKeeperEffect(player, storage);
        updateAbstinenceHealth(player, storage);
        ManualHarvestLogic.tryTriggerInstability(player, storage);

        if (player instanceof EntityPlayerMP) {
            UUID id = player.getUniqueID();
            String currentState = storage.getAmount() + ":" + storage.getTickCounter() + ":" + storage.isManualHarvesting() + ":" +
                    storage.getManualHarvestTicks() + ":" + storage.getAbstinenceTicks() + ":" + storage.getAbstinenceStage() + ":" +
                    storage.hasSeedKeeper() + ":" + storage.getUnstableBeamTicks() + ":" + storage.getWeaknessLockTicks() + ":" +
                    player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getAttributeValue() + ":" + storage.getKeeperHoverTicks() + ":" + storage.getKeeperGlideTicks();
            String prev = lastSyncedState.get(id);

            if (prev == null || !prev.equals(currentState)) {
                PacketSyncSemen syncPacket = new PacketSyncSemen(player.getEntityId(), storage.getAmount(), storage.getTickCounter(),
                        storage.isManualHarvesting(), storage.getManualHarvestTicks(),
                        storage.getAbstinenceTicks(), storage.getAbstinenceStage(), storage.hasSeedKeeper());
                ModNetwork.CHANNEL.sendTo(syncPacket, (EntityPlayerMP) player);
                ModNetwork.CHANNEL.sendToAllTracking(syncPacket, (EntityPlayerMP) player);
                lastSyncedState.put(id, currentState);
            }
        }
    }

    private void updateAbstinence(EntityPlayer player, ISemenStorage storage) {
        if (!storage.isFull() || player.isDead) {
            storage.resetAbstinence();
            return;
        }

        storage.setAbstinenceTicks(storage.getAbstinenceTicks() + 1);
        int newStage = AbstinenceData.getStageForTicks(storage.getAbstinenceTicks());
        storage.setAbstinenceStage(newStage);
        storage.setSeedKeeper(AbstinenceData.isKeeper(newStage));
    }

    private void updateSeedKeeperEffect(EntityPlayer player, ISemenStorage storage) {
        if (storage.hasSeedKeeper()) {
            PotionEffect current = player.getActivePotionEffect(DiseaseRegistry.SEED_KEEPER);
            if (current == null || current.getDuration() < 200) {
                player.addPotionEffect(new PotionEffect(DiseaseRegistry.SEED_KEEPER, Integer.MAX_VALUE, 0, false, false));
            }
        } else if (player.isPotionActive(DiseaseRegistry.SEED_KEEPER)) {
            player.removePotionEffect(DiseaseRegistry.SEED_KEEPER);
        }
    }

    private void updateAbstinenceHealth(EntityPlayer player, ISemenStorage storage) {
        IAttributeInstance attr = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (attr == null) return;

        double oldMaxHealth = attr.getAttributeValue();
        AttributeModifier existing = attr.getModifier(ABSTINENCE_HEALTH_UUID);
        if (existing != null) {
            attr.removeModifier(existing);
        }

        int pairs = AbstinenceData.getExtraHeartPairs(storage.getAbstinenceStage());
        if (pairs > 0) {
            double amount = pairs * 4.0D;
            attr.applyModifier(new AttributeModifier(ABSTINENCE_HEALTH_UUID, "harvestech_abstinence_health", amount, 0));
        }

        double newMaxHealth = attr.getAttributeValue();
        if (newMaxHealth < oldMaxHealth && player.getHealth() > newMaxHealth) {
            player.setHealth((float) newMaxHealth);
        }

        if (pairs <= 0 && !storage.hasSeedKeeper()) {
            double normalizedMaxHealth = Math.min(20.0D, newMaxHealth);
            if (player.getHealth() > normalizedMaxHealth) {
                player.setHealth((float) normalizedMaxHealth);
            }
        }
    }
}
