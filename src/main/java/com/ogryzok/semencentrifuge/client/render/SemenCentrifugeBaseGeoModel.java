package com.ogryzok.semencentrifuge.client.render;

import com.ogryzok.harvestech;
import com.ogryzok.semencentrifuge.tile.TileSemenCentrifugeBase;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class SemenCentrifugeBaseGeoModel extends AnimatedGeoModel<TileSemenCentrifugeBase> {
    private static final ResourceLocation MODEL = new ResourceLocation(harvestech.MODID, "geo/semen_centrifuge_base.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(harvestech.MODID, "textures/blocks/semen_centrifuge.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(harvestech.MODID, "animations/chair.animation.json");

    @Override
    public ResourceLocation getModelLocation(TileSemenCentrifugeBase object) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureLocation(TileSemenCentrifugeBase object) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationFileLocation(TileSemenCentrifugeBase animatable) {
        return ANIMATION;
    }
}
