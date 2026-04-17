package com.ogryzok.lifecrusher.client.render;

import com.ogryzok.lifecrusher.item.ItemLifeCrusherBase;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class LifeCrusherBaseGeoModel extends AnimatedGeoModel<ItemLifeCrusherBase> {
    private static final String MODID = "harvestech";

    @Override
    public ResourceLocation getModelLocation(ItemLifeCrusherBase object) {
        return new ResourceLocation(MODID, "geo/life_crusher_base.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(ItemLifeCrusherBase object) {
        return new ResourceLocation(MODID, "textures/blocks/life_crusher.png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(ItemLifeCrusherBase object) {
        return new ResourceLocation(MODID, "animations/life_crusher.animation.json");
    }
}