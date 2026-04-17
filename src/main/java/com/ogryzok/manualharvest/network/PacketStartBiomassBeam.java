package com.ogryzok.manualharvest.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class PacketStartBiomassBeam implements IMessage {
    public double fromX;
    public double fromY;
    public double fromZ;
    public double toX;
    public double toY;
    public double toZ;
    public int duration;
    public double width;
    public boolean keeperStyle;
    public boolean whiteCore;

    public PacketStartBiomassBeam() {
    }

    public PacketStartBiomassBeam(Vec3d from, Vec3d to, int duration, double width, boolean keeperStyle, boolean whiteCore) {
        this.fromX = from.x;
        this.fromY = from.y;
        this.fromZ = from.z;
        this.toX = to.x;
        this.toY = to.y;
        this.toZ = to.z;
        this.duration = duration;
        this.width = width;
        this.keeperStyle = keeperStyle;
        this.whiteCore = whiteCore;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        fromX = buf.readDouble();
        fromY = buf.readDouble();
        fromZ = buf.readDouble();
        toX = buf.readDouble();
        toY = buf.readDouble();
        toZ = buf.readDouble();
        duration = buf.readInt();
        width = buf.readDouble();
        keeperStyle = buf.readBoolean();
        whiteCore = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(fromX);
        buf.writeDouble(fromY);
        buf.writeDouble(fromZ);
        buf.writeDouble(toX);
        buf.writeDouble(toY);
        buf.writeDouble(toZ);
        buf.writeInt(duration);
        buf.writeDouble(width);
        buf.writeBoolean(keeperStyle);
        buf.writeBoolean(whiteCore);
    }
}
