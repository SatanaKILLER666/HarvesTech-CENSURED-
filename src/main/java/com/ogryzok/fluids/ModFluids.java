package com.ogryzok.fluids;

import com.ogryzok.harvestech;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

public final class ModFluids {
    public static final ResourceLocation BIOMASS_STILL = new ResourceLocation(harvestech.MODID, "blocks/biomass_still");
    public static final ResourceLocation BIOMASS_FLOW = new ResourceLocation(harvestech.MODID, "blocks/biomass_flow");
    public static final ResourceLocation DISTILLED_BIOMASS_STILL = new ResourceLocation(harvestech.MODID, "blocks/distilled_biomass_still");
    public static final ResourceLocation DISTILLED_BIOMASS_FLOW = new ResourceLocation(harvestech.MODID, "blocks/distilled_biomass_flow");
    public static final ResourceLocation ENRICHED_BIOMASS_STILL = new ResourceLocation(harvestech.MODID, "blocks/enriched_biomass_still");
    public static final ResourceLocation ENRICHED_BIOMASS_FLOW = new ResourceLocation(harvestech.MODID, "blocks/enriched_biomass_flow");
    public static final ResourceLocation FERMENTED_SEMEN_STILL = new ResourceLocation(harvestech.MODID, "blocks/biomass_still");
    public static final ResourceLocation FERMENTED_SEMEN_FLOW = new ResourceLocation(harvestech.MODID, "blocks/biomass_flow");
    public static final ResourceLocation DISTILLED_FERMENTED_SEMEN_STILL = new ResourceLocation(harvestech.MODID, "blocks/distilled_biomass_still");
    public static final ResourceLocation DISTILLED_FERMENTED_SEMEN_FLOW = new ResourceLocation(harvestech.MODID, "blocks/distilled_biomass_flow");
    public static final ResourceLocation TOXIC_FLESH_STILL = new ResourceLocation(harvestech.MODID, "blocks/biomass_still");
    public static final ResourceLocation TOXIC_FLESH_FLOW = new ResourceLocation(harvestech.MODID, "blocks/biomass_flow");
    public static final ResourceLocation NECRO_SUBSTRATE_STILL = new ResourceLocation(harvestech.MODID, "blocks/distilled_biomass_still");
    public static final ResourceLocation NECRO_SUBSTRATE_FLOW = new ResourceLocation(harvestech.MODID, "blocks/distilled_biomass_flow");
    public static final ResourceLocation BIO_TOXIN_STILL = new ResourceLocation(harvestech.MODID, "blocks/biomass_still");
    public static final ResourceLocation BIO_TOXIN_FLOW = new ResourceLocation(harvestech.MODID, "blocks/biomass_flow");
    public static final ResourceLocation AIDS_VACCINE_STILL = new ResourceLocation(harvestech.MODID, "blocks/distilled_biomass_still");
    public static final ResourceLocation AIDS_VACCINE_FLOW = new ResourceLocation(harvestech.MODID, "blocks/distilled_biomass_flow");
    public static final ResourceLocation MALE_POWER_STEROID_STILL = new ResourceLocation(harvestech.MODID, "blocks/distilled_biomass_still");
    public static final ResourceLocation MALE_POWER_STEROID_FLOW = new ResourceLocation(harvestech.MODID, "blocks/distilled_biomass_flow");

    public static Fluid BIOMASS;
    public static Fluid DISTILLED_BIOMASS;
    public static Fluid ENRICHED_BIOMASS;
    public static Fluid FERMENTED_SEMEN;
    public static Fluid DISTILLED_FERMENTED_SEMEN;
    public static Fluid TOXIC_FLESH;
    public static Fluid NECRO_SUBSTRATE;
    public static Fluid BIO_TOXIN;
    public static Fluid AIDS_VACCINE;
    public static Fluid MALE_POWER_STEROID;

    private ModFluids() {}

