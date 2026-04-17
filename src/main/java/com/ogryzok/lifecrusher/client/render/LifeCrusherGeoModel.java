package com.ogryzok.lifecrusher.client.render;

import com.ogryzok.lifecrusher.tile.TileLifeCrusher;

import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class LifeCrusherGeoModel extends AnimatedGeoModel<TileLifeCrusher> {

    private static final String MODID = "harvestech";

    @Override
    public ResourceLocation getModelLocation(TileLifeCrusher tile) {
        if (tile == null) {
            return new ResourceLocation(MODID, "geo/life_crusher_base.geo.json");
        }

        if (tile.isAssembled()) {
            return new ResourceLocation(MODID, "geo/life_crusher.geo.json");
        }

        int wires = tile.getWiresInstalled();

        if (wires <= 0) {
            return new ResourceLocation(MODID, "geo/life_crusher_base.geo.json");
        }

        return new ResourceLocation(MODID, "geo/life_crusher_with_" + wires + "wire.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(TileLifeCrusher tile) {
        return new ResourceLocation(MODID, "textures/blocks/life_crusher.png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(TileLifeCrusher tile) {
        return new ResourceLocation(MODID, "animations/life_crusher.animation.json");
    }
}