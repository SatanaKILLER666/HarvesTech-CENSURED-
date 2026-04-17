package com.ogryzok.lifecrusher.sound;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = "harvestech")
public final class LifeCrusherSoundRegistry {
    public static SoundEvent WORK;
    public static SoundEvent BERSERK;
    public static SoundEvent STOP;
    public static SoundEvent CRUSH;
    public static SoundEvent HIT;
    public static SoundEvent CABLE;
    public static SoundEvent MACHINEDONE;

    private LifeCrusherSoundRegistry() {
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        WORK = register(event, "work");
        BERSERK = register(event, "berserk");
        STOP = register(event, "stop");
        CRUSH = register(event, "crush");
        HIT = register(event, "hit");
        CABLE = register(event,"cable");
        MACHINEDONE = register(event,"machinedone");
    }

    private static SoundEvent register(RegistryEvent.Register<SoundEvent> event, String name) {
        ResourceLocation id = new ResourceLocation("harvestech", name);
        SoundEvent sound = new SoundEvent(id);
        sound.setRegistryName(id);
        event.getRegistry().register(sound);
        return sound;
    }
}
