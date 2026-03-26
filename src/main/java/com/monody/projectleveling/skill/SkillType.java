package com.monody.projectleveling.skill;

import com.monody.projectleveling.skill.StatContribRegistry.StatDef;
import static com.monody.projectleveling.skill.StatContribRegistry.StatLine.*;
import static com.monody.projectleveling.skill.StatContribRegistry.stat;
import static com.monody.projectleveling.skill.StatContribRegistry.statTag;

import java.util.HashMap;
import java.util.Map;

public enum SkillType {
    // =====================================================
    // Tier 0 (Novice) — Universal, available from level 1
    // =====================================================
    DASH("dash", "Dash", 5, 0, null, 0, false, false,
            14, 10, 200, 80,
            "Burst forward with a speed boost.", "DSH"),
    ENDURANCE("endurance", "Endurance", 5, 0, null, 0, false, true,
            0, 0, 0, 0,
            "Passive. +2% max HP and +5% HP regen per level.", "END",
            stat(HP_PCT, 2.0)),

    // =====================================================
    // Tier 1 — Class skills, requires player level 10
    // =====================================================

    // --- Warrior (STR) ---
    SLASH_BLAST("slash_blast", "Slash Blast", 10, 1, PlayerClass.WARRIOR, 10, false, false,
            18, 14, 160, 80,
            "Enhance next melee to deal bonus damage. Hits up to 3 nearby enemies.", "SB"),
    WAR_CRY("war_cry", "War Cry", 10, 1, PlayerClass.WARRIOR, 10, false, false,
            22, 16, 400, 200,
            "Buff ATK by %. Pull aggro from nearby mobs. Range scales with VIT.", "WC"),
    WARRIOR_MASTERY("warrior_mastery", "Warrior Mastery", 10, 1, PlayerClass.WARRIOR, 10, false, true,
            0, 0, 0, 0,
            "Passive. +2% max HP, -1% damage taken, +Knockback Resist per level.", "WM",
            stat(HP_PCT, 2.0), stat(DMG_RED, 1.0)),

    // --- Assassin (LUK) ---
    SHADOW_STRIKE("shadow_strike", "Shadow Strike", 10, 1, PlayerClass.ASSASSIN, 10, false, false,
            22, 16, 280, 100,
            "Empower your next attack with bonus shadow damage.", "SS"),
    VENOM("venom", "Bleeding Edge", 10, 1, PlayerClass.ASSASSIN, 10, false, true,
            0, 0, 0, 0,
            "Passive. Attacks have a chance to cause bleeding. Stacks up to 10.", "BE"),
    CRITICAL_EDGE("critical_edge", "Critical Edge", 10, 1, PlayerClass.ASSASSIN, 10, false, true,
            0, 0, 0, 0,
            "Passive. +1% crit rate and +2% crit damage per level.", "CE",
            stat(CRIT, 1.0), stat(CDMG, 2.0)),

    // --- Archer (DEX) ---
    ARROW_RAIN("arrow_rain", "Arrow Rain", 10, 1, PlayerClass.ARCHER, 10, false, false,
            22, 16, 240, 120,
            "Fire a volley of arrows from the sky in a target area.", "AR"),
    SOUL_ARROW("soul_arrow", "Soul Arrow", 10, 1, PlayerClass.ARCHER, 10, true, false,
            20, 10, 0, 0,
            "Toggle. Bow attacks don't consume arrows. +5-15% projectile damage.", "SA"),
    SHARP_EYES("sharp_eyes", "Sharp Eyes", 10, 1, PlayerClass.ARCHER, 10, false, true,
            0, 0, 0, 0,
            "Passive. +1.5% crit rate, +5% crit dmg per level. +1 proj range per 2 levels.", "SE",
            stat(CRIT, 1.5), stat(CDMG, 5.0)),

    // --- Healer (INT) ---
    HOLY_LIGHT("holy_light", "Holy Light", 10, 1, PlayerClass.HEALER, 10, false, false,
            20, 15, 160, 80,
            "Heal self and nearby players. Damages undead mobs.", "HL"),
    BLESS("bless", "Bless", 10, 1, PlayerClass.HEALER, 10, false, false,
            25, 18, 900, 450,
            "Buff ATK, DEF, and Regen for self and nearby players.", "BLS"),
    MP_RECOVERY("mp_recovery", "MP Recovery", 10, 1, PlayerClass.HEALER, 10, false, true,
            0, 0, 0, 0,
            "Passive. +0.2% MP regen per level. Restore MP on kills.", "MR"),

