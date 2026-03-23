package com.monody.projectleveling.client;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.capability.PlayerStatsCapability;
import com.monody.projectleveling.network.C2SEquipSkillPacket;
import com.monody.projectleveling.network.C2SSelectClassPacket;
import com.monody.projectleveling.network.C2SUnlockSkillPacket;
import com.monody.projectleveling.network.ModNetwork;
import com.monody.projectleveling.skill.PlayerClass;
import com.monody.projectleveling.skill.SkillData;
import com.monody.projectleveling.skill.SkillTooltips;
import com.monody.projectleveling.skill.SkillType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class SkillTreeScreen extends Screen {
    private static final float PANEL_WIDTH_RATIO = 0.42f;
    private static final float PANEL_HEIGHT_RATIO = 0.80f;
    private static final int MIN_PANEL_WIDTH = 240;
    private static final int MAX_PANEL_WIDTH = 320;
    private static final int MIN_PANEL_HEIGHT = 260;

    private static final int ICON_SIZE = 36;

    // Colors
    private static final int BG = 0xF0051020;
    private static final int FRAME = 0xFF1890D0;
    private static final int FRAME_DIM = 0xFF0C3050;
    private static final int TEXT_BRIGHT = 0xFFE8F0FF;
    private static final int TEXT_VALUE = 0xFFC0D8F0;
    private static final int TEXT_DIM = 0xFF405868;
    private static final int TEXT_ACCENT = 0xFF40D0FF;
    private static final int SEP = 0xFF103858;
    private static final int BTN_BG = 0xFF0C1C30;
    private static final int BTN_HOVER_BG = 0xFF142840;
    private static final int BTN_HOVER_BORDER = 0xFF30B0E8;
    private static final int BTN_TEXT = 0xFF50C8F0;
    private static final int CLOSE_NORMAL = 0xFF607888;
    private static final int CLOSE_HOVER = 0xFFFF6060;
    private static final int ICON_BG = 0xFF081828;
    private static final int ICON_BG_HOVER = 0xFF102038;
    private static final int SKILL_UNLOCKED = 0xFF40E860;
    private static final int SKILL_LOCKED = 0xFF883030;
    private static final int SKILL_MAX = 0xFFF0C020;
    private static final int EQUIP_SLOT_BG = 0xFF081828;

    // Tab colors
    private static final int TAB_ACTIVE_BG = 0xFF0C2848;
    private static final int TAB_ACTIVE_BORDER = 0xFF1890D0;
    private static final int TAB_ACTIVE_TEXT = 0xFF40D0FF;
    private static final int TAB_INACTIVE_BG = 0xFF060C18;
    private static final int TAB_INACTIVE_BORDER = 0xFF0C3050;
    private static final int TAB_INACTIVE_TEXT = 0xFF405868;
    private static final int TAB_LOCKED_TEXT = 0xFF602020;

    // Class selection colors
    private static final int CLASS_WARRIOR = 0xFFE04040;
    private static final int CLASS_ASSASSIN = 0xFF8040E0;
    private static final int CLASS_ARCHER = 0xFF40C040;
    private static final int CLASS_HEALER = 0xFFF0D040;
    private static final int CLASS_MAGE = 0xFF4080F0;
    private static final int CLASS_NINJA = 0xFFFF8040;
    private static final int CLASS_NECROMANCER = 0xFF40E0A0;
    private static final int CLASS_BEAST_MASTER = 0xFFE08040;
    private static final int PASSIVE_COLOR = 0xFF60A0C0;

    // Tier definitions
    private static final int[] TIER_NUMBERS = {0, 1, 2, 3};
    private static final String[] TAB_LABELS = {"NOVICE", "TIER 1", "TIER 2", "TIER 3"};
    private static final int[] TIER_REQ_LEVELS = {0, 10, 30, 60};

    private int activeTab = 0;
    private int panelW, panelH, panelX, panelY;
    private int pad, lineH;

    private final int[][] tabBounds = new int[4][4];
    private final int[][] skillRowBounds = new int[8][4];
    private final int[][] iconBounds = new int[8][4];
    private final int[][] plusBtnBounds = new int[8][4];
    private final int[][] equipSlotBounds = new int[SkillData.MAX_SLOTS][4];
    private final int[] closeBtnBounds = new int[4];

    // Class selection bounds
    private final int[] warriorBtnBounds = new int[4];
    private final int[] assassinBtnBounds = new int[4];
    private final int[] archerBtnBounds = new int[4];
    private final int[] healerBtnBounds = new int[4];
    private final int[] mageBtnBounds = new int[4];
    private final int[] ninjaBtnBounds = new int[4];
    private final int[] necromancerBtnBounds = new int[4];
    private final int[] beastMasterBtnBounds = new int[4];

    private SkillType pendingEquipSkill = null;
    private SkillType hoveredSkill = null;
    private boolean showClassSelection = false;
    private int scrollOffset = 0;
    private int maxScrollOffset = 0;
    private int skillListTop = 0;
    private int skillListBottom = 0;

    public SkillTreeScreen() {
        super(Component.literal("Skills"));
    }

    @Override
    protected void init() {
        super.init();
        panelW = Math.max(MIN_PANEL_WIDTH, Math.min(MAX_PANEL_WIDTH, (int) (this.width * PANEL_WIDTH_RATIO)));
        panelH = Math.max(MIN_PANEL_HEIGHT, (int) (this.height * PANEL_HEIGHT_RATIO));
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;
        pad = Math.max(10, panelW / 18);
        lineH = Math.max(12, panelH / 22);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);

        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        PlayerStats stats = player.getCapability(PlayerStatsCapability.PLAYER_STATS).orElse(null);
        if (stats == null) return;
        SkillData sd = stats.getSkillData();

        int playerLevel = stats.getLevel();
        showClassSelection = sd.getSelectedClass() == PlayerClass.NONE && playerLevel >= 10;

        int x = panelX;
        int y = panelY;

        // Background + frame
        g.fill(0, 0, this.width, this.height, 0x60000000);
        g.fill(x, y, x + panelW, y + panelH, BG);
        drawBox(g, x, y, panelW, panelH, FRAME);
        drawBox(g, x + 2, y + 2, panelW - 4, panelH - 4, FRAME_DIM);
        int cs = 10;
        drawCorner(g, x, y, cs, 1, 1);
        drawCorner(g, x + panelW, y, cs, -1, 1);
        drawCorner(g, x, y + panelH, cs, 1, -1);
        drawCorner(g, x + panelW, y + panelH, cs, -1, -1);

        // Close button
        int right = x + panelW - pad;
        int closeX = right - 6;
        int closeY = y + 6;
        closeBtnBounds[0] = closeX - 2;
        closeBtnBounds[1] = closeY - 2;
        closeBtnBounds[2] = 11;
        closeBtnBounds[3] = 11;
        boolean closeHov = isInside(mouseX, mouseY, closeBtnBounds);
        g.drawString(font, "x", closeX, closeY, closeHov ? CLOSE_HOVER : CLOSE_NORMAL, false);

        if (showClassSelection) {
            renderClassSelection(g, stats, mouseX, mouseY);
        } else {
            renderSkillView(g, stats, sd, playerLevel, mouseX, mouseY);
        }
    }

    // === Class Selection Mode ===

    private void renderClassSelection(GuiGraphics g, PlayerStats stats, int mx, int my) {
        int x = panelX;
        int y = panelY;
        int left = x + pad;
        int innerW = panelW - pad * 2;
        int cx = x + panelW / 2;

        int rowY = y + pad + 2;
        String header = "CHOOSE YOUR CLASS";
        g.drawString(font, header, cx - font.width(header) / 2, rowY, TEXT_BRIGHT, false);
        rowY += lineH + 2;

        String sub = "This choice is permanent.";
        g.drawString(font, sub, cx - font.width(sub) / 2, rowY, TEXT_DIM, false);
        rowY += lineH + 4;
        drawSep(g, left, rowY, innerW);
        rowY += 6;

        // Row 1: Warrior, Assassin, Ninja, Necromancer (4-column)
        int colGap = 4;
        int colW4 = (innerW - colGap * 3) / 4;
        int boxH = lineH * 4 + 6;

        renderClassBox(g, warriorBtnBounds, left, rowY, colW4, boxH,
                "WARRIOR", "STR", CLASS_WARRIOR, new String[]{"Bloodlust", "Iron Will", "Domain"}, mx, my);
        renderClassBox(g, assassinBtnBounds, left + (colW4 + colGap), rowY, colW4, boxH,
                "ASSASSIN", "LUK", CLASS_ASSASSIN, new String[]{"Shadow Strike", "Stealth", "Ruler's Auth."}, mx, my);
        renderClassBox(g, ninjaBtnBounds, left + (colW4 + colGap) * 2, rowY, colW4, boxH,
                "NINJA", "AGI/LUK", CLASS_NINJA, new String[]{"Shadow Clone", "Rasengan", "Sage Mode"}, mx, my);
        renderClassBox(g, necromancerBtnBounds, left + (colW4 + colGap) * 3, rowY, colW4, boxH,
                "NECRO", "INT/MND", CLASS_NECROMANCER, new String[]{"Life Drain", "Raise Skeleton", "Army of Dead"}, mx, my);

        rowY += boxH + 6;

        // Row 2: Archer, Healer, Mage, Beast Master (4-column)
        renderClassBox(g, archerBtnBounds, left, rowY, colW4, boxH,
                "ARCHER", "DEX", CLASS_ARCHER, new String[]{"Arrow Rain", "Phoenix", "Hurricane"}, mx, my);
        renderClassBox(g, healerBtnBounds, left + (colW4 + colGap), rowY, colW4, boxH,
                "HEALER", "FAI", CLASS_HEALER, new String[]{"Holy Light", "Benediction", "Angel Ray"}, mx, my);
        renderClassBox(g, mageBtnBounds, left + (colW4 + colGap) * 2, rowY, colW4, boxH,
                "MAGE", "INT", CLASS_MAGE, new String[]{"Flame Orb", "Frost Bind", "Infinity"}, mx, my);
        renderClassBox(g, beastMasterBtnBounds, left + (colW4 + colGap) * 3, rowY, colW4, boxH,
                "BEASTM", "STR/VIT", CLASS_BEAST_MASTER, new String[]{"Tiger Claw", "Bear Paw", "Power of Nat."}, mx, my);
    }

    private void renderClassBox(GuiGraphics g, int[] bounds, int bx, int by, int bw, int bh,
                                 String name, String stat, int classColor, String[] skills, int mx, int my) {
        bounds[0] = bx;
        bounds[1] = by;
        bounds[2] = bw;
        bounds[3] = bh;

        boolean hov = isInside(mx, my, bounds);
        g.fill(bx, by, bx + bw, by + bh, hov ? BTN_HOVER_BG : BTN_BG);
        drawBox(g, bx, by, bw, bh, hov ? classColor : FRAME_DIM);

        int ty = by + 3;
        g.drawString(font, name, bx + 4, ty, classColor, false);
        g.drawString(font, stat, bx + bw - font.width(stat) - 4, ty, TEXT_DIM, false);
        for (String skill : skills) {
            ty += lineH;
            String truncated = skill;
            while (font.width(truncated) > bw - 8 && truncated.length() > 3) {
                truncated = truncated.substring(0, truncated.length() - 1);
            }
            g.drawString(font, truncated, bx + 4, ty, TEXT_VALUE, false);
        }
    }

    // === Normal Skill View ===

    private void renderSkillView(GuiGraphics g, PlayerStats stats, SkillData sd, int playerLevel,
                                  int mouseX, int mouseY) {
        int x = panelX;
        int y = panelY;
        int left = x + pad;
        int innerW = panelW - pad * 2;
        int cx = x + panelW / 2;

        // Header
        int rowY = y + pad + 2;
        String header = "SKILLS";
        if (sd.getSelectedClass() != PlayerClass.NONE) {
            header += " - " + sd.getSelectedClass().getDisplayName();
        }
        g.drawString(font, header, cx - font.width(header) / 2, rowY, TEXT_BRIGHT, false);
        rowY += lineH + 2;

        // Skill points
        String spText = "SP: " + sd.getTierSP(activeTab);
        g.drawString(font, spText, cx - font.width(spText) / 2, rowY, TEXT_ACCENT, false);
        rowY += lineH + 2;
        drawSep(g, left, rowY, innerW);
        rowY += 6;

        // 4 Tier tabs
        int tabGap = 3;
        int tabCount = 4;
        int totalGap = tabGap * (tabCount - 1);
        int tabW = (innerW - totalGap) / tabCount;
        int tabH = lineH + 2;

        for (int i = 0; i < tabCount; i++) {
            int tabX = left + i * (tabW + tabGap);
            int tabY = rowY;
            tabBounds[i] = new int[]{tabX, tabY, tabW, tabH};

            boolean isActive = activeTab == i;
            boolean tierLocked = playerLevel < TIER_REQ_LEVELS[i];
            boolean isHovered = !isActive && !tierLocked && isInside(mouseX, mouseY, tabBounds[i]);

            int tbg, tborder, ttext;
            if (isActive) {
                tbg = TAB_ACTIVE_BG;
                tborder = TAB_ACTIVE_BORDER;
                ttext = TAB_ACTIVE_TEXT;
            } else if (tierLocked) {
                tbg = TAB_INACTIVE_BG;
                tborder = TAB_INACTIVE_BORDER;
                ttext = TAB_LOCKED_TEXT;
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
            String label = TAB_LABELS[i];
            int lw = font.width(label);
            g.drawString(font, label, tabX + (tabW - lw) / 2,
                    tabY + (tabH - font.lineHeight) / 2, ttext, false);
        }

        rowY += tabH + 4;

        // Show level requirement if active tab is locked
        int activeTier = TIER_NUMBERS[activeTab];
        boolean activeTierLocked = playerLevel < TIER_REQ_LEVELS[activeTab];
        if (activeTierLocked) {
            String lockMsg = "Requires Player Level " + TIER_REQ_LEVELS[activeTab];
            g.drawString(font, lockMsg, cx - font.width(lockMsg) / 2, rowY + 2, SKILL_LOCKED, false);
            rowY += lineH + 2;
        }

        drawSep(g, left, rowY, innerW);
        rowY += 8;

        // Get visible skills for current tab
        List<SkillType> skills = sd.getVisibleSkills(activeTier);
        hoveredSkill = null;

        int rowGap = 4;
        int infoX = left + ICON_SIZE + 6;
        int infoW = innerW - ICON_SIZE - 6;

        // Calculate skill list area (between tabs and equip section)
        skillListTop = rowY;
        int equipReserved = lineH * 2 + 14 + lineH + 8 + pad;
        skillListBottom = y + panelH - equipReserved;
        int availableHeight = skillListBottom - skillListTop;
        int totalContentHeight = skills.size() * (ICON_SIZE + rowGap) - (skills.isEmpty() ? 0 : rowGap);
        maxScrollOffset = Math.max(0, totalContentHeight - availableHeight);
        scrollOffset = Math.min(scrollOffset, maxScrollOffset);

        // Enable scissor clipping for skill list area
        g.enableScissor(left, skillListTop, left + innerW, skillListBottom);

        int scrolledRowY = rowY - scrollOffset;

        for (int i = 0; i < skills.size() && i < 8; i++) {
            SkillType skill = skills.get(i);
            int level = sd.getLevel(skill);
            boolean unlocked = level > 0;
            boolean canUnlock = sd.canUnlock(skill, playerLevel);
            boolean maxed = level >= skill.getMaxLevel();

            int iy = scrolledRowY;
            skillRowBounds[i] = new int[]{left, iy, innerW, ICON_SIZE};
            iconBounds[i] = new int[]{left, iy, ICON_SIZE, ICON_SIZE};

            // Only check hover if within visible area
            boolean rowHov = iy >= skillListTop - ICON_SIZE && iy < skillListBottom
                    && isInside(mouseX, mouseY, skillRowBounds[i]);
            if (rowHov) hoveredSkill = skill;

            // Icon background + border
            g.fill(left, iy, left + ICON_SIZE, iy + ICON_SIZE, rowHov ? ICON_BG_HOVER : ICON_BG);
            int borderColor = maxed ? SKILL_MAX : (unlocked ? SKILL_UNLOCKED : (canUnlock ? FRAME : SKILL_LOCKED));
            drawBox(g, left, iy, ICON_SIZE, ICON_SIZE, borderColor);

            if (pendingEquipSkill == skill) {
                drawBox(g, left + 1, iy + 1, ICON_SIZE - 2, ICON_SIZE - 2, TEXT_ACCENT);
            }

            // Abbreviation
            String abbr = skill.getAbbreviation();
            int abbrColor = unlocked ? TEXT_BRIGHT : (canUnlock ? TEXT_VALUE : TEXT_DIM);
            g.drawString(font, abbr, left + (ICON_SIZE - font.width(abbr)) / 2,
                    iy + (ICON_SIZE - font.lineHeight) / 2, abbrColor, false);

            // Info panel
            g.fill(infoX, iy, left + innerW, iy + ICON_SIZE, rowHov ? ICON_BG_HOVER : ICON_BG);
            drawBox(g, infoX, iy, infoW, ICON_SIZE, rowHov ? BTN_HOVER_BORDER : FRAME_DIM);

            int textX = infoX + 4;
            int textY1 = iy + 3;
            String displayName = skill.getDisplayName();
            if (skill.isPassive()) displayName += " (P)";
            else if (skill.isToggle()) displayName += " (T)";
            int nameColor = unlocked ? TEXT_BRIGHT : (canUnlock ? TEXT_VALUE : TEXT_DIM);
            g.drawString(font, displayName, textX, textY1, nameColor, false);

            String lvText = "Lv " + level + "/" + skill.getMaxLevel();
            int lvColor = maxed ? SKILL_MAX : TEXT_VALUE;
            g.drawString(font, lvText, left + innerW - font.width(lvText) - 4, textY1, lvColor, false);

            int textY2 = iy + 3 + font.lineHeight + 2;
            if (activeTierLocked) {
                g.drawString(font, "Lv " + TIER_REQ_LEVELS[activeTab] + " required", textX, textY2, TEXT_DIM, false);
            } else if (!unlocked && !canUnlock) {
                g.drawString(font, getReqText(skill, sd), textX, textY2, TEXT_DIM, false);
            } else if (skill.isPassive()) {
                g.drawString(font, "Passive - Always Active", textX, textY2, PASSIVE_COLOR, false);
            } else {
                int displayLevel = Math.max(level, 1);
                String mpText = skill.isToggle()
                        ? "MP: " + String.format("%.1f", skill.getToggleDrainPercent(displayLevel)) + "%/sec"
                        : "MP: " + skill.getMpCost(displayLevel);
                g.drawString(font, mpText, textX, textY2, TEXT_DIM, false);

                String cdText = "CD: " + String.format("%.1f", skill.getCooldownTicks(displayLevel) / 20.0) + "s";
                g.drawString(font, cdText, textX + font.width(mpText) + 8, textY2, TEXT_DIM, false);
            }

            // [+] button
            if (canUnlock && !maxed) {
                int pW = 14;
                int pH = 14;
                int px = left + innerW - pW - 3;
                int py = iy + ICON_SIZE - pH - 3;
                plusBtnBounds[i] = new int[]{px, py, pW, pH};

                boolean plusHov = isInside(mouseX, mouseY, plusBtnBounds[i]);
                int plusColor = unlocked ? SKILL_MAX : SKILL_UNLOCKED;
                g.fill(px, py, px + pW, py + pH, plusHov ? BTN_HOVER_BG : BTN_BG);
                drawBox(g, px, py, pW, pH, plusHov ? BTN_HOVER_BORDER : plusColor);
                g.drawString(font, "+", px + (pW - font.width("+")) / 2,
                        py + (pH - font.lineHeight) / 2, plusHov ? BTN_HOVER_BORDER : plusColor, false);
            } else {
                plusBtnBounds[i] = new int[]{0, 0, 0, 0};
            }

            scrolledRowY += ICON_SIZE + rowGap;
        }

        g.disableScissor();

        // Clear unused bounds
        for (int i = skills.size(); i < 8; i++) {
            skillRowBounds[i] = new int[]{0, 0, 0, 0};
            iconBounds[i] = new int[]{0, 0, 0, 0};
            plusBtnBounds[i] = new int[]{0, 0, 0, 0};
        }

        // Show "no skills" message if empty
        if (skills.isEmpty() && !activeTierLocked) {
            String msg = sd.getSelectedClass() == PlayerClass.NONE
                    ? "Choose a class at Lv 10" : "No skills in this tier";
            g.drawString(font, msg, cx - font.width(msg) / 2, skillListTop, TEXT_DIM, false);
        }

        // Scroll indicator
        if (maxScrollOffset > 0) {
            int barX = left + innerW - 3;
            int barH = Math.max(10, availableHeight * availableHeight / totalContentHeight);
            int barY = skillListTop + (int)((float) scrollOffset / maxScrollOffset * (availableHeight - barH));
            g.fill(barX, barY, barX + 2, barY + barH, FRAME_DIM);
        }

        // Equip slots section at bottom
        int equipSectionY = y + panelH - pad - lineH * 2 - 14;
        drawSep(g, left, equipSectionY, innerW);
        equipSectionY += 6;

        if (pendingEquipSkill != null) {
            String eqText = "Equip " + pendingEquipSkill.getDisplayName() + " to:";
            while (font.width(eqText) > innerW && eqText.length() > 10) {
                eqText = eqText.substring(0, eqText.length() - 1);
            }
            g.drawString(font, eqText, cx - font.width(eqText) / 2, equipSectionY, TEXT_ACCENT, false);
        } else {
            String eqLabel = "EQUIPPED";
            g.drawString(font, eqLabel, cx - font.width(eqLabel) / 2, equipSectionY, TEXT_DIM, false);
        }
        equipSectionY += lineH + 2;

        KeyMapping[] slotMappings = {
                KeyBindings.SKILL_SLOT_1, KeyBindings.SKILL_SLOT_2, KeyBindings.SKILL_SLOT_3,
                KeyBindings.SKILL_SLOT_4, KeyBindings.SKILL_SLOT_5, KeyBindings.SKILL_SLOT_6,
                KeyBindings.SKILL_SLOT_7
        };
        String[] slotKeys = new String[SkillData.MAX_SLOTS];
        for (int i = 0; i < SkillData.MAX_SLOTS; i++) {
            String name = slotMappings[i].getKey().getDisplayName().getString().toUpperCase();
            if (name.startsWith("BUTTON ")) {
                name = "B" + name.substring(7);
            }
            slotKeys[i] = name;
        }
        int slotW = (innerW - (SkillData.MAX_SLOTS - 1) * 4) / SkillData.MAX_SLOTS;
        int slotH = lineH + 6;
        for (int i = 0; i < SkillData.MAX_SLOTS; i++) {
            int sx = left + i * (slotW + 4);
            equipSlotBounds[i] = new int[]{sx, equipSectionY, slotW, slotH};

            boolean hov = isInside(mouseX, mouseY, equipSlotBounds[i]);
            SkillType equipped = sd.getEquipped(i);
            int bg = hov ? BTN_HOVER_BG : EQUIP_SLOT_BG;
            int border = hov ? BTN_HOVER_BORDER : FRAME_DIM;

            g.fill(sx, equipSectionY, sx + slotW, equipSectionY + slotH, bg);
            drawBox(g, sx, equipSectionY, slotW, slotH, border);

            String keyLabel = "[" + slotKeys[i] + "]";
            g.drawString(font, keyLabel, sx + 3, equipSectionY + 2, TEXT_DIM, false);

            String skillName = equipped != null ? equipped.getDisplayName() : "---";
            int nameColor = equipped != null ? TEXT_VALUE : TEXT_DIM;
            int nameX = sx + font.width(keyLabel) + 4;
            String truncated = skillName;
            while (font.width(truncated) > slotW - font.width(keyLabel) - 8 && truncated.length() > 3) {
                truncated = truncated.substring(0, truncated.length() - 1);
            }
            g.drawString(font, truncated, nameX, equipSectionY + 2, nameColor, false);

            if (equipped != null && hov) {
                g.drawString(font, "R-click: unequip", sx, equipSectionY + slotH + 1, TEXT_DIM, false);
            }
        }

        // Tooltip
        if (hoveredSkill != null) {
            renderTooltip(g, stats, sd, hoveredSkill, playerLevel, mouseX, mouseY);
        }
    }

    private void renderTooltip(GuiGraphics g, PlayerStats stats, SkillData sd, SkillType skill,
                                int playerLevel, int mx, int my) {
        int level = sd.getLevel(skill);
        boolean unlocked = level > 0;
        boolean canUnlock = sd.canUnlock(skill, playerLevel);
        boolean maxed = level >= skill.getMaxLevel();
        int displayLevel = Math.max(level, 1);

        java.util.List<int[]> lines = new java.util.ArrayList<>();
        java.util.List<String> texts = new java.util.ArrayList<>();

        String typeTag = skill.isPassive() ? " (Passive)" : (skill.isToggle() ? " (Toggle)" : "");
        texts.add(skill.getDisplayName() + typeTag);
        lines.add(new int[]{TEXT_ACCENT});

        texts.add(skill.getDescription());
        lines.add(new int[]{TEXT_DIM});

        texts.add("Level: " + level + "/" + skill.getMaxLevel());
        lines.add(new int[]{maxed ? SKILL_MAX : TEXT_VALUE});

        if (skill.isPassive()) {
            texts.add("Always active when learned");
            lines.add(new int[]{PASSIVE_COLOR});
        } else {
            String mpLine;
            if (skill.isToggle()) {
                mpLine = "MP: " + String.format("%.1f", skill.getToggleDrainPercent(displayLevel)) + "%/sec";
            } else {
                mpLine = "MP: " + skill.getMpCost(displayLevel) + "  CD: "
                        + String.format("%.1f", skill.getCooldownTicks(displayLevel) / 20.0) + "s";
            }
            texts.add(mpLine);
            lines.add(new int[]{TEXT_VALUE});
        }

        if (!unlocked && !canUnlock) {
            texts.add(getReqText(skill, sd));
            lines.add(new int[]{SKILL_LOCKED});
        } else if (canUnlock && !maxed) {
            texts.add(unlocked ? "[+] Level Up (1 SP)" : "[+] Unlock (1 SP)");
            lines.add(new int[]{SKILL_UNLOCKED});
        } else if (maxed) {
            texts.add("MAX LEVEL");
            lines.add(new int[]{SKILL_MAX});
        }

        if (unlocked && !skill.isPassive()) {
            texts.add("Click to equip");
            lines.add(new int[]{TEXT_DIM});
        }

        boolean shiftHeld = hasShiftDown();
        if (shiftHeld) {
            texts.add("--- Details ---");
            lines.add(new int[]{FRAME});
            SkillTooltips.addDetailLines(texts, lines, skill, displayLevel, stats);
        } else {
            texts.add("[Shift] More details");
            lines.add(new int[]{TEXT_DIM});
        }

        int tipPad = 6;
        int tipLineH = font.lineHeight + 2;
        int tipW = 0;
        for (String t : texts) tipW = Math.max(tipW, font.width(t));
        tipW += tipPad * 2;
        int tipH = tipPad * 2 + texts.size() * tipLineH;

        int tx = mx + 12;
        int ty = my - tipH - 4;
        if (tx + tipW > this.width) tx = mx - tipW - 12;
        if (ty < 0) ty = my + 16;
        if (tx < 0) tx = 4;

        g.fill(tx, ty, tx + tipW, ty + tipH, 0xF0051020);
        drawBox(g, tx, ty, tipW, tipH, FRAME);

        int textY = ty + tipPad;
        for (int i = 0; i < texts.size(); i++) {
            g.drawString(font, texts.get(i), tx + tipPad, textY, lines.get(i)[0], false);
            textY += tipLineH;
        }
    }

    private String getReqText(SkillType skill, SkillData sd) {
        if (sd.getTierSP(skill.getTier()) <= 0) return "No skill points for this tier";
        int reqLevel = skill.getRequiredPlayerLevel();
        if (reqLevel > 0) return "Requires Player Lv " + reqLevel;
        PlayerClass req = skill.getRequiredClass();
        if (req != null && req != sd.getSelectedClass()) {
            return "Requires " + req.getDisplayName() + " class";
        }
        return "Locked";
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0) {
            // Close
            if (isInside((int) mx, (int) my, closeBtnBounds)) {
                onClose();
                return true;
            }

            // Class selection
            if (showClassSelection) {
                int[][] classBounds = {warriorBtnBounds, assassinBtnBounds, ninjaBtnBounds, necromancerBtnBounds, archerBtnBounds, healerBtnBounds, mageBtnBounds, beastMasterBtnBounds};
                PlayerClass[] classes = {PlayerClass.WARRIOR, PlayerClass.ASSASSIN, PlayerClass.NINJA, PlayerClass.NECROMANCER, PlayerClass.ARCHER, PlayerClass.HEALER, PlayerClass.MAGE, PlayerClass.BEAST_MASTER};
                for (int i = 0; i < classBounds.length; i++) {
                    if (isInside((int) mx, (int) my, classBounds[i])) {
                        ModNetwork.sendToServer(new C2SSelectClassPacket(classes[i].getId()));
                        return true;
                    }
                }
                return super.mouseClicked(mx, my, btn);
            }

            // Tab clicks
            Player player = Minecraft.getInstance().player;
            PlayerStats stats = null;
            if (player != null) {
                stats = player.getCapability(PlayerStatsCapability.PLAYER_STATS).orElse(null);
            }
            int playerLevel = stats != null ? stats.getLevel() : 0;

            for (int i = 0; i < 4; i++) {
                if (isInside((int) mx, (int) my, tabBounds[i])) {
                    if (playerLevel >= TIER_REQ_LEVELS[i]) {
                        activeTab = i;
                        pendingEquipSkill = null;
                        scrollOffset = 0;
                    }
                    return true;
                }
            }

            // Equip slot selection
            if (pendingEquipSkill != null) {
                for (int i = 0; i < SkillData.MAX_SLOTS; i++) {
                    if (isInside((int) mx, (int) my, equipSlotBounds[i])) {
                        ModNetwork.sendToServer(new C2SEquipSkillPacket(pendingEquipSkill.getId(), i));
                        pendingEquipSkill = null;
                        return true;
                    }
                }
            }

            // Skill row interactions
            SkillData sd = stats != null ? stats.getSkillData() : null;
            if (sd != null) {
                List<SkillType> skills = sd.getVisibleSkills(TIER_NUMBERS[activeTab]);
                for (int i = 0; i < skills.size() && i < 8; i++) {
                    if (isInside((int) mx, (int) my, plusBtnBounds[i])) {
                        ModNetwork.sendToServer(new C2SUnlockSkillPacket(skills.get(i).getId()));
                        return true;
                    }

                    if (isInside((int) mx, (int) my, skillRowBounds[i])) {
                        SkillType clicked = skills.get(i);
                        if (sd.isUnlocked(clicked) && !clicked.isPassive()) {
                            if (pendingEquipSkill == clicked) {
                                pendingEquipSkill = null;
                            } else {
                                pendingEquipSkill = clicked;
                            }
                        }
                        return true;
                    }
                }
            }

            pendingEquipSkill = null;
        }

        // Right-click to unequip
        if (btn == 1) {
            for (int i = 0; i < SkillData.MAX_SLOTS; i++) {
                if (isInside((int) mx, (int) my, equipSlotBounds[i])) {
                    ModNetwork.sendToServer(new C2SEquipSkillPacket("", i));
                    return true;
                }
            }
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (maxScrollOffset > 0) {
            scrollOffset = (int) Math.max(0, Math.min(maxScrollOffset, scrollOffset - delta * 20));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
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

    private static boolean isInside(int mx, int my, int[] bounds) {
        return bounds[2] > 0 && mx >= bounds[0] && mx < bounds[0] + bounds[2]
                && my >= bounds[1] && my < bounds[1] + bounds[3];
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
