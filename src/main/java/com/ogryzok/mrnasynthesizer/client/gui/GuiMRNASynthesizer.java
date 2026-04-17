package com.ogryzok.mrnasynthesizer.client.gui;

import com.ogryzok.food.FoodRegistry;
import com.ogryzok.mrnasynthesizer.container.ContainerMRNASynthesizer;
import com.ogryzok.mrnasynthesizer.tile.TileMRNASynthesizer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class GuiMRNASynthesizer extends GuiContainer {
    private final TileMRNASynthesizer tile;

    public GuiMRNASynthesizer(InventoryPlayer playerInv, TileMRNASynthesizer tile) {
        super(new ContainerMRNASynthesizer(playerInv, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 257;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
        renderCustomTooltips(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("gui.harvestech.mrna.title"), 8, 6, 0xE2E6EA);

        String fillers = I18n.format("gui.harvestech.mrna.fillers");
        String antigens = I18n.format("gui.harvestech.mrna.antigens");
        String syringes = I18n.format("gui.harvestech.mrna.syringes");
        String inventory = I18n.format("gui.harvestech.common.inventory");

        fontRenderer.drawString(fillers, 49 - fontRenderer.getStringWidth(fillers) / 2, 18, 0xBFC6CE);
        fontRenderer.drawString(antigens, 127 - fontRenderer.getStringWidth(antigens) / 2, 18, 0xBFC6CE);
        fontRenderer.drawString(syringes, 88 - fontRenderer.getStringWidth(syringes) / 2, 101, 0xBFC6CE);
        fontRenderer.drawString(inventory, 8, 163, 0xBFC6CE);
    }

    @Override
    protected void renderHoveredToolTip(int x, int y) {
        Slot slot = this.getSlotUnderMouse();
        if (slot != null) {
            if (slot.slotNumber == TileMRNASynthesizer.SLOT_FILLER_TOP || slot.slotNumber == TileMRNASynthesizer.SLOT_FILLER_BOTTOM
                    || slot.slotNumber == TileMRNASynthesizer.SLOT_CATALYST_TOP || slot.slotNumber == TileMRNASynthesizer.SLOT_CATALYST_BOTTOM) {
                List<String> lines = new ArrayList<>();
                ItemStack stack = slot.getStack();
                if (stack.isEmpty()) {
                    lines.add(I18n.format("gui.harvestech.common.empty"));
                } else if (stack.getItem() == FoodRegistry.EVAPORATED_BIOMASS) {
                    lines.add(I18n.format("item.harvestech.evaporated_biomass.name"));
                } else if (stack.getItem() == FoodRegistry.PROTEIN_BIOMASS) {
                    lines.add(I18n.format("item.harvestech.protein_biomass.name"));
                } else {
                    lines.add(I18n.format("item.harvestech.toxic_biomass.name"));
                }
                this.drawHoveringText(lines, x, y);
                return;
            }
        }
        super.renderHoveredToolTip(x, y);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        int x = guiLeft;
        int y = guiTop;
        int anim = (mc.player != null ? mc.player.ticksExisted : 0);

        drawRect(x, y, x + xSize, y + ySize, 0xFF0C0F12);
        drawGradientRect(x + 2, y + 2, x + xSize - 2, y + ySize - 2, 0xFF232931, 0xFF14181E);

        drawPanel(x + 6, y + 22, 20, 86);      // energy
        drawPanel(x + 33, y + 28, 32, 70);     // left components
        drawPanel(x + 111, y + 28, 32, 70);    // right components
        drawPanel(x + 74, y + 24, 28, 78);     // centered tank
        drawPanel(x + 20, y + 110, 136, 58);   // syringes area

        drawEnergyBar(x + 10, y + 32, 12, 70, tile.getEnergyStored(), tile.getMaxEnergyStored());
        drawTank(x + 77, y + 30, 22, 66, tile.getTankAmount(), tile.getTankCapacity(), anim, tile.hasWater(), tile.getProductFluidColor());

        int fillerSlotX = x + 40;
        int antigenSlotX = x + 118;

        drawSlotFrame(fillerSlotX, y + 35);
        drawSlotFrame(fillerSlotX, y + 62);
        drawSlotFrame(antigenSlotX, y + 35);
        drawSlotFrame(antigenSlotX, y + 62);

        int fillArrowColor = tile.getFillArrowColor();
        for (int i = 0; i < 4; i++) {
            int sx = x + 37 + i * 28;
            drawSlotFrame(sx, y + 119);
            drawSlotFrame(sx, y + 143);
            drawFillArrow(sx + 5, y + 137, 6, tile.getSyringeProgressScaled(i, 6), fillArrowColor);
        }

        int craftArrowColor = tile.getCraftArrowColor();
        int leftArrowX = x + 65;
        int rightArrowX = x + 101;
        drawCraftArrows(leftArrowX, y + 40, rightArrowX, y + 40, tile.getCraftProgressScaled(10), craftArrowColor);
        drawCraftArrows(leftArrowX, y + 67, rightArrowX, y + 67, tile.getCraftProgressScaled(10), craftArrowColor);
    }

    private void renderCustomTooltips(int mouseX, int mouseY) {
        if (isPointInRegion(10, 32, 12, 70, mouseX, mouseY)) {
            List<String> lines = new ArrayList<>();
            lines.add("Energy: " + tile.getEnergyStored() + " / " + tile.getMaxEnergyStored() + " FE");
            drawHoveringText(lines, mouseX, mouseY);
            return;
        }

        if (isPointInRegion(77, 30, 22, 66, mouseX, mouseY)) {
            List<String> lines = new ArrayList<>();
            if (tile.hasProductFluid()) lines.add(I18n.format(tile.getDisplayedFluidKey()) + ": " + tile.getTankAmount() + " mB");
            else if (tile.hasWater()) lines.add(I18n.format("gui.harvestech.common.water") + ": " + tile.getTankAmount() + " mB");
            else lines.add(I18n.format("gui.harvestech.common.empty"));
            drawHoveringText(lines, mouseX, mouseY);
            return;
        }

        if (isPointInRegion(40, 35, 18, 18, mouseX, mouseY) || isPointInRegion(40, 62, 18, 18, mouseX, mouseY)) {
            List<String> lines = new ArrayList<>();
            ItemStack hovered = getHoveredSlotStack();
            lines.add(hovered.isEmpty() ? I18n.format("gui.harvestech.common.empty") : I18n.format("item.harvestech.evaporated_biomass.name"));
            drawHoveringText(lines, mouseX, mouseY);
            return;
        }

        if (isPointInRegion(118, 35, 18, 18, mouseX, mouseY) || isPointInRegion(118, 62, 18, 18, mouseX, mouseY)) {
            List<String> lines = new ArrayList<>();
            ItemStack hovered = getHoveredSlotStack();
            if (hovered.isEmpty()) lines.add(I18n.format("gui.harvestech.common.empty"));
            else if (hovered.getItem() == FoodRegistry.PROTEIN_BIOMASS) lines.add(I18n.format("item.harvestech.protein_biomass.name"));
            else lines.add(I18n.format("item.harvestech.toxic_biomass.name"));
            drawHoveringText(lines, mouseX, mouseY);
        }
    }

    private ItemStack getHoveredSlotStack() {
        Slot slot = getSlotUnderMouse();
        return slot == null ? ItemStack.EMPTY : slot.getStack();
    }

    private void drawPanel(int x, int y, int w, int h) {
        drawRect(x, y, x + w, y + h, 0xFF040506);
        drawGradientRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF313943, 0xFF1A2027);
        drawRect(x + 1, y + 1, x + w - 1, y + 2, 0x40FFFFFF);
        drawRect(x + 1, y + h - 2, x + w - 1, y + h - 1, 0x66000000);
    }

    private void drawSlotFrame(int x, int y) {
        drawRect(x, y, x + 18, y + 18, 0xFF040506);
        drawGradientRect(x + 1, y + 1, x + 17, y + 17, 0xFF3D4651, 0xFF242A31);
        drawRect(x + 2, y + 2, x + 16, y + 16, 0xFF101419);
        drawRect(x + 2, y + 2, x + 16, y + 3, 0x33FFFFFF);
    }

    private void drawEnergyBar(int x, int y, int w, int h, int stored, int max) {
        drawRect(x, y, x + w, y + h, 0xFF040506);
        drawRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF0E1216);
        int inner = h - 2;
        int fill = max <= 0 ? 0 : stored * inner / max;
        if (fill > 0) {
            drawGradientRect(x + 1, y + 1 + (inner - fill), x + w - 1, y + h - 1, 0xFF59E17B, 0xFF248846);
            drawRect(x + 2, y + 2 + (inner - fill), x + w - 2, y + 4 + (inner - fill), 0x55FFFFFF);
        }
    }

    private void drawTank(int x, int y, int w, int h, int amount, int capacity, int anim, boolean water, int productColor) {
        drawRect(x, y, x + w, y + h, 0xFF040506);
        drawGradientRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF10151B, 0xFF070A0D);

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

        if (water) {
            topColor = 0xFF3E8BFF;
            bottomColor = 0xFF1B5FCC;
            stripeColor = 0x22356CBC;
            shimmerColor = 0x4474B8FF;
            glossColor = 0x5589BEFF;
            edgeColor = 0x302A58A8;
        } else {
            topColor = productColor;
            bottomColor = darken(productColor, 0.82F);
            stripeColor = withAlpha(darken(productColor, 0.55F), 0x22);
            shimmerColor = withAlpha(lighten(productColor, 1.12F), 0x44);
            glossColor = withAlpha(lighten(productColor, 1.2F), 0x55);
            edgeColor = withAlpha(darken(productColor, 0.65F), 0x30);
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


    private int darken(int color, float factor) {
        int a = (color >>> 24) & 0xFF;
        int r = Math.max(0, Math.min(255, (int)(((color >>> 16) & 0xFF) * factor)));
        int g = Math.max(0, Math.min(255, (int)(((color >>> 8) & 0xFF) * factor)));
        int b = Math.max(0, Math.min(255, (int)((color & 0xFF) * factor)));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int lighten(int color, float factor) {
        int a = (color >>> 24) & 0xFF;
        int r = Math.max(0, Math.min(255, (int)(((color >>> 16) & 0xFF) * factor)));
        int g = Math.max(0, Math.min(255, (int)(((color >>> 8) & 0xFF) * factor)));
        int b = Math.max(0, Math.min(255, (int)((color & 0xFF) * factor)));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }

    private void drawCraftArrows(int leftX, int y, int rightX, int rightY, int progress, int color) {
        drawSingleArrow(leftX, y, true, progress, color);
        drawSingleArrow(rightX, rightY, false, progress, color);
    }

    private void drawSingleArrow(int x, int y, boolean right, int progress, int color) {
        drawRect(x, y, x + 10, y + 8, 0xFF06080B);
        drawGradientRect(x + 1, y + 1, x + 9, y + 7, 0xFF1C2128, 0xFF101419);

        int fill = Math.min(8, Math.max(0, progress));
        int color1 = lighten(color, 1.08F);
        int color2 = darken(color, 0.82F);

        if (fill > 0) {
            if (right) {
                drawGradientRect(x + 1, y + 2, x + 1 + Math.min(5, fill), y + 6, color1, color2);
                if (fill >= 6) drawRect(x + 6, y + 3, x + 7, y + 5, color1);
                if (fill >= 7) drawRect(x + 7, y + 2, x + 8, y + 6, color1);
                if (fill >= 8) drawRect(x + 8, y + 3, x + 9, y + 5, color1);
            } else {
                drawGradientRect(x + 9 - Math.min(5, fill), y + 2, x + 9, y + 6, color1, color2);
                if (fill >= 6) drawRect(x + 3, y + 3, x + 4, y + 5, color1);
                if (fill >= 7) drawRect(x + 2, y + 2, x + 3, y + 6, color1);
                if (fill >= 8) drawRect(x + 1, y + 3, x + 2, y + 5, color1);
            }
        }
    }

    private void drawFillArrow(int x, int y, int h, int progress, int color) {
        drawRect(x, y, x + 8, y + h, 0xFF06080B);
        drawGradientRect(x + 1, y + 1, x + 7, y + h - 1, 0xFF1C2128, 0xFF101419);
        if (progress <= 0) return;

        int fy2 = Math.min(y + h - 2, y + 1 + progress);
        int topColor = lighten(color, 1.08F);
        int bottomColor = darken(color, 0.82F);
        drawGradientRect(x + 2, y + 1, x + 6, fy2, topColor, bottomColor);
        drawRect(x + 2, y + 1, x + 6, Math.min(y + 2, fy2), withAlpha(lighten(color, 1.18F), 0x66));
        drawRect(x + 1, fy2, x + 7, fy2 + 1, topColor);
    }
}
