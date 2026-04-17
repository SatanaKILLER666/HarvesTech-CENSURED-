package com.ogryzok.disease.potion;

import com.ogryzok.harvestech;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PotionDiseaseStage extends Potion {
    private static final ResourceLocation ICONS = new ResourceLocation(harvestech.MODID, "textures/gui/potion_effects/disease_stages.png");

    private final int iconIndex;

    public PotionDiseaseStage(String name, int liquidColor, int iconIndex, String healthUuid, double maxHealthMultiplier, String speedUuid, double moveSpeedMultiplier) {
        super(true, liquidColor);
        this.iconIndex = iconIndex;
        setRegistryName(name);
        setPotionName("effect." + harvestech.MODID + "." + name);

        if (maxHealthMultiplier != 1.0D) {
            registerPotionAttributeModifier(SharedMonsterAttributes.MAX_HEALTH, healthUuid, maxHealthMultiplier - 1.0D, 2);
        }
        if (moveSpeedMultiplier != 1.0D) {
            registerPotionAttributeModifier(SharedMonsterAttributes.MOVEMENT_SPEED, speedUuid, moveSpeedMultiplier - 1.0D, 2);
        }
    }

    @Override
    public boolean hasStatusIcon() {
        return false;
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
        mc.getTextureManager().bindTexture(ICONS);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        Gui.drawModalRectWithCustomSizedTexture(x, y, iconIndex * 18, 0, 18, 18, 72, 18);
        GlStateManager.disableBlend();
    }
}
