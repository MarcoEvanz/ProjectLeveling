package com.monody.projectleveling.client;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.capability.PlayerStatsCapability;
import com.monody.projectleveling.network.C2SAllocateStatPacket;
import com.monody.projectleveling.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class StatusScreen extends Screen {
    private static final float PANEL_WIDTH_RATIO = 0.35f;
    private static final float PANEL_HEIGHT_RATIO = 0.85f;
    private static final int MIN_PANEL_WIDTH = 210;
    private static final int MAX_PANEL_WIDTH = 280;
    private static final int MIN_PANEL_HEIGHT = 280;

    // Colors
    private static final int BG = 0xF0051020;
    private static final int FRAME = 0xFF1890D0;
    private static final int FRAME_DIM = 0xFF0C3050;
    private static final int TEXT_BRIGHT = 0xFFE8F0FF;
    private static final int TEXT_VALUE = 0xFFC0D8F0;
    private static final int TEXT_LABEL = 0xFF608098;
    private static final int TEXT_ACCENT = 0xFF40D0FF;
    private static final int TEXT_DIM = 0xFF405868;
    private static final int HP_BRIGHT = 0xFF28CC50;
    private static final int HP_DARK = 0xFF148828;
    private static final int MP_BRIGHT = 0xFF3878EE;
    private static final int MP_DARK = 0xFF1C48A0;
    private static final int BAR_BG = 0xFF060C18;
    private static final int BAR_BORDER = 0xFF182838;
    private static final int SEP = 0xFF103858;
    private static final int BTN_BG = 0xFF0C1C30;
    private static final int BTN_BORDER = 0xFF1880B0;
    private static final int BTN_HOVER_BG = 0xFF142840;
    private static final int BTN_HOVER_BORDER = 0xFF30B0E8;
    private static final int BTN_TEXT = 0xFF50C8F0;
    private static final int EXP_BRIGHT = 0xFFF0C020;
    private static final int EXP_DARK = 0xFFA08010;
    private static final int CLOSE_NORMAL = 0xFF607888;
    private static final int CLOSE_HOVER = 0xFFFF6060;

    private int panelW, panelH, panelX, panelY;
    private int pad, lineH, colMid;

    private static final int NUM_STATS = 5;
    private final int[][] statBtnBounds = new int[NUM_STATS][4];
    private final int[] closeBtnBounds = new int[4];
    private boolean hasPoints;

    public StatusScreen() {
        super(Component.literal("Status"));
    }

    @Override
    protected void init() {
        super.init();
        panelW = Math.max(MIN_PANEL_WIDTH, Math.min(MAX_PANEL_WIDTH, (int) (this.width * PANEL_WIDTH_RATIO)));
        panelH = Math.max(MIN_PANEL_HEIGHT, (int) (this.height * PANEL_HEIGHT_RATIO));
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;
        pad = Math.max(10, panelW / 18);
        lineH = Math.max(13, panelH / 20);
        colMid = panelX + panelW / 2 + 2;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);

        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        PlayerStats stats = player.getCapability(PlayerStatsCapability.PLAYER_STATS).orElse(null);
        if (stats == null) return;

        hasPoints = stats.getRemainingPoints() > 0;
        int x = panelX;
        int y = panelY;
        int left = x + pad;
        int right = x + panelW - pad;
        int innerW = panelW - pad * 2;
        int cx = x + panelW / 2;

        // Background overlay + panel
        g.fill(0, 0, this.width, this.height, 0x60000000);
        g.fill(x, y, x + panelW, y + panelH, BG);

        // Frame: single bright border + inner dim border
        drawBox(g, x, y, panelW, panelH, FRAME);
        drawBox(g, x + 2, y + 2, panelW - 4, panelH - 4, FRAME_DIM);

        // Corner accents (small L-shapes)
        int cs = 10;
        drawCorner(g, x, y, cs, 1, 1);
        drawCorner(g, x + panelW, y, cs, -1, 1);
        drawCorner(g, x, y + panelH, cs, 1, -1);
        drawCorner(g, x + panelW, y + panelH, cs, -1, -1);

        // Close button
        int closeX = right - 6;
        int closeY = y + 6;
        closeBtnBounds[0] = closeX - 2;
        closeBtnBounds[1] = closeY - 2;
        closeBtnBounds[2] = 11;
        closeBtnBounds[3] = 11;
        boolean closeHov = isInside(mouseX, mouseY, closeBtnBounds);
        g.drawString(font, "x", closeX, closeY, closeHov ? CLOSE_HOVER : CLOSE_NORMAL, false);

        // Header
        int rowY = y + pad + 2;
        int hw = font.width("STATUS");
        g.drawString(font, "STATUS", cx - hw / 2, rowY, TEXT_BRIGHT, false);

        // Separator
        rowY += lineH + 2;
        drawSep(g, left, rowY, innerW);

        // Info section
        rowY += 7;
        drawLabel(g, "NAME:", left, rowY);
        g.drawString(font, player.getName().getString(), left + font.width("NAME: "), rowY, TEXT_VALUE, false);
        drawRight(g, "LEVEL: " + stats.getLevel(), right, rowY, TEXT_VALUE);

        rowY += lineH;
        drawLabel(g, "JOB:", left, rowY);
        g.drawString(font, stats.getJob(), left + font.width("JOB: "), rowY, TEXT_VALUE, false);
        drawRight(g, "FATIGUE: 0", right, rowY, TEXT_DIM);

        rowY += lineH;
        drawLabel(g, "TITLE:", left, rowY);
        g.drawString(font, stats.getTitle(), left + font.width("TITLE: "), rowY, TEXT_VALUE, false);

        rowY += lineH + 3;
        drawSep(g, left, rowY, innerW);

        // HP bar
        rowY += 7;
        float hpPct = player.getMaxHealth() > 0 ? player.getHealth() / player.getMaxHealth() : 0;
        drawBar(g, "HP", left, rowY, innerW, hpPct, HP_BRIGHT, HP_DARK,
                String.format("%.0f / %.0f", player.getHealth(), player.getMaxHealth()));

        // MP bar
        rowY += lineH + 4;
        float mpPct = stats.getMaxMp() > 0 ? (float) stats.getCurrentMp() / stats.getMaxMp() : 0;
        drawBar(g, "MP", left, rowY, innerW, mpPct, MP_BRIGHT, MP_DARK,
                stats.getCurrentMp() + " / " + stats.getMaxMp());

        // EXP bar
        rowY += lineH + 4;
        float expPct = stats.getMaxExp() > 0 ? (float) stats.getCurrentExp() / stats.getMaxExp() : 0;
        drawBar(g, "EXP", left, rowY, innerW, expPct, EXP_BRIGHT, EXP_DARK,
                stats.getCurrentExp() + " / " + stats.getMaxExp());

        rowY += lineH + 4;
        drawSep(g, left, rowY, innerW);

        // Stats
        rowY += 8;
        int btnS = 11;

        int colEnd1 = colMid - 6;   // right edge of left column
        int colEnd2 = right;         // right edge of right column

        drawStatRow(g, 0, "STR", stats.getStrength(), left, rowY, btnS, colEnd1, mouseX, mouseY);
        drawStatRow(g, 1, "VIT", stats.getVitality(), colMid, rowY, btnS, colEnd2, mouseX, mouseY);

        rowY += lineH + 2;
        drawStatRow(g, 2, "AGI", stats.getAgility(), left, rowY, btnS, colEnd1, mouseX, mouseY);
        drawStatRow(g, 3, "INT", stats.getIntelligence(), colMid, rowY, btnS, colEnd2, mouseX, mouseY);

        rowY += lineH + 2;
        drawStatRow(g, 4, "SEN", stats.getSense(), left, rowY, btnS, colEnd1, mouseX, mouseY);

        rowY += lineH + 4;
        drawSep(g, left, rowY, innerW);

        // Summary
        rowY += 7;
        double dmg = (stats.getStrength() - 1) * 0.25;
        double hp = (stats.getVitality() - 1) * 0.5;
        double spd = (stats.getAgility() - 1) * 0.01;

        g.drawString(font, "ATK BONUS:", left, rowY, TEXT_DIM, false);
        g.drawString(font, String.format(" +%.2f", dmg), left + font.width("ATK BONUS:"), rowY, TEXT_ACCENT, false);
        String hpVal = String.format("+%.1f", hp);
        String hpLabel = "HP BONUS: ";
        drawRight(g, hpVal, right, rowY, TEXT_ACCENT);
        g.drawString(font, hpLabel, right - font.width(hpVal) - font.width(hpLabel), rowY, TEXT_DIM, false);

        rowY += lineH;
        g.drawString(font, "SPD BONUS:", left, rowY, TEXT_DIM, false);
        g.drawString(font, String.format(" +%.2f", spd), left + font.width("SPD BONUS:"), rowY, TEXT_ACCENT, false);
        String mpVal = String.valueOf(stats.getMaxMp());
        String mpLabel = "MAX MP: ";
        drawRight(g, mpVal, right, rowY, TEXT_ACCENT);
        g.drawString(font, mpLabel, right - font.width(mpVal) - font.width(mpLabel), rowY, TEXT_DIM, false);

        // Remaining points
        rowY += lineH + 3;
        drawSep(g, left, rowY, innerW);
        rowY += 7;
        String rpText = "REMAINING POINTS: " + stats.getRemainingPoints();
        int rpColor = stats.getRemainingPoints() > 0 ? TEXT_ACCENT : TEXT_DIM;
        g.drawString(font, rpText, cx - font.width(rpText) / 2, rowY, rpColor, false);
    }

    // === Stat row with inline ▲ button ===

    private void drawStatRow(GuiGraphics g, int idx, String label, int value,
                             int sx, int sy, int btnS, int colEnd, int mouseX, int mouseY) {
        String text = label + ": " + value;
        g.drawString(font, text, sx, sy, TEXT_VALUE, false);

        int bx = colEnd - btnS; // align button to right edge of column
        int by = sy - 1;
        boolean hover = false;

        if (hasPoints) {
            statBtnBounds[idx] = new int[]{bx, by, btnS, btnS};
            hover = isInside(mouseX, mouseY, statBtnBounds[idx]);
        } else {
            statBtnBounds[idx] = new int[]{0, 0, 0, 0};
        }

        // Button box (greyed out when no points)
        int bgColor = hasPoints ? (hover ? BTN_HOVER_BG : BTN_BG) : BAR_BG;
        int borderColor = hasPoints ? (hover ? BTN_HOVER_BORDER : BTN_BORDER) : FRAME_DIM;
        g.fill(bx, by, bx + btnS, by + btnS, bgColor);
        drawBox(g, bx, by, btnS, btnS, borderColor);

        // Centered ▲ triangle
        int triColor = hasPoints ? (hover ? BTN_HOVER_BORDER : BTN_TEXT) : FRAME_DIM;
        int triH = 4;
        int triW = 7;
        int triTopY = by + (btnS - triH) / 2;
        int triCx = bx + btnS / 2;
        for (int row = 0; row < triH; row++) {
            int halfSpan = (row * triW) / (2 * (triH - 1));
            g.fill(triCx - halfSpan, triTopY + row, triCx + halfSpan + 1, triTopY + row + 1, triColor);
        }
    }

    // === Click handling ===

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0) {
            if (isInside((int) mx, (int) my, closeBtnBounds)) {
                onClose();
                return true;
            }
            if (hasPoints) {
                String[] names = {"strength", "vitality", "agility", "intelligence", "sense"};
                for (int i = 0; i < NUM_STATS; i++) {
                    if (statBtnBounds[i][2] > 0 && isInside((int) mx, (int) my, statBtnBounds[i])) {
                        ModNetwork.sendToServer(new C2SAllocateStatPacket(names[i]));
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    // === Drawing helpers ===

    private void drawBox(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    private void drawCorner(GuiGraphics g, int cx, int cy, int size, int dx, int dy) {
        // Horizontal arm
        int x1 = dx > 0 ? cx : cx - size;
        int x2 = dx > 0 ? cx + size : cx;
        int yy = dy > 0 ? cy : cy - 1;
        g.fill(x1, yy, x2, yy + 1, FRAME);
        // Vertical arm
        int y1 = dy > 0 ? cy : cy - size;
        int y2 = dy > 0 ? cy + size : cy;
        int xx = dx > 0 ? cx : cx - 1;
        g.fill(xx, y1, xx + 1, y2, FRAME);
    }

    private void drawSep(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, SEP);
    }

    private void drawBar(GuiGraphics g, String label, int x, int y, int totalW,
                         float pct, int bright, int dark, String valText) {
        int lw = font.width(label) + 4;
        int vw = font.width(valText);
        int barX = x + lw;
        int barW = totalW - lw - vw - 6;
        int barH = 8;
        int barY = y + 1;

        g.drawString(font, label, x, y, TEXT_LABEL, false);
        g.fill(barX, barY, barX + barW, barY + barH, BAR_BG);

        int fw = (int) (barW * Math.min(pct, 1.0f));
        if (fw > 0) {
            g.fill(barX + 1, barY + 1, barX + fw, barY + barH / 2, bright);
            g.fill(barX + 1, barY + barH / 2, barX + fw, barY + barH - 1, dark);
        }

        drawBox(g, barX, barY, barW, barH, BAR_BORDER);
        g.drawString(font, valText, x + totalW - vw, y, TEXT_VALUE, false);
    }

    private void drawLabel(GuiGraphics g, String t, int x, int y) {
        g.drawString(font, t, x, y, TEXT_LABEL, false);
    }

    private void drawRight(GuiGraphics g, String t, int rx, int y, int color) {
        g.drawString(font, t, rx - font.width(t), y, color, false);
    }

    private static boolean isInside(int mx, int my, int[] bounds) {
        return mx >= bounds[0] && mx < bounds[0] + bounds[2]
                && my >= bounds[1] && my < bounds[1] + bounds[3];
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
