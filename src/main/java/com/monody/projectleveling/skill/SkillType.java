package com.monody.projectleveling.skill;

import java.util.HashMap;
import java.util.Map;

public enum SkillType {
    // =====================================================
    // Tier 0 (Novice) — Universal, available from level 1
    // =====================================================
    DASH("dash", "Dash", 5, 0, null, 0, false, false,
            18, 10, 200, 80,
            "Burst forward with a speed boost.", "DSH"),
    ENDURANCE("endurance", "Endurance", 5, 0, null, 0, false, true,
            0, 0, 0, 0,
            "Passive. +2% max HP and +5% HP regen per level.", "END"),

    // =====================================================
    // Tier 1 — Class skills, requires player level 10
    // =====================================================

    // --- Warrior (STR) ---
    BLOODLUST("bloodlust", "Bloodlust", 10, 1, PlayerClass.WARRIOR, 10, false, false,
            35, 18, 500, 200,
            "Unleash an aura that slows and weakens nearby enemies.", "BL"),
    WAR_CRY("war_cry", "War Cry", 10, 1, PlayerClass.WARRIOR, 10, false, false,
            30, 15, 300, 150,
            "Shout that buffs ATK for self and nearby players. Weakens mobs.", "WC"),
    WEAPON_MASTERY("weapon_mastery", "Weapon Mastery", 10, 1, PlayerClass.WARRIOR, 10, false, true,
            0, 0, 0, 0,
            "Passive. +1% melee damage and +5% knockback resistance per level.", "WM"),

    // --- Assassin (LUK) ---
    SHADOW_STRIKE("shadow_strike", "Shadow Strike", 10, 1, PlayerClass.ASSASSIN, 10, false, false,
            30, 15, 280, 100,
            "Empower your next attack with bonus shadow damage.", "SS"),
    VENOM("venom", "Venom", 10, 1, PlayerClass.ASSASSIN, 10, false, false,
            25, 12, 300, 150,
            "Coat weapon in poison. Attacks apply stacking poison DoT.", "VN"),
    CRITICAL_EDGE("critical_edge", "Critical Edge", 10, 1, PlayerClass.ASSASSIN, 10, false, true,
            0, 0, 0, 0,
            "Passive. +1% crit rate and +2% crit damage per level.", "CE"),

    // --- Archer (DEX) ---
    ARROW_RAIN("arrow_rain", "Arrow Rain", 10, 1, PlayerClass.ARCHER, 10, false, false,
            32, 16, 240, 120,
            "Fire a volley of arrows from the sky in a target area.", "AR"),
    SOUL_ARROW("soul_arrow", "Soul Arrow", 10, 1, PlayerClass.ARCHER, 10, true, false,
            6, 3, 0, 0,
            "Toggle. Bow attacks don't consume arrows. +5-15% projectile damage.", "SA"),
    SHARP_EYES("sharp_eyes", "Sharp Eyes", 10, 1, PlayerClass.ARCHER, 10, false, true,
            0, 0, 0, 0,
            "Passive. +1.5% crit rate, +5% crit dmg per level. +1 proj range per 2 levels.", "SE"),

    // --- Healer (INT) ---
    HOLY_LIGHT("holy_light", "Holy Light", 10, 1, PlayerClass.HEALER, 10, false, false,
            28, 14, 160, 80,
            "Heal self and nearby players. Damages undead mobs.", "HL"),
    BLESS("bless", "Bless", 10, 1, PlayerClass.HEALER, 10, false, false,
            35, 18, 900, 450,
            "Buff ATK, DEF, and Regen for self and nearby players.", "BLS"),
    MP_RECOVERY("mp_recovery", "MP Recovery", 10, 1, PlayerClass.HEALER, 10, false, true,
            0, 0, 0, 0,
            "Passive. +0.2% MP regen per level. Restore MP on kills.", "MR"),

    // --- Mage (INT) ---
    FLAME_ORB("flame_orb", "Flame Orb", 10, 1, PlayerClass.MAGE, 10, false, false,
            22, 11, 120, 60,
            "Launch a fireball that explodes on impact. Leaves fire patch.", "FO"),
    MAGIC_GUARD("magic_guard", "Magic Guard", 10, 1, PlayerClass.MAGE, 10, true, false,
            8, 4, 0, 0,
            "Toggle. Redirect 30-60% of incoming damage to MP.", "MG"),
    ELEMENTAL_DRAIN("elemental_drain", "Elemental Drain", 10, 1, PlayerClass.MAGE, 10, false, true,
            0, 0, 0, 0,
            "Passive. +5% damage per active debuff on target (max +25%).", "ED"),

