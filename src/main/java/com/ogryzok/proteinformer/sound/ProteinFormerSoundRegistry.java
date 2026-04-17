package com.ogryzok.proteinformer.sound;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = "harvestech")
public final class ProteinFormerSoundRegistry {
    public static SoundEvent PROTEIN_FORMER;

    private ProteinFormerSoundRegistry() {
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        PROTEIN_FORMER = register(event, "protein_former");
    }

    private static SoundEvent register(RegistryEvent.Register<SoundEvent> event, String name) {
        ResourceLocation id = new ResourceLocation("harvestech", name);
        SoundEvent sound = new SoundEvent(id);
        sound.setRegistryName(id);
        event.getRegistry().register(sound);
        return sound;
    }
}
