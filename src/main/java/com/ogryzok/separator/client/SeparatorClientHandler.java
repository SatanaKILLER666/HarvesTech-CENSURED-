package com.ogryzok.separator.client;

import com.ogryzok.separator.client.sound.SeparatorSoundController;
import com.ogryzok.separator.tile.TileSeparator;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class SeparatorClientHandler {
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        SeparatorSoundController.onClientTick();

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null || mc.currentScreen != null) return;

        RayTraceResult hit = mc.objectMouseOver;
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK || hit.getBlockPos() == null) return;

        TileEntity te = mc.world.getTileEntity(hit.getBlockPos());
        if (te instanceof TileSeparator && !((TileSeparator) te).isAssembled()) {
            mc.player.sendStatusMessage(new TextComponentTranslation("tooltip.harvestech.separator.missing_blade"), true);
        }
    }
}