    // --- Necromancer (INT + Mind) ---
    LIFE_DRAIN("life_drain", "Life Drain", 10, 1, PlayerClass.NECROMANCER, 10, false, false,
            25, 15, 300, 160,
            "Drain life from nearby enemies, healing yourself.", "LD"),
    RAISE_SKELETON("raise_skeleton", "Raise Skeleton", 10, 1, PlayerClass.NECROMANCER, 10, true, false,
            8, 4, 0, 0,
            "Toggle. Summon a skeleton minion that fights for you.", "RS"),
    DARK_PACT("dark_pact", "Dark Pact", 10, 1, PlayerClass.NECROMANCER, 10, false, true,
            0, 0, 0, 0,
            "Passive. +2% max MP and +1.5% summon damage per level.", "DkP"),
    UNHOLY_FERVOR("unholy_fervor", "Unholy Fervor", 10, 1, PlayerClass.NECROMANCER, 10, false, false,
            30, 15, 400, 200,
            "Buff all skeleton minions with speed and damage boost.", "UF"),

    // =====================================================
    // Tier 2 — Class skills, requires player level 30
    // =====================================================

    // --- Warrior ---
    IRON_WILL("iron_will", "Iron Will", 15, 2, PlayerClass.WARRIOR, 30, true, false,
            8, 3, 40, 20,
            "Toggle. Grants damage resistance and knockback immunity.", "IW"),
    GROUND_SLAM("ground_slam", "Ground Slam", 15, 2, PlayerClass.WARRIOR, 30, false, false,
            40, 20, 400, 200,
            "Slam the ground. AoE damage + stun in radius.", "GS"),
    RAGE("rage", "Rage", 15, 2, PlayerClass.WARRIOR, 30, false, true,
            0, 0, 0, 0,
            "Passive. +2% damage per level when below 50% HP. +0.5% lifesteal.", "RG"),

    // --- Assassin ---
    STEALTH("stealth", "Stealth", 15, 2, PlayerClass.ASSASSIN, 30, true, false,
            10, 3, 120, 60,
            "Toggle. Become invisible. Mobs ignore you beyond detection range.", "STH"),
    BLADE_FURY("blade_fury", "Blade Fury", 15, 2, PlayerClass.ASSASSIN, 30, false, false,
            28, 14, 160, 80,
            "360 degree spin attack hitting all nearby mobs.", "BF"),
    EVASION("evasion", "Evasion", 15, 2, PlayerClass.ASSASSIN, 30, false, true,
            0, 0, 0, 0,
            "Passive. 2% dodge per level. Dodge guarantees next crit.", "EV"),

    // --- Archer ---
    ARROW_BOMB("arrow_bomb", "Arrow Bomb", 15, 2, PlayerClass.ARCHER, 30, false, false,
            30, 15, 200, 100,
            "Explosive arrow. AoE damage + stun on impact.", "AB"),
    COVERING_FIRE("covering_fire", "Covering Fire", 15, 2, PlayerClass.ARCHER, 30, false, false,
            25, 12, 300, 150,
            "Leap backward while firing arrows forward.", "CF"),
    EVASION_BOOST("evasion_boost", "Evasion Boost", 15, 2, PlayerClass.ARCHER, 30, false, true,
            0, 0, 0, 0,
            "Passive. 2% dodge per level. Dodge guarantees next arrow crits.", "EB"),

    // --- Healer ---
    HOLY_SHELL("holy_shell", "Holy Shell", 15, 2, PlayerClass.HEALER, 30, false, false,
            50, 25, 1200, 600,
            "Grant absorption shield to self and nearby players. Cleanses debuffs.", "HS"),
    DISPEL("dispel", "Dispel", 15, 2, PlayerClass.HEALER, 30, false, false,
            30, 15, 600, 300,
            "Remove negative effects from allies. Remove buffs from enemies.", "DSP"),
    DIVINE_PROTECTION("divine_protection", "Divine Protection", 15, 2, PlayerClass.HEALER, 30, false, true,
            0, 0, 0, 0,
            "Passive. +3% status resistance per level. Auto-cleanse chance.", "DP"),

    // --- Mage ---
    FROST_BIND("frost_bind", "Frost Bind", 15, 2, PlayerClass.MAGE, 30, false, false,
            40, 20, 400, 200,
            "Frost wave pulls mobs inward and freezes them. Frozen take +25% damage.", "FrB"),
    POISON_MIST("poison_mist", "Poison Mist", 15, 2, PlayerClass.MAGE, 30, false, false,
            35, 18, 300, 150,
            "Place a poison cloud zone. Can be detonated by Mist Eruption.", "PM"),
    ELEMENT_AMPLIFICATION("element_amplification", "Element Amplification", 15, 2, PlayerClass.MAGE, 30, false, true,
            0, 0, 0, 0,
            "Passive. +3% skill damage per level. +20% MP cost for all skills.", "EA"),

