package com.ogryzok.client.render;

import com.ogryzok.client.item.ItemGeoSimple;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class GeoSimpleItemModel extends AnimatedGeoModel<ItemGeoSimple> {
    @Override
    public ResourceLocation getModelLocation(ItemGeoSimple object) {
        return object.getGeoModel();
    }

    @Override
    public ResourceLocation getTextureLocation(ItemGeoSimple object) {
        return object.getTexture();
    }

    @Override
    public ResourceLocation getAnimationFileLocation(ItemGeoSimple animatable) {
        return animatable.getAnimation();
    }
}
