package com.ogryzok.semenenrichment.client.gui;

import com.ogryzok.semenenrichment.container.ContainerSemenEnrichmentChamber;
import com.ogryzok.semenenrichment.tile.TileSemenEnrichmentChamber;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;

import java.util.ArrayList;
import java.util.List;

public class GuiSemenEnrichmentChamber extends GuiContainer {
    private final TileSemenEnrichmentChamber tile;

    public GuiSemenEnrichmentChamber(InventoryPlayer playerInv, TileSemenEnrichmentChamber tile) {
        super(new ContainerSemenEnrichmentChamber(playerInv, tile));
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
        fontRenderer.drawString(I18n.format("gui.harvestech.enrichment.title"), 8, 6, 0xF2F2F2);
        fontRenderer.drawString(I18n.format("gui.harvestech.destiller.output"), 31, 24, 0xCFCFCF);
        fontRenderer.drawString(I18n.format("gui.harvestech.enrichment.enriched_short"), 95, 24, 0xCFCFCF);
        fontRenderer.drawString(I18n.format("gui.harvestech.common.inventory"), 8, 95, 0xD8D8D8);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        drawDefaultBackground();

        int x = guiLeft;
        int y = guiTop;
        int anim = (mc.player != null ? mc.player.ticksExisted : 0);

        drawRect(x, y, x + xSize, y + ySize, 0xFF0D0F12);
        drawGradientRect(x + 2, y + 2, x + xSize - 2, y + ySize - 2, 0xFF222830, 0xFF161A20);

        drawPanel(x + 6, y + 18, 20, 80);
        drawPanel(x + 28, y + 18, 36, 72);
        drawPanel(x + 92, y + 18, 36, 72);

        drawEnergyBar(x + 10, y + 28, 12, 58, tile.getEnergyStored(), tile.getMaxEnergyStored());
        boolean fermentedRecipe = tile.hasDistilledFermentedInput() || tile.hasIc2BiomassOutput();
        boolean necroRecipe = tile.hasNecroSubstrateInput() || tile.hasBioToxinOutput();
        drawFluidTankAnimated(x + 37, y + 34, 18, 48, tile.getInputStored(), tile.getMaxInput(), anim, fermentedRecipe, false, tile.hasNecroSubstrateInput(), false);
        drawFluidTankAnimated(x + 101, y + 34, 18, 48, tile.getOutputStored(), tile.getMaxOutput(), anim + 11, false, tile.hasIc2BiomassOutput(), false, tile.hasBioToxinOutput());

        drawProgressArrow(x + 64, y + 50, 28, 10, tile.getProcessProgressScaled(28), fermentedRecipe, necroRecipe);

        // рисованные слоты теперь совпадают с реальными вертикальными
        drawSlotFrame(x + 130, y + 56);
        drawSlotFrame(x + 130, y + 78);

        int canProgress = tile.getCanFillProgressScaled(16);
        if (canProgress > 0) {
            drawGradientRect(x + 130, y + 94 - canProgress, x + 146, y + 94, 0x66FFFFFF, 0x22FFFFFF);
        }
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
            if (tile.getInputStored() > 0) {
                lines.add((tile.hasDistilledFermentedInput() ? I18n.format("fluid.harvestech.distilled_fermented_semen") + ": " : tile.hasNecroSubstrateInput() ? I18n.format("fluid.harvestech.necro_substrate") + ": " : I18n.format("fluid.harvestech.distilled_biomass") + ": ") + tile.getInputStored() + " mB");
            } else {
                lines.add(I18n.format("gui.harvestech.common.empty"));
            }
            drawHoveringText(lines, mouseX, mouseY);
            return;
        }

