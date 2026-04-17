package com.ogryzok.separator.tile;

import com.ogryzok.fluids.ModFluids;
import com.ogryzok.food.FoodRegistry;
import com.ogryzok.separator.SeparatorRegistry;
import com.ogryzok.separator.block.BlockSeparator;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import javax.annotation.Nullable;

public class TileSeparator extends TileEntity implements ITickable, IEnergyStorage, IAnimatable {
    private static final int MAX_ENERGY = 100000;
    private static final int ENRICHED_INPUT_MB = 200;
    private static final int BIO_TOXIN_INPUT_MB = 1000;
    private static final int IC2_BIOMASS_INPUT_MB = 1000;
    private static final int ENRICHED_OUTPUT_WATER_MB = 100;
    private static final int BIO_TOXIN_OUTPUT_WATER_MB = 700;
    private static final int IC2_BIOMASS_OUTPUT_WATER_MB = 500;
    private static final int PROCESS_TIME_TICKS = 160;
    private static final int BIO_TOXIN_PROCESS_TIME_TICKS = PROCESS_TIME_TICKS * 3;
    private static final int PROCESS_ENERGY = 1000;
    private static final int TANK_CAPACITY = 4000;
    private static final int AUTO_OUTPUT_RATE = 50;
    private static final int BUCKET_VOLUME = 1000;

    private final AnimationFactory factory = new AnimationFactory(this);
    private String lastAnimationKey = "";

