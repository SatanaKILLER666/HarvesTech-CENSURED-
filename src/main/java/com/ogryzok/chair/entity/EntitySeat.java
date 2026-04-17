package com.ogryzok.chair.entity;

import com.ogryzok.chair.ChairRegistry;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class EntitySeat extends Entity {
    private static final DataParameter<Integer> SEAT_X = EntityDataManager.createKey(EntitySeat.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> SEAT_Y = EntityDataManager.createKey(EntitySeat.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> SEAT_Z = EntityDataManager.createKey(EntitySeat.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> FACING = EntityDataManager.createKey(EntitySeat.class, DataSerializers.VARINT);

    private static final float MAX_HEAD_YAW_OFFSET = 40.0F;

    public EntitySeat(World worldIn) {
        super(worldIn);
        setSize(0.01F, 0.01F);
        noClip = true;
        isImmuneToFire = true;
    }

    public EntitySeat(World worldIn, BlockPos pos, EnumFacing facing) {
        this(worldIn);
        setSeatPos(pos, facing);
    }

    public void setSeatPos(BlockPos pos, EnumFacing facing) {
        dataManager.set(SEAT_X, pos.getX());
        dataManager.set(SEAT_Y, pos.getY());
        dataManager.set(SEAT_Z, pos.getZ());
        dataManager.set(FACING, facing.getHorizontalIndex());
        updateActualPosition();
    }

    public BlockPos getSeatPos() {
        return new BlockPos(dataManager.get(SEAT_X), dataManager.get(SEAT_Y), dataManager.get(SEAT_Z));
    }

    public EnumFacing getSeatFacing() {
        return EnumFacing.byHorizontalIndex(dataManager.get(FACING) & 3);
    }

    private float getSeatYaw() {
        EnumFacing facing = getSeatFacing();
        switch (facing) {
            case NORTH:
                return 180.0F;
            case SOUTH:
                return 0.0F;
            case WEST:
                return 90.0F;
            case EAST:
                return -90.0F;
            default:
                return -90.0F;
        }
    }

    private float wrapDegrees(float value) {
        value = value % 360.0F;

        if (value >= 180.0F) {
            value -= 360.0F;
        }

        if (value < -180.0F) {
            value += 360.0F;
        }

        return value;
    }

    private float clampHeadYaw(float headYaw, float centerYaw, float maxOffset) {
        float delta = wrapDegrees(headYaw - centerYaw);

        if (delta > maxOffset) {
            delta = maxOffset;
        }

        if (delta < -maxOffset) {
            delta = -maxOffset;
        }

        return centerYaw + delta;
    }

    private void updateActualPosition() {
        BlockPos pos = getSeatPos();
        EnumFacing facing = getSeatFacing();

        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.50D;
        double z = pos.getZ() + 0.5D;

        switch (facing) {
            case NORTH:
                z += 0.10D;
                break;
            case SOUTH:
                z -= 0.10D;
                break;
            case WEST:
                x -= 0.08D;
                break;
            case EAST:
                x += 0.08D;
                break;
            default:
                break;
        }

        setPosition(x, y, z);
        motionX = 0.0D;
        motionY = 0.0D;
        motionZ = 0.0D;
    }

    private void applySeatTransform(Entity passenger) {
        passenger.motionX = 0.0D;
        passenger.motionY = 0.0D;
        passenger.motionZ = 0.0D;
        passenger.fallDistance = 0.0F;
        passenger.setPosition(posX, posY, posZ);

        if (passenger instanceof EntityLivingBase) {
            EntityLivingBase living = (EntityLivingBase) passenger;
            float seatYaw = getSeatYaw();

            living.renderYawOffset = seatYaw;
            living.prevRenderYawOffset = seatYaw;
            living.rotationYaw = seatYaw;
            living.prevRotationYaw = seatYaw;

            float clampedHeadYaw = clampHeadYaw(living.rotationYawHead, seatYaw, MAX_HEAD_YAW_OFFSET);
            living.rotationYawHead = clampedHeadYaw;
            living.prevRotationYawHead = clampedHeadYaw;
        }
    }

    @Override
    protected void entityInit() {
        dataManager.register(SEAT_X, 0);
        dataManager.register(SEAT_Y, 0);
        dataManager.register(SEAT_Z, 0);
        dataManager.register(FACING, 0);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        updateActualPosition();

        if (world.isRemote) {
            return;
        }

        BlockPos pos = getSeatPos();
        if (!world.isBlockLoaded(pos)) {
            setDead();
            return;
        }

        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() != ChairRegistry.CHAIR) {
            setDead();
            return;
        }

        if (getPassengers().isEmpty()) {
            setDead();
            return;
        }

        Entity passenger = getPassengers().get(0);
        if (passenger == null || passenger.isDead) {
            setDead();
            return;
        }

        if (passenger instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) passenger;

            if (player.isSneaking()) {
                player.dismountRidingEntity();
                player.setSneaking(false);
                player.setPositionAndUpdate(player.posX, player.posY, player.posZ);
                setDead();
                return;
            }
        }

        applySeatTransform(passenger);
    }

    @Override
    public void updatePassenger(Entity passenger) {
        if (!isPassenger(passenger)) {
            return;
        }

        applySeatTransform(passenger);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        BlockPos pos = new BlockPos(
                compound.getInteger("SeatX"),
                compound.getInteger("SeatY"),
                compound.getInteger("SeatZ")
        );
        EnumFacing facing = EnumFacing.byHorizontalIndex(compound.getInteger("SeatFacing") & 3);
        setSeatPos(pos, facing);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        BlockPos pos = getSeatPos();
        compound.setInteger("SeatX", pos.getX());
        compound.setInteger("SeatY", pos.getY());
        compound.setInteger("SeatZ", pos.getZ());
        compound.setInteger("SeatFacing", getSeatFacing().getHorizontalIndex());
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public void applyEntityCollision(Entity entityIn) {
    }

    @Override
    public void move(MoverType type, double x, double y, double z) {
    }

    @Override
    public double getMountedYOffset() {
        return 0.0D;
    }

    @Override
    public boolean shouldRiderSit() {
        return true;
    }

    @Override
    protected boolean canFitPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    public AxisAlignedBB getEntityBoundingBox() {
        return new AxisAlignedBB(
                posX - 0.01D, posY - 0.01D, posZ - 0.01D,
                posX + 0.01D, posY + 0.01D, posZ + 0.01D
        );
    }

    @Override
    public void removePassenger(Entity passenger) {
        super.removePassenger(passenger);

        if (!world.isRemote && passenger instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) passenger;
            player.setSneaking(false);
        }

        if (!world.isRemote && getPassengers().isEmpty()) {
            setDead();
        }
    }
}