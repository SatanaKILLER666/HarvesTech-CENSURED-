package com.ogryzok.disease.potion;

import com.ogryzok.harvestech;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PotionSeedKeeper extends Potion {
    private static final ResourceLocation ICON = new ResourceLocation(harvestech.MODID, "textures/gui/potion_effects/seed_keeper.png");

    public PotionSeedKeeper() {
        super(false, 0x8F58FF);
        setRegistryName(harvestech.MODID, "seed_keeper");
        setPotionName("effect." + harvestech.MODID + ".seed_keeper");
    }

    @Override
    public boolean hasStatusIcon() {
        return false;
    }

    @Override
    public boolean shouldRender(PotionEffect effect) {
        return true;
    }

    @Override
    public boolean shouldRenderHUD(PotionEffect effect) {
        return true;
    }

    @Override
    public boolean shouldRenderInvText(PotionEffect effect) {
        return true;
    }

    @Override
    public boolean isReady(int duration, int amplifier) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderInventoryEffect(int x, int y, PotionEffect effect, Minecraft mc) {
        renderIcon(x + 6, y + 7, mc);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderHUDEffect(int x, int y, PotionEffect effect, Minecraft mc, float alpha) {
        renderIcon(x + 3, y + 3, mc);
    }

    @SideOnly(Side.CLIENT)
    private void renderIcon(int x, int y, Minecraft mc) {
        mc.getTextureManager().bindTexture(ICON);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, 0.0F, 18, 18, 18.0F, 18.0F);
        GlStateManager.disableBlend();
    }
}
