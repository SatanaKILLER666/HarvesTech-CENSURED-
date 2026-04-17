package com.ogryzok.lifecrusher.client.render;

import com.ogryzok.lifecrusher.item.ItemLifeCrusherRod;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class LifeCrusherRodGeoModel extends AnimatedGeoModel<ItemLifeCrusherRod> {
    private static final String MODID = "harvestech";

    @Override
    public ResourceLocation getModelLocation(ItemLifeCrusherRod object) {
        return new ResourceLocation(MODID, "geo/life_crusher_rod.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(ItemLifeCrusherRod object) {
        return new ResourceLocation(MODID, "textures/items/life_crusher_rod.png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(ItemLifeCrusherRod object) {
        return new ResourceLocation(MODID, "animations/life_crusher_rod.animation.json");
    }
}