package com.ogryzok.chair.client.render;

import com.ogryzok.chair.tile.TileChair;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumFacing;
import software.bernie.geckolib3.renderers.geo.GeoBlockRenderer;

public class ChairGeoRenderer extends GeoBlockRenderer<TileChair> {
    public ChairGeoRenderer() {
        super(new ChairGeoModel());
    }

    @Override
    public void rotateBlock(EnumFacing facing) {
        if (facing == null) {
            return;
        }

        switch (facing) {
            case NORTH:
                GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
                break;
            case EAST:
                break;
            case SOUTH:
                GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
                break;
            case WEST:
                GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                break;
            default:
                break;
        }
    }
}
