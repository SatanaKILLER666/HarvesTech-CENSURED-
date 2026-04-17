package com.ogryzok.manualharvest.network;

import com.ogryzok.manualharvest.client.BiomassBeamEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketStartBiomassBeamHandler implements IMessageHandler<PacketStartBiomassBeam, IMessage> {
    @Override
    public IMessage onMessage(PacketStartBiomassBeam message, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> BiomassBeamEffect.start(
                new Vec3d(message.fromX, message.fromY, message.fromZ),
                new Vec3d(message.toX, message.toY, message.toZ),
                message.duration,
                message.width,
                message.keeperStyle,
                message.whiteCore
        ));
        return null;
    }
}