    // --- Mage (INT) ---
    FLAME_ORB("flame_orb", "Flame Orb", 10, 1, PlayerClass.MAGE, 10, false, false,
            16, 12, 140, 100,
            "Launch a fireball that explodes on impact. Leaves fire patch.", "FO"),
    MAGIC_GUARD("magic_guard", "Magic Guard", 10, 1, PlayerClass.MAGE, 10, true, false,
            30, 15, 0, 0,
            "Toggle. Redirect 30-60% of incoming damage to MP.", "MG"),
    ELEMENTAL_DRAIN("elemental_drain", "Elemental Drain", 10, 1, PlayerClass.MAGE, 10, false, true,
            0, 0, 0, 0,
            "Passive. +5% damage per active debuff on target (max +25%).", "ED"),

    // --- Ninja (AGI + LUK) ---
    SHURIKEN_JUTSU("shuriken_jutsu", "Shuriken Jutsu", 10, 1, PlayerClass.NINJA, 10, false, false,
            16, 12, 200, 100,
            "Throw shurikens in a cone, hitting all enemies in front.", "SJu"),
    SUBSTITUTION_JUTSU("substitution_jutsu", "Substitution Jutsu", 10, 1, PlayerClass.NINJA, 10, false, false,
            18, 14, 300, 160,
            "Dodge buffer. First damage is negated; teleport behind attacker.", "SUB"),
    KUNAI_MASTERY("kunai_mastery", "Kunai Mastery", 10, 1, PlayerClass.NINJA, 10, false, true,
            0, 0, 0, 0,
            "Passive. +AGI melee damage, +LUK crit rate, +1.5% projectile damage per level.", "KM",
            stat(PROJ, 1.5)),

    // --- Necromancer (INT + Mind) ---
    LIFE_DRAIN("life_drain", "Life Drain", 10, 1, PlayerClass.NECROMANCER, 10, false, false,
            20, 15, 300, 160,
            "Drain life from nearby enemies, healing yourself.", "LD"),
    RAISE_SKELETON("raise_skeleton", "Raise Skeleton", 10, 1, PlayerClass.NECROMANCER, 10, true, false,
            25, 15, 0, 0,
            "Toggle. Summon a skeleton minion that fights for you.", "RS"),
    DARK_PACT("dark_pact", "Dark Pact", 10, 1, PlayerClass.NECROMANCER, 10, false, true,
            0, 0, 0, 0,
            "Passive. +2% max MP and +1.5% summon damage per level.", "DkP"),
    UNHOLY_FERVOR("unholy_fervor", "Unholy Fervor", 10, 1, PlayerClass.NECROMANCER, 10, false, false,
            22, 16, 400, 200,
            "Buff all skeleton minions with speed and damage boost.", "UF"),

    // --- Beast Master (STR/VIT) ---
    TIGER_CLAW("tiger_claw", "Tiger Claw", 10, 1, PlayerClass.BEAST_MASTER, 10, false, false,
            18, 14, 80, 80,
            "Next melee hits extra times at reduced damage. Bypasses iframe.", "TC"),
    TURTLE_SHELL("turtle_shell", "Turtle Shell", 10, 1, PlayerClass.BEAST_MASTER, 10, false, false,
            16, 12, 80, 80,
            "Gain max HP % as absorption shield for 5s.", "TS"),
    BEAR_PAW("bear_paw", "Bear Paw", 10, 1, PlayerClass.BEAST_MASTER, 10, false, false,
            18, 14, 80, 80,
            "Next melee stuns. Applies Weakness, Darkness, Slowness.", "BP"),
    PHOENIX_WINGS("phoenix_wings", "Phoenix Wings", 10, 1, PlayerClass.BEAST_MASTER, 10, false, false,
            22, 16, 80, 80,
            "Burst heal + Regen + lifesteal for next 2 melee hits.", "PW"),
    POWER_OF_NATURE("power_of_nature", "Power of Nature", 10, 1, PlayerClass.BEAST_MASTER, 10, false, false,
            30, 22, 1200, 1200,
            "Doubles next skill effect. CD reduced 2s per basic skill use.", "PoN"),

