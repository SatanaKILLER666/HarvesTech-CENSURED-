package com.ogryzok.disease.client;

import com.ogryzok.disease.DiseaseRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;

public class MalePowerClientHandler {
    private static final double RED = 0.96D;
    private static final double GREEN = 0.91D;
    private static final double BLUE = 0.82D;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.isGamePaused()) {
            return;
        }

        List<EntityPlayer> players = mc.world.playerEntities;
        for (EntityPlayer player : players) {
            if (player == null || !player.isPotionActive(DiseaseRegistry.MALE_POWER)) {
                continue;
            }

            if ((player.ticksExisted & 1) != 0) {
                continue;
            }

            for (int i = 0; i < 2; i++) {
                double x = player.posX + (mc.world.rand.nextDouble() - 0.5D) * 0.8D;
                double y = player.posY + 0.2D + mc.world.rand.nextDouble() * Math.max(0.4D, player.height * 0.9D);
                double z = player.posZ + (mc.world.rand.nextDouble() - 0.5D) * 0.8D;
                mc.world.spawnParticle(EnumParticleTypes.SPELL_MOB_AMBIENT, x, y, z, RED, GREEN, BLUE);
            }
        }
    }
}
