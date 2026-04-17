package com.ogryzok.lifecrusher.client.render;

import com.ogryzok.lifecrusher.item.ItemLifeCrusherRod;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

public class LifeCrusherRodRenderer extends GeoItemRenderer<ItemLifeCrusherRod> {
    public LifeCrusherRodRenderer() {
        super(new LifeCrusherRodGeoModel());
    }

    @Override
    public void renderByItem(ItemStack stack) {
        GlStateManager.pushMatrix();

        GlStateManager.scale(1.0F, 1.0F, 1.0F);
        GlStateManager.translate(0.0F, 0.0F, 0.0F);

        super.renderByItem(stack);

        GlStateManager.popMatrix();
    }
}