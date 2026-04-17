package com.ogryzok.player.semen.packet;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class PacketSyncSemen implements IMessage {

    private int entityId;
    private int amount;
    private int tickCounter;
    private boolean manualHarvesting;
    private int manualHarvestTicks;
    private int abstinenceTicks;
    private int abstinenceStage;
    private boolean seedKeeper;

    public PacketSyncSemen() {
    }

    public PacketSyncSemen(int entityId, int amount, int tickCounter) {
        this(entityId, amount, tickCounter, false, 0, 0, 0, false);
    }

    public PacketSyncSemen(int entityId, int amount, int tickCounter, boolean manualHarvesting, int manualHarvestTicks,
                           int abstinenceTicks, int abstinenceStage, boolean seedKeeper) {
        this.entityId = entityId;
        this.amount = amount;
        this.tickCounter = tickCounter;
        this.manualHarvesting = manualHarvesting;
        this.manualHarvestTicks = manualHarvestTicks;
        this.abstinenceTicks = abstinenceTicks;
        this.abstinenceStage = abstinenceStage;
        this.seedKeeper = seedKeeper;
    }

    public int getEntityId() {
        return entityId;
    }

    public int getAmount() {
        return amount;
    }

    public int getTickCounter() {
        return tickCounter;
    }

    public boolean isManualHarvesting() {
        return manualHarvesting;
    }

    public int getManualHarvestTicks() {
        return manualHarvestTicks;
    }

    public int getAbstinenceTicks() {
        return abstinenceTicks;
    }

    public int getAbstinenceStage() {
        return abstinenceStage;
    }

    public boolean isSeedKeeper() {
        return seedKeeper;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = buf.readInt();
        this.amount = buf.readInt();
        this.tickCounter = buf.readInt();
        this.manualHarvesting = buf.readBoolean();
        this.manualHarvestTicks = buf.readInt();
        this.abstinenceTicks = buf.readInt();
        this.abstinenceStage = buf.readInt();
        this.seedKeeper = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeInt(this.amount);
        buf.writeInt(this.tickCounter);
        buf.writeBoolean(this.manualHarvesting);
        buf.writeInt(this.manualHarvestTicks);
        buf.writeInt(this.abstinenceTicks);
        buf.writeInt(this.abstinenceStage);
        buf.writeBoolean(this.seedKeeper);
    }
}
