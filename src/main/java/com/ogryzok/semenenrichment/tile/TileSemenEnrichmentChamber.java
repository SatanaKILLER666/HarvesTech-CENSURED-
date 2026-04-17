package com.ogryzok.semenenrichment.tile;

import com.ogryzok.food.FoodRegistry;
import com.ogryzok.fluids.ModFluids;
import com.ogryzok.semenenrichment.SemenEnrichmentRegistry;
import com.ogryzok.semenenrichment.block.BlockSemenEnrichmentChamber;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import javax.annotation.Nullable;

public class TileSemenEnrichmentChamber extends TileEntity implements ITickable, IEnergyStorage, IAnimatable {
    private static final int MAX_ENERGY = 100000;
    private static final int DEFAULT_PROCESS_BATCH_MB = 200;
    private static final int DEFAULT_PROCESS_TIME_TICKS = 160;
    private static final int NECRO_PROCESS_TIME_TICKS = 240;
    private static final int DEFAULT_PROCESS_ENERGY_PER_BATCH = 1000;
    private static final int TANK_CAPACITY = 4000;
    private static final int CAN_VOLUME = 200;
    private static final int CAN_FILL_TIME_TICKS = 20;
    private static final int CAN_FILL_ENERGY = 80;
    private static final int AUTO_OUTPUT_RATE = 50;

    private final AnimationFactory factory = new AnimationFactory(this);
    private int energyStored = 0;
    private boolean active = false;
    private int processTime = 0;
    private int processMovedMb = 0;
    private int processEnergyUsed = 0;
    private int processTargetMb = 0;
    private int processTargetEnergy = 0;
    private int processTimeTotal = 0;
    private int canFillTime = 0;

    private final FluidTank inputTank = new FluidTank(TANK_CAPACITY) {
        @Override
        public boolean canFillFluidType(FluidStack fluid) {
            return isAcceptedInputFluid(fluid);
        }

        @Override
        protected void onContentsChanged() {
            markForSync();
        }
    };