    // --- Beast Master T2 (passives) ---
    TIGER_CLAW_MASTERY("tiger_claw_mastery", "Tiger Claw Mastery", 10, 2, PlayerClass.BEAST_MASTER, 30, false, true,
            0, 0, 0, 0,
            "Passive. +1 Tiger Claw max hit. +Tiger Claw final damage.", "TCM"),
    TURTLE_SHELL_MASTERY("turtle_shell_mastery", "Turtle Shell Mastery", 10, 2, PlayerClass.BEAST_MASTER, 30, false, true,
            0, 0, 0, 0,
            "Passive. +0.5% shield per level.", "TSM"),
    BEAR_PAW_MASTERY("bear_paw_mastery", "Bear Paw Mastery", 10, 2, PlayerClass.BEAST_MASTER, 30, false, true,
            0, 0, 0, 0,
            "Passive. +0.1s stun per level. +Bear Paw final damage.", "BPM"),
    PHOENIX_WINGS_MASTERY("phoenix_wings_mastery", "Phoenix Wings Mastery", 10, 2, PlayerClass.BEAST_MASTER, 30, false, true,
            0, 0, 0, 0,
            "Passive. +0.5% healing per level. Max level: +1 lifesteal hit.", "PWM"),
    PREDATOR_INSTINCT("predator_instinct", "Predator Instinct", 10, 2, PlayerClass.BEAST_MASTER, 30, false, true,
            0, 0, 0, 0,
            "Passive. +1.5% crit rate and +2% crit damage per level. Crits grant Speed.", "PI"),

    // =====================================================
    // Tier 2 — Class skills, requires player level 30
    // =====================================================

    // --- Warrior ---
    SPIRIT_BLADE("spirit_blade", "Spirit Blade", 15, 2, PlayerClass.WARRIOR, 30, false, false,
            30, 22, 900, 900,
            "Party buff. Flat +ATK for 30s. Self: -Damage Taken.", "SpB"),
    GROUND_SLAM("ground_slam", "Ground Slam", 15, 2, PlayerClass.WARRIOR, 30, false, false,
            30, 22, 280, 140,
            "Slam the ground. AoE damage + stun in radius.", "GS"),
    FINAL_ATTACK("final_attack", "Final Attack", 15, 2, PlayerClass.WARRIOR, 30, false, true,
            0, 0, 0, 0,
            "Passive. Chance to repeat hit at 50% damage.", "FA"),

    // --- Assassin ---
    STEALTH("stealth", "Stealth", 15, 2, PlayerClass.ASSASSIN, 30, true, false,
            35, 20, 120, 60,
            "Toggle. Become invisible. Mobs ignore you beyond detection range.", "STH"),
    BLADE_FURY("blade_fury", "Blade Fury", 15, 2, PlayerClass.ASSASSIN, 30, false, false,
            24, 18, 160, 80,
            "360 degree spin attack hitting all nearby mobs.", "BF"),
    EVASION("evasion", "Evasion", 15, 2, PlayerClass.ASSASSIN, 30, false, true,
            0, 0, 0, 0,
            "Passive. 2% dodge per level. Dodge guarantees next crit.", "EV",
            stat(DODGE, 2.0)),

    // --- Archer ---
    ARROW_BOMB("arrow_bomb", "Arrow Bomb", 15, 2, PlayerClass.ARCHER, 30, false, false,
            25, 18, 200, 100,
            "Explosive arrow. AoE damage + stun on impact.", "AB"),
    COVERING_FIRE("covering_fire", "Covering Fire", 15, 2, PlayerClass.ARCHER, 30, false, false,
            22, 16, 300, 150,
            "Leap backward while firing arrows forward.", "CF"),
    EVASION_BOOST("evasion_boost", "Evasion Boost", 15, 2, PlayerClass.ARCHER, 30, false, true,
            0, 0, 0, 0,
            "Passive. 2% dodge per level. Dodge guarantees next arrow crits.", "EB",
            stat(DODGE, 2.0)),

    // --- Healer ---
    HOLY_SHELL("holy_shell", "Holy Shell", 15, 2, PlayerClass.HEALER, 30, false, false,
            38, 28, 1200, 600,
            "Grant absorption shield to self and nearby players. Cleanses debuffs.", "HS"),
    DISPEL("dispel", "Dispel", 15, 2, PlayerClass.HEALER, 30, false, false,
            24, 18, 600, 300,
            "Remove negative effects from allies. Remove buffs from enemies.", "DSP"),
    DIVINE_PROTECTION("divine_protection", "Divine Protection", 15, 2, PlayerClass.HEALER, 30, false, true,
            0, 0, 0, 0,
            "Passive. +3% status resistance per level. Auto-cleanse chance.", "DP"),
    HOLY_FERVOR("holy_fervor", "Holy Fervor", 15, 2, PlayerClass.HEALER, 30, false, true,
            0, 0, 0, 0,
            "Passive. +2% damage per level. +3% vs undead. +Crit rate/damage. MATK enhances skills.", "HF"),

