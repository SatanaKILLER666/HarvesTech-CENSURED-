package com.ogryzok.semendestiller.client.gui;

import com.ogryzok.fluids.ModFluids;
import com.ogryzok.semendestiller.container.ContainerSemenDestiller;
import com.ogryzok.semendestiller.tile.TileSemenDestillerBase;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public class GuiSemenDestiller extends GuiContainer {
    private final TileSemenDestillerBase tile;

    public GuiSemenDestiller(InventoryPlayer playerInv, TileSemenDestillerBase tile) {
        super(new ContainerSemenDestiller(playerInv, tile));
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
        fontRenderer.drawString(I18n.format("gui.harvestech.destiller.title"), 8, 6, 0xF2F2F2);
        fontRenderer.drawString(I18n.format("gui.harvestech.destiller.input"), 30, 24, 0xCFCFCF);
        fontRenderer.drawString(I18n.format("gui.harvestech.destiller.output"), 95, 24, 0xCFCFCF);
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
        drawFluidTankAnimated(x + 37, y + 34, 18, 48, tile.getBiomassStored(), tile.getMaxInput(), anim,
                getFluidPrimaryColor(tile.getBiomassFluid()), getFluidSecondaryColor(tile.getBiomassFluid()));
        drawFluidTankAnimated(x + 101, y + 34, 18, 48, tile.getDistilledStored(), tile.getMaxOutput(), anim + 11,
                getFluidPrimaryColor(tile.getDistilledFluid()), getFluidSecondaryColor(tile.getDistilledFluid()));

        drawProgressArrow(x + 64, y + 50, 28, 10, tile.getProcessProgressScaled(28), getArrowStartColor(), getArrowEndColor());

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
            if (tile.getBiomassStored() > 0) {
                lines.add(getInputFluidName() + ": " + tile.getBiomassStored() + " mB");
            } else {
                lines.add(I18n.format("gui.harvestech.common.empty"));
            }
            drawHoveringText(lines, mouseX, mouseY);
            return;
        }

        if (isPointInRegion(101, 34, 18, 48, mouseX, mouseY)) {
            List<String> lines = new ArrayList<>();
            if (tile.getDistilledStored() > 0) {
                lines.add(getOutputFluidName() + ": " + tile.getDistilledStored() + " mB");
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

    private void drawProgressArrow(int x, int y, int w, int h, int progress, int bodyStartColor, int bodyEndColor) {
        drawRect(x, y, x + w, y + h, 0xFF07090C);
        drawGradientRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF202733, 0xFF12171E);

        int totalMax = w - 2;
        int fill = Math.min(totalMax, Math.max(0, progress));
        if (fill <= 0) return;

        int bodyEnd = x + 1 + Math.min(fill, w - 8);
        drawGradientRect(x + 1, y + 2, bodyEnd, y + h - 2, bodyStartColor, bodyEndColor);

        int headStart = x + w - 7;
        int headFill = fill - (w - 8);
        if (headFill > 0) {
            if (headFill >= 1) drawRect(headStart, y + 4, headStart + 1, y + h - 4, bodyStartColor);
            if (headFill >= 2) drawRect(headStart + 1, y + 3, headStart + 2, y + h - 3, bodyStartColor);
            if (headFill >= 3) drawRect(headStart + 2, y + 2, headStart + 3, y + h - 2, bodyStartColor);
            if (headFill >= 4) drawRect(headStart + 3, y + 3, headStart + 4, y + h - 3, bodyStartColor);
            if (headFill >= 5) drawRect(headStart + 4, y + 4, headStart + 5, y + h - 4, bodyStartColor);
        }
    }

    private void drawFluidTankAnimated(int x, int y, int w, int h, int amount, int capacity, int anim, int fluidTopColor, int fluidBottomColor) {
        drawRect(x, y, x + w, y + h, 0xFF050607);
        drawGradientRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF10151B, 0xFF080B0F);

        int innerH = h - 2;
        int fill = capacity <= 0 ? 0 : amount * innerH / capacity;
        if (fill <= 0) return;

        int fy1 = y + 1 + (innerH - fill);
        int fy2 = y + h - 1;

        drawGradientRect(x + 1, fy1, x + w - 1, fy2, fluidTopColor, fluidBottomColor);

        for (int yy = fy1 + 2; yy < fy2; yy += 6) {
            int shift = (anim + yy) % 6;
            drawRect(x + 2 + shift / 2, yy, x + w - 2, Math.min(yy + 1, fy2), 0x22BFCBDA);
        }

        int shimmer = anim % (w + 6);
        int sx1 = x - 3 + shimmer;
        int sx2 = sx1 + 3;
        if (sx2 > x + 1 && sx1 < x + w - 1) {
            drawGradientRect(Math.max(x + 1, sx1), fy1, Math.min(x + w - 1, sx2), fy2, 0x00FFFFFF, 0x66FFFFFF);
        }

        drawRect(x + 2, fy1 + 1, x + w - 2, Math.min(fy1 + 3, fy2), 0x55FFFFFF);
        drawRect(x + w - 3, fy1 + 1, x + w - 2, fy2 - 1, 0x30FFFFFF);
    }

    private int getFluidPrimaryColor(FluidStack stack) {
        if (stack != null && stack.getFluid() == ModFluids.FERMENTED_SEMEN) {
            return 0xFFA4ADA4;
        }
        if (stack != null && stack.getFluid() == ModFluids.DISTILLED_FERMENTED_SEMEN) {
            return 0xFFB7C1B7;
        }
        if (stack != null && stack.getFluid() == ModFluids.TOXIC_FLESH) {
            return 0xFF9A3447;
        }
        if (stack != null && stack.getFluid() == ModFluids.NECRO_SUBSTRATE) {
            return 0xFFC66075;
        }
        return 0xFFF4F7FB;
    }

    private int getFluidSecondaryColor(FluidStack stack) {
        if (stack != null && stack.getFluid() == ModFluids.FERMENTED_SEMEN) {
            return 0xFF8D968D;
        }
        if (stack != null && stack.getFluid() == ModFluids.DISTILLED_FERMENTED_SEMEN) {
            return 0xFFA3ADA3;
        }
        if (stack != null && stack.getFluid() == ModFluids.TOXIC_FLESH) {
            return 0xFF7A2030;
        }
        if (stack != null && stack.getFluid() == ModFluids.NECRO_SUBSTRATE) {
            return 0xFFB34A5E;
        }
        return 0xFFD8DEE7;
    }

    private int getArrowStartColor() {
        FluidStack input = tile.getBiomassFluid();
        if (input != null && input.getFluid() == ModFluids.FERMENTED_SEMEN) {
            return 0xFFC0CAC0;
        }
        if (input != null && input.getFluid() == ModFluids.TOXIC_FLESH) {
            return 0xFFC85A70;
        }
        return 0xFFE8EDF2;
    }

    private int getArrowEndColor() {
        FluidStack input = tile.getBiomassFluid();
        if (input != null && input.getFluid() == ModFluids.FERMENTED_SEMEN) {
            return 0xFFA7B2A7;
        }
        if (input != null && input.getFluid() == ModFluids.TOXIC_FLESH) {
            return 0xFFAD4458;
        }
        return 0xFF9FB4C9;
    }

    private String getInputFluidName() {
        FluidStack input = tile.getBiomassFluid();
        if (input != null && input.getFluid() == ModFluids.FERMENTED_SEMEN) {
            return I18n.format("fluid.harvestech.fermented_semen");
        }
        if (input != null && input.getFluid() == ModFluids.TOXIC_FLESH) {
            return I18n.format("fluid.harvestech.toxic_flesh");
        }
        return I18n.format("fluid.harvestech.semen");
    }

    private String getOutputFluidName() {
        FluidStack output = tile.getDistilledFluid();
        if (output != null && output.getFluid() == ModFluids.DISTILLED_FERMENTED_SEMEN) {
            return I18n.format("fluid.harvestech.distilled_fermented_semen");
        }
        if (output != null && output.getFluid() == ModFluids.NECRO_SUBSTRATE) {
            return I18n.format("fluid.harvestech.necro_substrate");
        }
        return I18n.format("fluid.harvestech.distilled_biomass");
    }
}
