package com.ogryzok.client.render;

import com.ogryzok.client.item.ItemGeoBlock;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class GeoBlockItemModel extends AnimatedGeoModel<ItemGeoBlock> {
    @Override
    public ResourceLocation getModelLocation(ItemGeoBlock object) {
        return object.getGeoModel();
    }

    @Override
    public ResourceLocation getTextureLocation(ItemGeoBlock object) {
        return object.getTexture();
    }

    @Override
    public ResourceLocation getAnimationFileLocation(ItemGeoBlock animatable) {
        return animatable.getAnimation();
    }
}
