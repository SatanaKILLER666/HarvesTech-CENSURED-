package com.ogryzok.player.semen;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;

public class SemenCapability {

    public static void register() {
        CapabilityManager.INSTANCE.register(ISemenStorage.class, new Capability.IStorage<ISemenStorage>() {
            @Override
            public NBTBase writeNBT(Capability<ISemenStorage> capability, ISemenStorage instance, EnumFacing side) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setInteger("Amount", instance.getAmount());
                tag.setInteger("TickCounter", instance.getTickCounter());
                tag.setBoolean("ManualHarvesting", instance.isManualHarvesting());
                tag.setInteger("ManualHarvestTicks", instance.getManualHarvestTicks());
                tag.setInteger("ManualHarvestCooldownTicks", instance.getManualHarvestCooldownTicks());
                tag.setBoolean("SteroidHarvest", instance.isSteroidHarvest());
                tag.setBoolean("SteroidLoadedShot", instance.isSteroidLoadedShot());
                tag.setInteger("SteroidHarvestLevel", instance.getSteroidHarvestLevel());
                tag.setInteger("AbstinenceTicks", instance.getAbstinenceTicks());
                tag.setInteger("AbstinenceStage", instance.getAbstinenceStage());
                tag.setBoolean("SeedKeeper", instance.hasSeedKeeper());
                tag.setDouble("ManualHarvestStartY", instance.getManualHarvestStartY());
                tag.setInteger("WeaknessLockTicks", instance.getWeaknessLockTicks());
                tag.setInteger("KeeperHoverTicks", instance.getKeeperHoverTicks());
                tag.setInteger("KeeperGlideTicks", instance.getKeeperGlideTicks());
                tag.setInteger("UnstableBeamTicks", instance.getUnstableBeamTicks());
                tag.setInteger("UnstableBeamStage", instance.getUnstableBeamStage());
                tag.setInteger("LastBurstRollMinute", instance.getLastBurstRollMinute());
                return tag;
            }

            @Override
            public void readNBT(Capability<ISemenStorage> capability, ISemenStorage instance, EnumFacing side, NBTBase nbt) {
                if (!(nbt instanceof NBTTagCompound)) return;

                NBTTagCompound tag = (NBTTagCompound) nbt;
                instance.setAmount(tag.getInteger("Amount"));
                instance.setTickCounter(tag.getInteger("TickCounter"));
                instance.setManualHarvesting(tag.getBoolean("ManualHarvesting"));
                instance.setManualHarvestTicks(tag.getInteger("ManualHarvestTicks"));
                instance.setManualHarvestCooldownTicks(tag.getInteger("ManualHarvestCooldownTicks"));
                instance.setSteroidHarvest(tag.getBoolean("SteroidHarvest"));
                instance.setSteroidLoadedShot(tag.getBoolean("SteroidLoadedShot"));
                instance.setSteroidHarvestLevel(tag.getInteger("SteroidHarvestLevel"));
                instance.setAbstinenceTicks(tag.getInteger("AbstinenceTicks"));
                instance.setAbstinenceStage(tag.getInteger("AbstinenceStage"));
                instance.setSeedKeeper(tag.getBoolean("SeedKeeper"));
                instance.setManualHarvestStartY(tag.getDouble("ManualHarvestStartY"));
                instance.setWeaknessLockTicks(tag.getInteger("WeaknessLockTicks"));
                instance.setKeeperHoverTicks(tag.getInteger("KeeperHoverTicks"));
                instance.setKeeperGlideTicks(tag.getInteger("KeeperGlideTicks"));
                instance.setUnstableBeamTicks(tag.getInteger("UnstableBeamTicks"));
                instance.setUnstableBeamStage(tag.getInteger("UnstableBeamStage"));
                instance.setLastBurstRollMinute(tag.getInteger("LastBurstRollMinute"));
            }
        }, SemenStorage::new);
    }
}
