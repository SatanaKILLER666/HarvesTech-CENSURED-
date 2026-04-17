package com.ogryzok.blender.client.render;

import com.ogryzok.blender.item.ItemBlenderWhisk;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class BlenderWhiskGeoModel extends AnimatedGeoModel<ItemBlenderWhisk> {
    @Override
    public ResourceLocation getModelLocation(ItemBlenderWhisk object) {
        return new ResourceLocation("harvestech", "geo/blender_whisk.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(ItemBlenderWhisk object) {
        return new ResourceLocation("harvestech", "textures/blocks/blender.png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(ItemBlenderWhisk animatable) {
        return new ResourceLocation("harvestech", "animations/blender.animation.json");
    }
}
