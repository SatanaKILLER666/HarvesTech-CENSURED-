package com.ogryzok.lifecrusher.player;

import com.ogryzok.lifecrusher.LifeCrusherRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class PlayerBerserkHandler {

    private static final String PERSISTED_TAG = EntityPlayer.PERSISTED_NBT_TAG;
    private static final String ROOT_TAG = "harvestech";
    private static final String STUPOR_TIMER_TAG = "LifeCrusherBerserkStuporTicks";

    public static final int STUPOR_DURATION_TICKS = 20 * 120; // 2 минуты
    private static final int NAUSEA_DURATION = 20 * 15; // 15 секунд

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

    public static void refreshBerserkStupor(EntityPlayer player) {
        if (player == null) {
            return;
        }

        NBTTagCompound root = getPersistedRoot(player);
        root.setInteger(STUPOR_TIMER_TAG, STUPOR_DURATION_TICKS);
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
        int ticksLeft = root.getInteger(STUPOR_TIMER_TAG);

        if (ticksLeft <= 0) {
            root.removeTag(STUPOR_TIMER_TAG);
            return;
        }

        // ПРОДУПЛЕНИЕ!!!!!1
        Potion stupor = LifeCrusherRegistry.STUPOR;
        if (stupor != null) {
            PotionEffect current = player.getActivePotionEffect(stupor);

            if (current == null || current.getDuration() < ticksLeft - 5) {
                player.addPotionEffect(new PotionEffect(
                        stupor,
                        ticksLeft,
                        0,
                        true,
                        false
                ));
            }
        }

        // слабость 2мин
        PotionEffect weakness = player.getActivePotionEffect(MobEffects.WEAKNESS);
        if (weakness == null || weakness.getDuration() < ticksLeft - 5) {
            player.addPotionEffect(new PotionEffect(
                    MobEffects.WEAKNESS,
                    ticksLeft,
                    0,
                    true,
                    false
            ));
        }

        // тощнота на 15сек
        if (ticksLeft > STUPOR_DURATION_TICKS - NAUSEA_DURATION) {
            player.addPotionEffect(new PotionEffect(
                    MobEffects.NAUSEA,
                    200,
                    0,
                    true,
                    false
            ));
        }

        root.setInteger(STUPOR_TIMER_TAG, ticksLeft - 1);
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
        newPersisted.setTag(ROOT_TAG, oldPersisted.getCompoundTag(ROOT_TAG).copy());
    }
}