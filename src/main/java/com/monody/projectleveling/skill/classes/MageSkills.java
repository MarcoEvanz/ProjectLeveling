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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.monody.projectleveling.skill.SkillTooltips.TEXT_DIM;
import static com.monody.projectleveling.skill.SkillTooltips.TEXT_VALUE;

public final class MageSkills {

    public static final SkillType[] ALL = {
            SkillType.FLAME_ORB, SkillType.MAGIC_GUARD, SkillType.ELEMENTAL_DRAIN,
            SkillType.FROST_BIND, SkillType.POISON_MIST, SkillType.ELEMENT_AMPLIFICATION,
            SkillType.MIST_ERUPTION, SkillType.INFINITY, SkillType.ARCANE_OVERDRIVE,
    };

    private MageSkills() {}

    // ========== TOOLTIPS ==========

    public static void addDetailLines(List<String> texts, List<int[]> lines,
                                       SkillType skill, int level, PlayerStats stats) {
        switch (skill) {
            // === T1 ===
            case FLAME_ORB -> {
                float dmg = 2 + level * 0.6f + stats.getIntelligence() * 0.12f;
                double radius = 3 + level * 0.1;
                int fireDur = 3 + level / 3;
                texts.add("Damage: " + String.format("%.1f", dmg) + " (INT scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("AoE radius: " + String.format("%.1f", radius) + " blocks");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Sets fire: " + fireDur + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Projectile, explodes on impact");
                lines.add(new int[]{TEXT_DIM});
            }
            case MAGIC_GUARD -> {
                float redirect = 0.3f + level * 0.03f;
                texts.add("Dmg to MP: " + String.format("%.0f", redirect * 100) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP drain: " + skill.getToggleMpPerSecond(level) + "/sec");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Remaining damage still hurts HP");
                lines.add(new int[]{TEXT_DIM});
            }
            case ELEMENTAL_DRAIN -> {
                texts.add("Bonus per debuff: +" + (level * 5) + "% dmg");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Max bonus: +25%");
                lines.add(new int[]{TEXT_VALUE});
            }

            // === T2 ===
            case FROST_BIND -> {
                double range = 5 + level * 0.15 + stats.getIntelligence() * 0.05;
                float dmg = 1 + level * 0.3f + stats.getIntelligence() * 0.08f;
                int freezeDur = 3 + level / 3;
                texts.add("Damage: " + String.format("%.1f", dmg) + " (INT scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Range: " + String.format("%.1f", range) + " blocks (INT scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Freeze: " + freezeDur + "s (Slowness IV + Weakness I)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Pulls enemies inward");
                lines.add(new int[]{TEXT_DIM});
            }
            case POISON_MIST -> {
                int mistDur = 8 + level / 3;
                float tickDmg = 0.5f + level * 0.1f + stats.getIntelligence() * 0.03f;
                double radius = 4 + level * 0.1;
                texts.add("Duration: " + mistDur + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("DPS: " + String.format("%.1f", tickDmg) + "/sec (INT scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Radius: " + String.format("%.1f", radius) + " blocks");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Applies: Poison I, stationary zone");
                lines.add(new int[]{TEXT_DIM});
            }
            case ELEMENT_AMPLIFICATION -> {
                texts.add("Skill damage: +" + (level * 3) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP cost: +20% for all skills");
                lines.add(new int[]{TEXT_DIM});
            }

            // === T3 ===
            case MIST_ERUPTION -> {
                float detDmg = 4 + level * 0.8f + stats.getIntelligence() * 0.2f;
                double detRadius = 5 + level * 0.15;
                float blastDmg = 2 + level * 0.4f + stats.getIntelligence() * 0.1f;
                texts.add("With Mist: " + String.format("%.1f", detDmg) + " dmg (INT scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("  Radius: " + String.format("%.1f", detRadius) + " blocks + fire");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("No Mist: " + String.format("%.1f", blastDmg) + " dmg, 4 block range");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Detonates Poison Mist for bonus damage");
                lines.add(new int[]{TEXT_DIM});
            }
            case INFINITY -> {
                int dur = 20 + level;
                texts.add("Duration: " + dur + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("All skills cost 0 MP while active");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Every 4s: Strength I + 3% max HP heal");
                lines.add(new int[]{TEXT_VALUE});
            }
            case ARCANE_OVERDRIVE -> {
                texts.add("Crit rate: +" + String.format("%.1f", level * 1.5) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Crit damage: +" + level + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Armor pen: +" + level + "%");
                lines.add(new int[]{TEXT_VALUE});
            }
            default -> {}
        }
    }

    // ========== EXECUTION ==========

    public static void execute(ServerPlayer player, PlayerStats stats,
                               SkillData sd, SkillType skill, int level) {
        switch (skill) {
            case FLAME_ORB -> executeFlameOrb(player, stats, sd, level);
            case MAGIC_GUARD -> executeMagicGuard(player, stats, sd, level);
            case FROST_BIND -> executeFrostBind(player, stats, sd, level);
            case POISON_MIST -> executePoisonMist(player, stats, sd, level);
            case MIST_ERUPTION -> executeMistEruption(player, stats, sd, level);
            case INFINITY -> executeInfinity(player, stats, sd, level);
            default -> {}
        }
    }

    private static void executeFlameOrb(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.FLAME_ORB.getMpCost(level));
        float damage = 2 + level * 0.6f + stats.getIntelligence() * 0.12f + stats.getMagicAttack();
        float aoeRadius = (float) (3 + level * 0.1);
        Vec3 look = player.getLookAngle();
        SkillFireballEntity fireball = new SkillFireballEntity(
                player.level(), player,
                look.x * 0.5, look.y * 0.5, look.z * 0.5,
                SkillFireballEntity.FireballType.FLAME_ORB, damage, aoeRadius, level);
        fireball.setPos(player.getX() + look.x, player.getEyeY(), player.getZ() + look.z);
        player.level().addFreshEntity(fireball);
        SkillSounds.playAt(player, SoundEvents.BLAZE_SHOOT, 0.8f, 1.0f);
        sd.startCooldown(SkillType.FLAME_ORB, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a76Flame Orb launched!"));
    }

    private static void executeMagicGuard(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        if (stats.getCurrentMp() < SkillType.MAGIC_GUARD.getToggleMpPerSecond(level)) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough MP!"));
            return;
        }
        sd.setToggleActive(SkillType.MAGIC_GUARD, true);
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.ring(sl, player.getX(), player.getY() + 1, player.getZ(), 1.0, 12, ParticleTypes.ENCHANTED_HIT);
            SkillSounds.playAt(player, SoundEvents.ENCHANTMENT_TABLE_USE, 0.3f, 1.0f);
        }
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a79Magic Guard activated. Damage redirected to MP."));
    }

    /** Get the damage-to-MP redirect ratio for Magic Guard (30-60%). */
    public static float getMagicGuardRedirectRatio(int level) {
        return 0.3f + level * 0.03f; // 33% at lv1, 60% at lv10
    }

    private static void executeFrostBind(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.FROST_BIND.getMpCost(level));
        double range = 5 + level * 0.15 + stats.getIntelligence() * 0.05;
        float damage = 1 + level * 0.3f + stats.getIntelligence() * 0.08f + stats.getMagicAttack();
        int freezeDuration = (3 + level / 3) * 20;
        AABB area = player.getBoundingBox().inflate(range);
        List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
        for (Monster mob : mobs) {
            // Pull inward
            Vec3 pullDir = player.position().subtract(mob.position()).normalize();
            mob.setDeltaMovement(pullDir.scale(0.4));
            mob.hurtMarked = true;
            // Freeze + damage
            mob.hurt(SkillDamageSource.get(player.level()), damage);
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, freezeDuration, 3, false, true));
            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, freezeDuration, 0, false, true));
        }
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 25, range * 0.5, ParticleTypes.SNOWFLAKE);
            for (Monster mob : mobs) {
                SkillParticles.burst(sl, mob.getX(), mob.getY() + 1, mob.getZ(), 5, 0.3, ParticleTypes.ITEM_SNOWBALL);
            }
            SkillSounds.playAt(player, SoundEvents.GLASS_BREAK, 0.7f, 0.5f);
        }
        CombatLog.aoeSkill(player, "Frost Bind", damage, mobs);
        sd.startCooldown(SkillType.FROST_BIND, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7bFrost Bind! " + mobs.size() + " enemies frozen."));
    }

    private static void executePoisonMist(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.POISON_MIST.getMpCost(level));
        int duration = (8 + level / 3) * 20;
        sd.setMistActive(true);
        sd.setMistTicks(duration);
        sd.setMistPos(player.getX(), player.getY(), player.getZ());
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 0.5, player.getZ(), 20, 1.5, ParticleTypes.ITEM_SLIME);
            SkillParticles.column(sl, player.getX(), player.getZ(), player.getY(), player.getY() + 2, 2.0, 15, ParticleTypes.WITCH);
            SkillSounds.playAt(player, SoundEvents.BREWING_STAND_BREW, 0.6f, 1.0f);
        }
        sd.startCooldown(SkillType.POISON_MIST, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a72Poison Mist deployed!"));
    }

    /** Called every second while mist is active. */
    public static void tickPoisonMist(ServerPlayer player, PlayerStats stats, SkillData sd) {
        int level = sd.getLevel(SkillType.POISON_MIST);
        double radius = 4 + level * 0.1;
        float damage = 0.5f + level * 0.1f + stats.getIntelligence() * 0.03f + stats.getMagicAttack() * 0.2f;
        AABB area = new AABB(
                sd.getMistX() - radius, sd.getMistY() - radius, sd.getMistZ() - radius,
                sd.getMistX() + radius, sd.getMistY() + radius, sd.getMistZ() + radius);
        List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
        for (Monster mob : mobs) {
            mob.hurt(SkillDamageSource.get(player.level()), damage);
            mob.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0, false, false));
        }
        CombatLog.aoeSkill(player, "Poison Mist", damage, mobs);
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.column(sl, sd.getMistX(), sd.getMistZ(), sd.getMistY(), sd.getMistY() + 2.0, radius, 15, ParticleTypes.WITCH);
            SkillParticles.disc(sl, sd.getMistX(), sd.getMistY() + 0.3, sd.getMistZ(), radius, 10, ParticleTypes.ITEM_SLIME);
        }
    }

    private static void executeMistEruption(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.MIST_ERUPTION.getMpCost(level));
        if (sd.isMistActive()) {
            // Detonate the mist for massive damage
            double radius = 5 + level * 0.15;
            float damage = 4 + level * 0.8f + stats.getIntelligence() * 0.2f + stats.getMagicAttack();
            AABB area = new AABB(
                    sd.getMistX() - radius, sd.getMistY() - radius, sd.getMistZ() - radius,
                    sd.getMistX() + radius, sd.getMistY() + radius, sd.getMistZ() + radius);
            List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
            for (Monster mob : mobs) {
                mob.hurt(SkillDamageSource.get(player.level()), damage);
                mob.setSecondsOnFire(3);
            }
            CombatLog.aoeSkill(player, "Mist Eruption", damage, mobs);
            sd.setMistActive(false);
            sd.setMistTicks(0);
            if (player.level() instanceof ServerLevel sl) {
                SkillParticles.explosion(sl, sd.getMistX(), sd.getMistY() + 1, sd.getMistZ(), radius, ParticleTypes.FLAME, ParticleTypes.LAVA);
                SkillParticles.burst(sl, sd.getMistX(), sd.getMistY() + 1, sd.getMistZ(), 20, radius * 0.5, ParticleTypes.LARGE_SMOKE);
                SkillSounds.playAt(sl, sd.getMistX(), sd.getMistY(), sd.getMistZ(), SoundEvents.GENERIC_EXPLODE, 1.0f, 1.0f);
            }
            sd.startCooldown(SkillType.MIST_ERUPTION, level);
            player.sendSystemMessage(Component.literal(
                    "\u00a7b[System]\u00a7r \u00a76Mist Eruption! " + mobs.size() + " enemies blasted!"));
        } else {
            // Weaker arcane blast without mist
            float damage = 2 + level * 0.4f + stats.getIntelligence() * 0.1f + stats.getMagicAttack();
            double range = 4;
            AABB area = player.getBoundingBox().inflate(range);
            List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
            for (Monster mob : mobs) {
                mob.hurt(SkillDamageSource.get(player.level()), damage);
            }
            CombatLog.aoeSkill(player, "Arcane Blast", damage, mobs);
            if (player.level() instanceof ServerLevel sl) {
                SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 15, range * 0.4, ParticleTypes.ENCHANTED_HIT);
                SkillSounds.playAt(player, SoundEvents.ENCHANTMENT_TABLE_USE, 0.6f, 1.0f);
            }
            sd.startCooldown(SkillType.MIST_ERUPTION, level);
            player.sendSystemMessage(Component.literal(
                    "\u00a7b[System]\u00a7r \u00a7dArcane Blast! " + mobs.size() + " enemies hit."));
        }
    }

    private static void executeInfinity(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.INFINITY.getMpCost(level));
        int duration = (20 + level) * 20; // 21-40 seconds
        sd.setInfinityTicks(duration);
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.spiral(sl, player.getX(), player.getY(), player.getZ(), 1.2, 3.0, 3, 12, ParticleTypes.DRAGON_BREATH);
            SkillParticles.sphere(sl, player.getX(), player.getY() + 1, player.getZ(), 1.5, 15, ParticleTypes.END_ROD);
            SkillSounds.playAt(player, SoundEvents.ENDER_DRAGON_GROWL, 0.3f, 1.5f);
        }
        sd.startCooldown(SkillType.INFINITY, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a75Infinity activated! All skills cost 0 MP."));
    }

    /** Called every 4 seconds while Infinity is active to ramp damage buff. */
    public static void tickInfinity(ServerPlayer player, SkillData sd) {
        // Ramp damage: +5% per 4 seconds, shown as Strength buff
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 0, false, true));
        // Regen HP/MP
        float infinityHeal = player.getMaxHealth() * 0.03f;
        player.heal(infinityHeal);
        CombatLog.heal(player, "Infinity", infinityHeal);
        SkillParticles.playerAura(player, 8, 1.0, ParticleTypes.END_ROD);
        SkillParticles.playerFeet(player, 6, 0.8, ParticleTypes.WITCH);
    }

    // ========== TOGGLE DEACTIVATION ==========

    public static void deactivateToggle(ServerPlayer player, SkillData sd, SkillType skill) {
        sd.setToggleActive(skill, false);
        switch (skill) {
            case MAGIC_GUARD ->
                    player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Magic Guard deactivated."));
            default -> player.sendSystemMessage(Component.literal(
                    "\u00a7b[System]\u00a7r \u00a77" + skill.getDisplayName() + " deactivated."));
        }
    }

    // ========== MIRROR ==========

    public static void mirrorSkill(ServerLevel sl, ShadowPartnerEntity partner,
                                   ServerPlayer player, PlayerStats stats,
                                   SkillType skill, int level, float multiplier) {
        Vec3 pos = partner.position();
        Vec3 look = player.getLookAngle();
        switch (skill) {
            case FLAME_ORB -> {
                float dmg = (3 + level * 1.2f + stats.getIntelligence() * 0.2f + stats.getMagicAttack()) * multiplier;
                SkillFireballEntity fb = new SkillFireballEntity(sl, partner,
                        look.x, look.y, look.z,
                        SkillFireballEntity.FireballType.FLAME_ORB, dmg, 3.0f, level);
                sl.addFreshEntity(fb);
            }
            case FROST_BIND -> {
                float radius = 4 + level * 0.2f;
                float dmg = (3 + level * 0.8f + stats.getMagicAttack()) * multiplier;
                List<Monster> mobs = sl.getEntitiesOfClass(Monster.class,
                        partner.getBoundingBox().inflate(radius));
                for (Monster mob : mobs) {
                    mob.hurt(SkillDamageSource.get(player.level()), dmg);
                    mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60 + level * 4, 4, false, true));
                }
                CombatLog.aoeSkill(player, "Shadow Frost Bind", dmg, mobs);
                SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 20, radius * 0.5, ParticleTypes.SNOWFLAKE);
            }
            case POISON_MIST -> {
                float dmg = (0.5f + level * 0.1f + stats.getIntelligence() * 0.03f + stats.getMagicAttack() * 0.2f) * multiplier;
                List<Monster> mobs = sl.getEntitiesOfClass(Monster.class,
                        partner.getBoundingBox().inflate(4));
                for (Monster mob : mobs) {
                    mob.hurt(SkillDamageSource.get(player.level()), dmg);
                    mob.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0, false, true));
                }
                CombatLog.aoeSkill(player, "Shadow Poison Mist", dmg, mobs);
                SkillParticles.burst(sl, pos.x, pos.y + 0.5, pos.z, 12, 1.0, ParticleTypes.ITEM_SLIME);
            }
            case MIST_ERUPTION -> {
                float radius = 4 + level * 0.1f;
                float dmg = (2 + level * 0.5f + stats.getIntelligence() * 0.1f + stats.getMagicAttack()) * multiplier;
                List<Monster> mobs = sl.getEntitiesOfClass(Monster.class,
                        partner.getBoundingBox().inflate(radius));
                for (Monster mob : mobs) {
                    mob.hurt(SkillDamageSource.get(player.level()), dmg);
                }
                CombatLog.aoeSkill(player, "Shadow Eruption", dmg, mobs);
                SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 15, radius * 0.4, ParticleTypes.ENCHANTED_HIT);
            }
            default -> {
                SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 8, 0.4, ParticleTypes.PORTAL);
                partner.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
        }
    }
}
