package com.monody.projectleveling.skill.classes;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.entity.assassin.ShadowPartnerEntity;
import com.monody.projectleveling.entity.mage.SkillFireballEntity;
import com.monody.projectleveling.skill.*;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.monody.projectleveling.skill.SkillTooltips.TEXT_DIM;
import static com.monody.projectleveling.skill.SkillTooltips.TEXT_VALUE;

public final class HealerSkills {

    public static final SkillType[] ALL = {
            SkillType.HOLY_LIGHT, SkillType.BLESS, SkillType.MP_RECOVERY,
            SkillType.HOLY_SHELL, SkillType.DISPEL, SkillType.DIVINE_PROTECTION,
            SkillType.BENEDICTION, SkillType.ANGEL_RAY, SkillType.BLESSED_ENSEMBLE,
    };

    private HealerSkills() {}

    // ================================================================
    // Tooltips
    // ================================================================

    public static void addDetailLines(List<String> texts, List<int[]> lines,
                                       SkillType skill, int level, PlayerStats stats) {
        switch (skill) {
            // === T1 ===
            case HOLY_LIGHT -> {
                float heal = 2 + level * 0.8f + stats.getFaith() * 0.15f + stats.getHealingPower();
                float undeadDmg = 1 + level * 0.5f + stats.getFaith() * 0.1f + stats.getHealingPower();
                texts.add("Heal: " + String.format("%.1f", heal) + " HP (FAI scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Undead dmg: " + String.format("%.1f", undeadDmg) + " (FAI scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Range: 6 blocks (self + allies)");
                lines.add(new int[]{TEXT_DIM});
            }
            case BLESS -> {
                int strAmp = Math.min(level / 4, 1);
                int dur = 30 + level * 3;
                texts.add("Duration: " + dur + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Buffs: Strength " + SkillTooltips.toRoman(strAmp + 1) + ", Resistance I, Regen I");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Range: 8 blocks (self + allies)");
                lines.add(new int[]{TEXT_DIM});
            }
            case MP_RECOVERY -> {
                int mpOnKill = 2 + level;
                texts.add("MP regen: +" + String.format("%.1f", level * 0.2) + "%/sec");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP on kill: +" + mpOnKill);
                lines.add(new int[]{TEXT_VALUE});
            }

            // === T2 ===
            case HOLY_SHELL -> {
                float absorb = 2 + level * 0.4f + stats.getFaith() * 0.05f + stats.getHealingPower();
                texts.add("Absorption: " + String.format("%.1f", absorb) + " HP (FAI scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Range: 6 blocks (self + allies)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Cleanses: Poison, Wither");
                lines.add(new int[]{TEXT_DIM});
            }
            case DISPEL -> {
                texts.add("Self: Remove Poison, Wither, Weakness,");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("  Slowness, Mining Fatigue, Blindness");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Allies: Remove Poison, Wither, Weakness,");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("  Slowness (8 block range)");
                lines.add(new int[]{TEXT_VALUE});
            }
            case DIVINE_PROTECTION -> {
                texts.add("Status resist: +" + (level * 3) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Auto-cleanse chance on debuff");
                lines.add(new int[]{TEXT_DIM});
            }

            // === T3 ===
            case BENEDICTION -> {
                int dur = 15 + level;
                double radius = 4 + level * 0.2 + stats.getFaith() * 0.05;
                float tickDmg = 0.5f + level * 0.1f + stats.getFaith() * 0.03f + stats.getHealingPower() * 0.2f;
                texts.add("Duration: " + dur + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Radius: " + String.format("%.1f", radius) + " blocks (FAI scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Allies: Regen II + Strength I each sec");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Enemies: " + String.format("%.1f", tickDmg) + " DPS + Slowness I");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Stationary zone");
                lines.add(new int[]{TEXT_DIM});
            }
            case ANGEL_RAY -> {
                float dmg = 1.5f + level * 0.4f + stats.getFaith() * 0.1f + stats.getHealingPower();
                float heal = dmg * 0.3f;
                texts.add("Damage: " + String.format("%.1f", dmg) + " (FAI scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("AoE radius: 3 blocks on impact");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Heals allies: " + String.format("%.1f", heal) + " HP");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Holy projectile, damages + heals");
                lines.add(new int[]{TEXT_DIM});
            }
            case BLESSED_ENSEMBLE -> {
                texts.add("Damage: +" + (level * 3) + "% per nearby player");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("XP bonus: +" + (level * 5) + "% per nearby player");
                lines.add(new int[]{TEXT_VALUE});
            }
            default -> {}
        }
    }

    // ================================================================
    // Execute dispatcher
    // ================================================================

    public static void execute(ServerPlayer player, PlayerStats stats, SkillData sd, SkillType skill, int level) {
        switch (skill) {
            case HOLY_LIGHT -> executeHolyLight(player, stats, sd, level);
            case BLESS -> executeBless(player, stats, sd, level);
            case HOLY_SHELL -> executeHolyShell(player, stats, sd, level);
            case DISPEL -> executeDispel(player, stats, sd, level);
            case BENEDICTION -> executeBenediction(player, stats, sd, level);
            case ANGEL_RAY -> executeAngelRay(player, stats, sd, level);
            default -> {}
        }
    }

    // ================================================================
    // Execution methods
    // ================================================================

    private static void executeHolyLight(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.HOLY_LIGHT.getMpCost(level));
        float healAmount = 2 + level * 0.8f + stats.getFaith() * 0.15f + stats.getHealingPower();
        double range = 6;
        // Heal self
        player.heal(healAmount);
        // Heal nearby players
        AABB area = player.getBoundingBox().inflate(range);
        List<ServerPlayer> nearby = player.level().getEntitiesOfClass(ServerPlayer.class, area, p -> p != player);
        for (ServerPlayer p : nearby) {
            p.heal(healAmount);
        }
        // Damage undead mobs
        float undeadDamage = 1 + level * 0.5f + stats.getFaith() * 0.1f + stats.getHealingPower();
        List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area,
                m -> m.getMobType() == net.minecraft.world.entity.MobType.UNDEAD);
        for (Monster mob : mobs) {
            mob.hurt(SkillDamageSource.get(player.level()), undeadDamage);
        }
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 10, 0.6, ParticleTypes.HEART);
            SkillParticles.ring(sl, player.getX(), player.getY() + 0.5, player.getZ(), range * 0.5, 12, ParticleTypes.END_ROD);
            for (ServerPlayer p : nearby) {
                SkillParticles.burst(sl, p.getX(), p.getY() + 1.5, p.getZ(), 5, 0.3, ParticleTypes.HEART);
            }
            for (Monster mob : mobs) {
                SkillParticles.burst(sl, mob.getX(), mob.getY() + 1, mob.getZ(), 5, 0.3, ParticleTypes.ENCHANTED_HIT);
            }
            SkillSounds.playAt(player, SoundEvents.PLAYER_LEVELUP, 0.5f, 1.5f);
        }
        CombatLog.heal(player, "Holy Light", healAmount);
        CombatLog.aoeSkill(player, "Holy Light", undeadDamage, mobs);
        sd.startCooldown(SkillType.HOLY_LIGHT, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7aHoly Light! Healed " + (nearby.size() + 1)
                        + " allies. " + mobs.size() + " undead damaged."));
    }

    private static void executeBless(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.BLESS.getMpCost(level));
        int duration = (30 + level * 3) * 20; // 33-60 seconds
        int amp = Math.min(level / 4, 1);
        double range = 8;
        // Buff self
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, amp, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 0, false, true));
        // Buff nearby players
        AABB area = player.getBoundingBox().inflate(range);
        List<ServerPlayer> nearby = player.level().getEntitiesOfClass(ServerPlayer.class, area, p -> p != player);
        for (ServerPlayer p : nearby) {
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, amp, false, true));
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, true));
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 0, false, true));
        }
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 2, player.getZ(), 10, 0.4, ParticleTypes.TOTEM_OF_UNDYING);
            for (ServerPlayer p : nearby) {
                SkillParticles.column(sl, p.getX(), p.getZ(), p.getY(), p.getY() + 2.5, 0.3, 8, ParticleTypes.ENCHANTED_HIT);
            }
            SkillSounds.playAt(player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.4f, 1.0f);
        }
        sd.startCooldown(SkillType.BLESS, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7aBless! " + (nearby.size() + 1) + " allies blessed."));
    }

    private static void executeHolyShell(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.HOLY_SHELL.getMpCost(level));
        float absorb = 2 + level * 0.4f + stats.getFaith() * 0.05f + stats.getHealingPower();
        double range = 6;
        // Shield self
        player.setAbsorptionAmount(player.getAbsorptionAmount() + absorb);
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.WITHER);
        // Shield nearby players
        AABB area = player.getBoundingBox().inflate(range);
        List<ServerPlayer> nearby = player.level().getEntitiesOfClass(ServerPlayer.class, area, p -> p != player);
        for (ServerPlayer p : nearby) {
            p.setAbsorptionAmount(p.getAbsorptionAmount() + absorb);
            p.removeEffect(MobEffects.POISON);
            p.removeEffect(MobEffects.WITHER);
        }
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.sphere(sl, player.getX(), player.getY() + 1, player.getZ(), 1.2, 15, ParticleTypes.END_ROD);
            for (ServerPlayer p : nearby) {
                SkillParticles.sphere(sl, p.getX(), p.getY() + 1, p.getZ(), 1.0, 10, ParticleTypes.END_ROD);
            }
            SkillSounds.playAt(player, SoundEvents.ENCHANTMENT_TABLE_USE, 0.6f, 1.0f);
        }
        sd.startCooldown(SkillType.HOLY_SHELL, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7aHoly Shell! " + (nearby.size() + 1) + " allies shielded."));
    }

    private static void executeDispel(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.DISPEL.getMpCost(level));
        double range = 8;
        // Cleanse self
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.WITHER);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.DIG_SLOWDOWN);
        player.removeEffect(MobEffects.BLINDNESS);
        // Cleanse nearby players
        AABB area = player.getBoundingBox().inflate(range);
        List<ServerPlayer> nearby = player.level().getEntitiesOfClass(ServerPlayer.class, area, p -> p != player);
        for (ServerPlayer p : nearby) {
            p.removeEffect(MobEffects.POISON);
            p.removeEffect(MobEffects.WITHER);
            p.removeEffect(MobEffects.WEAKNESS);
            p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 12, 0.8, ParticleTypes.HAPPY_VILLAGER);
            for (ServerPlayer p : nearby) {
                SkillParticles.burst(sl, p.getX(), p.getY() + 1, p.getZ(), 6, 0.4, ParticleTypes.HAPPY_VILLAGER);
            }
            SkillSounds.playAt(player, SoundEvents.BREWING_STAND_BREW, 0.5f, 1.5f);
        }
        sd.startCooldown(SkillType.DISPEL, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7aDispel! Debuffs cleansed from " + (nearby.size() + 1) + " allies."));
    }

    private static void executeBenediction(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.BENEDICTION.getMpCost(level));
        int duration = (15 + level) * 20; // 16-35 seconds
        sd.setBenedictionTicks(duration);
        sd.setBenedictionPos(player.getX(), player.getY(), player.getZ());
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.column(sl, player.getX(), player.getZ(), player.getY(), player.getY() + 3, 0.5, 20, ParticleTypes.END_ROD);
            SkillParticles.burst(sl, player.getX(), player.getY() + 1.5, player.getZ(), 10, 0.5, ParticleTypes.HEART);
            SkillSounds.playAt(player, SoundEvents.BEACON_ACTIVATE, 0.6f, 1.0f);
        }
        sd.startCooldown(SkillType.BENEDICTION, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7aBenediction zone created!"));
    }

    private static void executeAngelRay(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.ANGEL_RAY.getMpCost(level));
        float damage = 1.5f + level * 0.4f + stats.getFaith() * 0.1f + stats.getHealingPower();
        float aoeRadius = 3.0f;
        Vec3 look = player.getLookAngle();
        SkillFireballEntity ray = new SkillFireballEntity(
                player.level(), player,
                look.x * 0.5, look.y * 0.5, look.z * 0.5,
                SkillFireballEntity.FireballType.ANGEL_RAY, damage, aoeRadius, level);
        ray.setPos(player.getX() + look.x, player.getEyeY(), player.getZ() + look.z);
        player.level().addFreshEntity(ray);
        SkillSounds.playAt(player, SoundEvents.BEACON_ACTIVATE, 0.5f, 1.5f);
        sd.startCooldown(SkillType.ANGEL_RAY, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7aAngel Ray launched!"));
    }

    // ================================================================
    // Tick methods (called while skill is active)
    // ================================================================

    /** Called every second while benediction zone is active. */
    public static void tickBenediction(ServerPlayer player, PlayerStats stats, SkillData sd) {
        int level = sd.getLevel(SkillType.BENEDICTION);
        double radius = 4 + level * 0.2 + stats.getFaith() * 0.05;
        float damage = 0.5f + level * 0.1f + stats.getFaith() * 0.03f + stats.getHealingPower() * 0.2f;
        AABB area = new AABB(
                sd.getBenedictionX() - radius, sd.getBenedictionY() - radius, sd.getBenedictionZ() - radius,
                sd.getBenedictionX() + radius, sd.getBenedictionY() + radius, sd.getBenedictionZ() + radius);
        // Heal allies inside
        List<ServerPlayer> allies = player.level().getEntitiesOfClass(ServerPlayer.class, area);
        for (ServerPlayer p : allies) {
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 30, 1, false, false));
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30, 0, false, false));
        }
        // Damage + slow enemies inside
        List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
        for (Monster mob : mobs) {
            mob.hurt(SkillDamageSource.get(player.level()), damage);
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0, false, false));
        }
        CombatLog.aoeSkill(player, "Benediction", damage, mobs);
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.ring(sl, sd.getBenedictionX(), sd.getBenedictionY() + 0.1, sd.getBenedictionZ(), radius, 12, ParticleTypes.HEART);
            SkillParticles.ring(sl, sd.getBenedictionX(), sd.getBenedictionY() + 0.5, sd.getBenedictionZ(), radius * 0.7, 8, ParticleTypes.END_ROD);
        }
    }

    // ================================================================
    // Shadow Partner mirror
    // ================================================================

    public static void mirrorSkill(ServerLevel sl, ShadowPartnerEntity partner,
                                   ServerPlayer player, PlayerStats stats,
                                   SkillType skill, int level, float multiplier) {
        Vec3 pos = partner.position();
        Vec3 look = player.getLookAngle();

        switch (skill) {
            case HOLY_LIGHT -> {
                float healAmt = (1 + level * 0.4f + stats.getFaith() * 0.08f + stats.getHealingPower()) * multiplier;
                player.heal(healAmt);
                CombatLog.heal(player, "Shadow Holy Light", healAmt);
                float undeadDmg = (0.5f + level * 0.25f + stats.getFaith() * 0.05f + stats.getHealingPower()) * multiplier;
                List<Monster> mobs = sl.getEntitiesOfClass(Monster.class,
                        partner.getBoundingBox().inflate(6),
                        m -> m.getMobType() == net.minecraft.world.entity.MobType.UNDEAD);
                for (Monster mob : mobs) {
                    mob.hurt(SkillDamageSource.get(player.level()), undeadDmg);
                }
                CombatLog.aoeSkill(player, "Shadow Holy Light", undeadDmg, mobs);
                SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 8, 0.5, ParticleTypes.HEART);
            }
            case BENEDICTION -> {
                SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 8, 0.4, ParticleTypes.HEART);
                SkillParticles.column(sl, pos.x, pos.z, pos.y, pos.y + 2, 0.3, 8, ParticleTypes.END_ROD);
            }
            case ANGEL_RAY -> {
                float dmg = (2 + level * 0.8f + stats.getFaith() * 0.15f + stats.getHealingPower()) * multiplier;
                SkillFireballEntity ray = new SkillFireballEntity(sl, partner,
                        look.x, look.y, look.z,
                        SkillFireballEntity.FireballType.ANGEL_RAY, dmg, 3.0f, level);
                sl.addFreshEntity(ray);
            }

            // All other healer skills: visual mirror (particles + swing)
            default -> {
                SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 8, 0.4, ParticleTypes.PORTAL);
                partner.swing(InteractionHand.MAIN_HAND);
            }
        }
    }
}
