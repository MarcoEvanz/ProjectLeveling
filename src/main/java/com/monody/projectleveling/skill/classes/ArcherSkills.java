package com.monody.projectleveling.skill.classes;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.entity.archer.SkillArrowEntity;
import com.monody.projectleveling.entity.assassin.ShadowPartnerEntity;
import com.monody.projectleveling.skill.*;
import static com.monody.projectleveling.skill.StatContribRegistry.*;

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

public final class ArcherSkills {

    public static final SkillType[] ALL = {
            SkillType.ARROW_RAIN, SkillType.SOUL_ARROW, SkillType.SHARP_EYES,
            SkillType.ARROW_BOMB, SkillType.COVERING_FIRE, SkillType.EVASION_BOOST,
            SkillType.PHOENIX, SkillType.HURRICANE, SkillType.MORTAL_BLOW,
            SkillType.STORM_OF_ARROWS,
            SkillType.HAWK_EYE,
    };

    private ArcherSkills() {}

    // ================================================================
    // ATK multiplier helpers
    // ================================================================

    /** Arrow Rain per-arrow multiplier: ATK × this value. (+50% buffed) */
    public static float getArrowRainMultiplier(int level, int dex) { return 0.12f + level * 0.012f + dex * 0.0015f; }
    /** Arrow Bomb multiplier: ATK × this value. (+50% buffed) */
    public static float getArrowBombMultiplier(int level, int dex) { return 1.20f + level * 0.06f + dex * 0.008f; }
    /** Covering Fire per-arrow multiplier: ATK × this value. (+50% buffed) */
    public static float getCoveringFireMultiplier(int level, int dex) { return 0.75f + level * 0.045f + dex * 0.006f; }
    /** Phoenix per-tick multiplier: ATK × this value. (+50% buffed) */
    public static float getPhoenixMultiplier(int level, int dex) { return 0.38f + level * 0.03f + dex * 0.005f; }
    /** Hurricane per-arrow multiplier: ATK × this value. (+50% buffed) */
    public static float getHurricaneMultiplier(int level, int dex) { return 0.45f + level * 0.03f + dex * 0.006f; }
    /** Storm of Arrows per-arrow multiplier: ATK × this value. */
    public static float getStormOfArrowsMultiplier(int level, int dex) { return 0.5f + level * 0.047f + dex * 0.0027f; }

    // ================================================================
    // Tooltips
    // ================================================================

