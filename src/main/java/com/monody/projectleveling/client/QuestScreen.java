package com.monody.projectleveling.client;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.capability.PlayerStatsCapability;
import com.monody.projectleveling.network.C2SClaimQuestRewardPacket;
import com.monody.projectleveling.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class QuestScreen extends Screen {
    private static final float PANEL_WIDTH_RATIO = 0.40f;
    private static final float PANEL_HEIGHT_RATIO = 0.70f;
    private static final int MIN_PANEL_WIDTH = 230;
    private static final int MAX_PANEL_WIDTH = 300;
    private static final int MIN_PANEL_HEIGHT = 200;

    // Colors (same palette as StatusScreen)
    private static final int BG = 0xF0051020;
    private static final int FRAME = 0xFF1890D0;
    private static final int FRAME_DIM = 0xFF0C3050;
    private static final int TEXT_BRIGHT = 0xFFE8F0FF;
    private static final int TEXT_VALUE = 0xFFC0D8F0;
    private static final int TEXT_DIM = 0xFF405868;
    private static final int SEP = 0xFF103858;
    private static final int BTN_BG = 0xFF0C1C30;
    private static final int BTN_BORDER = 0xFF1880B0;
    private static final int BTN_HOVER_BG = 0xFF142840;
    private static final int BTN_HOVER_BORDER = 0xFF30B0E8;
    private static final int BTN_TEXT = 0xFF50C8F0;
    private static final int QUEST_DONE = 0xFF40E860;
    private static final int QUEST_PENDING = 0xFF888888;
    private static final int CLOSE_NORMAL = 0xFF607888;
    private static final int CLOSE_HOVER = 0xFFFF6060;

    // Tab colors
    private static final int TAB_ACTIVE_BG = 0xFF0C2848;
    private static final int TAB_ACTIVE_BORDER = 0xFF1890D0;
    private static final int TAB_ACTIVE_TEXT = 0xFF40D0FF;
    private static final int TAB_INACTIVE_BG = 0xFF060C18;
    private static final int TAB_INACTIVE_BORDER = 0xFF0C3050;
    private static final int TAB_INACTIVE_TEXT = 0xFF405868;

    private enum Tab { DAILY, SIDE, MAIN }
    private Tab activeTab = Tab.DAILY;

    private int panelW, panelH, panelX, panelY;
    private int pad, lineH;

    private final int[][] tabBounds = new int[3][4];
    private final int[] closeBtnBounds = new int[4];
    private final int[] backBtnBounds = new int[4];
    private final int[] claimBtnBounds = new int[4];
    private boolean claimBtnVisible = false;
    private int lastMouseX;
    private int lastMouseY;

    public QuestScreen() {
        super(Component.literal("Quests"));
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
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        PlayerStats stats = player.getCapability(PlayerStatsCapability.PLAYER_STATS).orElse(null);
        if (stats == null) return;

        int x = panelX;
        int y = panelY;
        int left = x + pad;
        int right = x + panelW - pad;
        int innerW = panelW - pad * 2;
        int cx = x + panelW / 2;

        // Background overlay + panel
        g.fill(0, 0, this.width, this.height, 0x60000000);
        g.fill(x, y, x + panelW, y + panelH, BG);

        // Frame
        drawBox(g, x, y, panelW, panelH, FRAME);
        drawBox(g, x + 2, y + 2, panelW - 4, panelH - 4, FRAME_DIM);

        // Corner accents
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
        int hw = font.width("QUESTS");
        g.drawString(font, "QUESTS", cx - hw / 2, rowY, TEXT_BRIGHT, false);

        // Separator below header
        rowY += lineH + 2;
        drawSep(g, left, rowY, innerW);

        // Tab buttons
        rowY += 6;
        int tabGap = 4;
        int tabCount = 3;
        int totalGap = tabGap * (tabCount - 1);
        int tabW = (innerW - totalGap) / tabCount;
        int tabH = lineH + 2;

        String[] tabLabels = {"DAILY", "SIDE", "MAIN"};
        Tab[] tabValues = {Tab.DAILY, Tab.SIDE, Tab.MAIN};

        for (int i = 0; i < tabCount; i++) {
            int tabX = left + i * (tabW + tabGap);
            int tabY = rowY;
            tabBounds[i] = new int[]{tabX, tabY, tabW, tabH};

            boolean isActive = activeTab == tabValues[i];
            boolean isHovered = !isActive && isInside(mouseX, mouseY, tabBounds[i]);

            int tbg, tborder, ttext;
            if (isActive) {
                tbg = TAB_ACTIVE_BG;
                tborder = TAB_ACTIVE_BORDER;
                ttext = TAB_ACTIVE_TEXT;
            } else if (isHovered) {
                tbg = BTN_HOVER_BG;
                tborder = BTN_HOVER_BORDER;
                ttext = BTN_TEXT;
            } else {
                tbg = TAB_INACTIVE_BG;
                tborder = TAB_INACTIVE_BORDER;
                ttext = TAB_INACTIVE_TEXT;
            }

            g.fill(tabX, tabY, tabX + tabW, tabY + tabH, tbg);
            drawBox(g, tabX, tabY, tabW, tabH, tborder);

            int lw = font.width(tabLabels[i]);
            g.drawString(font, tabLabels[i], tabX + (tabW - lw) / 2,
                    tabY + (tabH - font.lineHeight) / 2, ttext, false);
        }

        // Separator below tabs
        rowY += tabH + 6;
        drawSep(g, left, rowY, innerW);

        // Tab content
        int contentY = rowY + 7;
        switch (activeTab) {
            case DAILY -> renderDailyTab(g, stats, left, contentY, innerW, cx);
            case SIDE -> renderPlaceholderTab(g, "No side quests available.", cx, contentY);
            case MAIN -> renderPlaceholderTab(g, "No main quests available.", cx, contentY);
        }

        // Back button at bottom-left
        int backY = y + panelH - pad - lineH - 2;
        int backW = font.width("< BACK") + 12;
        int backX = left;
        backBtnBounds[0] = backX;
        backBtnBounds[1] = backY;
        backBtnBounds[2] = backW;
        backBtnBounds[3] = lineH + 2;

        boolean backHov = isInside(mouseX, mouseY, backBtnBounds);
        g.fill(backX, backY, backX + backW, backY + lineH + 2, backHov ? BTN_HOVER_BG : BTN_BG);
        drawBox(g, backX, backY, backW, lineH + 2, backHov ? BTN_HOVER_BORDER : BTN_BORDER);
        g.drawString(font, "< BACK", backX + 6, backY + (lineH + 2 - font.lineHeight) / 2,
                backHov ? BTN_HOVER_BORDER : BTN_TEXT, false);
    }

    // === Tab content renderers ===

    private void renderDailyTab(GuiGraphics g, PlayerStats stats, int left, int rowY, int innerW, int cx) {
        String questHeader = stats.isQuestCompleted() ? "DAILY QUEST - COMPLETE" : "DAILY QUEST";
        int qhColor = stats.isQuestCompleted() ? QUEST_DONE : TEXT_BRIGHT;
        g.drawString(font, questHeader, cx - font.width(questHeader) / 2, rowY, qhColor, false);

        rowY += lineH + 4;
        drawQuestObjective(g, "Kill hostile mobs", stats.getQuestKills(), PlayerStats.QUEST_KILL_TARGET, left, rowY, innerW);

        rowY += lineH + 2;
        drawQuestObjective(g, "Mine blocks", stats.getQuestBlocksMined(), PlayerStats.QUEST_MINE_TARGET, left, rowY, innerW);

        rowY += lineH + 2;
        drawQuestObjective(g, "Walk distance", stats.getQuestBlocksWalked(), PlayerStats.QUEST_WALK_TARGET, left, rowY, innerW);

        // Reward info
        rowY += lineH + 6;
        drawSep(g, left, rowY, innerW);
        rowY += 7;
        String rewardText = "Reward: " + stats.getQuestExpReward() + " EXP + Status Recovery";
        int rewardColor = stats.isQuestRewardClaimed() ? TEXT_DIM : TEXT_VALUE;
        g.drawString(font, rewardText, cx - font.width(rewardText) / 2, rowY, rewardColor, false);

        // Claim button
        if (stats.isQuestCompleted()) {
            rowY += lineH + 6;
            boolean claimed = stats.isQuestRewardClaimed();
            String claimLabel = claimed ? "CLAIMED" : "CLAIM REWARD";
            int claimW = font.width(claimLabel) + 16;
            int claimX = cx - claimW / 2;
            int claimH = lineH + 4;

            claimBtnBounds[0] = claimX;
            claimBtnBounds[1] = rowY;
            claimBtnBounds[2] = claimW;
            claimBtnBounds[3] = claimH;
            claimBtnVisible = true;

            if (claimed) {
                g.fill(claimX, rowY, claimX + claimW, rowY + claimH, TAB_INACTIVE_BG);
                drawBox(g, claimX, rowY, claimW, claimH, FRAME_DIM);
                g.drawString(font, claimLabel, claimX + 8, rowY + (claimH - font.lineHeight) / 2, TEXT_DIM, false);
            } else {
                boolean hov = isInside(lastMouseX, lastMouseY, claimBtnBounds);
                int bg = hov ? BTN_HOVER_BG : BTN_BG;
                int border = hov ? BTN_HOVER_BORDER : QUEST_DONE;
                int text = hov ? BTN_HOVER_BORDER : QUEST_DONE;
                g.fill(claimX, rowY, claimX + claimW, rowY + claimH, bg);
                drawBox(g, claimX, rowY, claimW, claimH, border);
                g.drawString(font, claimLabel, claimX + 8, rowY + (claimH - font.lineHeight) / 2, text, false);
            }
        } else {
            claimBtnVisible = false;
        }
    }

    private void renderPlaceholderTab(GuiGraphics g, String message, int cx, int contentY) {
        int msgW = font.width(message);
        g.drawString(font, message, cx - msgW / 2, contentY + lineH * 2, TEXT_DIM, false);
    }

    // === Click handling ===

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0) {
            if (isInside((int) mx, (int) my, closeBtnBounds)) {
                onClose();
                return true;
            }
            if (isInside((int) mx, (int) my, backBtnBounds)) {
                onClose();
                return true;
            }
            if (claimBtnVisible && isInside((int) mx, (int) my, claimBtnBounds)) {
                Player player = Minecraft.getInstance().player;
                if (player != null) {
                    PlayerStats stats = player.getCapability(PlayerStatsCapability.PLAYER_STATS).orElse(null);
                    if (stats != null && stats.isQuestCompleted() && !stats.isQuestRewardClaimed()) {
                        ModNetwork.sendToServer(new C2SClaimQuestRewardPacket());
                    }
                }
                return true;
            }
            Tab[] tabValues = {Tab.DAILY, Tab.SIDE, Tab.MAIN};
            for (int i = 0; i < 3; i++) {
                if (isInside((int) mx, (int) my, tabBounds[i])) {
                    activeTab = tabValues[i];
                    return true;
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
        int x1 = dx > 0 ? cx : cx - size;
        int x2 = dx > 0 ? cx + size : cx;
        int yy = dy > 0 ? cy : cy - 1;
        g.fill(x1, yy, x2, yy + 1, FRAME);
        int y1 = dy > 0 ? cy : cy - size;
        int y2 = dy > 0 ? cy + size : cy;
        int xx = dx > 0 ? cx : cx - 1;
        g.fill(xx, y1, xx + 1, y2, FRAME);
    }

    private void drawSep(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, SEP);
    }

    private void drawQuestObjective(GuiGraphics g, String label, int current, int target, int x, int y, int totalW) {
        boolean done = current >= target;
        String progress = current + "/" + target;
        int labelColor = done ? QUEST_DONE : QUEST_PENDING;
        int progColor = done ? QUEST_DONE : TEXT_VALUE;
        String prefix = done ? "[v] " : "[ ] ";
        g.drawString(font, prefix + label, x, y, labelColor, false);
        g.drawString(font, progress, x + totalW - font.width(progress), y, progColor, false);
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