    // --- Mage ---
    FROST_BIND("frost_bind", "Frost Bind", 15, 2, PlayerClass.MAGE, 30, false, false,
            30, 22, 400, 200,
            "Frost wave pulls mobs inward and freezes them. Frozen take +25% damage.", "FrB"),
    POISON_MIST("poison_mist", "Poison Mist", 15, 2, PlayerClass.MAGE, 30, false, false,
            28, 20, 300, 150,
            "Place a poison cloud zone. Can be detonated by Mist Eruption.", "PM"),
    ELEMENT_AMPLIFICATION("element_amplification", "Element Amplification", 15, 2, PlayerClass.MAGE, 30, false, true,
            0, 0, 0, 0,
            "Passive. +3% damage per level. +20% MP cost for all skills.", "EA",
            stat(DMG, 3.0)),

    // --- Ninja T2 ---
    SHADOW_CLONE("shadow_clone", "Shadow Clone", 15, 2, PlayerClass.NINJA, 30, true, false,
            30, 15, 0, 0,
            "Toggle. Summon shadow clones that fight and copy your skills.", "SC"),
    FLYING_RAIJIN("flying_raijin", "Flying Raijin", 15, 2, PlayerClass.NINJA, 30, false, false,
            24, 18, 300, 160,
            "Throw a kunai and teleport to it. Marks hit targets.", "FRJ"),
    FLYING_RAIJIN_GROUND("flying_raijin_ground", "Flying Raijin: Ground", 15, 2, PlayerClass.NINJA, 30, false, false,
            16, 12, 400, 200,
            "Place a kunai at your feet. Reuse to teleport back.", "FRG"),
    CHAKRA_CONTROL("chakra_control", "Chakra Control", 15, 2, PlayerClass.NINJA, 30, false, true,
            0, 0, 0, 0,
            "Passive. -1% MP cost per level. +0.15% MP regen per level.", "ChC"),

    // --- Necromancer T2 ---
    BONE_SHIELD("bone_shield", "Bone Shield", 15, 2, PlayerClass.NECROMANCER, 30, true, false,
            30, 15, 0, 0,
            "Toggle. Reduce incoming damage by 10-25%.", "BnS"),
    CORPSE_EXPLOSION("corpse_explosion", "Corpse Explosion", 15, 2, PlayerClass.NECROMANCER, 30, false, false,
            30, 22, 400, 200,
            "Dark AoE explosion. Bonus damage if skeleton is alive.", "CEx"),
    SOUL_SIPHON("soul_siphon", "Soul Siphon", 15, 2, PlayerClass.NECROMANCER, 30, false, true,
            0, 0, 0, 0,
            "Passive. Restore HP and MP on kill.", "Si"),
    SKELETAL_MASTERY("skeletal_mastery", "Skeletal Mastery", 15, 2, PlayerClass.NECROMANCER, 30, false, true,
            0, 0, 0, 0,
            "Passive. Minions gain lifesteal and take reduced damage.", "SM"),
    DARK_RESONANCE("dark_resonance", "Dark Resonance", 15, 2, PlayerClass.NECROMANCER, 30, false, true,
            0, 0, 0, 0,
            "Passive. +1% crit rate and +2% crit damage per level. Crits apply Wither.", "DR"),

    // =====================================================
    // Tier 3 — Class skills, requires player level 60
    // =====================================================

    // --- Warrior ---
    BEAM_BLADE("beam_blade", "Beam Blade", 20, 3, PlayerClass.WARRIOR, 60, false, false,
            30, 22, 80, 80,
            "Launch a piercing blade. Hits all enemies in a line.", "BB"),
    UNBREAKABLE("unbreakable", "Unbreakable", 20, 3, PlayerClass.WARRIOR, 60, false, true,
            0, 0, 0, 0,
            "Passive. Revive on fatal damage. 5 minute cooldown.", "UB"),
    BERSERKER_SPIRIT("berserker_spirit", "Berserker Spirit", 20, 3, PlayerClass.WARRIOR, 60, false, true,
            0, 0, 0, 0,
            "Passive. +1% crit rate per level. +Crit damage. +Lifesteal. +Final Attack trigger chance.", "BS",
            stat(CRIT, 1.0)),

