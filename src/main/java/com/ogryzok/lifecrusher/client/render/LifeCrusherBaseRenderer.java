package com.ogryzok.lifecrusher.client.render;

import com.ogryzok.lifecrusher.item.ItemLifeCrusherBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

public class LifeCrusherBaseRenderer extends GeoItemRenderer<ItemLifeCrusherBase> {
    public LifeCrusherBaseRenderer() {
        super(new LifeCrusherBaseGeoModel());
    }

    @Override
    public void renderByItem(ItemStack stack) {
        GlStateManager.pushMatrix();

        // только лёгкая общая подстройка
        GlStateManager.scale(0.75F, 0.75F, 0.75F);
        GlStateManager.translate(0.10F, -0.02F, 0.0F);

        super.renderByItem(stack);

        GlStateManager.popMatrix();
    }
}