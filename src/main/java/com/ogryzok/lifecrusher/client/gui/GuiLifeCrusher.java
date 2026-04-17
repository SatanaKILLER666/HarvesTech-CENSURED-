package com.ogryzok.lifecrusher.client.gui;

import com.ogryzok.lifecrusher.container.ContainerLifeCrusher;
import com.ogryzok.lifecrusher.tile.CrusherState;
import com.ogryzok.lifecrusher.tile.TileLifeCrusher;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;

import java.util.ArrayList;
import java.util.List;

public class GuiLifeCrusher extends GuiContainer {
    private final InventoryPlayer playerInventory;
    private final TileLifeCrusher tile;

    public GuiLifeCrusher(InventoryPlayer playerInventory, TileLifeCrusher tile) {
        super(new ContainerLifeCrusher(playerInventory, tile));
        this.playerInventory = playerInventory;
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderCustomTooltips(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("gui.harvestech.life_crusher.title"), 8, 6, 0xF2F2F2);
        fontRenderer.drawString(I18n.format("gui.harvestech.life_crusher.work_window"), 36, 20, 0xCFCFCF);
        fontRenderer.drawString(playerInventory.getDisplayName().getUnformattedText(), 8, 72, 0xD8D8D8);

        fontRenderer.drawString(I18n.format("gui.harvestech.life_crusher.status"), 40, 34, 0x9FA8B4);
        fontRenderer.drawString(getStateLabel(), 82, 34, getStateColor());

        fontRenderer.drawString(I18n.format("gui.harvestech.life_crusher.mode"), 40, 46, 0x9FA8B4);
        fontRenderer.drawString(tile.isBerserk() ? I18n.format("gui.harvestech.life_crusher.mode.berserk") : I18n.format("gui.harvestech.life_crusher.mode.normal"), 82, 46, tile.isBerserk() ? 0xFF7070 : 0xD7DEE8);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        drawDefaultBackground();

        int x = guiLeft;
        int y = guiTop;

        drawRect(x, y, x + xSize, y + ySize, 0xFF0D0F12);
        drawGradientRect(x + 2, y + 2, x + xSize - 2, y + ySize - 2, 0xFF222830, 0xFF161A20);

        drawPanel(x + 6, y + 18, 20, 52);
        drawPanel(x + 32, y + 18, 96, 52);
        drawPanel(x + 132, y + 18, 38, 52);
        drawPanel(x + 6, y + 80, 164, 80);

        drawEnergyBar(x + 10, y + 27, 12, 38, tile.getEnergyStored(), tile.getMaxEnergyStored());
        drawProgressBar(x + 40, y + 57, 80, 8, tile.getProgress(partialTicks));

        drawBuildPlaceholder(x + 136, y + 34, 30, 34);


    }

    private void renderCustomTooltips(int mouseX, int mouseY) {
        if (isPointInRegion(10, 27, 12, 38, mouseX, mouseY)) {
            List<String> lines = new ArrayList<>();
            lines.add(tile.getEnergyStored() + " / " + tile.getMaxEnergyStored() + " FE");
            if (tile.isOverloaded()) {
                lines.add(I18n.format("gui.harvestech.life_crusher.accumulator_overloaded"));
            }
            drawHoveringText(lines, mouseX, mouseY);
            return;
        }

        if (isPointInRegion(40, 57, 80, 8, mouseX, mouseY)) {
            List<String> lines = new ArrayList<>();
            lines.add(I18n.format("gui.harvestech.life_crusher.load") + ": " + Math.round(tile.getProgressRaw() * 100.0F) + "%");
            drawHoveringText(lines, mouseX, mouseY);
            return;
        }

        if (isPointInRegion(32, 18, 96, 52, mouseX, mouseY)) {
            List<String> lines = new ArrayList<>();
            lines.add(I18n.format("gui.harvestech.life_crusher.status") + ": " + getStateLabel());
            lines.add(tile.isPowered() ? I18n.format("gui.harvestech.life_crusher.drive_active") : I18n.format("gui.harvestech.life_crusher.drive_stopped"));
            lines.add(I18n.format("gui.harvestech.life_crusher.mode") + ": " + (tile.isBerserk() ? I18n.format("gui.harvestech.life_crusher.mode.berserk_lower") : I18n.format("gui.harvestech.life_crusher.mode.normal_lower")));
            drawHoveringText(lines, mouseX, mouseY);
        }
    }

    private void drawPanel(int x, int y, int w, int h) {
        drawRect(x, y, x + w, y + h, 0xFF050607);
        drawGradientRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF303843, 0xFF1A1F26);
        drawRect(x + 1, y + 1, x + w - 1, y + 2, 0x55FFFFFF);
        drawRect(x + 1, y + h - 2, x + w - 1, y + h - 1, 0x66000000);
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

    private void drawProgressBar(int x, int y, int w, int h, float progress) {
        drawRect(x, y, x + w, y + h, 0xFF07090C);
        drawRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF161B22);

        int fill = Math.max(0, Math.min(w - 2, Math.round(progress * (w - 2))));
        if (fill > 0) {
            drawRect(x + 1, y + 1, x + 1 + fill, y + h - 1, 0xFF9FB4C9);
        }
    }

    private void drawBuildPlaceholder(int x, int y, int w, int h) {
        drawRect(x, y, x + w, y + h, 0xFF050607);
        drawGradientRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF1A1F26, 0xFF12161C);
        drawRect(x + 4, y + 4, x + w - 4, y + h - 4, 0xFF0D1117);
    }

    private String getStateLabel() {
        CrusherState state = tile.getState();
        if (state == CrusherState.SPINUP) return I18n.format("gui.harvestech.life_crusher.state.idle");
        if (state == CrusherState.WORK) return I18n.format("gui.harvestech.life_crusher.state.working");
        if (state == CrusherState.SPINDOWN) return I18n.format("gui.harvestech.life_crusher.state.stopping");
        if (state == CrusherState.FAIL) return I18n.format("gui.harvestech.life_crusher.state.stopping");
        return I18n.format("gui.harvestech.life_crusher.state.idle");
    }

    private int getStateColor() {
        CrusherState state = tile.getState();
        if (state == CrusherState.SPINUP) return 0xDCE6F0;
        if (state == CrusherState.WORK) return 0x5AFF7D;
        if (state == CrusherState.SPINDOWN) return 0xC8D0DA;
        if (state == CrusherState.FAIL) return 0xFF7070;
        return 0x9FA8B4;
    }
}