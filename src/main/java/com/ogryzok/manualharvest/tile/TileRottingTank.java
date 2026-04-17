package com.ogryzok.manualharvest.tile;

import com.ogryzok.fluids.ModFluids;
import com.ogryzok.food.FoodRegistry;
import com.ogryzok.manualharvest.ManualHarvestRegistry;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.ItemHandlerHelper;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import javax.annotation.Nullable;

public class TileRottingTank extends TileEntity implements ITickable, IAnimatable {
    public static final int CAPACITY = 1000;
    public static final int STEP_MB = 200;
    public static final int MAX_STAGE = 5;
    public static final int ROTTING_TICKS = 8 * 60 * 20;

    private final AnimationFactory factory = new AnimationFactory(this);

    private final FluidTank rottenTank = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            markDirty();
            if (world != null && !world.isRemote) {
                if (getFluidAmount() <= 0) {
                    rottenReady = false;
                    closed = false;
                    rotting = false;
                    rottenTicksRemaining = 0;
                }
                syncNow();
            }
        }
    };

    private final IFluidHandler bottomOutput = new IFluidHandler() {
        @Override
        public IFluidTankProperties[] getTankProperties() {
            return rottenTank.getTankProperties();
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            if (resource == null) return null;
            Fluid current = getRottenFluid();
            if (current == null || resource.getFluid() != current) return null;
            return drain(resource.amount, doDrain);
        }

        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            if (!canDrainOutput()) return null;
            return rottenTank.drain(maxDrain, doDrain);
        }
    };

    private boolean closed = false;
    private boolean rotting = false;
    private boolean rottenReady = false;
    private int rottenTicksRemaining = 0;

    @Override
    public void update() {
        if (world == null) return;

        if (closed && rotting && !rottenReady && getFillAmount() > 0 && rottenTicksRemaining > 0) {
            rottenTicksRemaining--;

            if (rottenTicksRemaining <= 0) {
                rottenTicksRemaining = 0;
                rottenReady = true;
                rotting = false;
                closed = false;
                markDirty();
                if (!world.isRemote) {
                    syncNow();
                }
            } else if (!world.isRemote && rottenTicksRemaining % 20 == 0) {
                markDirty();
            }
        }
    }

    public boolean handleActivation(EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);

        if (player.isSneaking() && held.isEmpty()) {
            if (world.isRemote) return true;
            return toggleClosed(player);
        }

        if (world.isRemote) return true;
        if (!closed && !rottenReady && tryInsertBiomass(player, hand, held)) {
            return true;
        }
        return false;
    }

    private boolean toggleClosed(EntityPlayer player) {
        if (getFillAmount() <= 0) return false;

        if (rottenReady) {
            return false;
        }

        if (!closed) {
            closed = true;
            rotting = true;
            rottenReady = false;
            rottenTicksRemaining = ROTTING_TICKS;
            markDirty();
            syncNow();
            return true;
        }

        closed = false;
        rotting = false;
        rottenReady = false;
        rottenTicksRemaining = 0;
        markDirty();
        syncNow();
        return true;
    }

    private boolean tryInsertBiomass(EntityPlayer player, EnumHand hand, ItemStack held) {
        if (held.isEmpty()) return false;
        if (getFillAmount() + STEP_MB > CAPACITY) return false;

        ItemStack emptyContainer = FoodRegistry.getEmptyContainer(held);
        if (emptyContainer.isEmpty()) return false;

        if (!isAcceptedInputItem(held.getItem())) return false;

        rottenTank.fill(new FluidStack(getRottenFluid(), STEP_MB), true);
        consumeHeldAndGiveEmpty(player, hand, held, emptyContainer);
        closed = false;
        rotting = false;
        rottenReady = false;
        rottenTicksRemaining = 0;
        playInsertSound();
        markDirty();
        syncNow();
        return true;
    }

    private boolean isAcceptedInputItem(Item item) {
        return item == FoodRegistry.BIOMASS_CAN
                || item == FoodRegistry.DISTILLED_BIOMASS_CAN
                || item == FoodRegistry.ENRICHED_BIOMASS_CAN
                || item == FoodRegistry.BIOMASS_JAR
                || item == FoodRegistry.DISTILLED_BIOMASS_JAR
                || item == FoodRegistry.ENRICHED_BIOMASS_JAR
                || item == FoodRegistry.DIRTY_BIOMASS_WOODEN_JAR
                || item == ManualHarvestRegistry.DIRTY_BIOMASS;
    }

    private void consumeHeldAndGiveEmpty(EntityPlayer player, EnumHand hand, ItemStack held, ItemStack emptyContainer) {
        if (!player.capabilities.isCreativeMode) {
            held.shrink(1);
            if (held.isEmpty()) {
                player.setHeldItem(hand, emptyContainer.copy());
            } else {
                ItemHandlerHelper.giveItemToPlayer(player, emptyContainer.copy());
            }
        }
    }

    private void playInsertSound() {
        if (world == null) return;
        SoundEvent sound = net.minecraft.init.SoundEvents.BLOCK_SLIME_PLACE;
        world.playSound(null, pos, sound, SoundCategory.BLOCKS, 0.5F, 0.9F + world.rand.nextFloat() * 0.2F);
    }

    private boolean canDrainOutput() {
        return rottenReady && getFillAmount() > 0;
    }

    public void markForSync() {
        if (world != null && !world.isRemote) syncNow();
    }

    private void syncNow() {
        IBlockState state = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, state, state, 3);
        world.markChunkDirty(pos, this);
    }

    public int getFillAmount() {
        return rottenTank.getFluidAmount();
    }

    public int getVisualStageMb() {
        int amount = getFillAmount();
        if (amount <= 0) return 0;
        if (amount <= 200) return 200;
        if (amount <= 400) return 400;
        if (amount <= 600) return 600;
        if (amount <= 800) return 800;
        return 1000;
    }

    public int getStageIndex() {
        return Math.min(MAX_STAGE, Math.max(0, (getVisualStageMb() + STEP_MB - 1) / STEP_MB));
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean isRottenReady() {
        return rottenReady;
    }

    public boolean isRotting() {
        return closed && !rottenReady && rotting;
    }

    public int getRottenTicksRemaining() {
        return rottenTicksRemaining;
    }

    public String getOverlayText() {
        if (getFillAmount() <= 0) return "";
        if (closed) {
            if (rottenReady) {
                return "Biomass is ready";
            }
            int totalSeconds = Math.max(0, rottenTicksRemaining / 20);
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            return String.format("Rotting tank: %d:%02d", minutes, seconds);
        }
        if (rottenReady) {
            return "Biomass is ready";
        }
        return "";
    }

    @Nullable
    private Fluid getRottenFluid() {
        Fluid fluid = FluidRegistry.getFluid("fermented_semen");
        return fluid != null ? fluid : ModFluids.FERMENTED_SEMEN;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return facing == EnumFacing.DOWN;
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY && facing == EnumFacing.DOWN) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(bottomOutput);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setBoolean("Closed", closed);
        compound.setBoolean("Rotting", rotting);
        compound.setBoolean("RottenReady", rottenReady);
        compound.setInteger("RottenTicksRemaining", rottenTicksRemaining);
        NBTTagCompound tankTag = new NBTTagCompound();
        rottenTank.writeToNBT(tankTag);
        compound.setTag("RottenTank", tankTag);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        closed = compound.getBoolean("Closed");
        rotting = compound.getBoolean("Rotting");
        rottenReady = compound.getBoolean("RottenReady");
        rottenTicksRemaining = compound.getInteger("RottenTicksRemaining");
        rottenTank.readFromNBT(compound.getCompoundTag("RottenTank"));
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 1, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

    @Override public void registerControllers(AnimationData animationData) {}
    @Override public AnimationFactory getFactory() { return factory; }
}
