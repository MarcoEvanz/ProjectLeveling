package com.monody.projectleveling.skill.classes;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.entity.assassin.ShadowPartnerEntity;
import com.monody.projectleveling.entity.mage.SkillFireballEntity;
import com.monody.projectleveling.particle.ModParticles;
import com.monody.projectleveling.skill.*;
import static com.monody.projectleveling.skill.StatContribRegistry.*;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.monody.projectleveling.skill.SkillTooltips.TEXT_DIM;
import static com.monody.projectleveling.skill.SkillTooltips.TEXT_VALUE;

public final class HealerSkills {

    public static final SkillType[] ALL = {
            SkillType.HOLY_LIGHT, SkillType.BLESS, SkillType.MP_RECOVERY,
            SkillType.HOLY_SHELL, SkillType.DISPEL, SkillType.DIVINE_PROTECTION, SkillType.HOLY_FERVOR,
            SkillType.BENEDICTION, SkillType.ANGEL_RAY, SkillType.BLESSED_ENSEMBLE,
            SkillType.MAGIC_FINALE,
    };

    private HealerSkills() {}

    // ================================================================
    // MATK multiplier helpers (damage only, healing stays flat)
    // ================================================================

    /** Holy Light damage: MATK × this value. Undead full, others 50%. (~30% reduced) */
    public static float getHolyLightDmgMultiplier(int level, int fai) { return 0.70f + level * 0.06f + fai * 0.014f; }
    /** Benediction per-tick damage: MATK × this value. (~30% reduced) */
    public static float getBenedictionDmgMultiplier(int level, int fai) { return 0.28f + level * 0.02f + fai * 0.004f; }
    /** Angel Ray damage: MATK × this value. (~30% reduced) */
    public static float getAngelRayMultiplier(int level, int fai) { return 1.00f + level * 0.07f + fai * 0.014f; }
    /** Magic: Finale beam damage: MATK × this value. Massive single hit. */
    public static float getFinaleMultiplier(int level, int intel, int faith) { return 5.0f + level * 0.3f + intel * 0.01f + faith * 0.015f; }

    // ================================================================
    // Tooltips
    // ================================================================

