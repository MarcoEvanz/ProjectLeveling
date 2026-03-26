package com.monody.projectleveling.client;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.capability.PlayerStatsCapability;
import com.monody.projectleveling.dimension.ModDimensions;
import com.monody.projectleveling.mob.MobLevelUtil;
import com.monody.projectleveling.network.C2SReturnFromDungeonPacket;
import com.monody.projectleveling.network.C2STeleportToZonePacket;
import com.monody.projectleveling.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class DungeonScreen extends Screen {

    private static final float PANEL_WIDTH_RATIO = 0.42f;
    private static final float PANEL_HEIGHT_RATIO = 0.80f;
    private static final int MIN_PANEL_WIDTH = 240;
    private static final int MAX_PANEL_WIDTH = 320;
    private static final int MIN_PANEL_HEIGHT = 260;

    private static final int BG = 0xF0051020;
    private static final int FRAME = 0xFF1890D0;
    private static final int FRAME_DIM = 0xFF0C3050;
    private static final int TEXT_BRIGHT = 0xFFE8F0FF;
    private static final int TEXT_VALUE = 0xFFC0D8F0;
    private static final int TEXT_ACCENT = 0xFF40D0FF;
    private static final int SEP = 0xFF103858;
    private static final int BTN_BG = 0xFF0C1C30;
    private static final int BTN_BORDER = 0xFF1880B0;
    private static final int BTN_HOVER_BG = 0xFF142840;
    private static final int BTN_HOVER_BORDER = 0xFF30B0E8;
    private static final int ZONE_LOCKED_BG = 0xFF0A0A18;
    private static final int ZONE_LOCKED_BORDER = 0xFF602020;
    private static final int ZONE_LOCKED_TEXT = 0xFF603030;
    private static final int RETURN_TEXT = 0xFFFF8040;
    private static final int CLOSE_NORMAL = 0xFF607888;
    private static final int CLOSE_HOVER = 0xFFFF6060;

    private int panelW, panelH, panelX, panelY;
    private int pad, lineH;

    private static final int NUM_ZONES = ModDimensions.NUM_DUNGEONS;
    private final int[][] zoneBtnBounds = new int[NUM_ZONES][4];
    private final int[] returnBtnBounds = new int[4];
    private final int[] closeBtnBounds = new int[4];
    private boolean returnBtnVisible = false;

    private int scrollOffset = 0;
    private int maxScroll = 0;
    private int scrollAreaTop;
    private int scrollAreaBottom;

    public DungeonScreen() {
        super(Component.literal("Dungeons"));
    }

    @Override
    protected void init() {
        super.init();
        panelW = Math.max(MIN_PANEL_WIDTH, Math.min(MAX_PANEL_WIDTH, (int) (width * PANEL_WIDTH_RATIO)));
        panelH = Math.max(MIN_PANEL_HEIGHT, (int) (height * PANEL_HEIGHT_RATIO));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        pad = Math.max(10, panelW / 18);
        lineH = Math.max(13, panelH / 20);
        scrollOffset = 0;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);

        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        PlayerStats stats = player.getCapability(PlayerStatsCapability.PLAYER_STATS).orElse(null);
        if (stats == null) return;

        int playerLevel = stats.getLevel();
        int x = panelX, y = panelY;
        int left = x + pad, right = x + panelW - pad;
        int innerW = panelW - pad * 2;
        int cx = x + panelW / 2;

        // Background + frame
        g.fill(0, 0, width, height, 0x60000000);
        g.fill(x, y, x + panelW, y + panelH, BG);
        drawBox(g, x, y, panelW, panelH, FRAME);
        drawBox(g, x + 2, y + 2, panelW - 4, panelH - 4, FRAME_DIM);

        // Close button [X]
        String closeLabel = "X";
        int closeW = font.width(closeLabel) + 6;
        int closeX = x + panelW - closeW - 4;
        int closeY = y + 4;
        closeBtnBounds[0] = closeX;
        closeBtnBounds[1] = closeY;
        closeBtnBounds[2] = closeW;
        closeBtnBounds[3] = font.lineHeight + 2;
        boolean closeHov = isInside(mouseX, mouseY, closeBtnBounds);
        g.drawString(font, closeLabel, closeX + 3, closeY + 1, closeHov ? CLOSE_HOVER : CLOSE_NORMAL, false);

        // Title
        int ty = y + pad;
        g.drawCenteredString(font, "DUNGEONS", cx, ty, TEXT_ACCENT);
        ty += lineH;
        g.drawCenteredString(font, "Your Level: " + playerLevel, cx, ty, TEXT_VALUE);
        ty += lineH + 4;

        // Separator
        g.fill(left, ty, right, ty + 1, SEP);
        ty += 6;

        // Return button (only if in a dungeon)
        boolean inDungeon = ModDimensions.isDungeon(player.level().dimension());
        returnBtnVisible = inDungeon;
        if (inDungeon) {
            int btnW = innerW - 20;
            int btnH = 16;
            int btnX = cx - btnW / 2;
            returnBtnBounds[0] = btnX;
            returnBtnBounds[1] = ty;
            returnBtnBounds[2] = btnW;
            returnBtnBounds[3] = btnH;
            boolean hover = isInside(mouseX, mouseY, returnBtnBounds);
            g.fill(btnX, ty, btnX + btnW, ty + btnH, hover ? BTN_HOVER_BG : BTN_BG);
            drawBox(g, btnX, ty, btnW, btnH, hover ? BTN_HOVER_BORDER : BTN_BORDER);
            g.drawCenteredString(font, "Return to Overworld", cx, ty + 4, RETURN_TEXT);
            ty += btnH + 6;
        }

        // Zone list (scrollable)
        int btnH = 22;
        int btnGap = 4;
        scrollAreaTop = ty;
        scrollAreaBottom = y + panelH - pad;
        int visibleHeight = scrollAreaBottom - scrollAreaTop;
        int totalHeight = NUM_ZONES * (btnH + btnGap);
        maxScroll = Math.max(0, totalHeight - visibleHeight);

        g.enableScissor(left, scrollAreaTop, right, scrollAreaBottom);

        for (int i = 0; i < NUM_ZONES; i++) {
            int btnY = scrollAreaTop + i * (btnH + btnGap) - scrollOffset;
            int btnW = innerW - 10;
            int btnX = cx - btnW / 2;

            zoneBtnBounds[i][0] = btnX;
            zoneBtnBounds[i][1] = btnY;
            zoneBtnBounds[i][2] = btnW;
            zoneBtnBounds[i][3] = btnH;

            int minLv = MobLevelUtil.getDungeonMinLevel(i);
            int maxLv = MobLevelUtil.getDungeonMaxLevel(i);
            boolean locked = playerLevel < minLv;
            boolean visible = btnY + btnH > scrollAreaTop && btnY < scrollAreaBottom;
            boolean hover = !locked && visible && isInside(mouseX, mouseY, zoneBtnBounds[i])
                    && mouseY >= scrollAreaTop && mouseY < scrollAreaBottom;

            int bgColor = locked ? ZONE_LOCKED_BG : (hover ? BTN_HOVER_BG : BTN_BG);
            int borderColor = locked ? ZONE_LOCKED_BORDER : (hover ? BTN_HOVER_BORDER : BTN_BORDER);
            int textColor = locked ? ZONE_LOCKED_TEXT : (hover ? TEXT_BRIGHT : TEXT_VALUE);

            g.fill(btnX, btnY, btnX + btnW, btnY + btnH, bgColor);
            drawBox(g, btnX, btnY, btnW, btnH, borderColor);

            String label = "Dungeon " + (i + 1) + "  [Lv." + minLv + " - " + maxLv + "]";
            g.drawCenteredString(font, label, cx, btnY + (btnH - 8) / 2, textColor);

            if (locked) {
                String lockLabel = "LOCKED";
                int lockW = font.width(lockLabel);
                g.drawString(font, lockLabel, btnX + btnW - lockW - 6, btnY + (btnH - 8) / 2, ZONE_LOCKED_BORDER, false);
            }
        }

        g.disableScissor();

        // Scrollbar
        if (maxScroll > 0) {
            int barH = Math.max(10, visibleHeight * visibleHeight / totalHeight);
            int barY = scrollAreaTop + (int) ((float) scrollOffset / maxScroll * (visibleHeight - barH));
            int barX = right - 3;
            g.fill(barX, barY, barX + 2, barY + barH, FRAME_DIM);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        if (isInside((int) mouseX, (int) mouseY, closeBtnBounds)) {
            onClose();
            return true;
        }

        if (returnBtnVisible && isInside((int) mouseX, (int) mouseY, returnBtnBounds)) {
            ModNetwork.sendToServer(new C2SReturnFromDungeonPacket());
            onClose();
            return true;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) return false;
        PlayerStats stats = player.getCapability(PlayerStatsCapability.PLAYER_STATS).orElse(null);
        if (stats == null) return false;

        for (int i = 0; i < NUM_ZONES; i++) {
            if ((int) mouseY < scrollAreaTop || (int) mouseY >= scrollAreaBottom) continue;
            if (isInside((int) mouseX, (int) mouseY, zoneBtnBounds[i])) {
                int minLv = MobLevelUtil.getDungeonMinLevel(i);
                if (stats.getLevel() >= minLv) {
                    ModNetwork.sendToServer(new C2STeleportToZonePacket(i));
                    onClose();
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (delta * 16)));
        return true;
    }

    private void drawBox(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    private boolean isInside(int mx, int my, int[] bounds) {
        return mx >= bounds[0] && mx < bounds[0] + bounds[2]
                && my >= bounds[1] && my < bounds[1] + bounds[3];
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
