package com.monody.projectleveling.skill;

import com.monody.projectleveling.skill.StatContribRegistry.StatDef;
import static com.monody.projectleveling.skill.StatContribRegistry.StatLine.*;
import static com.monody.projectleveling.skill.StatContribRegistry.stat;
import static com.monody.projectleveling.skill.StatContribRegistry.statTag;

import java.util.EnumMap;
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
            "Empower next melee: +20-50% bonus. Cleaves up to 3 enemies in 4 blocks.", "SB"),
    WAR_CRY("war_cry", "War Cry", 10, 1, PlayerClass.WARRIOR, 10, false, false,
            22, 16, 400, 200,
            "Buff ATK 10-20% for 15-30s. Pull aggro from nearby mobs. Range scales with VIT.", "WC"),
    WARRIOR_MASTERY("warrior_mastery", "Warrior Mastery", 10, 1, PlayerClass.WARRIOR, 10, false, true,
            0, 0, 0, 0,
            "Passive. +2% max HP, -1% damage taken, +Knockback Resist per level.", "WM",
            stat(HP_PCT, 2.0), stat(DMG_RED, 1.0)),

    // --- Assassin (LUK) ---
    SHADOW_STRIKE("shadow_strike", "Shadow Strike", 10, 1, PlayerClass.ASSASSIN, 10, false, false,
            22, 16, 280, 100,
            "Empower next melee with bonus shadow damage.", "SS"),
    VENOM("venom", "Bleeding Edge", 10, 1, PlayerClass.ASSASSIN, 10, false, true,
            0, 0, 0, 0,
            "Passive. 20-64% chance to bleed per hit. Stacks up to 10.", "BE"),
    CRITICAL_EDGE("critical_edge", "Critical Edge", 10, 1, PlayerClass.ASSASSIN, 10, false, true,
            0, 0, 0, 0,
            "Passive. +1% crit rate and +2% crit damage per level.", "CE",
            stat(CRIT, 1.0), stat(CDMG, 2.0)),

    // --- Archer (DEX) ---
    ARROW_RAIN("arrow_rain", "Arrow Rain", 10, 1, PlayerClass.ARCHER, 10, false, false,
            22, 16, 240, 120,
            "Rain arrows in 3-block radius for 1-3s. 2-3 arrows/tick. Scales with DEX.", "AR"),
    SOUL_ARROW("soul_arrow", "Soul Arrow", 10, 1, PlayerClass.ARCHER, 10, true, false,
            20, 10, 0, 0,
            "Toggle. No arrow cost. +(5+level)% projectile damage.", "SA"),
    SHARP_EYES("sharp_eyes", "Sharp Eyes", 10, 1, PlayerClass.ARCHER, 10, false, true,
            0, 0, 0, 0,
            "Passive. +1.5% crit rate, +5% crit dmg per level. +1 proj range per 2 levels.", "SE",
            stat(CRIT, 1.5), stat(CDMG, 5.0)),

    // --- Healer (INT) ---
    HOLY_LIGHT("holy_light", "Holy Light", 10, 1, PlayerClass.HEALER, 10, false, false,
            20, 15, 160, 80,
            "Heal self and allies in 6 blocks. Damages undead (50% to non-undead). Scales with FAI.", "HL"),
    BLESS("bless", "Bless", 10, 1, PlayerClass.HEALER, 10, false, false,
            25, 18, 900, 450,
            "+10% ATK, +10% MATK, +10% DMG reduction for 30+3s/lv. Heals. 8-block range.", "BLS"),
    MP_RECOVERY("mp_recovery", "MP Recovery", 10, 1, PlayerClass.HEALER, 10, false, true,
            0, 0, 0, 0,
            "Passive. +0.2% MP regen per level. Restore MP on kills.", "MR"),

    // --- Mage (INT) ---
    FLAME_ORB("flame_orb", "Flame Orb", 10, 1, PlayerClass.MAGE, 10, false, false,
            16, 12, 140, 100,
            "Launch fireball: AoE 3+ block radius. Sets fire 3-6s. Scales with INT.", "FO"),
    MAGIC_GUARD("magic_guard", "Magic Guard", 10, 1, PlayerClass.MAGE, 10, true, false,
            30, 15, 0, 0,
            "Toggle. Redirect 30-60% of incoming damage to MP.", "MG"),
    ELEMENTAL_DRAIN("elemental_drain", "Elemental Drain", 10, 1, PlayerClass.MAGE, 10, false, true,
            0, 0, 0, 0,
            "Passive. +2%/lv damage per debuff on target (max 50% total).", "ED"),

    // --- Ninja (AGI + LUK) ---
    SHURIKEN_JUTSU("shuriken_jutsu", "Shuriken Jutsu", 10, 1, PlayerClass.NINJA, 10, false, false,
            16, 12, 200, 100,
            "Throw 5-10 shurikens in a cone. Count scales with level.", "SJu"),
    SUBSTITUTION_JUTSU("substitution_jutsu", "Substitution Jutsu", 10, 1, PlayerClass.NINJA, 10, false, false,
            18, 14, 300, 160,
            "Buffer 3-13s. First hit negated, teleport behind attacker.", "SUB"),
    KUNAI_MASTERY("kunai_mastery", "Kunai Mastery", 10, 1, PlayerClass.NINJA, 10, false, true,
            0, 0, 0, 0,
            "Passive. +ATK% (AGI scales), +LUK crit rate, +1.5% projectile damage per level.", "KM",
            stat(PROJ, 1.5)),

    // --- Necromancer (INT + Mind) ---
    LIFE_DRAIN("life_drain", "Life Drain", 10, 1, PlayerClass.NECROMANCER, 10, false, false,
            20, 15, 300, 160,
            "MATK beam. Heals 20-36% of damage dealt. Range 5+ blocks.", "LD"),
    RAISE_SKELETON("raise_skeleton", "Raise Skeleton", 10, 1, PlayerClass.NECROMANCER, 10, true, false,
            25, 15, 0, 0,
            "Toggle. Summon skeleton minion. Gains better weapons by level.", "RS"),
    DARK_PACT("dark_pact", "Dark Pact", 10, 1, PlayerClass.NECROMANCER, 10, false, true,
            0, 0, 0, 0,
            "Passive. +2% max MP and +1.5% summon damage per level.", "DkP"),
    UNHOLY_FERVOR("unholy_fervor", "Unholy Fervor", 10, 1, PlayerClass.NECROMANCER, 10, false, false,
            22, 16, 400, 200,
            "Buff skeletons: +15-30% damage, +speed for 8-12s.", "UF"),

    // --- Beast Master (STR/VIT) ---
    TIGER_CLAW("tiger_claw", "Tiger Claw", 10, 1, PlayerClass.BEAST_MASTER, 10, false, false,
            18, 14, 80, 80,
            "Next melee hits 2-3 extra times. Bypasses iframe.", "TC"),
    TURTLE_SHELL("turtle_shell", "Turtle Shell", 10, 1, PlayerClass.BEAST_MASTER, 10, false, false,
            16, 12, 80, 80,
            "Gain max HP % as absorption shield for 5s.", "TS"),
    BEAR_PAW("bear_paw", "Bear Paw", 10, 1, PlayerClass.BEAST_MASTER, 10, false, false,
            18, 14, 80, 80,
            "Next melee stuns 0.5-2s. Applies Weakness, Darkness, Slowness.", "BP"),
    PHOENIX_WINGS("phoenix_wings", "Phoenix Wings", 10, 1, PlayerClass.BEAST_MASTER, 10, false, false,
            22, 16, 80, 80,
            "Heal 1-10% max HP + Regen I. Next 2 hits lifesteal 1-5%.", "PW"),
    POWER_OF_NATURE("power_of_nature", "Power of Nature", 10, 1, PlayerClass.BEAST_MASTER, 10, false, false,
            30, 22, 1200, 1200,
            "Doubles next skill effect. CD reduced 2s per basic skill use.", "PoN"),

    // --- Beast Master T2 (passives) ---
    TIGER_CLAW_MASTERY("tiger_claw_mastery", "Tiger Claw Mastery", 10, 2, PlayerClass.BEAST_MASTER, 30, false, true,
            0, 0, 0, 0,
            "Passive. +1 Tiger Claw max hit. +0.5-5% final damage/lv.", "TCM"),
    TURTLE_SHELL_MASTERY("turtle_shell_mastery", "Turtle Shell Mastery", 10, 2, PlayerClass.BEAST_MASTER, 30, false, true,
            0, 0, 0, 0,
            "Passive. +0.5% shield per level.", "TSM"),
    BEAR_PAW_MASTERY("bear_paw_mastery", "Bear Paw Mastery", 10, 2, PlayerClass.BEAST_MASTER, 30, false, true,
            0, 0, 0, 0,
            "Passive. +0.1s stun/lv. +1-10% Bear Paw final damage/lv.", "BPM"),
    PHOENIX_WINGS_MASTERY("phoenix_wings_mastery", "Phoenix Wings Mastery", 10, 2, PlayerClass.BEAST_MASTER, 30, false, true,
            0, 0, 0, 0,
            "Passive. +0.5% healing per level. Max level: +1 lifesteal hit.", "PWM"),
    PREDATOR_INSTINCT("predator_instinct", "Predator Instinct", 10, 2, PlayerClass.BEAST_MASTER, 30, false, true,
            0, 0, 0, 0,
            "Passive. +1.5% crit, +2% cdmg per level. Crits grant Speed I 2s.", "PI"),

    // =====================================================
    // Tier 2 — Class skills, requires player level 30
    // =====================================================

    // --- Warrior ---
    SPIRIT_BLADE("spirit_blade", "Spirit Blade", 15, 2, PlayerClass.WARRIOR, 30, false, false,
            30, 22, 900, 900,
            "Party buff: +20% ATK for 30s. Self: -5 to 20% damage taken.", "SpB"),
    GROUND_SLAM("ground_slam", "Ground Slam", 15, 2, PlayerClass.WARRIOR, 30, false, false,
            30, 22, 280, 140,
            "AoE slam + stun. Radius scales with level.", "GS"),
    FINAL_ATTACK("final_attack", "Final Attack", 15, 2, PlayerClass.WARRIOR, 30, false, true,
            0, 0, 0, 0,
            "Passive. 15-50% chance to repeat melee hit at 50% damage.", "FA"),

    // --- Assassin ---
    STEALTH("stealth", "Stealth", 15, 2, PlayerClass.ASSASSIN, 30, true, false,
            35, 20, 120, 60,
            "Toggle. Invisible. Mobs ignore beyond 5-1.6 block detection range.", "STH"),
    BLADE_FURY("blade_fury", "Blade Fury", 15, 2, PlayerClass.ASSASSIN, 30, false, false,
            24, 18, 160, 80,
            "360 AoE spin. Applies 3 bleed stacks.", "BF"),
    EVASION("evasion", "Evasion", 15, 2, PlayerClass.ASSASSIN, 30, false, true,
            0, 0, 0, 0,
            "Passive. 2% dodge per level. Dodge guarantees next crit.", "EV",
            stat(DODGE, 2.0)),

    // --- Archer ---
    ARROW_BOMB("arrow_bomb", "Arrow Bomb", 15, 2, PlayerClass.ARCHER, 30, false, false,
            25, 18, 200, 100,
            "Explosive arrow: AoE 3-4 blocks, stun 2-3s. Damage scales with DEX.", "AB"),
    COVERING_FIRE("covering_fire", "Covering Fire", 15, 2, PlayerClass.ARCHER, 30, false, false,
            22, 16, 300, 150,
            "Leap backward, fire 3-5 arrows forward. Damage scales with DEX.", "CF"),
    EVASION_BOOST("evasion_boost", "Evasion Boost", 15, 2, PlayerClass.ARCHER, 30, false, true,
            0, 0, 0, 0,
            "Passive. 2% dodge per level. Dodge guarantees next arrow crits.", "EB",
            stat(DODGE, 2.0)),

    // --- Healer ---
    HOLY_SHELL("holy_shell", "Holy Shell", 15, 2, PlayerClass.HEALER, 30, false, false,
            38, 28, 1200, 600,
            "Shield self and allies in 6 blocks. Cleanses Poison and Wither. Scales with FAI.", "HS"),
    DISPEL("dispel", "Dispel", 15, 2, PlayerClass.HEALER, 30, false, false,
            24, 18, 600, 300,
            "Cleanse allies in 8 blocks: Poison, Wither, Weakness, Slowness.", "DSP"),
    DIVINE_PROTECTION("divine_protection", "Divine Protection", 15, 2, PlayerClass.HEALER, 30, false, true,
            0, 0, 0, 0,
            "Passive. +3% status resistance per level. Auto-cleanse chance.", "DP"),
    HOLY_FERVOR("holy_fervor", "Holy Fervor", 15, 2, PlayerClass.HEALER, 30, false, true,
            0, 0, 0, 0,
            "Passive. +2% dmg, +3% vs undead, +0.5% crit, +1% cdmg, +1.5% MATK per level.", "HF"),

    // --- Mage ---
    FROST_BIND("frost_bind", "Frost Bind", 15, 2, PlayerClass.MAGE, 30, false, false,
            30, 22, 400, 200,
            "Pull mobs inward and freeze 3-6s. Slowness IV + Weakness I. Scales with INT.", "FrB"),
    POISON_MIST("poison_mist", "Poison Mist", 15, 2, PlayerClass.MAGE, 30, false, false,
            28, 20, 300, 150,
            "Poison cloud 4+ block radius for 8-11s. Combo: detonated by Mist Eruption.", "PM"),
    ELEMENT_AMPLIFICATION("element_amplification", "Element Amplification", 15, 2, PlayerClass.MAGE, 30, false, true,
            0, 0, 0, 0,
            "Passive. +3% damage per level. +20% MP cost for all skills.", "EA",
            stat(DMG, 3.0)),

    // --- Ninja T2 ---
    SHADOW_CLONE("shadow_clone", "Shadow Clone", 15, 2, PlayerClass.NINJA, 30, true, false,
            30, 15, 0, 0,
            "Toggle. Summon 1-2 clones at 20-50% stats. Copy your skill attacks.", "SC"),
    FLYING_RAIJIN("flying_raijin", "Flying Raijin", 15, 2, PlayerClass.NINJA, 30, false, false,
            24, 18, 300, 160,
            "Throw kunai, teleport to target. Marks for re-teleport.", "FRJ"),
    FLYING_RAIJIN_GROUND("flying_raijin_ground", "Flying Raijin: Ground", 15, 2, PlayerClass.NINJA, 30, false, false,
            16, 12, 400, 200,
            "Place a kunai at your feet. Reuse to teleport back.", "FRG"),
    CHAKRA_CONTROL("chakra_control", "Chakra Control", 15, 2, PlayerClass.NINJA, 30, false, true,
            0, 0, 0, 0,
            "Passive. -1% MP cost per level. +0.15% MP regen per level.", "ChC"),

    // --- Necromancer T2 ---
    BONE_SHIELD("bone_shield", "Bone Shield", 15, 2, PlayerClass.NECROMANCER, 30, true, false,
            30, 15, 0, 0,
            "Toggle. Reduce damage taken by 8-20%.", "BnS"),
    CORPSE_EXPLOSION("corpse_explosion", "Corpse Explosion", 15, 2, PlayerClass.NECROMANCER, 30, false, false,
            30, 22, 400, 200,
            "Explode all skeletons. 4-block AoE per skeleton.", "CEx"),
    SOUL_SIPHON("soul_siphon", "Soul Siphon", 15, 2, PlayerClass.NECROMANCER, 30, false, true,
            0, 0, 0, 0,
            "Passive. +0.5%/lv HP and +0.2%/lv MP restored on kill.", "Si"),
    SKELETAL_MASTERY("skeletal_mastery", "Skeletal Mastery", 15, 2, PlayerClass.NECROMANCER, 30, false, true,
            0, 0, 0, 0,
            "Passive. Minions +0.5-2.5% lifesteal, -3-6% damage taken.", "SM"),
    DARK_RESONANCE("dark_resonance", "Dark Resonance", 15, 2, PlayerClass.NECROMANCER, 30, false, true,
            0, 0, 0, 0,
            "Passive. +1% crit, +2% cdmg per level. Crits apply Wither I 2s.", "DR"),

    // =====================================================
    // Tier 3 — Class skills, requires player level 60
    // =====================================================

    // --- Warrior ---
    BEAM_BLADE("beam_blade", "Beam Blade", 20, 3, PlayerClass.WARRIOR, 60, false, false,
            30, 22, 80, 80,
            "Piercing blade. Hits all enemies in a line.", "BB"),
    UNBREAKABLE("unbreakable", "Unbreakable", 20, 3, PlayerClass.WARRIOR, 60, false, true,
            0, 0, 0, 0,
            "Passive. Revive at 20-35% HP on fatal damage. 5 min cooldown.", "UB"),
    BERSERKER_SPIRIT("berserker_spirit", "Berserker Spirit", 20, 3, PlayerClass.WARRIOR, 60, false, true,
            0, 0, 0, 0,
            "Passive. +1% crit, +1.5% cdmg, +0.3% lifesteal/lv. +Final Attack chance.", "BS",
            stat(CRIT, 1.0)),

    // --- Assassin ---
    SHADOW_SNEAK("shadow_sneak", "Shadow Sneak", 20, 3, PlayerClass.ASSASSIN, 60, false, false,
            38, 26, 500, 160,
            "Teleport behind target + 5 bleed stacks.", "SS"),
    SHADOW_PARTNER("shadow_partner", "Shadow Partner", 20, 3, PlayerClass.ASSASSIN, 60, true, false,
            40, 20, 0, 0,
            "Toggle. Shadow clone mirrors attacks at 30-40% damage.", "SP"),
    FATAL_BLOW("fatal_blow", "Fatal Blow", 20, 3, PlayerClass.ASSASSIN, 60, false, true,
            0, 0, 0, 0,
            "Passive. Below 30% HP target: +2%/lv damage. Execute chance.", "FB",
            statTag(DMG, 2.0)),
    SHADOW_LEGION("shadow_legion", "Shadow Legion", 20, 3, PlayerClass.ASSASSIN, 60, false, true,
            0, 0, 0, 0,
            "Passive. 2nd Shadow Partner, auto-attacks, counter on dodge, reduced MP penalty.", "SL"),

    // --- Assassin T4 ---
    FINAL_BLOW("final_blow", "Final Blow", 25, 4, PlayerClass.ASSASSIN, 100, false, false,
            50, 30, 600, 300,
            "Empower next attack. +1.0x per bleed stack consumed.", "FLB"),
    LETHAL_MASTERY("lethal_mastery", "Lethal Mastery", 25, 4, PlayerClass.ASSASSIN, 100, false, true,
            0, 0, 0, 0,
            "Passive. +0.2% ATK and +0.4% bleed chance per level.", "LM"),

    // --- Archer ---
    PHOENIX("phoenix", "Phoenix", 20, 3, PlayerClass.ARCHER, 60, false, false,
            48, 35, 1800, 900,
            "Summon phoenix: auto-attacks, 80% Slow III. Grants Resistance I. 30-60s.", "PX"),
    HURRICANE("hurricane", "Hurricane", 20, 3, PlayerClass.ARCHER, 60, true, false,
            40, 20, 0, 0,
            "Toggle. Rapid arrows at nearest mob, 2/s. Self Slowness II. Scales with DEX.", "HC"),
    MORTAL_BLOW("mortal_blow", "Mortal Blow", 20, 3, PlayerClass.ARCHER, 60, false, true,
            0, 0, 0, 0,
            "Passive. Below 30% HP: +2%/lv proj damage. Execute chance on projectiles.", "MB",
            statTag(DMG, 2.0)),

    // --- Archer T4 ---
    STORM_OF_ARROWS("storm_of_arrows", "Storm of Arrows", 25, 4, PlayerClass.ARCHER, 100, false, false,
            60, 40, 1200, 1200,
            "Rain homing arrows for 30s, 2-3 per 5s. Bypasses iframe. Scales with DEX.", "SOA"),

    // --- Archer T5 ---
    HAWK_EYE("hawk_eye", "Hawk Eye", 5, 4, PlayerClass.ARCHER, 120, false, true,
            0, 0, 0, 0,
            "Passive. +1% crit rate, +2% crit dmg per level.", "HE",
            stat(CRIT, 1.0), stat(CDMG, 2.0)),

    // --- Healer ---
    BENEDICTION("benediction", "Benediction", 20, 3, PlayerClass.HEALER, 60, false, false,
            55, 40, 1800, 900,
            "Holy zone 4+ block radius, 15+lv s. Heals allies, +lv% ATK/MATK, damages enemies.", "BN"),
    ANGEL_RAY("angel_ray", "Angel Ray", 20, 3, PlayerClass.HEALER, 60, false, false,
            18, 14, 200, 100,
            "Holy beam: MATK damage + heals allies in 3-block AoE. Scales with FAI.", "ARL"),
    BLESSED_ENSEMBLE("blessed_ensemble", "Blessed Ensemble", 20, 3, PlayerClass.HEALER, 60, false, true,
            0, 0, 0, 0,
            "Passive. +20% damage per nearby player (max 60%). +20% XP per nearby player (max 60%).", "BE"),

    // --- Healer T4 ---
    MAGIC_FINALE("magic_finale", "Magic: Finale", 25, 4, PlayerClass.HEALER, 100, false, false,
            80, 55, 1800, 1800,
            "Channel 15s, draw pentagram, beam from sky. 15-block radius. Scales with INT+FAI.", "MF"),
    RIGHTEOUSLY_INDIGNANT("righteously_indignant", "Righteously Indignant", 25, 4, PlayerClass.HEALER, 100, true, false,
            20, 15, 0, 0,
            "Toggle. Convert 30-100% Heal Power to MATK. Heal Power becomes 0.", "RI"),

    // --- Mage ---
    MIST_ERUPTION("mist_eruption", "Mist Eruption", 20, 3, PlayerClass.MAGE, 60, false, false,
            48, 35, 900, 350,
            "Detonate Poison Mist for burst AoE, 5+ blocks. No mist: arcane blast.", "ME"),
    ARCANE_INFINITY("arcane_infinity", "Arcane Infinity", 20, 3, PlayerClass.MAGE, 60, false, false,
            65, 45, 2400, 1200,
            "Free casting for 20+lv s. +5% damage every 4s, stacking.", "AI"),
    ARCANE_OVERDRIVE("arcane_overdrive", "Arcane Overdrive", 20, 3, PlayerClass.MAGE, 60, false, true,
            0, 0, 0, 0,
            "Passive. +1.5% crit rate, +1% crit damage, +1% armor pen per level.", "AO",
            stat(CRIT, 1.5), stat(CDMG, 1.0)),

    // --- Mage T4 ---
    STAR_FALL("star_fall", "Star Fall", 25, 4, PlayerClass.MAGE, 100, false, false,
            70, 45, 1200, 1200,
            "Channel: rain ~30 meteors over 10s, 10-block radius. Cannot move.", "SF"),
    ARCANE_POWER("arcane_power", "Arcane Power", 25, 4, PlayerClass.MAGE, 100, true, false,
            20, 20, 0, 0,
            "Toggle. Drain 2% max MP/s. +10% Magic Attack while active.", "AP"),

    // --- Ninja T3 ---
    RASENGAN("rasengan", "Rasengan", 20, 3, PlayerClass.NINJA, 60, false, false,
            70, 50, 600, 160,
            "Empower next melee. AoE 2-block radius. Combo with FR for extra hit.", "RSG"),
    SAGE_MODE("sage_mode", "Sage Mode", 20, 3, PlayerClass.NINJA, 60, true, false,
            30, 20, 0, 0,
            "Toggle. +2%/lv damage, +15% speed, knockback resist. Drains MP%/s.", "SgM"),
    EIGHT_INNER_GATES("eight_inner_gates", "Eight Inner Gates", 20, 3, PlayerClass.NINJA, 60, false, true,
            0, 0, 0, 0,
            "Passive. Below 30% HP: +2%/lv damage, +1%/lv speed boost.", "8G"),
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
            "Giant sword from sky. AoE in 10-block radius.", "HS"),
    WARLORDS_COMMAND("warlords_command", "Warlord's Command", 25, 4, PlayerClass.WARRIOR, 100, false, true,
            0, 0, 0, 0,
            "Passive. War Cry +0.4% ATK/lv. Aggro range scales more with VIT.", "WLC"),

    // --- Necromancer T3 ---
    ARMY_OF_THE_DEAD("army_of_the_dead", "Army of the Dead", 20, 3, PlayerClass.NECROMANCER, 60, false, false,
            65, 45, 1200, 600,
            "Summon 5 skeletons at 50% stats for 10+0.5s/lv.", "AD"),
    DEATH_MARK("death_mark", "Death Mark", 20, 3, PlayerClass.NECROMANCER, 60, false, false,
            40, 30, 600, 300,
            "Mark enemy: MATK DoT. On death: AoE explosion, restore HP/MP.", "DkM"),
    UNDYING_WILL("undying_will", "Undying Will", 20, 3, PlayerClass.NECROMANCER, 60, false, true,
            0, 0, 0, 0,
            "Passive. Revive at 10+lv% HP, 90-30s CD. +2% minion HP/lv.", "UW"),
    SOUL_LINK("soul_link", "Soul Link", 20, 3, PlayerClass.NECROMANCER, 60, true, false,
            30, 15, 0, 0,
            "Toggle. 10-22% damage to minion. Minion +8-20% bonus damage.", "SL"),
    ENHANCE_UNDEAD("enhance_undead", "Enhance Undead", 20, 3, PlayerClass.NECROMANCER, 60, false, true,
            0, 0, 0, 0,
            "Passive. +1% summon damage per level. Max level: summon Wither Skeletons.", "EU"),

    // --- Necromancer T4 ---
    NIGHT_OF_THE_LIVING_DEAD("night_of_the_living_dead", "Night of The Living Dead", 25, 4, PlayerClass.NECROMANCER, 100, false, false,
            80, 55, 1800, 1800,
            "Domain 20 blocks: minions x2 speed, immortal inside. 30s.", "NLD"),
    UNDEAD_ARMAMENT("undead_armament", "Undead Armament", 20, 4, PlayerClass.NECROMANCER, 100, false, true,
            0, 0, 0, 0,
            "Passive. Skeletons gain armor: Leather\u2192Chain\u2192Iron\u2192Diamond by level.", "UA"),

    // --- Beast Master T3 (passives) ---
    TIGER_CLAW_MASTERY_2("tiger_claw_mastery_2", "Tiger Claw Mastery (+1)", 20, 3, PlayerClass.BEAST_MASTER, 60, false, true,
            0, 0, 0, 0,
            "Passive. +1 Tiger Claw max hit. +0.5-5% final damage/lv.", "TC+"),
    TURTLE_SHELL_MASTERY_2("turtle_shell_mastery_2", "Turtle Shell Mastery (+1)", 20, 3, PlayerClass.BEAST_MASTER, 60, false, true,
            0, 0, 0, 0,
            "Passive. +0.5% shield per level.", "TS+"),
    BEAR_PAW_MASTERY_2("bear_paw_mastery_2", "Bear Paw Mastery (+1)", 20, 3, PlayerClass.BEAST_MASTER, 60, false, true,
            0, 0, 0, 0,
            "Passive. +0.1s stun/lv. +1-10% Bear Paw final damage/lv.", "BP+"),
    PHOENIX_WINGS_MASTERY_2("phoenix_wings_mastery_2", "Phoenix Wings Mastery (+1)", 20, 3, PlayerClass.BEAST_MASTER, 60, false, true,
            0, 0, 0, 0,
            "Passive. +0.5% healing per level. Max level: +1 lifesteal hit.", "PW+"),
    MASTER_OF_NATURE("master_of_nature", "Master of Nature", 20, 3, PlayerClass.BEAST_MASTER, 60, false, true,
            0, 0, 0, 0,
            "Passive. +1%/lv enhanced skill effect when Power of Nature active.", "MoN"),

    // =====================================================
    // Limitless — Tier 1 (requires level 10, max 10)
    // =====================================================
    BLACK_FLASH("black_flash", "Black Flash", 10, 1, PlayerClass.LIMITLESS, 10, false, false,
            0, 0, 120, 60, // MP: manual 5% max MP. CD: 6s→3s
            "MATK strike. Empowers next melee for 3s. 15% MP cost.", "BF"),
    SIX_EYES_NEWBORN("six_eyes_newborn", "Six Eyes - New Born", 10, 1, PlayerClass.LIMITLESS, 10, false, true,
            0, 0, 0, 0,
            "Passive. -5% all skill MP cost per level.", "SEN"),
    INFINITY("limitless_infinity", "Infinity", 10, 1, PlayerClass.LIMITLESS, 10, true, false,
            50, 25, 0, 0, // Toggle drain: 5%→2.5% MP/sec
            "Toggle. All damage redirected to MP (2:1 ratio). Deflects projectiles.", "INF"),

    // Limitless — Tier 2 (requires level 30, max 15)
    CURSED_TECHNIQUE_BLUE("cursed_technique_blue", "Cursed Technique: Blue", 15, 2, PlayerClass.LIMITLESS, 30, false, false,
            0, 0, 200, 100, // MP: manual 10%. CD: 10s→5s
            "Tap: push cone 6+ blocks. Hold: pull mobs 8+ blocks. 25% MP.", "BLU"),
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
            "Charge 1.5-3s, fire beam 40 blocks. AoE 3+ blocks. 40% MP +10%/0.5s.", "RED"),
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
            "Red+Blue combo: 128-block beam, 10-block radius. Consumes all MP. 60s CD.", "PRP"),
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

    /** Multiplier formula for real-time tooltip display. */
    public static class MultInfo {
        public final String label;
        public final float base, perLevel, perStat1, perStat2;
        public final String stat1, stat2;
        public final String extraFormula; // optional extra term for formula display (e.g. "+0.2/MP")

        public MultInfo(String label, float base, float perLevel, float perStat, String stat) {
            this(label, base, perLevel, perStat, stat, 0, null, null);
        }

        public MultInfo(String label, float base, float perLevel, float perStat1, String stat1, float perStat2, String stat2) {
            this(label, base, perLevel, perStat1, stat1, perStat2, stat2, null);
        }

        public MultInfo(String label, float base, float perLevel, float perStat1, String stat1, float perStat2, String stat2, String extraFormula) {
            this.label = label; this.base = base; this.perLevel = perLevel;
            this.perStat1 = perStat1; this.stat1 = stat1;
            this.perStat2 = perStat2; this.stat2 = stat2;
            this.extraFormula = extraFormula;
        }

        public float calc(int level, int s1, int s2) {
            return base + level * perLevel + s1 * perStat1 + s2 * perStat2;
        }

        public int pct(int level, int s1, int s2) {
            return Math.round(calc(level, s1, s2) * 100);
        }

        /** Raw multiplier formula, e.g. "DMG: 5.0 + Lv*0.30 + INT*0.15" */
        public String toFormula() {
            StringBuilder sb = new StringBuilder();
            sb.append(label).append(": ").append(fmt(base));
            if (perLevel != 0) sb.append(" + Lv*").append(fmt(perLevel));
            if (stat1 != null && perStat1 != 0) sb.append(" + ").append(stat1.toUpperCase()).append("*").append(fmt(perStat1));
            if (stat2 != null && perStat2 != 0) sb.append(" + ").append(stat2.toUpperCase()).append("*").append(fmt(perStat2));
            if (extraFormula != null) sb.append(extraFormula);
            return sb.toString();
        }

        private static String fmt(float v) {
            if (v == (int) v) return String.valueOf((int) v);
            String s = String.format("%.4f", v);
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
            return s;
        }
    }

    private static final Map<SkillType, MultInfo> MULT_MAP = new EnumMap<>(SkillType.class);

    static {
        // Warrior
        MULT_MAP.put(GROUND_SLAM, new MultInfo("DMG", 1.20f, 0.06f, 0.009f, "str"));
        MULT_MAP.put(BEAM_BLADE, new MultInfo("DMG", 1.50f, 0.05f, 0.012f, "str"));
        MULT_MAP.put(HEAVEN_SWORD, new MultInfo("DMG", 4.00f, 0.16f, 0.03f, "str"));
        // Assassin
        MULT_MAP.put(SHADOW_STRIKE, new MultInfo("Bonus", 0.60f, 0.09f, 0.008f, "luk"));
        MULT_MAP.put(BLADE_FURY, new MultInfo("DMG", 1.05f, 0.06f, 0.008f, "luk"));
        MULT_MAP.put(SHADOW_SNEAK, new MultInfo("DMG", 1.20f, 0.10f, 0.010f, "luk"));
        MULT_MAP.put(FINAL_BLOW, new MultInfo("DMG", 4.00f, 0.16f, 0.015f, "luk"));
        // Archer
        MULT_MAP.put(ARROW_RAIN, new MultInfo("Per Arrow", 0.12f, 0.012f, 0.0015f, "dex"));
        MULT_MAP.put(ARROW_BOMB, new MultInfo("DMG", 1.20f, 0.06f, 0.008f, "dex"));
        MULT_MAP.put(COVERING_FIRE, new MultInfo("Per Arrow", 0.75f, 0.045f, 0.006f, "dex"));
        MULT_MAP.put(PHOENIX, new MultInfo("Per Tick", 0.38f, 0.03f, 0.005f, "dex"));
        MULT_MAP.put(HURRICANE, new MultInfo("Per Arrow", 0.45f, 0.03f, 0.006f, "dex"));
        MULT_MAP.put(STORM_OF_ARROWS, new MultInfo("Per Arrow", 0.50f, 0.047f, 0.0027f, "dex"));
        // Healer
        MULT_MAP.put(HOLY_LIGHT, new MultInfo("DMG", 0.70f, 0.06f, 0.014f, "fai"));
        MULT_MAP.put(BLESS, new MultInfo("Heal", 1.00f, 0.05f, 0.01f, "fai"));
        MULT_MAP.put(HOLY_SHELL, new MultInfo("Shield", 1.50f, 0.06f, 0.008f, "fai"));
        MULT_MAP.put(BENEDICTION, new MultInfo("Heal/Tick", 0.30f, 0.02f, 0.004f, "fai"));
        MULT_MAP.put(ANGEL_RAY, new MultInfo("DMG", 1.00f, 0.07f, 0.014f, "fai"));
        MULT_MAP.put(MAGIC_FINALE, new MultInfo("DMG", 5.00f, 0.30f, 0.01f, "int", 0.015f, "fai"));
        // Mage
        MULT_MAP.put(FLAME_ORB, new MultInfo("DMG", 1.00f, 0.06f, 0.010f, "int"));
        MULT_MAP.put(FROST_BIND, new MultInfo("DMG", 0.65f, 0.04f, 0.006f, "int"));
        MULT_MAP.put(POISON_MIST, new MultInfo("Per Tick", 0.26f, 0.02f, 0.003f, "int"));
        MULT_MAP.put(MIST_ERUPTION, new MultInfo("DMG", 1.60f, 0.08f, 0.013f, "int"));
        MULT_MAP.put(STAR_FALL, new MultInfo("Per Meteor", 1.00f, 0.08f, 0.012f, "int"));
        // Ninja
        MULT_MAP.put(SHURIKEN_JUTSU, new MultInfo("Per Hit", 0.40f, 0.04f, 0.006f, "agi"));
        MULT_MAP.put(FLYING_RAIJIN, new MultInfo("DMG", 1.00f, 0.05f, 0.008f, "agi"));
        MULT_MAP.put(RASENGAN, new MultInfo("DMG", 1.10f, 0.07f, 0.012f, "agi"));
        MULT_MAP.put(FLYING_RAIJIN_SSRZ, new MultInfo("Per Strike", 1.00f, 0.06f, 0.010f, "agi"));
        // Necromancer
        MULT_MAP.put(LIFE_DRAIN, new MultInfo("DMG", 1.00f, 0.05f, 0.008f, "int"));
        MULT_MAP.put(CORPSE_EXPLOSION, new MultInfo("DMG", 2.00f, 0.10f, 0.013f, "int"));
        MULT_MAP.put(DEATH_MARK, new MultInfo("DoT/Tick", 0.40f, 0.025f, 0.004f, "int"));
        // Beast Master
        MULT_MAP.put(TIGER_CLAW, new MultInfo("Per Hit", 0.1222f, 0.0278f, 0f, null));
        // Limitless
        MULT_MAP.put(BLACK_FLASH, new MultInfo("DMG", 1.50f, 0.15f, 0.008f, "int"));
        MULT_MAP.put(CURSED_TECHNIQUE_BLUE, new MultInfo("DMG", 0.80f, 0.08f, 0.006f, "int"));
        MULT_MAP.put(CURSED_TECHNIQUE_RED, new MultInfo("DMG", 2.00f, 0.15f, 0.01f, "int"));
        MULT_MAP.put(HOLLOW_TECHNIQUE_PURPLE, new MultInfo("DMG", 5.00f, 0.30f, 0.15f, "int", 0, null, " + MP*0.002"));
    }

    /** Extra formula lines for skills — supplements MultInfo or standalone. */
    private static final Map<SkillType, String[]> FORMULA_MAP = new EnumMap<>(SkillType.class);

    static {
        // --- Warrior ---
        FORMULA_MAP.put(SLASH_BLAST, new String[]{"Bonus: 20 + (Lv-1)*3.33 %"});
        FORMULA_MAP.put(WAR_CRY, new String[]{"ATK: 10 + (Lv-1)*1.11 %", "Range: 5 + VIT*0.1"});
        FORMULA_MAP.put(WARRIOR_MASTERY, new String[]{"HP: Lv*2%, DMG Red: Lv*1%"});
        FORMULA_MAP.put(SPIRIT_BLADE, new String[]{"ATK: Lv*1.33%", "DEF: 0.05 + (Lv-1)*0.0107"});
        FORMULA_MAP.put(FINAL_ATTACK, new String[]{"Chance: 15 + (Lv-1)*2.5 %"});
        FORMULA_MAP.put(UNBREAKABLE, new String[]{"Revive: 0.2 + Lv*0.01"});
        FORMULA_MAP.put(BERSERKER_SPIRIT, new String[]{"FA Bonus: Lv*1.0"});
        FORMULA_MAP.put(WARLORDS_COMMAND, new String[]{"ATK: Lv*0.4", "VIT: Lv*0.004"});
        // --- Assassin ---
        FORMULA_MAP.put(VENOM, new String[]{"Chance: 20 + (Lv-1)*4.44 %"});
        FORMULA_MAP.put(STEALTH, new String[]{"Detection: max(5 - Lv*0.27, 1)"});
        FORMULA_MAP.put(SHADOW_PARTNER, new String[]{"Clone DMG: 0.3 + (Lv-1)*0.005"});
        FORMULA_MAP.put(LETHAL_MASTERY, new String[]{"ATK: Lv*0.2%", "Bleed: Lv*0.4%"});
        // --- Archer ---
        FORMULA_MAP.put(SOUL_ARROW, new String[]{"Proj DMG: (5+Lv)%"});
        // --- Healer (extras beyond MultInfo) ---
        FORMULA_MAP.put(HOLY_LIGHT, new String[]{"Heal: 2.0 + Lv*0.08 + FAI*0.015"});
        FORMULA_MAP.put(BENEDICTION, new String[]{"Buff: Lv*1.0%"});
        FORMULA_MAP.put(ANGEL_RAY, new String[]{"Heal: 1.0 + Lv*0.05 + FAI*0.01"});
        FORMULA_MAP.put(RIGHTEOUSLY_INDIGNANT, new String[]{"Convert: 30 + (Lv-1)*2.92 %"});
        // --- Mage (extras beyond MultInfo) ---
        FORMULA_MAP.put(MAGIC_GUARD, new String[]{"Redirect: 0.3 + Lv*0.03"});
        FORMULA_MAP.put(MIST_ERUPTION, new String[]{"No Mist: 0.8 + Lv*0.04 + INT*0.006"});
        // --- Ninja ---
        FORMULA_MAP.put(KUNAI_MASTERY, new String[]{"ATK%: AGI*0.008*Lv", "Proj: 1.0 + Lv*0.015"});
        FORMULA_MAP.put(SHADOW_CLONE, new String[]{"Stats: 0.2 + (Lv-1)*0.0214"});
        FORMULA_MAP.put(CHAKRA_CONTROL, new String[]{"Cost: 1.0 - Lv*0.01", "MP Regen: Lv*0.015"});
        FORMULA_MAP.put(EIGHT_INNER_GATES, new String[]{"DMG: 1.0 + Lv*0.02"});
        // --- Necromancer (extras beyond MultInfo) ---
        FORMULA_MAP.put(DEATH_MARK, new String[]{"Explode: 1.3 + Lv*0.065 + INT*0.01"});
        FORMULA_MAP.put(BONE_SHIELD, new String[]{"Reduction: min(8+Lv, 20)%"});
        FORMULA_MAP.put(UNHOLY_FERVOR, new String[]{"Buff: (15 + Lv*1.5)/100"});
        FORMULA_MAP.put(SOUL_LINK, new String[]{"Redirect: (10 + Lv*0.5)/100", "Minion: (8 + Lv*0.5)/100"});
        FORMULA_MAP.put(SKELETAL_MASTERY, new String[]{"Lifesteal: (0.5 + Lv*0.2)/100", "DMG Red: (3 + Lv*0.3)/100"});
    }

    public MultInfo getMultInfo() {
        return MULT_MAP.get(this);
    }

    public String[] getExtraFormulas() {
        return FORMULA_MAP.get(this);
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
