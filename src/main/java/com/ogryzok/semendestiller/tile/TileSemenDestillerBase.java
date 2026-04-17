package com.ogryzok.semendestiller.tile;

import com.ogryzok.food.FoodRegistry;
import com.ogryzok.fluids.ModFluids;
import com.ogryzok.semendestiller.SemenDestillerRegistry;
import com.ogryzok.semendestiller.block.BlockSemenDestillerBase;
import com.ogryzok.semendestiller.block.BlockSemenDestillerMotor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraft.world.WorldServer;
import com.ogryzok.semendestiller.sound.SemenDestillerSoundRegistry;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import javax.annotation.Nullable;

public class TileSemenDestillerBase extends TileEntity implements ITickable, IEnergyStorage, IAnimatable {
    private static final int MAX_ENERGY = 20000;
    private static final int MAX_INPUT = 4000;
    private static final int MAX_OUTPUT = 4000;
    private static final int PROCESS_BATCH_MB = 200;
    private static final int PROCESS_TIME_TICKS = 160;
    private static final int TOXIC_FLESH_PROCESS_TIME_TICKS = 240;
    private static final int PROCESS_ENERGY_PER_BATCH = 1000;
    private static final int CAN_FILL_ENERGY = 80;
    private static final int CAN_VOLUME = 200;
    private static final int CAN_FILL_TIME_TICKS = 20;
    private static final int AUTO_OUTPUT_RATE = 50;
    private static final int STEAM_STEP_MB = 100;


    private final AnimationFactory factory = new AnimationFactory(this);

    private int energyStored;
    private boolean assembled;
    private boolean active;
    private boolean distilling;
    private int processTime;
    private int processMovedMb;
    private int processTargetMb;
    private int processEnergyUsed;
    private int processTargetEnergy;
    private int canFillTime;
    private int steamProgressMb;

    private final FluidTank biomassTank = new FluidTank(MAX_INPUT) {
        @Override
        public boolean canFillFluidType(FluidStack fluid) {
            return fluid != null && isValidInputFluid(fluid.getFluid());
        }

        @Override
        protected void onContentsChanged() {
            markDirty();
            syncNow();
        }
    };

