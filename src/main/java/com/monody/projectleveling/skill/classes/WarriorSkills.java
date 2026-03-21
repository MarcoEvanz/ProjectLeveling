package com.monody.projectleveling.skill.classes;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.entity.assassin.ShadowPartnerEntity;
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

public final class WarriorSkills {

    public static final SkillType[] ALL = {
            SkillType.BLOODLUST, SkillType.WAR_CRY, SkillType.WEAPON_MASTERY,
            SkillType.IRON_WILL, SkillType.GROUND_SLAM, SkillType.RAGE,
            SkillType.DOMAIN_OF_MONARCH, SkillType.UNBREAKABLE, SkillType.BERSERKER_SPIRIT,
    };

    private WarriorSkills() {}

    // ========== TOOLTIPS ==========

    public static void addDetailLines(List<String> texts, List<int[]> lines,
                                       SkillType skill, int level, PlayerStats stats) {
        switch (skill) {
            case BLOODLUST -> {
                double range = 4 + level * 0.8 + stats.getStrength() * 0.3;
                int dur = (int) (3 + level * 0.5);
                texts.add("Range: " + String.format("%.1f", range) + " blocks (STR scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Duration: " + dur + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Applies: Slowness II, Weakness I");
                lines.add(new int[]{TEXT_DIM});
            }
            case WAR_CRY -> {
                int strAmp = Math.min((level - 1) / 3, 2);
                int dur = 10 + level;
                texts.add("Strength: " + SkillTooltips.toRoman(strAmp + 1) + " for " + dur + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Range: 6 blocks (self + allies)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Applies Weakness I to enemies in range");
                lines.add(new int[]{TEXT_DIM});
            }
            case WEAPON_MASTERY -> {
                texts.add("Melee damage: +" + level + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Knockback resist: +" + (level * 5) + "%");
                lines.add(new int[]{TEXT_VALUE});
            }
            case IRON_WILL -> {
                int amp = Math.min((level - 1) / 5, 2);
                texts.add("Resistance: " + SkillTooltips.toRoman(amp + 1));
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP drain: " + skill.getToggleMpPerSecond(level) + "/sec");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Damage reduction + knockback resist");
                lines.add(new int[]{TEXT_DIM});
            }
            case GROUND_SLAM -> {
                double range = 3 + level * 0.15 + stats.getStrength() * 0.05;
                float dmg = 2 + level * 0.5f + stats.getStrength() * 0.1f;
                int stunDur = 1 + level / 5;
                texts.add("Damage: " + String.format("%.1f", dmg) + " (STR scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Range: " + String.format("%.1f", range) + " blocks (STR scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Stun: " + stunDur + "s (Slowness IV)");
                lines.add(new int[]{TEXT_VALUE});
            }
            case RAGE -> {
                texts.add("Below 50% HP: +" + (level * 2) + "% damage");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Lifesteal: " + String.format("%.1f", level * 0.5) + "%");
                lines.add(new int[]{TEXT_VALUE});
            }
            case DOMAIN_OF_MONARCH -> {
                int dur = 4 + level / 2;
                double radius = 2 + level * 0.3 + stats.getStrength() * 0.1;
                float dps = 0.5f + level * 0.15f + stats.getStrength() * 0.05f;
                texts.add("Duration: " + dur + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Radius: " + String.format("%.1f", radius) + " blocks (STR scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("DPS: " + String.format("%.1f", dps) + "/sec (STR scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Applies: Slowness I, Glowing on enemies");
                lines.add(new int[]{TEXT_DIM});
                texts.add("Grants: Strength I to self");
                lines.add(new int[]{TEXT_DIM});
            }
            case UNBREAKABLE -> {
                int dur = 3 + level / 4;
                float healPct = 20 + level;
                texts.add("Invulnerable: " + dur + "s (Resistance V)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Burst heal: " + String.format("%.0f", healPct) + "% max HP");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Cleanses: Poison, Wither, Weakness, Slow");
                lines.add(new int[]{TEXT_DIM});
            }
            case BERSERKER_SPIRIT -> {
                texts.add("Crit rate: +" + level + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Double strike: " + level + "% chance");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Lifesteal: " + String.format("%.1f", level * 0.3) + "%");
                lines.add(new int[]{TEXT_VALUE});
            }
            default -> {}
        }
    }

    // ========== EXECUTION ==========

    public static void execute(ServerPlayer player, PlayerStats stats,
                               SkillData sd, SkillType skill, int level) {
        switch (skill) {
            case BLOODLUST -> executeBloodlust(player, stats, sd, level);
            case WAR_CRY -> executeWarCry(player, stats, sd, level);
            case IRON_WILL -> executeIronWill(player, stats, sd, level);
            case GROUND_SLAM -> executeGroundSlam(player, stats, sd, level);
            case DOMAIN_OF_MONARCH -> executeDomainOfMonarch(player, stats, sd, level);
            case UNBREAKABLE -> executeUnbreakable(player, stats, sd, level);
            default -> {}
        }
    }

    private static void executeBloodlust(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.BLOODLUST.getMpCost(level));
        double range = 4 + level * 0.8 + stats.getStrength() * 0.3;
        int duration = (int) ((3 + level * 0.5) * 20);
        AABB area = player.getBoundingBox().inflate(range);
        List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
        for (Monster mob : mobs) {
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 1, false, true));
            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, false, true));
        }
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.sphere(sl, player.getX(), player.getY() + 1, player.getZ(), range * 0.5, 20, ParticleTypes.DAMAGE_INDICATOR);
            SkillParticles.ring(sl, player.getX(), player.getY() + 0.1, player.getZ(), range * 0.4, 12, ParticleTypes.SOUL_FIRE_FLAME);
            SkillSounds.playAt(player, SoundEvents.RAVAGER_ROAR, 0.7f, 0.8f);
        }
        sd.startCooldown(SkillType.BLOODLUST, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7eBloodlust! " + mobs.size() + " enemies affected."));
    }

    private static void executeWarCry(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.WAR_CRY.getMpCost(level));
        int duration = (10 + level) * 20;
        int strAmp = Math.min((level - 1) / 3, 2);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, strAmp, false, true));
        double range = 6;
        AABB area = player.getBoundingBox().inflate(range);
        List<ServerPlayer> nearby = player.level().getEntitiesOfClass(ServerPlayer.class, area, p -> p != player);
        for (ServerPlayer p : nearby) {
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, strAmp, false, true));
        }
        List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
        for (Monster mob : mobs) {
            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, false, true));
        }
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 1.5, player.getZ(), 15, 0.5, ParticleTypes.CRIT);
            for (ServerPlayer p : nearby) {
                SkillParticles.burst(sl, p.getX(), p.getY() + 1.5, p.getZ(), 5, 0.3, ParticleTypes.ENCHANTED_HIT);
            }
            SkillSounds.playAt(player, SoundEvents.ENDER_DRAGON_GROWL, 0.5f, 1.2f);
        }
        sd.startCooldown(SkillType.WAR_CRY, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7eWar Cry! " + (nearby.size() + 1) + " allies buffed."));
    }

    private static void executeIronWill(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        if (stats.getCurrentMp() < SkillType.IRON_WILL.getToggleMpPerSecond(level)) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough MP!"));
            return;
        }
        sd.setToggleActive(SkillType.IRON_WILL, true);
        applyIronWillEffects(player, stats, level);
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.sphere(sl, player.getX(), player.getY() + 1, player.getZ(), 1.0, 15, ParticleTypes.ENCHANTED_HIT);
            SkillSounds.playAt(player, SoundEvents.ANVIL_LAND, 0.4f, 1.5f);
        }
        sd.startCooldown(SkillType.IRON_WILL, level);
        player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a7eIron Will activated."));
    }

    public static void applyIronWillEffects(ServerPlayer player, PlayerStats stats, int level) {
        int amp = Math.min((level - 1) / 5, 2);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, amp, false, true));
    }

    private static void executeGroundSlam(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.GROUND_SLAM.getMpCost(level));
        double range = 3 + level * 0.15 + stats.getStrength() * 0.05;
        float damage = 2 + level * 0.5f + stats.getStrength() * 0.1f;
        int stunDuration = (1 + level / 5) * 20;
        AABB area = player.getBoundingBox().inflate(range);
        List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
        for (Monster mob : mobs) {
            mob.hurt(SkillDamageSource.get(player.level()), damage);
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stunDuration, 3, false, true));
        }
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.disc(sl, player.getX(), player.getY() + 0.1, player.getZ(), range, 40, ParticleTypes.CAMPFIRE_COSY_SMOKE);
            SkillParticles.ring(sl, player.getX(), player.getY() + 0.1, player.getZ(), range, 16, ParticleTypes.EXPLOSION);
            SkillSounds.playAt(player, SoundEvents.GENERIC_EXPLODE, 0.8f, 0.7f);
        }
        CombatLog.aoeSkill(player, "Ground Slam", damage, mobs);
        sd.startCooldown(SkillType.GROUND_SLAM, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7eGround Slam! " + mobs.size() + " enemies stunned."));
    }

    private static void executeDomainOfMonarch(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.DOMAIN_OF_MONARCH.getMpCost(level));
        int durationTicks = (4 + level / 2) * 20;
        sd.setDomainTicks(durationTicks);
        sd.setDomainPos(player.getX(), player.getY(), player.getZ());
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, durationTicks, 0, false, true));
        if (player.level() instanceof ServerLevel sl) {
            double domainRadius = 2 + level * 0.3 + stats.getStrength() * 0.1;
            SkillParticles.ring(sl, player.getX(), player.getY() + 0.1, player.getZ(), domainRadius, 20, ParticleTypes.WITCH);
            SkillParticles.ring(sl, player.getX(), player.getY() + 0.1, player.getZ(), domainRadius, 12, ParticleTypes.ENCHANTED_HIT);
            SkillSounds.playAt(player, SoundEvents.ELDER_GUARDIAN_CURSE, 0.6f, 1.0f);
        }
        sd.startCooldown(SkillType.DOMAIN_OF_MONARCH, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a75Domain of the Monarch activated!"));
    }

    public static void tickDomain(ServerPlayer player, PlayerStats stats, SkillData sd) {
        int level = sd.getLevel(SkillType.DOMAIN_OF_MONARCH);
        double radius = 2 + level * 0.3 + stats.getStrength() * 0.1;
        float damage = 0.5f + level * 0.15f + stats.getStrength() * 0.05f;
        AABB area = new AABB(
                sd.getDomainX() - radius, sd.getDomainY() - radius, sd.getDomainZ() - radius,
                sd.getDomainX() + radius, sd.getDomainY() + radius, sd.getDomainZ() + radius);
        List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
        for (Monster mob : mobs) {
            mob.hurt(SkillDamageSource.get(player.level()), damage);
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0, false, false));
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
        }
        CombatLog.aoeSkill(player, "Domain", damage, mobs);
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.ring(sl, sd.getDomainX(), sd.getDomainY() + 0.1, sd.getDomainZ(), radius, 16, ParticleTypes.WITCH);
        }
    }

    private static void executeUnbreakable(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.UNBREAKABLE.getMpCost(level));
        int duration = (3 + level / 4) * 20;
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 4, false, true));
        float healPct = 0.2f + level * 0.01f;
        float healAmt = player.getMaxHealth() * healPct;
        player.heal(healAmt);
        CombatLog.heal(player, "Unbreakable", healAmt);
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.WITHER);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 20, 1.0, ParticleTypes.TOTEM_OF_UNDYING);
            SkillParticles.sphere(sl, player.getX(), player.getY() + 1, player.getZ(), 1.2, 15, ParticleTypes.END_ROD);
            SkillSounds.playAt(player, SoundEvents.TOTEM_USE, 0.8f, 1.0f);
        }
        sd.startCooldown(SkillType.UNBREAKABLE, level);
        player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a76Unbreakable!"));
    }

    // ========== TOGGLE DEACTIVATION ==========

    public static void deactivateToggle(ServerPlayer player, SkillData sd, SkillType skill) {
        sd.setToggleActive(skill, false);
        switch (skill) {
            case IRON_WILL -> {
                player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Iron Will deactivated."));
            }
            default -> player.sendSystemMessage(Component.literal(
                    "\u00a7b[System]\u00a7r \u00a77" + skill.getDisplayName() + " deactivated."));
        }
    }

    // ========== MIRROR ==========

    public static void mirrorSkill(ServerLevel sl, ShadowPartnerEntity partner,
                                   ServerPlayer player, PlayerStats stats,
                                   SkillType skill, int level, float multiplier) {
        Vec3 pos = partner.position();
        switch (skill) {
            case BLOODLUST -> {
                float radius = 4 + level * 0.3f;
                float dmg = (2 + level * 0.5f) * multiplier;
                List<Monster> mobs = sl.getEntitiesOfClass(Monster.class,
                        partner.getBoundingBox().inflate(radius));
                for (Monster mob : mobs) {
                    mob.hurt(SkillDamageSource.get(player.level()), dmg);
                    mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, true));
                }
                CombatLog.aoeSkill(player, "Shadow Bloodlust", dmg, mobs);
                SkillParticles.sphere(sl, pos.x, pos.y + 1, pos.z, radius, 15, ParticleTypes.DAMAGE_INDICATOR);
            }
            case WAR_CRY -> {
                float radius = 6;
                List<Monster> mobs = sl.getEntitiesOfClass(Monster.class,
                        partner.getBoundingBox().inflate(radius));
                for (Monster mob : mobs) {
                    mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, true));
                }
                SkillParticles.burst(sl, pos.x, pos.y + 1.5, pos.z, 10, 0.5, ParticleTypes.CRIT);
            }
            case GROUND_SLAM -> {
                float radius = 5 + level * 0.2f;
                float dmg = (4 + level * 1.0f + stats.getStrength() * 0.3f) * multiplier;
                List<Monster> mobs = sl.getEntitiesOfClass(Monster.class,
                        partner.getBoundingBox().inflate(radius));
                for (Monster mob : mobs) {
                    mob.hurt(SkillDamageSource.get(player.level()), dmg);
                    mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40 + level * 4, 2, false, true));
                }
                CombatLog.aoeSkill(player, "Shadow Ground Slam", dmg, mobs);
                SkillParticles.disc(sl, pos.x, pos.y + 0.1, pos.z, radius, 25, ParticleTypes.CAMPFIRE_COSY_SMOKE);
            }
            case DOMAIN_OF_MONARCH -> {
                float radius = 3 + level * 0.2f;
                float dmg = (1 + level * 0.3f + stats.getStrength() * 0.1f) * multiplier;
                List<Monster> mobs = sl.getEntitiesOfClass(Monster.class,
                        partner.getBoundingBox().inflate(radius));
                for (Monster mob : mobs) {
                    mob.hurt(SkillDamageSource.get(player.level()), dmg);
                    mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true));
                }
                CombatLog.aoeSkill(player, "Shadow Domain", dmg, mobs);
                SkillParticles.ring(sl, pos.x, pos.y + 0.1, pos.z, radius, 12, ParticleTypes.WITCH);
            }
            case UNBREAKABLE -> {
                partner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 3, false, true));
                SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 10, 0.5, ParticleTypes.TOTEM_OF_UNDYING);
            }
            default -> {
                SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 8, 0.4, ParticleTypes.PORTAL);
                partner.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
        }
    }
}
