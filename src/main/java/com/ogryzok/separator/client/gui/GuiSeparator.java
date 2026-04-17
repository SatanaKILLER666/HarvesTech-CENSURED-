package com.ogryzok.separator.client.gui;

import com.ogryzok.separator.container.ContainerSeparator;
import com.ogryzok.separator.tile.TileSeparator;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;

import java.util.ArrayList;
import java.util.List;

public class GuiSeparator extends GuiContainer {
    private final TileSeparator tile;

    public GuiSeparator(InventoryPlayer playerInv, TileSeparator tile) {
        super(new ContainerSeparator(playerInv, tile));
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
        fontRenderer.drawString(I18n.format("gui.harvestech.separator.title"), 8, 6, 0xF2F2F2);
        fontRenderer.drawString(I18n.format("gui.harvestech.common.inventory"), 8, 95, 0xD8D8D8);
        fontRenderer.drawString(I18n.format("gui.harvestech.common.fluid"), 30, 22, 0xD8D8D8);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        drawDefaultBackground();

        int x = guiLeft;
        int y = guiTop;
        int anim = (mc.player != null ? mc.player.ticksExisted : 0);

        drawRect(x, y, x + xSize, y + ySize, 0xFF0D0F12);
        drawGradientRect(x + 2, y + 2, x + xSize - 2, y + ySize - 2, 0xFF222830, 0xFF161A20);

        // Левая часть
        drawPanel(x + 6, y + 18, 20, 80);
        drawPanel(x + 28, y + 18, 36, 72);

        // Главная плашка продукта
        drawProductPanel(x + 80, y + 28, 66, 54);

        // Ровная аккуратная рамка остатка
        drawWasteSlotFrame(x + 151, y + 42);

        // Контент
        drawEnergyBar(x + 10, y + 28, 12, 58, tile.getEnergyStored(), tile.getMaxEnergyStored());
        drawEnrichedTank(x + 37, y + 34, 18, 48, tile.getInputStored(), tile.getMaxInput(), anim, tile.hasIc2BiomassInput(), tile.hasBioToxinInput());

        drawProgressArrow(x + 64, y + 50, 16, 10, tile.getProcessProgressScaled(16), tile.hasIc2BiomassInput(), tile.hasBioToxinInput());

        // Главный слот
        drawProductSlotFrame(x + 104, y + 46);


        // Остаток
        drawWaterTank(x + 153, y + 44, 8, 20, tile.getOutputStored(), tile.getMaxOutput(), anim + 11);
    }

