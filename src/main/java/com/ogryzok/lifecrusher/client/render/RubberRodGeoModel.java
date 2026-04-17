package com.ogryzok.lifecrusher.client.render;

import com.ogryzok.lifecrusher.item.ItemRubberRod;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class RubberRodGeoModel extends AnimatedGeoModel<ItemRubberRod> {
    private static final String MODID = "harvestech";

    @Override
    public ResourceLocation getModelLocation(ItemRubberRod object) {
        return new ResourceLocation(MODID, "geo/rubber_rod.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(ItemRubberRod object) {
        return new ResourceLocation(MODID, "textures/items/rubber_rod.png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(ItemRubberRod object) {
        return new ResourceLocation(MODID, "animations/life_crusher.animation.json");
    }
}