    // --- Assassin ---
    SHADOW_SNEAK("shadow_sneak", "Shadow Sneak", 20, 3, PlayerClass.ASSASSIN, 60, false, false,
            38, 26, 500, 160,
            "Mark a target, then teleport behind it for a devastating strike.", "SS"),
    SHADOW_PARTNER("shadow_partner", "Shadow Partner", 20, 3, PlayerClass.ASSASSIN, 60, true, false,
            40, 20, 0, 0,
            "Toggle. Shadow clone mirrors attacks at 30-40% damage.", "SP"),
    FATAL_BLOW("fatal_blow", "Fatal Blow", 20, 3, PlayerClass.ASSASSIN, 60, false, true,
            0, 0, 0, 0,
            "Passive. +2% damage per level vs low HP mobs. Execute chance.", "FB",
            statTag(DMG, 2.0)),
    SHADOW_LEGION("shadow_legion", "Shadow Legion", 20, 3, PlayerClass.ASSASSIN, 60, false, true,
            0, 0, 0, 0,
            "Passive. 2nd Shadow Partner, auto-attacks, counter on dodge, reduced MP penalty.", "SL"),

    // --- Assassin T4 ---
    FINAL_BLOW("final_blow", "Final Blow", 25, 4, PlayerClass.ASSASSIN, 100, false, false,
            50, 30, 600, 300,
            "Buff next attack. Consumes bleed stacks for massive bonus damage.", "FLB"),
    LETHAL_MASTERY("lethal_mastery", "Lethal Mastery", 25, 4, PlayerClass.ASSASSIN, 100, false, true,
            0, 0, 0, 0,
            "Passive. +ATK% and +Bleed chance per level.", "LM"),

    // --- Archer ---
    PHOENIX("phoenix", "Phoenix", 20, 3, PlayerClass.ARCHER, 60, false, false,
            48, 35, 1800, 900,
            "Summon a fire bird. Auto-attacks with stun. Grants damage resistance.", "PX"),
    HURRICANE("hurricane", "Hurricane", 20, 3, PlayerClass.ARCHER, 60, true, false,
            40, 20, 0, 0,
            "Toggle. Rapid-fire arrow stream at nearest mob. Slows movement.", "HC"),
    MORTAL_BLOW("mortal_blow", "Mortal Blow", 20, 3, PlayerClass.ARCHER, 60, false, true,
            0, 0, 0, 0,
            "Passive. +2% damage vs low HP mobs. Execute chance on projectiles.", "MB",
            statTag(DMG, 2.0)),

    // --- Archer T4 ---
    STORM_OF_ARROWS("storm_of_arrows", "Storm of Arrows", 25, 4, PlayerClass.ARCHER, 100, false, false,
            60, 40, 1200, 1200,
            "Rain homing arrows on enemies every 5s for 30s. Bypasses I-frame.", "SOA"),

    // --- Archer T5 ---
    HAWK_EYE("hawk_eye", "Hawk Eye", 5, 4, PlayerClass.ARCHER, 120, false, true,
            0, 0, 0, 0,
            "Passive. +1% crit rate, +2% crit dmg per level.", "HE",
            stat(CRIT, 1.0), stat(CDMG, 2.0)),

    // --- Healer ---
    BENEDICTION("benediction", "Benediction", 20, 3, PlayerClass.HEALER, 60, false, false,
            55, 40, 1800, 900,
            "Place holy zone. Allies gain Regen + damage boost. Enemies take damage.", "BN"),
    ANGEL_RAY("angel_ray", "Angel Ray", 20, 3, PlayerClass.HEALER, 60, false, false,
            18, 14, 200, 100,
            "Holy beam that damages mobs and heals nearby allies.", "ARL"),
    BLESSED_ENSEMBLE("blessed_ensemble", "Blessed Ensemble", 20, 3, PlayerClass.HEALER, 60, false, true,
            0, 0, 0, 0,
            "Passive. +20% damage per nearby player (max 60%). +20% XP per nearby player (max 60%).", "BE"),

