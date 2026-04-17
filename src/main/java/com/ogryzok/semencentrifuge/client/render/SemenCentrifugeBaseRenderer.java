package com.ogryzok.semencentrifuge.client.render;

import com.ogryzok.semencentrifuge.tile.TileSemenCentrifugeBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumFacing;
import org.lwjgl.opengl.GL11;
import software.bernie.geckolib3.renderers.geo.GeoBlockRenderer;

public class SemenCentrifugeBaseRenderer extends GeoBlockRenderer<TileSemenCentrifugeBase> {
    public SemenCentrifugeBaseRenderer() {
        super(new SemenCentrifugeBaseGeoModel());
    }

    @Override
    public void render(TileSemenCentrifugeBase tile, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.0F);

        super.render(tile, x, y, z, partialTicks, destroyStage, alpha);

        GlStateManager.disableBlend();
    }

    @Override
    public void rotateBlock(EnumFacing facing) {
        if (facing == null) {
            return;
        }

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