    // --- Necromancer T2 ---
    BONE_SHIELD("bone_shield", "Bone Shield", 15, 2, PlayerClass.NECROMANCER, 30, true, false,
            10, 5, 0, 0,
            "Toggle. Reduce incoming damage by 10-25%.", "BnS"),
    CORPSE_EXPLOSION("corpse_explosion", "Corpse Explosion", 15, 2, PlayerClass.NECROMANCER, 30, false, false,
            40, 20, 400, 200,
            "Dark AoE explosion. Bonus damage if skeleton is alive.", "CEx"),
    SOUL_SIPHON("soul_siphon", "Soul Siphon", 15, 2, PlayerClass.NECROMANCER, 30, false, true,
            0, 0, 0, 0,
            "Passive. Restore HP and MP on kill.", "Si"),
    SKELETAL_MASTERY("skeletal_mastery", "Skeletal Mastery", 15, 2, PlayerClass.NECROMANCER, 30, false, true,
            0, 0, 0, 0,
            "Passive. Minions gain lifesteal and take reduced damage.", "SM"),

    // =====================================================
    // Tier 3 — Class skills, requires player level 60
    // =====================================================

    // --- Warrior ---
    DOMAIN_OF_MONARCH("domain_of_monarch", "Domain of the Monarch", 20, 3, PlayerClass.WARRIOR, 60, false, false,
            70, 35, 1400, 600,
            "Create a zone of power that damages all enemies within.", "DM"),
    UNBREAKABLE("unbreakable", "Unbreakable", 20, 3, PlayerClass.WARRIOR, 60, false, false,
            80, 40, 2400, 1600,
            "Brief invulnerability. Clears debuffs and burst heals.", "UB"),
    BERSERKER_SPIRIT("berserker_spirit", "Berserker Spirit", 20, 3, PlayerClass.WARRIOR, 60, false, true,
            0, 0, 0, 0,
            "Passive. +1% crit rate per level. Chance for double strike. Lifesteal.", "BS"),

    // --- Assassin ---
    RULERS_AUTHORITY("rulers_authority", "Ruler's Authority", 20, 3, PlayerClass.ASSASSIN, 60, false, false,
            45, 20, 500, 160,
            "Pull nearby enemies toward you with telekinetic force.", "RA"),
    SHADOW_PARTNER("shadow_partner", "Shadow Partner", 20, 3, PlayerClass.ASSASSIN, 60, true, false,
            12, 5, 0, 0,
            "Toggle. Shadow clone mirrors attacks at 30-50% damage.", "SP"),
    FATAL_BLOW("fatal_blow", "Fatal Blow", 20, 3, PlayerClass.ASSASSIN, 60, false, true,
            0, 0, 0, 0,
            "Passive. +2% damage per level vs low HP mobs. Execute chance.", "FB"),

    // --- Archer ---
    PHOENIX("phoenix", "Phoenix", 20, 3, PlayerClass.ARCHER, 60, false, false,
            60, 30, 1800, 900,
            "Summon a fire bird. Auto-attacks with stun. Grants damage resistance.", "PX"),
    HURRICANE("hurricane", "Hurricane", 20, 3, PlayerClass.ARCHER, 60, true, false,
            15, 6, 0, 0,
            "Toggle. Rapid-fire arrow stream at nearest mob. Slows movement.", "HC"),
    MORTAL_BLOW("mortal_blow", "Mortal Blow", 20, 3, PlayerClass.ARCHER, 60, false, true,
            0, 0, 0, 0,
            "Passive. +2% damage vs low HP mobs. Execute chance on projectiles.", "MB"),

    // --- Healer ---
    BENEDICTION("benediction", "Benediction", 20, 3, PlayerClass.HEALER, 60, false, false,
            70, 35, 1800, 900,
            "Place holy zone. Allies gain Regen + damage boost. Enemies take damage.", "BN"),
    ANGEL_RAY("angel_ray", "Angel Ray", 20, 3, PlayerClass.HEALER, 60, false, false,
            20, 10, 200, 100,
            "Holy beam that damages mobs and heals nearby allies.", "ARL"),
    BLESSED_ENSEMBLE("blessed_ensemble", "Blessed Ensemble", 20, 3, PlayerClass.HEALER, 60, false, true,
            0, 0, 0, 0,
            "Passive. +3% damage per nearby player. +5% XP per nearby player.", "BE"),

