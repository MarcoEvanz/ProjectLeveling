package com.monody.projectleveling.client;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.capability.PlayerStatsCapability;
import com.monody.projectleveling.network.C2SAllocateStatPacket;
import com.monody.projectleveling.network.ModNetwork;
import com.monody.projectleveling.skill.SkillData;
import com.monody.projectleveling.skill.SkillExecutor;
import com.monody.projectleveling.skill.SkillType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

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

    private static final int NUM_STATS = 9;
    private final int[][] statBtnBounds = new int[NUM_STATS][4];
    private final int[] closeBtnBounds = new int[4];
    private boolean hasPoints;

    // Scrolling
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private int scrollAreaTop;
    private int scrollAreaBottom;

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
        scrollOffset = 0;
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

        // Frame
        drawBox(g, x, y, panelW, panelH, FRAME);
        drawBox(g, x + 2, y + 2, panelW - 4, panelH - 4, FRAME_DIM);

        // Corner accents
        int cs = 10;
        drawCorner(g, x, y, cs, 1, 1);
        drawCorner(g, x + panelW, y, cs, -1, 1);
        drawCorner(g, x, y + panelH, cs, 1, -1);
        drawCorner(g, x + panelW, y + panelH, cs, -1, -1);

        // Close button (fixed)
        int closeX = right - 6;
        int closeY = y + 6;
        closeBtnBounds[0] = closeX - 2;
        closeBtnBounds[1] = closeY - 2;
        closeBtnBounds[2] = 11;
        closeBtnBounds[3] = 11;
        boolean closeHov = isInside(mouseX, mouseY, closeBtnBounds);
        g.drawString(font, "x", closeX, closeY, closeHov ? CLOSE_HOVER : CLOSE_NORMAL, false);

        // Header (fixed)
        int headerY = y + pad + 2;
        int hw = font.width("STATUS");
        g.drawString(font, "STATUS", cx - hw / 2, headerY, TEXT_BRIGHT, false);
        int sepY = headerY + lineH + 2;
        drawSep(g, left, sepY, innerW);

        // Scrollable area bounds
        scrollAreaTop = sepY + 1;
        scrollAreaBottom = y + panelH - pad;

        // Enable scissor for scrollable content
        g.enableScissor(x + 3, scrollAreaTop, x + panelW - 3, scrollAreaBottom);

        int rowY = scrollAreaTop + 6 - scrollOffset;

        // Info section
        drawLabel(g, "NAME:", left, rowY);
        g.drawString(font, player.getName().getString(), left + font.width("NAME: "), rowY, TEXT_VALUE, false);
        drawRight(g, "LV: " + stats.getLevel(), right, rowY, TEXT_VALUE);

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

        // Stats (4 rows x 2 columns)
        rowY += 8;
        int btnS = 11;
        int colEnd1 = colMid - 6;
        int colEnd2 = right;

        drawStatRow(g, 0, "STR", stats.getStrength(), left, rowY, btnS, colEnd1, mouseX, mouseY);
        drawStatRow(g, 1, "VIT", stats.getVitality(), colMid, rowY, btnS, colEnd2, mouseX, mouseY);

        rowY += lineH + 2;
        drawStatRow(g, 2, "AGI", stats.getAgility(), left, rowY, btnS, colEnd1, mouseX, mouseY);
        drawStatRow(g, 3, "INT", stats.getIntelligence(), colMid, rowY, btnS, colEnd2, mouseX, mouseY);

        rowY += lineH + 2;
        drawStatRow(g, 4, "SIG", stats.getSight(), left, rowY, btnS, colEnd1, mouseX, mouseY);
        drawStatRow(g, 5, "LUK", stats.getLuck(), colMid, rowY, btnS, colEnd2, mouseX, mouseY);

        rowY += lineH + 2;
        drawStatRow(g, 6, "DEX", stats.getDexterity(), left, rowY, btnS, colEnd1, mouseX, mouseY);
        drawStatRow(g, 7, "MND", stats.getMind(), colMid, rowY, btnS, colEnd2, mouseX, mouseY);

        rowY += lineH + 2;
        drawStatRow(g, 8, "FAI", stats.getFaith(), left, rowY, btnS, colEnd1, mouseX, mouseY);

        rowY += lineH + 4;
        drawSep(g, left, rowY, innerW);

        // Summary with passive skill bonuses
        rowY += 7;
        rowY = renderSummary(g, stats, left, right, innerW, rowY);

        // Remaining points
        rowY += lineH + 3;
        drawSep(g, left, rowY, innerW);
        rowY += 7;
        String rpText = "REMAINING POINTS: " + stats.getRemainingPoints();
        int rpColor = stats.getRemainingPoints() > 0 ? TEXT_ACCENT : TEXT_DIM;
        g.drawString(font, rpText, cx - font.width(rpText) / 2, rowY, rpColor, false);

        // Calculate content height and max scroll
        int contentBottom = rowY + lineH;
        int contentHeight = (contentBottom + scrollOffset) - (scrollAreaTop + 6);
        int visibleHeight = scrollAreaBottom - scrollAreaTop;
        maxScroll = Math.max(0, contentHeight - visibleHeight);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        g.disableScissor();

        // Scroll indicator
        if (maxScroll > 0) {
            int trackH = scrollAreaBottom - scrollAreaTop - 4;
            int thumbH = Math.max(10, trackH * visibleHeight / contentHeight);
            int thumbY = scrollAreaTop + 2 + (int) ((trackH - thumbH) * ((float) scrollOffset / maxScroll));
            int scrollBarX = x + panelW - 5;
            g.fill(scrollBarX, scrollAreaTop + 2, scrollBarX + 2, scrollAreaTop + 2 + trackH, 0x20FFFFFF);
            g.fill(scrollBarX, thumbY, scrollBarX + 2, thumbY + thumbH, 0x60FFFFFF);
        }
    }

    // === Summary + Buffs rendering ===

    private int renderSummary(GuiGraphics g, PlayerStats stats, int left, int right, int innerW, int rowY) {
        SkillData sd = stats.getSkillData();
        Player player = Minecraft.getInstance().player;

        int kmLv = sd.getLevel(SkillType.KUNAI_MASTERY);
        int ceLv = sd.getLevel(SkillType.CRITICAL_EDGE);
        int seLv = sd.getLevel(SkillType.SHARP_EYES);
        int aoLv = sd.getLevel(SkillType.ARCANE_OVERDRIVE);
        int bsLv = sd.getLevel(SkillType.BERSERKER_SPIRIT);
        int ccLv = sd.getLevel(SkillType.CHAKRA_CONTROL);
        int lukStat = stats.getLuck();

        // --- Damage ---
        // ATK
        double dmg = (stats.getStrength() - 1) * 0.1 + (lukStat - 1) * 0.05 + (stats.getDexterity() - 1) * 0.05;
        float weaponDmg = SkillExecutor.getWeaponDamage(player);
        if (kmLv > 0) dmg += stats.getAgility() * 0.08f * kmLv / 10.0f;
        StringBuilder atkTag = new StringBuilder();
        if (weaponDmg > 0) atkTag.append("WPN+").append(String.format("%.1f", weaponDmg));
        if (kmLv > 0) {
            if (atkTag.length() > 0) atkTag.append(" ");
            atkTag.append("KM+").append(String.format("%.1f", stats.getAgility() * 0.08f * kmLv / 10.0f));
        }
        String atkSkill = atkTag.length() > 0 ? " (" + atkTag + ")" : "";
        drawStat(g, "ATK:", String.format("+%.2f", dmg + weaponDmg) + atkSkill, left, rowY);

        // MATK
        rowY += lineH;
        float matk = stats.getIntelligence() * 0.1f;
        drawStat(g, "MATK:", String.format("+%.2f", matk), left, rowY);

        // HEAL
        rowY += lineH;
        float healPow = stats.getFaith() * 0.1f;
        drawStat(g, "HEAL:", String.format("+%.2f", healPow), left, rowY);

        // PROJ
        rowY += lineH;
        double projPct = (stats.getDexterity() - 1) * 0.1;
        int saLv = sd.getLevel(SkillType.SOUL_ARROW);
        if (saLv > 0 && sd.isToggleActive(SkillType.SOUL_ARROW)) projPct += 5 + saLv;
        if (kmLv > 0) projPct += kmLv * 1.5;
        drawStat(g, "PROJ:", String.format("+%.1f%%", projPct), left, rowY);

        // --- Critical (Sight-based) ---
        rowY += lineH;
        int sightStat = stats.getSight();
        double critRate = (sightStat - 1) * 0.1;
        if (ceLv > 0) critRate += ceLv;
        if (seLv > 0) critRate += seLv * 1.5;
        if (aoLv > 0) critRate += aoLv * 1.5;
        if (bsLv > 0) critRate += bsLv;
        if (kmLv > 0) critRate += sightStat * 0.05f * kmLv / 10.0f;
        drawStat(g, "CRIT:", String.format("%.1f%%", critRate), left, rowY);

        rowY += lineH;
        double critDmg = 150 + (sightStat - 1) * 0.2;
        if (ceLv > 0) critDmg += ceLv * 2;
        if (seLv > 0) critDmg += seLv * 5;
        if (aoLv > 0) critDmg += aoLv;
        drawStat(g, "CDMG:", String.format("%.0f%%", critDmg), left, rowY);

        // --- Combat ---
        rowY += lineH;
        double atkSpd = (stats.getAgility() - 1) * 0.01;
        drawStat(g, "ATK SPD:", String.format("+%.2f", atkSpd), left, rowY);

        rowY += lineH;
        int armor = player != null ? player.getArmorValue() : 0;
        drawStat(g, "DEF:", String.valueOf(armor), left, rowY);

        rowY += lineH;
        double meleePct = 0;
        drawStat(g, "MELEE:", String.format("+%.1f%%", meleePct), left, rowY);

        rowY += lineH;
        double moveSpd = player != null ? player.getAttributeValue(Attributes.MOVEMENT_SPEED) : 0;
        drawStat(g, "SPD:", String.format("%.3f", moveSpd), left, rowY);

        // --- Damage Bonus (multipliers) ---
        rowY += lineH;
        double dmgBonus = 0;
        StringBuilder dmgTag = new StringBuilder();
        int sageLv = sd.getLevel(SkillType.SAGE_MODE);
        if (sageLv > 0 && sd.isToggleActive(SkillType.SAGE_MODE)) {
            double sageBonus = 20 + sageLv;
            dmgBonus += sageBonus;
            dmgTag.append("Sage+").append(String.format("%.0f", sageBonus)).append("%");
        }
        int eaLv = sd.getLevel(SkillType.ELEMENT_AMPLIFICATION);
        if (eaLv > 0) {
            if (dmgTag.length() > 0) dmgTag.append(" ");
            double eaBonus = eaLv * 3.0;
            dmgBonus += eaBonus;
            dmgTag.append("EA+").append(String.format("%.0f", eaBonus)).append("%");
        }
        int ufLv = sd.getLevel(SkillType.UNHOLY_FERVOR);
        if (ufLv > 0) {
            if (dmgTag.length() > 0) dmgTag.append(" ");
            double ufBonus = 15.0 + ufLv * 1.5;
            dmgBonus += ufBonus;
            dmgTag.append("UF+").append(String.format("%.1f", ufBonus)).append("%");
        }
        int fbLv = sd.getLevel(SkillType.FATAL_BLOW);
        if (fbLv > 0) {
            if (dmgTag.length() > 0) dmgTag.append(" ");
            dmgTag.append("FB+").append(fbLv * 2).append("%");
        }
        int mbLv = sd.getLevel(SkillType.MORTAL_BLOW);
        if (mbLv > 0) {
            if (dmgTag.length() > 0) dmgTag.append(" ");
            dmgTag.append("MB+").append(mbLv * 2).append("%");
        }
        String dmgExtra = dmgTag.length() > 0 ? " (" + dmgTag + ")" : "";
        drawStat(g, "DMG:", String.format("+%.1f%%", dmgBonus) + dmgExtra, left, rowY);

        // --- MP ---
        rowY += lineH;
        int dpkLv = sd.getLevel(SkillType.DARK_PACT);
        StringBuilder mpTag = new StringBuilder();
        if (dpkLv > 0) mpTag.append("+").append(dpkLv * 2).append("%");
        if (ccLv > 0) {
            if (mpTag.length() > 0) mpTag.append(" ");
            mpTag.append("-").append(ccLv).append("% cost");
        }
        String mpExtra = mpTag.length() > 0 ? " (" + mpTag + ")" : "";
        drawStat(g, "MAX MP:", stats.getMaxMp() + mpExtra, left, rowY);

        rowY += lineH;
        double mpRegenPct = 1.0 + (stats.getMind() - 1) * 0.01;
        int mpRecovLv = sd.getLevel(SkillType.MP_RECOVERY);
        StringBuilder regenTag = new StringBuilder();
        if (mpRecovLv > 0) regenTag.append("+").append(String.format("%.1f", mpRecovLv * 0.2)).append("%");
        if (ccLv > 0) {
            if (regenTag.length() > 0) regenTag.append(" ");
            regenTag.append("CC+").append(String.format("%.1f", ccLv * 0.15f)).append("%");
        }
        String mpSkill = regenTag.length() > 0 ? " (" + regenTag + ")" : "";
        drawStat(g, "MP REGEN:", String.format("%.1f%%/s", mpRegenPct) + mpSkill, left, rowY);

        // Active buffs section
        rowY += lineH + 3;
        drawSep(g, left, rowY, innerW);
        rowY += 5;
        List<String> buffs = collectActiveBuffs(sd, player);
        if (!buffs.isEmpty()) {
            g.drawString(font, "BUFFS:", left, rowY, TEXT_DIM, false);
            int bx = left + font.width("BUFFS: ");
            int resetX = left + font.width("BUFFS: ");
            for (int i = 0; i < buffs.size(); i++) {
                String b = buffs.get(i);
                int bw = font.width(b);
                if (bx + bw > right && bx > resetX) {
                    bx = resetX;
                    rowY += lineH - 2;
                }
                g.drawString(font, b, bx, rowY, TEXT_ACCENT, false);
                bx += bw;
                if (i < buffs.size() - 1) {
                    g.drawString(font, ", ", bx, rowY, TEXT_DIM, false);
                    bx += font.width(", ");
                }
            }
        } else {
            g.drawString(font, "NO ACTIVE BUFFS", left, rowY, TEXT_DIM, false);
        }

        return rowY;
    }

    // === Active buffs collector ===

    private List<String> collectActiveBuffs(SkillData sd, Player player) {
        List<String> buffs = new ArrayList<>();
        // Toggles
        if (sd.isToggleActive(SkillType.STEALTH)) buffs.add("Stealth");
        if (sd.isToggleActive(SkillType.SOUL_ARROW)) buffs.add("Soul Arrow");
        if (sd.isToggleActive(SkillType.MAGIC_GUARD)) buffs.add("Magic Guard");
        if (sd.isToggleActive(SkillType.RAISE_SKELETON)) buffs.add("Skeleton");
        if (sd.isToggleActive(SkillType.SHADOW_CLONE)) buffs.add("Clone");
        if (sd.isToggleActive(SkillType.BONE_SHIELD)) buffs.add("Bone Shield");
        if (sd.isToggleActive(SkillType.SHADOW_PARTNER)) buffs.add("Shadow Partner");
        if (sd.isToggleActive(SkillType.HURRICANE)) buffs.add("Hurricane");
        if (sd.isToggleActive(SkillType.SAGE_MODE))
            buffs.add("Sage +" + (20 + sd.getLevel(SkillType.SAGE_MODE)) + "%");
        if (sd.isToggleActive(SkillType.SOUL_LINK)) buffs.add("Soul Link");
        // Eight Inner Gates (passive, active below 30% HP)
        int gatesLv = sd.getLevel(SkillType.EIGHT_INNER_GATES);
        if (gatesLv > 0 && player.getHealth() / Math.max(1, player.getMaxHealth()) <= 0.3f) {
            buffs.add("8Gates +" + (gatesLv * 2) + "%");
        }
        // Timed buffs
        if (sd.isShadowStrikeActive()) buffs.add("S.Strike");
        if (sd.isRasenganBuffActive()) buffs.add("Rasengan " + fmtSec(sd.getRasenganBuffTicks()));
        if (sd.getSubstitutionTicks() > 0) buffs.add("Sub " + fmtSec(sd.getSubstitutionTicks()));
        if (sd.getFlyingRaijinPhase() == 1) buffs.add("FR Kunai");
        if (sd.getFrgPhase() == 1) buffs.add("FRG " + fmtSec(sd.getFrgTicks()));
        if (sd.isSlashBlastActive()) buffs.add("S.Blast " + fmtSec(sd.getSlashBlastTicks()));
        if (sd.getWarCryTicks() > 0) buffs.add("War Cry " + fmtSec(sd.getWarCryTicks()));
        if (sd.getSpiritBladeTicks() > 0) buffs.add("S.Blade " + fmtSec(sd.getSpiritBladeTicks()));
        if (sd.getUnbreakableCooldown() > 0) buffs.add("Unbrk CD " + fmtSec(sd.getUnbreakableCooldown()));
        if (sd.getArmyTicks() > 0) buffs.add("Army " + fmtSec(sd.getArmyTicks()));
        if (sd.getDeathMarkTicks() > 0) buffs.add("D.Mark " + fmtSec(sd.getDeathMarkTicks()));
        if (sd.getUndyingWillCooldown() > 0) buffs.add("Undying CD " + fmtSec(sd.getUndyingWillCooldown()));
        if (sd.getFervorTicks() > 0) buffs.add("Fervor " + fmtSec(sd.getFervorTicks()));
        // Beast Master
        if (sd.getBmActiveBuff() != null) {
            String name = sd.getBmActiveBuff().getDisplayName();
            if (sd.isBmEnhanced()) name = "Enh." + name;
            buffs.add(name + " " + fmtSec(sd.getBmBuffTicks()));
        }
        if (sd.isPowerOfNatureActive()) buffs.add("Pwr of Nature");
        if (sd.getTurtleShellTicks() > 0) buffs.add("Shell " + fmtSec(sd.getTurtleShellTicks()));
        if (sd.getPhoenixLifestealHits() > 0) buffs.add("Lifesteal x" + sd.getPhoenixLifestealHits());
        return buffs;
    }

    private static String fmtSec(int ticks) {
        return String.format("%.0fs", ticks / 20.0f);
    }

    // === Stat row with inline button ===

    private void drawStatRow(GuiGraphics g, int idx, String label, int value,
                             int sx, int sy, int btnS, int colEnd, int mouseX, int mouseY) {
        String text = label + ": " + value;
        g.drawString(font, text, sx, sy, TEXT_VALUE, false);

        int bx = colEnd - btnS;
        int by = sy - 1;
        boolean hover = false;

        if (hasPoints) {
            statBtnBounds[idx] = new int[]{bx, by, btnS, btnS};
            hover = isInside(mouseX, mouseY, statBtnBounds[idx]);
        } else {
            statBtnBounds[idx] = new int[]{0, 0, 0, 0};
        }

        int bgColor = hasPoints ? (hover ? BTN_HOVER_BG : BTN_BG) : BAR_BG;
        int borderColor = hasPoints ? (hover ? BTN_HOVER_BORDER : BTN_BORDER) : FRAME_DIM;
        g.fill(bx, by, bx + btnS, by + btnS, bgColor);
        drawBox(g, bx, by, btnS, btnS, borderColor);

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
            // Only handle stat button clicks within the scroll area
            if (hasPoints && my >= scrollAreaTop && my <= scrollAreaBottom) {
                String[] names = {"strength", "vitality", "agility", "intelligence", "sight", "luck", "dexterity", "mind", "faith"};
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (delta * lineH * 2)));
        return true;
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

    private void drawStat(GuiGraphics g, String label, String value, int x, int y) {
        g.drawString(font, label, x, y, TEXT_DIM, false);
        g.drawString(font, " " + value, x + font.width(label), y, TEXT_ACCENT, false);
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
