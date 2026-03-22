package com.monody.projectleveling.skill.classes;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.entity.assassin.ShadowPartnerEntity;
import com.monody.projectleveling.skill.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.monody.projectleveling.skill.SkillTooltips.TEXT_DIM;
import static com.monody.projectleveling.skill.SkillTooltips.TEXT_VALUE;

public final class BeastMasterSkills {

    public static final SkillType[] ALL = {
            SkillType.TIGER_CLAW, SkillType.TURTLE_SHELL, SkillType.BEAR_PAW,
            SkillType.PHOENIX_WINGS, SkillType.POWER_OF_NATURE,
    };

    private static final SkillType[] BASIC_SKILLS = {
            SkillType.TIGER_CLAW, SkillType.TURTLE_SHELL, SkillType.BEAR_PAW, SkillType.PHOENIX_WINGS,
    };

    private BeastMasterSkills() {}

    // ========== TOOLTIPS ==========

    public static void addDetailLines(List<String> texts, List<int[]> lines,
                                       SkillType skill, int level, PlayerStats stats) {
        switch (skill) {
            case TIGER_CLAW -> {
                SkillData sd = stats.getSkillData();
                int extraHits = getExtraHits(level);
                int tcmLv = sd.getLevel(SkillType.TIGER_CLAW_MASTERY);
                int tcm2Lv = sd.getLevel(SkillType.TIGER_CLAW_MASTERY_2);
                extraHits += getTigerClawMasteryHits(tcmLv) + getTigerClawMasteryHits(tcm2Lv);
                float dmgPct = getHitDamagePct(level);
                float dmgBonus = getTigerClawMasteryDmg(tcmLv) + getTigerClawMasteryDmg(tcm2Lv);
                float totalDmgPct = dmgPct * (1.0f + dmgBonus);
                texts.add("Extra hits: " + extraHits);
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Per-hit damage: " + String.format("%.0f", totalDmgPct * 100) + "% of original");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Bypasses iframe");
                lines.add(new int[]{TEXT_DIM});
                texts.add("Buff expires in 4s if unused");
                lines.add(new int[]{TEXT_DIM});
            }
            case TURTLE_SHELL -> {
                SkillData sd = stats.getSkillData();
                float shieldPct = getShieldPct(level);
                int tsmTotal = sd.getLevel(SkillType.TURTLE_SHELL_MASTERY)
                             + sd.getLevel(SkillType.TURTLE_SHELL_MASTERY_2);
                if (tsmTotal > 0) shieldPct += tsmTotal * 0.005f;
                texts.add("Shield: " + String.format("%.1f", shieldPct * 100) + "% of max HP");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Duration: 5s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Doesn't stack, persists between skills");
                lines.add(new int[]{TEXT_DIM});
            }
            case BEAR_PAW -> {
                SkillData sd = stats.getSkillData();
                float stunDur = getStunDuration(level);
                int bpmTotal = sd.getLevel(SkillType.BEAR_PAW_MASTERY)
                             + sd.getLevel(SkillType.BEAR_PAW_MASTERY_2);
                if (bpmTotal > 0) stunDur += bpmTotal * 0.1f;
                float bpDmgBonus = getBearPawMasteryDmg(sd.getLevel(SkillType.BEAR_PAW_MASTERY))
                                 + getBearPawMasteryDmg(sd.getLevel(SkillType.BEAR_PAW_MASTERY_2));
                texts.add("Stun: " + String.format("%.1f", stunDur) + "s");
                lines.add(new int[]{TEXT_VALUE});
                if (bpDmgBonus > 0) {
                    texts.add("Final damage: +" + String.format("%.1f", bpDmgBonus * 100) + "%");
                    lines.add(new int[]{TEXT_VALUE});
                }
                texts.add("Applies: Weakness, Darkness, Slowness");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Buff expires in 4s if unused");
                lines.add(new int[]{TEXT_DIM});
            }
            case PHOENIX_WINGS -> {
                SkillData sd = stats.getSkillData();
                float burstHealPct = getBurstHealPct(level);
                int pwmTotal = sd.getLevel(SkillType.PHOENIX_WINGS_MASTERY)
                             + sd.getLevel(SkillType.PHOENIX_WINGS_MASTERY_2);
                if (pwmTotal > 0) burstHealPct += pwmTotal * 0.005f;
                float lifestealPct = getLifestealPct(level);
                int lifestealHits = 2;
                if (sd.getLevel(SkillType.PHOENIX_WINGS_MASTERY) >= 10) lifestealHits++;
                if (sd.getLevel(SkillType.PHOENIX_WINGS_MASTERY_2) >= 20) lifestealHits++;
                texts.add("Burst heal: " + String.format("%.1f", burstHealPct * 100) + "% max HP");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Regen I for 4s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Lifesteal: " + String.format("%.1f", lifestealPct * 100) + "% for next " + lifestealHits + " hits");
                lines.add(new int[]{TEXT_VALUE});
            }
            case POWER_OF_NATURE -> {
                texts.add("Doubles next skill effect");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Cooldown: 60s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("CD reduced 2s per basic skill use");
                lines.add(new int[]{TEXT_DIM});
                texts.add("Combo: \"Enhanced + Skill Name\"");
                lines.add(new int[]{TEXT_DIM});
            }
            case TIGER_CLAW_MASTERY -> {
                texts.add("Tiger Claw max hits: +1");
                lines.add(new int[]{TEXT_VALUE});
                float tcDmg = getTigerClawMasteryDmg(level);
                texts.add("Tiger Claw final damage: +" + String.format("%.1f", tcDmg * 100) + "%");
                lines.add(new int[]{TEXT_VALUE});
            }
            case TURTLE_SHELL_MASTERY -> {
                float bonusShield = level * 0.5f;
                texts.add("Bonus shield: +" + String.format("%.1f", bonusShield) + "%");
                lines.add(new int[]{TEXT_VALUE});
            }
            case BEAR_PAW_MASTERY -> {
                float bonusStun = level * 0.1f;
                float bpDmg = getBearPawMasteryDmg(level);
                texts.add("Bonus stun/effect time: +" + String.format("%.1f", bonusStun) + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Bear Paw final damage: +" + String.format("%.1f", bpDmg * 100) + "%");
                lines.add(new int[]{TEXT_VALUE});
            }
            case PHOENIX_WINGS_MASTERY -> {
                float bonusHeal = level * 0.5f;
                texts.add("Bonus healing: +" + String.format("%.1f", bonusHeal) + "%");
                lines.add(new int[]{TEXT_VALUE});
                if (level >= 10) {
                    texts.add("Lifesteal hits: +1");
                    lines.add(new int[]{TEXT_VALUE});
                }
            }
            // --- Tier 3 (+1) ---
            case TIGER_CLAW_MASTERY_2 -> {
                texts.add("Tiger Claw max hits: +1");
                lines.add(new int[]{TEXT_VALUE});
                float tcDmg = getTigerClawMasteryDmg(level);
                texts.add("Tiger Claw final damage: +" + String.format("%.1f", tcDmg * 100) + "%");
                lines.add(new int[]{TEXT_VALUE});
            }
            case TURTLE_SHELL_MASTERY_2 -> {
                float bonusShield = level * 0.5f;
                texts.add("Bonus shield: +" + String.format("%.1f", bonusShield) + "%");
                lines.add(new int[]{TEXT_VALUE});
            }
            case BEAR_PAW_MASTERY_2 -> {
                float bonusStun = level * 0.1f;
                float bpDmg = getBearPawMasteryDmg(level);
                texts.add("Bonus stun/effect time: +" + String.format("%.1f", bonusStun) + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Bear Paw final damage: +" + String.format("%.1f", bpDmg * 100) + "%");
                lines.add(new int[]{TEXT_VALUE});
            }
            case PHOENIX_WINGS_MASTERY_2 -> {
                float bonusHeal2 = level * 0.5f;
                texts.add("Bonus healing: +" + String.format("%.1f", bonusHeal2) + "%");
                lines.add(new int[]{TEXT_VALUE});
                if (level >= 20) {
                    texts.add("Lifesteal hits: +1");
                    lines.add(new int[]{TEXT_VALUE});
                }
            }
            case MASTER_OF_NATURE -> {
                texts.add("Enhanced skill damage: +" + level + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Applies when Power of Nature is consumed");
                lines.add(new int[]{TEXT_DIM});
            }
            default -> {}
        }
    }

    // ========== EXECUTION ==========

    public static void execute(ServerPlayer player, PlayerStats stats,
                               SkillData sd, SkillType skill, int level) {
        switch (skill) {
            case TIGER_CLAW -> executeTigerClaw(player, stats, sd, level);
            case TURTLE_SHELL -> executeTurtleShell(player, stats, sd, level);
            case BEAR_PAW -> executeBearPaw(player, stats, sd, level);
            case PHOENIX_WINGS -> executePhoenixWings(player, stats, sd, level);
            case POWER_OF_NATURE -> executePowerOfNature(player, stats, sd, level);
            default -> {}
        }
    }

    private static void executeTigerClaw(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.TIGER_CLAW.getMpCost(level));

        boolean enhanced = consumePowerOfNature(sd);
        sd.setBmActiveBuff(SkillType.TIGER_CLAW);
        sd.setBmBuffTicks(80);
        sd.setBmEnhanced(enhanced);

        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 10, 0.5, ParticleTypes.CRIT);
            SkillSounds.playAt(player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.7f, 1.3f);
        }

