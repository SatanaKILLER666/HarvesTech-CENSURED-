package com.ogryzok.proteinformer.client.render;

import com.ogryzok.harvestech;
import com.ogryzok.proteinformer.tile.TileProteinFormer;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class ProteinFormerGeoModel extends AnimatedGeoModel<TileProteinFormer> {
    private static final ResourceLocation MODEL = new ResourceLocation(harvestech.MODID, "geo/protein_former.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(harvestech.MODID, "textures/blocks/protein_former.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(harvestech.MODID, "animations/protein_former.animation.json");

    @Override public ResourceLocation getModelLocation(TileProteinFormer object) { return MODEL; }
    @Override public ResourceLocation getTextureLocation(TileProteinFormer object) { return TEXTURE; }
    @Override public ResourceLocation getAnimationFileLocation(TileProteinFormer animatable) { return ANIMATION; }
}
