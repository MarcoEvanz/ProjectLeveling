package com.monody.projectleveling.capability;

import com.monody.projectleveling.skill.PlayerClass;
import com.monody.projectleveling.skill.SkillData;
import com.monody.projectleveling.skill.SkillType;
import net.minecraft.nbt.CompoundTag;

public class PlayerStats {
    private final SkillData skillData = new SkillData();

    public SkillData getSkillData() { return skillData; }
    private int level = 1;
    private int strength = 1;
    private int vitality = 1;
    private int agility = 1;
    private int intelligence = 1;
    private int sight = 1;
    private int luck = 1;
    private int dexterity = 1;
    private int mind = 1;
    private int faith = 1;
    private int remainingPoints = 5;
    private int currentMp = 100;
    private int currentExp = 0;
    private String job = "Novice";
    private String title = "None";

    // Daily Quest tracking
    public static final int QUEST_KILL_TARGET = 20;
    public static final int QUEST_MINE_TARGET = 50;
    public static final int QUEST_WALK_TARGET = 1000;
    private int questKills = 0;
    private int questBlocksMined = 0;
    private int questBlocksWalked = 0;
    private boolean questCompleted = false;
    private boolean questRewardClaimed = false;
    private long questDay = -1; // the game day this quest was assigned

    // Getters
    public int getLevel() { return level; }
    public int getStrength() { return strength; }
    public int getVitality() { return vitality; }
    public int getAgility() { return agility; }
    public int getIntelligence() { return intelligence; }
    public int getSight() { return sight; }
    public int getLuck() { return luck; }
    public int getDexterity() { return dexterity; }
    public int getMind() { return mind; }
    public int getFaith() { return faith; }
    public int getRemainingPoints() { return remainingPoints; }
    public int getCurrentMp() { return currentMp; }
    public int getCurrentExp() { return currentExp; }
    public int getMaxExp() { return level * 100; }
    public String getJob() { return job; }
    public String getTitle() { return title; }
    public int getQuestKills() { return questKills; }
    public int getQuestBlocksMined() { return questBlocksMined; }
    public int getQuestBlocksWalked() { return questBlocksWalked; }
    public boolean isQuestCompleted() { return questCompleted; }
    public boolean isQuestRewardClaimed() { return questRewardClaimed; }
    public long getQuestDay() { return questDay; }

    /** Magic Attack: derived from INT. 0.1 MATK per INT point. */
    public float getMagicAttack() { return intelligence * 0.1f; }

    /** Healing Power: derived from Faith. 0.1 per Faith point. */
    public float getHealingPower() { return faith * 0.1f; }

    public int getMaxMp() {
        int base = 100 + (mind - 1) * 10;
        int dpLv = skillData.getLevel(SkillType.DARK_PACT);
        if (dpLv > 0) {
            base = (int) (base * (1 + dpLv * 0.02));
        }
        if (skillData.isToggleActive(SkillType.SHADOW_PARTNER)) {
            base /= 2;
        }
        // Multi Shadow Clone: -4% max MP per level when Shadow Clone is active
        if (skillData.isToggleActive(SkillType.SHADOW_CLONE)) {
            int mscLv = skillData.getLevel(SkillType.MULTI_SHADOW_CLONE);
            if (mscLv > 0) {
                base = (int) (base * (1.0 - mscLv * 0.04));
            }
        }
        return Math.max(1, base);
    }

    // Setters
    public void setLevel(int level) { this.level = level; }
    public void setStrength(int strength) { this.strength = strength; }
    public void setVitality(int vitality) { this.vitality = vitality; }
    public void setAgility(int agility) { this.agility = agility; }
    public void setIntelligence(int intelligence) { this.intelligence = intelligence; }
    public void setSight(int sight) { this.sight = sight; }
    public void setLuck(int luck) { this.luck = luck; }
    public void setDexterity(int dexterity) { this.dexterity = dexterity; }
    public void setMind(int mind) { this.mind = mind; }
    public void setFaith(int faith) { this.faith = faith; }
    public void setRemainingPoints(int remainingPoints) { this.remainingPoints = remainingPoints; }
    public void setCurrentMp(int currentMp) { this.currentMp = Math.min(currentMp, getMaxMp()); }
    public void setCurrentExp(int currentExp) { this.currentExp = currentExp; }
    public void setJob(String job) { this.job = job; }
    public void setQuestKills(int questKills) { this.questKills = questKills; }
    public void setQuestBlocksMined(int questBlocksMined) { this.questBlocksMined = questBlocksMined; }
    public void setQuestBlocksWalked(int questBlocksWalked) { this.questBlocksWalked = questBlocksWalked; }
    public void setQuestCompleted(boolean questCompleted) { this.questCompleted = questCompleted; }
    public void setQuestRewardClaimed(boolean questRewardClaimed) { this.questRewardClaimed = questRewardClaimed; }
    public void setQuestDay(long questDay) { this.questDay = questDay; }
    public void setTitle(String title) { this.title = title; }

