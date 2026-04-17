package com.ogryzok.semencentrifuge.client.gui;

import com.ogryzok.semencentrifuge.container.ContainerSemenCentrifugeBase;
import com.ogryzok.semencentrifuge.tile.TileSemenCentrifugeBase;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;

import java.util.ArrayList;
import java.util.List;

public class GuiSemenCentrifugeBase extends GuiContainer {
    private final TileSemenCentrifugeBase tile;

    public GuiSemenCentrifugeBase(InventoryPlayer playerInv, TileSemenCentrifugeBase tile) {
        super(new ContainerSemenCentrifugeBase(playerInv, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 175;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderCustomTooltips(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRenderer.drawString(I18n.format("gui.harvestech.centrifuge.title"), 8, 6, 0xF2F2F2);
        this.fontRenderer.drawString(I18n.format("gui.harvestech.common.inventory"), 8, 80, 0xD8D8D8);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        drawDefaultBackground();

        int x = this.guiLeft;
        int y = this.guiTop;
        int anim = (mc.player != null ? mc.player.ticksExisted : 0);

        drawRect(x, y, x + this.xSize, y + this.ySize, 0xFF0D0F12);
        drawGradientRect(x + 2, y + 2, x + this.xSize - 2, y + this.ySize - 2, 0xFF222830, 0xFF161A20);

        drawPanel(x + 8, y + 18, 52, 52);
        drawPanel(x + 66, y + 18, 52, 52);
        drawPanel(x + 122, y + 18, 24, 52);

        drawStorageBarIndustrial(x + 16, y + 40, 36, 14, tile.getBiomassStored(), tile.getMaxBiomassStored(), anim, true);
        drawStorageBarIndustrial(x + 74, y + 40, 36, 14, tile.getEnergyStored(), tile.getMaxEnergyStored(), anim, false);


        drawSlotFrame(x + 126, y + 23);
        drawSlotFrame(x + 126, y + 48);
    }

    private void renderCustomTooltips(int mouseX, int mouseY) {
        if (isPointInRegion(16, 40, 36, 14, mouseX, mouseY)) {
            List<String> lines = new ArrayList<>();
            if (tile.getBiomassStored() > 0) {
                lines.add(I18n.format("fluid.harvestech.semen") + ": " + tile.getBiomassStored() + " mB");
            } else {
                lines.add(I18n.format("gui.harvestech.common.empty"));
            }
            drawHoveringText(lines, mouseX, mouseY);
            return;
        }

        if (isPointInRegion(74, 40, 36, 14, mouseX, mouseY)) {
            List<String> lines = new ArrayList<>();
            lines.add(tile.getEnergyStored() + " / " + tile.getMaxEnergyStored() + " FE");
            drawHoveringText(lines, mouseX, mouseY);
        }
    }

    private void drawPanel(int x, int y, int w, int h) {
        drawRect(x, y, x + w, y + h, 0xFF050607);
        drawGradientRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF303843, 0xFF1A1F26);
        drawRect(x + 1, y + 1, x + w - 1, y + 2, 0x55FFFFFF);
        drawRect(x + 1, y + h - 2, x + w - 1, y + h - 1, 0x66000000);
    }

    private void drawSlotFrame(int x, int y) {
        drawRect(x, y, x + 18, y + 18, 0xFF050607);
        drawGradientRect(x + 1, y + 1, x + 17, y + 17, 0xFF4A5563, 0xFF212833);
        drawRect(x + 2, y + 2, x + 16, y + 16, 0xFF11151A);
        drawRect(x + 2, y + 2, x + 16, y + 3, 0x33FFFFFF);
    }

    private void drawStorageBarIndustrial(int x, int y, int width, int height, int value, int maxValue, int anim, boolean fluid) {
        drawRect(x, y, x + width, y + height, 0xFF050607);
        drawGradientRect(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF10151B, 0xFF080B0F);

        int innerW = width - 2;
        int fill = maxValue <= 0 ? 0 : Math.max(0, Math.min(innerW, Math.round((value / (float) maxValue) * innerW)));
        if (fill <= 0) return;

        if (fluid) {
            drawGradientRect(x + 1, y + 1, x + 1 + fill, y + height - 1, 0xFFF4F7FB, 0xFFD8DEE7);

            for (int xx = x + 2; xx < x + 1 + fill; xx += 6) {
                int shift = (anim + xx) % 6;
                drawRect(xx, y + 2 + shift / 3, Math.min(xx + 2, x + 1 + fill), y + height - 2, 0x22BFCBDA);
            }

            int shimmer = anim % (width + 8);
            int sx1 = x - 3 + shimmer;
            int sx2 = sx1 + 4;
            if (sx2 > x + 1 && sx1 < x + 1 + fill) {
                drawGradientRect(Math.max(x + 1, sx1), y + 1, Math.min(x + 1 + fill, sx2), y + height - 1, 0x00FFFFFF, 0x66FFFFFF);
            }

            drawRect(x + 2, y + 2, Math.min(x + 1 + fill, x + 6), y + height - 2, 0x55FFFFFF);
        } else {
            drawGradientRect(x + 1, y + 1, x + 1 + fill, y + height - 1, 0xFF5AFF7D, 0xFF1FAE46);
            drawRect(x + 2, y + 2, Math.min(x + 1 + fill, x + width - 2), y + 4, 0x66FFFFFF);
        }
    }
}