package com.ogryzok.proteinformer.client.gui;

import com.ogryzok.proteinformer.container.ContainerProteinFormer;
import com.ogryzok.proteinformer.tile.TileProteinFormer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;

import java.util.ArrayList;
import java.util.List;

public class GuiProteinFormer extends GuiContainer {
    private final TileProteinFormer tile;

    public GuiProteinFormer(InventoryPlayer playerInv, TileProteinFormer tile) {
        super(new ContainerProteinFormer(playerInv, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 190;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderCustomTooltips(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("gui.harvestech.protein_former.title"), 8, 6, 0xF2F2F2);
        fontRenderer.drawString(I18n.format("gui.harvestech.common.input"), 37, 24, 0xCFCFCF);
        fontRenderer.drawString(I18n.format("gui.harvestech.common.output"), 99, 24, 0xCFCFCF);
        fontRenderer.drawString(I18n.format("gui.harvestech.common.inventory"), 8, 95, 0xD8D8D8);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        drawDefaultBackground();

        int x = guiLeft;
        int y = guiTop;

        drawRect(x, y, x + xSize, y + ySize, 0xFF0D0F12);
        drawGradientRect(x + 2, y + 2, x + xSize - 2, y + ySize - 2, 0xFF222830, 0xFF161A20);

        drawPanel(x + 6, y + 18, 20, 80);
        drawPanel(x + 28, y + 18, 36, 72);
        drawPanel(x + 92, y + 18, 36, 72);

        drawEnergyBar(x + 10, y + 28, 12, 58, tile.getEnergyStored(), tile.getMaxEnergyStored());
        drawSlotFrame(x + 37, y + 50);
        drawSlotFrame(x + 101, y + 50);
        drawProgressArrow(x + 64, y + 54, 28, 10, tile.getProcessProgressScaled(28));
    }

    private void renderCustomTooltips(int mouseX, int mouseY) {
        if (isPointInRegion(10, 28, 12, 58, mouseX, mouseY)) {
            List<String> lines = new ArrayList<>();
            lines.add(tile.getEnergyStored() + " / " + tile.getMaxEnergyStored() + " FE");
            drawHoveringText(lines, mouseX, mouseY);
            return;
        }

        if (isPointInRegion(37, 50, 18, 18, mouseX, mouseY)) {
            List<String> lines = new ArrayList<>();
            if (tile.getStackInSlot(0).isEmpty()) {
                lines.add(I18n.format("gui.harvestech.common.empty"));
            } else {
                lines.add(tile.getStackInSlot(0).getDisplayName() + ": " + tile.getStackInSlot(0).getCount());
            }
            drawHoveringText(lines, mouseX, mouseY);
            return;
        }

        if (isPointInRegion(101, 50, 18, 18, mouseX, mouseY)) {
            List<String> lines = new ArrayList<>();
            if (tile.getStackInSlot(1).isEmpty()) {
                lines.add(I18n.format("gui.harvestech.common.empty"));
            } else {
                lines.add(tile.getStackInSlot(1).getDisplayName() + ": " + tile.getStackInSlot(1).getCount());
            }
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

    private void drawEnergyBar(int x, int y, int w, int h, int stored, int max) {
        drawRect(x, y, x + w, y + h, 0xFF050607);
        drawRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF0F1318);

        int inner = h - 2;
        int fill = max <= 0 ? 0 : stored * inner / max;
        if (fill > 0) {
            drawGradientRect(x + 1, y + 1 + (inner - fill), x + w - 1, y + h - 1, 0xFF5AFF7D, 0xFF1FAE46);
            drawRect(x + 2, y + 2 + (inner - fill), x + w - 2, y + 4 + (inner - fill), 0x66FFFFFF);
        }
    }

    private void drawProgressArrow(int x, int y, int w, int h, int progress) {
        drawRect(x, y, x + w, y + h, 0xFF07090C);
        drawGradientRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF202733, 0xFF12171E);

        int totalMax = w - 2;
        int fill = Math.min(totalMax, Math.max(0, progress));
        if (fill <= 0) return;

        int bodyEnd = x + 1 + Math.min(fill, w - 8);
        drawGradientRect(x + 1, y + 2, bodyEnd, y + h - 2, 0xFFE8EDF2, 0xFF9FB4C9);

        int headStart = x + w - 7;
        int headFill = fill - (w - 8);
        if (headFill > 0) {
            if (headFill >= 1) drawRect(headStart, y + 4, headStart + 1, y + h - 4, 0xFFDCE6F0);
            if (headFill >= 2) drawRect(headStart + 1, y + 3, headStart + 2, y + h - 3, 0xFFDCE6F0);
            if (headFill >= 3) drawRect(headStart + 2, y + 2, headStart + 3, y + h - 2, 0xFFDCE6F0);
            if (headFill >= 4) drawRect(headStart + 3, y + 3, headStart + 4, y + h - 3, 0xFFDCE6F0);
            if (headFill >= 5) drawRect(headStart + 4, y + 4, headStart + 5, y + h - 4, 0xFFDCE6F0);
        }
    }
}
