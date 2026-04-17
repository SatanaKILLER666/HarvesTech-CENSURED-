package com.ogryzok.player.semen;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

public class SemenProvider implements ICapabilitySerializable<NBTTagCompound> {

    @CapabilityInject(ISemenStorage.class)
    public static final Capability<ISemenStorage> SEMEN_CAP = null;

    private final ISemenStorage instance = new SemenStorage();

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == SEMEN_CAP;
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        return capability == SEMEN_CAP ? SEMEN_CAP.cast(instance) : null;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        return (NBTTagCompound) SEMEN_CAP.getStorage().writeNBT(SEMEN_CAP, instance, null);
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        SEMEN_CAP.getStorage().readNBT(SEMEN_CAP, instance, null, nbt);
    }
}