package com.ogryzok.lifecrusher.client.render;

import com.ogryzok.lifecrusher.item.ItemBayonet;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class BayonetGeoModel extends AnimatedGeoModel<ItemBayonet> {

    @Override
    public ResourceLocation getModelLocation(ItemBayonet object) {
        return new ResourceLocation("harvestech", "geo/bayonet.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(ItemBayonet object) {
        return new ResourceLocation("harvestech", "textures/items/life_crusher_rod.png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(ItemBayonet animatable) {
        return new ResourceLocation("harvestech", "animations/bayonet.animation.json");
    }
}