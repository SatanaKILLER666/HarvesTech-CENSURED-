package com.ogryzok.disease.client;

import com.ogryzok.disease.DiseaseRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class DiseaseOverlayHandler extends Gui {
    private static final ResourceLocation SOFT_BLOOD = new ResourceLocation("harvestech", "textures/gui/soft_blood.png");
    private static final ResourceLocation MID_BLOOD = new ResourceLocation("harvestech", "textures/gui/mid_blood.png");
    private static final ResourceLocation HARD_BLOOD = new ResourceLocation("harvestech", "textures/gui/hard_blood.png");

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null) {
            return;
        }

        ResourceLocation overlay = null;
        if (player.isPotionActive(DiseaseRegistry.AIDS_STAGE_1)) {
            overlay = SOFT_BLOOD;
        }
        if (player.isPotionActive(DiseaseRegistry.AIDS_STAGE_2)) {
            overlay = MID_BLOOD;
        }
        if (player.isPotionActive(DiseaseRegistry.VEGETABLE)) {
            overlay = HARD_BLOOD;
        }

        if (overlay == null) {
            return;
        }

        ScaledResolution res = event.getResolution();
        int w = res.getScaledWidth();
        int h = res.getScaledHeight();

        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );

        mc.getTextureManager().bindTexture(overlay);
        drawScaledCustomSizeModalRect(0, 0, 0.0F, 0.0F, 256, 256, w, h, 256.0F, 256.0F);

        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
    }
}
