package com.ogryzok.manualharvest.client.render;

import com.ogryzok.harvestech;
import com.ogryzok.manualharvest.tile.TileRottingTank;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class RottingTankGeoModel extends AnimatedGeoModel<TileRottingTank> {
    private static final ResourceLocation MODEL_EMPTY = new ResourceLocation(harvestech.MODID, "geo/rotting_tank.geo.json");
    private static final ResourceLocation MODEL_200 = new ResourceLocation(harvestech.MODID, "geo/rotting_tank_200.geo.json");
    private static final ResourceLocation MODEL_400 = new ResourceLocation(harvestech.MODID, "geo/rotting_tank_400.geo.json");
    private static final ResourceLocation MODEL_600 = new ResourceLocation(harvestech.MODID, "geo/rotting_tank_600.geo.json");
    private static final ResourceLocation MODEL_800 = new ResourceLocation(harvestech.MODID, "geo/rotting_tank_800.geo.json");
    private static final ResourceLocation MODEL_1000 = new ResourceLocation(harvestech.MODID, "geo/rotting_tank_1000.geo.json");
    private static final ResourceLocation MODEL_CLOSED = new ResourceLocation(harvestech.MODID, "geo/rotting_tank_closed.geo.json");
    private static final ResourceLocation TEXTURE_NORMAL = new ResourceLocation(harvestech.MODID, "textures/blocks/rotting_tank_biomass.png");
    private static final ResourceLocation TEXTURE_ROTTEN = new ResourceLocation(harvestech.MODID, "textures/blocks/rotting_tank_biomass_rotten.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(harvestech.MODID, "animations/chair.animation.json");

    @Override
    public ResourceLocation getModelLocation(TileRottingTank object) {
        if (object == null) return MODEL_EMPTY;
        if (object.isClosed()) return MODEL_CLOSED;
        switch (object.getVisualStageMb()) {
            case 200: return MODEL_200;
            case 400: return MODEL_400;
            case 600: return MODEL_600;
            case 800: return MODEL_800;
            case 1000: return MODEL_1000;
            default: return MODEL_EMPTY;
        }
    }

    @Override
    public ResourceLocation getTextureLocation(TileRottingTank object) {
        return object != null && object.isRottenReady() && !object.isClosed() ? TEXTURE_ROTTEN : TEXTURE_NORMAL;
    }

    @Override
    public ResourceLocation getAnimationFileLocation(TileRottingTank animatable) {
        return ANIMATION;
    }
}
