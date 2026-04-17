package com.ogryzok.semenenrichment.client.render;

import com.ogryzok.harvestech;
import com.ogryzok.semenenrichment.tile.TileSemenEnrichmentChamber;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class SemenEnrichmentChamberGeoModel extends AnimatedGeoModel<TileSemenEnrichmentChamber> {
    private static final ResourceLocation MODEL = new ResourceLocation(harvestech.MODID, "geo/semen_enrichment_chamber.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(harvestech.MODID, "textures/blocks/semen_enrichment_chamber.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(harvestech.MODID, "animations/chair.animation.json");
    @Override public ResourceLocation getModelLocation(TileSemenEnrichmentChamber object) { return MODEL; }
    @Override public ResourceLocation getTextureLocation(TileSemenEnrichmentChamber object) { return TEXTURE; }
    @Override public ResourceLocation getAnimationFileLocation(TileSemenEnrichmentChamber animatable) { return ANIMATION; }
}
