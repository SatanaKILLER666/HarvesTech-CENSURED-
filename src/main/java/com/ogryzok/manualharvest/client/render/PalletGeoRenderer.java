package com.ogryzok.manualharvest.client.render;

import com.ogryzok.manualharvest.tile.TilePallet;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumFacing;
import software.bernie.geckolib3.renderers.geo.GeoBlockRenderer;

public class PalletGeoRenderer extends GeoBlockRenderer<TilePallet> {
    public PalletGeoRenderer() {
        super(new PalletGeoModel());
    }

    @Override
    public void rotateBlock(EnumFacing facing) {
        if (facing == null) return;
        switch (facing) {
            case NORTH:
                GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
                break;
            case EAST:
                GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                break;
            case SOUTH:
                GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
                break;
            case WEST:
            default:
                break;
        }
    }
}
