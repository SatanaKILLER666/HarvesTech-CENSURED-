package com.ogryzok.blender.client.render;

import com.ogryzok.blender.tile.TileBlender;
import com.ogryzok.harvestech;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class BlenderGeoModel extends AnimatedGeoModel<TileBlender> {
    private static final ResourceLocation MODEL = new ResourceLocation(harvestech.MODID, "geo/blender.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(harvestech.MODID, "textures/blocks/blender.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(harvestech.MODID, "animations/blender.animation.json");

    @Override
    public ResourceLocation getModelLocation(TileBlender object) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureLocation(TileBlender object) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationFileLocation(TileBlender animatable) {
        return ANIMATION;
    }
}
