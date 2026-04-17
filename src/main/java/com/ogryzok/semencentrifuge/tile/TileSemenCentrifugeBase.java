package com.ogryzok.semencentrifuge.tile;

import com.ogryzok.chair.ChairRegistry;
import com.ogryzok.fluids.ModFluids;
import com.ogryzok.food.FoodRegistry;
import com.ogryzok.chair.block.BlockChair;
import com.ogryzok.chair.entity.EntitySeat;
import com.ogryzok.network.ModNetwork;
import com.ogryzok.player.semen.ISemenStorage;
import com.ogryzok.player.semen.SemenProvider;
import com.ogryzok.player.semen.packet.PacketSyncSemen;
import com.ogryzok.semencentrifuge.SemenCentrifugeRegistry;
import com.ogryzok.semencentrifuge.block.BlockSemenCentrifugeBase;
import com.ogryzok.semencentrifuge.block.BlockSemenCentrifugeMotor;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import javax.annotation.Nullable;
import java.util.UUID;

public class TileSemenCentrifugeBase extends TileEntity implements ITickable, IEnergyStorage, IAnimatable {
    private static final int MAX_ENERGY = 10000;
    private static final int MAX_BIOMASS = 10000;
    private static final int ENERGY_PER_TICK_ACTIVE = 12;
    private static final int ENERGY_PER_TICK_PAUSE = 4;
    private static final int LOAD_FAIL = 100;
    private static final int CAN_VOLUME = 200;
    private static final int CAN_FILL_ENERGY = 80;
    private static final int CAN_FILL_TIME_TICKS = 20;
    private static final int AUTO_OUTPUT_RATE = 50;

    private static final int[] CHECKS_PER_ROUND = new int[]{3, 3, 4, 4, 5};
    private static final int[] CHECK_TICKS = new int[]{60, 54, 52, 38, 30};
    private static final int[] CHECK_LIFETIME_TICKS = new int[]{85, 78, 78, 56, 44};
    private static final float[] HIT_SIZES = new float[]{0.26F, 0.23F, 0.21F, 0.16F, 0.12F};
    private static final float[] PERFECT_SIZES = new float[]{0.10F, 0.088F, 0.078F, 0.058F, 0.045F};

    public static final int STATE_IDLE = 0;
    public static final int STATE_RUNNING = 1;
    public static final int STATE_BETWEEN_ROUNDS = 2;
    public static final int STATE_FORCED_COUNTDOWN = 3;
    public static final int STATE_FAILED = 4;
    public static final int STATE_FINISHED = 5;

    private final AnimationFactory factory = new AnimationFactory(this);

