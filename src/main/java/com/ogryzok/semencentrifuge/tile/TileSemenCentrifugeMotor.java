package com.ogryzok.semencentrifuge.tile;

import com.ogryzok.chair.entity.EntitySeat;
import com.ogryzok.semencentrifuge.SemenCentrifugeRegistry;
import com.ogryzok.semencentrifuge.block.BlockSemenCentrifugeBase;
import com.ogryzok.semencentrifuge.block.BlockSemenCentrifugeMotor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import javax.annotation.Nullable;
import java.util.List;

public class TileSemenCentrifugeMotor extends TileEntity implements ITickable, IAnimatable {
    private static final int START_ANIMATION_TICKS = 50; // 2.5s for centri_start

    private final AnimationFactory factory = new AnimationFactory(this);
    private boolean assembled = false;
    private boolean chairOccupied = false;
    private boolean openingSequence = false;
    private int openingTicksRemaining = 0;
    private boolean openedVisual = false;
    private String lastAnimationKey = "idle";

    @Override
    public void update() {
        if (world == null) {
            return;
        }

        if (!world.isRemote) {
            if (world.getTotalWorldTime() % 10L == 0L) {
                syncStructureState();
            }
            tickMotorVisualState();
        }
    }

    private void tickMotorVisualState() {
        TileSemenCentrifugeBase base = getBaseTile();
        boolean occupiedNow = isChairOccupied(base);
        boolean workingNow = isWorking(base);
        boolean changed = false;

        if (workingNow) {
            if (this.chairOccupied != occupiedNow) {
                this.chairOccupied = occupiedNow;
                changed = true;
            }
            if (this.openingSequence || this.openingTicksRemaining != 0 || this.openedVisual) {
                this.openingSequence = false;
                this.openingTicksRemaining = 0;
                this.openedVisual = false;
                changed = true;
            }
        } else if (occupiedNow) {
            if (!this.chairOccupied) {
                this.chairOccupied = true;
                this.openingSequence = true;
                this.openingTicksRemaining = START_ANIMATION_TICKS;
                this.openedVisual = false;
                changed = true;
            } else if (this.openingSequence) {
                if (this.openingTicksRemaining > 0) {
                    this.openingTicksRemaining--;
                    if (this.openingTicksRemaining <= 0) {
                        this.openingSequence = false;
                        this.openedVisual = true;
                        changed = true;
                    }
                }
            }
        } else {
            if (this.chairOccupied || this.openingSequence || this.openingTicksRemaining != 0 || this.openedVisual) {
                this.chairOccupied = false;
                this.openingSequence = false;
                this.openingTicksRemaining = 0;
                this.openedVisual = false;
                changed = true;
            }
        }

        if (changed) {
            markDirty();
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
    }

    private boolean isWorking(@Nullable TileSemenCentrifugeBase base) {
        return base != null && base.isSessionActive();
    }

    private boolean isChairOccupied(@Nullable TileSemenCentrifugeBase base) {
        if (world == null || base == null) {
            return false;
        }

        BlockPos chairPos = base.getChairPos();
        List<Entity> passengers = world.getEntitiesWithinAABB(Entity.class,
                new net.minecraft.util.math.AxisAlignedBB(chairPos));

        for (Entity entity : passengers) {
            if (entity instanceof EntitySeat) {
                EntitySeat seat = (EntitySeat) entity;
                if (chairPos.equals(seat.getSeatPos()) && seat.isBeingRidden()) {
                    return true;
                }
            }
        }

        return false;
    }

    public void syncStructureState() {
        boolean newValue = checkAssembled();
        if (newValue != this.assembled) {
            this.assembled = newValue;
            markDirty();
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
    }

    public boolean isAssembled() {
        return checkAssembled();
    }

    public boolean isOpenedVisual() {
        return this.openedVisual;
    }

    public boolean isOpeningSequence() {
        return this.openingSequence;
    }

    public boolean shouldUseOpenedModel() {
        TileSemenCentrifugeBase base = getBaseTile();
        return !isWorking(base) && this.openedVisual;
    }

    private boolean checkAssembled() {
        if (world == null || pos == null) {
            return false;
        }

        IBlockState self = world.getBlockState(pos);
        if (self.getBlock() != SemenCentrifugeRegistry.SEMEN_CENTRIFUGE_MOTOR) {
            return false;
        }

        IBlockState below = world.getBlockState(pos.down());
        if (below.getBlock() != SemenCentrifugeRegistry.SEMEN_CENTRIFUGE_BASE) {
            return false;
        }

        return self.getValue(BlockSemenCentrifugeMotor.FACING) == below.getValue(BlockSemenCentrifugeBase.FACING);
    }

    @Nullable
    public TileSemenCentrifugeBase getBaseTile() {
        if (world == null || pos == null) {
            return null;
        }
        TileEntity te = world.getTileEntity(new BlockPos(pos.getX(), pos.getY() - 1, pos.getZ()));
        return te instanceof TileSemenCentrifugeBase ? (TileSemenCentrifugeBase) te : null;
    }

    private double getWorkAnimationSpeed(@Nullable TileSemenCentrifugeBase base) {
        if (base == null) {
            return 1.0D;
        }

        switch (Math.max(1, Math.min(5, base.getCurrentRound()))) {
            case 1:
                return 0.75D;
            case 2:
                return 1.0D;
            case 3:
                return 1.25D;
            case 4:
                return 1.5D;
            default:
                return 1.75D;
        }
    }

    private <E extends IAnimatable> PlayState animationPredicate(AnimationEvent<E> event) {
        AnimationController<?> controller = event.getController();

        if (!this.isAssembled()) {
            this.lastAnimationKey = "idle";
            controller.setAnimationSpeed(0.0D);
            return PlayState.STOP;
        }

        TileSemenCentrifugeBase base = getBaseTile();
        String animationKey = "idle";
        boolean loop = false;
        double speed = 0.0D;

        if (isWorking(base)) {
            animationKey = "centri_work";
            loop = true;
            speed = getWorkAnimationSpeed(base);
        } else if (this.openingSequence) {
            animationKey = "centri_start";
            loop = false;
            speed = 1.0D;
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
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setBoolean("Assembled", this.assembled);
        compound.setBoolean("ChairOccupied", this.chairOccupied);
        compound.setBoolean("OpeningSequence", this.openingSequence);
        compound.setInteger("OpeningTicksRemaining", this.openingTicksRemaining);
        compound.setBoolean("OpenedVisual", this.openedVisual);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.assembled = compound.getBoolean("Assembled");
        this.chairOccupied = compound.getBoolean("ChairOccupied");
        this.openingSequence = compound.getBoolean("OpeningSequence");
        this.openingTicksRemaining = compound.getInteger("OpeningTicksRemaining");
        this.openedVisual = compound.getBoolean("OpenedVisual");
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
    public void registerControllers(AnimationData animationData) {
        animationData.addAnimationController(
                new AnimationController<TileSemenCentrifugeMotor>(this, "controller", 0.0f, this::animationPredicate)
        );
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }
}