    public static void addDetailLines(List<String> texts, List<int[]> lines,
                                       SkillType skill, int level, PlayerStats stats) {
        switch (skill) {
            // === T1 ===
            case ARROW_RAIN -> {
                float mult = getArrowRainMultiplier(level, stats.getDexterity()) * 100;
                double dur = 20 + (level - 1) * (40.0 / 9.0);
                texts.add("Dmg/arrow: ATK x " + String.format("%.0f", mult) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Duration: " + String.format("%.1f", dur / 20.0) + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Radius: 3 blocks, 2-3 arrows/tick");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Scout arrow marks target for rain");
                lines.add(new int[]{TEXT_DIM});
            }
            case SOUL_ARROW -> {
                texts.add("MP drain: " + String.format("%.1f", skill.getToggleDrainPercent(level)) + "% MP/sec");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Infinite arrows (no ammo consumed)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Projectile damage: +" + (5 + level) + "%");
                lines.add(new int[]{TEXT_VALUE});
            }
            case SHARP_EYES -> {
                texts.add("Crit rate: +" + String.format("%.1f", level * 1.5) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Crit damage: +" + (level * 5) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Projectile range: +" + (level / 2));
                lines.add(new int[]{TEXT_VALUE});
            }

            // === T2 ===
            case ARROW_BOMB -> {
                float mult = getArrowBombMultiplier(level, stats.getDexterity()) * 100;
                double radius = 3 + level * 0.1;
                int stunDur = 2 + level / 5;
                texts.add("Damage: ATK x " + String.format("%.0f", mult) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("AoE radius: " + String.format("%.1f", radius) + " blocks");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Stun: " + stunDur + "s (Slowness IV)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Explosive arrow projectile");
                lines.add(new int[]{TEXT_DIM});
            }
            case COVERING_FIRE -> {
                float mult = getCoveringFireMultiplier(level, stats.getDexterity()) * 100;
                int arrowCount = 3 + level / 4;
                texts.add("Dmg/arrow: ATK x " + String.format("%.0f", mult) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Arrows: " + arrowCount);
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Leaps backward while firing forward");
                lines.add(new int[]{TEXT_DIM});
            }
            case EVASION_BOOST -> {
                texts.add("Dodge chance: " + (level * 2) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Dodge guarantees next arrow crits");
                lines.add(new int[]{TEXT_DIM});
            }

            // === T3 ===
            case PHOENIX -> {
                float mult = getPhoenixMultiplier(level, stats.getDexterity()) * 100;
                double range = 6 + level * 0.2 + stats.getDexterity() * 0.05;
                int dur = (int) Math.min(30 + level * 1.5, 60);
                texts.add("Duration: " + dur + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("DPS: ATK x " + String.format("%.0f", mult) + "%/sec");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Range: " + String.format("%.1f", range) + " blocks (DEX scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Grants: Resistance I, sets target on fire");
                lines.add(new int[]{TEXT_DIM});
            }
            case HURRICANE -> {
                float mult = getHurricaneMultiplier(level, stats.getDexterity()) * 100;
                double range = 8 + stats.getDexterity() * 0.1;
                texts.add("Dmg/arrow: ATK x " + String.format("%.0f", mult) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Range: " + String.format("%.1f", range) + " blocks (DEX scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP drain: " + String.format("%.1f", skill.getToggleDrainPercent(level)) + "% MP/sec");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Auto-fires at nearest enemy, slows self");
                lines.add(new int[]{TEXT_DIM});
            }
            case MORTAL_BLOW -> {
                texts.add("Below 30% HP: +" + (level * 2) + "% projectile dmg");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Execute chance: " + String.format("%.1f", level * 0.5) + "%");
                lines.add(new int[]{TEXT_VALUE});
            }

            // === T4 ===
            case STORM_OF_ARROWS -> {
                float mult = getStormOfArrowsMultiplier(level, stats.getDexterity()) * 100;
                texts.add("Dmg/arrow: ATK x " + String.format("%.0f", mult) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Arrows: 2-3 every 5s for 30s (0.5s apart)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Range: " + String.format("%.1f", 10 + stats.getDexterity() * 0.1) + " blocks");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Homing arrows from the sky, bypasses I-frame");
                lines.add(new int[]{TEXT_DIM});
            }

            // === T5 ===
            case HAWK_EYE -> {
                texts.add("Crit rate: +" + level + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Crit damage: +" + (level * 2) + "%");
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
            case ARROW_RAIN -> executeArrowRain(player, stats, sd, level);
            case SOUL_ARROW -> executeSoulArrow(player, stats, sd, level);
            case ARROW_BOMB -> executeArrowBomb(player, stats, sd, level);
            case COVERING_FIRE -> executeCoveringFire(player, stats, sd, level);
            case PHOENIX -> executePhoenix(player, stats, sd, level);
            case HURRICANE -> executeHurricane(player, stats, sd, level);
            case STORM_OF_ARROWS -> executeStormOfArrows(player, stats, sd, level);
            default -> {}
        }
    }

    // ================================================================
    // Execution methods
    // ================================================================

    private static void executeArrowRain(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.ARROW_RAIN.getMpCost(level));
        float damage = stats.getAttack(player) * getArrowRainMultiplier(level, stats.getDexterity());
        float radius = 3.0f;
        int durationTicks = (int) (20 + (level - 1) * (40.0 / 9.0)); // 1s at lv1, 3s at lv10

        // Fire scout arrow where player is looking — rain triggers on impact
        SkillArrowEntity scout = new SkillArrowEntity(
                player.level(), player, SkillArrowEntity.ArrowType.RAIN_SCOUT, damage, radius, durationTicks);
        scout.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 2.5f, 1.0f);
        player.level().addFreshEntity(scout);

        SkillSounds.playAt(player, SoundEvents.CROSSBOW_SHOOT, 0.8f, 0.8f);
        sd.startCooldown(SkillType.ARROW_RAIN, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7eArrow Rain fired!"));
    }

    private static void executeSoulArrow(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        if (stats.getCurrentMp() < SkillType.SOUL_ARROW.getToggleMpPerSecond(level, stats.getMaxMp())) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough MP!"));
            return;
        }
        sd.setToggleActive(SkillType.SOUL_ARROW, true);
        SkillParticles.playerAura(player, 10, 0.8, ParticleTypes.END_ROD);
        SkillSounds.playAt(player, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.3f, 1.5f);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7eSoul Arrow activated. Infinite arrows!"));
    }

    private static void executeArrowBomb(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.ARROW_BOMB.getMpCost(level));
        float aoeRadius = (float) (3 + level * 0.1);
        float damage = stats.getAttack(player) * getArrowBombMultiplier(level, stats.getDexterity());
        int stunTicks = (2 + level / 5) * 20;
        SkillArrowEntity arrow = new SkillArrowEntity(
                player.level(), player, SkillArrowEntity.ArrowType.BOMB, damage, aoeRadius, stunTicks);
        arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 2.5f, 1.0f);
        player.level().addFreshEntity(arrow);
        SkillSounds.playAt(player, SoundEvents.CROSSBOW_SHOOT, 0.8f, 0.9f);
        sd.startCooldown(SkillType.ARROW_BOMB, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7eArrow Bomb launched!"));
    }

    private static void executeCoveringFire(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.COVERING_FIRE.getMpCost(level));
        Vec3 look = player.getLookAngle();
        // Leap backward
        double force = 1.0 + level * 0.05;
        player.push(-look.x * force, 0.3, -look.z * force);
        player.hurtMarked = true;
        // Spawn arrows forward
        float damage = stats.getAttack(player) * getCoveringFireMultiplier(level, stats.getDexterity());
        int arrowCount = 3 + level / 4;
        for (int i = 0; i < arrowCount; i++) {
            SkillArrowEntity arrow = new SkillArrowEntity(
                    player.level(), player, SkillArrowEntity.ArrowType.COVERING_FIRE, damage, 0, 0);
            float spreadAngle = (i - arrowCount / 2.0f) * 8.0f;
            arrow.shootFromRotation(player, player.getXRot(), player.getYRot() + spreadAngle, 0.0f, 2.0f, 3.0f);
            player.level().addFreshEntity(arrow);
        }
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 0.5, player.getZ(), 8, 0.5, ParticleTypes.CLOUD);
            SkillSounds.playAt(player, SoundEvents.CROSSBOW_SHOOT, 0.7f, 1.0f);
            SkillSounds.playAt(player, SoundEvents.BAT_TAKEOFF, 0.5f, 1.0f);
        }
        sd.startCooldown(SkillType.COVERING_FIRE, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7eCovering Fire! " + arrowCount + " arrows fired."));
    }

    private static void executePhoenix(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.PHOENIX.getMpCost(level));
        int duration = (30 + level * 1.5f > 60 ? 60 : (int) (30 + level * 1.5f)) * 20;
        sd.setPhoenixTicks(duration);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, true));
        if (player.level() instanceof ServerLevel sl) {
            // Summon phoenix shape rising above player
            SkillParticles.phoenix(sl, player.getX(), player.getY() + 3, player.getZ(),
                    player.getYRot(), ParticleTypes.FLAME, ParticleTypes.SOUL_FIRE_FLAME);
            SkillParticles.column(sl, player.getX(), player.getZ(), player.getY(), player.getY() + 4, 0.5, 15, ParticleTypes.FLAME);
            SkillSounds.playAt(player, SoundEvents.BLAZE_SHOOT, 0.8f, 0.5f);
        }
        sd.startCooldown(SkillType.PHOENIX, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a76Phoenix summoned!"));
    }

    private static void executeHurricane(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        if (stats.getCurrentMp() < SkillType.HURRICANE.getToggleMpPerSecond(level, stats.getMaxMp())) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough MP!"));
            return;
        }
        sd.setToggleActive(SkillType.HURRICANE, true);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false));
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.spiral(sl, player.getX(), player.getY(), player.getZ(), 1.5, 2.5, 3, 10, ParticleTypes.CRIT);
            SkillSounds.playAt(player, SoundEvents.CROSSBOW_SHOOT, 0.3f, 1.0f);
        }
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7eHurricane activated!"));
    }

    // ================================================================
    // Tick methods (called while skill is active)
    // ================================================================

    /** Called every tick while arrow rain is active. Spawns many arrows at impact zone. */
    public static void tickArrowRain(ServerPlayer player, SkillData sd) {
        if (!(player.level() instanceof ServerLevel sl)) return;

        // Follow target entity if one was hit by the scout arrow
        int targetId = sd.getArrowRainTargetId();
        if (targetId >= 0) {
            net.minecraft.world.entity.Entity target = sl.getEntity(targetId);
            if (target != null && target.isAlive()) {
                sd.setArrowRainPos(target.getX(), target.getY(), target.getZ());
            }
        }

        double x = sd.getArrowRainX();
        double y = sd.getArrowRainY();
        double z = sd.getArrowRainZ();
        float damage = sd.getArrowRainDamage();
        float radius = sd.getArrowRainRadius();

        // Spawn 2-3 arrows every tick for a dense rain effect
        int arrowsPerTick = 2 + (sd.getArrowRainTicks() % 2 == 0 ? 1 : 0);
        for (int i = 0; i < arrowsPerTick; i++) {
            double ox = (player.getRandom().nextDouble() - 0.5) * radius * 2;
            double oz = (player.getRandom().nextDouble() - 0.5) * radius * 2;
            SkillArrowEntity arrow = new SkillArrowEntity(
                    player.level(), player, SkillArrowEntity.ArrowType.RAIN, damage, 0, 0);
            arrow.setPos(x + ox, y + 10, z + oz);
            arrow.shoot(0, -1, 0, 2.0f, 1.5f);
            sl.addFreshEntity(arrow);
        }
    }

    /** Called every second while phoenix is active. */
    public static void tickPhoenix(ServerPlayer player, PlayerStats stats, SkillData sd) {
        int level = sd.getLevel(SkillType.PHOENIX);
        double range = 6 + level * 0.2 + stats.getDexterity() * 0.05;
        float damage = stats.getAttack(player) * getPhoenixMultiplier(level, stats.getDexterity());
        AABB area = player.getBoundingBox().inflate(range);
        List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
        if (!mobs.isEmpty()) {
            Monster target = mobs.get(0);
            target.hurt(SkillDamageSource.get(player.level()), damage);
            if (player.getRandom().nextFloat() < 0.8f) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, false));
            }
            target.setSecondsOnFire(2);
            CombatLog.damageSkill(player, "Phoenix", damage, target);
            if (player.level() instanceof ServerLevel sl) {
                // Phoenix flies from above player toward target
                Vec3 from = player.position().add(0, 2.5, 0);
                Vec3 to = target.position().add(0, 1, 0);
                Vec3 mid = from.add(to).scale(0.5).add(0, 1, 0);
                double yaw = Math.toDegrees(Math.atan2(-(to.x - from.x), to.z - from.z));
                SkillParticles.phoenix(sl, mid.x, mid.y, mid.z, yaw,
                        ParticleTypes.FLAME, ParticleTypes.SOUL_FIRE_FLAME);
                SkillParticles.burst(sl, target.getX(), target.getY() + 1, target.getZ(), 8, 0.4, ParticleTypes.FLAME);
            }
        } else {
            // No target: phoenix circles above player
            if (player.level() instanceof ServerLevel sl) {
                double angle = (player.tickCount % 40) * (360.0 / 40.0);
                double orbX = player.getX() + Math.cos(Math.toRadians(angle)) * 2;
                double orbZ = player.getZ() + Math.sin(Math.toRadians(angle)) * 2;
                SkillParticles.phoenix(sl, orbX, player.getY() + 2.5, orbZ, angle + 90,
                        ParticleTypes.FLAME, ParticleTypes.SOUL_FIRE_FLAME);
            }
        }
    }

    /** Called every second while Hurricane is active. Fires 3 arrows at targets. */
    public static void tickHurricane(ServerPlayer player, PlayerStats stats, SkillData sd) {
        int level = sd.getLevel(SkillType.HURRICANE);
        double range = 8 + stats.getDexterity() * 0.1;
        float damage = stats.getAttack(player) * getHurricaneMultiplier(level, stats.getDexterity());
        AABB area = player.getBoundingBox().inflate(range);
        List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
        if (!mobs.isEmpty()) {
            for (int i = 0; i < 2; i++) {
                Monster target = mobs.get(i % mobs.size());
                SkillArrowEntity arrow = new SkillArrowEntity(
                        player.level(), player, SkillArrowEntity.ArrowType.HURRICANE, damage, 0, 0);
                Vec3 dir = target.position().add(0, target.getBbHeight() * 0.5, 0)
                        .subtract(player.getEyePosition());
                arrow.shoot(dir.x, dir.y, dir.z, 2.5f, 1.0f + i * 0.3f);
                player.level().addFreshEntity(arrow);
            }
            if (player.level() instanceof ServerLevel sl) {
                SkillSounds.playAt(sl, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.CROSSBOW_SHOOT, 0.4f, 1.2f);
            }
        }
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.spiral(sl, player.getX(), player.getY(), player.getZ(), 1.5, 2.5, 2, 8, ParticleTypes.CRIT);
        }
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 1, false, false));
    }

    private static void executeStormOfArrows(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.STORM_OF_ARROWS.getMpCost(level));
        sd.setStormOfArrowsTicks(600); // 30 seconds
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.column(sl, player.getX(), player.getZ(), player.getY(), player.getY() + 5, 0.4, 15, ParticleTypes.CRIT);
            SkillParticles.ring(sl, player.getX(), player.getY() + 5, player.getZ(), 3.0, 12, ParticleTypes.END_ROD);
            SkillSounds.playAt(player, SoundEvents.CROSSBOW_SHOOT, 1.0f, 0.6f);
            SkillSounds.playAt(player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.5f, 1.5f);
        }
        sd.startCooldown(SkillType.STORM_OF_ARROWS, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7eStorm of Arrows! Arrows will rain for 30s."));
    }

    /** Called every tick while Storm of Arrows is active. 2-3 arrows per batch, staggered 0.5s apart, every 5s. */
    public static void tickStormOfArrows(ServerPlayer player, PlayerStats stats, SkillData sd) {
        int rem = sd.getStormOfArrowsTicks() % 100;
        // Arrow 1 at batch start, arrow 2 at 0.5s later, arrow 3 (50% chance) at 1.0s later
        boolean spawn = rem == 0 || rem == 90 || (rem == 80 && player.getRandom().nextBoolean());
        if (!spawn) return;
        if (!(player.level() instanceof ServerLevel sl)) return;

        int level = sd.getLevel(SkillType.STORM_OF_ARROWS);
        double range = 10 + stats.getDexterity() * 0.1;
        float damage = stats.getAttack(player) * getStormOfArrowsMultiplier(level, stats.getDexterity());

        List<Monster> mobs = sl.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(range));
        if (mobs.isEmpty()) return;

        Monster target = mobs.get(player.getRandom().nextInt(mobs.size()));
        SkillArrowEntity arrow = new SkillArrowEntity(
                sl, player, SkillArrowEntity.ArrowType.STORM, damage, 0, 0);
        arrow.setPos(target.getX(), target.getY() + target.getBbHeight() * 0.5 + 12, target.getZ());
        arrow.shoot(0, -1, 0, 3.0f, 0.5f);
        sl.addFreshEntity(arrow);

        SkillSounds.playAt(sl, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CROSSBOW_SHOOT, 0.5f, 1.3f);
    }

    // ================================================================
    // Toggle deactivation
    // ================================================================

    public static void deactivateToggle(ServerPlayer player, SkillData sd, SkillType skill) {
        sd.setToggleActive(skill, false);
        switch (skill) {
            case SOUL_ARROW ->
                    player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Soul Arrow deactivated."));
            case HURRICANE -> {
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Hurricane deactivated."));
            }
            default -> player.sendSystemMessage(Component.literal(
                    "\u00a7b[System]\u00a7r \u00a77" + skill.getDisplayName() + " deactivated."));
        }
    }

    // ================================================================
    // Shadow Partner mirror
    // ================================================================

    public static void mirrorSkill(ServerLevel sl, ShadowPartnerEntity partner,
                                   ServerPlayer player, PlayerStats stats,
                                   SkillType skill, int level, float multiplier) {
        Vec3 pos = partner.position();

        switch (skill) {
            case ARROW_RAIN -> {
                float dmg = stats.getAttack(player) * getArrowRainMultiplier(level, stats.getDexterity()) * multiplier;
                for (int i = 0; i < 5; i++) {
                    double ox = (player.getRandom().nextDouble() - 0.5) * 6;
                    double oz = (player.getRandom().nextDouble() - 0.5) * 6;
                    SkillArrowEntity arrow = new SkillArrowEntity(sl, partner,
                            SkillArrowEntity.ArrowType.RAIN, dmg, 0, 0);
                    arrow.setPos(pos.x + ox, pos.y + 10, pos.z + oz);
                    arrow.shoot(0, -1, 0, 2.0f, 1.5f);
                    sl.addFreshEntity(arrow);
                }
            }
            case ARROW_BOMB -> {
                float dmg = stats.getAttack(player) * getArrowBombMultiplier(level, stats.getDexterity()) * multiplier;
                SkillArrowEntity arrow = new SkillArrowEntity(sl, partner,
                        SkillArrowEntity.ArrowType.BOMB, dmg, 3 + level * 0.1f, 30 + level * 2);
                arrow.shootFromRotation(partner, partner.getXRot(), partner.getYRot(), 0, 2.5f, 1.0f);
                sl.addFreshEntity(arrow);
            }
            case COVERING_FIRE -> {
                float dmg = stats.getAttack(player) * getCoveringFireMultiplier(level, stats.getDexterity()) * multiplier;
                for (int i = 0; i < 3; i++) {
                    SkillArrowEntity arrow = new SkillArrowEntity(sl, partner,
                            SkillArrowEntity.ArrowType.COVERING_FIRE, dmg, 0, 0);
                    float spread = (i - 1) * 8.0f;
                    arrow.shootFromRotation(partner, partner.getXRot(), partner.getYRot() + spread, 0, 2.0f, 3.0f);
                    sl.addFreshEntity(arrow);
                }
            }
            case PHOENIX -> {
                float dmg = stats.getAttack(player) * getPhoenixMultiplier(level, stats.getDexterity()) * multiplier;
                List<Monster> mobs = sl.getEntitiesOfClass(Monster.class,
                        partner.getBoundingBox().inflate(4));
                for (Monster mob : mobs) {
                    mob.hurt(SkillDamageSource.get(player.level()), dmg);
                    mob.setSecondsOnFire(2);
                }
                CombatLog.aoeSkill(player, "Shadow Phoenix", dmg, mobs);
                SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 10, 0.5, ParticleTypes.FLAME);
            }

            // All other archer skills: visual mirror (particles + swing)
            default -> {
                SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 8, 0.4, ParticleTypes.PORTAL);
                partner.swing(InteractionHand.MAIN_HAND);
            }
        }
    }

    // === Stat contributions ===
    public static void registerStats() {
        reg(StatLine.PROJ, (sd, p, s, tags) -> {
            int lv = sd.getLevel(SkillType.SOUL_ARROW);
            if (lv <= 0 || !sd.isToggleActive(SkillType.SOUL_ARROW)) return 0;
            double val = 5 + lv;
            tags.pct(SkillType.SOUL_ARROW.getAbbreviation() + "+", val);
            return val;
        });
        // Simple contributions (SE, EB, MB) defined in SkillType enum
    }
}
