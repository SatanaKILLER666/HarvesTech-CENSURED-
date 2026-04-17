package com.ogryzok.disease.client.render;

import com.ogryzok.disease.item.ItemSyringe;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class SyringeGeoModel extends AnimatedGeoModel<ItemSyringe> {
    @Override
    public ResourceLocation getModelLocation(ItemSyringe object) {
        return new ResourceLocation("harvestech", "geo/syringe.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(ItemSyringe object) {
        return new ResourceLocation("harvestech", "textures/items/vaccine.png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(ItemSyringe animatable) {
        return new ResourceLocation("harvestech", "animations/vaccine.animation.json");
    }
}