    public static void addDetailLines(List<String> texts, List<int[]> lines,
                                       SkillType skill, int level, PlayerStats stats) {
        switch (skill) {
            // === T1 ===
            case HOLY_LIGHT -> {
                float heal = 2 + level * 0.8f + stats.getFaith() * 0.15f + stats.getHealingPower();
                float dmgMult = getHolyLightDmgMultiplier(level, stats.getFaith()) * 100;
                texts.add("Heal: " + String.format("%.1f", heal) + " HP (FAI scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Undead dmg: MATK x " + String.format("%.0f", dmgMult) + "% (others 50%)");
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
                float dmgMult = getBenedictionDmgMultiplier(level, stats.getFaith()) * 100;
                texts.add("Duration: " + dur + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Radius: " + String.format("%.1f", radius) + " blocks (FAI scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Allies: Regen II + Strength I each sec");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Enemies: MATK x " + String.format("%.0f", dmgMult) + "% DPS + Slowness I");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Stationary zone");
                lines.add(new int[]{TEXT_DIM});
            }
            case ANGEL_RAY -> {
                float mult = getAngelRayMultiplier(level, stats.getFaith()) * 100;
                texts.add("Damage: MATK x " + String.format("%.0f", mult) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("AoE radius: 3 blocks on impact");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Heals allies: 30% of damage dealt");
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
            case HOLY_FERVOR -> {
                texts.add("Damage: +" + (level * 2) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Damage vs undead: +" + (level * 3) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Crit rate: +" + String.format("%.1f", level * 0.5f) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Crit damage: +" + level + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MATK scaling: +" + String.format("%.0f", level * 1.5f) + "% of MATK added to skills");
                lines.add(new int[]{TEXT_VALUE});
            }

            // === T4 ===
            case MAGIC_FINALE -> {
                float mult = getFinaleMultiplier(level, stats.getIntelligence(), stats.getFaith()) * 100;
                texts.add("Beam damage: MATK x " + String.format("%.0f", mult) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Radius: 15 blocks");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Channel: 12.5s (rooted, magic circle)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Massive holy beam after channeling completes");
                lines.add(new int[]{TEXT_DIM});
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
            // MAGIC_FINALE uses hold-channel via handleHold(), not execute()
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
        // Damage mobs (full damage to undead, 50% to others)
        float holyDamage = stats.getMagicAttack(player) * getHolyLightDmgMultiplier(level, stats.getFaith());
        int hfLv = sd.getLevel(SkillType.HOLY_FERVOR);
        if (hfLv > 0) {
            holyDamage *= 1.0f + hfLv * 0.02f;
        }
        List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
        for (Monster mob : mobs) {
            float dmg = holyDamage;
            if (mob.getMobType() == net.minecraft.world.entity.MobType.UNDEAD) {
                if (hfLv > 0) dmg *= 1.0f + hfLv * 0.03f; // extra vs undead
            } else {
                dmg *= 0.5f;
            }
            mob.hurt(SkillDamageSource.get(player.level()), dmg);
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
        CombatLog.aoeSkill(player, "Holy Light", holyDamage, mobs);
        sd.startCooldown(SkillType.HOLY_LIGHT, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7aHoly Light! Healed " + (nearby.size() + 1)
                        + " allies. " + mobs.size() + " mobs damaged."));
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
        float damage = stats.getMagicAttack(player) * getAngelRayMultiplier(level, stats.getFaith());
        int hfLv = sd.getLevel(SkillType.HOLY_FERVOR);
        if (hfLv > 0) damage *= 1.0f + hfLv * 0.02f;
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
        float damage = stats.getMagicAttack(player) * getBenedictionDmgMultiplier(level, stats.getFaith());
        int hfLv = sd.getLevel(SkillType.HOLY_FERVOR);
        if (hfLv > 0) damage *= 1.0f + hfLv * 0.02f;
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
    // Magic: Finale (channel → beam)
    // ================================================================

    public static final int FINALE_CAST_TICKS = 250;  // 12.5s channel
    public static final int FINALE_BEAM_TICKS = 20;   // 1s beam sequence after channel
    public static final double FINALE_RADIUS = 15.0;

    public static void startFinaleChannel(ServerPlayer player, PlayerStats stats, SkillData sd) {
        int level = sd.getLevel(SkillType.MAGIC_FINALE);
        if (level <= 0) return;
        if (sd.isOnCooldown(SkillType.MAGIC_FINALE)) return;
        if (sd.isFinaleChanneling()) return;

        int mpCost = SkillType.MAGIC_FINALE.getMpCost(level);
        if (stats.getCurrentMp() < mpCost) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough MP!"));
            return;
        }
        stats.setCurrentMp(stats.getCurrentMp() - mpCost);

        // Raycast to find center
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(50));
        BlockHitResult hit = player.level().clip(
                new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        sd.setFinaleCenterPos(hit.getLocation().x, hit.getLocation().y, hit.getLocation().z);
        sd.setFinaleCastPos(player.getX(), player.getY(), player.getZ());
        sd.setFinaleChanneling(true);
        sd.setFinaleTicks(0);
        sd.setChannelTicks(0);
        sd.setChannelMaxTicks(FINALE_CAST_TICKS);
        sd.setChannelSkillName("Magic: Finale");

        if (player.level() instanceof ServerLevel sl) {
            double cx = hit.getLocation().x, cy = hit.getLocation().y, cz = hit.getLocation().z;
            SkillParticles.burst(sl, cx, cy + 1, cz, 20, 2.0, ParticleTypes.END_ROD, true);
            SkillSounds.playAt(sl, cx, cy, cz, SoundEvents.BEACON_ACTIVATE, 1.0f, 0.5f);
        }

        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7aMagic: Finale! Channeling..."));
    }

    /** Called every tick while Finale is active. Handles both channel phase and beam phase. */
    public static void tickFinale(ServerPlayer player, PlayerStats stats, SkillData sd) {
        sd.setFinaleTicks(sd.getFinaleTicks() + 1);
        int tick = sd.getFinaleTicks();

        // Root player throughout entire sequence
        player.teleportTo(sd.getFinaleCastX(), sd.getFinaleCastY(), sd.getFinaleCastZ());

        double cx = sd.getFinaleCenterX();
        double cy = sd.getFinaleCenterY();
        double cz = sd.getFinaleCenterZ();

        // ============================
        // CHANNEL PHASE (ticks 1-99)
        // ============================
        if (tick < FINALE_CAST_TICKS) {
            sd.setChannelTicks(tick);
            if (!(player.level() instanceof ServerLevel sl)) return;

            double t = (double) tick / FINALE_CAST_TICKS;
            double spin = -tick * (2.0 * Math.PI / 400.0); // clockwise, 1 rotation per 20s

            // Main pentagram every 2 ticks (lifetime=1 covers 2 frames, so no gaps)
            if (tick % 2 == 0) {
                drawPentagram(sl, cx, cy + 0.2, cz, FINALE_RADIUS, 0.35, ModParticles.SHORT_END_ROD.get(), spin);
            }

            // Circle + center glow every 4 ticks
            if (tick % 4 == 0) {
                SkillParticles.ring(sl, cx, cy + 0.2, cz, FINALE_RADIUS, (int) (FINALE_RADIUS * 6), ParticleTypes.END_ROD, true);
                SkillParticles.burst(sl, cx, cy + 0.5, cz, 3, 0.5, ParticleTypes.END_ROD, true);
            }

            // Layer 1: from 2.5s (tick 50) — 1/3 size, 8 blocks up, counter-clockwise
            if (tick >= 50) {
                double r1 = FINALE_RADIUS / 3.0;
                double s1 = tick * (2.0 * Math.PI / 400.0); // counter-clockwise
                if (tick % 2 == 0) {
                    drawPentagram(sl, cx, cy + 8.2, cz, r1, 0.25, ModParticles.SHORT_END_ROD.get(), s1);
                }
                if (tick % 4 == 0) {
                    SkillParticles.ring(sl, cx, cy + 8.2, cz, r1, (int) (r1 * 6), ParticleTypes.END_ROD, true);
                }
            }

            // Layer 2: from 5s (tick 100) — 2/3 size, cy+11.2, clockwise
            if (tick >= 100) {
                double r2 = FINALE_RADIUS * 2.0 / 3.0;
                double s2 = -tick * (2.0 * Math.PI / 400.0); // clockwise
                if (tick % 2 == 0) {
                    drawPentagram(sl, cx, cy + 11.2, cz, r2, 0.3, ModParticles.SHORT_END_ROD.get(), s2);
                }
                if (tick % 4 == 0) {
                    SkillParticles.ring(sl, cx, cy + 11.2, cz, r2, (int) (r2 * 6), ParticleTypes.END_ROD, true);
                }
            }

            // Layer 3: from 7.5s (tick 150) — same size as main, cy+14.2, counter-clockwise
            if (tick >= 150) {
                double r3 = FINALE_RADIUS;
                double s3 = tick * (2.0 * Math.PI / 400.0); // counter-clockwise
                if (tick % 2 == 0) {
                    drawPentagram(sl, cx, cy + 14.2, cz, r3, 0.35, ModParticles.SHORT_END_ROD.get(), s3);
                }
                if (tick % 4 == 0) {
                    SkillParticles.ring(sl, cx, cy + 14.2, cz, r3, (int) (r3 * 6), ParticleTypes.END_ROD, true);
                }
            }

            // Layer 4: from 10s (tick 200) — 1.25x size, cy+17.2, clockwise
            if (tick >= 200) {
                double r4 = FINALE_RADIUS * 1.25;
                double s4 = -tick * (2.0 * Math.PI / 400.0); // clockwise
                if (tick % 2 == 0) {
                    drawPentagram(sl, cx, cy + 17.2, cz, r4, 0.35, ModParticles.SHORT_END_ROD.get(), s4);
                }
                if (tick % 4 == 0) {
                    SkillParticles.ring(sl, cx, cy + 17.2, cz, r4, (int) (r4 * 6), ParticleTypes.END_ROD, true);
                }
            }
            // Rising sound
            if (tick % 20 == 0) {
                SkillSounds.playAt(sl, cx, cy, cz, SoundEvents.BEACON_AMBIENT, 0.6f, (float) (0.5 + t * 1.5));
            }
            return;
        }

        // Clear channel bar on transition
        if (tick == FINALE_CAST_TICKS) {
            sd.setChannelTicks(0);
            sd.setChannelMaxTicks(0);
            sd.setChannelSkillName("");
        }

        // ============================
        // BEAM PHASE (bt 0-29)
        // ============================
        int bt = tick - FINALE_CAST_TICKS;
        if (!(player.level() instanceof ServerLevel sl)) return;

        // --- Sub-phase 1: Beam descends from sky (bt 0-7) ---
        // Column drops from Y+50 down to ground over 8 ticks
        if (bt >= 0 && bt <= 7) {
            // Keep all pentagrams + circles visible during descent (frozen at final angle)
            double finalSpinCW = -FINALE_CAST_TICKS * (2.0 * Math.PI / 400.0);
            double finalSpinCCW = FINALE_CAST_TICKS * (2.0 * Math.PI / 400.0);
            if (bt % 2 == 0) {
                // Main (ground)
                drawPentagram(sl, cx, cy + 0.2, cz, FINALE_RADIUS, 0.35, ModParticles.SHORT_END_ROD.get(), finalSpinCW);
                // Layer 1 (cy+8.2, 1/3 size, CCW)
                drawPentagram(sl, cx, cy + 8.2, cz, FINALE_RADIUS / 3.0, 0.25, ModParticles.SHORT_END_ROD.get(), finalSpinCCW);
                // Layer 2 (cy+11.2, 2/3 size, CW)
                drawPentagram(sl, cx, cy + 11.2, cz, FINALE_RADIUS * 2.0 / 3.0, 0.3, ModParticles.SHORT_END_ROD.get(), finalSpinCW);
                // Layer 3 (cy+14.2, full size, CCW)
                drawPentagram(sl, cx, cy + 14.2, cz, FINALE_RADIUS, 0.35, ModParticles.SHORT_END_ROD.get(), finalSpinCCW);
                // Layer 4 (cy+17.2, 1.25x size, CW)
                drawPentagram(sl, cx, cy + 17.2, cz, FINALE_RADIUS * 1.25, 0.35, ModParticles.SHORT_END_ROD.get(), finalSpinCW);
            }
            if (bt % 4 == 0) {
                SkillParticles.ring(sl, cx, cy + 0.2, cz, FINALE_RADIUS, (int) (FINALE_RADIUS * 6), ParticleTypes.END_ROD, true);
                SkillParticles.ring(sl, cx, cy + 8.2, cz, FINALE_RADIUS / 3.0, (int) (FINALE_RADIUS / 3.0 * 6), ParticleTypes.END_ROD, true);
                SkillParticles.ring(sl, cx, cy + 11.2, cz, FINALE_RADIUS * 2.0 / 3.0, (int) (FINALE_RADIUS * 2.0 / 3.0 * 6), ParticleTypes.END_ROD, true);
                SkillParticles.ring(sl, cx, cy + 14.2, cz, FINALE_RADIUS, (int) (FINALE_RADIUS * 6), ParticleTypes.END_ROD, true);
                SkillParticles.ring(sl, cx, cy + 17.2, cz, FINALE_RADIUS * 1.25, (int) (FINALE_RADIUS * 1.25 * 6), ParticleTypes.END_ROD, true);
            }

            double progress = (bt + 1) / 8.0; // 0.125 → 1.0
            double bottom = cy + 50.0 * (1.0 - progress); // sky → ground
            int count = (int) (80 * progress);
            SkillParticles.column(sl, cx, cz, bottom, cy + 50, 2.0, count, ParticleTypes.END_ROD, true);
            SkillParticles.column(sl, cx, cz, bottom, cy + 50, 0.8, count / 2, ParticleTypes.FLASH, true);
            SkillParticles.column(sl, cx, cz, bottom, cy + 50, 3.5, (int) (count * 0.75), ParticleTypes.ENCHANT, true);
            if (bt % 2 == 0) {
                SkillSounds.playAt(sl, cx, bottom, cz, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0f, 0.5f + (float) progress);
            }
        }

        // --- Sub-phase 2: GROUND IMPACT (bt 8) ---
        // Beam hits ground, damage applied, massive explosion
        if (bt == 8) {
            finaleImpact(player, stats, sd);
            // Full beam at impact
            SkillParticles.column(sl, cx, cz, cy, cy + 50, 2.0, 80, ParticleTypes.END_ROD, true);
            SkillParticles.column(sl, cx, cz, cy, cy + 50, 0.8, 40, ParticleTypes.FLASH, true);
            SkillParticles.column(sl, cx, cz, cy, cy + 50, 3.5, 60, ParticleTypes.ENCHANT, true);
            // Ground explosion + disc
            SkillParticles.explosion(sl, cx, cy + 1, cz, FINALE_RADIUS, ParticleTypes.END_ROD, ParticleTypes.ENCHANT, true);
            SkillParticles.disc(sl, cx, cy + 0.3, cz, FINALE_RADIUS, 60, ParticleTypes.END_ROD, true);
            // Full pentagram flash (frozen at final spin angle)
            double impactSpin = -FINALE_CAST_TICKS * (2.0 * Math.PI / 400.0);
            drawPentagram(sl, cx, cy + 0.3, cz, FINALE_RADIUS, 0.2, ParticleTypes.END_ROD, impactSpin);
            SkillParticles.ring(sl, cx, cy + 0.3, cz, FINALE_RADIUS, (int) (FINALE_RADIUS * 6), ParticleTypes.END_ROD, true);
            // Impact sounds
            SkillSounds.playAt(sl, cx, cy, cz, SoundEvents.ENDER_DRAGON_GROWL, 1.5f, 1.5f);
            SkillSounds.playAt(sl, cx, cy, cz, SoundEvents.GENERIC_EXPLODE, 1.5f, 0.6f);
            SkillSounds.playAt(sl, cx, cy, cz, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.5f, 0.5f);
            player.sendSystemMessage(Component.literal(
                    "\u00a7b[System]\u00a7r \u00a7a\u00a7lMagic: Finale! \u00a7r\u00a7aBeam strikes!"));
        }

        // --- Sub-phase 3: Shockwave + fading beam (bt 9-19) ---
        if (bt > 8 && bt <= 19) {
            double wave = (bt - 8.0) / 11.0; // 0.0 → 1.0
            // Expanding shockwave ring (goes beyond original radius)
            double waveRadius = FINALE_RADIUS * 1.5 * wave;
            SkillParticles.ring(sl, cx, cy + 0.3, cz, waveRadius, (int) (waveRadius * 3), ParticleTypes.END_ROD, true);
            SkillParticles.ring(sl, cx, cy + 1.0, cz, waveRadius * 0.7, (int) (waveRadius * 2), ParticleTypes.ENCHANT, true);
            // Fading beam (shrinks in height + radius over time)
            if (bt <= 14) {
                double fade = 1.0 - (bt - 8.0) / 6.0; // 1.0 → 0.0
                int count = (int) (60 * fade);
                if (count > 0) {
                    SkillParticles.column(sl, cx, cz, cy, cy + 50 * fade, 1.5 * fade, count, ParticleTypes.END_ROD, true);
                }
            }
        }

        // --- End sequence ---
        if (bt >= FINALE_BEAM_TICKS) {
            endFinale(player, sd);
        }
    }

    /** Draw a pentagram (5-pointed star) inscribed in a circle, with rotation offset. */
    private static void drawPentagram(ServerLevel sl, double cx, double cy, double cz,
                                       double radius, double spacing, ParticleOptions particle,
                                       double angleOffset) {
        if (radius < 0.5) return;
        Vec3[] verts = new Vec3[5];
        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(-90 + i * 72) + angleOffset;
            verts[i] = new Vec3(cx + Math.cos(angle) * radius, cy, cz + Math.sin(angle) * radius);
        }
        for (int i = 0; i < 5; i++) {
            SkillParticles.line(sl, verts[i], verts[(i + 2) % 5], spacing, particle, true);
        }
    }

    /** Apply Finale damage to all mobs in radius. */
    private static void finaleImpact(ServerPlayer player, PlayerStats stats, SkillData sd) {
        double cx = sd.getFinaleCenterX();
        double cy = sd.getFinaleCenterY();
        double cz = sd.getFinaleCenterZ();

        int level = sd.getLevel(SkillType.MAGIC_FINALE);
        float damage = stats.getMagicAttack(player) * getFinaleMultiplier(level, stats.getIntelligence(), stats.getFaith());
        int hfLv = sd.getLevel(SkillType.HOLY_FERVOR);
        if (hfLv > 0) damage *= 1.0f + hfLv * 0.02f;

        AABB area = new AABB(cx - FINALE_RADIUS, cy - 10, cz - FINALE_RADIUS,
                cx + FINALE_RADIUS, cy + 10, cz + FINALE_RADIUS);
        List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
        for (Monster mob : mobs) {
            double dx = mob.getX() - cx;
            double dz = mob.getZ() - cz;
            if (dx * dx + dz * dz <= FINALE_RADIUS * FINALE_RADIUS) {
                mob.hurt(SkillDamageSource.get(player.level()), damage);
            }
        }
        CombatLog.aoeSkill(player, "Magic: Finale", damage, mobs);
    }

    /** Clear all Finale channel state and start cooldown. */
    public static void endFinale(ServerPlayer player, SkillData sd) {
        sd.setFinaleChanneling(false);
        sd.setFinaleTicks(0);
        sd.setChannelTicks(0);
        sd.setChannelMaxTicks(0);
        sd.setChannelSkillName("");
        sd.startCooldown(SkillType.MAGIC_FINALE, sd.getLevel(SkillType.MAGIC_FINALE));
    }

    /** Called when player releases the hold key. Cancels if still in channel phase. */
    public static void releaseFinale(ServerPlayer player, SkillData sd) {
        if (!sd.isFinaleChanneling()) return;
        // If beam already firing (past channel phase), let it finish
        if (sd.getFinaleTicks() >= FINALE_CAST_TICKS) return;
        // Cancel channel — clear state without starting full cooldown
        sd.setFinaleChanneling(false);
        sd.setFinaleTicks(0);
        sd.setChannelTicks(0);
        sd.setChannelMaxTicks(0);
        sd.setChannelSkillName("");
        // Half cooldown on cancel
        int level = sd.getLevel(SkillType.MAGIC_FINALE);
        int halfCd = SkillType.MAGIC_FINALE.getCooldownTicks(level) / 2;
        sd.startCooldownRaw(SkillType.MAGIC_FINALE, halfCd);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a77Magic: Finale cancelled."));
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
                float undeadDmg = stats.getMagicAttack(player) * getHolyLightDmgMultiplier(level, stats.getFaith()) * multiplier;
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
                float dmg = stats.getMagicAttack(player) * getAngelRayMultiplier(level, stats.getFaith()) * multiplier;
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

    // === Stat contributions ===
    public static void registerStats() {
        reg(StatLine.MP_REGEN, (sd, p, s, tags) -> {
            int lv = sd.getLevel(SkillType.MP_RECOVERY);
            if (lv <= 0) return 0;
            double val = lv * 0.2;
            tags.add("+" + String.format("%.1f", val) + "%");
            return val;
        });
        reg(StatLine.DMG, (sd, p, s, tags) -> {
            int lv = sd.getLevel(SkillType.BLESSED_ENSEMBLE);
            if (lv > 0) tags.add(SkillType.BLESSED_ENSEMBLE.getAbbreviation() + "+" + lv * 3 + "%/plr");
            return 0;
        });
    }
}
