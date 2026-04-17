package com.ogryzok.semendestiller.tile;

import com.ogryzok.semendestiller.SemenDestillerRegistry;
import com.ogryzok.semendestiller.block.BlockSemenDestillerBase;
import com.ogryzok.semendestiller.block.BlockSemenDestillerMotor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import javax.annotation.Nullable;

public class TileSemenDestillerMotor extends TileEntity implements ITickable, IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);
    private boolean assembled = false;
    @Override public void update() { if (!world.isRemote && world.getTotalWorldTime() % 10L == 0L) syncStructureState(); }
    public void syncStructureState() { boolean nv = checkAssembled(); if (nv != assembled) { assembled = nv; markDirty(); IBlockState state = world.getBlockState(pos); world.notifyBlockUpdate(pos, state, state, 3); } }
    public boolean isAssembled() { return checkAssembled(); }
    private boolean checkAssembled() { if (world == null || pos == null) return false; IBlockState self = world.getBlockState(pos); if (self.getBlock() != SemenDestillerRegistry.SEMEN_DESTILLER_MOTOR) return false; IBlockState below = world.getBlockState(pos.down()); if (below.getBlock() != SemenDestillerRegistry.SEMEN_DESTILLER_BASE) return false; return self.getValue(BlockSemenDestillerMotor.FACING) == below.getValue(BlockSemenDestillerBase.FACING); }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound compound) { super.writeToNBT(compound); compound.setBoolean("Assembled", assembled); return compound; }
    @Override public void readFromNBT(NBTTagCompound compound) { super.readFromNBT(compound); assembled = compound.getBoolean("Assembled"); }
    @Override public NBTTagCompound getUpdateTag() { return writeToNBT(new NBTTagCompound()); }
    @Nullable @Override public SPacketUpdateTileEntity getUpdatePacket() { return new SPacketUpdateTileEntity(this.pos, 0, getUpdateTag()); }
    @Override public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) { readFromNBT(pkt.getNbtCompound()); }
    @Override public void registerControllers(AnimationData animationData) {}
    @Override public AnimationFactory getFactory() { return factory; }
}
