package com.ogryzok.semendestiller.sound;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = "harvestech")
public final class SemenDestillerSoundRegistry {
    public static SoundEvent DISTILLER_WORK;
    public static SoundEvent DISTILLER_STOP;
    public static SoundEvent DISTILLER_STEAM;

    private SemenDestillerSoundRegistry() {
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        DISTILLER_WORK = register(event, "distiller_work");
        DISTILLER_STOP = register(event, "distiller_stop");
        DISTILLER_STEAM = register(event, "distiller_steam");
    }

    private static SoundEvent register(RegistryEvent.Register<SoundEvent> event, String name) {
        ResourceLocation id = new ResourceLocation("harvestech", name);
        SoundEvent sound = new SoundEvent(id);
        sound.setRegistryName(id);
        event.getRegistry().register(sound);
        return sound;
    }
}