    /** Regenerate MP. Call once per second (every 20 ticks). Mind stat adds 0.01% per point. Returns true if MP changed. */
    public boolean regenMp() {
        int max = getMaxMp();
        if (currentMp >= max) return false;
        // Base: 1% of max per second. Mind adds 0.01% per point (so Mind 100 = +1% = doubled regen)
        double regenPct = 0.01 + (mind - 1) * 0.0001;
        int regen = Math.max(1, (int) (max * regenPct));
        currentMp = Math.min(currentMp + regen, max);
        return true;
    }

    // === Daily Quest Logic ===

    /** Reset quest progress for a new day. */
    public void resetQuest(long day) {
        questKills = 0;
        questBlocksMined = 0;
        questBlocksWalked = 0;
        questCompleted = false;
        questRewardClaimed = false;
        questDay = day;
    }

    /** Check if a new day has started and reset if needed. Returns true if quest was reset. */
    public boolean checkNewDay(long currentDay) {
        if (questDay < 0 || currentDay > questDay) {
            resetQuest(currentDay);
            return true;
        }
        return false;
    }

    public void addQuestKill() {
        if (!questCompleted) questKills = Math.min(questKills + 1, QUEST_KILL_TARGET);
    }

    public void addQuestBlockMined() {
        if (!questCompleted) questBlocksMined = Math.min(questBlocksMined + 1, QUEST_MINE_TARGET);
    }

    public void addQuestBlockWalked(int blocks) {
        if (!questCompleted) questBlocksWalked = Math.min(questBlocksWalked + blocks, QUEST_WALK_TARGET);
    }

    /** Check if all objectives are met. Does not mark as completed. */
    public boolean areQuestObjectivesMet() {
        return questKills >= QUEST_KILL_TARGET
                && questBlocksMined >= QUEST_MINE_TARGET
                && questBlocksWalked >= QUEST_WALK_TARGET;
    }

    /** Get EXP reward for completing the daily quest. After level 10, only 50% of EXP needed. */
    public int getQuestExpReward() {
        if (level <= 10) {
            return level * 80;
        }
        return getMaxExp() / 2;
    }

    // === Leveling ===

    private static final int POINTS_PER_LEVEL = 4;

    /**
     * Add EXP and handle level-ups. Returns the number of levels gained.
     */
    public int addExp(int amount) {
        if (amount <= 0) return 0;
        currentExp += amount;
        int levelsGained = 0;
        while (currentExp >= getMaxExp()) {
            currentExp -= getMaxExp();
            level++;
            remainingPoints += POINTS_PER_LEVEL;
            // Tier-separated SP: each tier earns SP in its own level range
            // Class-specific bonus SP is defined in PlayerClass.getBonusSPPerEvenLevel()
            PlayerClass playerClass = skillData.getSelectedClass();
            if (level >= 2 && level <= 10) {
                int sp = level == 10 ? 2 : 1;
                if (level % 2 == 0) sp += playerClass.getBonusSPPerEvenLevel(0);
                skillData.addTierSP(0, sp);
            } else if (level >= 11 && level <= 30) {
                int sp = 1 + (level % 2 == 0 ? 1 : 0);
                if (level % 2 == 0) sp += playerClass.getBonusSPPerEvenLevel(1);
                skillData.addTierSP(1, sp);
            } else if (level >= 31 && level <= 60) {
                int sp = 1 + (level % 2 == 0 ? 1 : 0);
                if (level % 2 == 0) sp += playerClass.getBonusSPPerEvenLevel(2);
                skillData.addTierSP(2, sp);
            } else if (level >= 61 && level <= 100) {
                int sp = 1 + (level % 2 == 0 ? 1 : 0);
                if (level % 2 == 0) sp += playerClass.getBonusSPPerEvenLevel(3);
                skillData.addTierSP(3, sp);
            }
            levelsGained++;
        }
        return levelsGained;
    }

    /**
     * Retroactively grant class-specific bonus SP for all even levels
     * already reached. Called once when a class is first selected.
     */
    public void grantRetroactiveBonusSP(PlayerClass cls) {
        int[][] tierRanges = {{2, 10}, {11, 30}, {31, 60}, {61, 100}};
        for (int tier = 0; tier < 4; tier++) {
            int bonusPerEven = cls.getBonusSPPerEvenLevel(tier);
            if (bonusPerEven <= 0) continue;
            int start = tierRanges[tier][0];
            int end = Math.min(tierRanges[tier][1], level);
            if (level < start) continue;
            int evenCount = 0;
            for (int lv = start; lv <= end; lv++) {
                if (lv % 2 == 0) evenCount++;
            }
            skillData.addTierSP(tier, evenCount * bonusPerEven);
        }
    }

