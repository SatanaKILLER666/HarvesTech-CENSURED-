package com.ogryzok.lifecrusher.potion;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PotionStupor extends Potion {

    private static final ResourceLocation STUPOR_ICON =
            new ResourceLocation("harvestech", "textures/gui/potion_effects/stupor.png");

    public PotionStupor() {
        super(true, 0x6b4a2b);

        setRegistryName(new ResourceLocation("harvestech", "stupor"));
        setPotionName("effect.harvestech.stupor");

        registerPotionAttributeModifier(
                SharedMonsterAttributes.MOVEMENT_SPEED,
                "7b7f0f2d-8a9a-4e1a-bc7b-2c6a8d8b9f11",
                -0.3D,
                2
        );
    }

    @Override
    public boolean hasStatusIcon() {
        return false;
    }

    @Override
    public boolean isReady(int duration, int amplifier) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderInventoryEffect(int x, int y, PotionEffect effect, Minecraft mc) {
        renderCustomIcon(x + 6, y + 7, mc);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderHUDEffect(int x, int y, PotionEffect effect, Minecraft mc, float alpha) {
        renderCustomIcon(x + 3, y + 3, mc);
    }

    @SideOnly(Side.CLIENT)
    private void renderCustomIcon(int x, int y, Minecraft mc) {
        mc.getTextureManager().bindTexture(STUPOR_ICON);

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();

        Gui.drawModalRectWithCustomSizedTexture(
                x, y,
                0.0F, 0.0F,
                18, 18,
                18.0F, 18.0F
        );

        GlStateManager.disableBlend();
    }
}