package com.ogryzok.semendestiller.client.render;

import com.ogryzok.harvestech;
import com.ogryzok.semendestiller.tile.TileSemenDestillerMotor;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class SemenDestillerMotorGeoModel extends AnimatedGeoModel<TileSemenDestillerMotor> {
    private static final ResourceLocation MODEL = new ResourceLocation(harvestech.MODID, "geo/semen_destiller_motor.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(harvestech.MODID, "textures/blocks/semen_destiller.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(harvestech.MODID, "animations/chair.animation.json");
    @Override public ResourceLocation getModelLocation(TileSemenDestillerMotor object) { return MODEL; }
    @Override public ResourceLocation getTextureLocation(TileSemenDestillerMotor object) { return TEXTURE; }
    @Override public ResourceLocation getAnimationFileLocation(TileSemenDestillerMotor animatable) { return ANIMATION; }
}
