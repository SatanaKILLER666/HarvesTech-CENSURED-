package com.ogryzok.manualharvest.client.render;

import com.ogryzok.harvestech;
import com.ogryzok.manualharvest.block.BlockPallet;
import com.ogryzok.manualharvest.tile.TilePallet;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class PalletGeoModel extends AnimatedGeoModel<TilePallet> {
    private static final ResourceLocation EMPTY_MODEL = new ResourceLocation(harvestech.MODID, "geo/pallet.geo.json");
    private static final ResourceLocation FULL_MODEL = new ResourceLocation(harvestech.MODID, "geo/full_pallet.geo.json");
    private static final ResourceLocation SEMI_MODEL = new ResourceLocation(harvestech.MODID, "geo/semi_fulled_pallet.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(harvestech.MODID, "textures/blocks/pallet.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(harvestech.MODID, "animations/chair.animation.json");

    @Override
    public ResourceLocation getModelLocation(TilePallet object) {
        if (object != null) {
            int stage = object.getStage();
            if (stage == BlockPallet.STAGE_FULL) return FULL_MODEL;
            if (stage == BlockPallet.STAGE_SEMI) return SEMI_MODEL;
        }
        return EMPTY_MODEL;
    }

    @Override
    public ResourceLocation getTextureLocation(TilePallet object) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationFileLocation(TilePallet animatable) {
        return ANIMATION;
    }
}