    private int energyStored = 0;
    private boolean assembled = false;
    private boolean active = false;
    private int processTime = 0;
    private int processEnergyUsed = 0;

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
            return fluid != null && fluid.getFluid() == FluidRegistry.WATER;
        }
        @Override
        protected void onContentsChanged() {
            markForSync();
        }
    };

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            markForSync();
        }
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }
    };

    private final IFluidHandler leftInput = new IFluidHandler() {
        @Override public IFluidTankProperties[] getTankProperties() { return inputTank.getTankProperties(); }
        @Override public int fill(FluidStack resource, boolean doFill) { return inputTank.fill(resource, doFill); }
        @Override public FluidStack drain(FluidStack resource, boolean doDrain) { return null; }
        @Override public FluidStack drain(int maxDrain, boolean doDrain) { return null; }
    };

    private final IFluidHandler bottomOutput = new IFluidHandler() {
        @Override public IFluidTankProperties[] getTankProperties() { return outputTank.getTankProperties(); }
        @Override public int fill(FluidStack resource, boolean doFill) { return 0; }
        @Override public FluidStack drain(FluidStack resource, boolean doDrain) {
            if (resource == null || resource.getFluid() != FluidRegistry.WATER) return null;
            return outputTank.drain(resource.amount, doDrain);
        }
        @Override public FluidStack drain(int maxDrain, boolean doDrain) { return outputTank.drain(maxDrain, doDrain); }
    };

    private final IItemHandler rightOutputHandler = new IItemHandler() {
        @Override public int getSlots() { return 1; }
        @Override public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(0); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return inventory.extractItem(0, amount, simulate); }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(0); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return false; }
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
        @Override public int fill(FluidStack resource, boolean doFill) { return inputTank.fill(resource, doFill); }
        @Override public FluidStack drain(FluidStack resource, boolean doDrain) {
            if (resource == null || resource.getFluid() != FluidRegistry.WATER) return null;
            return outputTank.drain(resource.amount, doDrain);
        }
        @Override public FluidStack drain(int maxDrain, boolean doDrain) { return outputTank.drain(maxDrain, doDrain); }
    };

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        if (!assembled) {
            if (active) {
                active = false;
                markForSync();
            }
            return;
        }

        boolean worked = tickSeparation();
        pushWaterOutput();
        pushItemOutputRight();

        if (active != worked) {
            active = worked;
            markForSync();
        }
    }

    private boolean tickSeparation() {
        FluidStack input = inputTank.getFluid();
        if (!isAcceptedInputFluid(input)) {
            if (processTime <= 0) {
                processEnergyUsed = 0;
            }
            return false;
        }

        boolean isIc2Biomass = isIc2Biomass(input);
        boolean isBioToxin = isBioToxin(input);
        ItemStack resultStack = new ItemStack(isIc2Biomass
                ? FoodRegistry.EVAPORATED_BIOMASS
                : (isBioToxin ? FoodRegistry.TOXIC_BIOMASS : FoodRegistry.PROTEIN_BIOMASS));
        int requiredInput = isIc2Biomass ? IC2_BIOMASS_INPUT_MB : (isBioToxin ? BIO_TOXIN_INPUT_MB : ENRICHED_INPUT_MB);
        int outputWater = isIc2Biomass ? IC2_BIOMASS_OUTPUT_WATER_MB : (isBioToxin ? BIO_TOXIN_OUTPUT_WATER_MB : ENRICHED_OUTPUT_WATER_MB);
        int processTimeTarget = isBioToxin ? BIO_TOXIN_PROCESS_TIME_TICKS : PROCESS_TIME_TICKS;

        ItemStack outputStack = inventory.getStackInSlot(0);
        boolean canOutputItem = outputStack.isEmpty()
                || (ItemStack.areItemsEqual(outputStack, resultStack) && outputStack.getCount() < outputStack.getMaxStackSize());
        boolean canProcess = inputTank.getFluidAmount() >= requiredInput
                && outputTank.getCapacity() - outputTank.getFluidAmount() >= outputWater
                && energyStored >= (PROCESS_ENERGY - processEnergyUsed)
                && canOutputItem;

        if (!canProcess) {
            if (processTime <= 0) {
                processEnergyUsed = 0;
            }
            return false;
        }

        processTime++;

        int targetEnergy = processTime * PROCESS_ENERGY / processTimeTarget;
        int deltaEnergy = targetEnergy - processEnergyUsed;
        if (deltaEnergy > 0) {
            energyStored -= deltaEnergy;
            if (energyStored < 0) energyStored = 0;
            processEnergyUsed += deltaEnergy;
            markForSync();
        }

        if (processTime >= processTimeTarget) {
            processTime = 0;
            processEnergyUsed = 0;
            inputTank.drain(requiredInput, true);
            if (outputWater > 0) {
                outputTank.fill(new FluidStack(FluidRegistry.WATER, outputWater), true);
            }
            inventory.insertItem(0, resultStack.copy(), false);
            markForSync();
        }

        return true;
    }

    private boolean isAcceptedInputFluid(@Nullable FluidStack stack) {
        return isIc2Biomass(stack) || isEnrichedBiomass(stack) || isBioToxin(stack);
    }

    private boolean isEnrichedBiomass(@Nullable FluidStack stack) {
        return stack != null && stack.getFluid() == ModFluids.ENRICHED_BIOMASS;
    }

    private boolean isBioToxin(@Nullable FluidStack stack) {
        return stack != null && stack.getFluid() == ModFluids.BIO_TOXIN;
    }

    private boolean isIc2Biomass(@Nullable FluidStack stack) {
        if (stack == null) return false;

        Fluid fluid = stack.getFluid();
        if (fluid == null) return false;

        if (fluid == ModFluids.BIOMASS || fluid == ModFluids.DISTILLED_BIOMASS || fluid == ModFluids.ENRICHED_BIOMASS) {
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

    private boolean isIc2OwnedFluid(Fluid fluid) {
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

    public boolean hasIc2BiomassInput() {
        return isIc2Biomass(inputTank.getFluid());
    }

    public boolean hasEnrichedBiomassInput() {
        return isEnrichedBiomass(inputTank.getFluid());
    }

    public boolean hasBioToxinInput() {
        return isBioToxin(inputTank.getFluid());
    }

    private void pushWaterOutput() {
        if (outputTank.getFluidAmount() <= 0 || world == null || pos == null) return;

        EnumFacing out = EnumFacing.DOWN;
        TileEntity targetTile = world.getTileEntity(pos.offset(out));
        if (targetTile == null || !targetTile.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, out.getOpposite())) return;

        IFluidHandler target = targetTile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, out.getOpposite());
        if (target == null) return;

        int amount = Math.min(AUTO_OUTPUT_RATE, outputTank.getFluidAmount());
        FluidStack offer = new FluidStack(FluidRegistry.WATER, amount);
        int accepted = target.fill(offer, false);
        if (accepted <= 0) return;

        FluidStack drained = bottomOutput.drain(Math.min(amount, accepted), true);
        if (drained == null || drained.amount <= 0) return;

        target.fill(drained, true);
        markForSync();
    }


    private void pushItemOutputRight() {
        if (world == null || pos == null) return;
        ItemStack output = inventory.getStackInSlot(0);
        if (output.isEmpty()) return;

        EnumFacing out = getFacing().rotateYCCW();
        TileEntity targetTile = world.getTileEntity(pos.offset(out));
        if (targetTile == null || !targetTile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, out.getOpposite())) return;

        IItemHandler target = targetTile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, out.getOpposite());
        if (target == null) return;

        ItemStack stackToMove = output.copy();
        int original = stackToMove.getCount();
        for (int i = 0; i < target.getSlots() && !stackToMove.isEmpty(); i++) {
            stackToMove = target.insertItem(i, stackToMove, false);
        }

        int moved = original - stackToMove.getCount();
        if (moved > 0) {
            inventory.extractItem(0, moved, false);
            markForSync();
        }
    }

    public boolean tryFillBucket(EntityPlayer player, EnumHand hand, ItemStack held) {
        if (world == null || world.isRemote || player == null || held.isEmpty() || held.getItem() != Items.BUCKET) return false;
        if (outputTank.getFluidAmount() < BUCKET_VOLUME) return false;

        ItemStack waterBucket = new ItemStack(Items.WATER_BUCKET);
        outputTank.drain(BUCKET_VOLUME, true);

        if (!player.capabilities.isCreativeMode) {
            if (held.getCount() == 1) {
                player.setHeldItem(hand, waterBucket);
            } else {
                held.shrink(1);
                if (!player.inventory.addItemStackToInventory(waterBucket)) {
                    EntityItem entityItem = new EntityItem(world, player.posX, player.posY, player.posZ, waterBucket);
                    world.spawnEntity(entityItem);
                }
            }
        }

        markForSync();
        return true;
    }

    public EnumFacing getFacing() {
        if (world == null || pos == null) return EnumFacing.NORTH;
        IBlockState state = world.getBlockState(pos);
        return state.getBlock() == SeparatorRegistry.SEPARATOR ? state.getValue(BlockSeparator.FACING) : EnumFacing.NORTH;
    }

    public boolean isAssembled() {
        return assembled;
    }

    public boolean assemble() {
        if (assembled) return false;
        assembled = true;
        processTime = 0;
        processEnergyUsed = 0;
        if (world != null && pos != null) {
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock() == SeparatorRegistry.SEPARATOR) {
                world.setBlockState(pos, state.withProperty(BlockSeparator.ASSEMBLED, Boolean.TRUE), 3);
            }
        }
        markForSync();
        return true;
    }

    public int getInputStored() { return inputTank.getFluidAmount(); }
    public int getOutputStored() { return outputTank.getFluidAmount(); }
    public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
    public int getMaxInput() { return inputTank.getCapacity(); }
    public int getMaxOutput() { return outputTank.getCapacity(); }
    public boolean isActiveNow() { return active; }
    public int getProcessProgressScaled(int pixels) {
        int processTimeTarget = getCurrentProcessTimeTarget();
        return processTimeTarget <= 0 || processTime <= 0 ? 0 : Math.min(pixels, processTime * pixels / processTimeTarget);
    }

    private int getCurrentProcessTimeTarget() {
        return hasBioToxinInput() ? BIO_TOXIN_PROCESS_TIME_TICKS : PROCESS_TIME_TICKS;
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
        return facing == null || facing == EnumFacing.DOWN;
    }

    private boolean canExtractItemsTo(@Nullable EnumFacing facing) {
        return facing == null || facing == getFacing().rotateYCCW();
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (!assembled) return super.hasCapability(capability, facing);
        if (capability == CapabilityEnergy.ENERGY) return canExposeEnergyTo(facing);
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return canFillInputFrom(facing) || canDrainOutputTo(facing);
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return facing == null || canExtractItemsTo(facing);
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (!assembled) {
            return super.getCapability(capability, facing);
        }
        if (capability == CapabilityEnergy.ENERGY) {
            return canExposeEnergyTo(facing) ? CapabilityEnergy.ENERGY.cast(this) : null;
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            if (facing == null) return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(internalHandler);
            if (canFillInputFrom(facing) && canDrainOutputTo(facing)) return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(internalHandler);
            if (canFillInputFrom(facing)) return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(leftInput);
            if (canDrainOutputTo(facing)) return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(bottomOutput);
            return null;
        }
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if (facing == null) return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
            if (canExtractItemsTo(facing)) return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(rightOutputHandler);
            return null;
        }
        return super.getCapability(capability, facing);
    }


    @Override
    public boolean shouldRefresh(net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos, net.minecraft.block.state.IBlockState oldState, net.minecraft.block.state.IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("Energy", energyStored);
        compound.setBoolean("Assembled", assembled);
        compound.setBoolean("Active", active);
        compound.setTag("InputTank", inputTank.writeToNBT(new NBTTagCompound()));
        compound.setTag("OutputTank", outputTank.writeToNBT(new NBTTagCompound()));
        compound.setTag("Items", inventory.serializeNBT());
        compound.setInteger("ProcessTime", processTime);
        compound.setInteger("ProcessEnergyUsed", processEnergyUsed);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        energyStored = compound.getInteger("Energy");
        assembled = compound.getBoolean("Assembled");
        active = compound.getBoolean("Active");
        if (compound.hasKey("InputTank")) inputTank.readFromNBT(compound.getCompoundTag("InputTank"));
        if (compound.hasKey("OutputTank")) outputTank.readFromNBT(compound.getCompoundTag("OutputTank"));
        inventory.deserializeNBT(compound.getCompoundTag("Items"));
        processTime = compound.getInteger("ProcessTime");
        processEnergyUsed = compound.getInteger("ProcessEnergyUsed");
    }

    @Override public NBTTagCompound getUpdateTag() { return writeToNBT(new NBTTagCompound()); }
    @Nullable @Override public SPacketUpdateTileEntity getUpdatePacket() { return new SPacketUpdateTileEntity(this.pos, 0, getUpdateTag()); }
    @Override public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) { readFromNBT(pkt.getNbtCompound()); }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = Math.min(MAX_ENERGY - energyStored, Math.max(0, maxReceive));
        if (!simulate && received > 0) {
            energyStored += received;
            markForSync();
        }
        return received;
    }

    @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
    @Override public int getEnergyStored() { return energyStored; }
    @Override public int getMaxEnergyStored() { return MAX_ENERGY; }
    @Override public boolean canExtract() { return false; }
    @Override public boolean canReceive() { return true; }

    private <E extends IAnimatable> PlayState animationPredicate(AnimationEvent<E> event) {
        AnimationController<?> controller = event.getController();
        String animationKey = active ? "separator_work" : "idle";

        if (!animationKey.equals(lastAnimationKey)) {
            controller.markNeedsReload();
            lastAnimationKey = animationKey;
        }

        if (!active) {
            controller.setAnimationSpeed(0.0D);
            return PlayState.STOP;
        }

        controller.setAnimation(new AnimationBuilder().addAnimation("separator_work", true));
        controller.setAnimationSpeed(1.0D);
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimationData animationData) {
        animationData.addAnimationController(new AnimationController<TileSeparator>(this, "controller", 0.0f, this::animationPredicate));
    }

    @Override public AnimationFactory getFactory() { return factory; }
}