    // --- Mage ---
    MIST_ERUPTION("mist_eruption", "Mist Eruption", 20, 3, PlayerClass.MAGE, 60, false, false,
            60, 30, 900, 450,
            "Detonate Poison Mist for massive burst damage.", "ME"),
    INFINITY("infinity", "Infinity", 20, 3, PlayerClass.MAGE, 60, false, false,
            80, 40, 2400, 1200,
            "All skills cost 0 MP. +5% damage every 4s. Lasts 20-40s.", "INF"),
    ARCANE_OVERDRIVE("arcane_overdrive", "Arcane Overdrive", 20, 3, PlayerClass.MAGE, 60, false, true,
            0, 0, 0, 0,
            "Passive. +1.5% crit rate, +1% crit damage, +1% armor pen per level.", "AO"),

    // --- Necromancer T3 ---
    ARMY_OF_THE_DEAD("army_of_the_dead", "Army of the Dead", 20, 3, PlayerClass.NECROMANCER, 60, false, false,
            80, 40, 1200, 600,
            "Summon an undead horde zone that damages enemies.", "AD"),
    DEATH_MARK("death_mark", "Death Mark", 20, 3, PlayerClass.NECROMANCER, 60, false, false,
            50, 25, 600, 300,
            "Mark an enemy for death. DoT + AoE explosion on death.", "DkM"),
    UNDYING_WILL("undying_will", "Undying Will", 20, 3, PlayerClass.NECROMANCER, 60, false, true,
            0, 0, 0, 0,
            "Passive. Revive on fatal damage. +2% minion HP per level.", "UW"),
    SOUL_LINK("soul_link", "Soul Link", 20, 3, PlayerClass.NECROMANCER, 60, true, false,
            12, 6, 0, 0,
            "Toggle. Redirect damage to nearest minion. Minions deal bonus damage.", "SL"),

    // =====================================================
    // Hidden — Reserved / Legacy
    // =====================================================
    VITAL_SURGE("vital_surge", "Vital Surge", 15, 99, null, 999, false, false,
            50, 25, 700, 300,
            "Heal yourself and cleanse poison and wither effects.", "VS");

    private static final Map<String, SkillType> BY_ID = new HashMap<>();

    static {
        for (SkillType type : values()) {
            BY_ID.put(type.id, type);
        }
    }

    private final String id;
    private final String displayName;
    private final int maxLevel;
    private final int tier;
    private final PlayerClass requiredClass; // null = universal
    private final int requiredPlayerLevel;
    private final boolean toggle;
    private final boolean passive;
    private final int baseMpCost;
    private final int minMpCost;
    private final int baseCooldown;
    private final int minCooldown;
    private final String description;
    private final String abbreviation;

    SkillType(String id, String displayName, int maxLevel, int tier,
              PlayerClass requiredClass, int requiredPlayerLevel, boolean toggle, boolean passive,
              int baseMpCost, int minMpCost, int baseCooldown, int minCooldown,
              String description, String abbreviation) {
        this.id = id;
        this.displayName = displayName;
        this.maxLevel = maxLevel;
        this.tier = tier;
        this.requiredClass = requiredClass;
        this.requiredPlayerLevel = requiredPlayerLevel;
        this.toggle = toggle;
        this.passive = passive;
        this.baseMpCost = baseMpCost;
        this.minMpCost = minMpCost;
        this.baseCooldown = baseCooldown;
        this.minCooldown = minCooldown;
        this.description = description;
        this.abbreviation = abbreviation;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getMaxLevel() { return maxLevel; }
    public int getTier() { return tier; }
    public PlayerClass getRequiredClass() { return requiredClass; }
    public int getRequiredPlayerLevel() { return requiredPlayerLevel; }
    public boolean isToggle() { return toggle; }
    public boolean isPassive() { return passive; }
    public String getDescription() { return description; }
    public String getAbbreviation() { return abbreviation; }

    /** Whether this skill is visible (not hidden/reserved). */
    public boolean isAvailable() { return tier <= 3; }

    /** Get MP cost for the given skill level (1-based). Linearly scales from base to min. */
    public int getMpCost(int level) {
        if (passive) return 0;
        if (maxLevel <= 1) return baseMpCost;
        return baseMpCost - (level - 1) * (baseMpCost - minMpCost) / (maxLevel - 1);
    }

    /** Get cooldown in ticks for the given skill level (1-based). Linearly scales from base to min. */
    public int getCooldownTicks(int level) {
        if (passive) return 0;
        if (maxLevel <= 1) return baseCooldown;
        return baseCooldown - (level - 1) * (baseCooldown - minCooldown) / (maxLevel - 1);
    }

    /** MP cost per second for toggle skills. Uses same formula as getMpCost. */
    public int getToggleMpPerSecond(int level) {
        return getMpCost(level);
    }

    public static SkillType fromId(String id) {
        return BY_ID.get(id);
    }
}