    public static void register() {
        BIOMASS = registerFluid("semen", BIOMASS_STILL, BIOMASS_FLOW, 0xFFFFFFFF);
        DISTILLED_BIOMASS = registerFluid("distilled_biomass", DISTILLED_BIOMASS_STILL, DISTILLED_BIOMASS_FLOW, 0xFFFFFFFF);
        ENRICHED_BIOMASS = registerFluid("enriched_biomass", ENRICHED_BIOMASS_STILL, ENRICHED_BIOMASS_FLOW, 0xFFFFFFFF);
        FERMENTED_SEMEN = registerFluid("fermented_semen", FERMENTED_SEMEN_STILL, FERMENTED_SEMEN_FLOW, 0xFF8D968D);
        DISTILLED_FERMENTED_SEMEN = registerFluid("distilled_fermented_semen", DISTILLED_FERMENTED_SEMEN_STILL, DISTILLED_FERMENTED_SEMEN_FLOW, 0xFFA9B3A9);
        TOXIC_FLESH = registerFluid("toxic_flesh", TOXIC_FLESH_STILL, TOXIC_FLESH_FLOW, 0xFF7A2030);
        NECRO_SUBSTRATE = registerFluid("necro_substrate", NECRO_SUBSTRATE_STILL, NECRO_SUBSTRATE_FLOW, 0xFFB34A5E);
        BIO_TOXIN = registerFluid("bio_toxin", BIO_TOXIN_STILL, BIO_TOXIN_FLOW, 0xFF52131D);
        AIDS_VACCINE = registerFluid("aids_vaccine_fluid", AIDS_VACCINE_STILL, AIDS_VACCINE_FLOW, 0xFFB24A5D);
        MALE_POWER_STEROID = registerFluid("male_power_steroid_fluid", MALE_POWER_STEROID_STILL, MALE_POWER_STEROID_FLOW, 0xFFFFEBC9);
    }

    private static Fluid registerFluid(String name, ResourceLocation still, ResourceLocation flow, int color) {
        Fluid existing = FluidRegistry.getFluid(name);
        if (existing != null) {
            existing.setColor(color);
            return existing;
        }

        Fluid fluid = new Fluid(name, still, flow)
                .setDensity(1100)
                .setViscosity(1400)
                .setLuminosity(0)
                .setTemperature(295)
                .setColor(color)
                .setUnlocalizedName(harvestech.MODID + "." + name);

        FluidRegistry.registerFluid(fluid);
        FluidRegistry.addBucketForFluid(fluid);
        return fluid;
    }

    @Mod.EventBusSubscriber(modid = harvestech.MODID, value = Side.CLIENT)
    public static class ClientTextures {
        @SubscribeEvent
        public static void onTextureStitch(TextureStitchEvent.Pre event) {
            event.getMap().registerSprite(BIOMASS_STILL);
            event.getMap().registerSprite(BIOMASS_FLOW);
            event.getMap().registerSprite(DISTILLED_BIOMASS_STILL);
            event.getMap().registerSprite(DISTILLED_BIOMASS_FLOW);
            event.getMap().registerSprite(ENRICHED_BIOMASS_STILL);
            event.getMap().registerSprite(ENRICHED_BIOMASS_FLOW);
            event.getMap().registerSprite(FERMENTED_SEMEN_STILL);
            event.getMap().registerSprite(FERMENTED_SEMEN_FLOW);
            event.getMap().registerSprite(DISTILLED_FERMENTED_SEMEN_STILL);
            event.getMap().registerSprite(DISTILLED_FERMENTED_SEMEN_FLOW);
            event.getMap().registerSprite(TOXIC_FLESH_STILL);
            event.getMap().registerSprite(TOXIC_FLESH_FLOW);
            event.getMap().registerSprite(NECRO_SUBSTRATE_STILL);
            event.getMap().registerSprite(NECRO_SUBSTRATE_FLOW);
            event.getMap().registerSprite(BIO_TOXIN_STILL);
            event.getMap().registerSprite(BIO_TOXIN_FLOW);
            event.getMap().registerSprite(AIDS_VACCINE_STILL);
            event.getMap().registerSprite(AIDS_VACCINE_FLOW);
            event.getMap().registerSprite(MALE_POWER_STEROID_STILL);
            event.getMap().registerSprite(MALE_POWER_STEROID_FLOW);
        }
    }
}
