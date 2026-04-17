package com.ogryzok.manualharvest.network;

import com.ogryzok.manualharvest.ManualHarvestLogic;
import com.ogryzok.player.semen.ISemenStorage;
import com.ogryzok.player.semen.SemenProvider;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketStartManualHarvestHandler implements IMessageHandler<PacketStartManualHarvest, IMessage> {
    @Override
    public IMessage onMessage(PacketStartManualHarvest message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        player.getServerWorld().addScheduledTask(() -> {
            ISemenStorage storage = player.getCapability(SemenProvider.SEMEN_CAP, null);
            ManualHarvestLogic.tryStart(player, storage);
        });
        return null;
    }
}
