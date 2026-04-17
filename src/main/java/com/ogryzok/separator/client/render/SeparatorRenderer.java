package com.ogryzok.separator.client.render;

import com.ogryzok.separator.tile.TileSeparator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumFacing;
import org.lwjgl.opengl.GL11;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.renderers.geo.GeoBlockRenderer;

public class SeparatorRenderer extends GeoBlockRenderer<TileSeparator> {
    private enum RenderPass { SOLID, GLASS }
    private RenderPass renderPass = RenderPass.SOLID;

    public SeparatorRenderer() {
        super(new SeparatorGeoModel());
    }

    @Override
    public void render(TileSeparator tile, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        try {
            renderPass = RenderPass.SOLID;
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.depthMask(true);
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            super.render(tile, x, y, z, partialTicks, destroyStage, alpha);

            renderPass = RenderPass.GLASS;
            GlStateManager.enableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.05F);
            GlStateManager.depthMask(false);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            super.render(tile, x, y, z, partialTicks, destroyStage, alpha);
        } finally {
            renderPass = RenderPass.SOLID;
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @Override
    public void renderRecursively(BufferBuilder builder, GeoBone bone, float red, float green, float blue, float alpha) {
        boolean isGlass = bone.getName() != null && bone.getName().toLowerCase().contains("glass");
        if (renderPass == RenderPass.SOLID && isGlass) return;
        if (renderPass == RenderPass.GLASS && !isGlass) return;
        super.renderRecursively(builder, bone, red, green, blue, alpha);
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
