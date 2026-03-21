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

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        Player player = mc.player;
        PlayerStats stats = player.getCapability(PlayerStatsCapability.PLAYER_STATS).orElse(null);
        if (stats == null) return;
        SkillData sd = stats.getSkillData();

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
