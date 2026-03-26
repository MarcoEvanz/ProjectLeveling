package com.monody.projectleveling.mob;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;

import java.util.Random;

public class MobLevelUtil {

    public static final String KEY_MOB_LEVEL = "pl_mob_level";
    public static final String KEY_MOB_LEVELED = "pl_mob_leveled";

    // === Level Range by Dungeon Index ===

    public static int getDungeonMinLevel(int dungeonIndex) {
        return dungeonIndex * 10 + 1;
    }

    public static int getDungeonMaxLevel(int dungeonIndex) {
        return (dungeonIndex + 1) * 10;
    }

    public static int randomLevelForDungeon(int dungeonIndex, Random random) {
        int min = getDungeonMinLevel(dungeonIndex);
        int max = getDungeonMaxLevel(dungeonIndex);
        return min + random.nextInt(max - min + 1);
    }

    // === Stat Scaling ===

    public static double getHpMultiplier(int level) {
        return 1.0 + 0.15 * (level - 1);
    }

    public static double getDamageMultiplier(int level) {
        return 1.0 + 0.10 * (level - 1);
    }

    public static double getArmorBonus(int level) {
        return Math.min(30.0, (level - 1) * 0.5);
    }

    // === EXP Reward ===

    public static int getExpReward(int mobLevel, int playerLevel, double mobMaxHp) {
        // Base scales with level; HP bonus uses pow(0.7) so bosses reward significantly more
        int base = 3 + mobLevel * 2 + (int) (Math.pow(mobMaxHp, 0.7) * 0.8);
        int diff = playerLevel - mobLevel;
        if (diff > 20) {
            return Math.max(1, base / 10);
        } else if (diff > 10) {
            return Math.max(1, base / 2);
        }
        return base;
    }

    // === PersistentData Helpers ===

    public static int getMobLevel(LivingEntity entity) {
        return entity.getPersistentData().getInt(KEY_MOB_LEVEL);
    }

    public static boolean isMobLeveled(LivingEntity entity) {
        return entity.getPersistentData().getBoolean(KEY_MOB_LEVELED);
    }

    public static void setMobLevel(LivingEntity entity, int level) {
        entity.getPersistentData().putInt(KEY_MOB_LEVEL, level);
        entity.getPersistentData().putBoolean(KEY_MOB_LEVELED, true);
    }

    // === Name Formatting ===

    public static Component getLeveledName(int level, String mobName) {
        ChatFormatting color;
        if (level <= 10) color = ChatFormatting.GRAY;
        else if (level <= 30) color = ChatFormatting.GREEN;
        else if (level <= 60) color = ChatFormatting.YELLOW;
        else if (level <= 100) color = ChatFormatting.GOLD;
        else if (level <= 160) color = ChatFormatting.RED;
        else color = ChatFormatting.DARK_PURPLE;

        MutableComponent lvTag = Component.literal("Lv." + level + " ").withStyle(color);
        return lvTag.append(Component.literal(mobName).withStyle(ChatFormatting.WHITE));
    }
}
