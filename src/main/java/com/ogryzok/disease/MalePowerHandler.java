package com.ogryzok.disease;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class MalePowerHandler {
    private static final String PERSISTED_TAG = EntityPlayer.PERSISTED_NBT_TAG;
    private static final String ROOT_TAG = "harvestech";
    private static final String MALE_POWER_TIME_TAG = "MalePowerTicks";
    private static final String MALE_POWER_LEVEL_TAG = "MalePowerLevel";
    private static final String MALE_POWER_ELAPSED_TAG = "MalePowerElapsedTicks";

    public static final int MAX_DURATION_TICKS = 20 * 60 * 10;
    public static final int REUSE_ADD_TICKS = 20 * 60 * 2;

    private static NBTTagCompound getPersistedRoot(EntityPlayer player) {
        NBTTagCompound entityData = player.getEntityData();
        if (!entityData.hasKey(PERSISTED_TAG, 10)) {
            entityData.setTag(PERSISTED_TAG, new NBTTagCompound());
        }
        NBTTagCompound persisted = entityData.getCompoundTag(PERSISTED_TAG);
        if (!persisted.hasKey(ROOT_TAG, 10)) {
            persisted.setTag(ROOT_TAG, new NBTTagCompound());
        }
        return persisted.getCompoundTag(ROOT_TAG);
    }

    public static void consumeSteroid(EntityPlayer player) {
        if (player == null) {
            return;
        }

        NBTTagCompound root = getPersistedRoot(player);
        int ticksLeft = Math.max(0, root.getInteger(MALE_POWER_TIME_TAG));
        int level = Math.max(0, root.getInteger(MALE_POWER_LEVEL_TAG));

        if (ticksLeft <= 0 || level <= 0) {
            root.setInteger(MALE_POWER_TIME_TAG, MAX_DURATION_TICKS);
            root.setInteger(MALE_POWER_LEVEL_TAG, 1);
            root.setInteger(MALE_POWER_ELAPSED_TAG, 0);
            applyEffect(player, MAX_DURATION_TICKS, 1);
            return;
        }

        int newTicks = Math.min(MAX_DURATION_TICKS, ticksLeft + REUSE_ADD_TICKS);
        int newLevel = Math.min(2, Math.max(1, level + 1));
        root.setInteger(MALE_POWER_TIME_TAG, newTicks);
        root.setInteger(MALE_POWER_LEVEL_TAG, newLevel);
        applyEffect(player, newTicks, newLevel);
    }

    public static boolean isMalePowerActive(EntityPlayer player) {
        return player != null && getRemainingTicks(player) > 0;
    }

    public static int getRemainingTicks(EntityPlayer player) {
        if (player == null) {
            return 0;
        }
        return Math.max(0, getPersistedRoot(player).getInteger(MALE_POWER_TIME_TAG));
    }

    public static int getElapsedTicks(EntityPlayer player) {
        if (player == null || !isMalePowerActive(player)) {
            return 0;
        }
        return Math.max(0, getPersistedRoot(player).getInteger(MALE_POWER_ELAPSED_TAG));
    }

    public static int getElapsedFullMinutes(EntityPlayer player) {
        return getElapsedTicks(player) / (20 * 60);
    }

    public static int getActiveLevel(EntityPlayer player) {
        if (!isMalePowerActive(player)) {
            return 0;
        }
        return Math.max(1, Math.min(2, getPersistedRoot(player).getInteger(MALE_POWER_LEVEL_TAG)));
    }

    private static void clearMalePower(EntityPlayer player) {
        NBTTagCompound root = getPersistedRoot(player);
        root.removeTag(MALE_POWER_TIME_TAG);
        root.removeTag(MALE_POWER_LEVEL_TAG);
        root.removeTag(MALE_POWER_ELAPSED_TAG);
        player.removePotionEffect(DiseaseRegistry.MALE_POWER);
    }

    private static void applyEffect(EntityPlayer player, int ticksLeft, int level) {
        if (DiseaseRegistry.MALE_POWER == null || ticksLeft <= 0 || level <= 0) {
            return;
        }

        PotionEffect current = player.getActivePotionEffect(DiseaseRegistry.MALE_POWER);
        int amplifier = Math.max(0, level - 1);
        if (current == null || current.getAmplifier() != amplifier || current.getDuration() < ticksLeft - 5) {
            player.addPotionEffect(new PotionEffect(DiseaseRegistry.MALE_POWER, ticksLeft, amplifier, true, false));
        }
    }

    @SubscribeEvent
    public void onPlayerTick(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world == null || player.world.isRemote) {
            return;
        }

        NBTTagCompound root = getPersistedRoot(player);
        int ticksLeft = Math.max(0, root.getInteger(MALE_POWER_TIME_TAG));
        int level = Math.max(0, root.getInteger(MALE_POWER_LEVEL_TAG));

        if (ticksLeft <= 0 || level <= 0) {
            clearMalePower(player);
            return;
        }

        applyEffect(player, ticksLeft, level);
        root.setInteger(MALE_POWER_TIME_TAG, ticksLeft - 1);
        root.setInteger(MALE_POWER_ELAPSED_TAG, root.getInteger(MALE_POWER_ELAPSED_TAG) + 1);
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }

        NBTTagCompound oldEntityData = event.getOriginal().getEntityData();
        if (!oldEntityData.hasKey(PERSISTED_TAG, 10)) {
            return;
        }

        NBTTagCompound oldPersisted = oldEntityData.getCompoundTag(PERSISTED_TAG);
        if (!oldPersisted.hasKey(ROOT_TAG, 10)) {
            return;
        }

        NBTTagCompound newEntityData = event.getEntityPlayer().getEntityData();
        if (!newEntityData.hasKey(PERSISTED_TAG, 10)) {
            newEntityData.setTag(PERSISTED_TAG, new NBTTagCompound());
        }

        NBTTagCompound newPersisted = newEntityData.getCompoundTag(PERSISTED_TAG);
        NBTTagCompound oldRoot = oldPersisted.getCompoundTag(ROOT_TAG);
        NBTTagCompound newRoot = oldRoot.copy();
        newPersisted.setTag(ROOT_TAG, newRoot);
    }
}