        applyGlobalCooldown(sd, SkillType.TIGER_CLAW);
        reducePowerOfNatureCooldown(sd);
        sd.startCooldown(SkillType.TIGER_CLAW, level);

        String name = enhanced ? "Enhanced Tiger Claw" : "Tiger Claw";
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a76" + name + " ready! Next melee hits extra times."));
    }

    private static void executeTurtleShell(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.TURTLE_SHELL.getMpCost(level));

        boolean enhanced = consumePowerOfNature(sd);
        float shieldPct = getShieldPct(level);
        int tsmLv = sd.getLevel(SkillType.TURTLE_SHELL_MASTERY)
                 + sd.getLevel(SkillType.TURTLE_SHELL_MASTERY_2);
        if (tsmLv > 0) shieldPct += tsmLv * 0.005f;
        if (enhanced) {
            shieldPct *= 2;
            int monLv = sd.getLevel(SkillType.MASTER_OF_NATURE);
            if (monLv > 0) shieldPct *= (1.0f + monLv * 0.01f);
        }
        float shieldAmount = player.getMaxHealth() * shieldPct;

        // Turtle Shell is instant — clears any active next-attack buff
        sd.setBmActiveBuff(null);
        sd.setBmBuffTicks(0);
        sd.setBmEnhanced(false);

        // Apply absorption shield
        player.setAbsorptionAmount(shieldAmount);
        sd.setTurtleShellTicks(100); // 5 seconds

        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.ring(sl, player.getX(), player.getY() + 1, player.getZ(), 1.5, 12, ParticleTypes.ENCHANTED_HIT);
            SkillSounds.playAt(player, SoundEvents.SHIELD_BLOCK, 0.8f, 1.0f);
        }

        applyGlobalCooldown(sd, SkillType.TURTLE_SHELL);
        reducePowerOfNatureCooldown(sd);
        sd.startCooldown(SkillType.TURTLE_SHELL, level);

        String name = enhanced ? "Enhanced Turtle Shell" : "Turtle Shell";
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a76" + name + "! +" + String.format("%.1f", shieldAmount) + " shield."));
    }

    private static void executeBearPaw(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.BEAR_PAW.getMpCost(level));

        boolean enhanced = consumePowerOfNature(sd);
        sd.setBmActiveBuff(SkillType.BEAR_PAW);
        sd.setBmBuffTicks(80);
        sd.setBmEnhanced(enhanced);

        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 8, 0.4, ParticleTypes.ENCHANTED_HIT);
            SkillSounds.playAt(player, SoundEvents.RAVAGER_ROAR, 0.5f, 1.5f);
        }

        applyGlobalCooldown(sd, SkillType.BEAR_PAW);
        reducePowerOfNatureCooldown(sd);
        sd.startCooldown(SkillType.BEAR_PAW, level);

        String name = enhanced ? "Enhanced Bear Paw" : "Bear Paw";
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a76" + name + " ready! Next melee stuns."));
    }

    private static void executePhoenixWings(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.PHOENIX_WINGS.getMpCost(level));

        boolean enhanced = consumePowerOfNature(sd);
        float burstHealPct = getBurstHealPct(level);
        int pwmLv = sd.getLevel(SkillType.PHOENIX_WINGS_MASTERY);
        int pwm2Lv = sd.getLevel(SkillType.PHOENIX_WINGS_MASTERY_2);
        int pwmTotal = pwmLv + pwm2Lv;
        if (pwmTotal > 0) burstHealPct += pwmTotal * 0.005f;
        float lifestealPct = getLifestealPct(level);
        if (enhanced) {
            burstHealPct *= 2;
            lifestealPct *= 2;
            // Master of Nature: +1% enhanced effect per level
            int monLv = sd.getLevel(SkillType.MASTER_OF_NATURE);
            if (monLv > 0) {
                float monBonus = 1.0f + monLv * 0.01f;
                burstHealPct *= monBonus;
                lifestealPct *= monBonus;
            }
        }

        // Burst heal
        float healAmount = player.getMaxHealth() * burstHealPct;
        player.heal(healAmount);
        CombatLog.heal(player, enhanced ? "Enhanced Phoenix Wings" : "Phoenix Wings", healAmount);

        // Regen I for 4s
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, false, true));

        // Set lifesteal hits (base 2, +1 at max T2, +1 at max T3)
        int lifestealHits = 2;
        if (pwmLv >= 10) lifestealHits++;
        if (pwm2Lv >= 20) lifestealHits++;
        sd.setPhoenixLifestealHits(lifestealHits);
        sd.setPhoenixLifestealPct(lifestealPct);
        sd.setPhoenixLifestealEnhanced(enhanced);

        // Phoenix Wings is also a next-attack buff (lifesteal part)
        sd.setBmActiveBuff(SkillType.PHOENIX_WINGS);
        sd.setBmBuffTicks(80);
        sd.setBmEnhanced(enhanced);

        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 15, 0.6, ParticleTypes.HEART);
            SkillParticles.ring(sl, player.getX(), player.getY() + 0.5, player.getZ(), 2.0, 10, ParticleTypes.FLAME);
            SkillSounds.playAt(player, SoundEvents.CHICKEN_AMBIENT, 0.9f, 0.8f);
            SkillSounds.playAt(player, SoundEvents.FIRECHARGE_USE, 0.6f, 1.3f);
        }

        applyGlobalCooldown(sd, SkillType.PHOENIX_WINGS);
        reducePowerOfNatureCooldown(sd);
        sd.startCooldown(SkillType.PHOENIX_WINGS, level);

        String name = enhanced ? "Enhanced Phoenix Wings" : "Phoenix Wings";
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a76" + name + "! Healed " + String.format("%.1f", healAmount) + " HP. Lifesteal active."));
    }

    private static void executePowerOfNature(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.POWER_OF_NATURE.getMpCost(level));

        sd.setPowerOfNatureActive(true);

        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 20, 0.8, ParticleTypes.HAPPY_VILLAGER);
            SkillParticles.ring(sl, player.getX(), player.getY() + 0.1, player.getZ(), 2.5, 15, ParticleTypes.COMPOSTER);
            SkillSounds.playAt(player, SoundEvents.BEACON_ACTIVATE, 0.6f, 1.5f);
        }

        sd.startCooldown(SkillType.POWER_OF_NATURE, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a72Power of Nature! Next skill effect doubled."));
    }

    // ========== HELPERS ==========

    /** Put all other basic skills on 5-tick global cooldown. */
    private static void applyGlobalCooldown(SkillData sd, SkillType used) {
        for (SkillType basic : BASIC_SKILLS) {
            if (basic != used && !sd.isOnCooldown(basic)) {
                sd.setCooldown(basic, 5);
            }
        }
    }

    /** Reduce Power of Nature cooldown by 40 ticks (2 seconds). */
    private static void reducePowerOfNatureCooldown(SkillData sd) {
        int remaining = sd.getCooldownRemaining(SkillType.POWER_OF_NATURE);
        if (remaining > 0) {
            sd.setCooldown(SkillType.POWER_OF_NATURE, Math.max(0, remaining - 40));
        }
    }

    /** Check and consume Power of Nature buff. Returns true if it was active. */
    private static boolean consumePowerOfNature(SkillData sd) {
        if (sd.isPowerOfNatureActive()) {
            sd.setPowerOfNatureActive(false);
            return true;
        }
        return false;
    }

    // ========== SCALING ==========

    /** Extra hits: 2 at lv1, 3 at lv10. */
    public static int getExtraHits(int level) {
        return level >= 10 ? 3 : 2;
    }

    /** Per-hit damage %: 10% at lv1, 30% at lv10. */
    public static float getHitDamagePct(int level) {
        return 0.10f + (level - 1) * (0.20f / 9);
    }

    /** Shield %: 5% at lv1, 20% at lv10. */
    public static float getShieldPct(int level) {
        return 0.05f + (level - 1) * (0.15f / 9);
    }

    /** Stun duration in seconds: 0.5s at lv1, 2s at lv10. */
    public static float getStunDuration(int level) {
        return 0.5f + (level - 1) * (1.5f / 9);
    }

    /** Burst heal %: 1% at lv1, 10% at lv10. */
    public static float getBurstHealPct(int level) {
        return 0.01f + (level - 1) * (0.09f / 9);
    }

    /** Lifesteal %: 1% at lv1, 5% at lv10. */
    public static float getLifestealPct(int level) {
        return 0.01f + (level - 1) * (0.04f / 9);
    }

    // ========== MASTERY SCALING ==========

    /** Tiger Claw Mastery: bonus max hits. Always +1 when unlocked. */
    public static int getTigerClawMasteryHits(int level) {
        return level > 0 ? 1 : 0;
    }

    /** Tiger Claw Mastery: Tiger Claw final damage bonus. 0.5% at lv1, 5% at lv10. */
    public static float getTigerClawMasteryDmg(int level) {
        if (level <= 0) return 0;
        return 0.005f + (level - 1) * (0.045f / 9);
    }

    /** Bear Paw Mastery: Bear Paw final damage bonus. 1% at lv1, 10% at lv10. */
    public static float getBearPawMasteryDmg(int level) {
        if (level <= 0) return 0;
        return 0.01f + (level - 1) * (0.09f / 9);
    }

    // ========== MIRROR (Shadow Partner) ==========

    public static void mirrorSkill(ServerLevel sl, ShadowPartnerEntity partner,
                                   ServerPlayer player, PlayerStats stats,
                                   SkillType skill, int level, float multiplier) {
        // Beast Master skills are self-buffs — just play a generic mirror effect
        Vec3 pos = partner.position();
        SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 8, 0.4, ParticleTypes.PORTAL);
        partner.swing(InteractionHand.MAIN_HAND);
    }

    // ========== TOGGLE DEACTIVATION ==========

    public static void deactivateToggle(ServerPlayer player, SkillData sd, SkillType skill) {
        // Beast Master has no toggle skills
        sd.setToggleActive(skill, false);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a77" + skill.getDisplayName() + " deactivated."));
    }
}
