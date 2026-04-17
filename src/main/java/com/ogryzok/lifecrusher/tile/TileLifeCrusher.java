package com.ogryzok.lifecrusher.tile;

import com.ogryzok.lifecrusher.block.BlockLifeCrusher;
import com.ogryzok.lifecrusher.sound.LifeCrusherSoundRegistry;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import net.minecraft.init.SoundEvents;
import com.ogryzok.lifecrusher.player.PlayerBerserkHandler;

public class TileLifeCrusher extends TileEntity implements ITickable, IEnergyStorage, IAnimatable {
    private static final int MAX_ENERGY = 10000;
    private static final int OUTPUT_PER_TICK = 4000;
    private static final int NO_SHIFT_TIMEOUT = 80;
    private static final int FAIL_DURATION = 70;
    private static final int DAMAGE_INTERVAL = 10;

    private static final float PRESS_BOOST = 0.18f;
    private static final float PASSIVE_RISE_RATE = 0.035f;
    private static final float SPINDOWN_RATE = 0.02f;
    private static final float MIN_VISIBLE_SPEED = 0.01f;
    private static final float OVERLOAD_DAMAGE = 16.0F;

    private static final float KNOCKBACK_CHANCE = 0.15F;
    private static final double BACK_ALIGNMENT_DOT_MIN = 0.83D;
    private static final float WRONG_POSE_DAMAGE = 10.0F;
    private static final int KICK_ANIM_DURATION = 20;
    private static final double FAIL_KNOCKBACK_FORCE = 1.15D;

    private static final int BERSERK_REQUIRED_PRESSES = 20;
    private static final int BERSERK_PRESS_WINDOW_TICKS = 5;
    private static final int BERSERK_DURATION = 80;
    private static final float BERSERK_DAMAGE_MULT = 1.35F;
    private static final float BERSERK_ENERGY_MULT = 1.5F;
    private static final float BERSERK_ANIM_MULT = 1.8F;

    private static final int REQUIRED_WIRES = 8;
    private static final int BUILD_STATUS_RADIUS = 4;
    private static final int BUILD_STATUS_INTERVAL = 10;

    private static final double PRESS_DISTANCE_MIN = 0.45D;
    private static final double PRESS_DISTANCE_MAX = 1.50D;

    private final AnimationFactory factory = new AnimationFactory(this);

    private CrusherState state = CrusherState.IDLE;
    private int energy = 0;
    private float workSpeed = 0.0f;
    private int shiftProgress = 0;
    private long lastShiftTick = -9999L;
    private long failUntilTick = 0L;
    private long kickAnimUntilTick = 0L;
    private UUID trackedPlayerId = null;
    private boolean trackedPrevSneak = false;
    private String lastAnimationKey = "";
    private boolean failParticlesPlayed = false;

    private int wiresInstalled = 0;
    private boolean rodInstalled = false;

    private int rapidShiftCombo = 0;
    private long lastRapidShiftTick = -9999L;
    private long berserkUntilTick = 0L;

    private int assemblySteamTicks = 0;

