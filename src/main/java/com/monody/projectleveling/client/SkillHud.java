package com.monody.projectleveling.client;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.capability.PlayerStatsCapability;
import com.monody.projectleveling.skill.SkillData;
import com.monody.projectleveling.skill.SkillType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class SkillHud implements IGuiOverlay {
    private static final int SLOT_SIZE = 20;
    private static final int SLOT_GAP = 2;
    private static final int BG = 0xB0051020;
    private static final int BORDER = 0xFF0C3050;
    private static final int BORDER_ACTIVE = 0xFF40E860;
    private static final int CD_OVERLAY = 0xA0000000;
    private static final int TEXT_KEY = 0xFF405868;
    private static final int TEXT_CD = 0xFFFF6060;
    private static final int TEXT_NAME = 0xFFC0D8F0;
    // Channeling bar
    private static final int CHANNEL_BAR_WIDTH = 140;
    private static final int CHANNEL_BAR_HEIGHT = 10;
    private static final int CHANNEL_BG = 0xB0000000;
    private static final int CHANNEL_BORDER_COLOR = 0xFF404060;
    private static final int CHANNEL_FILL = 0xFF4090FF;
    private static final int CHANNEL_FILL_END = 0xFFFF4040;
    private static final int CHANNEL_LABEL_COLOR = 0xFFA0C0FF;
    // MP bar
    private static final int MP_BAR_WIDTH = 81;
    private static final int MP_BAR_HEIGHT = 11;
    private static final int MP_BG = 0xC0000000;
    private static final int MP_BORDER = 0xFF202040;
    private static final int MP_FILL = 0xFF3060D0;
    private static final int MP_FILL_LOW = 0xFFD04040;
    // Client-side time tracking for smooth animation
    private static long channelStartMs = 0;
    private static boolean wasChanneling = false;

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        Player player = mc.player;
        PlayerStats stats = player.getCapability(PlayerStatsCapability.PLAYER_STATS).orElse(null);
        if (stats == null) return;
        SkillData sd = stats.getSkillData();

        // MP bar (always visible)
        renderMpBar(g, mc.font, stats, screenWidth, screenHeight);

        // Check if any skills are equipped
        boolean hasAny = false;
        for (int i = 0; i < SkillData.MAX_SLOTS; i++) {
            if (sd.getEquipped(i) != null) {
                hasAny = true;
                break;
            }
        }
        if (!hasAny) return;

        Font font = mc.font;
        String[] keys = getKeyLabels();

        int startY = screenHeight - SLOT_SIZE - 1; // bottom-aligned with hotbar
        boolean offhandLeft = mc.options.mainHand().get() == HumanoidArm.RIGHT;
        boolean offhandRight = !offhandLeft;

        // Left group (slots 0-3): left of hotbar, skip offhand if right-handed
        int leftCount = 4;
        int hotbarLeft = (screenWidth - 182) / 2;
        int leftWidth = leftCount * SLOT_SIZE + (leftCount - 1) * SLOT_GAP;
        int leftStartX = hotbarLeft - leftWidth - (offhandLeft ? 36 : 4);

        // Right group (slots 4-6): right of hotbar, skip offhand if left-handed
        int hotbarRight = (screenWidth + 182) / 2;
        int rightStartX = hotbarRight + (offhandRight ? 36 : 4);

        for (int i = 0; i < SkillData.MAX_SLOTS; i++) {
            SkillType skill = sd.getEquipped(i);
            int sx = i < leftCount
                    ? leftStartX + i * (SLOT_SIZE + SLOT_GAP)
                    : rightStartX + (i - leftCount) * (SLOT_SIZE + SLOT_GAP);
            int sy = startY;

            // Background
            g.fill(sx, sy, sx + SLOT_SIZE, sy + SLOT_SIZE, BG);

            if (skill == null) {
                // Empty slot
                drawSlotBorder(g, sx, sy, BORDER);
                String key = keys[i];
                g.drawString(font, key, sx + (SLOT_SIZE - font.width(key)) / 2,
                        sy + (SLOT_SIZE - font.lineHeight) / 2, TEXT_KEY, false);
                continue;
            }

            // Active toggle indicator
            boolean toggleActive = skill.isToggle() && sd.isToggleActive(skill);
            drawSlotBorder(g, sx, sy, toggleActive ? BORDER_ACTIVE : BORDER);

            // Skill abbreviation (first 3 chars)
            String abbr = skill.getDisplayName().length() > 3
                    ? skill.getDisplayName().substring(0, 3)
                    : skill.getDisplayName();
            g.drawString(font, abbr, sx + (SLOT_SIZE - font.width(abbr)) / 2,
                    sy + 2, TEXT_NAME, false);

            // Key label at bottom
            g.drawString(font, keys[i], sx + (SLOT_SIZE - font.width(keys[i])) / 2,
                    sy + SLOT_SIZE - font.lineHeight, TEXT_KEY, false);

            // Cooldown overlay
            int cdRemaining = sd.getCooldownRemaining(skill);
            if (cdRemaining > 0 && !toggleActive) {
                g.fill(sx + 1, sy + 1, sx + SLOT_SIZE - 1, sy + SLOT_SIZE - 1, CD_OVERLAY);
                String cdText = String.valueOf((cdRemaining + 19) / 20);
                g.drawString(font, cdText, sx + (SLOT_SIZE - font.width(cdText)) / 2,
                        sy + (SLOT_SIZE - font.lineHeight) / 2, TEXT_CD, false);
            }
        }

        // === Universal channeling bar (anchored above MP bar) ===
        int mpBarX = screenWidth / 2 + 91 + 4;
        int mpBarY = screenHeight - 36;
        renderChannelBar(g, font, sd, mpBarX, mpBarY);
    }

    private static void renderChannelBar(GuiGraphics g, Font font, SkillData sd,
                                         int mpBarX, int mpBarY) {
        int maxTicks = sd.getChannelMaxTicks();
        boolean channeling = maxTicks > 0;

        // Track channel start/end using real time
        if (channeling && !wasChanneling) {
            channelStartMs = System.currentTimeMillis();
        }
        wasChanneling = channeling;
        if (!channeling) return;

        // Millisecond-precise progress from real elapsed time
        float maxMs = maxTicks * 50.0f; // ticks to milliseconds (1 tick = 50ms)
        long elapsedMs = System.currentTimeMillis() - channelStartMs;
        float progress = Math.min(1.0f, elapsedMs / maxMs);

        // Position: right above the MP bar, left-aligned
        int barX = mpBarX;
        int barY = mpBarY - CHANNEL_BAR_HEIGHT - 3;

        // Skill name label above the bar
        String skillName = sd.getChannelSkillName();
        if (!skillName.isEmpty()) {
            g.drawString(font, skillName,
                    barX + (CHANNEL_BAR_WIDTH - font.width(skillName)) / 2,
                    barY - font.lineHeight - 2, CHANNEL_LABEL_COLOR, true);
        }

        // Background + border
        g.fill(barX - 1, barY - 1,
                barX + CHANNEL_BAR_WIDTH + 1, barY + CHANNEL_BAR_HEIGHT + 1, CHANNEL_BORDER_COLOR);
        g.fill(barX, barY,
                barX + CHANNEL_BAR_WIDTH, barY + CHANNEL_BAR_HEIGHT, CHANNEL_BG);

        // Fill bar (color shifts from blue to red as it fills)
        int fillWidth = (int) (CHANNEL_BAR_WIDTH * progress);
        if (fillWidth > 0) {
            int color = lerpColor(CHANNEL_FILL, CHANNEL_FILL_END, progress);
            g.fill(barX, barY, barX + fillWidth, barY + CHANNEL_BAR_HEIGHT, color);
        }

        // Time remaining text centered inside the bar
        float secondsLeft = Math.max(0, (maxMs - elapsedMs) / 1000.0f);
        String timeText = String.format("%.1fs", secondsLeft);
        g.drawString(font, timeText,
                barX + (CHANNEL_BAR_WIDTH - font.width(timeText)) / 2,
                barY + (CHANNEL_BAR_HEIGHT - font.lineHeight) / 2 + 1, 0xFFFFFFFF, true);
    }

    private static void renderMpBar(GuiGraphics g, Font font, PlayerStats stats,
                                     int screenWidth, int screenHeight) {
        int maxMp = stats.getMaxMp();
        if (maxMp <= 0) return;
        int curMp = stats.getCurrentMp();
        float ratio = (float) curMp / maxMp;

        // Position: right of hunger bar row
        int barX = screenWidth / 2 + 91 + 4;
        int barY = screenHeight - 36;

        // Border
        g.fill(barX - 1, barY - 1,
                barX + MP_BAR_WIDTH + 1, barY + MP_BAR_HEIGHT + 1, MP_BORDER);
        // Background
        g.fill(barX, barY, barX + MP_BAR_WIDTH, barY + MP_BAR_HEIGHT, MP_BG);

        // Fill (blue, turns red when low <20%)
        int fillWidth = (int) (MP_BAR_WIDTH * ratio);
        if (fillWidth > 0) {
            int color = ratio < 0.2f ? MP_FILL_LOW : MP_FILL;
            g.fill(barX, barY, barX + fillWidth, barY + MP_BAR_HEIGHT, color);
        }

        // Text: "currentMP / maxMP" centered inside the bar
        String mpText = curMp + " / " + maxMp;
        g.drawString(font, mpText,
                barX + (MP_BAR_WIDTH - font.width(mpText)) / 2,
                barY + (MP_BAR_HEIGHT - font.lineHeight) / 2 + 1, 0xFFFFFFFF, true);
    }

    private static int lerpColor(int from, int to, float t) {
        int fa = (from >> 24) & 0xFF, fr = (from >> 16) & 0xFF, fg = (from >> 8) & 0xFF, fb = from & 0xFF;
        int ta = (to >> 24) & 0xFF, tr = (to >> 16) & 0xFF, tg = (to >> 8) & 0xFF, tb = to & 0xFF;
        int a = (int) (fa + (ta - fa) * t);
        int r = (int) (fr + (tr - fr) * t);
        int gr = (int) (fg + (tg - fg) * t);
        int b = (int) (fb + (tb - fb) * t);
        return (a << 24) | (r << 16) | (gr << 8) | b;
    }

    private static String[] getKeyLabels() {
        KeyMapping[] slots = {
                KeyBindings.SKILL_SLOT_1, KeyBindings.SKILL_SLOT_2, KeyBindings.SKILL_SLOT_3,
                KeyBindings.SKILL_SLOT_4, KeyBindings.SKILL_SLOT_5, KeyBindings.SKILL_SLOT_6,
                KeyBindings.SKILL_SLOT_7
        };
        String[] labels = new String[SkillData.MAX_SLOTS];
        for (int i = 0; i < SkillData.MAX_SLOTS; i++) {
            String name = slots[i].getKey().getDisplayName().getString().toUpperCase();
            if (name.startsWith("BUTTON ")) {
                name = "B" + name.substring(7);
            }
            labels[i] = name;
        }
        return labels;
    }

    private static void drawSlotBorder(GuiGraphics g, int x, int y, int color) {
        int s = SLOT_SIZE;
        g.fill(x, y, x + s, y + 1, color);
        g.fill(x, y + s - 1, x + s, y + s, color);
        g.fill(x, y + 1, x + 1, y + s - 1, color);
        g.fill(x + s - 1, y + 1, x + s, y + s - 1, color);
    }
}