    private final FluidTank outputTank = new FluidTank(TANK_CAPACITY) {
        @Override
        public boolean canFillFluidType(FluidStack fluid) {
            return isAcceptedOutputFluid(fluid);
        }

        @Override
        protected void onContentsChanged() {
            markForSync();
        }
    };

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            markForSync();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) return FoodRegistry.isEmptyBiomassContainer(stack);
            if (slot == 1) {
                return !stack.isEmpty() && (stack.getItem() == FoodRegistry.ENRICHED_BIOMASS_CAN || stack.getItem() == FoodRegistry.ENRICHED_BIOMASS_JAR);
            }
            return false;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }
    };

    private final IFluidHandler leftInput = new IFluidHandler() {
        @Override
        public IFluidTankProperties[] getTankProperties() {
            return inputTank.getTankProperties();
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return inputTank.fill(resource, doFill);
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
            return outputTank.getTankProperties();
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            if (resource == null || !canDrainRequestedFluid(resource)) return null;
            return outputTank.drain(resource.amount, doDrain);
        }

        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            return outputTank.drain(maxDrain, doDrain);
        }
    };

    private final IFluidHandler internalHandler = new IFluidHandler() {
        @Override
        public IFluidTankProperties[] getTankProperties() {
            IFluidTankProperties[] inProps = inputTank.getTankProperties();
            IFluidTankProperties[] outProps = outputTank.getTankProperties();
            IFluidTankProperties[] result = new IFluidTankProperties[inProps.length + outProps.length];
            System.arraycopy(inProps, 0, result, 0, inProps.length);
            System.arraycopy(outProps, 0, result, inProps.length, outProps.length);
            return result;
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return inputTank.fill(resource, doFill);
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            if (resource == null || !canDrainRequestedFluid(resource)) return null;
            return outputTank.drain(resource.amount, doDrain);
        }

        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            return outputTank.drain(maxDrain, doDrain);
        }
    };

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        boolean worked = tickEnrichment();
        if (tickCanFilling()) {
            worked = true;
        }

        pushEnrichedOutput();

        if (active != worked) {
            active = worked;
            markForSync();
        }
    }

    private boolean tickEnrichment() {
        FluidStack input = inputTank.getFluid();
        Fluid outputFluidForRecipe = getCurrentRecipeOutputFluid();
        boolean outputCompatible = outputFluidForRecipe != null &&
                (outputTank.getFluidAmount() <= 0 || (outputTank.getFluid() != null && outputTank.getFluid().getFluid() == outputFluidForRecipe));

        if (!outputCompatible || input == null || input.amount <= 0) {
            if (processTime <= 0) {
                resetProcess();
            }
            return false;
        }

        if (processTime <= 0 || processTargetMb <= 0 || processTimeTotal <= 0) {
            beginProcess(input, outputFluidForRecipe);
        }

        boolean canProcess = processTargetMb > 0
                && processTimeTotal > 0
                && inputTank.getFluidAmount() >= (processTargetMb - processMovedMb)
                && outputTank.getCapacity() - outputTank.getFluidAmount() >= (processTargetMb - processMovedMb)
                && energyStored >= (processTargetEnergy - processEnergyUsed);

        if (!canProcess) {
            if (processTime <= 0) {
                resetProcess();
            }
            return false;
        }

        processTime++;

        int targetMoved = processTime * processTargetMb / processTimeTotal;
        int deltaMoved = targetMoved - processMovedMb;
        if (deltaMoved > 0) {
            FluidStack drained = inputTank.drain(deltaMoved, true);
            int moved = drained == null ? 0 : drained.amount;
            if (moved > 0) {
                outputTank.fill(new FluidStack(outputFluidForRecipe, moved), true);
                processMovedMb += moved;
            }
        }

        int targetEnergy = processTime * processTargetEnergy / processTimeTotal;
        int deltaEnergy = targetEnergy - processEnergyUsed;
        if (deltaEnergy > 0) {
            energyStored -= deltaEnergy;
            if (energyStored < 0) energyStored = 0;
            processEnergyUsed += deltaEnergy;
            markForSync();
        }

        if (processTime >= processTimeTotal) {
            resetProcess();
            markForSync();
        }

        return true;
    }

    private void beginProcess(FluidStack input, Fluid outputFluid) {
        boolean necroRecipe = isNecroSubstrate(input) && outputFluid == ModFluids.BIO_TOXIN;
        int maxByOutput = outputTank.getCapacity() - outputTank.getFluidAmount();
        int maxByEnergy = energyStored <= 0 ? 0 : Math.max(1, energyStored * DEFAULT_PROCESS_BATCH_MB / DEFAULT_PROCESS_ENERGY_PER_BATCH);
        int requestedAmount = necroRecipe
                ? Math.min(DEFAULT_PROCESS_BATCH_MB, Math.min(input.amount, maxByOutput))
                : DEFAULT_PROCESS_BATCH_MB;

        if (necroRecipe) {
            requestedAmount = Math.min(requestedAmount, maxByEnergy);
        }

        if (requestedAmount <= 0) {
            resetProcess();
            return;
        }

        processTargetMb = requestedAmount;
        processTargetEnergy = Math.max(1, (requestedAmount * DEFAULT_PROCESS_ENERGY_PER_BATCH + DEFAULT_PROCESS_BATCH_MB - 1) / DEFAULT_PROCESS_BATCH_MB);
        processTimeTotal = necroRecipe ? NECRO_PROCESS_TIME_TICKS : DEFAULT_PROCESS_TIME_TICKS;
        processTime = 0;
        processMovedMb = 0;
        processEnergyUsed = 0;
    }

    private void resetProcess() {
        processTime = 0;
        processMovedMb = 0;
        processEnergyUsed = 0;
        processTargetMb = 0;
        processTargetEnergy = 0;
        processTimeTotal = 0;
    }

    private boolean tickCanFilling() {
        ItemStack canStack = inventory.getStackInSlot(0);
        if (!FoodRegistry.isEmptyBiomassContainer(canStack)) {
            canFillTime = 0;
            return false;
        }

        if (!isEnrichedBiomass(outputTank.getFluid()) || outputTank.getFluidAmount() < CAN_VOLUME || energyStored < CAN_FILL_ENERGY) {
            canFillTime = 0;
            return false;
        }

        ItemStack result = FoodRegistry.getFilledEnrichedContainer(canStack);
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
        outputTank.drain(CAN_VOLUME, true);
        energyStored -= CAN_FILL_ENERGY;
        markForSync();
        return true;
    }

    private void pushEnrichedOutput() {
        if (outputTank.getFluidAmount() <= 0 || world == null || pos == null) {
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

        Fluid outputFluid = getOutputFluidType();
        if (outputFluid == null) {
            return;
        }

        int amount = Math.min(AUTO_OUTPUT_RATE, outputTank.getFluidAmount());
        FluidStack offer = new FluidStack(outputFluid, amount);
        int accepted = target.fill(offer, false);
        if (accepted <= 0) {
            return;
        }

        FluidStack drained = rightOutput.drain(Math.min(amount, accepted), true);
        if (drained == null || drained.amount <= 0) {
            return;
        }

        target.fill(drained, true);
        markForSync();
    }


    private boolean isAcceptedInputFluid(@Nullable FluidStack stack) {
        return isDistilledBiomass(stack) || isDistilledFermentedSemen(stack) || isNecroSubstrate(stack);
    }

    private boolean isAcceptedOutputFluid(@Nullable FluidStack stack) {
        return isEnrichedBiomass(stack) || isIc2Biomass(stack) || isBioToxin(stack);
    }

    private boolean canDrainRequestedFluid(@Nullable FluidStack resource) {
        FluidStack stored = outputTank.getFluid();
        return resource != null && stored != null && resource.getFluid() == stored.getFluid();
    }

    private boolean isDistilledBiomass(@Nullable FluidStack stack) {
        return stack != null && stack.getFluid() == ModFluids.DISTILLED_BIOMASS;
    }

    private boolean isDistilledFermentedSemen(@Nullable FluidStack stack) {
        return stack != null && stack.getFluid() == ModFluids.DISTILLED_FERMENTED_SEMEN;
    }

    private boolean isNecroSubstrate(@Nullable FluidStack stack) {
        return stack != null && stack.getFluid() == ModFluids.NECRO_SUBSTRATE;
    }

    private boolean isBioToxin(@Nullable FluidStack stack) {
        return stack != null && stack.getFluid() == ModFluids.BIO_TOXIN;
    }

    private boolean isEnrichedBiomass(@Nullable FluidStack stack) {
        return stack != null && stack.getFluid() == ModFluids.ENRICHED_BIOMASS;
    }

    private boolean isIc2Biomass(@Nullable FluidStack stack) {
        if (stack == null) return false;

        Fluid fluid = stack.getFluid();
        if (fluid == null) return false;

        if (fluid == ModFluids.BIOMASS || fluid == ModFluids.DISTILLED_BIOMASS || fluid == ModFluids.ENRICHED_BIOMASS
                || fluid == ModFluids.FERMENTED_SEMEN || fluid == ModFluids.DISTILLED_FERMENTED_SEMEN) {
            return false;
        }

        String fluidName = safeLower(fluid.getName());
        String unlocalizedName = safeLower(fluid.getUnlocalizedName());

        if (!fluidName.contains("biomass") && !unlocalizedName.contains("biomass")) {
            return false;
        }

        if (isIc2OwnedFluid(fluid)) {
            return true;
        }

        Fluid registryBiomass = FluidRegistry.getFluid("biomass");
        return registryBiomass != null && fluid == registryBiomass;
    }

    private boolean isIc2OwnedFluid(@Nullable Fluid fluid) {
        if (fluid == null) return false;

        if (fluid.getBlock() != null && fluid.getBlock().getRegistryName() != null) {
            String namespace = safeLower(fluid.getBlock().getRegistryName().getNamespace());
            String path = safeLower(fluid.getBlock().getRegistryName().getPath());
            if ("ic2".equals(namespace) && path.contains("biomass")) {
                return true;
            }
        }

        if (fluid.getStill() != null) {
            String namespace = safeLower(fluid.getStill().getNamespace());
            String path = safeLower(fluid.getStill().getPath());
            if ("ic2".equals(namespace) && path.contains("biomass")) {
                return true;
            }
        }

        if (fluid.getFlowing() != null) {
            String namespace = safeLower(fluid.getFlowing().getNamespace());
            String path = safeLower(fluid.getFlowing().getPath());
            if ("ic2".equals(namespace) && path.contains("biomass")) {
                return true;
            }
        }

        String unlocalizedName = safeLower(fluid.getUnlocalizedName());
        return unlocalizedName.contains("ic2") && unlocalizedName.contains("biomass");
    }

    private String safeLower(@Nullable String value) {
        return value == null ? "" : value.toLowerCase(java.util.Locale.ROOT);
    }

    @Nullable
    private Fluid getIc2BiomassFluid() {
        Fluid direct = FluidRegistry.getFluid("biomass");
        if (isIc2Biomass(direct == null ? null : new FluidStack(direct, 1))) {
            return direct;
        }

        for (Fluid candidate : FluidRegistry.getRegisteredFluids().values()) {
            if (isIc2Biomass(candidate == null ? null : new FluidStack(candidate, 1))) {
                return candidate;
            }
        }

        return null;
    }

    @Nullable
    private Fluid getCurrentRecipeOutputFluid() {
        FluidStack input = inputTank.getFluid();
        if (isDistilledBiomass(input)) {
            return ModFluids.ENRICHED_BIOMASS;
        }
        if (isDistilledFermentedSemen(input)) {
            return getIc2BiomassFluid();
        }
        if (isNecroSubstrate(input)) {
            return ModFluids.BIO_TOXIN;
        }
        return null;
    }

    @Nullable
    private Fluid getOutputFluidType() {
        FluidStack output = outputTank.getFluid();
        return output == null ? null : output.getFluid();
    }

    public boolean hasDistilledFermentedInput() {
        return isDistilledFermentedSemen(inputTank.getFluid());
    }

    public boolean hasIc2BiomassOutput() {
        return isIc2Biomass(outputTank.getFluid());
    }

    public boolean hasDistilledBiomassInput() {
        return isDistilledBiomass(inputTank.getFluid());
    }

    public boolean hasNecroSubstrateInput() {
        return isNecroSubstrate(inputTank.getFluid());
    }

    public boolean hasBioToxinOutput() {
        return isBioToxin(outputTank.getFluid());
    }

    public EnumFacing getFacing() {
        if (world == null || pos == null) return EnumFacing.NORTH;
        IBlockState state = world.getBlockState(pos);
        return state.getBlock() == SemenEnrichmentRegistry.SEMEN_ENRICHMENT_CHAMBER
                ? state.getValue(BlockSemenEnrichmentChamber.FACING)
                : EnumFacing.NORTH;
    }

    public int getInputStored() {
        return inputTank.getFluidAmount();
    }

    public int getOutputStored() {
        return outputTank.getFluidAmount();
    }

    public int getMaxInput() {
        return inputTank.getCapacity();
    }

    public int getMaxOutput() {
        return outputTank.getCapacity();
    }

    public boolean isActiveNow() {
        return active;
    }

    public int getProcessProgressScaled(int pixels) {
        if (processTimeTotal <= 0 || processTime <= 0) return 0;
        return Math.min(pixels, processTime * pixels / processTimeTotal);
    }

    public int getCanFillProgressScaled(int pixels) {
        if (CAN_FILL_TIME_TICKS <= 0 || canFillTime <= 0) return 0;
        return Math.min(pixels, canFillTime * pixels / CAN_FILL_TIME_TICKS);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public void markForSync() {
        if (world == null || world.isRemote) return;
        markDirty();
        IBlockState state = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, state, state, 3);
    }

    private boolean canExposeEnergyTo(@Nullable EnumFacing facing) {
        return facing == null || facing == EnumFacing.DOWN || facing == getFacing().getOpposite();
    }

    private boolean canFillInputFrom(@Nullable EnumFacing facing) {
        return facing == null || facing == getFacing().rotateY();
    }

    private boolean canDrainOutputTo(@Nullable EnumFacing facing) {
        return facing == null || facing == getFacing().rotateYCCW();
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) return canExposeEnergyTo(facing);
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return canFillInputFrom(facing) || canDrainOutputTo(facing);
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
            if (canFillInputFrom(facing) && canDrainOutputTo(facing)) {
                return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(internalHandler);
            }
            if (canFillInputFrom(facing)) {
                return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(leftInput);
            }
            if (canDrainOutputTo(facing)) {
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
        compound.setBoolean("Active", active);
        compound.setTag("InputTank", inputTank.writeToNBT(new NBTTagCompound()));
        compound.setTag("OutputTank", outputTank.writeToNBT(new NBTTagCompound()));
        compound.setTag("Items", inventory.serializeNBT());
        compound.setInteger("ProcessTime", processTime);
        compound.setInteger("ProcessMovedMb", processMovedMb);
        compound.setInteger("ProcessEnergyUsed", processEnergyUsed);
        compound.setInteger("ProcessTargetMb", processTargetMb);
        compound.setInteger("ProcessTargetEnergy", processTargetEnergy);
        compound.setInteger("ProcessTimeTotal", processTimeTotal);
        compound.setInteger("CanFillTime", canFillTime);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        energyStored = compound.getInteger("Energy");
        active = compound.getBoolean("Active");
        if (compound.hasKey("InputTank")) inputTank.readFromNBT(compound.getCompoundTag("InputTank"));
        if (compound.hasKey("OutputTank")) outputTank.readFromNBT(compound.getCompoundTag("OutputTank"));
        inventory.deserializeNBT(compound.getCompoundTag("Items"));
        processTime = compound.getInteger("ProcessTime");
        processMovedMb = compound.getInteger("ProcessMovedMb");
        processEnergyUsed = compound.getInteger("ProcessEnergyUsed");
        processTargetMb = compound.getInteger("ProcessTargetMb");
        processTargetEnergy = compound.getInteger("ProcessTargetEnergy");
        processTimeTotal = compound.getInteger("ProcessTimeTotal");
        canFillTime = compound.getInteger("CanFillTime");
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
        int received = Math.min(MAX_ENERGY - energyStored, Math.max(0, maxReceive));
        if (!simulate && received > 0) {
            energyStored += received;
            markForSync();
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