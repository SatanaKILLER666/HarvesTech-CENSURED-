package com.ogryzok.disease.client.render;

import com.ogryzok.disease.item.ItemSyringe;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

public class SyringeItemRenderer extends GeoItemRenderer<ItemSyringe> {
    private enum RenderPass {
        SOLID,
        GLASS
    }

    private RenderPass renderPass = RenderPass.SOLID;

    public SyringeItemRenderer() {
        super(new SyringeGeoModel());
    }

    @Override
    public void renderByItem(ItemStack stack) {
        GlStateManager.pushMatrix();
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

        if (renderPass == RenderPass.SOLID && isGlass) {
            return;
        }
        if (renderPass == RenderPass.GLASS && !isGlass) {
            return;
        }

        super.renderRecursively(builder, bone, red, green, blue, alpha);
    }
}
