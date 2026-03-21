package com.monody.projectleveling.skill;

import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class SkillData {
    private final int[] tierSP = new int[4]; // Separate SP pool per tier (T0-T3)
    private PlayerClass selectedClass = PlayerClass.NONE;
    private final Map<SkillType, Integer> skillLevels = new EnumMap<>(SkillType.class);
    public static final int MAX_SLOTS = 7;
    private final SkillType[] equippedSlots = new SkillType[MAX_SLOTS];

    // Transient state (not saved to NBT)
    private final Map<SkillType, Integer> cooldowns = new EnumMap<>(SkillType.class);
    private final Map<SkillType, Boolean> toggleStates = new EnumMap<>(SkillType.class);
    private boolean shadowStrikeActive = false;
    private int shadowStrikeTicks = 0;
    private int domainTicks = 0;
    private double domainX;
    private double domainY;
    private double domainZ;
    // Venom
    private boolean venomActive = false;
    private int venomTicks = 0;
    // Phoenix summon
    private int phoenixTicks = 0;
    // Infinity buff
    private int infinityTicks = 0;
    // Poison Mist zone
    private boolean mistActive = false;
    private int mistTicks = 0;
    private double mistX, mistY, mistZ;
    // Benediction zone
    private int benedictionTicks = 0;
    private double benedictionX, benedictionY, benedictionZ;
    // Arrow Rain zone
    private int arrowRainTicks = 0;
    private double arrowRainX, arrowRainY, arrowRainZ;
    private float arrowRainDamage = 0;
    private float arrowRainRadius = 3.0f;
    private int arrowRainTargetId = -1; // Entity ID for rain to follow
    // Evasion guaranteed crit (set true on dodge)
    private boolean evasionCritReady = false;
    // Army of the Dead zone
    private int armyTicks = 0;
    private double armyX, armyY, armyZ;
    // Death Mark
    private int deathMarkTicks = 0;
    private int deathMarkTargetId = -1;
    // Undying Will internal cooldown
    private int undyingWillCooldown = 0;

    // === Getters ===

    public int getTierSP(int tier) { return (tier >= 0 && tier < 4) ? tierSP[tier] : 0; }
    public void addTierSP(int tier, int amount) { if (tier >= 0 && tier < 4) tierSP[tier] += amount; }
    public void setTierSP(int tier, int amount) { if (tier >= 0 && tier < 4) tierSP[tier] = amount; }

    public PlayerClass getSelectedClass() { return selectedClass; }
    public void setSelectedClass(PlayerClass cls) { this.selectedClass = cls; }

    public int getLevel(SkillType skill) {
        return skillLevels.getOrDefault(skill, 0);
    }

    public boolean isUnlocked(SkillType skill) {
        return getLevel(skill) > 0;
    }

    public SkillType getEquipped(int slot) {
        return (slot >= 0 && slot < MAX_SLOTS) ? equippedSlots[slot] : null;
    }

    public SkillType[] getEquippedSlots() { return equippedSlots; }

    public boolean isOnCooldown(SkillType skill) {
        return cooldowns.getOrDefault(skill, 0) > 0;
    }

    public int getCooldownRemaining(SkillType skill) {
        return cooldowns.getOrDefault(skill, 0);
    }

    public boolean isToggleActive(SkillType skill) {
        return toggleStates.getOrDefault(skill, false);
    }

    public boolean isShadowStrikeActive() { return shadowStrikeActive; }
    public int getShadowStrikeTicks() { return shadowStrikeTicks; }
    public void setShadowStrikeActive(boolean active) { this.shadowStrikeActive = active; }
    public void setShadowStrikeTicks(int ticks) { this.shadowStrikeTicks = ticks; }

    public int getDomainTicks() { return domainTicks; }
    public void setDomainTicks(int ticks) { this.domainTicks = ticks; }
    public double getDomainX() { return domainX; }
    public double getDomainY() { return domainY; }
    public double getDomainZ() { return domainZ; }
    public void setDomainPos(double x, double y, double z) {
        this.domainX = x;
        this.domainY = y;
        this.domainZ = z;
    }

    // Venom
    public boolean isVenomActive() { return venomActive; }
    public void setVenomActive(boolean active) { this.venomActive = active; }
    public int getVenomTicks() { return venomTicks; }
    public void setVenomTicks(int ticks) { this.venomTicks = ticks; }

    // Phoenix
    public int getPhoenixTicks() { return phoenixTicks; }
    public void setPhoenixTicks(int ticks) { this.phoenixTicks = ticks; }

    // Infinity
    public int getInfinityTicks() { return infinityTicks; }
    public void setInfinityTicks(int ticks) { this.infinityTicks = ticks; }

    // Poison Mist
    public boolean isMistActive() { return mistActive; }
    public void setMistActive(boolean active) { this.mistActive = active; }
    public int getMistTicks() { return mistTicks; }
    public void setMistTicks(int ticks) { this.mistTicks = ticks; }
    public double getMistX() { return mistX; }
    public double getMistY() { return mistY; }
    public double getMistZ() { return mistZ; }
    public void setMistPos(double x, double y, double z) {
        this.mistX = x; this.mistY = y; this.mistZ = z;
    }

    // Benediction
    public int getBenedictionTicks() { return benedictionTicks; }
    public void setBenedictionTicks(int ticks) { this.benedictionTicks = ticks; }
    public double getBenedictionX() { return benedictionX; }
    public double getBenedictionY() { return benedictionY; }
    public double getBenedictionZ() { return benedictionZ; }
    public void setBenedictionPos(double x, double y, double z) {
        this.benedictionX = x; this.benedictionY = y; this.benedictionZ = z;
    }

    // Arrow Rain
    public int getArrowRainTicks() { return arrowRainTicks; }
    public void setArrowRainTicks(int ticks) { this.arrowRainTicks = ticks; }
    public double getArrowRainX() { return arrowRainX; }
    public double getArrowRainY() { return arrowRainY; }
    public double getArrowRainZ() { return arrowRainZ; }
    public void setArrowRainPos(double x, double y, double z) {
        this.arrowRainX = x; this.arrowRainY = y; this.arrowRainZ = z;
    }
    public float getArrowRainDamage() { return arrowRainDamage; }
    public void setArrowRainDamage(float dmg) { this.arrowRainDamage = dmg; }
    public float getArrowRainRadius() { return arrowRainRadius; }
    public void setArrowRainRadius(float r) { this.arrowRainRadius = r; }
    public int getArrowRainTargetId() { return arrowRainTargetId; }
    public void setArrowRainTargetId(int id) { this.arrowRainTargetId = id; }

    // Evasion crit
    public boolean isEvasionCritReady() { return evasionCritReady; }
    public void setEvasionCritReady(boolean ready) { this.evasionCritReady = ready; }

    // Army of the Dead
    public int getArmyTicks() { return armyTicks; }
    public void setArmyTicks(int ticks) { this.armyTicks = ticks; }
    public double getArmyX() { return armyX; }
    public double getArmyY() { return armyY; }
    public double getArmyZ() { return armyZ; }
    public void setArmyPos(double x, double y, double z) {
        this.armyX = x; this.armyY = y; this.armyZ = z;
    }

    // Death Mark
    public int getDeathMarkTicks() { return deathMarkTicks; }
    public void setDeathMarkTicks(int ticks) { this.deathMarkTicks = ticks; }
    public int getDeathMarkTargetId() { return deathMarkTargetId; }
    public void setDeathMarkTargetId(int id) { this.deathMarkTargetId = id; }

    // Undying Will
    public int getUndyingWillCooldown() { return undyingWillCooldown; }
    public void setUndyingWillCooldown(int ticks) { this.undyingWillCooldown = ticks; }

    public Map<SkillType, Integer> getSkillLevels() { return skillLevels; }
    public Map<SkillType, Integer> getCooldowns() { return cooldowns; }
    public Map<SkillType, Boolean> getToggleStates() { return toggleStates; }

    // === Setters for sync ===

    public int[] getTierSPArray() { return tierSP; }

    public void setLevel(SkillType skill, int level) {
        if (level > 0) {
            skillLevels.put(skill, level);
        } else {
            skillLevels.remove(skill);
        }
    }

    public void setEquipped(int slot, SkillType skill) {
        if (slot >= 0 && slot < MAX_SLOTS) {
            equippedSlots[slot] = skill;
        }
    }

    public void setCooldown(SkillType skill, int ticks) {
        if (ticks > 0) {
            cooldowns.put(skill, ticks);
        } else {
            cooldowns.remove(skill);
        }
    }

    public void setToggleActive(SkillType skill, boolean active) {
        toggleStates.put(skill, active);
    }

    // === Skill visibility ===

    /** Get skills visible for a specific tier, filtered by player class. */
    public List<SkillType> getVisibleSkills(int tier) {
        List<SkillType> result = new ArrayList<>();
        for (SkillType skill : SkillType.values()) {
            if (skill.getTier() != tier) continue;
            if (!skill.isAvailable()) continue;
            PlayerClass req = skill.getRequiredClass();
            if (req == null || req == selectedClass) {
                result.add(skill);
            }
        }
        return result;
    }

    // === Unlock / Level Up ===

    public boolean canUnlock(SkillType skill, int playerLevel) {
        if (tierSP[skill.getTier()] <= 0) return false;
        int currentLevel = getLevel(skill);
        if (currentLevel >= skill.getMaxLevel()) return false;
        if (!skill.isAvailable()) return false;

        // Check player level requirement
        if (playerLevel < skill.getRequiredPlayerLevel()) return false;

        // Check class requirement
        PlayerClass req = skill.getRequiredClass();
        if (req != null && req != selectedClass) return false;

        return true;
    }

    /** Unlock or level up a skill. Returns true if successful. */
    public boolean unlockOrLevel(SkillType skill, int playerLevel) {
        if (!canUnlock(skill, playerLevel)) return false;
        skillLevels.merge(skill, 1, Integer::sum);
        tierSP[skill.getTier()]--;
        return true;
    }

    // === Equip / Unequip ===

    public void equip(SkillType skill, int slot) {
        if (slot < 0 || slot >= MAX_SLOTS) return;
        if (!isUnlocked(skill)) return;
        if (skill.isPassive()) return; // passives can't be equipped

        // Remove from any existing slot
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (equippedSlots[i] == skill) {
                equippedSlots[i] = null;
            }
        }
        equippedSlots[slot] = skill;
    }

    public void unequip(int slot) {
        if (slot >= 0 && slot < MAX_SLOTS) {
            equippedSlots[slot] = null;
        }
    }

    // === Cooldowns ===

    public void startCooldown(SkillType skill, int level) {
        cooldowns.put(skill, skill.getCooldownTicks(level));
    }

    public void tickCooldowns() {
        cooldowns.entrySet().removeIf(e -> {
            e.setValue(e.getValue() - 1);
            return e.getValue() <= 0;
        });
    }

    // === Reset ===

    public void reset() {
        Arrays.fill(tierSP, 0);
        selectedClass = PlayerClass.NONE;
        skillLevels.clear();
        for (int i = 0; i < MAX_SLOTS; i++) equippedSlots[i] = null;
        clearTransient();
    }

    public void clearTransient() {
        cooldowns.clear();
        toggleStates.clear();
        shadowStrikeActive = false;
        shadowStrikeTicks = 0;
        domainTicks = 0;
        venomActive = false;
        venomTicks = 0;
        phoenixTicks = 0;
        infinityTicks = 0;
        mistActive = false;
        mistTicks = 0;
        benedictionTicks = 0;
        arrowRainTicks = 0;
        arrowRainDamage = 0;
        arrowRainTargetId = -1;
        evasionCritReady = false;
        armyTicks = 0;
        deathMarkTicks = 0;
        deathMarkTargetId = -1;
        undyingWillCooldown = 0;
    }

    // === NBT (only persistent data) ===

    public CompoundTag saveNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("tierSP", tierSP);
        tag.putString("selectedClass", selectedClass.getId());

        CompoundTag levels = new CompoundTag();
        for (Map.Entry<SkillType, Integer> e : skillLevels.entrySet()) {
            levels.putInt(e.getKey().getId(), e.getValue());
        }
        tag.put("skillLevels", levels);

        for (int i = 0; i < MAX_SLOTS; i++) {
            tag.putString("slot" + i, equippedSlots[i] != null ? equippedSlots[i].getId() : "");
        }

        return tag;
    }

    public void loadNBT(CompoundTag tag) {
        Arrays.fill(tierSP, 0);
        if (tag.contains("tierSP")) {
            int[] saved = tag.getIntArray("tierSP");
            System.arraycopy(saved, 0, tierSP, 0, Math.min(saved.length, 4));
        } else if (tag.contains("skillPoints")) {
            // Backward compat: old saves had a single skillPoints field
            tierSP[0] = tag.getInt("skillPoints");
        }
        selectedClass = PlayerClass.fromId(tag.getString("selectedClass"));

        skillLevels.clear();
        if (tag.contains("skillLevels")) {
            CompoundTag levels = tag.getCompound("skillLevels");
            for (String key : levels.getAllKeys()) {
                SkillType type = SkillType.fromId(key);
                if (type != null) {
                    skillLevels.put(type, levels.getInt(key));
                }
            }
        }

        for (int i = 0; i < MAX_SLOTS; i++) {
            String id = tag.getString("slot" + i);
            equippedSlots[i] = id.isEmpty() ? null : SkillType.fromId(id);
        }

        clearTransient();
    }

    public void copyFrom(SkillData other) {
        System.arraycopy(other.tierSP, 0, this.tierSP, 0, 4);
        this.selectedClass = other.selectedClass;
        this.skillLevels.clear();
        this.skillLevels.putAll(other.skillLevels);
        for (int i = 0; i < MAX_SLOTS; i++) {
            this.equippedSlots[i] = other.equippedSlots[i];
        }
        clearTransient();
    }
}
