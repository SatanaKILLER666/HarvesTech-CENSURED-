package com.ogryzok.player.semen.packet;

import com.ogryzok.player.semen.ISemenStorage;
import com.ogryzok.player.semen.SemenProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketSyncSemenHandler implements IMessageHandler<PacketSyncSemen, IMessage> {

    @Override
    public IMessage onMessage(PacketSyncSemen message, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            Minecraft mc = Minecraft.getMinecraft();
            EntityPlayer player = mc.player;
            if (player == null || mc.world == null) return;

            Entity entity = mc.world.getEntityByID(message.getEntityId());
            if (!(entity instanceof EntityPlayer)) return;

            ISemenStorage storage = ((EntityPlayer) entity).getCapability(SemenProvider.SEMEN_CAP, null);
            if (storage == null) return;

            storage.setAmount(message.getAmount());
            storage.setTickCounter(message.getTickCounter());
            storage.setManualHarvesting(message.isManualHarvesting());
            storage.setManualHarvestTicks(message.getManualHarvestTicks());
            storage.setAbstinenceTicks(message.getAbstinenceTicks());
            storage.setAbstinenceStage(message.getAbstinenceStage());
            storage.setSeedKeeper(message.isSeedKeeper());
        });

        return null;
    }
}
