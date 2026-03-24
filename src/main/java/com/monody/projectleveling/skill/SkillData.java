package com.monody.projectleveling.skill;

import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class SkillData {
    private final int[] tierSP = new int[5]; // Separate SP pool per tier (T0-T4)
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
    private int infinityStacks = 0;
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
    // Unholy Fervor buff
    private int fervorTicks = 0;
    // Ninja: Substitution Jutsu
    private int substitutionTicks = 0;
    // Ninja: Sage Mode MP drain tick counter
    private int sageModeDrainTimer = 0;
    // Ninja: Eight Inner Gates invuln cooldown
    private int gatesInvulnCooldown = 0;
    // Ninja: Flying Raijin state
    private int flyingRaijinPhase = 0; // 0 = ready to throw, 1 = kunai out
    private double flyingRaijinX, flyingRaijinY, flyingRaijinZ;
    private int flyingRaijinTargetId = -1; // marked entity ID (-1 = none)
    private int flyingRaijinKunaiId = -1;  // kunai entity ID (-1 = none)
    // Ninja: Flying Raijin Ground state
    private int frgPhase = 0; // 0 = ready to place, 1 = kunai placed
    private double frgX, frgY, frgZ;
    private int frgTicks = 0; // remaining lifetime ticks
    private int frgKunaiId = -1; // ground kunai entity ID (-1 = none)
    // Ninja: Rasengan buff
    private boolean rasenganBuffActive = false;
    private int rasenganBuffTicks = 0;
    // Ninja: Flying Raijin SSRZ state
    private int ssrzPhase = 0;         // 0 = throw, 1-9 = combo strikes
    private int ssrzTargetId = -1;     // marked target entity ID
    private int ssrzMarkTicks = 0;     // mark duration (200 ticks = 10s)
    // Beast Master state
    private SkillType bmActiveBuff = null;       // Which next-attack buff is loaded
    private int bmBuffTicks = 0;                 // 80 tick (4s) countdown
    private boolean bmEnhanced = false;          // Power of Nature doubled this buff
    private boolean powerOfNatureActive = false;  // Doubles next skill
    private int turtleShellTicks = 0;            // 100 tick (5s) shield timer
    private int phoenixLifestealHits = 0;        // Remaining lifesteal hits (max 2)
    private float phoenixLifestealPct = 0;       // Lifesteal % per hit
    // Tiger Claw delayed hits
    private int tigerClawHitsLeft = 0;           // Remaining extra hits to deal
    private int tigerClawTargetId = -1;          // Entity ID of target
    private float tigerClawDmg = 0;              // Damage per extra hit
    private int tigerClawTimer = 0;              // Ticks until next hit (4 ticks = 0.2s)
    private boolean tigerClawEnhanced = false;   // Was Tiger Claw enhanced by Power of Nature?
    private boolean phoenixLifestealEnhanced = false; // Was Phoenix Wings enhanced?
    // Warrior: Slash Blast next-melee buff (4s duration)
    private boolean slashBlastActive = false;
    private float slashBlastPct = 0;             // Bonus damage %
    private int slashBlastTicks = 0;             // Buff expiry countdown
    // Warrior: War Cry attack buff
    private int warCryTicks = 0;
    private float warCryAtkBonus = 0;            // ATK% bonus (e.g. 10 = +10%)
    // Warrior: Spirit Blade party buff
    private int spiritBladeTicks = 0;
    private float spiritBladeAtk = 0;            // Flat attack bonus (added after %)
    private boolean spiritBladeDefActive = false; // -Damage Taken (caster only)
    // Warrior: Unbreakable passive cooldown
    private int unbreakableCooldown = 0;
    // Limitless: Black Flash next-melee buff
    private boolean blackFlashActive = false;
    private float blackFlashMultiplier = 0;
    private int blackFlashTicks = 0;
    // Limitless: Blue channel state
    private boolean blueChanneling = false;
    private int blueChannelTicks = 0;
    private int blueDrainTimer = 0;
    // Limitless: Red channel state
    private boolean redChanneling = false;
    private int redChannelTicks = 0;
    private int redDrainTimer = 0;
    // Limitless: Purple channel state
    private boolean purpleChanneling = false;
    private int purpleChannelTicks = 0;
    // Generic channeling bar (reusable for any skill)
    private int channelTicks = 0;
    private int channelMaxTicks = 0;
    private String channelSkillName = "";

    // === Getters ===

    public int getTierSP(int tier) { return (tier >= 0 && tier < 5) ? tierSP[tier] : 0; }
    public void addTierSP(int tier, int amount) { if (tier >= 0 && tier < 5) tierSP[tier] += amount; }
    public void setTierSP(int tier, int amount) { if (tier >= 0 && tier < 5) tierSP[tier] = amount; }

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
    public int getInfinityStacks() { return infinityStacks; }
    public void setInfinityStacks(int stacks) { this.infinityStacks = stacks; }

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
    public int getFervorTicks() { return fervorTicks; }
    public void setFervorTicks(int ticks) { this.fervorTicks = ticks; }

    // Ninja: Substitution Jutsu
    public int getSubstitutionTicks() { return substitutionTicks; }
    public void setSubstitutionTicks(int ticks) { this.substitutionTicks = ticks; }

    // Ninja: Sage Mode drain timer
    public int getSageModeDrainTimer() { return sageModeDrainTimer; }
    public void setSageModeDrainTimer(int ticks) { this.sageModeDrainTimer = ticks; }

    // Ninja: Eight Inner Gates invuln cooldown
    public int getGatesInvulnCooldown() { return gatesInvulnCooldown; }
    public void setGatesInvulnCooldown(int ticks) { this.gatesInvulnCooldown = ticks; }

    // Ninja: Flying Raijin
    public int getFlyingRaijinPhase() { return flyingRaijinPhase; }
    public void setFlyingRaijinPhase(int phase) { this.flyingRaijinPhase = phase; }
    public double getFlyingRaijinX() { return flyingRaijinX; }
    public double getFlyingRaijinY() { return flyingRaijinY; }
    public double getFlyingRaijinZ() { return flyingRaijinZ; }
    public void setFlyingRaijinPos(double x, double y, double z) {
        this.flyingRaijinX = x; this.flyingRaijinY = y; this.flyingRaijinZ = z;
    }
    public int getFlyingRaijinTargetId() { return flyingRaijinTargetId; }
    public void setFlyingRaijinTargetId(int id) { this.flyingRaijinTargetId = id; }
    public int getFlyingRaijinKunaiId() { return flyingRaijinKunaiId; }
    public void setFlyingRaijinKunaiId(int id) { this.flyingRaijinKunaiId = id; }

    // Ninja: Flying Raijin Ground
    public int getFrgPhase() { return frgPhase; }
    public void setFrgPhase(int phase) { this.frgPhase = phase; }
    public double getFrgX() { return frgX; }
    public double getFrgY() { return frgY; }
    public double getFrgZ() { return frgZ; }
    public void setFrgPos(double x, double y, double z) { this.frgX = x; this.frgY = y; this.frgZ = z; }
    public int getFrgTicks() { return frgTicks; }
    public void setFrgTicks(int ticks) { this.frgTicks = ticks; }
    public int getFrgKunaiId() { return frgKunaiId; }
    public void setFrgKunaiId(int id) { this.frgKunaiId = id; }

    // Ninja: Rasengan buff
    public boolean isRasenganBuffActive() { return rasenganBuffActive; }
    public void setRasenganBuffActive(boolean active) { this.rasenganBuffActive = active; }
    public int getRasenganBuffTicks() { return rasenganBuffTicks; }
    public void setRasenganBuffTicks(int ticks) { this.rasenganBuffTicks = ticks; }
    // Ninja: Flying Raijin SSRZ
    public int getSsrzPhase() { return ssrzPhase; }
    public void setSsrzPhase(int phase) { this.ssrzPhase = phase; }
    public int getSsrzTargetId() { return ssrzTargetId; }
    public void setSsrzTargetId(int id) { this.ssrzTargetId = id; }
    public int getSsrzMarkTicks() { return ssrzMarkTicks; }
    public void setSsrzMarkTicks(int ticks) { this.ssrzMarkTicks = ticks; }

    // Beast Master
    public SkillType getBmActiveBuff() { return bmActiveBuff; }
    public void setBmActiveBuff(SkillType buff) { this.bmActiveBuff = buff; }
    public int getBmBuffTicks() { return bmBuffTicks; }
    public void setBmBuffTicks(int ticks) { this.bmBuffTicks = ticks; }
    public boolean isBmEnhanced() { return bmEnhanced; }
    public void setBmEnhanced(boolean enhanced) { this.bmEnhanced = enhanced; }
    public boolean isPowerOfNatureActive() { return powerOfNatureActive; }
    public void setPowerOfNatureActive(boolean active) { this.powerOfNatureActive = active; }
    public int getTurtleShellTicks() { return turtleShellTicks; }
    public void setTurtleShellTicks(int ticks) { this.turtleShellTicks = ticks; }
    public int getPhoenixLifestealHits() { return phoenixLifestealHits; }
    public void setPhoenixLifestealHits(int hits) { this.phoenixLifestealHits = hits; }
    public float getPhoenixLifestealPct() { return phoenixLifestealPct; }
    public void setPhoenixLifestealPct(float pct) { this.phoenixLifestealPct = pct; }

    // Tiger Claw delayed hits
    public int getTigerClawHitsLeft() { return tigerClawHitsLeft; }
    public void setTigerClawHitsLeft(int hits) { this.tigerClawHitsLeft = hits; }
    public int getTigerClawTargetId() { return tigerClawTargetId; }
    public void setTigerClawTargetId(int id) { this.tigerClawTargetId = id; }
    public float getTigerClawDmg() { return tigerClawDmg; }
    public void setTigerClawDmg(float dmg) { this.tigerClawDmg = dmg; }
    public int getTigerClawTimer() { return tigerClawTimer; }
    public void setTigerClawTimer(int ticks) { this.tigerClawTimer = ticks; }
    public boolean isTigerClawEnhanced() { return tigerClawEnhanced; }
    public void setTigerClawEnhanced(boolean enhanced) { this.tigerClawEnhanced = enhanced; }
    public boolean isPhoenixLifestealEnhanced() { return phoenixLifestealEnhanced; }
    public void setPhoenixLifestealEnhanced(boolean enhanced) { this.phoenixLifestealEnhanced = enhanced; }

    // Warrior: Slash Blast
    public boolean isSlashBlastActive() { return slashBlastActive; }
    public void setSlashBlastActive(boolean active) { this.slashBlastActive = active; }
    public float getSlashBlastPct() { return slashBlastPct; }
    public void setSlashBlastPct(float pct) { this.slashBlastPct = pct; }
    public int getSlashBlastTicks() { return slashBlastTicks; }
    public void setSlashBlastTicks(int ticks) { this.slashBlastTicks = ticks; }
    // Warrior: War Cry
    public int getWarCryTicks() { return warCryTicks; }
    public void setWarCryTicks(int ticks) { this.warCryTicks = ticks; }
    public float getWarCryAtkBonus() { return warCryAtkBonus; }
    public void setWarCryAtkBonus(float bonus) { this.warCryAtkBonus = bonus; }
    // Warrior: Spirit Blade
    public int getSpiritBladeTicks() { return spiritBladeTicks; }
    public void setSpiritBladeTicks(int ticks) { this.spiritBladeTicks = ticks; }
    public float getSpiritBladeAtk() { return spiritBladeAtk; }
    public void setSpiritBladeAtk(float atk) { this.spiritBladeAtk = atk; }
    public boolean isSpiritBladeDefActive() { return spiritBladeDefActive; }
    public void setSpiritBladeDefActive(boolean active) { this.spiritBladeDefActive = active; }
    // Warrior: Unbreakable passive cooldown
    public int getUnbreakableCooldown() { return unbreakableCooldown; }
    public void setUnbreakableCooldown(int ticks) { this.unbreakableCooldown = ticks; }

    // Limitless: Black Flash
    public boolean isBlackFlashActive() { return blackFlashActive; }
    public void setBlackFlashActive(boolean active) { this.blackFlashActive = active; }
    public float getBlackFlashMultiplier() { return blackFlashMultiplier; }
    public void setBlackFlashMultiplier(float mult) { this.blackFlashMultiplier = mult; }
    public int getBlackFlashTicks() { return blackFlashTicks; }
    public void setBlackFlashTicks(int ticks) { this.blackFlashTicks = ticks; }
    // Limitless: Blue channel
    public boolean isBlueChanneling() { return blueChanneling; }
    public void setBlueChanneling(boolean active) { this.blueChanneling = active; }
    public int getBlueChannelTicks() { return blueChannelTicks; }
    public void setBlueChannelTicks(int ticks) { this.blueChannelTicks = ticks; }
    public int getBlueDrainTimer() { return blueDrainTimer; }
    public void setBlueDrainTimer(int ticks) { this.blueDrainTimer = ticks; }
    // Limitless: Red channel
    public boolean isRedChanneling() { return redChanneling; }
    public void setRedChanneling(boolean active) { this.redChanneling = active; }
    public int getRedChannelTicks() { return redChannelTicks; }
    public void setRedChannelTicks(int ticks) { this.redChannelTicks = ticks; }
    public int getRedDrainTimer() { return redDrainTimer; }
    public void setRedDrainTimer(int ticks) { this.redDrainTimer = ticks; }
    // Limitless: Purple channel
    public boolean isPurpleChanneling() { return purpleChanneling; }
    public void setPurpleChanneling(boolean active) { this.purpleChanneling = active; }
    public int getPurpleChannelTicks() { return purpleChannelTicks; }
    public void setPurpleChannelTicks(int ticks) { this.purpleChannelTicks = ticks; }
    // Generic channeling bar
    public int getChannelTicks() { return channelTicks; }
    public void setChannelTicks(int ticks) { this.channelTicks = ticks; }
    public int getChannelMaxTicks() { return channelMaxTicks; }
    public void setChannelMaxTicks(int ticks) { this.channelMaxTicks = ticks; }
    public String getChannelSkillName() { return channelSkillName; }
    public void setChannelSkillName(String name) { this.channelSkillName = name != null ? name : ""; }

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
        infinityStacks = 0;
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
        fervorTicks = 0;
        substitutionTicks = 0;
        sageModeDrainTimer = 0;
        gatesInvulnCooldown = 0;
        flyingRaijinPhase = 0;
        flyingRaijinTargetId = -1;
        flyingRaijinKunaiId = -1;
        frgPhase = 0;
        frgTicks = 0;
        frgKunaiId = -1;
        rasenganBuffActive = false;
        rasenganBuffTicks = 0;
        ssrzPhase = 0;
        ssrzTargetId = -1;
        ssrzMarkTicks = 0;
        bmActiveBuff = null;
        bmBuffTicks = 0;
        bmEnhanced = false;
        powerOfNatureActive = false;
        turtleShellTicks = 0;
        phoenixLifestealHits = 0;
        phoenixLifestealPct = 0;
        tigerClawHitsLeft = 0;
        tigerClawTargetId = -1;
        tigerClawDmg = 0;
        tigerClawTimer = 0;
        tigerClawEnhanced = false;
        phoenixLifestealEnhanced = false;
        slashBlastActive = false;
        slashBlastPct = 0;
        slashBlastTicks = 0;
        warCryTicks = 0;
        warCryAtkBonus = 0;
        spiritBladeTicks = 0;
        spiritBladeAtk = 0;
        spiritBladeDefActive = false;
        unbreakableCooldown = 0;
        blackFlashActive = false;
        blackFlashMultiplier = 0;
        blackFlashTicks = 0;
        blueChanneling = false;
        blueChannelTicks = 0;
        blueDrainTimer = 0;
        redChanneling = false;
        redChannelTicks = 0;
        redDrainTimer = 0;
        purpleChanneling = false;
        purpleChannelTicks = 0;
        channelTicks = 0;
        channelMaxTicks = 0;
        channelSkillName = "";
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
            System.arraycopy(saved, 0, tierSP, 0, Math.min(saved.length, tierSP.length));
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
        System.arraycopy(other.tierSP, 0, this.tierSP, 0, tierSP.length);
        this.selectedClass = other.selectedClass;
        this.skillLevels.clear();
        this.skillLevels.putAll(other.skillLevels);
        for (int i = 0; i < MAX_SLOTS; i++) {
            this.equippedSlots[i] = other.equippedSlots[i];
        }
        clearTransient();
    }
}