    // --- Healer T4 ---
    MAGIC_FINALE("magic_finale", "Magic: Finale", 25, 4, PlayerClass.HEALER, 100, false, false,
            80, 55, 1800, 1800,
            "Channel 5s. Draw magic circle, then massive beam from the sky.", "MF"),
    RIGHTEOUSLY_INDIGNANT("righteously_indignant", "Righteously Indignant", 25, 4, PlayerClass.HEALER, 100, true, false,
            20, 15, 0, 0,
            "Toggle. Convert all Heal Power to Magic Attack.", "RI"),

    // --- Mage ---
    MIST_ERUPTION("mist_eruption", "Mist Eruption", 20, 3, PlayerClass.MAGE, 60, false, false,
            48, 35, 900, 350,
            "Detonate Poison Mist for massive burst damage.", "ME"),
    ARCANE_INFINITY("arcane_infinity", "Arcane Infinity", 20, 3, PlayerClass.MAGE, 60, false, false,
            65, 45, 2400, 1200,
            "All skills cost 0 MP. +5% damage every 4s. Lasts 20-40s.", "AI"),
    ARCANE_OVERDRIVE("arcane_overdrive", "Arcane Overdrive", 20, 3, PlayerClass.MAGE, 60, false, true,
            0, 0, 0, 0,
            "Passive. +1.5% crit rate, +1% crit damage, +1% armor pen per level.", "AO",
            stat(CRIT, 1.5), stat(CDMG, 1.0)),

    // --- Mage T4 ---
    STAR_FALL("star_fall", "Star Fall", 25, 4, PlayerClass.MAGE, 100, false, false,
            70, 45, 1200, 1200,
            "Channel. Rain meteors on a zone for 10s. Cannot move while casting.", "SF"),
    ARCANE_POWER("arcane_power", "Arcane Power", 25, 4, PlayerClass.MAGE, 100, true, false,
            20, 20, 0, 0,
            "Toggle. Drain 2% max MP/s. +10% Magic Attack while active.", "AP"),

    // --- Ninja T3 ---
    RASENGAN("rasengan", "Rasengan", 20, 3, PlayerClass.NINJA, 60, false, false,
            70, 50, 600, 160,
            "Empower next attack with a spiraling explosion in 2-block radius.", "RSG"),
    SAGE_MODE("sage_mode", "Sage Mode", 20, 3, PlayerClass.NINJA, 60, true, false,
            30, 20, 0, 0,
            "Toggle. +2% dmg per level, +speed, knockback resist. Drains MP% per second.", "SgM"),
    EIGHT_INNER_GATES("eight_inner_gates", "Eight Inner Gates", 20, 3, PlayerClass.NINJA, 60, false, true,
            0, 0, 0, 0,
            "Passive. Below 30% HP: massive damage and speed boost.", "8G"),
    MULTI_SHADOW_CLONE("multi_shadow_clone", "Multi Shadow Clone", 10, 3, PlayerClass.NINJA, 60, false, true,
            0, 0, 0, 0,
            "Passive. +1-3 max clones. -4% max MP per level when clones active.", "MSC"),

    // --- Ninja T4 ---
    FLYING_RAIJIN_SSRZ("flying_raijin_ssrz", "FR: Shippu Senko Rennodan Zeroshiki", 25, 4, PlayerClass.NINJA, 100, false, false,
            60, 40, 1200, 1200,
            "Throw kunai. On hit: mark target, then 9 teleport strikes. 9th = massive finisher.", "SSRZ"),
    MASTERED_SAGE_MODE("mastered_sage_mode", "Mastered Sage Mode", 25, 4, PlayerClass.NINJA, 100, false, true,
            0, 0, 0, 0,
            "Passive. +0.4% Sage Mode damage, +0.4% ATK per level.", "MSM"),

    // --- Warrior T4 ---
    HEAVEN_SWORD("heaven_sword", "Heaven Sword", 25, 4, PlayerClass.WARRIOR, 100, false, false,
            50, 30, 600, 300,
            "Call a giant sword from the sky. AoE damage in 10 block radius.", "HS"),
    WARLORDS_COMMAND("warlords_command", "Warlord's Command", 25, 4, PlayerClass.WARRIOR, 100, false, true,
            0, 0, 0, 0,
            "Passive. Enhances War Cry: +ATK% bonus, increased aggro range with VIT.", "WLC"),

