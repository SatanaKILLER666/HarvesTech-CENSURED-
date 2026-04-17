package com.ogryzok.semenenrichment.sound;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = "harvestech")
public final class SemenEnrichmentSoundRegistry {
    public static SoundEvent ENRICH_CHAMBER_WORK;
    public static SoundEvent ENRICH_CHAMBER_STOP;

    private SemenEnrichmentSoundRegistry() {
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        ENRICH_CHAMBER_WORK = register(event, "enrich_chamber_work");
        ENRICH_CHAMBER_STOP = register(event, "enrich_chamber_stop");
    }

    private static SoundEvent register(RegistryEvent.Register<SoundEvent> event, String name) {
        ResourceLocation id = new ResourceLocation("harvestech", name);
        SoundEvent sound = new SoundEvent(id);
        sound.setRegistryName(id);
        event.getRegistry().register(sound);
        return sound;
    }
}
