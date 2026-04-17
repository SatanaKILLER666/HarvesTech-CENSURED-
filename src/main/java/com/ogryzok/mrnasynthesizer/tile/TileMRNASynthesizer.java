package com.ogryzok.mrnasynthesizer.tile;

import com.ogryzok.disease.DiseaseRegistry;
import com.ogryzok.fluids.ModFluids;
import com.ogryzok.food.FoodRegistry;
import com.ogryzok.mrnasynthesizer.MRNASynthesizerRegistry;
import com.ogryzok.mrnasynthesizer.block.BlockMRNASynthesizer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
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

public class TileMRNASynthesizer extends TileEntity implements ITickable, IEnergyStorage, IAnimatable {
    public static final int SLOT_FILLER_TOP = 0;
    public static final int SLOT_FILLER_BOTTOM = 1;
    public static final int SLOT_CATALYST_TOP = 2;
    public static final int SLOT_CATALYST_BOTTOM = 3;
    public static final int SLOT_SYRINGE_IN_0 = 4;
    public static final int SLOT_SYRINGE_IN_1 = 5;
    public static final int SLOT_SYRINGE_IN_2 = 6;
    public static final int SLOT_SYRINGE_IN_3 = 7;
    public static final int SLOT_VACCINE_OUT_0 = 8;
    public static final int SLOT_VACCINE_OUT_1 = 9;
    public static final int SLOT_VACCINE_OUT_2 = 10;
    public static final int SLOT_VACCINE_OUT_3 = 11;

    private static final int MAX_ENERGY = 20000;
    private static final int TANK_CAPACITY = 1000;
    private static final int AIDS_PROCESS_ENERGY = 7000;
    private static final int STEROID_PROCESS_ENERGY = 7000;
    private static final int START_TICKS = 100;
    private static final int WORK_TICKS = 600;
    private static final int STOP_TICKS = 110;
    private static final int ACTIVE_TICKS = START_TICKS + WORK_TICKS;
    private static final int FILL_PER_SYRINGE = 250;
    private static final int AUTO_WATER_PULL = 10;
    private static final int SYRINGE_FILL_TICKS = 40;
    private static final int MAX_RECEIVE_PER_TICK = 64;

    private static final int PRODUCT_COLOR_AIDS = 0xFFF05A8A;
    private static final int PRODUCT_COLOR_STEROID = 0xFFFFEBC9;

    private final AnimationFactory factory = new AnimationFactory(this);
    private String lastAnimationKey = "";

    private int energyStored = 0;
    private int processTick = 0;
    private int processEnergyUsed = 0;
    private int stopTick = 0;
    private int startCycle = 0;
    private int stopCycle = 0;
    private int waterPullCooldown = 0;
    private RecipeMode activeRecipe = RecipeMode.NONE;
    private final int[] syringeProgress = new int[4];
    private final int[] syringeFluidBuffered = new int[4];

    private final FluidTank tank = new FluidTank(TANK_CAPACITY) {
        @Override
        public boolean canFillFluidType(FluidStack fluid) {
            return canAcceptFluid(fluid);
        }

        @Override
        protected void onContentsChanged() {
            markForSync();
        }
    };

