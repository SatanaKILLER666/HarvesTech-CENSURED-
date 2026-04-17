package com.ogryzok.semencentrifuge.sound;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = "harvestech")
public final class SemenCentrifugeSoundRegistry {
    public static SoundEvent CENTRIFUGE_1SPEED;
    public static SoundEvent CENTRIFUGE_2SPEED;
    public static SoundEvent CENTRIFUGE_3SPEED;
    public static SoundEvent CENTRIFUGE_4SPEED;
    public static SoundEvent CENTRIFUGE_5SPEED;
    public static SoundEvent CENTRIFUGE_STOP;

    private SemenCentrifugeSoundRegistry() {
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        CENTRIFUGE_1SPEED = register(event, "centrifuge_1speed");
        CENTRIFUGE_2SPEED = register(event, "centrifuge_2speed");
        CENTRIFUGE_3SPEED = register(event, "centrifuge_3speed");
        CENTRIFUGE_4SPEED = register(event, "centrifuge_4speed");
        CENTRIFUGE_5SPEED = register(event, "centrifuge_5speed");
        CENTRIFUGE_STOP = register(event, "centrifuge_stop");
    }

    private static SoundEvent register(RegistryEvent.Register<SoundEvent> event, String name) {
        ResourceLocation id = new ResourceLocation("harvestech", name);
        SoundEvent sound = new SoundEvent(id);
        sound.setRegistryName(id);
        event.getRegistry().register(sound);
        return sound;
    }
}
