package com.ogryzok.manualharvest.sound;

import com.ogryzok.harvestech;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = harvestech.MODID)
public final class ManualHarvestSoundRegistry {
    public static SoundEvent MANUAL_HARVEST;
    public static SoundEvent USUAL_LUCH;
    public static SoundEvent MEGA_LUCH;

    private ManualHarvestSoundRegistry() {
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        MANUAL_HARVEST = register(event, "manual_harvest");
        USUAL_LUCH = register(event, "usual_luch");
        MEGA_LUCH = register(event, "mega_luch");
    }

    private static SoundEvent register(RegistryEvent.Register<SoundEvent> event, String name) {
        ResourceLocation id = new ResourceLocation(harvestech.MODID, name);
        SoundEvent sound = new SoundEvent(id);
        sound.setRegistryName(id);
        event.getRegistry().register(sound);
        return sound;
    }
}