    private final FluidTank distilledTank = new FluidTank(MAX_OUTPUT) {
        @Override
        public boolean canFillFluidType(FluidStack fluid) {
            return fluid != null && isValidOutputFluid(fluid.getFluid());
        }

        @Override
        protected void onContentsChanged() {
            markDirty();
            syncNow();
        }
    };

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
            syncNow();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) return FoodRegistry.isEmptyBiomassContainer(stack);
            if (slot == 1) return !stack.isEmpty() && (stack.getItem() == FoodRegistry.DISTILLED_BIOMASS_CAN || stack.getItem() == FoodRegistry.DISTILLED_BIOMASS_JAR);
            return false;
        }
    };

    private final IFluidHandler leftInput = new IFluidHandler() {
        @Override
        public IFluidTankProperties[] getTankProperties() {
            return biomassTank.getTankProperties();
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return biomassTank.fill(resource, doFill);
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            return null;
        }

        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            return null;
        }
    };

    private final IFluidHandler rightOutput = new IFluidHandler() {
        @Override
        public IFluidTankProperties[] getTankProperties() {
            return distilledTank.getTankProperties();
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            if (resource == null || !canDrainRequestedFluid(resource.getFluid())) return null;
            return distilledTank.drain(resource.amount, doDrain);
        }

        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            return distilledTank.drain(maxDrain, doDrain);
        }
    };

    private final IFluidHandler internalHandler = new IFluidHandler() {
        @Override
        public IFluidTankProperties[] getTankProperties() {
            IFluidTankProperties[] inProps = biomassTank.getTankProperties();
            IFluidTankProperties[] outProps = distilledTank.getTankProperties();
            IFluidTankProperties[] result = new IFluidTankProperties[inProps.length + outProps.length];
            System.arraycopy(inProps, 0, result, 0, inProps.length);
            System.arraycopy(outProps, 0, result, inProps.length, outProps.length);
            return result;
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return biomassTank.fill(resource, doFill);
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            if (resource == null || !canDrainRequestedFluid(resource.getFluid())) return null;
            return distilledTank.drain(resource.amount, doDrain);
        }

        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            return distilledTank.drain(maxDrain, doDrain);
        }
    };

    private boolean isValidInputFluid(@Nullable net.minecraftforge.fluids.Fluid fluid) {
        return fluid == ModFluids.BIOMASS || fluid == ModFluids.FERMENTED_SEMEN || fluid == ModFluids.TOXIC_FLESH;
    }

    private boolean isValidOutputFluid(@Nullable net.minecraftforge.fluids.Fluid fluid) {
        return fluid == ModFluids.DISTILLED_BIOMASS || fluid == ModFluids.DISTILLED_FERMENTED_SEMEN || fluid == ModFluids.NECRO_SUBSTRATE;
    }

    private boolean canDrainRequestedFluid(@Nullable net.minecraftforge.fluids.Fluid fluid) {
        FluidStack stored = distilledTank.getFluid();
        return stored != null && stored.getFluid() == fluid;
    }

    private int getCurrentProcessTimeTicks() {
        FluidStack input = biomassTank.getFluid();
        if (input != null && input.getFluid() == ModFluids.TOXIC_FLESH) {
            return TOXIC_FLESH_PROCESS_TIME_TICKS;
        }
        return PROCESS_TIME_TICKS;
    }

    @Nullable
    private net.minecraftforge.fluids.Fluid getCurrentOutputFluid() {
        FluidStack input = biomassTank.getFluid();
        if (input == null || input.getFluid() == null) {
            FluidStack output = distilledTank.getFluid();
            return output == null ? null : output.getFluid();
        }

        if (input.getFluid() == ModFluids.FERMENTED_SEMEN) {
            return ModFluids.DISTILLED_FERMENTED_SEMEN;
        }

        if (input.getFluid() == ModFluids.TOXIC_FLESH) {
            return ModFluids.NECRO_SUBSTRATE;
        }

        return ModFluids.DISTILLED_BIOMASS;
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        if (world.getTotalWorldTime() % 10L == 0L) {
            syncStructureState();
        }

        if (!assembled) {
            active = false;
            processTime = 0;
            processMovedMb = 0;
            processTargetMb = 0;
            processEnergyUsed = 0;
            processTargetEnergy = 0;
            canFillTime = 0;
            distilling = false;
            steamProgressMb = 0;
            return;
        }

        boolean wasDistilling = distilling;
        boolean worked = tickDistillation();
        if (tickCanFilling()) {
            worked = true;
        }

        pushDistilledOutput();

        boolean stateChanged = false;
        if (active != worked) {
            active = worked;
            stateChanged = true;
        }
        if (distilling != wasDistilling) {
            stateChanged = true;
        }
        if (stateChanged) {
            markDirty();
            syncNow();
        }
    }

    private boolean tickDistillation() {
        net.minecraftforge.fluids.Fluid outputFluid = getCurrentOutputFluid();
        FluidStack storedInput = biomassTank.getFluid();
        FluidStack storedOutput = distilledTank.getFluid();
        boolean outputCompatible = outputFluid != null && (storedOutput == null || storedOutput.amount <= 0 || storedOutput.getFluid() == outputFluid);

        if (!outputCompatible || storedInput == null || storedInput.getFluid() == null) {
            if (processTime <= 0) {
                processMovedMb = 0;
                processTargetMb = 0;
                processEnergyUsed = 0;
                processTargetEnergy = 0;
            }
            distilling = false;
            steamProgressMb = 0;
            return false;
        }

        if (processTime <= 0 || processTargetMb <= 0) {
            int inputAvailable = biomassTank.getFluidAmount();
            int outputSpace = distilledTank.getCapacity() - distilledTank.getFluidAmount();
            int maxByEnergy = energyStored * PROCESS_BATCH_MB / PROCESS_ENERGY_PER_BATCH;

            int targetMb;
            if (storedInput.getFluid() == ModFluids.TOXIC_FLESH) {
                targetMb = Math.min(inputAvailable, Math.min(outputSpace, Math.max(0, maxByEnergy)));
                if (targetMb > PROCESS_BATCH_MB) {
                    targetMb = PROCESS_BATCH_MB;
                }
            } else {
                targetMb = PROCESS_BATCH_MB;
            }

            if (targetMb <= 0) {
                processMovedMb = 0;
                processTargetMb = 0;
                processEnergyUsed = 0;
                processTargetEnergy = 0;
                distilling = false;
                steamProgressMb = 0;
                return false;
            }

            int targetEnergy = (targetMb * PROCESS_ENERGY_PER_BATCH + PROCESS_BATCH_MB - 1) / PROCESS_BATCH_MB;
            boolean canStart = biomassTank.getFluidAmount() >= targetMb
                    && outputSpace >= targetMb
                    && energyStored >= targetEnergy;
            if (!canStart) {
                processMovedMb = 0;
                processTargetMb = 0;
                processEnergyUsed = 0;
                processTargetEnergy = 0;
                distilling = false;
                steamProgressMb = 0;
                return false;
            }

            processTargetMb = targetMb;
            processTargetEnergy = targetEnergy;
        }

        boolean canContinue = biomassTank.getFluidAmount() >= (processTargetMb - processMovedMb)
                && distilledTank.getCapacity() - distilledTank.getFluidAmount() >= (processTargetMb - processMovedMb)
                && energyStored >= (processTargetEnergy - processEnergyUsed);
        if (!canContinue) {
            distilling = false;
            return false;
        }

        int currentProcessTimeTicks = getCurrentProcessTimeTicks();

        distilling = true;
        processTime++;

        int targetMoved = processTime * processTargetMb / currentProcessTimeTicks;
        int deltaMoved = targetMoved - processMovedMb;
        if (deltaMoved > 0) {
            FluidStack drained = biomassTank.drain(deltaMoved, true);
            int moved = drained == null ? 0 : drained.amount;
            if (moved > 0) {
                if (outputFluid != null) {
                    distilledTank.fill(new FluidStack(outputFluid, moved), true);
                }
                processMovedMb += moved;
                emitSteamBurstsIfNeeded();
            }
        }

        int targetEnergy = processTime * processTargetEnergy / currentProcessTimeTicks;
        int deltaEnergy = targetEnergy - processEnergyUsed;
        if (deltaEnergy > 0) {
            energyStored -= deltaEnergy;
            if (energyStored < 0) energyStored = 0;
            processEnergyUsed += deltaEnergy;
            markDirty();
            syncNow();
        }

        if (processTime >= currentProcessTimeTicks) {
            processTime = 0;
            processMovedMb = 0;
            processTargetMb = 0;
            processEnergyUsed = 0;
            processTargetEnergy = 0;
            steamProgressMb = 0;
            markDirty();
            syncNow();
        }

        return true;
    }

    private void emitSteamBurstsIfNeeded() {
        while (processMovedMb - steamProgressMb >= STEAM_STEP_MB) {
            steamProgressMb += STEAM_STEP_MB;
            spawnSteamBurst();
        }
    }

    private void spawnSteamBurst() {
        if (world == null || world.isRemote || pos == null) {
            return;
        }

        world.playSound(null, pos, SemenDestillerSoundRegistry.DISTILLER_STEAM, SoundCategory.BLOCKS, 0.85F, 0.95F + world.rand.nextFloat() * 0.15F);

        if (!(world instanceof WorldServer)) {
            return;
        }

        WorldServer ws = (WorldServer) world;
        double cx = pos.getX() + 0.5D;
        double topY = pos.getY() + 1.12D;
        double innerY = pos.getY() + 0.72D;

        ws.spawnParticle(EnumParticleTypes.CLOUD, cx, topY, pos.getZ() + 0.5D, 7, 0.11D, 0.06D, 0.11D, 0.012D);
        ws.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, cx, topY + 0.03D, pos.getZ() + 0.5D, 5, 0.08D, 0.04D, 0.08D, 0.006D);
        ws.spawnParticle(EnumParticleTypes.CLOUD, cx, innerY, pos.getZ() + 0.5D, 4, 0.09D, 0.03D, 0.09D, 0.008D);
    }

    private boolean tickCanFilling() {
        ItemStack canStack = inventory.getStackInSlot(0);
        if (!FoodRegistry.isEmptyBiomassContainer(canStack)) {
            canFillTime = 0;
            return false;
        }

        if (distilledTank.getFluidAmount() < CAN_VOLUME || energyStored < CAN_FILL_ENERGY) {
            canFillTime = 0;
            return false;
        }

        ItemStack result = FoodRegistry.getFilledDistilledContainer(canStack);
        if (result.isEmpty()) {
            canFillTime = 0;
            return false;
        }
        ItemStack simulated = inventory.insertItem(1, result.copy(), true);
        if (!simulated.isEmpty()) {
            canFillTime = 0;
            return false;
        }

        canFillTime++;
        if (canFillTime < CAN_FILL_TIME_TICKS) {
            return true;
        }

        canFillTime = 0;
        inventory.extractItem(0, 1, false);
        inventory.insertItem(1, result.copy(), false);
        distilledTank.drain(CAN_VOLUME, true);
        energyStored -= CAN_FILL_ENERGY;
        markDirty();
        syncNow();
        return true;
    }

    private void pushDistilledOutput() {
        if (distilledTank.getFluidAmount() <= 0 || world == null || pos == null) {
            return;
        }

        EnumFacing out = getFacing().rotateYCCW();
        TileEntity targetTile = world.getTileEntity(pos.offset(out));
        if (targetTile == null || !targetTile.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, out.getOpposite())) {
            return;
        }

        IFluidHandler target = targetTile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, out.getOpposite());
        if (target == null) {
            return;
        }

        int amount = Math.min(AUTO_OUTPUT_RATE, distilledTank.getFluidAmount());
        FluidStack stored = distilledTank.getFluid();
        if (stored == null || stored.getFluid() == null) {
            return;
        }

        FluidStack offer = new FluidStack(stored.getFluid(), amount);
        int accepted = target.fill(offer, false);
        if (accepted <= 0) {
            return;
        }

        FluidStack drained = rightOutput.drain(Math.min(amount, accepted), true);
        if (drained == null || drained.amount <= 0) {
            return;
        }

        target.fill(drained, true);
        syncNow();
    }

    public void syncStructureState() {
        boolean nv = checkAssembled();
        if (nv != assembled) {
            assembled = nv;
            syncNow();
        }
    }

    public boolean isAssembled() {
        return checkAssembled();
    }

    private boolean checkAssembled() {
        if (world == null || pos == null) return false;
        IBlockState self = world.getBlockState(pos);
        if (self.getBlock() != SemenDestillerRegistry.SEMEN_DESTILLER_BASE) return false;
        IBlockState above = world.getBlockState(pos.up());
        if (above.getBlock() != SemenDestillerRegistry.SEMEN_DESTILLER_MOTOR) return false;
        return self.getValue(BlockSemenDestillerBase.FACING) == above.getValue(BlockSemenDestillerMotor.FACING);
    }

    public EnumFacing getFacing() {
        if (world == null || pos == null) return EnumFacing.NORTH;
        IBlockState state = world.getBlockState(pos);
        return state.getBlock() == SemenDestillerRegistry.SEMEN_DESTILLER_BASE
                ? state.getValue(BlockSemenDestillerBase.FACING)
                : EnumFacing.NORTH;
    }

    public int getBiomassStored() {
        return biomassTank.getFluidAmount();
    }

    public int getDistilledStored() {
        return distilledTank.getFluidAmount();
    }

    public int getMaxInput() {
        return biomassTank.getCapacity();
    }

    @Nullable
    public FluidStack getBiomassFluid() {
        return biomassTank.getFluid();
    }

    @Nullable
    public FluidStack getDistilledFluid() {
        return distilledTank.getFluid();
    }

    public int getMaxOutput() {
        return distilledTank.getCapacity();
    }

    public boolean isActiveNow() {
        return active;
    }

    public boolean isDistillingNow() {
        return distilling;
    }

    public int getProcessProgressScaled(int pixels) {
        int currentProcessTimeTicks = getCurrentProcessTimeTicks();
        if (currentProcessTimeTicks <= 0 || processTime <= 0) return 0;
        return Math.min(pixels, processTime * pixels / currentProcessTimeTicks);
    }

    public int getCanFillProgressScaled(int pixels) {
        if (CAN_FILL_TIME_TICKS <= 0 || canFillTime <= 0) return 0;
        return Math.min(pixels, canFillTime * pixels / CAN_FILL_TIME_TICKS);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    private void syncNow() {
        if (world == null || world.isRemote) return;
        markDirty();
        IBlockState state = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, state, state, 3);
    }

    private boolean canExposeEnergyTo(@Nullable EnumFacing facing) {
        return facing == null || facing == EnumFacing.DOWN || facing == getFacing().getOpposite();
    }

    private boolean canFillBiomassFrom(@Nullable EnumFacing facing) {
        return facing == null || facing == getFacing().rotateY();
    }

    private boolean canDrainDistilledTo(@Nullable EnumFacing facing) {
        return facing == null || facing == getFacing().rotateYCCW();
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) return canExposeEnergyTo(facing);
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return canFillBiomassFrom(facing) || canDrainDistilledTo(facing);
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return canExposeEnergyTo(facing) ? CapabilityEnergy.ENERGY.cast(this) : null;
        }

        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            if (facing == null) return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(internalHandler);
            if (canFillBiomassFrom(facing) && canDrainDistilledTo(facing)) {
                return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(internalHandler);
            }
            if (canFillBiomassFrom(facing)) {
                return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(leftInput);
            }
            if (canDrainDistilledTo(facing)) {
                return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(rightOutput);
            }
            return null;
        }

        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
        }

        return super.getCapability(capability, facing);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("Energy", energyStored);
        compound.setBoolean("Assembled", assembled);
        compound.setBoolean("Active", active);
        compound.setBoolean("Distilling", distilling);
        compound.setTag("BiomassTank", biomassTank.writeToNBT(new NBTTagCompound()));
        compound.setTag("DistilledTank", distilledTank.writeToNBT(new NBTTagCompound()));
        compound.setTag("Items", inventory.serializeNBT());
        compound.setInteger("ProcessTime", processTime);
        compound.setInteger("ProcessMovedMb", processMovedMb);
        compound.setInteger("ProcessTargetMb", processTargetMb);
        compound.setInteger("ProcessEnergyUsed", processEnergyUsed);
        compound.setInteger("ProcessTargetEnergy", processTargetEnergy);
        compound.setInteger("CanFillTime", canFillTime);
        compound.setInteger("SteamProgressMb", steamProgressMb);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        energyStored = compound.getInteger("Energy");
        assembled = compound.getBoolean("Assembled");
        active = compound.getBoolean("Active");
        distilling = compound.getBoolean("Distilling");

        if (compound.hasKey("BiomassTank")) {
            biomassTank.readFromNBT(compound.getCompoundTag("BiomassTank"));
        } else if (compound.hasKey("Biomass")) {
            biomassTank.setFluid(new FluidStack(ModFluids.BIOMASS, compound.getInteger("Biomass")));
        }

        if (compound.hasKey("DistilledTank")) {
            distilledTank.readFromNBT(compound.getCompoundTag("DistilledTank"));
        } else if (compound.hasKey("Distilled")) {
            distilledTank.setFluid(new FluidStack(ModFluids.DISTILLED_BIOMASS, compound.getInteger("Distilled")));
        }

        inventory.deserializeNBT(compound.getCompoundTag("Items"));
        processTime = compound.getInteger("ProcessTime");
        processMovedMb = compound.getInteger("ProcessMovedMb");
        processTargetMb = compound.getInteger("ProcessTargetMb");
        processEnergyUsed = compound.getInteger("ProcessEnergyUsed");
        processTargetEnergy = compound.getInteger("ProcessTargetEnergy");
        canFillTime = compound.getInteger("CanFillTime");
        steamProgressMb = compound.getInteger("SteamProgressMb");
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!isAssembled()) return 0;
        int received = Math.min(MAX_ENERGY - energyStored, Math.max(0, maxReceive));
        if (!simulate && received > 0) {
            energyStored += received;
            syncNow();
        }
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return energyStored;
    }

    @Override
    public int getMaxEnergyStored() {
        return MAX_ENERGY;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    @Override
    public void registerControllers(AnimationData animationData) {
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }
}