    private final IEnergyStorage outputWrapper = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return TileLifeCrusher.this.extractEnergy(maxExtract, simulate);
        }

        @Override
        public int getEnergyStored() {
            return TileLifeCrusher.this.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return TileLifeCrusher.this.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    };

    public CrusherState getState() {
        return this.state;
    }

    public float getWorkSpeed() {
        return this.workSpeed;
    }

    public boolean isPowered() {
        return this.state == CrusherState.SPINUP
                || this.state == CrusherState.WORK
                || this.state == CrusherState.SPINDOWN;
    }

    public float getProgressRaw() {
        return this.workSpeed;
    }

    public float getProgress(float partialTicks) {
        return this.workSpeed;
    }

    public boolean isOverloaded() {
        return this.energy >= getMaxEnergyStored();
    }

    public boolean isBerserk() {
        return this.world != null && this.world.getTotalWorldTime() < this.berserkUntilTick;
    }

    private boolean isKickAnimating() {
        return this.world != null && this.world.getTotalWorldTime() < this.kickAnimUntilTick;
    }

    public int getWiresInstalled() {
        return this.wiresInstalled;
    }

    public boolean hasRodInstalled() {
        return this.rodInstalled;
    }

    public boolean isAssembled() {
        return this.wiresInstalled >= REQUIRED_WIRES && this.rodInstalled;
    }

    public boolean canAcceptWire() {
        return this.wiresInstalled < REQUIRED_WIRES;
    }

    public boolean canAcceptRod() {
        return this.wiresInstalled >= REQUIRED_WIRES && !this.rodInstalled;
    }

    public boolean addWire() {
        if (!this.canAcceptWire()) {
            return false;
        }

        this.wiresInstalled++;

        if (this.world != null && !this.world.isRemote && this.pos != null && LifeCrusherSoundRegistry.CABLE != null) {
            this.world.playSound(
                    null,
                    this.pos,
                    LifeCrusherSoundRegistry.CABLE,
                    SoundCategory.BLOCKS,
                    1.0F,
                    1.0F
            );
        }

        this.setChangedAndSync();
        return true;
    }

    public boolean installRod() {
        if (!this.canAcceptRod()) {
            return false;
        }

        this.rodInstalled = true;

        if (this.world != null && !this.world.isRemote && this.pos != null) {
            this.assemblySteamTicks = 20;

            if (LifeCrusherSoundRegistry.MACHINEDONE != null) {
                this.world.playSound(
                        null,
                        this.pos,
                        LifeCrusherSoundRegistry.MACHINEDONE,
                        SoundCategory.BLOCKS,
                        1.15F,
                        1.0F
                );
            }
        }

        this.setChangedAndSync();
        return true;
    }

    public String getBuildStatusText() {
        if (this.wiresInstalled < REQUIRED_WIRES) {
            return "copper_wires " + this.wiresInstalled + "/" + REQUIRED_WIRES;
        }

        if (!this.rodInstalled) {
            return "Bayonet missing";
        }

        return "Life crusher ready";
    }

    private void broadcastBuildStatus() {
        if (this.world == null || this.world.isRemote || this.pos == null) {
            return;
        }

        AxisAlignedBB box = new AxisAlignedBB(this.pos).grow(BUILD_STATUS_RADIUS);
        List<EntityPlayerMP> players = this.world.getEntitiesWithinAABB(EntityPlayerMP.class, box);

        if (players.isEmpty()) {
            return;
        }

        TextComponentString text = new TextComponentString(this.getBuildStatusText());

        for (EntityPlayerMP player : players) {
            player.connection.sendPacket(new SPacketChat(text, ChatType.GAME_INFO));
        }
    }

    @Override
    public void update() {
        if (this.world == null) {
            return;
        }

        if (!this.world.isRemote && this.assemblySteamTicks > 0) {
            this.spawnAssemblySteamBurst();
            this.assemblySteamTicks--;
        }

        if (this.world.isRemote) {
            return;
        }

        if (!this.isAssembled()) {
            if (this.world.getTotalWorldTime() % BUILD_STATUS_INTERVAL == 0L) {
                this.broadcastBuildStatus();
            }

            boolean changed = false;

            if (this.state != CrusherState.IDLE) {
                this.state = CrusherState.IDLE;
                changed = true;
            }

            if (Float.compare(this.workSpeed, 0.0F) != 0) {
                this.workSpeed = 0.0F;
                changed = true;
            }

            if (this.shiftProgress != 0) {
                this.shiftProgress = 0;
                changed = true;
            }

            if (this.trackedPlayerId != null) {
                this.trackedPlayerId = null;
                changed = true;
            }

            if (this.trackedPrevSneak) {
                this.trackedPrevSneak = false;
                changed = true;
            }

            if (this.rapidShiftCombo != 0) {
                this.rapidShiftCombo = 0;
                changed = true;
            }

            if (!this.lastAnimationKey.isEmpty()) {
                this.lastAnimationKey = "";
                changed = true;
            }

            if (changed) {
                this.setChangedAndSync();
            }

            return;
        }

        long now = this.world.getTotalWorldTime();
        boolean changed = false;

        if (this.isKickAnimating()) {
            changed |= this.pushEnergyOut();

            if (this.trackedPrevSneak) {
                this.trackedPrevSneak = false;
                changed = true;
            }

            if (changed) {
                this.setChangedAndSync();
            }
            return;
        }

        if (this.state == CrusherState.FAIL) {
            changed |= this.pushEnergyOut();

            if (now >= this.failUntilTick) {
                this.state = CrusherState.IDLE;
                this.workSpeed = 0.0F;
                this.shiftProgress = 0;
                this.trackedPlayerId = null;
                this.trackedPrevSneak = false;
                this.lastAnimationKey = "";
                this.failParticlesPlayed = false;
                changed = true;
            }

            if (changed) {
                this.setChangedAndSync();
            }
            return;
        }

        EntityPlayer player = this.getTrackedPlayer();
        boolean hasPlayer = player != null;

        if (hasPlayer && this.isBerserk()) {
            PlayerBerserkHandler.refreshBerserkStupor(player);
        }

        if (hasPlayer && this.trackedPlayerId == null) {
            this.trackedPlayerId = player.getUniqueID();
            changed = true;
        }

        boolean isSneaking = hasPlayer && player.isSneaking();
        boolean rawFreshShiftPress = isSneaking && !this.trackedPrevSneak;
        boolean validBackPose = hasPlayer && this.isPlayerBackAligned(player);
        boolean validPressDistance = hasPlayer && this.isPlayerInValidPressDistance(player);
        boolean freshShiftPress = rawFreshShiftPress && validBackPose && validPressDistance;

        if (hasPlayer && rawFreshShiftPress && validPressDistance && !validBackPose) {
            if (this.energy >= getMaxEnergyStored()) {
                this.triggerFail(player);
            } else {
                this.punishWrongPose(player);
            }

            this.trackedPrevSneak = isSneaking;
            return;
        }

        if (freshShiftPress) {
            if (this.energy >= getMaxEnergyStored()) {
                this.triggerFail(player);
                return;
            }

            this.lastShiftTick = now;
            this.shiftProgress = Math.min(100, this.shiftProgress + 1);

            if (this.trackedPlayerId == null || !player.getUniqueID().equals(this.trackedPlayerId)) {
                this.trackedPlayerId = player.getUniqueID();
                changed = true;
            }

            this.handleBerserkPress(now);

            float pressBoost = PRESS_BOOST;
            if (this.isBerserk()) {
                pressBoost *= 1.20F;
            }

            float nextSpeed = Math.min(1.0f, this.workSpeed + pressBoost);
            if (Float.compare(nextSpeed, this.workSpeed) != 0) {
                this.workSpeed = nextSpeed;
                changed = true;
            }
        }

        if (this.trackedPrevSneak != isSneaking) {
            this.trackedPrevSneak = isSneaking;
            changed = true;
        }

        boolean shouldKeepRunning = (now - this.lastShiftTick) <= NO_SHIFT_TIMEOUT;

        if (shouldKeepRunning) {
            if (this.state == CrusherState.IDLE || this.state == CrusherState.SPINDOWN) {
                this.state = CrusherState.SPINUP;
                changed = true;
            }

            if (!freshShiftPress && this.workSpeed < 1.0f) {
                float rise = PASSIVE_RISE_RATE;
                if (this.isBerserk()) {
                    rise *= 1.20F;
                }

                float nextSpeed = Math.min(1.0f, this.workSpeed + rise);
                if (Float.compare(nextSpeed, this.workSpeed) != 0) {
                    this.workSpeed = nextSpeed;
                    changed = true;
                }
            }

            if (this.workSpeed >= 0.999f && this.state != CrusherState.WORK) {
                this.state = CrusherState.WORK;
                changed = true;
            }
        } else {
            if (this.state != CrusherState.IDLE && this.state != CrusherState.SPINDOWN) {
                this.state = CrusherState.SPINDOWN;
                changed = true;
            }

            if (this.rapidShiftCombo != 0) {
                this.rapidShiftCombo = 0;
                changed = true;
            }
        }

        if (this.state == CrusherState.WORK && hasPlayer && freshShiftPress && now % DAMAGE_INTERVAL == 0L) {
            if (this.energy >= MAX_ENERGY) {
                this.triggerFail(player);
                return;
            }

            float damage = 1.0f + this.workSpeed * 1.5f;
            if (this.isBerserk()) {
                damage *= BERSERK_DAMAGE_MULT;
            }

            boolean dealt = player.attackEntityFrom(DamageSource.GENERIC, damage);
            if (dealt) {
                if (this.world.rand.nextFloat() < KNOCKBACK_CHANCE) {
                    this.knockPlayer(player);
                }

                this.addEnergyFromDamage(damage);

                if (this.energy >= MAX_ENERGY) {
                    this.triggerFail(player);
                    return;
                }

                changed = true;
            }
        }

        if (this.state == CrusherState.SPINDOWN) {
            float nextSpeed = Math.max(0.0f, this.workSpeed - SPINDOWN_RATE);
            if (Float.compare(nextSpeed, this.workSpeed) != 0) {
                this.workSpeed = nextSpeed;
                changed = true;
            }

            if (this.workSpeed <= MIN_VISIBLE_SPEED) {
                this.workSpeed = 0.0f;
                this.shiftProgress = 0;
                this.trackedPlayerId = null;
                this.trackedPrevSneak = false;
                this.state = CrusherState.IDLE;
                this.lastAnimationKey = "";
                changed = true;
            }
        }

        if (this.state == CrusherState.IDLE) {
            if (Float.compare(this.workSpeed, 0.0f) != 0) {
                this.workSpeed = 0.0f;
                changed = true;
            }

            if (!hasPlayer) {
                this.trackedPlayerId = null;
            }
        }

        if (!this.isBerserk()
                && this.rapidShiftCombo != 0
                && (now - this.lastRapidShiftTick) > BERSERK_PRESS_WINDOW_TICKS) {
            this.rapidShiftCombo = 0;
            changed = true;
        }

        changed |= this.pushEnergyOut();

        if (changed) {
            this.setChangedAndSync();
        }
    }

    private void handleBerserkPress(long now) {
        if ((now - this.lastRapidShiftTick) <= BERSERK_PRESS_WINDOW_TICKS) {
            this.rapidShiftCombo++;
        } else {
            this.rapidShiftCombo = 1;
        }

        this.lastRapidShiftTick = now;

        if (this.rapidShiftCombo >= BERSERK_REQUIRED_PRESSES) {
            this.berserkUntilTick = now + BERSERK_DURATION;
            this.rapidShiftCombo = 0;
        }
    }

    private void addEnergyFromDamage(float damage) {
        int gained = Math.round(damage * 100.0f);
        if (this.isBerserk()) {
            gained = Math.round(gained * BERSERK_ENERGY_MULT);
        }
        this.energy = Math.min(MAX_ENERGY, this.energy + gained);
    }

    private void punishWrongPose(EntityPlayer player) {
        if (player == null || this.world == null) {
            return;
        }

        if (!this.world.isRemote && this.pos != null && LifeCrusherSoundRegistry.HIT != null) {
            this.world.playSound(
                    null,
                    this.pos,
                    LifeCrusherSoundRegistry.HIT,
                    SoundCategory.BLOCKS,
                    1.0F,
                    1.0F
            );
        }

        this.kickAnimUntilTick = this.world.getTotalWorldTime() + KICK_ANIM_DURATION;
        this.lastAnimationKey = "";
        this.rapidShiftCombo = 0;
        this.lastShiftTick = -9999L;
        this.state = CrusherState.IDLE;
        this.workSpeed = 0.0F;
        this.shiftProgress = 0;

        player.attackEntityFrom(DamageSource.GENERIC, WRONG_POSE_DAMAGE);

        if (!this.world.isRemote && player instanceof EntityPlayerMP) {
            ((EntityPlayerMP) player).connection.sendPacket(
                    new SPacketChat(
                            new TextComponentString("Turn your back!"),
                            ChatType.GAME_INFO
                    )
            );
        }

        this.setChangedAndSync();
    }

    private void triggerFail(@Nullable EntityPlayer victim) {
        this.state = CrusherState.FAIL;
        this.shiftProgress = 0;
        this.trackedPlayerId = null;
        this.trackedPrevSneak = false;
        this.lastShiftTick = -9999L;
        this.failUntilTick = this.world.getTotalWorldTime() + FAIL_DURATION;
        this.lastAnimationKey = "";
        this.rapidShiftCombo = 0;
        this.failParticlesPlayed = false;

        if (this.world != null) {
            if (LifeCrusherSoundRegistry.CRUSH != null) {
                this.world.playSound(
                        null,
                        this.pos,
                        LifeCrusherSoundRegistry.CRUSH,
                        SoundCategory.BLOCKS,
                        1.15F,
                        1.0F
                );
            }

            this.world.playSound(
                    null,
                    this.pos,
                    SoundEvents.ENTITY_GENERIC_EXPLODE,
                    SoundCategory.BLOCKS,
                    1.2F,
                    0.9F + this.world.rand.nextFloat() * 0.15F
            );
        }

        if (victim != null) {
            victim.attackEntityFrom(DamageSource.GENERIC, OVERLOAD_DAMAGE);
            this.knockPlayerFail(victim);
        }

        this.setChangedAndSync();
    }

    @Nullable
    private EntityPlayer getTrackedPlayer() {
        if (this.world == null) {
            return null;
        }

        List<EntityPlayer> players = this.world.getEntitiesWithinAABB(EntityPlayer.class, this.getFrontZone());
        if (players.isEmpty()) {
            return null;
        }

        if (this.trackedPlayerId != null) {
            for (EntityPlayer p : players) {
                if (p.getUniqueID().equals(this.trackedPlayerId)) {
                    return p;
                }
            }
        }

        return players.get(0);
    }

    private boolean isPlayerBackAligned(EntityPlayer player) {
        if (player == null) {
            return false;
        }

        EnumFacing front = this.getSafeFacing();

        double requiredX = front.getXOffset();
        double requiredZ = front.getZOffset();

        float yaw = player.rotationYawHead;
        double rad = Math.toRadians(yaw);

        double lookX = -Math.sin(rad);
        double lookZ = Math.cos(rad);

        double dot = lookX * requiredX + lookZ * requiredZ;
        return dot >= BACK_ALIGNMENT_DOT_MIN;
    }

    private boolean isPlayerInValidPressDistance(EntityPlayer player) {
        if (player == null || this.pos == null) {
            return false;
        }

        AxisAlignedBB bb = player.getEntityBoundingBox();
        EnumFacing facing = this.getSafeFacing();

        double distanceFromFrontFace;

        switch (facing) {
            case NORTH:
                distanceFromFrontFace = this.pos.getZ() - bb.maxZ;
                break;
            case SOUTH:
                distanceFromFrontFace = bb.minZ - (this.pos.getZ() + 1.0D);
                break;
            case WEST:
                distanceFromFrontFace = this.pos.getX() - bb.maxX;
                break;
            case EAST:
            default:
                distanceFromFrontFace = bb.minX - (this.pos.getX() + 1.0D);
                break;
        }

        return distanceFromFrontFace >= PRESS_DISTANCE_MIN
                && distanceFromFrontFace <= PRESS_DISTANCE_MAX;
    }

    private EnumFacing getSafeFacing() {
        if (this.world == null || this.pos == null) {
            return EnumFacing.NORTH;
        }

        IBlockState state = this.world.getBlockState(this.pos);
        if (state == null) {
            return EnumFacing.NORTH;
        }

        Block block = state.getBlock();
        if (block instanceof BlockLifeCrusher && state.getProperties().containsKey(BlockLifeCrusher.FACING)) {
            return (EnumFacing) state.getValue((IProperty<?>) BlockLifeCrusher.FACING);
        }

        return EnumFacing.NORTH;
    }

    private AxisAlignedBB getFrontZone() {
        double x = this.pos.getX();
        double y = this.pos.getY();
        double z = this.pos.getZ();

        double start = 0.5D;
        double end = 1.5D;

        EnumFacing facing = this.getSafeFacing();
        switch (facing) {
            case NORTH:
                return new AxisAlignedBB(
                        x, y, z - end,
                        x + 1.0D, y + 2.0D, z - start
                );
            case SOUTH:
                return new AxisAlignedBB(
                        x, y, z + 1.0D + start,
                        x + 1.0D, y + 2.0D, z + 1.0D + end
                );
            case WEST:
                return new AxisAlignedBB(
                        x - end, y, z,
                        x - start, y + 2.0D, z + 1.0D
                );
            case EAST:
            default:
                return new AxisAlignedBB(
                        x + 1.0D + start, y, z,
                        x + 1.0D + end, y + 2.0D, z + 1.0D
                );
        }
    }

    private void knockPlayer(EntityPlayer player) {
        EnumFacing facing = this.getSafeFacing();
        double force = 0.1D;
        player.motionX += facing.getXOffset() * force;
        player.motionZ += facing.getZOffset() * force;
        player.velocityChanged = true;
    }

    private void setChangedAndSync() {
        if (this.world != null && !this.world.isRemote) {
            this.markDirty();
            IBlockState state = this.world.getBlockState(this.pos);
            this.world.notifyBlockUpdate(this.pos, state, state, 3);
        }
    }

    private boolean canExposeEnergyTo(@Nullable EnumFacing facing) {
        if (facing == null) {
            return true;
        }

        if (facing == EnumFacing.DOWN) {
            return true;
        }

        if (this.world == null || this.pos == null) {
            return false;
        }

        IBlockState state = this.world.getBlockState(this.pos);
        if (!(state.getBlock() instanceof BlockLifeCrusher)) {
            return false;
        }

        EnumFacing front = state.getValue(BlockLifeCrusher.FACING);
        if (front == null) {
            return false;
        }

        return facing == front.getOpposite();
    }

    private boolean pushEnergyOut() {
        if (this.world == null || this.world.isRemote) {
            return false;
        }

        if (this.energy <= 0) {
            return false;
        }

        boolean changed = false;

        changed |= this.pushEnergyToSide(EnumFacing.DOWN, OUTPUT_PER_TICK);

        EnumFacing back = this.getSafeFacing().getOpposite();
        changed |= this.pushEnergyToSide(back, OUTPUT_PER_TICK);

        return changed;
    }

    private boolean pushEnergyToSide(EnumFacing side, int maxSend) {
        if (side == null || maxSend <= 0 || this.energy <= 0) {
            return false;
        }

        BlockPos targetPos = this.pos.offset(side);
        TileEntity target = this.world.getTileEntity(targetPos);
        if (target == null) {
            return false;
        }

        EnumFacing targetSide = side.getOpposite();

        if (!target.hasCapability(CapabilityEnergy.ENERGY, targetSide)) {
            return false;
        }

        IEnergyStorage targetEnergy = target.getCapability(CapabilityEnergy.ENERGY, targetSide);
        if (targetEnergy == null || !targetEnergy.canReceive()) {
            return false;
        }

        int toSend = Math.min(maxSend, this.energy);
        if (toSend <= 0) {
            return false;
        }

        int acceptedSim = targetEnergy.receiveEnergy(toSend, true);
        if (acceptedSim <= 0) {
            return false;
        }

        int extracted = this.extractEnergy(acceptedSim, false);
        if (extracted <= 0) {
            return false;
        }

        int accepted = targetEnergy.receiveEnergy(extracted, false);

        if (accepted < extracted) {
            this.energy = Math.min(MAX_ENERGY, this.energy + (extracted - accepted));
        }

        return accepted > 0;
    }

    private void spawnAssemblySteamBurst() {
        if (!(this.world instanceof WorldServer) || this.pos == null) {
            return;
        }

        WorldServer ws = (WorldServer) this.world;

        double cx = this.pos.getX() + 0.5D;
        double cy = this.pos.getY() + 0.9D;
        double cz = this.pos.getZ() + 0.5D;

        ws.spawnParticle(
                EnumParticleTypes.CLOUD,
                cx, cy + 0.5D, cz,
                20,
                0.35D, 0.25D, 0.35D,
                0.04D
        );

        ws.spawnParticle(
                EnumParticleTypes.SMOKE_LARGE,
                cx, cy + 0.6D, cz,
                12,
                0.28D, 0.2D, 0.28D,
                0.025D
        );

        for (int i = 0; i < 4; i++) {
            double ox = (this.world.rand.nextDouble() - 0.5D) * 0.8D;
            double oz = (this.world.rand.nextDouble() - 0.5D) * 0.8D;

            ws.spawnParticle(
                    EnumParticleTypes.CLOUD,
                    cx + ox,
                    cy + 0.4D,
                    cz + oz,
                    6,
                    0.1D, 0.1D, 0.1D,
                    0.03D
            );
        }

        ws.spawnParticle(
                EnumParticleTypes.EXPLOSION_NORMAL,
                cx, cy + 0.4D, cz,
                2,
                0.2D, 0.1D, 0.2D,
                0.01D
        );
    }

    private void spawnFailParticles() {
        if (this.world == null || !this.world.isRemote || this.pos == null) {
            return;
        }

        double cx = this.pos.getX() + 0.5D;
        double cy = this.pos.getY() + 0.8D;
        double cz = this.pos.getZ() + 0.5D;

        this.world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, cx, cy + 0.4D, cz, 0.0D, 0.0D, 0.0D);

        for (int i = 0; i < 24; i++) {
            double px = cx + (this.world.rand.nextDouble() - 0.5D) * 0.9D;
            double py = cy + this.world.rand.nextDouble() * 1.2D;
            double pz = cz + (this.world.rand.nextDouble() - 0.5D) * 0.9D;

            double mx = (this.world.rand.nextDouble() - 0.5D) * 0.18D;
            double my = 0.05D + this.world.rand.nextDouble() * 0.12D;
            double mz = (this.world.rand.nextDouble() - 0.5D) * 0.18D;

            this.world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, px, py, pz, mx, my, mz);
        }

        for (int i = 0; i < 18; i++) {
            double px = cx + (this.world.rand.nextDouble() - 0.5D) * 0.8D;
            double py = cy + this.world.rand.nextDouble() * 1.0D;
            double pz = cz + (this.world.rand.nextDouble() - 0.5D) * 0.8D;

            double mx = (this.world.rand.nextDouble() - 0.5D) * 0.22D;
            double my = 0.02D + this.world.rand.nextDouble() * 0.10D;
            double mz = (this.world.rand.nextDouble() - 0.5D) * 0.22D;

            this.world.spawnParticle(EnumParticleTypes.FLAME, px, py, pz, mx, my, mz);
        }

        for (int i = 0; i < 10; i++) {
            double px = cx + (this.world.rand.nextDouble() - 0.5D) * 1.0D;
            double py = cy + this.world.rand.nextDouble() * 1.1D;
            double pz = cz + (this.world.rand.nextDouble() - 0.5D) * 1.0D;

            double mx = (this.world.rand.nextDouble() - 0.5D) * 0.30D;
            double my = this.world.rand.nextDouble() * 0.18D;
            double mz = (this.world.rand.nextDouble() - 0.5D) * 0.30D;

            this.world.spawnParticle(EnumParticleTypes.CRIT, px, py, pz, mx, my, mz);
        }
    }

    private <E extends IAnimatable> PlayState animationPredicate(AnimationEvent<E> event) {
        AnimationController<?> controller = event.getController();
        if (!this.isAssembled()) {
            this.lastAnimationKey = "idle";
            controller.setAnimationSpeed(0.0D);
            return PlayState.STOP;
        }

        String animationKey;
        boolean loop;
        double speed;

        if (this.isKickAnimating()) {
            animationKey = "kick";
            loop = false;
            speed = 1.0D;
        } else if (this.state == CrusherState.FAIL) {
            animationKey = "crush";
            loop = false;
            speed = 1.0D;
        } else if (this.state == CrusherState.SPINUP
                || this.state == CrusherState.WORK
                || this.state == CrusherState.SPINDOWN) {
            if (this.workSpeed > 0.0F) {
                animationKey = "work";
                loop = true;
                speed = Math.max(MIN_VISIBLE_SPEED, this.workSpeed);

                if (this.isBerserk()) {
                    speed *= BERSERK_ANIM_MULT;
                }
            } else {
                animationKey = "idle";
                loop = false;
                speed = 0.0D;
            }
        } else {
            animationKey = "idle";
            loop = false;
            speed = 0.0D;
        }

        if (!animationKey.equals(this.lastAnimationKey)) {
            controller.markNeedsReload();
            this.lastAnimationKey = animationKey;
        }

        if ("idle".equals(animationKey)) {
            controller.setAnimationSpeed(0.0D);
            return PlayState.STOP;
        }

        controller.setAnimation(new AnimationBuilder().addAnimation(animationKey, loop));
        controller.setAnimationSpeed(speed);
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(
                new AnimationController<TileLifeCrusher>(this, "controller", 0.0f, this::animationPredicate)
        );
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("Energy", this.energy);
        tag.setFloat("WorkSpeed", this.workSpeed);
        tag.setInteger("ShiftProgress", this.shiftProgress);
        tag.setString("State", this.state.name());
        tag.setLong("LastShiftTick", this.lastShiftTick);
        tag.setLong("FailUntilTick", this.failUntilTick);
        tag.setLong("KickAnimUntilTick", this.kickAnimUntilTick);

        if (this.trackedPlayerId != null) {
            tag.setString("TrackedPlayer", this.trackedPlayerId.toString());
        }

        tag.setBoolean("TrackedPrevSneak", this.trackedPrevSneak);
        tag.setInteger("RapidShiftCombo", this.rapidShiftCombo);
        tag.setLong("LastRapidShiftTick", this.lastRapidShiftTick);
        tag.setLong("BerserkUntilTick", this.berserkUntilTick);
        tag.setInteger("WiresInstalled", this.wiresInstalled);
        tag.setBoolean("RodInstalled", this.rodInstalled);
        tag.setInteger("AssemblySteamTicks", this.assemblySteamTicks);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.energy = Math.max(0, Math.min(MAX_ENERGY, tag.getInteger("Energy")));
        this.workSpeed = tag.getFloat("WorkSpeed");
        this.shiftProgress = tag.getInteger("ShiftProgress");

        try {
            this.state = CrusherState.valueOf(tag.getString("State"));
        } catch (Exception e) {
            this.state = CrusherState.IDLE;
        }

        this.lastShiftTick = tag.getLong("LastShiftTick");
        this.failUntilTick = tag.getLong("FailUntilTick");
        this.kickAnimUntilTick = tag.getLong("KickAnimUntilTick");

        String tracked = tag.getString("TrackedPlayer");
        if (tracked != null && !tracked.isEmpty()) {
            try {
                this.trackedPlayerId = UUID.fromString(tracked);
            } catch (Exception e) {
                this.trackedPlayerId = null;
            }
        } else {
            this.trackedPlayerId = null;
        }

        this.trackedPrevSneak = tag.getBoolean("TrackedPrevSneak");
        this.rapidShiftCombo = tag.getInteger("RapidShiftCombo");
        this.lastRapidShiftTick = tag.getLong("LastRapidShiftTick");
        this.berserkUntilTick = tag.getLong("BerserkUntilTick");
        this.wiresInstalled = Math.max(0, Math.min(REQUIRED_WIRES, tag.getInteger("WiresInstalled")));
        this.rodInstalled = tag.getBoolean("RodInstalled");
        this.assemblySteamTicks = tag.getInteger("AssemblySteamTicks");

        if (this.state != CrusherState.FAIL) {
            this.failParticlesPlayed = false;
        }
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return this.writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 0, this.getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        if (pkt != null) {
            this.handleUpdateTag(pkt.getNbtCompound());
        }

        if (this.world != null && this.pos != null) {
            IBlockState state = this.world.getBlockState(this.pos);
            this.world.markBlockRangeForRenderUpdate(this.pos, this.pos);
            this.world.notifyBlockUpdate(this.pos, state, state, 3);
        }
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        CrusherState oldState = this.state;
        this.readFromNBT(tag);

        if (this.world != null && this.world.isRemote) {
            if (this.state == CrusherState.FAIL && oldState != CrusherState.FAIL && !this.failParticlesPlayed) {
                this.spawnFailParticles();
                this.failParticlesPlayed = true;
            } else if (this.state != CrusherState.FAIL) {
                this.failParticlesPlayed = false;
            }
        }
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return this.canExposeEnergyTo(facing);
        }
        return super.hasCapability(capability, facing);
    }

    @Override
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            if (this.canExposeEnergyTo(facing)) {
                return CapabilityEnergy.ENERGY.cast(this.outputWrapper);
            }
            return null;
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return 0;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (maxExtract <= 0) {
            return 0;
        }

        int extracted = Math.min(maxExtract, this.energy);
        if (!simulate && extracted > 0) {
            this.energy -= extracted;
            this.setChangedAndSync();
        }
        return extracted;
    }

    @Override
    public int getEnergyStored() {
        return this.energy;
    }

    @Override
    public int getMaxEnergyStored() {
        return MAX_ENERGY;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return false;
    }

    private void knockPlayerFail(EntityPlayer player) {
        if (player == null) {
            return;
        }

        EnumFacing facing = this.getSafeFacing();

        double forceX = facing.getXOffset() * FAIL_KNOCKBACK_FORCE;
        double forceZ = facing.getZOffset() * FAIL_KNOCKBACK_FORCE;

        player.motionX = forceX;
        player.motionZ = forceZ;
        player.motionY = 0.38D;
        player.velocityChanged = true;
    }
}