        if (isPointInRegion(101, 34, 18, 48, mouseX, mouseY)) {
            List<String> lines = new ArrayList<>();
            if (tile.getOutputStored() > 0) {
                lines.add((tile.hasIc2BiomassOutput() ? I18n.format("gui.harvestech.common.ic2_biomass") + ": " : tile.hasBioToxinOutput() ? I18n.format("fluid.harvestech.bio_toxin") + ": " : I18n.format("fluid.harvestech.enriched_biomass") + ": ") + tile.getOutputStored() + " mB");
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

    private void drawProgressArrow(int x, int y, int w, int h, int progress, boolean fermentedRecipe, boolean necroRecipe) {
        drawRect(x, y, x + w, y + h, 0xFF07090C);
        drawGradientRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF202733, 0xFF12171E);

        int totalMax = w - 2;
        int fill = Math.min(totalMax, Math.max(0, progress));
        if (fill <= 0) return;

        int bodyEnd = x + 1 + Math.min(fill, w - 8);
        int bodyTop = necroRecipe ? 0xFF6A1824 : (fermentedRecipe ? 0xFF6C9857 : 0xFFE8EDF2);
        int bodyBottom = necroRecipe ? 0xFF52131D : (fermentedRecipe ? 0xFF4F7244 : 0xFF9FB4C9);
        drawGradientRect(x + 1, y + 2, bodyEnd, y + h - 2, bodyTop, bodyBottom);

        int headStart = x + w - 7;
        int headFill = fill - (w - 8);
        if (headFill > 0) {
            int headColor = necroRecipe ? 0xFF7A2030 : (fermentedRecipe ? 0xFF77A35D : 0xFFDCE6F0);
            if (headFill >= 1) drawRect(headStart, y + 4, headStart + 1, y + h - 4, headColor);
            if (headFill >= 2) drawRect(headStart + 1, y + 3, headStart + 2, y + h - 3, headColor);
            if (headFill >= 3) drawRect(headStart + 2, y + 2, headStart + 3, y + h - 2, headColor);
            if (headFill >= 4) drawRect(headStart + 3, y + 3, headStart + 4, y + h - 3, headColor);
            if (headFill >= 5) drawRect(headStart + 4, y + 4, headStart + 5, y + h - 4, headColor);
        }
    }

    private void drawFluidTankAnimated(int x, int y, int w, int h, int amount, int capacity, int anim, boolean fermentedInput, boolean ic2Output, boolean necroInput, boolean bioToxinOutput) {
        drawRect(x, y, x + w, y + h, 0xFF050607);
        drawGradientRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF10151B, 0xFF080B0F);

        int innerH = h - 2;
        int fill = capacity <= 0 ? 0 : amount * innerH / capacity;
        if (fill <= 0) return;

        int fy1 = y + 1 + (innerH - fill);
        int fy2 = y + h - 1;

        int topColor;
        int bottomColor;
        int stripeColor;
        int shimmerColor;
        int glossColor;
        int edgeColor;

        if (bioToxinOutput) {
            topColor = 0xFF6A1824;
            bottomColor = 0xFF52131D;
            stripeColor = 0x22350A11;
            shimmerColor = 0x444F1620;
            glossColor = 0x556A1D29;
            edgeColor = 0x304A121A;
        } else if (necroInput) {
            topColor = 0xFFBF566B;
            bottomColor = 0xFFB34A5E;
            stripeColor = 0x22642A36;
            shimmerColor = 0x44CF7487;
            glossColor = 0x55D88798;
            edgeColor = 0x308B3748;
        } else if (ic2Output) {
            topColor = 0xFF547C44;
            bottomColor = 0xFF4F7244;
            stripeColor = 0x22446037;
            shimmerColor = 0x446C9857;
            glossColor = 0x5577A35D;
            edgeColor = 0x305A7F4A;
        } else if (fermentedInput) {
            topColor = 0xFF9AA69A;
            bottomColor = 0xFF879387;
            stripeColor = 0x224F5C4F;
            shimmerColor = 0x449FB19F;
            glossColor = 0x55C1CDC1;
            edgeColor = 0x307B8B7B;
        } else {
            topColor = 0xFFF4F7FB;
            bottomColor = 0xFFD8DEE7;
            stripeColor = 0x22BFCBDA;
            shimmerColor = 0x66FFFFFF;
            glossColor = 0x55FFFFFF;
            edgeColor = 0x30FFFFFF;
        }

        drawGradientRect(x + 1, fy1, x + w - 1, fy2, topColor, bottomColor);

        for (int yy = fy1 + 2; yy < fy2; yy += 6) {
            int shift = (anim + yy) % 6;
            drawRect(x + 2 + shift / 2, yy, x + w - 2, Math.min(yy + 1, fy2), stripeColor);
        }

        int shimmer = anim % (w + 6);
        int sx1 = x - 3 + shimmer;
        int sx2 = sx1 + 3;
        if (sx2 > x + 1 && sx1 < x + w - 1) {
            drawGradientRect(Math.max(x + 1, sx1), fy1, Math.min(x + w - 1, sx2), fy2, 0x00FFFFFF, shimmerColor);
        }

        drawRect(x + 2, fy1 + 1, x + w - 2, Math.min(fy1 + 3, fy2), glossColor);
        drawRect(x + w - 3, fy1 + 1, x + w - 2, fy2 - 1, edgeColor);
    }
}