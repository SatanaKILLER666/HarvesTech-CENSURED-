package com.ogryzok.player.semen;

import com.ogryzok.harvestech;
import com.ogryzok.player.abstinence.AbstinenceData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class SemenHUD {

    private static final ResourceLocation STATUS_ICONS = new ResourceLocation(harvestech.MODID, "textures/gui/abstinence_icons.png");
    private static final ResourceLocation WHITE_HEARTS = new ResourceLocation(harvestech.MODID, "textures/gui/white_hearts.png");
    private static final ResourceLocation ENCHANT_GLINT = new ResourceLocation("textures/misc/enchanted_item_glint.png");

    private final Minecraft mc = Minecraft.getMinecraft();

    private boolean heartsRenderAdjusted = false;
    private float savedRealHealth = 0.0F;
    private AttributeModifier savedHealthModifier = null;

    @SubscribeEvent
    public void onDrawInventory(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.getGui() instanceof GuiInventory)) return;
        if (event.getGui() instanceof GuiContainerCreative) return;

        EntityPlayer player = mc.player;
        if (player == null) return;

        ISemenStorage storage = player.getCapability(SemenProvider.SEMEN_CAP, null);
        if (storage == null) return;

        int amount = storage.getAmount();
        int capacity = storage.getCapacity();
        if (capacity <= 0) return;

        int filledSegments = amount / 100;
        if (filledSegments < 0) filledSegments = 0;
        if (filledSegments > 10) filledSegments = 10;

        GuiContainer gui = (GuiContainer) event.getGui();

        int guiLeft = getGuiLeft(gui);
        int guiTop = getGuiTop(gui);

        int x = guiLeft + 49;
        int y = guiTop - 18;

        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.translate(0.0F, 0.0F, 300.0F);

        drawSegmentedBar(x, y, 58, 12, filledSegments, 10, storage.hasSeedKeeper());
        drawStatusIcons(x + 62, y - 2, storage);

        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        mc.getTextureManager().bindTexture(Gui.ICONS);
        GlStateManager.popMatrix();
    }

    @SubscribeEvent
    public void onRenderOverlayPre(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.HEALTH) return;
        if (mc.player == null || mc.gameSettings.hideGUI) return;
        if (heartsRenderAdjusted) return;

        EntityPlayer player = mc.player;
        ISemenStorage storage = player.getCapability(SemenProvider.SEMEN_CAP, null);
        if (storage == null) return;

        int extraPairs = AbstinenceData.getExtraHeartPairs(storage.getAbstinenceStage());
        if (extraPairs <= 0 && !storage.hasSeedKeeper()) return;

        IAttributeInstance attr = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (attr == null) return;

        savedRealHealth = player.getHealth();
        savedHealthModifier = attr.getModifier(SemenTickHandler.ABSTINENCE_HEALTH_UUID);
        if (savedHealthModifier == null) return;

        attr.removeModifier(savedHealthModifier);
        if (savedRealHealth > 20.0F) {
            player.setHealth(20.0F);
        }

        heartsRenderAdjusted = true;
    }

    @SubscribeEvent
    public void onRenderHealth(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.HEALTH) return;
        if (mc.player == null || mc.gameSettings.hideGUI) return;

        EntityPlayer player = mc.player;
        ISemenStorage storage = player.getCapability(SemenProvider.SEMEN_CAP, null);
        if (storage == null) return;

        int extraPairs = AbstinenceData.getExtraHeartPairs(storage.getAbstinenceStage());
        if (extraPairs <= 0 && !storage.hasSeedKeeper()) return;

        float extraMaxHealth = extraPairs * 4.0F;
        if (extraMaxHealth <= 0.0F) return;

        float extraHealth = Math.max(0.0F, Math.min(extraMaxHealth, savedRealHealth - 20.0F));

        ScaledResolution sr = event.getResolution();
        int left = sr.getScaledWidth() / 2 - 91;
        int top = sr.getScaledHeight() - 39;

        drawWhiteOverlayHearts(left, top, extraHealth, extraMaxHealth);
    }

    @SubscribeEvent
    public void onRenderOverlayPost(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.HEALTH) return;
        if (mc.player == null || mc.gameSettings.hideGUI) return;
        if (!heartsRenderAdjusted) return;

        EntityPlayer player = mc.player;
        IAttributeInstance attr = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (attr != null && savedHealthModifier != null && attr.getModifier(savedHealthModifier.getID()) == null) {
            attr.applyModifier(savedHealthModifier);
        }

        if (player.getHealth() != savedRealHealth) {
            player.setHealth(savedRealHealth);
        }

        heartsRenderAdjusted = false;
        savedHealthModifier = null;
    }

    private void drawWhiteOverlayHearts(int left, int top, float extraHealth, float extraMaxHealth) {
        int maxHalfHearts = Math.max(0, Math.min(20, Math.round(extraMaxHealth)));
        if (maxHalfHearts <= 0) return;

        int filledHalfHearts = Math.max(0, Math.min(maxHalfHearts, Math.round(extraHealth)));
        int maxFullHearts = (maxHalfHearts + 1) / 2;
        int filledFullHearts = filledHalfHearts / 2;
        boolean hasFilledHalf = (filledHalfHearts % 2) != 0;
        boolean hasEmptyHalf = (maxHalfHearts % 2) != 0;

        mc.getTextureManager().bindTexture(WHITE_HEARTS);
        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.translate(0.0F, 0.0F, 200.0F);

        GlStateManager.color(1F, 1F, 1F, 1F);
        for (int slot = 0; slot < maxFullHearts; slot++) {
            int x = left + slot * 8;
            Gui.drawModalRectWithCustomSizedTexture(x, top, 9, 0, 9, 9, 18, 9);
        }

        if (hasEmptyHalf && maxFullHearts > 0) {
            int x = left + (maxFullHearts - 1) * 8;
            Gui.drawModalRectWithCustomSizedTexture(x, top, 9, 0, 5, 9, 18, 9);
        }

        for (int slot = 0; slot < filledFullHearts; slot++) {
            int x = left + slot * 8;
            Gui.drawModalRectWithCustomSizedTexture(x, top, 0, 0, 9, 9, 18, 9);
        }

        if (hasFilledHalf && filledFullHearts < maxFullHearts) {
            int x = left + filledFullHearts * 8;
            Gui.drawModalRectWithCustomSizedTexture(x, top, 0, 0, 5, 9, 18, 9);
        }

        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        mc.getTextureManager().bindTexture(Gui.ICONS);
        GlStateManager.popMatrix();
    }

    private void drawSegmentedBar(int x, int y, int width, int height, int filledSegments, int totalSegments, boolean enchanted) {
        Gui.drawRect(x - 1, y - 1, x + width + 1, y + height + 1, 0x50000000);
        Gui.drawRect(x, y, x + width, y + height, 0xFF1C1C1C);
        Gui.drawRect(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF8A8A8A);
        Gui.drawRect(x + 2, y + 2, x + width - 2, y + height - 2, 0xFF202020);

        int innerX = x + 2;
        int innerY = y + 2;
        int innerW = width - 4;
        int innerH = height - 4;

        int segmentW = innerW / totalSegments;

        for (int i = 0; i < totalSegments; i++) {
            int sx = innerX + i * segmentW;
            int ex = (i == totalSegments - 1) ? (innerX + innerW) : (sx + segmentW);

            if (i < filledSegments) {
                Gui.drawRect(sx, innerY, ex, innerY + innerH, enchanted ? 0xFFE8D9FF : 0xFFF5F5F5);
                Gui.drawRect(sx, innerY, ex, innerY + 1, enchanted ? 0x90FFFFFF : 0x60FFFFFF);
                Gui.drawRect(sx, innerY + innerH - 1, ex, innerY + innerH, 0x18000000);
                drawSegmentBubble(sx, innerY, ex - sx, innerH, i);
            }
        }

        if (innerW > 3 && innerH > 2) {
            Gui.drawRect(innerX + 1, innerY + 1, innerX + innerW / 3, innerY + innerH - 1, 0x18FFFFFF);
        }

        if (enchanted) {
            drawGlint(innerX, innerY, innerW, innerH);
        }
    }

    private void drawStatusIcons(int x, int y, ISemenStorage storage) {
        int stage = storage.getAbstinenceStage();
        if (stage <= 0) return;

        mc.getTextureManager().bindTexture(STATUS_ICONS);
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableBlend();

        if (storage.hasSeedKeeper()) {
            Gui.drawModalRectWithCustomSizedTexture(x, y, 16, 0, 16, 16, 32, 16);
        } else {
            for (int i = 0; i < stage; i++) {
                Gui.drawModalRectWithCustomSizedTexture(x + i * 17, y, 0, 0, 16, 16, 32, 16);
            }
        }

        GlStateManager.disableBlend();
    }

    private void drawGlint(int x, int y, int width, int height) {
        mc.getTextureManager().bindTexture(ENCHANT_GLINT);
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_COLOR, GlStateManager.DestFactor.ONE);
        GlStateManager.color(0.75F, 0.45F, 1.0F, 0.45F);

        float t = (mc.player == null ? 0 : (mc.player.ticksExisted % 300) / 300.0F) * 32.0F;
        Gui.drawModalRectWithCustomSizedTexture(x, y, t, 0.0F, width, height, 64.0F, 64.0F);
        Gui.drawModalRectWithCustomSizedTexture(x, y, -t * 0.7F, 16.0F, width, height, 64.0F, 64.0F);

        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    private void drawSegmentBubble(int x, int y, int w, int h, int seed) {
        if (w < 3 || h < 3) return;

        long time = System.currentTimeMillis();
        long phase = (time + seed * 173L) % 1400L;
        float k = (float) phase / 1400.0F;

        int bx = x + 1 + (seed % Math.max(1, w - 2));
        int by = y + h - 2 - Math.round(k * Math.max(1, h - 2));

        if (by < y + 1) by = y + 1;
        if (bx > x + w - 1) bx = x + w - 1;

        Gui.drawRect(bx, by, bx + 1, by + 1, 0x80FFFFFF);
    }

    private int getGuiLeft(GuiContainer gui) {
        try {
            java.lang.reflect.Field f = GuiContainer.class.getDeclaredField("guiLeft");
            f.setAccessible(true);
            return f.getInt(gui);
        } catch (Exception e) {
            return (gui.width - 176) / 2;
        }
    }

    private int getGuiTop(GuiContainer gui) {
        try {
            java.lang.reflect.Field f = GuiContainer.class.getDeclaredField("guiTop");
            f.setAccessible(true);
            return f.getInt(gui);
        } catch (Exception e) {
            return (gui.height - 166) / 2;
        }
    }
}
