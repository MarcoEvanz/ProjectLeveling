package com.monody.projectleveling.skill;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.skill.classes.*;

import java.util.List;

public final class SkillTooltips {

    public static final int TEXT_VALUE = 0xFFC0D8F0;
    public static final int TEXT_DIM = 0xFF405868;

    private SkillTooltips() {}

    public static void addDetailLines(List<String> texts, List<int[]> lines,
                                       SkillType skill, int level, PlayerStats stats) {
        PlayerClass cls = skill.getRequiredClass();
        if (cls == null) {
            NoviceSkills.addDetailLines(texts, lines, skill, level, stats);
        } else {
            switch (cls) {
                case WARRIOR -> WarriorSkills.addDetailLines(texts, lines, skill, level, stats);
                case ASSASSIN -> AssassinSkills.addDetailLines(texts, lines, skill, level, stats);
                case ARCHER -> ArcherSkills.addDetailLines(texts, lines, skill, level, stats);
                case HEALER -> HealerSkills.addDetailLines(texts, lines, skill, level, stats);
                case MAGE -> MageSkills.addDetailLines(texts, lines, skill, level, stats);
                case NINJA -> NinjaSkills.addDetailLines(texts, lines, skill, level, stats);
                case NECROMANCER -> NecromancerSkills.addDetailLines(texts, lines, skill, level, stats);
                case BEAST_MASTER -> BeastMasterSkills.addDetailLines(texts, lines, skill, level, stats);
                default -> {}
            }
        }
    }

    public static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(n);
        };
    }
}