    // --- Necromancer T3 ---
    ARMY_OF_THE_DEAD("army_of_the_dead", "Army of the Dead", 20, 3, PlayerClass.NECROMANCER, 60, false, false,
            65, 45, 1200, 600,
            "Summon an undead horde zone that damages enemies.", "AD"),
    DEATH_MARK("death_mark", "Death Mark", 20, 3, PlayerClass.NECROMANCER, 60, false, false,
            40, 30, 600, 300,
            "Mark an enemy for death. DoT + AoE explosion on death.", "DkM"),
    UNDYING_WILL("undying_will", "Undying Will", 20, 3, PlayerClass.NECROMANCER, 60, false, true,
            0, 0, 0, 0,
            "Passive. Revive on fatal damage. +2% minion HP per level.", "UW"),
    SOUL_LINK("soul_link", "Soul Link", 20, 3, PlayerClass.NECROMANCER, 60, true, false,
            30, 15, 0, 0,
            "Toggle. Redirect damage to nearest minion. Minions deal bonus damage.", "SL"),
    ENHANCE_UNDEAD("enhance_undead", "Enhance Undead", 20, 3, PlayerClass.NECROMANCER, 60, false, true,
            0, 0, 0, 0,
            "Passive. +1% summon damage per level. Max level: summon Wither Skeletons.", "EU"),

    // --- Necromancer T4 ---
    NIGHT_OF_THE_LIVING_DEAD("night_of_the_living_dead", "Night of The Living Dead", 25, 4, PlayerClass.NECROMANCER, 100, false, false,
            80, 55, 1800, 1800,
            "Domain. Minions speed x2, you and minions cannot die inside. 30s.", "NLD"),
    UNDEAD_ARMAMENT("undead_armament", "Undead Armament", 20, 4, PlayerClass.NECROMANCER, 100, false, true,
            0, 0, 0, 0,
            "Passive. Summoned skeletons gain armor. Quality scales with level.", "UA"),

    // --- Beast Master T3 (passives) ---
    TIGER_CLAW_MASTERY_2("tiger_claw_mastery_2", "Tiger Claw Mastery (+1)", 20, 3, PlayerClass.BEAST_MASTER, 60, false, true,
            0, 0, 0, 0,
            "Passive. +1 Tiger Claw max hit. +Tiger Claw final damage.", "TC+"),
    TURTLE_SHELL_MASTERY_2("turtle_shell_mastery_2", "Turtle Shell Mastery (+1)", 20, 3, PlayerClass.BEAST_MASTER, 60, false, true,
            0, 0, 0, 0,
            "Passive. +0.5% shield per level.", "TS+"),
    BEAR_PAW_MASTERY_2("bear_paw_mastery_2", "Bear Paw Mastery (+1)", 20, 3, PlayerClass.BEAST_MASTER, 60, false, true,
            0, 0, 0, 0,
            "Passive. +0.1s stun per level. +Bear Paw final damage.", "BP+"),
    PHOENIX_WINGS_MASTERY_2("phoenix_wings_mastery_2", "Phoenix Wings Mastery (+1)", 20, 3, PlayerClass.BEAST_MASTER, 60, false, true,
            0, 0, 0, 0,
            "Passive. +0.5% healing per level. Max level: +1 lifesteal hit.", "PW+"),
    MASTER_OF_NATURE("master_of_nature", "Master of Nature", 20, 3, PlayerClass.BEAST_MASTER, 60, false, true,
            0, 0, 0, 0,
            "Passive. +1% enhanced skill damage per level.", "MoN"),

    // =====================================================
    // Limitless — Tier 1 (requires level 10, max 10)
    // =====================================================
    BLACK_FLASH("black_flash", "Black Flash", 10, 1, PlayerClass.LIMITLESS, 10, false, false,
            0, 0, 120, 60, // MP: manual 5% max MP. CD: 6s→3s
            "MATK-scaled strike. Empowers next melee attack. 5% MP cost.", "BF"),
    SIX_EYES_NEWBORN("six_eyes_newborn", "Six Eyes - New Born", 10, 1, PlayerClass.LIMITLESS, 10, false, true,
            0, 0, 0, 0,
            "Passive. -5% all skill MP cost per level.", "SEN"),
    INFINITY("limitless_infinity", "Infinity", 10, 1, PlayerClass.LIMITLESS, 10, true, false,
            50, 25, 0, 0, // Toggle drain: 5%→2.5% MP/sec
            "Toggle. All damage redirected to MP. Deflects projectiles.", "INF"),

