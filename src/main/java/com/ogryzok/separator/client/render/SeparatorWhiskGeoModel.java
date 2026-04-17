package com.ogryzok.separator.client.render;

import com.ogryzok.separator.item.ItemSeparatorWhisk;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class SeparatorWhiskGeoModel extends AnimatedGeoModel<ItemSeparatorWhisk> {
    @Override
    public ResourceLocation getModelLocation(ItemSeparatorWhisk object) {
        return new ResourceLocation("harvestech", "geo/separator_whisk.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(ItemSeparatorWhisk object) {
        return new ResourceLocation("harvestech", "textures/blocks/separator.png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(ItemSeparatorWhisk animatable) {
        return new ResourceLocation("harvestech", "animations/separator.animation.json");
    }
}
