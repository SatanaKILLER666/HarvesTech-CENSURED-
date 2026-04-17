package com.ogryzok.lifecrusher.client.render;

import com.ogryzok.lifecrusher.item.ItemRubberRod;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

public class RubberRodRenderer extends GeoItemRenderer<ItemRubberRod> {
    public RubberRodRenderer() {
        super(new RubberRodGeoModel());
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