    private void renderCustomTooltips(int mouseX, int mouseY) {
        if (isPointInRegion(10, 28, 12, 58, mouseX, mouseY)) {
            List<String> lines = new ArrayList<>();
            lines.add(tile.getEnergyStored() + " / " + tile.getMaxEnergyStored() + " FE");
            drawHoveringText(lines, mouseX, mouseY);
            return;
        }

        if (isPointInRegion(37, 34, 18, 48, mouseX, mouseY)) {
            List<String> lines = new ArrayList<>();
            lines.add(tile.getInputStored() > 0 ? (tile.hasIc2BiomassInput() ? I18n.format("gui.harvestech.common.ic2_biomass") + ": " : (tile.hasBioToxinInput() ? I18n.format("fluid.harvestech.bio_toxin") + ": " : I18n.format("fluid.harvestech.enriched_biomass") + ": ")) + tile.getInputStored() + " mB" : I18n.format("gui.harvestech.common.empty"));
            drawHoveringText(lines, mouseX, mouseY);
            return;
        }

        if (isPointInRegion(102, 46, 18, 18, mouseX, mouseY)) {
            List<String> lines = new ArrayList<>();
            if (tile.getStackInSlot(0).isEmpty()) {
                lines.add(I18n.format("gui.harvestech.separator.main_product"));
            } else {
                lines.add(tile.getStackInSlot(0).getItem() == com.ogryzok.food.FoodRegistry.EVAPORATED_BIOMASS ? I18n.format("item.harvestech.evaporated_biomass.name") : (tile.getStackInSlot(0).getItem() == com.ogryzok.food.FoodRegistry.TOXIC_BIOMASS ? I18n.format("item.harvestech.toxic_biomass.name") : I18n.format("item.harvestech.protein_biomass.name")));
            }
            drawHoveringText(lines, mouseX, mouseY);
            return;
        }

        if (isPointInRegion(151, 42, 12, 24, mouseX, mouseY)) {
            List<String> lines = new ArrayList<>();
            if (tile.getOutputStored() > 0) {
                lines.add(I18n.format("gui.harvestech.common.water") + ": " + tile.getOutputStored() + " mB");
            } else {
                lines.add(I18n.format("gui.harvestech.separator.waste"));
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

    private void drawProductPanel(int x, int y, int w, int h) {
        drawPanel(x, y, w, h);
    }

    private void drawProductSlotFrame(int x, int y) {
        drawRect(x, y, x + 18, y + 18, 0xFF050607);
        drawGradientRect(x + 1, y + 1, x + 17, y + 17, 0xFF303843, 0xFF1A1F26);
        drawRect(x + 2, y + 2, x + 16, y + 16, 0xFF11151A);
        drawRect(x + 2, y + 2, x + 16, y + 3, 0x55FFFFFF);
    }

    private void drawWasteSlotFrame(int x, int y) {
        // Внешняя рамка
        drawRect(x, y, x + 12, y + 24, 0xFF050607);

        // Основной фон рамки
        drawGradientRect(x + 1, y + 1, x + 11, y + 23, 0xFF303843, 0xFF1A1F26);

        // Внутреннее углубление
        drawRect(x + 2, y + 2, x + 10, y + 22, 0xFF11151A);

        // Верхний блик
        drawRect(x + 2, y + 2, x + 10, y + 3, 0x55FFFFFF);

        // Правая мягкая тень
        drawRect(x + 10, y + 3, x + 11, y + 22, 0x33000000);

        // Нижняя линия, чтобы не было обрыва снизу
        drawRect(x + 2, y + 21, x + 10, y + 22, 0x66000000);
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

    private void drawProgressArrow(int x, int y, int w, int h, int progress, boolean ic2Biomass, boolean bioToxin) {
        drawRect(x, y, x + w, y + h, 0xFF07090C);
        drawGradientRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF202733, 0xFF12171E);

        int totalMax = w - 2;
        int fill = Math.min(totalMax, Math.max(0, progress));
        if (fill <= 0) return;

        int bodyEnd = x + 1 + Math.min(fill, w - 6);

        // IC2 biomass -> болотная стрелка
        // Enriched biomass -> более светлая молочно-кремовая стрелка
        int bodyTop = ic2Biomass ? 0xFF5A7F4A : (bioToxin ? 0xFF4E1018 : 0xFFF7F1DE);
        int bodyBottom = ic2Biomass ? 0xFF4F7244 : (bioToxin ? 0xFF2D070D : 0xFFE6D9B8);

        drawGradientRect(x + 1, y + 2, bodyEnd, y + h - 2, bodyTop, bodyBottom);

        int headStart = x + w - 5;
        int headFill = fill - (w - 6);
        if (headFill > 0) {
            int headColor = ic2Biomass ? 0xFF5A7F4A : (bioToxin ? 0xFF631722 : 0xFFF9F4E5);
            if (headFill >= 1) drawRect(headStart, y + 4, headStart + 1, y + h - 4, headColor);
            if (headFill >= 2) drawRect(headStart + 1, y + 3, headStart + 2, y + h - 3, headColor);
            if (headFill >= 3) drawRect(headStart + 2, y + 2, headStart + 3, y + h - 2, headColor);
        }
    }

    private void drawEnrichedTank(int x, int y, int w, int h, int amount, int capacity, int anim, boolean ic2Biomass, boolean bioToxin) {
        drawRect(x, y, x + w, y + h, 0xFF050607);
        drawGradientRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF10151B, 0xFF080B0F);

        int innerH = h - 2;
        int fill = capacity <= 0 ? 0 : amount * innerH / capacity;
        if (fill <= 0) return;

        int fy1 = y + 1 + (innerH - fill);
        int fy2 = y + h - 1;

        if (ic2Biomass) {
            drawGradientRect(x + 1, fy1, x + w - 1, fy2, 0xFF547C44, 0xFF4F7244);
        } else if (bioToxin) {
            drawGradientRect(x + 1, fy1, x + w - 1, fy2, 0xFF55131D, 0xFF2E090F);
        } else {
            drawGradientRect(x + 1, fy1, x + w - 1, fy2, 0xFFE8F7FF, 0xFFBFDDF1);
        }

        for (int yy = fy1 + 2; yy < fy2; yy += 6) {
            int shift = (anim + yy) % 6;
            drawRect(x + 2 + shift / 2, yy, x + w - 2, Math.min(yy + 1, fy2), ic2Biomass ? 0x22446037 : (bioToxin ? 0x22240A10 : 0x22BFCBDA));
        }

        int shimmer = anim % (w + 6);
        int sx1 = x - 3 + shimmer;
        int sx2 = sx1 + 3;
        if (sx2 > x + 1 && sx1 < x + w - 1) {
            drawGradientRect(Math.max(x + 1, sx1), fy1, Math.min(x + w - 1, sx2), fy2, 0x00FFFFFF, ic2Biomass ? 0x446C9857 : (bioToxin ? 0x334B1822 : 0x66FFFFFF));
        }

        drawRect(x + 2, fy1 + 1, x + w - 2, Math.min(fy1 + 3, fy2), ic2Biomass ? 0x5577A35D : (bioToxin ? 0x554F1822 : 0x55FFFFFF));
        drawRect(x + w - 3, fy1 + 1, x + w - 2, fy2 - 1, ic2Biomass ? 0x305A7F4A : (bioToxin ? 0x30260A10 : 0x30FFFFFF));
    }

    private void drawWaterTank(int x, int y, int w, int h, int amount, int capacity, int anim) {
        drawRect(x, y, x + w, y + h, 0xFF050607);
        drawGradientRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF101926, 0xFF090E16);

        int innerH = h - 2;
        int fill = capacity <= 0 ? 0 : amount * innerH / capacity;
        if (fill <= 0) return;

        int fy1 = y + 1 + (innerH - fill);
        int fy2 = y + h - 1;

        drawGradientRect(x + 1, fy1, x + w - 1, fy2, 0xFF7AD7FF, 0xFF1F6FD6);

        for (int yy = fy1 + 2; yy < fy2; yy += 4) {
            int shift = (anim + yy) % 4;
            drawRect(x + 1 + shift / 2, yy, x + w - 1, Math.min(yy + 1, fy2), 0x33DDF7FF);
        }

        drawRect(x + 1, fy1 + 1, x + w - 1, Math.min(fy1 + 2, fy2), 0x55F5FEFF);

        if ((anim / 6) % 2 == 0) {
            drawRect(x + w - 2, fy1 + 3, x + w - 1, fy2 - 1, 0x33FFFFFF);
        }
    }
}