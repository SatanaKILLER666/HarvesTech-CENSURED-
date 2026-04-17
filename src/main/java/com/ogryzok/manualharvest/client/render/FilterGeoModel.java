package com.ogryzok.manualharvest.client.render;

import com.ogryzok.harvestech;
import com.ogryzok.manualharvest.tile.TileFilter;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class FilterGeoModel extends AnimatedGeoModel<TileFilter> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(harvestech.MODID, "textures/blocks/filter.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(harvestech.MODID, "animations/filter.animation.json");

    @Override
    public ResourceLocation getModelLocation(TileFilter tile) {
        String key = tile == null ? "empty_filter" : tile.getCurrentModelKey();
        String path;
        if ("filter_base".equals(key)
                || "empty_filter".equals(key)
                || "filter_with_sand".equals(key)
                || "filter_fulled_input".equals(key)) {
            path = "geo/" + key + ".geo.json";
        } else if ("filter_100".equals(key)) {
            path = "geo/filter_100mb.geo.json";
        } else if (key.startsWith("work_")) {
            path = "geo/" + key + ".geo.json";
        } else {
            path = "geo/" + key + "mb.geo.json";
        }
        return new ResourceLocation(harvestech.MODID, path);
    }

    @Override
    public ResourceLocation getTextureLocation(TileFilter object) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationFileLocation(TileFilter animatable) {
        return ANIMATION;
    }
}