    private final ItemStackHandler inventory = new ItemStackHandler(12) {
        @Override
        protected void onContentsChanged(int slot) {
            markForSync();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_FILLER_TOP || slot == SLOT_FILLER_BOTTOM) {
                return !stack.isEmpty() && stack.getItem() == FoodRegistry.EVAPORATED_BIOMASS;
            }
            if (slot == SLOT_CATALYST_TOP || slot == SLOT_CATALYST_BOTTOM) {
                return !stack.isEmpty() && isValidAntigen(stack);
            }
            if (slot >= SLOT_SYRINGE_IN_0 && slot <= SLOT_SYRINGE_IN_3) {
                return !stack.isEmpty() && stack.getItem() == DiseaseRegistry.SYRINGE;
            }
            return false;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot >= SLOT_SYRINGE_IN_0 && slot <= SLOT_SYRINGE_IN_3 ? 1 : (slot >= SLOT_VACCINE_OUT_0 ? 16 : 64);
        }
    };

    private final IFluidHandler bottomFluidHandler = new IFluidHandler() {
        @Override public IFluidTankProperties[] getTankProperties() { return tank.getTankProperties(); }
        @Override public int fill(FluidStack resource, boolean doFill) { return tank.fill(resource, doFill); }
        @Override public FluidStack drain(FluidStack resource, boolean doDrain) { return null; }
        @Override public FluidStack drain(int maxDrain, boolean doDrain) { return null; }
    };

    @Override
    public void update() {
        if (world == null) return;
        if (world.isRemote) return;

        if (waterPullCooldown-- <= 0) {
            waterPullCooldown = AUTO_WATER_PULL;
            pullWaterFromBelow();
        }

        tickCraft();
        tickSyringeFilling();
    }

    private void tickCraft() {
        if (processTick > 0) {
            if (!canContinueCraft()) {
                resetCraft();
                return;
            }

            processTick++;
            consumeEnergyForCurrentTick();

            if (processTick >= ACTIVE_TICKS) {
                tank.setFluid(new FluidStack(getOutputFluidForRecipe(activeRecipe), getOutputAmountForRecipe(activeRecipe)));
                stopTick = STOP_TICKS;
                stopCycle++;
                processTick = 0;
                processEnergyUsed = 0;
                markForSync();
            }
            return;
        }

        if (stopTick > 0) {
            stopTick--;
            if (stopTick == 0) {
                markForSync();
            }
        }

        if (canStartCraft()) {
            activeRecipe = detectRecipe();
            consumeCraftIngredients();
            processTick = 1;
            processEnergyUsed = 0;
            startCycle++;
            markForSync();
        }
    }

    private void consumeEnergyForCurrentTick() {
        int recipeEnergy = getProcessEnergyForRecipe(activeRecipe);
        int targetEnergy = processTick <= ACTIVE_TICKS ? processTick * recipeEnergy / ACTIVE_TICKS : recipeEnergy;
        int delta = targetEnergy - processEnergyUsed;
        if (delta > 0) {
            energyStored -= delta;
            if (energyStored < 0) energyStored = 0;
            processEnergyUsed += delta;
            markForSync();
        }
    }

    private boolean canStartCraft() {
        RecipeMode recipe = detectRecipe();
        return hasFiller() && recipe != RecipeMode.NONE && hasFullWaterTank() && !hasProductFluid() && !isCrafting() && energyStored >= getProcessEnergyForRecipe(recipe);
    }

    private boolean canContinueCraft() {
        return activeRecipe != RecipeMode.NONE && hasFullWaterTank() && !hasProductFluid() && energyStored > 0 && processEnergyUsed < getProcessEnergyForRecipe(activeRecipe);
    }

    private void resetCraft() {
        processTick = 0;
        processEnergyUsed = 0;
        activeRecipe = RecipeMode.NONE;
        markForSync();
    }

    private void consumeCraftIngredients() {
        inventory.extractItem(findFirstFilledSlot(SLOT_FILLER_TOP, SLOT_FILLER_BOTTOM), 1, false);
        if (activeRecipe == RecipeMode.STEROID) {
            inventory.extractItem(SLOT_CATALYST_TOP, 5, false);
            inventory.extractItem(SLOT_CATALYST_BOTTOM, 5, false);
        } else {
            inventory.extractItem(findFirstFilledSlot(SLOT_CATALYST_TOP, SLOT_CATALYST_BOTTOM), 1, false);
        }
    }

    private boolean hasFiller() {
        return !inventory.getStackInSlot(SLOT_FILLER_TOP).isEmpty() || !inventory.getStackInSlot(SLOT_FILLER_BOTTOM).isEmpty();
    }

    private boolean hasCatalyst() {
        return detectRecipe() != RecipeMode.NONE;
    }

    private boolean hasFullWaterTank() {
        FluidStack stack = tank.getFluid();
        return stack != null && stack.getFluid() == FluidRegistry.WATER && stack.amount >= TANK_CAPACITY;
    }

    public boolean hasVaccine() {
        FluidStack stack = tank.getFluid();
        return stack != null && stack.getFluid() == ModFluids.AIDS_VACCINE && stack.amount > 0;
    }

    public boolean hasProductFluid() {
        FluidStack stack = tank.getFluid();
        return stack != null && (stack.getFluid() == ModFluids.AIDS_VACCINE || stack.getFluid() == ModFluids.MALE_POWER_STEROID) && stack.amount > 0;
    }

    private int findFirstFilledSlot(int a, int b) {
        return inventory.getStackInSlot(a).isEmpty() ? b : a;
    }

    private void pullWaterFromBelow() {
        if (world == null || pos == null || hasProductFluid()) return;
        TileEntity below = world.getTileEntity(pos.down());
        if (below == null || !below.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.UP)) return;
        IFluidHandler source = below.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.UP);
        if (source == null) return;

        int missing = TANK_CAPACITY - tank.getFluidAmount();
        if (missing <= 0) return;

        FluidStack simulated = source.drain(new FluidStack(FluidRegistry.WATER, missing), false);
        if (simulated == null || simulated.amount <= 0 || simulated.getFluid() != FluidRegistry.WATER) return;

        int accepted = tank.fill(simulated, false);
        if (accepted <= 0) return;
        FluidStack drained = source.drain(new FluidStack(FluidRegistry.WATER, accepted), true);
        if (drained != null && drained.amount > 0) {
            tank.fill(drained, true);
        }
    }

    private void tickSyringeFilling() {
        boolean changed = false;

        for (int pair = 0; pair < 4; pair++) {
            if (!canFillSyringePair(pair)) {
                if (syringeProgress[pair] != 0 || syringeFluidBuffered[pair] != 0) {
                    refundBufferedVaccine(pair);
                    syringeProgress[pair] = 0;
                    changed = true;
                }
                continue;
            }

            syringeProgress[pair]++;
            int targetBuffered = Math.min(FILL_PER_SYRINGE, syringeProgress[pair] * FILL_PER_SYRINGE / SYRINGE_FILL_TICKS);
            int toDrain = targetBuffered - syringeFluidBuffered[pair];
            if (toDrain > 0) {
                FluidStack drained = tank.drain(toDrain, true);
                int drainedAmount = drained == null ? 0 : drained.amount;
                syringeFluidBuffered[pair] += drainedAmount;
                if (drainedAmount < toDrain) {
                    syringeProgress[pair] = 0;
                    refundBufferedVaccine(pair);
                    changed = true;
                    continue;
                }
            }

            changed = true;
            if (syringeProgress[pair] >= SYRINGE_FILL_TICKS) {
                completeSyringeFill(pair);
                syringeProgress[pair] = 0;
                syringeFluidBuffered[pair] = 0;
            }
        }

        if (changed) {
            markForSync();
        }
    }

    private boolean canFillSyringePair(int pair) {
        if ((!hasProductFluid() && syringeFluidBuffered[pair] <= 0) || tank.getFluidAmount() + syringeFluidBuffered[pair] < FILL_PER_SYRINGE) return false;

        int inSlot = SLOT_SYRINGE_IN_0 + pair;
        int outSlot = SLOT_VACCINE_OUT_0 + pair;

        ItemStack syringe = inventory.getStackInSlot(inSlot);
        if (syringe.isEmpty() || syringe.getItem() != DiseaseRegistry.SYRINGE) return false;

        ItemStack output = inventory.getStackInSlot(outSlot);
        ItemStack filled = getFilledResultForCurrentTank();
        if (!output.isEmpty() && !ItemHandlerHelper.canItemStacksStack(output, filled)) return false;
        return output.isEmpty() || output.getCount() < output.getMaxStackSize();
    }

    private void completeSyringeFill(int pair) {
        int inSlot = SLOT_SYRINGE_IN_0 + pair;
        int outSlot = SLOT_VACCINE_OUT_0 + pair;

        if (!canFillSyringePair(pair)) return;
        if (syringeFluidBuffered[pair] < FILL_PER_SYRINGE) return;

        inventory.extractItem(inSlot, 1, false);
        inventory.insertItem(outSlot, getFilledResultForCurrentTank(), false);
    }

    private void refundBufferedVaccine(int pair) {
        int buffered = syringeFluidBuffered[pair];
        if (buffered <= 0) return;

        FluidStack current = tank.getFluid();
        if (current == null || current.amount <= 0) {
            if (activeRecipe != RecipeMode.NONE) {
                tank.setFluid(new FluidStack(getOutputFluidForRecipe(activeRecipe), Math.min(TANK_CAPACITY, buffered)));
            }
        } else if (current.getFluid() == ModFluids.AIDS_VACCINE || current.getFluid() == ModFluids.MALE_POWER_STEROID) {
            current.amount = Math.min(TANK_CAPACITY, current.amount + buffered);
            tank.setFluid(current);
        }

        syringeFluidBuffered[pair] = 0;
    }

    private boolean canAcceptFluid(@Nullable FluidStack stack) {
        if (stack == null || stack.getFluid() != FluidRegistry.WATER) return false;
        FluidStack current = tank.getFluid();
        return current == null || current.amount <= 0 || current.getFluid() == FluidRegistry.WATER;
    }

    public EnumFacing getFacing() {
        if (world == null || pos == null) return EnumFacing.NORTH;
        IBlockState state = world.getBlockState(pos);
        return state.getBlock() == MRNASynthesizerRegistry.MRNA_SYNTHESIZER ? state.getValue(BlockMRNASynthesizer.FACING) : EnumFacing.NORTH;
    }

    private boolean canExposeEnergyTo(@Nullable EnumFacing side) {
        if (side == null) return true;
        return side == getFacing().getOpposite();
    }

    public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
    public int getTankAmount() { return tank.getFluidAmount(); }
    public int getTankCapacity() { return tank.getCapacity(); }
    public boolean hasWater() { FluidStack stack = tank.getFluid(); return stack != null && stack.getFluid() == FluidRegistry.WATER && stack.amount > 0; }
    public boolean isCrafting() { return processTick > 0; }
    public boolean isStarting() { return processTick > 0 && processTick <= START_TICKS; }
    public boolean isWorking() { return processTick > START_TICKS; }
    public boolean isStopping() { return stopTick > 0; }
    public int getCraftProgressScaled(int pixels) { return ACTIVE_TICKS <= 0 || processTick <= 0 ? 0 : Math.min(pixels, processTick * pixels / ACTIVE_TICKS); }
    public int getSyringeProgressScaled(int pair, int pixels) {
        if (pair < 0 || pair >= syringeProgress.length || syringeProgress[pair] <= 0) return 0;
        return Math.min(pixels, syringeProgress[pair] * pixels / SYRINGE_FILL_TICKS);
    }

    public boolean isValidAntigen(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() == FoodRegistry.TOXIC_BIOMASS || stack.getItem() == FoodRegistry.PROTEIN_BIOMASS);
    }

    public String getDisplayedFluidKey() {
        FluidStack stack = tank.getFluid();
        if (stack == null || stack.amount <= 0) return "gui.harvestech.common.empty";
        if (stack.getFluid() == ModFluids.MALE_POWER_STEROID) return "fluid.harvestech.male_power_steroid_fluid";
        if (stack.getFluid() == ModFluids.AIDS_VACCINE) return "fluid.harvestech.aids_vaccine_fluid";
        return "gui.harvestech.common.water";
    }

    public int getProductFluidColor() {
        FluidStack stack = tank.getFluid();
        if (stack == null || stack.amount <= 0 || stack.getFluid() == FluidRegistry.WATER) return PRODUCT_COLOR_AIDS;
        return stack.getFluid() == ModFluids.MALE_POWER_STEROID ? PRODUCT_COLOR_STEROID : PRODUCT_COLOR_AIDS;
    }

    public int getCraftArrowColor() {
        RecipeMode recipe = activeRecipe != RecipeMode.NONE ? activeRecipe : detectRecipe();
        return recipe == RecipeMode.STEROID ? PRODUCT_COLOR_STEROID : PRODUCT_COLOR_AIDS;
    }

    public int getFillArrowColor() {
        FluidStack stack = tank.getFluid();
        if (stack != null && stack.amount > 0 && stack.getFluid() == ModFluids.MALE_POWER_STEROID) {
            return PRODUCT_COLOR_STEROID;
        }
        return PRODUCT_COLOR_AIDS;
    }

    private RecipeMode detectRecipe() {
        ItemStack top = inventory.getStackInSlot(SLOT_CATALYST_TOP);
        ItemStack bottom = inventory.getStackInSlot(SLOT_CATALYST_BOTTOM);
        if (top.getItem() == FoodRegistry.PROTEIN_BIOMASS && top.getCount() >= 5
                && bottom.getItem() == FoodRegistry.PROTEIN_BIOMASS && bottom.getCount() >= 5) {
            return RecipeMode.STEROID;
        }
        if ((!top.isEmpty() && top.getItem() == FoodRegistry.TOXIC_BIOMASS)
                || (!bottom.isEmpty() && bottom.getItem() == FoodRegistry.TOXIC_BIOMASS)) {
            return RecipeMode.AIDS_VACCINE;
        }
        return RecipeMode.NONE;
    }

    private int getProcessEnergyForRecipe(RecipeMode recipe) {
        return recipe == RecipeMode.STEROID ? STEROID_PROCESS_ENERGY : (recipe == RecipeMode.AIDS_VACCINE ? AIDS_PROCESS_ENERGY : 0);
    }

    private net.minecraftforge.fluids.Fluid getOutputFluidForRecipe(RecipeMode recipe) {
        return recipe == RecipeMode.STEROID ? ModFluids.MALE_POWER_STEROID : ModFluids.AIDS_VACCINE;
    }

    private int getOutputAmountForRecipe(RecipeMode recipe) {
        return recipe == RecipeMode.STEROID ? 500 : TANK_CAPACITY;
    }

    private ItemStack getFilledResultForCurrentTank() {
        FluidStack current = tank.getFluid();
        if ((current != null && current.getFluid() == ModFluids.MALE_POWER_STEROID) || activeRecipe == RecipeMode.STEROID) {
            return new ItemStack(DiseaseRegistry.MALE_POWER_STEROID);
        }
        return new ItemStack(DiseaseRegistry.AIDS_VACCINE);
    }

    public void markForSync() {
        if (world == null || world.isRemote) return;
        markDirty();
        IBlockState state = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, state, state, 3);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) return canExposeEnergyTo(facing);
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return facing == null || facing == EnumFacing.DOWN;
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return facing == null;
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return canExposeEnergyTo(facing) ? CapabilityEnergy.ENERGY.cast(this) : null;
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return (facing == null || facing == EnumFacing.DOWN) ? CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(bottomFluidHandler) : null;
        }
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return facing == null ? CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory) : null;
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("Energy", energyStored);
        compound.setTag("Items", inventory.serializeNBT());
        compound.setTag("Tank", tank.writeToNBT(new NBTTagCompound()));
        compound.setInteger("ProcessTick", processTick);
        compound.setInteger("ProcessEnergyUsed", processEnergyUsed);
        compound.setInteger("StopTick", stopTick);
        compound.setInteger("StartCycle", startCycle);
        compound.setInteger("StopCycle", stopCycle);
        compound.setString("ActiveRecipe", activeRecipe.name());
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < syringeProgress.length; i++) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("Idx", i);
            tag.setInteger("Value", syringeProgress[i]);
            tag.setInteger("FluidBuffered", syringeFluidBuffered[i]);
            list.appendTag(tag);
        }
        compound.setTag("SyringeProgress", list);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        energyStored = compound.getInteger("Energy");
        inventory.deserializeNBT(compound.getCompoundTag("Items"));
        tank.readFromNBT(compound.getCompoundTag("Tank"));
        processTick = compound.getInteger("ProcessTick");
        processEnergyUsed = compound.getInteger("ProcessEnergyUsed");
        stopTick = compound.getInteger("StopTick");
        startCycle = compound.getInteger("StartCycle");
        stopCycle = compound.getInteger("StopCycle");
        try {
            activeRecipe = RecipeMode.valueOf(compound.getString("ActiveRecipe"));
        } catch (IllegalArgumentException ignored) {
            activeRecipe = RecipeMode.NONE;
        }
        for (int i = 0; i < syringeProgress.length; i++) {
            syringeProgress[i] = 0;
            syringeFluidBuffered[i] = 0;
        }
        NBTTagList list = compound.getTagList("SyringeProgress", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            int idx = tag.getInteger("Idx");
            if (idx >= 0 && idx < syringeProgress.length) {
                syringeProgress[idx] = tag.getInteger("Value");
                syringeFluidBuffered[idx] = tag.getInteger("FluidBuffered");
            }
        }
    }

    @Override public NBTTagCompound getUpdateTag() { return writeToNBT(new NBTTagCompound()); }
    @Nullable @Override public SPacketUpdateTileEntity getUpdatePacket() { return new SPacketUpdateTileEntity(this.pos, 0, getUpdateTag()); }
    @Override public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) { readFromNBT(pkt.getNbtCompound()); }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = Math.min(MAX_ENERGY - energyStored, Math.min(MAX_RECEIVE_PER_TICK, Math.max(0, maxReceive)));
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
        String animationKey;

        if (isStarting()) {
            animationKey = "start#" + startCycle;
            if (!animationKey.equals(lastAnimationKey)) {
                controller.markNeedsReload();
                lastAnimationKey = animationKey;
            }
            controller.setAnimation(new AnimationBuilder().addAnimation("vaccine_gen_start", false));
            return PlayState.CONTINUE;
        }

        if (isWorking()) {
            animationKey = "work#" + startCycle;
            if (!animationKey.equals(lastAnimationKey)) {
                controller.markNeedsReload();
                lastAnimationKey = animationKey;
            }
            controller.setAnimation(new AnimationBuilder().addAnimation("vaccine_gen_work", true));
            return PlayState.CONTINUE;
        }

        if (isStopping()) {
            animationKey = "stop#" + stopCycle;
            if (!animationKey.equals(lastAnimationKey)) {
                controller.markNeedsReload();
                lastAnimationKey = animationKey;
            }
            controller.setAnimation(new AnimationBuilder().addAnimation("vaccine_gen_stop", false));
            return PlayState.CONTINUE;
        }

        lastAnimationKey = "idle";
        return PlayState.STOP;
    }

    @Override
    public void registerControllers(AnimationData animationData) {
        animationData.addAnimationController(new AnimationController<TileMRNASynthesizer>(this, "controller", 0.0f, this::animationPredicate));
    }

    @Override public AnimationFactory getFactory() { return factory; }

    private enum RecipeMode {
        NONE,
        AIDS_VACCINE,
        STEROID
    }
}

