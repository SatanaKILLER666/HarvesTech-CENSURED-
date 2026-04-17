package com.ogryzok.disease;

import com.ogryzok.player.semen.ISemenStorage;
import com.ogryzok.player.semen.SemenProvider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class DiseaseHandler {
    private static final int HIV_STAGE_DURATION = 20 * 60 * 15;
    private static final int AIDS_STAGE_1_DURATION = 20 * 60 * 10;
    private static final int AIDS_STAGE_2_DURATION = 20 * 60 * 15;
    private static final int VEGETABLE_STAGE_DURATION = 20 * 60 * 20;
    private static final int HIV_SYMPTOM_INTERVAL = 20 * 15;
    private static final int AIDS_SYMPTOM_INTERVAL = 20 * 10;
    private static final int VEG_SYMPTOM_INTERVAL = 20 * 10;
    private static final int STAGE2_DAMAGE_INTERVAL = 20 * 10;
    private static final int RECOVERY_DEBUFF_DURATION = 20 * 60 * 10;

    private static final String PERSISTED_TAG = EntityPlayer.PERSISTED_NBT_TAG;
    private static final String ROOT_TAG = "fptDisease";
    private static final String TAG_STAGE = "Stage";
    private static final String TAG_STAGE_END = "StageEnd";
    private static final String TAG_STAGE2_DEATH = "fptStage2DeathFlag";
    private static final String TAG_VEG_START = "fptVegStart";
    private static final String TAG_LAST_HIV = "fptLastHivSymptom";
    private static final String TAG_LAST_AIDS = "fptLastAidsSymptom";
    private static final String TAG_LAST_VEG = "fptLastVegSymptom";
    private static final String TAG_LAST_DMG = "fptLastStage2Damage";
    private static final String TAG_RECOVERY_DEBUFF = "fptRecoveryDebuff";
    private static final String TAG_RECOVERY_DEBUFF_END = "fptRecoveryDebuffEnd";

    @SubscribeEvent
    public void onFoodFinish(LivingEntityUseItemEvent.Finish event) {
        EntityLivingBase living = event.getEntityLiving();
        if (!(living instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer) living;
        ItemStack stack = event.getItem();
        if (stack.isEmpty()) {
            return;
        }

        if (stack.getItem() == Items.POISONOUS_POTATO) {
            infectIfHealthy(player);
            return;
        }

        if (stack.getItem() == DiseaseRegistry.BIOMASS_CAN_TRIGGER_ITEM && player.getRNG().nextFloat() < 0.05F) {
            infectIfHealthy(player);
        }

        if (!(stack.getItem() instanceof ItemFood)) {
            return;
        }

        if (isCurrentStage(player, DiseaseRegistry.AIDS_STAGE_2)) {
            player.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 20 * 5, 0));
        } else if (isCurrentStage(player, DiseaseRegistry.AIDS_STAGE_1) && player.getRNG().nextBoolean()) {
            player.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 20 * 3, 0));
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }

        EntityPlayer player = event.player;
        NBTTagCompound data = getDiseaseData(player);
        long time = player.world.getTotalWorldTime();
        sanitizeDiseaseState(player, data, time);
        Potion currentStage = getStoredStage(data);

        ensureRecoveryDebuff(player, data, time);
        if (currentStage == null) {
            clearStageEffects(player);
            return;
        }

        removeExternalCures(player);
        ensureStageEffect(player, currentStage, data, time);
        ensureInfectedBiomassBlocked(player);
        clampHealth(player);

        if (currentStage == DiseaseRegistry.HIV) {
            triggerSymptom(player, data, TAG_LAST_HIV, HIV_SYMPTOM_INTERVAL, false);
            if (time >= data.getLong(TAG_STAGE_END)) {
                setStage(player, DiseaseRegistry.AIDS_STAGE_1, AIDS_STAGE_1_DURATION);
            }
            return;
        }

        if (currentStage == DiseaseRegistry.AIDS_STAGE_1) {
            player.setSprinting(false);
            triggerSymptom(player, data, TAG_LAST_AIDS, AIDS_SYMPTOM_INTERVAL, false);
            if (time >= data.getLong(TAG_STAGE_END)) {
                setStage(player, DiseaseRegistry.AIDS_STAGE_2, AIDS_STAGE_2_DURATION);
            }
            return;
        }

        if (currentStage == DiseaseRegistry.AIDS_STAGE_2) {
            player.setSprinting(false);
            triggerSymptom(player, data, TAG_LAST_AIDS, AIDS_SYMPTOM_INTERVAL, false);
            if (time - data.getLong(TAG_LAST_DMG) >= STAGE2_DAMAGE_INTERVAL) {
                data.setLong(TAG_LAST_DMG, time);
                if (player.getRNG().nextBoolean()) {
                    player.attackEntityFrom(DiseaseRegistry.DISEASE_DAMAGE, 1.0F);
                }
            }
            if (time >= data.getLong(TAG_STAGE_END)) {
                setStage(player, DiseaseRegistry.VEGETABLE, VEGETABLE_STAGE_DURATION);
            }
            return;
        }

        if (currentStage == DiseaseRegistry.VEGETABLE) {
            player.setSprinting(false);
            player.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 220, 0, true, false));
            triggerSymptom(player, data, TAG_LAST_VEG, VEG_SYMPTOM_INTERVAL, true);

            long vegStart = data.getLong(TAG_VEG_START);
            if (vegStart <= 0L) {
                data.setLong(TAG_VEG_START, time);
            }
            if (time >= data.getLong(TAG_STAGE_END)) {
                clearAllStages(player);
                applyNaturalRecoveryDebuff(player);
            }
        }
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

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote) {
            return;
        }

        NBTTagCompound data = getDiseaseData(player);
        Potion currentStage = getStoredStage(data);
        if (currentStage == DiseaseRegistry.VEGETABLE) {
            spawnRupter(player, 1.0F);
        } else if (currentStage == DiseaseRegistry.AIDS_STAGE_2) {
            spawnRupter(player, 0.7F);
        }
    }

    public static boolean downgradeStage(EntityPlayer player) {
        NBTTagCompound data = getDiseaseData(player);
        Potion currentStage = getStoredStage(data);
        if (currentStage == DiseaseRegistry.VEGETABLE) {
            return false;
        }
        if (currentStage == DiseaseRegistry.AIDS_STAGE_2) {
            setStage(player, DiseaseRegistry.AIDS_STAGE_1, AIDS_STAGE_1_DURATION);
            return true;
        }
        if (currentStage == DiseaseRegistry.AIDS_STAGE_1) {
            setStage(player, DiseaseRegistry.HIV, HIV_STAGE_DURATION);
            return true;
        }
        if (currentStage == DiseaseRegistry.HIV) {
            clearAllStages(player);
            return true;
        }
        return false;
    }

    public static boolean isDiseased(EntityPlayer player) {
        return getResolvedStage(player) != null;
    }

    public static boolean infectIfHealthy(EntityPlayer player) {
        Potion currentStage = getResolvedStage(player);
        if (currentStage != null) {
            NBTTagCompound data = getDiseaseData(player);
            if (getStoredStage(data) == null) {
                data.setString(TAG_STAGE, currentStage.getRegistryName().toString());
                PotionEffect active = player.getActivePotionEffect(currentStage);
                long remaining = active == null ? getDefaultStageDuration(currentStage) : Math.max(40, active.getDuration());
                data.setLong(TAG_STAGE_END, player.world.getTotalWorldTime() + remaining);
            }
            clearOtherStageEffects(player, currentStage);
            return false;
        }
        setStage(player, DiseaseRegistry.HIV, HIV_STAGE_DURATION);
        return true;
    }

    public static void setStage(EntityPlayer player, Potion potion, int duration) {
        clearRecoveryDebuff(player);
        NBTTagCompound data = getDiseaseData(player);
        data.setString(TAG_STAGE, potion.getRegistryName().toString());
        data.setLong(TAG_STAGE_END, player.world.getTotalWorldTime() + Math.max(1, duration));
        data.removeTag(TAG_LAST_HIV);
        data.removeTag(TAG_LAST_AIDS);
        data.removeTag(TAG_LAST_VEG);
        data.removeTag(TAG_LAST_DMG);
        if (potion == DiseaseRegistry.VEGETABLE) {
            data.setLong(TAG_VEG_START, player.world.getTotalWorldTime());
        } else {
            data.removeTag(TAG_VEG_START);
        }

        clearStageEffects(player);
        player.addPotionEffect(new PotionEffect(potion, duration, 0, false, true));
        clearBiomass(player);
        clampHealth(player);
    }

    private static void clearAllStages(EntityPlayer player) {
        clearStageEffects(player);
        NBTTagCompound persisted = getPersistedData(player);
        persisted.removeTag(ROOT_TAG);
    }

    private static void clearStageEffects(EntityPlayer player) {
        player.removePotionEffect(DiseaseRegistry.HIV);
        player.removePotionEffect(DiseaseRegistry.AIDS_STAGE_1);
        player.removePotionEffect(DiseaseRegistry.AIDS_STAGE_2);
        player.removePotionEffect(DiseaseRegistry.VEGETABLE);
    }

    private static void ensureInfectedBiomassBlocked(EntityPlayer player) {
        if (!isDiseased(player)) {
            return;
        }
        clearBiomass(player);
    }

    private static void clearBiomass(EntityPlayer player) {
        ISemenStorage storage = player.getCapability(SemenProvider.SEMEN_CAP, null);
        if (storage == null) {
            return;
        }
        storage.setAmount(0);
        storage.setTickCounter(0);
        storage.setManualHarvesting(false);
        storage.setManualHarvestTicks(0);
    }

    private static void applyNaturalRecoveryDebuff(EntityPlayer player) {
        NBTTagCompound data = getDiseaseData(player);
        int debuff = player.getRNG().nextInt(4);
        data.setInteger(TAG_RECOVERY_DEBUFF, debuff);
        data.setLong(TAG_RECOVERY_DEBUFF_END, player.world.getTotalWorldTime() + RECOVERY_DEBUFF_DURATION);
        applyRecoveryDebuffEffect(player, debuff, RECOVERY_DEBUFF_DURATION);
    }

    private static void clearRecoveryDebuff(EntityPlayer player) {
        NBTTagCompound data = getDiseaseData(player);
        Potion potion = getRecoveryDebuffPotion(data.getInteger(TAG_RECOVERY_DEBUFF));
        data.removeTag(TAG_RECOVERY_DEBUFF);
        data.removeTag(TAG_RECOVERY_DEBUFF_END);
        if (potion != null) {
            player.removePotionEffect(potion);
        }
    }

    private static void ensureRecoveryDebuff(EntityPlayer player, NBTTagCompound data, long time) {
        if (!data.hasKey(TAG_RECOVERY_DEBUFF)) {
            return;
        }

        long end = data.getLong(TAG_RECOVERY_DEBUFF_END);
        if (time >= end) {
            clearRecoveryDebuff(player);
            return;
        }

        applyRecoveryDebuffEffect(player, data.getInteger(TAG_RECOVERY_DEBUFF), (int) Math.min(Integer.MAX_VALUE, Math.max(40L, end - time)));
    }

    private static void applyRecoveryDebuffEffect(EntityPlayer player, int debuff, int duration) {
        Potion potion = getRecoveryDebuffPotion(debuff);
        if (potion != null) {
            player.addPotionEffect(new PotionEffect(potion, duration, 0, true, false));
        }
    }

    private static Potion getRecoveryDebuffPotion(int debuff) {
        switch (debuff) {
            case 0:
                return MobEffects.SLOWNESS;
            case 1:
                return MobEffects.NAUSEA;
            case 2:
                return MobEffects.BLINDNESS;
            default:
                return MobEffects.POISON;
        }
    }

    private static void clampHealth(EntityPlayer player) {
        float max = player.getMaxHealth();
        if (player.getHealth() > max) {
            player.setHealth(max);
        }
    }

    private static void ensureStageEffect(EntityPlayer player, Potion stage, NBTTagCompound data, long time) {
        long remaining = Math.max(40L, data.getLong(TAG_STAGE_END) - time);
        clearOtherStageEffects(player, stage);
        PotionEffect active = player.getActivePotionEffect(stage);
        if (active == null || active.getDuration() < 40 || active.getAmplifier() != 0) {
            player.addPotionEffect(new PotionEffect(stage, (int) Math.min(Integer.MAX_VALUE, remaining), 0, false, true));
        }
    }


    private static void sanitizeDiseaseState(EntityPlayer player, NBTTagCompound data, long time) {
        Potion resolvedStage = getResolvedStage(player);
        Potion storedStage = getStoredStage(data);

        if (resolvedStage == null) {
            if (storedStage != null) {
                clearAllStages(player);
            }
            return;
        }

        if (storedStage != resolvedStage) {
            data.setString(TAG_STAGE, resolvedStage.getRegistryName().toString());
            PotionEffect active = player.getActivePotionEffect(resolvedStage);
            long remaining = active == null ? getDefaultStageDuration(resolvedStage) : Math.max(40, active.getDuration());
            data.setLong(TAG_STAGE_END, Math.max(time + 40L, time + remaining));
            if (resolvedStage == DiseaseRegistry.VEGETABLE) {
                if (!data.hasKey(TAG_VEG_START)) {
                    data.setLong(TAG_VEG_START, time);
                }
            } else {
                data.removeTag(TAG_VEG_START);
            }
        }

        clearOtherStageEffects(player, resolvedStage);
    }


    private static int getDefaultStageDuration(Potion potion) {
        if (potion == DiseaseRegistry.HIV) {
            return HIV_STAGE_DURATION;
        }
        if (potion == DiseaseRegistry.AIDS_STAGE_1) {
            return AIDS_STAGE_1_DURATION;
        }
        if (potion == DiseaseRegistry.AIDS_STAGE_2) {
            return AIDS_STAGE_2_DURATION;
        }
        if (potion == DiseaseRegistry.VEGETABLE) {
            return VEGETABLE_STAGE_DURATION;
        }
        return 20 * 60;
    }

    private static void clearOtherStageEffects(EntityPlayer player, Potion keep) {
        if (keep != DiseaseRegistry.HIV) {
            player.removePotionEffect(DiseaseRegistry.HIV);
        }
        if (keep != DiseaseRegistry.AIDS_STAGE_1) {
            player.removePotionEffect(DiseaseRegistry.AIDS_STAGE_1);
        }
        if (keep != DiseaseRegistry.AIDS_STAGE_2) {
            player.removePotionEffect(DiseaseRegistry.AIDS_STAGE_2);
        }
        if (keep != DiseaseRegistry.VEGETABLE) {
            player.removePotionEffect(DiseaseRegistry.VEGETABLE);
        }
    }

    private static Potion getResolvedStage(EntityPlayer player) {
        Potion storedStage = getStoredStage(getDiseaseData(player));
        Potion activeStage = getHighestActiveStage(player);
        if (storedStage == null) {
            return activeStage;
        }
        if (activeStage == null) {
            return storedStage;
        }
        return getStagePriority(activeStage) > getStagePriority(storedStage) ? activeStage : storedStage;
    }

    private static Potion getHighestActiveStage(EntityPlayer player) {
        if (player.isPotionActive(DiseaseRegistry.VEGETABLE)) {
            return DiseaseRegistry.VEGETABLE;
        }
        if (player.isPotionActive(DiseaseRegistry.AIDS_STAGE_2)) {
            return DiseaseRegistry.AIDS_STAGE_2;
        }
        if (player.isPotionActive(DiseaseRegistry.AIDS_STAGE_1)) {
            return DiseaseRegistry.AIDS_STAGE_1;
        }
        if (player.isPotionActive(DiseaseRegistry.HIV)) {
            return DiseaseRegistry.HIV;
        }
        return null;
    }

    private static int getStagePriority(Potion potion) {
        if (potion == DiseaseRegistry.HIV) {
            return 0;
        }
        if (potion == DiseaseRegistry.AIDS_STAGE_1) {
            return 1;
        }
        if (potion == DiseaseRegistry.AIDS_STAGE_2) {
            return 2;
        }
        if (potion == DiseaseRegistry.VEGETABLE) {
            return 3;
        }
        return -1;
    }

    private static void removeExternalCures(EntityPlayer player) {
        Potion viral = ForgeRegistries.POTIONS.getValue(new ResourceLocation("srparasites:viral"));
        if (viral != null && player.isPotionActive(viral)) {
            player.removePotionEffect(viral);
        }
    }

    private static void triggerSymptom(EntityPlayer player, NBTTagCompound data, String tag, int interval, boolean vegetableStage) {
        long time = player.world.getTotalWorldTime();
        if (time - data.getLong(tag) < interval) {
            return;
        }
        data.setLong(tag, time);

        if (!player.getRNG().nextBoolean()) {
            if (vegetableStage) {
                player.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 20 * 10, 0));
            }
            return;
        }

        int roll = player.getRNG().nextInt(vegetableStage ? 4 : 3);
        switch (roll) {
            case 0:
                player.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 20 * 10, 0));
                break;
            case 1:
                player.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 20 * 2, 0));
                break;
            case 2:
                player.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 20 * 3, 0));
                break;
            default:
                player.addPotionEffect(new PotionEffect(MobEffects.POISON, 20 * 4, 0));
                break;
        }
    }

    private static void spawnRupter(EntityPlayer player, float chance) {
        if (player.getRNG().nextFloat() > chance) {
            return;
        }

        World world = player.world;
        Entity entity = createParasite(world);
        if (entity == null) {
            return;
        }

        entity.setPosition(player.posX, player.posY, player.posZ);
        world.spawnEntity(entity);
    }

    private static Entity createParasite(World world) {
        String[] ids = {"srparasites:incompleteform_small"};
        for (String id : ids) {
            EntityEntry entry = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(id));
            if (entry != null) {
                return entry.newInstance(world);
            }
        }
        return null;
    }

    private static boolean isCurrentStage(EntityPlayer player, Potion potion) {
        return getStoredStage(getDiseaseData(player)) == potion;
    }

    private static Potion getStoredStage(NBTTagCompound data) {
        if (data == null || !data.hasKey(TAG_STAGE, 8)) {
            return null;
        }
        return ForgeRegistries.POTIONS.getValue(new ResourceLocation(data.getString(TAG_STAGE)));
    }

    private static NBTTagCompound getDiseaseData(EntityPlayer player) {
        NBTTagCompound persisted = getPersistedData(player);
        if (!persisted.hasKey(ROOT_TAG, 10)) {
            persisted.setTag(ROOT_TAG, new NBTTagCompound());
        }
        return persisted.getCompoundTag(ROOT_TAG);
    }

    private static NBTTagCompound getPersistedData(EntityPlayer player) {
        NBTTagCompound entityData = player.getEntityData();
        if (!entityData.hasKey(PERSISTED_TAG, 10)) {
            entityData.setTag(PERSISTED_TAG, new NBTTagCompound());
        }
        return entityData.getCompoundTag(PERSISTED_TAG);
    }
}