    private final net.minecraftforge.items.ItemStackHandler inventory = new net.minecraftforge.items.ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) return FoodRegistry.isEmptyBiomassContainer(stack);
            if (slot == 1) return !stack.isEmpty() && (stack.getItem() == FoodRegistry.BIOMASS_CAN || stack.getItem() == FoodRegistry.BIOMASS_JAR);
            return false;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }
    };
    private int energyStored = 0;
    private int biomassStored = 0;
    private boolean assembled = false;

    private int state = STATE_IDLE;
    private int loadPercent = 0;
    private int currentRound = 0;
    private int currentCheckIndex = 0;
    private int successfulChecksThisRound = 0;
    private int checksThisRound = 0;
    private int earnedRounds = 0;
    private int maxRounds = 0;
    private int startMatter = 0;
    private int betweenTicks = 0;
    private int forcedCountdownTicks = 0;
    private int skillTicksRemaining = 0;
    private int resultDisplayTicks = 0;
    private boolean skillActive = false;
    private float markerProgress = 0.0F;
    private int markerDirection = 1;
    private float hitStart = 0.1F;
    private float hitSize = 0.2F;
    private float perfectStart = 0.15F;
    private float perfectSize = 0.06F;
    private UUID ownerUuid = null;
    private String statusText = "";
    private long sessionSeed = 0L;
    private int canFillTime = 0;
    public net.minecraftforge.items.ItemStackHandler getInventory() {
        return inventory;
    }
    private final IFluidHandler biomassOutputHandler = new IFluidHandler() {
        @Override
        public IFluidTankProperties[] getTankProperties() {
            final int stored = biomassStored;
            return new IFluidTankProperties[] {
                    new IFluidTankProperties() {
                        @Override public FluidStack getContents() { return stored > 0 ? new FluidStack(ModFluids.BIOMASS, stored) : null; }
                        @Override public int getCapacity() { return MAX_BIOMASS; }
                        @Override public boolean canFill() { return false; }
                        @Override public boolean canDrain() { return true; }
                        @Override public boolean canFillFluidType(FluidStack fluidStack) { return false; }
                        @Override public boolean canDrainFluidType(FluidStack fluidStack) { return fluidStack != null && fluidStack.getFluid() == ModFluids.BIOMASS; }
                    }
            };
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            if (resource == null || resource.amount <= 0 || resource.getFluid() != ModFluids.BIOMASS) return null;
            return drain(resource.amount, doDrain);
        }

        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            int drained = Math.min(Math.max(0, maxDrain), biomassStored);
            if (drained <= 0) return null;
            if (doDrain) {
                biomassStored -= drained;
                syncNow();
            }
            return new FluidStack(ModFluids.BIOMASS, drained);
        }
    };

    @Override
    public void update() {
        if (world == null || world.isRemote) {
            return;
        }

        if (world.getTotalWorldTime() % 10L == 0L) {
            syncStructureState();
        }

        if (!assembled && state != STATE_IDLE) {
            failSession(true, "@gui.harvestech.centrifuge.status.assembly_broken");
            return;
        }

        if (state == STATE_RUNNING || state == STATE_BETWEEN_ROUNDS || state == STATE_FORCED_COUNTDOWN) {
            EntityPlayerMP player = getOwnerPlayer();
            if (player == null || !canPlayerUseMotorGui(player)) {
                failSession(true, "@gui.harvestech.centrifuge.status.operator_lost");
                return;
            }
        }

        tickCanFilling();
        pushBiomassOutput();

        switch (state) {
            case STATE_RUNNING:
                tickRunning();
                break;
            case STATE_BETWEEN_ROUNDS:
                tickBetweenRounds();
                break;
            case STATE_FORCED_COUNTDOWN:
                tickForcedCountdown();
                break;
            case STATE_FAILED:
            case STATE_FINISHED:
                if (resultDisplayTicks > 0) {
                    resultDisplayTicks--;
                }
                if (resultDisplayTicks <= 0) {
                    state = STATE_IDLE;
                    statusText = "";
                    syncNow();
                }
                break;
            default:
                break;
        }
    }

    private void tickRunning() {
        if (!consumeEnergy(ENERGY_PER_TICK_ACTIVE)) {
            failSession(true, "@gui.harvestech.centrifuge.status.not_enough_energy");
            return;
        }

        if (!skillActive) {
            beginSkillCheck();
        }

        if (skillTicksRemaining > 0) {
            skillTicksRemaining--;
        }

        float step = 1.0F / (float) Math.max(1, CHECK_TICKS[getRoundIndex()]);
        markerProgress += step * markerDirection;
        markerProgress = wrap01(markerProgress);

        if (skillTicksRemaining <= 0) {
            registerSkillAttempt(false);
        }

        syncNow();
    }

    private void tickBetweenRounds() {
        if (!consumeEnergy(ENERGY_PER_TICK_PAUSE)) {
            failSession(true, "@gui.harvestech.centrifuge.status.not_enough_energy");
            return;
        }

        if (betweenTicks > 0) {
            betweenTicks--;
        }

        if (earnedRounds >= maxRounds) {
            state = STATE_FORCED_COUNTDOWN;
            forcedCountdownTicks = 60;
            statusText = "@gui.harvestech.centrifuge.status.collect_prompt";
            syncNow();
            return;
        }

        if (betweenTicks <= 0) {
            startRound(currentRound + 1);
            return;
        }

        syncNow();
    }

    private void tickForcedCountdown() {
        if (!consumeEnergy(ENERGY_PER_TICK_PAUSE)) {
            failSession(true, "@gui.harvestech.centrifuge.status.not_enough_energy");
            return;
        }

        if (forcedCountdownTicks > 0) {
            forcedCountdownTicks--;
        }

        int sec = Math.max(1, (forcedCountdownTicks + 19) / 20);
        statusText = "@gui.harvestech.centrifuge.status.collect_result_countdown|" + sec;

        if (forcedCountdownTicks <= 0) {
            failSession(true, "@gui.harvestech.centrifuge.status.collect_timeout");
            return;
        }

        syncNow();
    }

    public void syncStructureState() {
        boolean newValue = checkAssembled();
        if (newValue != this.assembled) {
            this.assembled = newValue;
            markDirty();
            syncNow();
        }
    }

    public boolean isAssembled() {
        return checkAssembled();
    }

    private boolean checkAssembled() {
        if (world == null || pos == null) {
            return false;
        }

        IBlockState self = world.getBlockState(pos);
        if (self.getBlock() != SemenCentrifugeRegistry.SEMEN_CENTRIFUGE_BASE) {
            return false;
        }

        IBlockState above = world.getBlockState(pos.up());
        if (above.getBlock() != SemenCentrifugeRegistry.SEMEN_CENTRIFUGE_MOTOR) {
            return false;
        }

        return self.getValue(BlockSemenCentrifugeBase.FACING) == above.getValue(BlockSemenCentrifugeMotor.FACING);
    }

    public EnumFacing getFacing() {
        if (world == null || pos == null) {
            return EnumFacing.NORTH;
        }
        IBlockState state = world.getBlockState(pos);
        return state.getBlock() == SemenCentrifugeRegistry.SEMEN_CENTRIFUGE_BASE
                ? state.getValue(BlockSemenCentrifugeBase.FACING)
                : EnumFacing.NORTH;
    }

    public BlockPos getChairPos() {
        return pos.offset(getFacing());
    }

    public boolean hasValidChairSetup() {
        if (world == null || pos == null) {
            return false;
        }

        IBlockState chairState = world.getBlockState(getChairPos());
        if (chairState.getBlock() != ChairRegistry.CHAIR) {
            return false;
        }

        return chairState.getValue(BlockChair.FACING) == getFacing().getOpposite();
    }

    public boolean canPlayerUseMotorGui(EntityPlayer player) {
        if (world == null || player == null) return false;

        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockSemenCentrifugeBase)) return false;

        EnumFacing baseFacing = state.getValue(BlockHorizontal.FACING);
        BlockPos chairPos = pos.offset(baseFacing);
        IBlockState chairState = world.getBlockState(chairPos);

        if (!(chairState.getBlock() instanceof BlockChair)) return false;

        EnumFacing chairFacing = chairState.getValue(BlockHorizontal.FACING);
        if (chairFacing != baseFacing.getOpposite()) return false;
        if (!(player.getRidingEntity() instanceof EntitySeat)) return false;

        EntitySeat seat = (EntitySeat) player.getRidingEntity();
        return chairPos.equals(seat.getSeatPos());
    }

    public boolean startSession(EntityPlayer player) {
        if (world == null || world.isRemote || player == null) {
            return false;
        }

        if (!isAssembled()) {
            statusText = "@gui.harvestech.centrifuge.status.machine_not_assembled";
            syncNow();
            return false;
        }

        if (!canPlayerUseMotorGui(player)) {
            statusText = "@gui.harvestech.centrifuge.status.sit_on_chair";
            syncNow();
            return false;
        }

        if (state == STATE_RUNNING || state == STATE_BETWEEN_ROUNDS || state == STATE_FORCED_COUNTDOWN) {
            statusText = "@gui.harvestech.centrifuge.status.already_running";
            syncNow();
            return false;
        }

        ISemenStorage storage = player.getCapability(SemenProvider.SEMEN_CAP, null);
        if (storage == null) {
            statusText = "@gui.harvestech.centrifuge.status.matter_storage_not_found";
            syncNow();
            return false;
        }

        if (storage.getAmount() < 200) {
            statusText = "@gui.harvestech.centrifuge.status.not_enough_matter";
            syncNow();
            return false;
        }

        this.ownerUuid = player.getUniqueID();
        this.startMatter = storage.getAmount();
        this.maxRounds = this.startMatter / 200;
        this.earnedRounds = 0;
        this.loadPercent = 0;
        this.currentRound = 0;
        this.currentCheckIndex = 0;
        this.successfulChecksThisRound = 0;
        this.checksThisRound = 0;
        this.skillActive = false;
        this.markerProgress = 0.0F;
        this.skillTicksRemaining = 0;
        this.sessionSeed = world.rand.nextLong();
        this.resultDisplayTicks = 0;
        this.statusText = "@gui.harvestech.centrifuge.status.operation_started";

        startRound(1);
        syncNow();
        return true;
    }

    private void startRound(int roundNumber) {
        this.currentRound = Math.max(1, Math.min(5, roundNumber));
        this.currentCheckIndex = 0;
        this.successfulChecksThisRound = 0;
        this.checksThisRound = CHECKS_PER_ROUND[getRoundIndex()];
        this.state = STATE_RUNNING;
        this.skillActive = false;
        this.statusText = "@gui.harvestech.centrifuge.status.round|" + this.currentRound;
        beginSkillCheck();
    }

    private void beginSkillCheck() {
        this.skillActive = true;
        this.skillTicksRemaining = CHECK_LIFETIME_TICKS[getRoundIndex()];
        this.hitSize = HIT_SIZES[getRoundIndex()];
        this.perfectSize = PERFECT_SIZES[getRoundIndex()];
        this.hitStart = world.rand.nextFloat() * (1.0F - this.hitSize);
        this.perfectStart = this.hitStart + (this.hitSize - this.perfectSize) * world.rand.nextFloat();
        this.markerDirection = world.rand.nextBoolean() ? 1 : -1;

        float hitCenter = wrap01(this.hitStart + this.hitSize * 0.5F);
        float minDistance;
        float maxDistance;
        switch (getRoundIndex()) {
            case 0:
                minDistance = 0.26F;
                maxDistance = 0.34F;
                break;
            case 1:
                minDistance = 0.20F;
                maxDistance = 0.28F;
                break;
            case 2:
                minDistance = 0.18F;
                maxDistance = 0.26F;
                break;
            case 3:
                minDistance = 0.12F;
                maxDistance = 0.18F;
                break;
            default:
                minDistance = 0.07F;
                maxDistance = 0.11F;
                break;
        }

        float distance = minDistance + world.rand.nextFloat() * (maxDistance - minDistance);
        // Стартуем ДО зоны по направлению движения, чтобы маркер гарантированно доезжал до неё.
        this.markerProgress = wrap01(hitCenter - (distance * this.markerDirection));
        this.statusText = "@gui.harvestech.centrifuge.status.round_check|" + currentRound + "|" + (currentCheckIndex + 1) + "|" + checksThisRound;
    }

    public void onHitKey(EntityPlayer player) {
        if (world == null || world.isRemote || player == null) return;
        if (!isOwnedBy(player) || state != STATE_RUNNING || !skillActive) return;

        boolean inHit = isInWrappedRange(markerProgress, hitStart, hitSize);
        boolean inPerfect = isInWrappedRange(markerProgress, perfectStart, perfectSize);

        if (!inHit) {
            registerSkillAttempt(false);
            return;
        }

        if (inPerfect) {
            addLoad(4);
        } else {
            addLoad(8);
        }

        if (loadPercent >= LOAD_FAIL) {
            failSession(true, "@gui.harvestech.centrifuge.status.load_failed");
            return;
        }

        registerSkillAttempt(true);
    }

    private void registerSkillAttempt(boolean success) {
        this.skillActive = false;
        this.currentCheckIndex++;

        if (success) {
            this.successfulChecksThisRound++;
        } else {
            addLoad(20);
        }

        if (loadPercent >= LOAD_FAIL) {
            failSession(true, "@gui.harvestech.centrifuge.status.load_failed");
            return;
        }

        if (currentCheckIndex >= checksThisRound) {
            if (successfulChecksThisRound >= checksThisRound) {
                earnedRounds++;
                state = STATE_BETWEEN_ROUNDS;
                betweenTicks = 10 * 20;
                statusText = earnedRounds >= maxRounds
                        ? "@gui.harvestech.centrifuge.status.collect_prompt"
                        : "";
            } else {
                failSession(true, "@gui.harvestech.centrifuge.status.round_failed");
            }
            syncNow();
            return;
        }

        beginSkillCheck();
        syncNow();
    }

    public void onFinishKey(EntityPlayer player) {
        if (world == null || world.isRemote || player == null) return;
        if (!isOwnedBy(player)) return;

        if (state == STATE_BETWEEN_ROUNDS || state == STATE_FORCED_COUNTDOWN) {
            finalizeSession(true, false);
        }
    }

    private void finalizeSession(boolean manual, boolean autoFinish) {
        EntityPlayerMP player = getOwnerPlayer();
        int reward = 0;
        if (loadPercent < LOAD_FAIL) {
            reward = Math.min(startMatter, earnedRounds * 200);
        }

        if (player != null) {
            ISemenStorage storage = player.getCapability(SemenProvider.SEMEN_CAP, null);
            if (storage != null) {
                storage.clear();
                ModNetwork.CHANNEL.sendTo(new PacketSyncSemen(player.getEntityId(), storage.getAmount(), storage.getTickCounter(), storage.isManualHarvesting(), storage.getManualHarvestTicks(), storage.getAbstinenceTicks(), storage.getAbstinenceStage(), storage.hasSeedKeeper()), player);
            }
        }

        addBiomass(reward);
        this.statusText = reward > 0
                ? (manual
                    ? "@gui.harvestech.centrifuge.status.completed|" + reward
                    : (autoFinish
                        ? "@gui.harvestech.centrifuge.status.auto_finish|" + reward
                        : "@gui.harvestech.centrifuge.status.reward|" + reward))
                : "@gui.harvestech.centrifuge.status.result_zero";
        this.state = STATE_FINISHED;
        this.resultDisplayTicks = 100;
        resetSessionCore();
        syncNow();
    }

    private void failSession(boolean consumeMatter, String message) {
        EntityPlayerMP player = getOwnerPlayer();
        if (consumeMatter && player != null) {
            ISemenStorage storage = player.getCapability(SemenProvider.SEMEN_CAP, null);
            if (storage != null) {
                storage.clear();
                ModNetwork.CHANNEL.sendTo(new PacketSyncSemen(player.getEntityId(), storage.getAmount(), storage.getTickCounter(), storage.isManualHarvesting(), storage.getManualHarvestTicks(), storage.getAbstinenceTicks(), storage.getAbstinenceStage(), storage.hasSeedKeeper()), player);
            }
        }

        this.state = STATE_FAILED;
        this.statusText = message == null ? "@gui.harvestech.centrifuge.status.failure" : message;
        this.resultDisplayTicks = 100;
        resetSessionCore();
        syncNow();
    }

    private void resetSessionCore() {
        this.ownerUuid = null;
        this.startMatter = 0;
        this.maxRounds = 0;
        this.currentRound = 0;
        this.currentCheckIndex = 0;
        this.successfulChecksThisRound = 0;
        this.checksThisRound = 0;
        this.betweenTicks = 0;
        this.forcedCountdownTicks = 0;
        this.skillTicksRemaining = 0;
        this.skillActive = false;
        this.markerProgress = 0.0F;
        this.markerDirection = 1;
        this.loadPercent = 0;
    }

    private void addLoad(int amount) {
        this.loadPercent = Math.max(0, Math.min(LOAD_FAIL, this.loadPercent + amount));
    }

    private boolean consumeEnergy(int amount) {
        if (amount <= 0) return true;
        if (energyStored < amount) return false;
        energyStored -= amount;
        markDirty();
        return true;
    }

    private boolean isOwnedBy(EntityPlayer player) {
        return ownerUuid != null && player != null && ownerUuid.equals(player.getUniqueID());
    }

    @Nullable
    private EntityPlayerMP getOwnerPlayer() {
        if (world == null || world.isRemote || ownerUuid == null) {
            return null;
        }
        EntityPlayer player = world.getPlayerEntityByUUID(ownerUuid);
        return player instanceof EntityPlayerMP ? (EntityPlayerMP) player : null;
    }

    private boolean isInWrappedRange(float value, float start, float size) {
        float end = start + size;
        if (end <= 1.0F) {
            return value >= start && value <= end;
        }
        return value >= start || value <= (end - 1.0F);
    }

    private float wrap01(float value) {
        while (value < 0.0F) value += 1.0F;
        while (value >= 1.0F) value -= 1.0F;
        return value;
    }

    private int getRoundIndex() {
        return Math.max(0, Math.min(4, currentRound - 1));
    }

    private void syncNow() {
        if (world == null || world.isRemote) return;
        markDirty();
        IBlockState state = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, state, state, 3);
    }

    public int addBiomass(int amount) {
        if (amount <= 0) return 0;
        int accepted = Math.min(amount, MAX_BIOMASS - biomassStored);
        if (accepted > 0) {
            biomassStored += accepted;
            markDirty();
        }
        return accepted;
    }

    public int getMaxSemenStored() { return MAX_BIOMASS; }
    public int getSemenStored() { return biomassStored; }
    public int getBiomassStored() { return biomassStored; }
    public int getMaxBiomassStored() { return MAX_BIOMASS; }
    public int getEnergyStoredVisual() { return energyStored; }
    public int getSessionState() { return state; }
    public boolean isSessionActive() { return state == STATE_RUNNING || state == STATE_BETWEEN_ROUNDS || state == STATE_FORCED_COUNTDOWN; }
    public int getLoadPercent() { return loadPercent; }
    public int getCurrentRound() { return currentRound; }
    public int getCurrentCheckIndex() { return currentCheckIndex; }
    public int getChecksThisRound() { return checksThisRound; }
    public int getEarnedRounds() { return earnedRounds; }
    public int getMaxRounds() { return maxRounds; }
    public int getStartMatter() { return startMatter; }
    public int getBetweenTicks() { return betweenTicks; }
    public int getForcedCountdownTicks() { return forcedCountdownTicks; }
    public int getSkillTicksRemaining() { return skillTicksRemaining; }
    public boolean isSkillActive() { return skillActive; }
    public float getMarkerProgress() { return markerProgress; }
    public int getMarkerDirection() { return markerDirection; }
    public float getHitStart() { return hitStart; }
    public float getHitSize() { return hitSize; }
    public float getPerfectStart() { return perfectStart; }
    public float getPerfectSize() { return perfectSize; }
    public String getStatusText() { return statusText == null ? "" : statusText; }
    public long getSessionSeed() { return sessionSeed; }
    public int getResultDisplayTicks() { return resultDisplayTicks; }

    private void tickCanFilling() {
        ItemStack input = inventory.getStackInSlot(0);
        if (!FoodRegistry.isEmptyBiomassContainer(input)) {
            canFillTime = 0;
            return;
        }

        if (biomassStored < CAN_VOLUME || energyStored < CAN_FILL_ENERGY) {
            canFillTime = 0;
            return;
        }

        ItemStack result = FoodRegistry.getFilledBiomassContainer(input);
        if (result.isEmpty()) {
            canFillTime = 0;
            return;
        }
        ItemStack simulated = inventory.insertItem(1, result.copy(), true);
        if (!simulated.isEmpty()) {
            canFillTime = 0;
            return;
        }

        canFillTime++;
        if (canFillTime < CAN_FILL_TIME_TICKS) {
            return;
        }

        canFillTime = 0;
        inventory.extractItem(0, 1, false);
        inventory.insertItem(1, result.copy(), false);
        biomassStored -= CAN_VOLUME;
        energyStored -= CAN_FILL_ENERGY;
        syncNow();
    }

    private void pushBiomassOutput() {
        if (biomassStored <= 0 || world == null || pos == null) {
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

        int amount = Math.min(AUTO_OUTPUT_RATE, biomassStored);
        FluidStack offer = new FluidStack(ModFluids.BIOMASS, amount);
        int accepted = target.fill(offer, false);
        if (accepted <= 0) {
            return;
        }

        FluidStack drained = biomassOutputHandler.drain(Math.min(amount, accepted), true);
        if (drained == null || drained.amount <= 0) {
            return;
        }

        target.fill(drained, true);
        syncNow();
    }

    private boolean canDrainBiomassTo(@Nullable EnumFacing facing) {
        return facing != null && facing == getFacing().rotateYCCW();
    }

    private boolean canExposeEnergyTo(@Nullable EnumFacing facing) {
        if (facing == null) {
            return true;
        }
        return facing == getFacing().getOpposite();
    }

    @Override
    public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return canExposeEnergyTo(facing);
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return canDrainBiomassTo(facing);
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            if (canExposeEnergyTo(facing)) {
                return CapabilityEnergy.ENERGY.cast(this);
            }
            return null;
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            if (canDrainBiomassTo(facing)) {
                return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(biomassOutputHandler);
            }
            return null;
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("Energy", this.energyStored);
        compound.setInteger("Biomass", this.biomassStored);
        compound.setBoolean("Assembled", this.assembled);
        compound.setInteger("State", this.state);
        compound.setInteger("Load", this.loadPercent);
        compound.setInteger("CurrentRound", this.currentRound);
        compound.setInteger("CurrentCheckIndex", this.currentCheckIndex);
        compound.setInteger("SuccessfulChecksThisRound", this.successfulChecksThisRound);
        compound.setInteger("ChecksThisRound", this.checksThisRound);
        compound.setInteger("EarnedRounds", this.earnedRounds);
        compound.setInteger("MaxRounds", this.maxRounds);
        compound.setInteger("StartMatter", this.startMatter);
        compound.setInteger("BetweenTicks", this.betweenTicks);
        compound.setInteger("ForcedCountdownTicks", this.forcedCountdownTicks);
        compound.setInteger("SkillTicksRemaining", this.skillTicksRemaining);
        compound.setInteger("ResultDisplayTicks", this.resultDisplayTicks);
        compound.setBoolean("SkillActive", this.skillActive);
        compound.setFloat("MarkerProgress", this.markerProgress);
        compound.setInteger("MarkerDirection", this.markerDirection);
        compound.setFloat("HitStart", this.hitStart);
        compound.setFloat("HitSize", this.hitSize);
        compound.setFloat("PerfectStart", this.perfectStart);
        compound.setFloat("PerfectSize", this.perfectSize);
        compound.setString("StatusText", this.getStatusText());
        compound.setLong("SessionSeed", this.sessionSeed);
        compound.setTag("Inventory", inventory.serializeNBT());
        compound.setInteger("CanFillTime", this.canFillTime);
        if (this.ownerUuid != null) {
            compound.setUniqueId("Owner", this.ownerUuid);
        }
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.energyStored = compound.getInteger("Energy");
        this.biomassStored = compound.getInteger("Biomass");
        this.assembled = compound.getBoolean("Assembled");
        this.state = compound.getInteger("State");
        this.loadPercent = compound.getInteger("Load");
        this.currentRound = compound.getInteger("CurrentRound");
        this.currentCheckIndex = compound.getInteger("CurrentCheckIndex");
        this.successfulChecksThisRound = compound.getInteger("SuccessfulChecksThisRound");
        this.checksThisRound = compound.getInteger("ChecksThisRound");
        this.earnedRounds = compound.getInteger("EarnedRounds");
        this.maxRounds = compound.getInteger("MaxRounds");
        this.startMatter = compound.getInteger("StartMatter");
        this.betweenTicks = compound.getInteger("BetweenTicks");
        this.forcedCountdownTicks = compound.getInteger("ForcedCountdownTicks");
        this.skillTicksRemaining = compound.getInteger("SkillTicksRemaining");
        this.resultDisplayTicks = compound.getInteger("ResultDisplayTicks");
        this.skillActive = compound.getBoolean("SkillActive");
        this.markerProgress = compound.getFloat("MarkerProgress");
        this.markerDirection = compound.getInteger("MarkerDirection");
        this.hitStart = compound.getFloat("HitStart");
        this.hitSize = compound.getFloat("HitSize");
        this.perfectStart = compound.getFloat("PerfectStart");
        this.perfectSize = compound.getFloat("PerfectSize");
        this.statusText = compound.getString("StatusText");
        this.sessionSeed = compound.getLong("SessionSeed");
        if (compound.hasKey("Inventory")) {
            inventory.deserializeNBT(compound.getCompoundTag("Inventory"));
        }
        this.canFillTime = compound.getInteger("CanFillTime");
        this.ownerUuid = compound.hasUniqueId("Owner") ? compound.getUniqueId("Owner") : null;
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
        if (!isAssembled()) {
            return 0;
        }
        int received = Math.min(MAX_ENERGY - this.energyStored, Math.max(0, maxReceive));
        if (!simulate && received > 0) {
            this.energyStored += received;
            markDirty();
            syncNow();
        }
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
    @Override
    public int getEnergyStored() { return this.energyStored; }
    @Override
    public int getMaxEnergyStored() { return MAX_ENERGY; }
    @Override
    public boolean canExtract() { return false; }
    @Override
    public boolean canReceive() { return true; }
    @Override
    public void registerControllers(AnimationData animationData) { }
    @Override
    public AnimationFactory getFactory() { return factory; }
}
