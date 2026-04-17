package com.ogryzok.semencentrifuge.network.packet;

import com.ogryzok.semencentrifuge.tile.TileSemenCentrifugeBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketCentrifugeFinishHandler implements IMessageHandler<PacketCentrifugeFinish, IMessage> {
    @Override
    public IMessage onMessage(PacketCentrifugeFinish message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        player.getServerWorld().addScheduledTask(() -> {
            TileEntity te = player.world.getTileEntity(message.getPos());
            if (te instanceof TileSemenCentrifugeBase) {
                ((TileSemenCentrifugeBase) te).onFinishKey(player);
            }
        });
        return null;
    }
}
