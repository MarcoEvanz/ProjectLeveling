package com.monody.projectleveling.skill;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.skill.classes.*;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * Central registry for skill → stat-line contributions.
 * Each class skill file registers its contributions via {@link #reg}/{@link #regTag}
 * in its own {@code registerStats()} method.
 *
 * Adding a new skill bonus = one line in the relevant class skill file.
 * StatusScreen never needs modification.
 */
public final class StatContribRegistry {

    public enum StatLine { CRIT, CDMG, PROJ, DODGE, DMG_RED, DMG, HP_PCT, MP_REGEN, ATK_PCT, MATK_PCT }

    /** Simple stat definition embedded in SkillType enum constants. */
    public record StatDef(StatLine line, double factor, boolean tagOnly) {}

    /** Create a stat def: level * factor → contributes to total + auto-tagged. */
    public static StatDef stat(StatLine line, double factor) {
        return new StatDef(line, factor, false);
    }

    /** Create a tag-only stat def: displays in tags but doesn't contribute to total. */
    public static StatDef statTag(StatLine line, double factor) {
        return new StatDef(line, factor, true);
    }

    /** Fluent tag builder for stat display breakdown. */
    public static class Tags {
        private final StringBuilder sb = new StringBuilder();

        public Tags add(String entry) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(entry);
            return this;
        }

        /** Base source: "LABEL+X.X" (skipped if value <= 0) */
        public Tags base(String label, double value) {
            if (value > 0) add(label + "+" + String.format("%.1f", value));
            return this;
        }

        /** Percentage buff source: "LABEL+X.X[buff]" (skipped if value <= 0) */
        public Tags buff(String label, double value) {
            if (value > 0) add(label + "+" + String.format("%.1f", value) + "[buff]");
            return this;
        }

        /** Flat buff source: "LABEL+X.X[flat]" (skipped if value <= 0) */
        public Tags flat(String label, double value) {
            if (value > 0) add(label + "+" + String.format("%.1f", value) + "[flat]");
            return this;
        }

        /** Percentage tag: "LABELX%" (skipped if value <= 0) */
        public Tags pct(String label, double value) {
            if (value > 0) add(label + String.format("%.0f", value) + "%");
            return this;
        }

        /** Weapon percentage shorthand: "WPN+X%" */
        public Tags wpn(double value) { return pct("WPN+", value); }

        public String raw() { return sb.toString(); }

        @Override public String toString() {
            return sb.length() > 0 ? " (" + sb + ")" : "";
        }
    }

    // === Registry internals ===

    @FunctionalInterface
    public interface StatContrib {
        double apply(SkillData sd, Player player, PlayerStats stats, Tags tags);
    }

    /** Per-line buckets → O(1) lookup instead of scanning all entries. */
    private static final EnumMap<StatLine, List<StatContrib>> REGISTRY = new EnumMap<>(StatLine.class);

    static {
        for (StatLine line : StatLine.values()) REGISTRY.put(line, new ArrayList<>());
    }

    // === Registration methods ===

    /** Register: skill level * factor → adds to total + pct tag using SkillType.abbreviation */
    public static void reg(StatLine line, SkillType type, double factor) {
        String label = type.getAbbreviation();
        REGISTRY.get(line).add((sd, p, s, tags) -> {
            int lv = sd.getLevel(type);
            if (lv <= 0) return 0;
            double val = lv * factor;
            tags.pct(label, val);
            return val;
        });
    }

    /** Register: tag-only display (returns 0, no contribution to total) */
    public static void regTag(StatLine line, SkillType type, double factor) {
        String label = type.getAbbreviation();
        REGISTRY.get(line).add((sd, p, s, tags) -> {
            int lv = sd.getLevel(type);
            if (lv > 0) tags.pct(label, lv * factor);
            return 0;
        });
    }

    /** Register: custom contribution lambda */
    public static void reg(StatLine line, StatContrib contrib) {
        REGISTRY.get(line).add(contrib);
    }

    // === Query ===

    /** Sum all registered contributions for a stat line, building tags along the way. */
    public static double applyContribs(StatLine line, SkillData sd, Player player, PlayerStats stats, Tags tags) {
        double total = 0;
        for (StatContrib c : REGISTRY.get(line)) {
            total += c.apply(sd, player, stats, tags);
        }
        return total;
    }

    /**
     * Same as {@link #applyContribs} but formats tags as absolute buff values:
     * each contribution's pct is converted to {@code baseValue * pct / 100} and tagged as {@code LABEL+X.X[buff]}.
     */
    public static double applyContribsAsBuff(StatLine line, SkillData sd, Player player, PlayerStats stats, Tags tags, double baseValue) {
        double total = 0;
        for (StatContrib c : REGISTRY.get(line)) {
            Tags temp = new Tags();
            double pct = c.apply(sd, player, stats, temp);
            if (pct > 0) {
                String raw = temp.raw().trim();
                String label = raw.split("\\+")[0];
                tags.buff(label, baseValue * pct / 100.0);
            }
            total += pct;
        }
        return total;
    }

    // === Initialization — call once from mod setup ===

    public static void init() {
        // Auto-register simple contributions from SkillType enum definitions
        for (SkillType type : SkillType.values()) {
            for (StatDef def : type.getStatDefs()) {
                if (def.tagOnly()) regTag(def.line(), type, def.factor());
                else reg(def.line(), type, def.factor());
            }
        }
        // Complex contributions (toggle/stat-dependent, custom format)
        // Add new class here when it has complex stat contribs
        WarriorSkills.registerStats();
        ArcherSkills.registerStats();
        HealerSkills.registerStats();
        MageSkills.registerStats();
        NinjaSkills.registerStats();
        NecromancerSkills.registerStats();
        LimitlessSkills.registerStats();
    }
}
