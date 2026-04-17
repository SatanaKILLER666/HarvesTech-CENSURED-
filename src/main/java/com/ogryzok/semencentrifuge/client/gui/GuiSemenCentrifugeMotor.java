package com.ogryzok.semencentrifuge.client.gui;

import com.ogryzok.network.ModNetwork;
import com.ogryzok.player.semen.ISemenStorage;
import com.ogryzok.player.semen.SemenProvider;
import com.ogryzok.semencentrifuge.container.ContainerSemenCentrifugeMotor;
import com.ogryzok.semencentrifuge.network.packet.PacketCentrifugeFinish;
import com.ogryzok.semencentrifuge.network.packet.PacketCentrifugeHit;
import com.ogryzok.semencentrifuge.network.packet.PacketCentrifugeStart;
import com.ogryzok.semencentrifuge.tile.TileSemenCentrifugeBase;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiSemenCentrifugeMotor extends GuiContainer {

    private final TileSemenCentrifugeBase tile;
    private GuiButton startButton;

    public GuiSemenCentrifugeMotor(ContainerSemenCentrifugeMotor container, TileSemenCentrifugeBase tile) {
        super(container);
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);

        int x = this.guiLeft;
        int y = this.guiTop;

        this.buttonList.clear();
        this.startButton = new GuiButton(0, x + 8, y + 8, 38, 20, tile.isSessionActive() ? "STOP" : "START");
        this.buttonList.add(this.startButton);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (this.startButton != null) {
            this.startButton.displayString = tile.isSessionActive() ? "STOP" : "START";
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderCustomTooltips(mouseX, mouseY);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == null || !button.enabled) {
            return;
        }

        if (button.id == 0) {
            if (!tile.isSessionActive()) {
                ModNetwork.CHANNEL.sendToServer(new PacketCentrifugeStart(tile.getPos()));
            } else {
                ModNetwork.CHANNEL.sendToServer(new PacketCentrifugeFinish(tile.getPos()));
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == this.mc.gameSettings.keyBindInventory.getKeyCode() || keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.player.closeScreen();
            return;
        }

        if (keyCode == Keyboard.KEY_SPACE) {
            ModNetwork.CHANNEL.sendToServer(new PacketCentrifugeHit(tile.getPos()));
            return;
        }

        if (keyCode == Keyboard.KEY_LCONTROL || keyCode == Keyboard.KEY_RCONTROL) {
            ModNetwork.CHANNEL.sendToServer(new PacketCentrifugeFinish(tile.getPos()));
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        int x = this.guiLeft;
        int y = this.guiTop;

        drawDefaultBackground();

        drawRect(x, y, x + this.xSize, y + this.ySize, 0xFF0D0F12);
        drawGradientRect(x + 2, y + 2, x + this.xSize - 2, y + this.ySize - 2, 0xFF222830, 0xFF161A20);

        int sx = x + 44;
        int sy = y + 36;
        int sw = 92;
        int sh = 70;
        drawPanel(sx, sy, sw, sh);
        drawRect(sx + 8, sy + 8, sx + sw - 8, sy + sh - 8, 0xFF0A1017);
        drawGradientRect(sx + 9, sy + 9, sx + sw - 9, sy + sh - 9, 0xFF0D1723, 0xFF0A1320);

        drawPanel(x + 144, y + 21, 18, 90);
        int energyBarHeight = 88;
        int energy = tile.getEnergyStored();
        int maxEnergy = Math.max(1, tile.getMaxEnergyStored());
        int energyFill = (int) ((energy / (float) maxEnergy) * energyBarHeight);
        if (energyFill > 0) {
            drawGradientRect(x + 145, y + 22 + (energyBarHeight - energyFill), x + 161, y + 22 + energyBarHeight, 0xFF5AFF7D, 0xFF1FAE46);
            drawRect(x + 146, y + 23 + (energyBarHeight - energyFill), x + 160, y + 26 + (energyBarHeight - energyFill), 0x55FFFFFF);
        }

        drawPanel(x + 144, y + 120, 18, 34);
        int loadBarHeight = 32;
        int load = Math.max(0, Math.min(100, tile.getLoadPercent()));
        int loadFill = (int) ((load / 100.0F) * loadBarHeight);
        if (loadFill > 0) {
            drawGradientRect(x + 145, y + 121 + (loadBarHeight - loadFill), x + 161, y + 121 + loadBarHeight, 0xFFFF7A7A, 0xFFA54A4A);
            drawRect(x + 146, y + 122 + (loadBarHeight - loadFill), x + 160, y + 124 + (loadBarHeight - loadFill), 0x44FFFFFF);
        }

        if (tile.isSkillActive()) {
            drawPerimeterSkillCheck(sx + 6, sy + 6, sw - 12, sh - 12);
        }
    }

    private void drawPanel(int x, int y, int w, int h) {
        drawRect(x, y, x + w, y + h, 0xFF050607);
        drawGradientRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF303843, 0xFF1A1F26);
        drawRect(x + 1, y + 1, x + w - 1, y + 2, 0x55FFFFFF);
        drawRect(x + 1, y + h - 2, x + w - 1, y + h - 1, 0x66000000);
    }

    private void renderCustomTooltips(int mouseX, int mouseY) {
        if (isPointInRegion(144, 21, 18, 90, mouseX, mouseY)) {
            List<String> lines = new ArrayList<>();
            lines.add(tile.getEnergyStored() + " / " + tile.getMaxEnergyStored() + " FE");
            drawHoveringText(lines, mouseX, mouseY);
            return;
        }

        if (isPointInRegion(144, 120, 18, 34, mouseX, mouseY)) {
            List<String> lines = new ArrayList<>();
            lines.add(I18n.format("gui.harvestech.centrifuge.warning.line1"));
            lines.add(I18n.format("gui.harvestech.centrifuge.warning.line2"));
            drawHoveringText(lines, mouseX, mouseY);
        }
    }

    private void drawPerimeterSkillCheck(int x, int y, int w, int h) {
        float hitStart = tile.getHitStart();
        float hitSize = tile.getHitSize();
        float perfectStart = tile.getPerfectStart();
        float perfectSize = tile.getPerfectSize();
        float marker = tile.getMarkerProgress();

        int perimeter = (w * 2) + (h * 2) - 4;
        if (perimeter <= 0) {
            return;
        }

        for (int i = 0; i < perimeter; i++) {
            int[] p = perimeterPoint(x, y, w, h, i / (float) perimeter);
            drawRect(p[0], p[1], p[0] + 1, p[1] + 1, 0xFF4A5568);
        }

        int hitSteps = Math.max(1, (int) (hitSize * perimeter));
        for (int i = 0; i < hitSteps; i++) {
            float t = wrap01(hitStart + (i / (float) perimeter));
            int[] p = perimeterPoint(x, y, w, h, t);
            drawRect(p[0], p[1], p[0] + 2, p[1] + 2, 0xFFB8C5D6);
        }

        int perfectSteps = Math.max(1, (int) (perfectSize * perimeter));
        for (int i = 0; i < perfectSteps; i++) {
            float t = wrap01(perfectStart + (i / (float) perimeter));
            int[] p = perimeterPoint(x, y, w, h, t);
            drawRect(p[0] - 1, p[1] - 1, p[0] + 3, p[1] + 3, 0xFFFFFFFF);
        }

        int[] mp = perimeterPoint(x, y, w, h, marker);
        drawRect(mp[0] - 2, mp[1] - 2, mp[0] + 3, mp[1] + 3, 0xFFFFFFFF);
    }

    private int[] perimeterPoint(int x, int y, int w, int h, float t) {
        t = wrap01(t);

        int top = w;
        int right = h - 1;
        int bottom = w - 1;
        int left = h - 2;
        int total = top + right + bottom + left;

        int d = Math.min(total - 1, Math.max(0, (int) (t * total)));

        if (d < top) {
            return new int[]{x + d, y};
        }
        d -= top;

        if (d < right) {
            return new int[]{x + w - 1, y + 1 + d};
        }
        d -= right;

        if (d < bottom) {
            return new int[]{x + w - 2 - d, y + h - 1};
        }
        d -= bottom;

        return new int[]{x, y + h - 2 - d};
    }

    private float wrap01(float v) {
        while (v < 0.0F) v += 1.0F;
        while (v >= 1.0F) v -= 1.0F;
        return v;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        ISemenStorage storage = mc.player == null ? null : mc.player.getCapability(SemenProvider.SEMEN_CAP, null);
        int playerMatter = storage == null ? 0 : storage.getAmount();
        int playerMaxMilliBuckets = Math.max(0, (playerMatter / 200) * 200);

        int loadColor = tile.getLoadPercent() > 70 ? 0xFFFF5555 : 0xE0E0E0;

        this.fontRenderer.drawString("max = " + playerMaxMilliBuckets + " mB", 122 - 12, 12, 0xC8E6FF);
        this.fontRenderer.drawString(tile.getLoadPercent() + "%", 123, 133, loadColor);
        this.fontRenderer.drawString(I18n.format("fluid.harvestech.semen") + ": " + tile.getBiomassStored() + " / " + tile.getMaxBiomassStored(), 54, 28, 0xE0E0E0);

        String centerText = "";
        int centerColor = 0xE5E7EB;

        if (tile.isSkillActive()) {
            centerText = I18n.format("gui.harvestech.centrifuge.center.space");
        } else {
            switch (tile.getSessionState()) {
                case TileSemenCentrifugeBase.STATE_FORCED_COUNTDOWN:
                    centerText = I18n.format("gui.harvestech.centrifuge.center.take") + ": " + Math.max(1, (tile.getForcedCountdownTicks() + 19) / 20);
                    centerColor = 0xFFFF5555;
                    break;
                case TileSemenCentrifugeBase.STATE_BETWEEN_ROUNDS:
                    centerText = I18n.format("gui.harvestech.centrifuge.center.next_wave") + ": " + Math.max(1, (tile.getBetweenTicks() + 19) / 20);
                    centerColor = 0xFFFF5555;
                    break;
                case TileSemenCentrifugeBase.STATE_FAILED:
                    centerText = I18n.format("gui.harvestech.centrifuge.center.fail");
                    centerColor = 0xFFFF5555;
                    break;
                case TileSemenCentrifugeBase.STATE_FINISHED:
                    centerText = I18n.format("gui.harvestech.centrifuge.center.ready");
                    break;
                default:
                    break;
            }
        }

        if (!centerText.isEmpty()) {
            int cw = this.fontRenderer.getStringWidth(centerText);
            this.fontRenderer.drawString(centerText, 90 - cw / 2, 67, centerColor);
        }

        int rewardNow = Math.min(tile.getStartMatter(), tile.getEarnedRounds() * 200);

        String status;
        if (tile.getSessionState() == TileSemenCentrifugeBase.STATE_BETWEEN_ROUNDS) {
            status = I18n.format("gui.harvestech.centrifuge.status.take_now", rewardNow);
        } else if (tile.getSessionState() == TileSemenCentrifugeBase.STATE_FORCED_COUNTDOWN) {
            status = I18n.format("gui.harvestech.centrifuge.status.take_now", rewardNow);
        } else {
            status = resolveStatusText(tile.getStatusText());
            if (status == null || status.trim().isEmpty()) {
                if (!tile.isSessionActive()) {
                    status = playerMatter >= 200 ? I18n.format("gui.harvestech.centrifuge.status.press_start") : I18n.format("gui.harvestech.centrifuge.status.not_enough_matter");
                } else {
                    status = I18n.format("gui.harvestech.centrifuge.status.controls");
                }
            }
        }

        this.fontRenderer.drawSplitString(status, 24, 116, 84, 0xF2F2F2);
    }
    private String resolveStatusText(String rawStatus) {
        if (rawStatus == null) {
            return "";
        }

        String status = rawStatus.trim();
        if (!status.startsWith("@")) {
            return status;
        }

        String payload = status.substring(1);
        String[] parts = payload.split("\\|", -1);
        if (parts.length == 0 || parts[0].isEmpty()) {
            return rawStatus;
        }

        Object[] args = new Object[Math.max(0, parts.length - 1)];
        for (int i = 1; i < parts.length; i++) {
            args[i - 1] = parts[i];
        }

        return I18n.format(parts[0], args);
    }

}
