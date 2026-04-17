package com.ogryzok.semendestiller.client.render;

import com.ogryzok.harvestech;
import com.ogryzok.semendestiller.tile.TileSemenDestillerBase;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class SemenDestillerBaseGeoModel extends AnimatedGeoModel<TileSemenDestillerBase> {
    private static final ResourceLocation MODEL = new ResourceLocation(harvestech.MODID, "geo/semen_destiller_base.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(harvestech.MODID, "textures/blocks/semen_destiller.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(harvestech.MODID, "animations/chair.animation.json");
    @Override public ResourceLocation getModelLocation(TileSemenDestillerBase object) { return MODEL; }
    @Override public ResourceLocation getTextureLocation(TileSemenDestillerBase object) { return TEXTURE; }
    @Override public ResourceLocation getAnimationFileLocation(TileSemenDestillerBase animatable) { return ANIMATION; }
}
