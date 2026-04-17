package com.ogryzok.chair.client.render;

import com.ogryzok.chair.tile.TileChair;
import com.ogryzok.harvestech;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class ChairGeoModel extends AnimatedGeoModel<TileChair> {
    private static final ResourceLocation MODEL = new ResourceLocation(harvestech.MODID, "geo/chair.geo.json");
    private static final ResourceLocation OCCUPIED_MODEL = new ResourceLocation(harvestech.MODID, "geo/occupied_chair.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(harvestech.MODID, "textures/blocks/chair.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(harvestech.MODID, "animations/chair.animation.json");

    @Override
    public ResourceLocation getModelLocation(TileChair object) {
        if (object != null && object.isOccupied()) {
            return OCCUPIED_MODEL;
        }
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureLocation(TileChair object) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationFileLocation(TileChair animatable) {
        return ANIMATION;
    }
}
