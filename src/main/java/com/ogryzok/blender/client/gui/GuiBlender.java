package com.ogryzok.blender.client.gui;

import com.ogryzok.blender.container.ContainerBlender;
import com.ogryzok.blender.tile.TileBlender;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;

import java.util.ArrayList;
import java.util.List;

public class GuiBlender extends GuiContainer {
    private final TileBlender tile;

    public GuiBlender(InventoryPlayer playerInv, TileBlender tile) {
        super(new ContainerBlender(playerInv, tile));
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
        fontRenderer.drawString(I18n.format("gui.harvestech.blender.title"), 8, 6, 0xF2F2F2);
        fontRenderer.drawString(I18n.format("gui.harvestech.common.input"), 35, 24, 0xCFCFCF);
        fontRenderer.drawString(I18n.format("gui.harvestech.common.output"), 101, 24, 0xCFCFCF);
        fontRenderer.drawString(I18n.format("gui.harvestech.common.inventory"), 8, 95, 0xD8D8D8);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        drawDefaultBackground();

        int x = guiLeft;
        int y = guiTop;
        int anim = mc.player != null ? mc.player.ticksExisted : 0;

        drawRect(x, y, x + xSize, y + ySize, 0xFF0D0F12);
        drawGradientRect(x + 2, y + 2, x + xSize - 2, y + ySize - 2, 0xFF222830, 0xFF161A20);

        drawPanel(x + 6, y + 18, 20, 72);
        drawPanel(x + 28, y + 18, 36, 72);
        drawPanel(x + 68, y + 42, 24, 18);
        drawPanel(x + 96, y + 18, 36, 72);
        drawPanel(x + 6, y + 104, 164, 80);

        drawEnergyBar(x + 10, y + 26, 12, 56, tile.getEnergyStored(), tile.getMaxEnergyStored());
        drawSlotFrame(x + 37, y + 50);
        drawProgressArrow(x + 70, y + 46, 20, 10, tile.getProgressScaled(20));
        drawTankFrame(x + 105, y + 34, 18, 42);
        drawFluidTank(x + 107, y + 36, 14, 38, tile.getOutputStored(), tile.getMaxOutput(), anim);
    }

    private void renderCustomTooltips(int mouseX, int mouseY) {
        if (isPointInRegion(10, 26, 12, 56, mouseX, mouseY)) {
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

        if (isPointInRegion(105, 34, 18, 42, mouseX, mouseY)) {
            List<String> lines = new ArrayList<>();
            if (tile.getOutputStored() > 0) {
                lines.add(I18n.format("fluid.harvestech.toxic_flesh") + ": " + tile.getOutputStored() + " mB");
            } else {
                lines.add(I18n.format("gui.harvestech.common.empty"));
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

    private void drawTankFrame(int x, int y, int w, int h) {
        drawRect(x, y, x + w, y + h, 0xFF050607);
        drawGradientRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF4A5563, 0xFF212833);
        drawRect(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF11151A);
        drawRect(x + 2, y + 2, x + w - 2, y + 3, 0x33FFFFFF);
    }

    private void drawEnergyBar(int x, int y, int w, int h, int stored, int max) {
        drawRect(x, y, x + w, y + h, 0xFF050607);
        drawRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF0F1318);

        int inner = h - 2;
        int fill = max <= 0 ? 0 : stored * inner / max;
        if (fill > 0) {
            drawGradientRect(x + 1, y + 1 + (inner - fill), x + w - 1, y + h - 1, 0xFF5AFF7D, 0xFF1FAE46);
            drawRect(x + 2, y + 2 + (inner - fill), x + w - 2, Math.min(y + 4 + (inner - fill), y + h - 1), 0x66FFFFFF);
        }
    }

    private void drawProgressArrow(int x, int y, int w, int h, int progress) {
        drawRect(x, y, x + w, y + h, 0xFF07090C);
        drawGradientRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF202733, 0xFF12171E);

        int totalMax = w - 2;
        int fill = Math.min(totalMax, Math.max(0, progress));
        if (fill <= 0) return;

        int bodyEnd = x + 1 + Math.min(fill, w - 6);
        drawGradientRect(x + 1, y + 2, bodyEnd, y + h - 2, 0xFFF2D6DB, 0xFFAA5968);

        int headStart = x + w - 5;
        int headFill = fill - (w - 6);
        if (headFill > 0) {
            int headColor = 0xFFD58A98;
            if (headFill >= 1) drawRect(headStart, y + 4, headStart + 1, y + h - 4, headColor);
            if (headFill >= 2) drawRect(headStart + 1, y + 3, headStart + 2, y + h - 3, headColor);
            if (headFill >= 3) drawRect(headStart + 2, y + 2, headStart + 3, y + h - 2, headColor);
        }
    }

    private void drawFluidTank(int x, int y, int w, int h, int amount, int capacity, int anim) {
        drawRect(x, y, x + w, y + h, 0xFF050607);
        drawGradientRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF160B10, 0xFF0B0608);

        int innerH = h - 2;
        int fill = capacity <= 0 ? 0 : amount * innerH / capacity;
        if (fill <= 0) return;

        int fy1 = y + 1 + (innerH - fill);
        int fy2 = y + h - 1;

        drawGradientRect(x + 1, fy1, x + w - 1, fy2, 0xFF8F293C, 0xFF4F111B);

        for (int yy = fy1 + 2; yy < fy2; yy += 5) {
            int shift = (anim + yy) % 5;
            drawRect(x + 2 + shift / 2, yy, x + w - 2, Math.min(yy + 1, fy2), 0x337C2333);
        }

        drawRect(x + 1, fy1 + 1, x + w - 1, Math.min(fy1 + 2, fy2), 0x44D98D9C);
        drawRect(x + w - 2, fy1 + 2, x + w - 1, fy2 - 1, 0x22000000);
    }
}
