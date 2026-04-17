package com.ogryzok.blender.tile;

import com.ogryzok.blender.BlenderRegistry;
import com.ogryzok.blender.block.BlockBlender;
import com.ogryzok.blender.sound.BlenderSoundRegistry;
import com.ogryzok.fluids.ModFluids;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import javax.annotation.Nullable;

public class TileBlender extends TileEntity implements ITickable, IEnergyStorage, IAnimatable {
    public enum Stage {
        IDLE,
        STARTING,
        WORKING,
        STOPPING
    }

    private static final int MAX_ENERGY = 100000;
    private static final int PROCESS_ENERGY = 5000;
    private static final int PROCESS_INPUT_COUNT = 16;
    private static final int PROCESS_OUTPUT_MB = 500;
    private static final int TANK_CAPACITY = 4000;
    private static final int START_TICKS = 60;
    private static final int WORK_TICKS = 440;
    private static final int STOP_TICKS = 50;
    private static final int PROGRESS_DELAY_TICKS = 0;
    private static final ResourceLocation ASSIMILATED_FLESH_ID = new ResourceLocation("srparasites", "assimilated_flesh");

    private final AnimationFactory factory = new AnimationFactory(this);
    private String lastAnimationKey = "idle";

    private int energyStored = 0;
    private Stage stage = Stage.IDLE;
    private int stageTicks = 0;
    private int processEnergyUsed = 0;
    private int animationCycle = 0;

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            markForSync();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && isValidInput(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }
    };

    private final FluidTank outputTank = new FluidTank(TANK_CAPACITY) {
        @Override
        public boolean canFillFluidType(FluidStack fluid) {
            return fluid != null && fluid.getFluid() == ModFluids.TOXIC_FLESH;
        }

        @Override
        protected void onContentsChanged() {
            markForSync();
        }
    };

    private final IFluidHandler bottomOutputHandler = new IFluidHandler() {
        @Override public IFluidTankProperties[] getTankProperties() { return outputTank.getTankProperties(); }
        @Override public int fill(FluidStack resource, boolean doFill) { return 0; }
        @Override public FluidStack drain(FluidStack resource, boolean doDrain) {
            if (resource == null || resource.getFluid() != ModFluids.TOXIC_FLESH) return null;
            return outputTank.drain(resource.amount, doDrain);
        }
        @Override public FluidStack drain(int maxDrain, boolean doDrain) { return outputTank.drain(maxDrain, doDrain); }
    };

    private final IFluidHandler internalFluidHandler = new IFluidHandler() {
        @Override public IFluidTankProperties[] getTankProperties() { return outputTank.getTankProperties(); }
        @Override public int fill(FluidStack resource, boolean doFill) { return 0; }
        @Override public FluidStack drain(FluidStack resource, boolean doDrain) { return bottomOutputHandler.drain(resource, doDrain); }
        @Override public FluidStack drain(int maxDrain, boolean doDrain) { return bottomOutputHandler.drain(maxDrain, doDrain); }
    };

    @Override
    public void update() {
        if (world == null) return;

        if (world.isRemote) {
            spawnProcessParticles();
            return;
        }

        tickMachine();
        pushFluidDown();
    }


    private void spawnProcessParticles() {
        if (stage != Stage.STARTING && stage != Stage.WORKING) return;

        int particleStartDelay = 50;
        int activeTicks = stage == Stage.STARTING ? stageTicks : START_TICKS + stageTicks;
        int particleTicks = activeTicks - particleStartDelay;
        if (particleTicks < 0) return;

        float ramp = Math.min(1.0F, (particleTicks + 1) / 45.0F);
        int spawnChance = Math.max(1, 7 - Math.round(ramp * 5.0F));
        if (world.rand.nextInt(spawnChance) != 0) return;

        int burstCount = 1;
        if (ramp > 0.35F && world.rand.nextFloat() < ramp * 0.75F) burstCount++;
        if (ramp > 0.75F && world.rand.nextFloat() < (ramp - 0.75F) * 2.0F) burstCount++;

        EnumFacing facing = getFacing();
        IBlockState particleState = net.minecraft.init.Blocks.NETHER_WART_BLOCK.getDefaultState();
        for (int i = 0; i < burstCount; i++) {
            spawnSingleProcessParticle(facing, particleState, 1.0D + world.rand.nextDouble() * 0.15D);
            if (ramp > 0.45F && world.rand.nextFloat() < 0.45F * ramp) {
                spawnSingleProcessParticle(facing, particleState, 0.55D + world.rand.nextDouble() * 0.20D);
            }
        }
    }

    private void spawnSingleProcessParticle(EnumFacing facing, IBlockState particleState, double motionScale) {
        double cx = pos.getX() + 0.5D;
        double cy = pos.getY() + 0.72D;
        double cz = pos.getZ() + 0.5D;

        double sideSpread = 0.22D;
        double frontOffset = 0.18D;
        double sideways = (world.rand.nextDouble() - 0.5D) * 0.30D;

        double px = cx + facing.getXOffset() * frontOffset + facing.rotateY().getXOffset() * sideways;
        double py = cy + (world.rand.nextDouble() - 0.5D) * 0.18D;
        double pz = cz + facing.getZOffset() * frontOffset + facing.rotateY().getZOffset() * sideways;

        px += (world.rand.nextDouble() - 0.5D) * sideSpread;
        pz += (world.rand.nextDouble() - 0.5D) * sideSpread;

        double mx = (facing.getXOffset() * 0.015D + (world.rand.nextDouble() - 0.5D) * 0.025D) * motionScale;
        double my = (0.015D + world.rand.nextDouble() * 0.02D) * motionScale;
        double mz = (facing.getZOffset() * 0.015D + (world.rand.nextDouble() - 0.5D) * 0.025D) * motionScale;

        world.spawnParticle(
                EnumParticleTypes.BLOCK_CRACK,
                px, py, pz,
                mx, my, mz,
                Block.getStateId(particleState)
        );
    }

    private void tickMachine() {
        switch (stage) {
            case IDLE:
                processEnergyUsed = 0;
                stageTicks = 0;
                if (canStartProcess()) {
                    stage = Stage.STARTING;
                    stageTicks = 0;
                    animationCycle++;
                    if (BlenderSoundRegistry.BLENDER != null) {
                        world.playSound(null, pos, BlenderSoundRegistry.BLENDER, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    }
                    markForSync();
                }
                break;
            case STARTING:
                if (!canContinueProcess()) {
                    resetToIdle();
                    break;
                }
                consumeEnergyForCurrentTick(getTotalProgressTicks());
                stageTicks++;
                if (stageTicks >= START_TICKS) {
                    stage = Stage.WORKING;
                    stageTicks = 0;
                    markForSync();
                }
                break;
            case WORKING:
                if (!canContinueProcess()) {
                    resetToIdle();
                    break;
                }
                consumeEnergyForCurrentTick(getTotalProgressTicks());
                stageTicks++;
                if (stageTicks >= WORK_TICKS) {
                    finishCraft();
                    stage = Stage.STOPPING;
                    stageTicks = 0;
                    animationCycle++;
                    markForSync();
                }
                break;
            case STOPPING:
                stageTicks++;
                if (stageTicks >= STOP_TICKS) {
                    resetToIdle();
                }
                break;
        }
    }

    private void consumeEnergyForCurrentTick(int totalTicks) {
        int absoluteProgress;
        if (stage == Stage.STARTING) {
            absoluteProgress = stageTicks + 1;
        } else if (stage == Stage.WORKING) {
            absoluteProgress = START_TICKS + stageTicks + 1;
        } else {
            return;
        }

        int effectiveProgress = absoluteProgress - PROGRESS_DELAY_TICKS;
        if (effectiveProgress <= 0) {
            return;
        }

        int effectiveTotalTicks = totalTicks - PROGRESS_DELAY_TICKS;
        int targetEnergy = effectiveProgress * PROCESS_ENERGY / effectiveTotalTicks;
        int deltaEnergy = targetEnergy - processEnergyUsed;
        if (deltaEnergy > 0) {
            energyStored -= deltaEnergy;
            if (energyStored < 0) energyStored = 0;
            processEnergyUsed += deltaEnergy;
            markForSync();
        }
    }

    private void finishCraft() {
        inventory.extractItem(0, PROCESS_INPUT_COUNT, false);
        outputTank.fill(new FluidStack(ModFluids.TOXIC_FLESH, PROCESS_OUTPUT_MB), true);
        processEnergyUsed = PROCESS_ENERGY;
    }

    private void resetToIdle() {
        stage = Stage.IDLE;
        stageTicks = 0;
        processEnergyUsed = 0;
        markForSync();
    }

    private int getTotalProgressTicks() {
        return START_TICKS + WORK_TICKS;
    }

    private int getRequiredEnergyBuffer() {
        int currentAbsoluteProgress;
        if (stage == Stage.STARTING) {
            currentAbsoluteProgress = stageTicks;
        } else if (stage == Stage.WORKING) {
            currentAbsoluteProgress = START_TICKS + stageTicks;
        } else {
            currentAbsoluteProgress = 0;
        }

        int nextEffectiveProgress = Math.max(0, (currentAbsoluteProgress + 1) - PROGRESS_DELAY_TICKS);
        int effectiveTotalTicks = Math.max(1, getTotalProgressTicks() - PROGRESS_DELAY_TICKS);
        int targetEnergyNextTick = nextEffectiveProgress * PROCESS_ENERGY / effectiveTotalTicks;
        return Math.max(0, targetEnergyNextTick - processEnergyUsed);
    }

    private boolean canStartProcess() {
        return hasRequiredInput() && hasOutputSpace() && energyStored >= PROCESS_ENERGY;
    }

    private boolean canContinueProcess() {
        return hasRequiredInput() && hasOutputSpace() && energyStored >= getRequiredEnergyBuffer();
    }

    private boolean hasRequiredInput() {
        ItemStack stack = inventory.getStackInSlot(0);
        return isValidInput(stack) && stack.getCount() >= PROCESS_INPUT_COUNT;
    }

    private boolean hasOutputSpace() {
        FluidStack current = outputTank.getFluid();
        return current == null || (current.getFluid() == ModFluids.TOXIC_FLESH && current.amount <= TANK_CAPACITY - PROCESS_OUTPUT_MB);
    }

    private boolean isValidInput(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item target = ForgeRegistries.ITEMS.getValue(ASSIMILATED_FLESH_ID);
        return target != null && stack.getItem() == target;
    }

    private void pushFluidDown() {
        if (outputTank.getFluidAmount() <= 0 || world == null || pos == null) return;
        TileEntity below = world.getTileEntity(pos.down());
        if (below == null) return;

        IFluidHandler target = below.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.UP);
        if (target == null) return;

        FluidStack available = outputTank.drain(Math.min(100, outputTank.getFluidAmount()), false);
        if (available == null || available.amount <= 0) return;

        int accepted = target.fill(available, true);
        if (accepted > 0) {
            outputTank.drain(accepted, true);
        }
    }

    public ItemStack getStackInSlot(int slot) {
        return inventory.getStackInSlot(slot);
    }

    public int getProgressScaled(int width) {
        if (stage == Stage.IDLE || stage == Stage.STOPPING) return 0;

        int absoluteProgress = stage == Stage.STARTING
                ? stageTicks
                : START_TICKS + Math.min(stageTicks, WORK_TICKS);

        int effectiveProgress = Math.max(0, absoluteProgress - PROGRESS_DELAY_TICKS);
        int effectiveTotalTicks = Math.max(1, getTotalProgressTicks() - PROGRESS_DELAY_TICKS);
        return Math.min(width, effectiveProgress * width / effectiveTotalTicks);
    }

    public int getOutputStored() {
        return outputTank.getFluidAmount();
    }

    public int getMaxOutput() {
        return outputTank.getCapacity();
    }

    public Stage getStage() {
        return stage;
    }

    public void markForSync() {
        if (world != null && !world.isRemote) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
            markDirty();
        }
    }

    public EnumFacing getFacing() {
        if (world == null || pos == null) return EnumFacing.NORTH;
        IBlockState state = world.getBlockState(pos);
        return state.getBlock() == BlenderRegistry.BLENDER ? state.getValue(BlockBlender.FACING) : EnumFacing.NORTH;
    }

    private boolean canExposeEnergyTo(@Nullable EnumFacing side) {
        if (side == null) return true;
        return side == getFacing().getOpposite();
    }

    private boolean canExposeFluidTo(@Nullable EnumFacing side) {
        return side == null || side == EnumFacing.DOWN;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) return canExposeEnergyTo(facing);
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return canExposeFluidTo(facing);
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
            if (!canExposeFluidTo(facing)) return null;
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(facing == null ? internalFluidHandler : bottomOutputHandler);
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
        compound.setTag("OutputTank", outputTank.writeToNBT(new NBTTagCompound()));
        compound.setString("Stage", stage.name());
        compound.setInteger("StageTicks", stageTicks);
        compound.setInteger("ProcessEnergyUsed", processEnergyUsed);
        compound.setInteger("AnimationCycle", animationCycle);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        energyStored = compound.getInteger("Energy");
        inventory.deserializeNBT(compound.getCompoundTag("Items"));
        outputTank.readFromNBT(compound.getCompoundTag("OutputTank"));
        try {
            stage = Stage.valueOf(compound.getString("Stage"));
        } catch (Exception ignored) {
            stage = Stage.IDLE;
        }
        stageTicks = compound.getInteger("StageTicks");
        processEnergyUsed = compound.getInteger("ProcessEnergyUsed");
        animationCycle = compound.getInteger("AnimationCycle");
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
        String animationKey;
        switch (stage) {
            case STARTING:
                animationKey = "start_and_process#" + animationCycle;
                break;
            case WORKING:
                animationKey = "process";
                break;
            case STOPPING:
                animationKey = "stop#" + animationCycle;
                break;
            default:
                animationKey = "idle";
                break;
        }

        if (!animationKey.equals(lastAnimationKey)) {
            controller.markNeedsReload();
            lastAnimationKey = animationKey;
        }

        if (stage == Stage.STARTING) {
            controller.setAnimation(new AnimationBuilder()
                    .addAnimation("blender_anim", false)
                    .addAnimation("blender_in_process", true));
            controller.setAnimationSpeed(1.0D);
            return PlayState.CONTINUE;
        }
        if (stage == Stage.WORKING) {
            controller.setAnimation(new AnimationBuilder().addAnimation("blender_in_process", true));
            controller.setAnimationSpeed(1.0D);
            return PlayState.CONTINUE;
        }
        if (stage == Stage.STOPPING) {
            controller.setAnimation(new AnimationBuilder().addAnimation("blender stop", false));
            controller.setAnimationSpeed(1.0D);
            return PlayState.CONTINUE;
        }

        controller.setAnimationSpeed(0.0D);
        return PlayState.STOP;
    }

    @Override
    public void registerControllers(AnimationData animationData) {
        animationData.addAnimationController(new AnimationController<TileBlender>(this, "controller", 0.0f, this::animationPredicate));
    }

    @Override public AnimationFactory getFactory() { return factory; }
}
