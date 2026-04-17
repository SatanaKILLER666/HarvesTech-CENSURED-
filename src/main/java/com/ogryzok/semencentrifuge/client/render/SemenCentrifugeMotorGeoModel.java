package com.ogryzok.semencentrifuge.client.render;

import com.ogryzok.harvestech;
import com.ogryzok.semencentrifuge.tile.TileSemenCentrifugeMotor;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class SemenCentrifugeMotorGeoModel extends AnimatedGeoModel<TileSemenCentrifugeMotor> {
    private static final ResourceLocation MODEL = new ResourceLocation(harvestech.MODID, "geo/semen_centrifuge_motor.geo.json");
    private static final ResourceLocation OPENED_MODEL = new ResourceLocation(harvestech.MODID, "geo/semen_centrifuge_motor_opened.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(harvestech.MODID, "textures/blocks/semen_centrifuge.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(harvestech.MODID, "animations/semencentrifuge.animation.json");

    @Override
    public ResourceLocation getModelLocation(TileSemenCentrifugeMotor object) {
        return object != null && object.shouldUseOpenedModel() ? OPENED_MODEL : MODEL;
    }

    @Override
    public ResourceLocation getTextureLocation(TileSemenCentrifugeMotor object) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationFileLocation(TileSemenCentrifugeMotor animatable) {
        return ANIMATION;
    }
}
