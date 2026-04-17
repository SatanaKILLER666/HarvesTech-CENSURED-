package com.ogryzok.manualharvest.client.render;

import com.ogryzok.manualharvest.tile.TileRottingTank;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumFacing;
import software.bernie.geckolib3.renderers.geo.GeoBlockRenderer;

public class RottingTankGeoRenderer extends GeoBlockRenderer<TileRottingTank> {
    public RottingTankGeoRenderer() {
        super(new RottingTankGeoModel());
    }

    @Override
    public void rotateBlock(EnumFacing facing) {
        if (facing == null) return;
        switch (facing) {
            case NORTH:
                break;
            case EAST:
                GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
                break;
            case SOUTH:
                GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                break;
            case WEST:
                GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
                break;
            default:
                break;
        }
    }
}