    // Limitless — Tier 2 (requires level 30, max 15)
    CURSED_TECHNIQUE_BLUE("cursed_technique_blue", "Cursed Technique: Blue", 15, 2, PlayerClass.LIMITLESS, 30, false, false,
            0, 0, 200, 100, // MP: manual 10%. CD: 10s→5s
            "Tap: push enemies forward. Hold: pull to cursor. 10% MP.", "BLU"),
    SIX_EYES_JUNIOR("six_eyes_junior", "Six Eyes - Junior", 15, 2, PlayerClass.LIMITLESS, 30, false, true,
            0, 0, 0, 0,
            "Passive. Total MP cost reduction up to 60%. Flight during Infinity (5% MP/s).", "SEJ"),
    SIX_EYES_SEE_THROUGH("six_eyes_see_through", "Six Eyes - See Through", 15, 2, PlayerClass.LIMITLESS, 30, false, true,
            0, 0, 0, 0,
            "Passive. +1.33% crit rate, +3.33% crit damage per level.", "SET",
            stat(CRIT, 1.33), stat(CDMG, 3.33)),

    // Limitless T3 (required level 60, max level 20)
    CURSED_TECHNIQUE_RED("cursed_technique_red", "C.T. Reversal: Red", 20, 3, PlayerClass.LIMITLESS, 60, false, false,
            0, 0, 300, 150,
            "Charge 1.5-3s, fire destructive beam. Explosion on impact. 40% MP + 10%/0.5s.", "RED"),
    SIX_EYES_MASTERED("six_eyes_mastered", "Six Eyes - Mastered", 20, 3, PlayerClass.LIMITLESS, 60, false, true,
            0, 0, 0, 0,
            "Passive. +0.5% MATK per level. Six Eyes MP reduction cap \u2192 70%.", "SEM",
            stat(MATK_PCT, 0.5)),
    REVERSE_CURSED_TECHNIQUE("reverse_cursed_technique", "Reverse Cursed Technique", 20, 3, PlayerClass.LIMITLESS, 60, false, false,
            0, 0, 400, 200,
            "Instant heal to full. MP cost = missing HP% of current MP.", "RCT"),

    // =====================================================
    // Limitless — Tier 4 (requires level 100, max 25)
    // =====================================================
    HOLLOW_TECHNIQUE_PURPLE("hollow_technique_purple", "Hollow Technique: Purple", 25, 4, PlayerClass.LIMITLESS, 100, false, false,
            0, 0, 1200, 1200,
            "Passive. Use Red+Blue together to fire devastating beam. Pierces all.", "PRP"),
    SIX_EYES_AWAKENED("six_eyes_awakened", "Six Eyes - Awakened", 25, 4, PlayerClass.LIMITLESS, 100, false, true,
            0, 0, 0, 0,
            "Passive. +0.2% MATK per level. Six Eyes MP reduction cap \u2192 80%.", "SEA",
            stat(MATK_PCT, 0.2)),

    // =====================================================
    // Hidden — Reserved / Legacy
    // =====================================================
    VITAL_SURGE("vital_surge", "Vital Surge", 15, 99, null, 999, false, false,
            38, 28, 700, 300,
            "Heal yourself and cleanse poison and wither effects.", "VS");

    private static final Map<String, SkillType> BY_ID = new HashMap<>();

    static {
        for (SkillType type : values()) {
            BY_ID.put(type.id, type);
        }
        // NBT migration: old "infinity" id → renamed Mage skill
        BY_ID.put("infinity", ARCANE_INFINITY);
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
    private final StatDef[] statDefs;

    SkillType(String id, String displayName, int maxLevel, int tier,
              PlayerClass requiredClass, int requiredPlayerLevel, boolean toggle, boolean passive,
              int baseMpCost, int minMpCost, int baseCooldown, int minCooldown,
              String description, String abbreviation, StatDef... statDefs) {
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
        this.statDefs = statDefs;
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
    public StatDef[] getStatDefs() { return statDefs; }

    /** Whether this skill is visible (not hidden/reserved). */
    public boolean isAvailable() { return tier <= 4; }

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

    /** Toggle drain % per second (e.g. 3.0 means 3% of max MP). baseMpCost stores tenths of percent. */
    public double getToggleDrainPercent(int level) {
        return getMpCost(level) / 10.0;
    }

    /** Actual MP drain per second for toggle skills, based on % of max MP. Minimum 1. */
    public int getToggleMpPerSecond(int level, int maxMp) {
        return Math.max(1, maxMp * getMpCost(level) / 1000);
    }

    public static SkillType fromId(String id) {
        return BY_ID.get(id);
    }
}