    public void resetStats() {
        int spent = (strength - 1) + (vitality - 1) + (agility - 1) + (intelligence - 1) + (sight - 1)
                + (luck - 1) + (dexterity - 1) + (mind - 1) + (faith - 1);
        strength = 1;
        vitality = 1;
        agility = 1;
        intelligence = 1;
        sight = 1;
        luck = 1;
        dexterity = 1;
        mind = 1;
        faith = 1;
        remainingPoints += spent;
        currentMp = getMaxMp();
    }

    public void resetLevel() {
        level = 1;
        strength = 1;
        vitality = 1;
        agility = 1;
        intelligence = 1;
        sight = 1;
        luck = 1;
        dexterity = 1;
        mind = 1;
        faith = 1;
        remainingPoints = 5;
        currentMp = getMaxMp();
        currentExp = 0;
        skillData.reset();
    }

    public boolean allocateStat(String stat) {
        if (remainingPoints <= 0) return false;
        switch (stat.toLowerCase()) {
            case "strength" -> strength++;
            case "vitality" -> vitality++;
            case "agility" -> agility++;
            case "intelligence" -> intelligence++;
            case "sight" -> sight++;
            case "luck" -> luck++;
            case "dexterity" -> dexterity++;
            case "mind" -> mind++;
            case "faith" -> faith++;
            default -> { return false; }
        }
        remainingPoints--;
        return true;
    }

    public void copyFrom(PlayerStats other) {
        this.level = other.level;
        this.strength = other.strength;
        this.vitality = other.vitality;
        this.agility = other.agility;
        this.intelligence = other.intelligence;
        this.sight = other.sight;
        this.luck = other.luck;
        this.dexterity = other.dexterity;
        this.mind = other.mind;
        this.faith = other.faith;
        this.remainingPoints = other.remainingPoints;
        this.currentMp = other.currentMp;
        this.currentExp = other.currentExp;
        this.job = other.job;
        this.title = other.title;
        this.questKills = other.questKills;
        this.questBlocksMined = other.questBlocksMined;
        this.questBlocksWalked = other.questBlocksWalked;
        this.questCompleted = other.questCompleted;
        this.questRewardClaimed = other.questRewardClaimed;
        this.questDay = other.questDay;
        this.skillData.copyFrom(other.skillData);
    }

    public CompoundTag saveNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("level", level);
        tag.putInt("strength", strength);
        tag.putInt("vitality", vitality);
        tag.putInt("agility", agility);
        tag.putInt("intelligence", intelligence);
        tag.putInt("sight", sight);
        tag.putInt("luck", luck);
        tag.putInt("dexterity", dexterity);
        tag.putInt("mind", mind);
        tag.putInt("faith", faith);
        tag.putInt("remainingPoints", remainingPoints);
        tag.putInt("currentMp", currentMp);
        tag.putInt("currentExp", currentExp);
        tag.putString("job", job);
        tag.putString("title", title);
        tag.putInt("questKills", questKills);
        tag.putInt("questBlocksMined", questBlocksMined);
        tag.putInt("questBlocksWalked", questBlocksWalked);
        tag.putBoolean("questCompleted", questCompleted);
        tag.putBoolean("questRewardClaimed", questRewardClaimed);
        tag.putLong("questDay", questDay);
        tag.put("skills", skillData.saveNBT());
        return tag;
    }

    public void loadNBT(CompoundTag tag) {
        level = tag.getInt("level");
        strength = tag.getInt("strength");
        vitality = tag.getInt("vitality");
        agility = tag.getInt("agility");
        intelligence = tag.getInt("intelligence");
        sight = tag.contains("sight") ? tag.getInt("sight") : (tag.contains("sense") ? tag.getInt("sense") : 1);
        luck = tag.contains("luck") ? tag.getInt("luck") : 1;
        dexterity = tag.contains("dexterity") ? tag.getInt("dexterity") : 1;
        mind = tag.contains("mind") ? tag.getInt("mind") : 1;
        faith = tag.contains("faith") ? tag.getInt("faith") : 1;
        remainingPoints = tag.getInt("remainingPoints");
        currentMp = tag.getInt("currentMp");
        currentExp = tag.getInt("currentExp");
        job = tag.contains("job") ? tag.getString("job") : "Novice";
        title = tag.contains("title") ? tag.getString("title") : "None";
        questKills = tag.getInt("questKills");
        questBlocksMined = tag.getInt("questBlocksMined");
        questBlocksWalked = tag.getInt("questBlocksWalked");
        questCompleted = tag.getBoolean("questCompleted");
        questRewardClaimed = tag.getBoolean("questRewardClaimed");
        questDay = tag.getLong("questDay");
        if (tag.contains("skills")) {
            skillData.loadNBT(tag.getCompound("skills"));
        }
    }
}
