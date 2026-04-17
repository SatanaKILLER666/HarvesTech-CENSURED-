package com.ogryzok.client.render;

import com.ogryzok.client.item.ItemGeoBlock;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

public class GeoBlockItemRenderer extends GeoItemRenderer<ItemGeoBlock> {
    private enum RenderPass { SOLID, GLASS }
    private RenderPass renderPass = RenderPass.SOLID;

    public GeoBlockItemRenderer() {
        super(new GeoBlockItemModel());
    }

    @Override
    public void renderByItem(ItemStack stack) {
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.82F, 0.82F, 0.82F);
        GlStateManager.enableRescaleNormal();
        try {
            renderPass = RenderPass.SOLID;
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.depthMask(true);
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            super.renderByItem(stack);

            renderPass = RenderPass.GLASS;
            GlStateManager.enableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.depthMask(false);
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.01F);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            super.renderByItem(stack);
        } finally {
            renderPass = RenderPass.SOLID;
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.disableRescaleNormal();
            GlStateManager.popMatrix();
        }
    }

    @Override
    public void renderRecursively(BufferBuilder builder, GeoBone bone, float red, float green, float blue, float alpha) {
        String name = bone.getName() == null ? "" : bone.getName().toLowerCase();
        boolean isGlass = name.contains("glass");
        if (renderPass == RenderPass.SOLID && isGlass) return;
        if (renderPass == RenderPass.GLASS && !isGlass) return;
        super.renderRecursively(builder, bone, red, green, blue, alpha);
    }
}
