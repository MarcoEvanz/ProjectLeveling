package com.monody.projectleveling.client;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.capability.PlayerStatsCapability;
import com.monody.projectleveling.network.C2SEquipSkillPacket;
import com.monody.projectleveling.network.C2SSelectClassPacket;
import com.monody.projectleveling.network.C2SUnlockSkillPacket;
import com.monody.projectleveling.network.ModNetwork;
import com.monody.projectleveling.skill.PlayerClass;
import com.monody.projectleveling.skill.SkillData;
import com.monody.projectleveling.skill.SkillType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private static final int CLASS_NECROMANCER = 0xFF40E0A0;
    private static final int PASSIVE_COLOR = 0xFF60A0C0;

    // Abbreviation map
    private static final Map<SkillType, String> ABBREV_MAP = new HashMap<>();
    static {
        // T0
        ABBREV_MAP.put(SkillType.DASH, "DSH");
        ABBREV_MAP.put(SkillType.ENDURANCE, "END");
        // Warrior
        ABBREV_MAP.put(SkillType.BLOODLUST, "BL");
        ABBREV_MAP.put(SkillType.WAR_CRY, "WC");
        ABBREV_MAP.put(SkillType.WEAPON_MASTERY, "WM");
        ABBREV_MAP.put(SkillType.IRON_WILL, "IW");
        ABBREV_MAP.put(SkillType.GROUND_SLAM, "GS");
        ABBREV_MAP.put(SkillType.RAGE, "RG");
        ABBREV_MAP.put(SkillType.DOMAIN_OF_MONARCH, "DM");
        ABBREV_MAP.put(SkillType.UNBREAKABLE, "UB");
        ABBREV_MAP.put(SkillType.BERSERKER_SPIRIT, "BS");
        // Assassin
        ABBREV_MAP.put(SkillType.SHADOW_STRIKE, "SS");
        ABBREV_MAP.put(SkillType.VENOM, "VN");
        ABBREV_MAP.put(SkillType.CRITICAL_EDGE, "CE");
        ABBREV_MAP.put(SkillType.STEALTH, "STH");
        ABBREV_MAP.put(SkillType.BLADE_FURY, "BF");
        ABBREV_MAP.put(SkillType.EVASION, "EV");
        ABBREV_MAP.put(SkillType.RULERS_AUTHORITY, "RA");
        ABBREV_MAP.put(SkillType.SHADOW_PARTNER, "SP");
        ABBREV_MAP.put(SkillType.FATAL_BLOW, "FB");
        // Archer
        ABBREV_MAP.put(SkillType.ARROW_RAIN, "AR");
        ABBREV_MAP.put(SkillType.SOUL_ARROW, "SA");
        ABBREV_MAP.put(SkillType.SHARP_EYES, "SE");
        ABBREV_MAP.put(SkillType.ARROW_BOMB, "AB");
        ABBREV_MAP.put(SkillType.COVERING_FIRE, "CF");
        ABBREV_MAP.put(SkillType.EVASION_BOOST, "EB");
        ABBREV_MAP.put(SkillType.PHOENIX, "PX");
        ABBREV_MAP.put(SkillType.HURRICANE, "HC");
        ABBREV_MAP.put(SkillType.MORTAL_BLOW, "MB");
        // Healer
        ABBREV_MAP.put(SkillType.HOLY_LIGHT, "HL");
        ABBREV_MAP.put(SkillType.BLESS, "BLS");
        ABBREV_MAP.put(SkillType.MP_RECOVERY, "MR");
        ABBREV_MAP.put(SkillType.HOLY_SHELL, "HS");
        ABBREV_MAP.put(SkillType.DISPEL, "DSP");
        ABBREV_MAP.put(SkillType.DIVINE_PROTECTION, "DP");
        ABBREV_MAP.put(SkillType.BENEDICTION, "BN");
        ABBREV_MAP.put(SkillType.ANGEL_RAY, "ARL");
        ABBREV_MAP.put(SkillType.BLESSED_ENSEMBLE, "BE");
        // Mage
        ABBREV_MAP.put(SkillType.FLAME_ORB, "FO");
        ABBREV_MAP.put(SkillType.MAGIC_GUARD, "MG");
        ABBREV_MAP.put(SkillType.ELEMENTAL_DRAIN, "ED");
        ABBREV_MAP.put(SkillType.FROST_BIND, "FrB");
        ABBREV_MAP.put(SkillType.POISON_MIST, "PM");
        ABBREV_MAP.put(SkillType.ELEMENT_AMPLIFICATION, "EA");
        ABBREV_MAP.put(SkillType.MIST_ERUPTION, "ME");
        ABBREV_MAP.put(SkillType.INFINITY, "INF");
        ABBREV_MAP.put(SkillType.ARCANE_OVERDRIVE, "AO");
        // Necromancer
        ABBREV_MAP.put(SkillType.LIFE_DRAIN, "LD");
        ABBREV_MAP.put(SkillType.RAISE_SKELETON, "RS");
        ABBREV_MAP.put(SkillType.DARK_PACT, "DkP");
        ABBREV_MAP.put(SkillType.BONE_SHIELD, "BnS");
        ABBREV_MAP.put(SkillType.CORPSE_EXPLOSION, "CEx");
        ABBREV_MAP.put(SkillType.SOUL_SIPHON, "Si");
        ABBREV_MAP.put(SkillType.ARMY_OF_THE_DEAD, "AD");
        ABBREV_MAP.put(SkillType.DEATH_MARK, "DkM");
        ABBREV_MAP.put(SkillType.UNDYING_WILL, "UW");
        // Legacy
        ABBREV_MAP.put(SkillType.VITAL_SURGE, "VS");
    }

    // Tier definitions
    private static final int[] TIER_NUMBERS = {0, 1, 2, 3};
    private static final String[] TAB_LABELS = {"NOVICE", "TIER 1", "TIER 2", "TIER 3"};
    private static final int[] TIER_REQ_LEVELS = {0, 10, 30, 60};

    private int activeTab = 0;
    private int panelW, panelH, panelX, panelY;
    private int pad, lineH;

    private final int[][] tabBounds = new int[4][4];
    private final int[][] skillRowBounds = new int[3][4];
    private final int[][] iconBounds = new int[3][4];
    private final int[][] plusBtnBounds = new int[3][4];
    private final int[][] equipSlotBounds = new int[SkillData.MAX_SLOTS][4];
    private final int[] closeBtnBounds = new int[4];

    // Class selection bounds
    private final int[] warriorBtnBounds = new int[4];
    private final int[] assassinBtnBounds = new int[4];
    private final int[] archerBtnBounds = new int[4];
    private final int[] healerBtnBounds = new int[4];
    private final int[] mageBtnBounds = new int[4];
    private final int[] necromancerBtnBounds = new int[4];

    private SkillType pendingEquipSkill = null;
    private SkillType hoveredSkill = null;
    private boolean showClassSelection = false;

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

        // Row 1: Warrior, Assassin, Necromancer (3-column)
        int colGap = 4;
        int colW = (innerW - colGap * 2) / 3;
        int boxH = lineH * 4 + 6;

        renderClassBox(g, warriorBtnBounds, left, rowY, colW, boxH,
                "WARRIOR", "STR", CLASS_WARRIOR, new String[]{"Bloodlust", "Iron Will", "Domain"}, mx, my);
        renderClassBox(g, assassinBtnBounds, left + colW + colGap, rowY, colW, boxH,
                "ASSASSIN", "LUK", CLASS_ASSASSIN, new String[]{"Shadow Strike", "Stealth", "Ruler's Auth."}, mx, my);
        renderClassBox(g, necromancerBtnBounds, left + (colW + colGap) * 2, rowY, colW, boxH,
                "NECRO", "INT/MND", CLASS_NECROMANCER, new String[]{"Life Drain", "Raise Skeleton", "Army of Dead"}, mx, my);

        rowY += boxH + 6;

        // Row 2: Archer, Healer, Mage (3-column)
        renderClassBox(g, archerBtnBounds, left, rowY, colW, boxH,
                "ARCHER", "DEX", CLASS_ARCHER, new String[]{"Arrow Rain", "Phoenix", "Hurricane"}, mx, my);
        renderClassBox(g, healerBtnBounds, left + colW + colGap, rowY, colW, boxH,
                "HEALER", "INT", CLASS_HEALER, new String[]{"Holy Light", "Benediction", "Angel Ray"}, mx, my);
        renderClassBox(g, mageBtnBounds, left + (colW + colGap) * 2, rowY, colW, boxH,
                "MAGE", "INT", CLASS_MAGE, new String[]{"Flame Orb", "Frost Bind", "Infinity"}, mx, my);
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

        for (int i = 0; i < skills.size() && i < 3; i++) {
            SkillType skill = skills.get(i);
            int level = sd.getLevel(skill);
            boolean unlocked = level > 0;
            boolean canUnlock = sd.canUnlock(skill, playerLevel);
            boolean maxed = level >= skill.getMaxLevel();

            skillRowBounds[i] = new int[]{left, rowY, innerW, ICON_SIZE};
            int ix = left;
            int iy = rowY;
            iconBounds[i] = new int[]{ix, iy, ICON_SIZE, ICON_SIZE};

            boolean rowHov = isInside(mouseX, mouseY, skillRowBounds[i]);
            if (rowHov) hoveredSkill = skill;

            // Icon background + border
            g.fill(ix, iy, ix + ICON_SIZE, iy + ICON_SIZE, rowHov ? ICON_BG_HOVER : ICON_BG);
            int borderColor = maxed ? SKILL_MAX : (unlocked ? SKILL_UNLOCKED : (canUnlock ? FRAME : SKILL_LOCKED));
            drawBox(g, ix, iy, ICON_SIZE, ICON_SIZE, borderColor);

            if (pendingEquipSkill == skill) {
                drawBox(g, ix + 1, iy + 1, ICON_SIZE - 2, ICON_SIZE - 2, TEXT_ACCENT);
            }

            // Abbreviation
            String abbr = ABBREV_MAP.getOrDefault(skill, "?");
            int abbrColor = unlocked ? TEXT_BRIGHT : (canUnlock ? TEXT_VALUE : TEXT_DIM);
            g.drawString(font, abbr, ix + (ICON_SIZE - font.width(abbr)) / 2,
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
                        ? "MP: " + skill.getToggleMpPerSecond(displayLevel) + "/sec"
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

            rowY += ICON_SIZE + rowGap;
        }

        // Clear unused bounds
        for (int i = skills.size(); i < 3; i++) {
            skillRowBounds[i] = new int[]{0, 0, 0, 0};
            iconBounds[i] = new int[]{0, 0, 0, 0};
            plusBtnBounds[i] = new int[]{0, 0, 0, 0};
        }

        // Show "no skills" message if empty
        if (skills.isEmpty() && !activeTierLocked) {
            String msg = sd.getSelectedClass() == PlayerClass.NONE
                    ? "Choose a class at Lv 10" : "No skills in this tier";
            g.drawString(font, msg, cx - font.width(msg) / 2, rowY, TEXT_DIM, false);
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
                mpLine = "MP: " + skill.getToggleMpPerSecond(displayLevel) + "/sec";
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
            addDetailLines(texts, lines, skill, displayLevel, stats);
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

    private void addDetailLines(java.util.List<String> texts, java.util.List<int[]> lines,
                                 SkillType skill, int level, PlayerStats stats) {
        switch (skill) {
            // === T0 ===
            case DASH -> {
                int speedAmp = Math.min((level - 1) / 2, 1);
                double force = 0.8 + level * 0.15 + stats.getAgility() * 0.01;
                texts.add("Speed: " + toRoman(speedAmp + 1) + " (3s)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Impulse: " + String.format("%.2f", force) + " (AGI scales)");
                lines.add(new int[]{TEXT_VALUE});
            }
            case ENDURANCE -> {
                texts.add("Max HP: +" + (level * 2) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("HP regen: " + String.format("%.1f", level * 0.5) + "% max HP every 3s");
                lines.add(new int[]{TEXT_VALUE});
            }

            // === Warrior T1 ===
            case BLOODLUST -> {
                double range = 4 + level * 0.8 + stats.getStrength() * 0.3;
                int dur = (int) (3 + level * 0.5);
                texts.add("Range: " + String.format("%.1f", range) + " blocks (STR scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Duration: " + dur + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Applies: Slowness II, Weakness I");
                lines.add(new int[]{TEXT_DIM});
            }
            case WAR_CRY -> {
                int strAmp = Math.min((level - 1) / 3, 2);
                int dur = 10 + level;
                texts.add("Strength: " + toRoman(strAmp + 1) + " for " + dur + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Range: 6 blocks (self + allies)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Applies Weakness I to enemies in range");
                lines.add(new int[]{TEXT_DIM});
            }
            case WEAPON_MASTERY -> {
                texts.add("Melee damage: +" + level + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Knockback resist: +" + (level * 5) + "%");
                lines.add(new int[]{TEXT_VALUE});
            }

            // === Assassin T1 ===
            case SHADOW_STRIKE -> {
                float dmg = 1 + level * 0.8f + stats.getLuck() * 0.15f;
                texts.add("Bonus dmg: +" + String.format("%.1f", dmg) + " (LUK scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Window: 5s to land a hit");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Consumes buff on hit");
                lines.add(new int[]{TEXT_DIM});
            }
            case VENOM -> {
                int venomDur = 10 + level;
                int poisonDur = 3 + level / 3;
                int poisonAmp = Math.min(level / 4, 2);
                float directDmg = 0.5f + level * 0.15f;
                texts.add("Active: " + venomDur + "s (all melee attacks poison)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Poison " + toRoman(poisonAmp + 1) + ": " + poisonDur + "s per hit");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Magic dmg: " + String.format("%.1f", directDmg) + " per hit");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Wither on undead (immune to Poison)");
                lines.add(new int[]{TEXT_DIM});
            }
            case CRITICAL_EDGE -> {
                texts.add("Crit rate: +" + level + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Crit damage: +" + (level * 2) + "%");
                lines.add(new int[]{TEXT_VALUE});
            }

            // === Archer T1 ===
            case ARROW_RAIN -> {
                float dmg = 0.3f + level * 0.1f + stats.getDexterity() * 0.02f;
                double dur = 20 + (level - 1) * (40.0 / 9.0);
                texts.add("Dmg/arrow: " + String.format("%.1f", dmg) + " (DEX scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Duration: " + String.format("%.1f", dur / 20.0) + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Radius: 3 blocks, 2-3 arrows/tick");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Scout arrow marks target for rain");
                lines.add(new int[]{TEXT_DIM});
            }
            case SOUL_ARROW -> {
                texts.add("MP drain: " + skill.getToggleMpPerSecond(level) + "/sec");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Infinite arrows (no ammo consumed)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Projectile damage: +" + (5 + level) + "%");
                lines.add(new int[]{TEXT_VALUE});
            }
            case SHARP_EYES -> {
                texts.add("Crit rate: +" + String.format("%.1f", level * 1.5) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Crit damage: +" + (level * 5) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Projectile range: +" + (level / 2));
                lines.add(new int[]{TEXT_VALUE});
            }

            // === Healer T1 ===
            case HOLY_LIGHT -> {
                float heal = 2 + level * 0.8f + stats.getIntelligence() * 0.15f;
                float undeadDmg = 1 + level * 0.5f + stats.getIntelligence() * 0.1f;
                texts.add("Heal: " + String.format("%.1f", heal) + " HP (INT scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Undead dmg: " + String.format("%.1f", undeadDmg) + " (INT scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Range: 6 blocks (self + allies)");
                lines.add(new int[]{TEXT_DIM});
            }
            case BLESS -> {
                int strAmp = Math.min(level / 4, 1);
                int dur = 30 + level * 3;
                texts.add("Duration: " + dur + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Buffs: Strength " + toRoman(strAmp + 1) + ", Resistance I, Regen I");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Range: 8 blocks (self + allies)");
                lines.add(new int[]{TEXT_DIM});
            }
            case MP_RECOVERY -> {
                int mpOnKill = 2 + level;
                texts.add("MP regen: +" + String.format("%.1f", level * 0.2) + "%/sec");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP on kill: +" + mpOnKill);
                lines.add(new int[]{TEXT_VALUE});
            }

            // === Mage T1 ===
            case FLAME_ORB -> {
                float dmg = 2 + level * 0.6f + stats.getIntelligence() * 0.12f;
                double radius = 3 + level * 0.1;
                int fireDur = 3 + level / 3;
                texts.add("Damage: " + String.format("%.1f", dmg) + " (INT scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("AoE radius: " + String.format("%.1f", radius) + " blocks");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Sets fire: " + fireDur + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Projectile, explodes on impact");
                lines.add(new int[]{TEXT_DIM});
            }
            case MAGIC_GUARD -> {
                float redirect = 0.3f + level * 0.03f;
                texts.add("Dmg to MP: " + String.format("%.0f", redirect * 100) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP drain: " + skill.getToggleMpPerSecond(level) + "/sec");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Remaining damage still hurts HP");
                lines.add(new int[]{TEXT_DIM});
            }
            case ELEMENTAL_DRAIN -> {
                texts.add("Bonus per debuff: +" + (level * 5) + "% dmg");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Max bonus: +25%");
                lines.add(new int[]{TEXT_VALUE});
            }

            // === Warrior T2 ===
            case IRON_WILL -> {
                int amp = Math.min((level - 1) / 5, 2);
                texts.add("Resistance: " + toRoman(amp + 1));
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP drain: " + skill.getToggleMpPerSecond(level) + "/sec");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Damage reduction + knockback resist");
                lines.add(new int[]{TEXT_DIM});
            }
            case GROUND_SLAM -> {
                double range = 3 + level * 0.15 + stats.getStrength() * 0.05;
                float dmg = 2 + level * 0.5f + stats.getStrength() * 0.1f;
                int stunDur = 1 + level / 5;
                texts.add("Damage: " + String.format("%.1f", dmg) + " (STR scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Range: " + String.format("%.1f", range) + " blocks (STR scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Stun: " + stunDur + "s (Slowness IV)");
                lines.add(new int[]{TEXT_VALUE});
            }
            case RAGE -> {
                texts.add("Below 50% HP: +" + (level * 2) + "% damage");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Lifesteal: " + String.format("%.1f", level * 0.5) + "%");
                lines.add(new int[]{TEXT_VALUE});
            }

            // === Assassin T2 ===
            case STEALTH -> {
                double detect = Math.max(5.0 - level * 0.27, 1.0);
                texts.add("Detection range: " + String.format("%.1f", detect) + " blocks");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP drain: " + skill.getToggleMpPerSecond(level) + "/sec");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Grants: Invisibility, clears mob aggro");
                lines.add(new int[]{TEXT_DIM});
                texts.add("Breaks on attack or taking damage");
                lines.add(new int[]{TEXT_DIM});
            }
            case BLADE_FURY -> {
                double range = 3 + level * 0.1 + stats.getLuck() * 0.03;
                float dmg = 2 + level * 0.6f + stats.getLuck() * 0.12f;
                texts.add("Damage: " + String.format("%.1f", dmg) + " (LUK scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Range: " + String.format("%.1f", range) + " blocks (LUK scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Hits all enemies in radius");
                lines.add(new int[]{TEXT_DIM});
            }
            case EVASION -> {
                texts.add("Dodge chance: " + (level * 2) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Dodge guarantees next crit");
                lines.add(new int[]{TEXT_DIM});
            }

            // === Archer T2 ===
            case ARROW_BOMB -> {
                float dmg = 2 + level * 0.6f + stats.getDexterity() * 0.1f;
                double radius = 3 + level * 0.1;
                int stunDur = 2 + level / 5;
                texts.add("Damage: " + String.format("%.1f", dmg) + " (DEX scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("AoE radius: " + String.format("%.1f", radius) + " blocks");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Stun: " + stunDur + "s (Slowness IV)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Explosive arrow projectile");
                lines.add(new int[]{TEXT_DIM});
            }
            case COVERING_FIRE -> {
                float dmg = 1 + level * 0.4f + stats.getDexterity() * 0.08f;
                int arrowCount = 3 + level / 4;
                texts.add("Dmg/arrow: " + String.format("%.1f", dmg) + " (DEX scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Arrows: " + arrowCount);
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Leaps backward while firing forward");
                lines.add(new int[]{TEXT_DIM});
            }
            case EVASION_BOOST -> {
                texts.add("Dodge chance: " + (level * 2) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Dodge guarantees next arrow crits");
                lines.add(new int[]{TEXT_DIM});
            }

            // === Healer T2 ===
            case HOLY_SHELL -> {
                float absorb = 2 + level * 0.4f + stats.getIntelligence() * 0.05f;
                texts.add("Absorption: " + String.format("%.1f", absorb) + " HP (INT scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Range: 6 blocks (self + allies)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Cleanses: Poison, Wither");
                lines.add(new int[]{TEXT_DIM});
            }
            case DISPEL -> {
                texts.add("Self: Remove Poison, Wither, Weakness,");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("  Slowness, Mining Fatigue, Blindness");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Allies: Remove Poison, Wither, Weakness,");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("  Slowness (8 block range)");
                lines.add(new int[]{TEXT_VALUE});
            }
            case DIVINE_PROTECTION -> {
                texts.add("Status resist: +" + (level * 3) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Auto-cleanse chance on debuff");
                lines.add(new int[]{TEXT_DIM});
            }

            // === Mage T2 ===
            case FROST_BIND -> {
                double range = 5 + level * 0.15 + stats.getIntelligence() * 0.05;
                float dmg = 1 + level * 0.3f + stats.getIntelligence() * 0.08f;
                int freezeDur = 3 + level / 3;
                texts.add("Damage: " + String.format("%.1f", dmg) + " (INT scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Range: " + String.format("%.1f", range) + " blocks (INT scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Freeze: " + freezeDur + "s (Slowness IV + Weakness I)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Pulls enemies inward");
                lines.add(new int[]{TEXT_DIM});
            }
            case POISON_MIST -> {
                int mistDur = 8 + level / 3;
                float tickDmg = 0.5f + level * 0.1f + stats.getIntelligence() * 0.03f;
                double radius = 4 + level * 0.1;
                texts.add("Duration: " + mistDur + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("DPS: " + String.format("%.1f", tickDmg) + "/sec (INT scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Radius: " + String.format("%.1f", radius) + " blocks");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Applies: Poison I, stationary zone");
                lines.add(new int[]{TEXT_DIM});
            }
            case ELEMENT_AMPLIFICATION -> {
                texts.add("Skill damage: +" + (level * 3) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP cost: +20% for all skills");
                lines.add(new int[]{TEXT_DIM});
            }

            // === Warrior T3 ===
            case DOMAIN_OF_MONARCH -> {
                int dur = 4 + level / 2;
                double radius = 2 + level * 0.3 + stats.getStrength() * 0.1;
                float dps = 0.5f + level * 0.15f + stats.getStrength() * 0.05f;
                texts.add("Duration: " + dur + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Radius: " + String.format("%.1f", radius) + " blocks (STR scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("DPS: " + String.format("%.1f", dps) + "/sec (STR scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Applies: Slowness I, Glowing on enemies");
                lines.add(new int[]{TEXT_DIM});
                texts.add("Grants: Strength I to self");
                lines.add(new int[]{TEXT_DIM});
            }
            case UNBREAKABLE -> {
                int dur = 3 + level / 4;
                float healPct = 20 + level;
                texts.add("Invulnerable: " + dur + "s (Resistance V)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Burst heal: " + String.format("%.0f", healPct) + "% max HP");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Cleanses: Poison, Wither, Weakness, Slow");
                lines.add(new int[]{TEXT_DIM});
            }
            case BERSERKER_SPIRIT -> {
                texts.add("Crit rate: +" + level + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Double strike: " + level + "% chance");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Lifesteal: " + String.format("%.1f", level * 0.3) + "%");
                lines.add(new int[]{TEXT_VALUE});
            }

            // === Assassin T3 ===
            case RULERS_AUTHORITY -> {
                double range = 4 + level * 0.4 + stats.getLuck() * 0.15;
                float dmg = level * 0.4f + stats.getLuck() * 0.08f;
                double pullForce = 0.3 + level * 0.1;
                texts.add("Damage: " + String.format("%.1f", dmg) + " (LUK scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Range: " + String.format("%.1f", range) + " blocks (LUK scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Pull force: " + String.format("%.1f", pullForce));
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Cone: ~60 degrees in front");
                lines.add(new int[]{TEXT_DIM});
            }
            case SHADOW_PARTNER -> {
                float mult = 0.3f + (level - 1) * (0.1f / 19.0f);
                texts.add("Mirror damage: " + String.format("%.0f", mult * 100) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP drain: " + skill.getToggleMpPerSecond(level) + "/sec");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Max MP halved while active");
                lines.add(new int[]{TEXT_DIM});
                texts.add("Mirrors all skills + melee + ranged");
                lines.add(new int[]{TEXT_DIM});
            }
            case FATAL_BLOW -> {
                texts.add("Below 30% HP: +" + (level * 2) + "% damage");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Execute chance: " + String.format("%.1f", level * 0.5) + "%");
                lines.add(new int[]{TEXT_VALUE});
            }

            // === Archer T3 ===
            case PHOENIX -> {
                float dmg = 1 + level * 0.3f + stats.getDexterity() * 0.06f;
                double range = 6 + level * 0.2 + stats.getDexterity() * 0.05;
                int dur = (int) Math.min(30 + level * 1.5, 60);
                texts.add("Duration: " + dur + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("DPS: " + String.format("%.1f", dmg) + "/sec (DEX scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Range: " + String.format("%.1f", range) + " blocks (DEX scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Grants: Resistance I, sets target on fire");
                lines.add(new int[]{TEXT_DIM});
            }
            case HURRICANE -> {
                float dmg = 1.5f + level * 0.4f + stats.getDexterity() * 0.1f;
                double range = 8 + stats.getDexterity() * 0.1;
                texts.add("Dmg/arrow: " + String.format("%.1f", dmg) + " (DEX scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Range: " + String.format("%.1f", range) + " blocks (DEX scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP drain: " + skill.getToggleMpPerSecond(level) + "/sec");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Auto-fires at nearest enemy, slows self");
                lines.add(new int[]{TEXT_DIM});
            }
            case MORTAL_BLOW -> {
                texts.add("Below 30% HP: +" + (level * 2) + "% projectile dmg");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Execute chance: " + String.format("%.1f", level * 0.5) + "%");
                lines.add(new int[]{TEXT_VALUE});
            }

            // === Healer T3 ===
            case BENEDICTION -> {
                int dur = 15 + level;
                double radius = 4 + level * 0.2 + stats.getIntelligence() * 0.05;
                float tickDmg = 0.5f + level * 0.1f + stats.getIntelligence() * 0.03f;
                texts.add("Duration: " + dur + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Radius: " + String.format("%.1f", radius) + " blocks (INT scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Allies: Regen II + Strength I each sec");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Enemies: " + String.format("%.1f", tickDmg) + " DPS + Slowness I");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Stationary zone");
                lines.add(new int[]{TEXT_DIM});
            }
            case ANGEL_RAY -> {
                float dmg = 1.5f + level * 0.4f + stats.getIntelligence() * 0.1f;
                float heal = dmg * 0.3f;
                texts.add("Damage: " + String.format("%.1f", dmg) + " (INT scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("AoE radius: 3 blocks on impact");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Heals allies: " + String.format("%.1f", heal) + " HP");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Holy projectile, damages + heals");
                lines.add(new int[]{TEXT_DIM});
            }
            case BLESSED_ENSEMBLE -> {
                texts.add("Damage: +" + (level * 3) + "% per nearby player");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("XP bonus: +" + (level * 5) + "% per nearby player");
                lines.add(new int[]{TEXT_VALUE});
            }

            // === Mage T3 ===
            case MIST_ERUPTION -> {
                float detDmg = 4 + level * 0.8f + stats.getIntelligence() * 0.2f;
                double detRadius = 5 + level * 0.15;
                float blastDmg = 2 + level * 0.4f + stats.getIntelligence() * 0.1f;
                texts.add("With Mist: " + String.format("%.1f", detDmg) + " dmg (INT scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("  Radius: " + String.format("%.1f", detRadius) + " blocks + fire");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("No Mist: " + String.format("%.1f", blastDmg) + " dmg, 4 block range");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Detonates Poison Mist for bonus damage");
                lines.add(new int[]{TEXT_DIM});
            }
            case INFINITY -> {
                int dur = 20 + level;
                texts.add("Duration: " + dur + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("All skills cost 0 MP while active");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Every 4s: Strength I + 3% max HP heal");
                lines.add(new int[]{TEXT_VALUE});
            }
            case ARCANE_OVERDRIVE -> {
                texts.add("Crit rate: +" + String.format("%.1f", level * 1.5) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Crit damage: +" + level + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Armor pen: +" + level + "%");
                lines.add(new int[]{TEXT_VALUE});
            }

            // === Necromancer T1 ===
            case LIFE_DRAIN -> {
                float ldRange = 5 + level * 0.3f;
                float ldDmg = 3 + level * 0.8f + stats.getIntelligence() * 0.15f;
                float ldHeal = 30 + level * 2;
                texts.add("Damage: " + String.format("%.1f", ldDmg) + " (INT scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Range: " + String.format("%.1f", ldRange) + " blocks");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Heal: " + String.format("%.0f", ldHeal) + "% of damage (Mind scales)");
                lines.add(new int[]{TEXT_VALUE});
            }
            case RAISE_SKELETON -> {
                float mDmg = 3 + stats.getMind() * 0.15f;
                float mHp = 20 + stats.getMind() * 0.5f;
                String weapon = level >= 9 ? "Diamond" : level >= 7 ? "Iron" : level >= 5 ? "Iron" : level >= 3 ? "Stone" : "Wooden";
                texts.add("Minion HP: " + String.format("%.0f", mHp) + " (Mind scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Minion DMG: " + String.format("%.1f", mDmg) + " (Mind scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Weapon: " + weapon + " Sword");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP drain: " + SkillType.RAISE_SKELETON.getToggleMpPerSecond(level) + "/s");
                lines.add(new int[]{TEXT_VALUE});
            }
            case DARK_PACT -> {
                texts.add("Max MP: +" + (level * 3) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Summon damage: +" + (level * 2) + "%");
                lines.add(new int[]{TEXT_VALUE});
            }

            // === Necromancer T2 ===
            case BONE_SHIELD -> {
                int reduction = Math.min(10 + level, 25);
                texts.add("Damage reduction: " + reduction + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP drain: " + SkillType.BONE_SHIELD.getToggleMpPerSecond(level) + "/s");
                lines.add(new int[]{TEXT_VALUE});
            }
            case CORPSE_EXPLOSION -> {
                float ceDmg = 8 + level * 1.5f + stats.getIntelligence() * 0.3f;
                texts.add("Dmg/skeleton: " + String.format("%.1f", ceDmg) + " (INT scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Blast radius: 4 blocks per skeleton");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Detonates ALL skeletons (Raise + Army)");
                lines.add(new int[]{TEXT_DIM});
                texts.add("Skeletons rush to target, then explode");
                lines.add(new int[]{TEXT_DIM});
            }
            case SOUL_SIPHON -> {
                texts.add("HP restore: " + String.format("%.1f", 1 + level * 0.2f) + "% on kill");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP restore: " + String.format("%.1f", 2 + level * 0.3f) + "% on kill");
                lines.add(new int[]{TEXT_VALUE});
            }

            // === Necromancer T3 ===
            case ARMY_OF_THE_DEAD -> {
                float mDmg = (3 + stats.getMind() * 0.15f) * 0.7f;
                float mHp = (20 + stats.getMind() * 0.5f) * 0.7f;
                float adDur = 10 + level * 0.5f;
                texts.add("Summons: 5 skeleton minions");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Minion HP: " + String.format("%.0f", mHp) + " (70% of Raise)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Minion DMG: " + String.format("%.1f", mDmg) + " (70% of Raise)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Duration: " + String.format("%.1f", adDur) + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Corpse Explosion detonates these too");
                lines.add(new int[]{TEXT_DIM});
            }
            case DEATH_MARK -> {
                float dmDot = 2 + level * 0.5f + stats.getIntelligence() * 0.1f;
                float dmDur = 10 + level * 0.5f;
                texts.add("DoT: " + String.format("%.1f", dmDot) + "/s (INT scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Duration: " + String.format("%.1f", dmDur) + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("On target death: AoE + restore " + String.format("%.1f", 5 + level * 0.5f) + "% HP/MP");
                lines.add(new int[]{TEXT_DIM});
            }
            case UNDYING_WILL -> {
                int revPct = 10 + level;
                int cdSec = Math.max(3600 - level * 90, 1800) / 20;
                texts.add("Revive at: " + revPct + "% HP on fatal damage");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Cooldown: " + cdSec + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Minion max HP: +" + (level * 3) + "%");
                lines.add(new int[]{TEXT_VALUE});
            }

            // === Legacy ===
            case VITAL_SURGE -> {
                float heal = 2 + level * 0.9f + stats.getVitality() * 0.1f;
                int regenDur = 2 + level / 3;
                texts.add("Heal: " + String.format("%.1f", heal) + " HP (VIT scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Regen I: " + regenDur + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Cleanses: Poison, Wither");
                lines.add(new int[]{TEXT_DIM});
            }
            default -> {}
        }
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(n);
        };
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
                int[][] classBounds = {warriorBtnBounds, assassinBtnBounds, necromancerBtnBounds, archerBtnBounds, healerBtnBounds, mageBtnBounds};
                PlayerClass[] classes = {PlayerClass.WARRIOR, PlayerClass.ASSASSIN, PlayerClass.NECROMANCER, PlayerClass.ARCHER, PlayerClass.HEALER, PlayerClass.MAGE};
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
                for (int i = 0; i < skills.size() && i < 3; i++) {
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
