package com.ogryzok.mrnasynthesizer.client.render;

import com.ogryzok.harvestech;
import com.ogryzok.mrnasynthesizer.tile.TileMRNASynthesizer;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class MRNASynthesizerGeoModel extends AnimatedGeoModel<TileMRNASynthesizer> {
    private static final ResourceLocation MODEL = new ResourceLocation(harvestech.MODID, "geo/vaccine_generator.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(harvestech.MODID, "textures/blocks/vaccine_generator.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(harvestech.MODID, "animations/vaccine_generator.animation.json");

    @Override public ResourceLocation getModelLocation(TileMRNASynthesizer object) { return MODEL; }
    @Override public ResourceLocation getTextureLocation(TileMRNASynthesizer object) { return TEXTURE; }
    @Override public ResourceLocation getAnimationFileLocation(TileMRNASynthesizer animatable) { return ANIMATION; }
}
