package com.ogryzok.separator.client.render;

import com.ogryzok.harvestech;
import com.ogryzok.separator.tile.TileSeparator;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class SeparatorGeoModel extends AnimatedGeoModel<TileSeparator> {
    private static final ResourceLocation MODEL = new ResourceLocation(harvestech.MODID, "geo/separator.geo.json");
    private static final ResourceLocation BASE_MODEL = new ResourceLocation(harvestech.MODID, "geo/separator_base.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(harvestech.MODID, "textures/blocks/separator_base.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(harvestech.MODID, "animations/separator.animation.json");
    @Override public ResourceLocation getModelLocation(TileSeparator object) { return object != null && object.isAssembled() ? MODEL : BASE_MODEL; }
    @Override public ResourceLocation getTextureLocation(TileSeparator object) { return TEXTURE; }
    @Override public ResourceLocation getAnimationFileLocation(TileSeparator animatable) { return ANIMATION; }
}
