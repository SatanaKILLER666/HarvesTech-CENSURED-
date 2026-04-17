package com.ogryzok.semencentrifuge.client.sound;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = "harvestech", value = Side.CLIENT)
public final class SemenCentrifugeClientSoundEvents {
    private SemenCentrifugeClientSoundEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            SemenCentrifugeSoundController.onClientTick();
        }
    }
}
