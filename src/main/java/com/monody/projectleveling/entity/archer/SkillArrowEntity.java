package com.monody.projectleveling.entity.archer;

import com.monody.projectleveling.entity.ModEntities;
import com.monody.projectleveling.entity.assassin.ShadowPartnerEntity;
import com.monody.projectleveling.skill.CombatLog;
import com.monody.projectleveling.skill.SkillDamageSource;
import com.monody.projectleveling.skill.SkillParticles;
import com.monody.projectleveling.skill.SkillSounds;
import com.monody.projectleveling.capability.PlayerStatsCapability;
import com.monody.projectleveling.skill.SkillData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import java.util.List;

public class SkillArrowEntity extends AbstractArrow {

    public enum ArrowType { RAIN, BOMB, COVERING_FIRE, RAIN_SCOUT, HURRICANE }

    private static final EntityDataAccessor<Integer> DATA_TYPE =
            SynchedEntityData.defineId(SkillArrowEntity.class, EntityDataSerializers.INT);

    private float aoeRadius = 3.0f;
    private int stunTicks = 0;

    // Required for entity registration
    public SkillArrowEntity(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
        this.pickup = Pickup.DISALLOWED;
    }

    // Server spawn constructor
    public SkillArrowEntity(Level level, LivingEntity owner,
                            ArrowType type, float damage, float aoeRadius, int stunTicks) {
        super(ModEntities.SKILL_ARROW.get(), owner, level);
        this.entityData.set(DATA_TYPE, type.ordinal());
        this.setBaseDamage(damage);
        this.aoeRadius = aoeRadius;
        this.stunTicks = stunTicks;
        this.pickup = Pickup.DISALLOWED;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_TYPE, 0);
    }

    public ArrowType getArrowType() {
        return ArrowType.values()[Math.min(this.entityData.get(DATA_TYPE), ArrowType.values().length - 1)];
    }

    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        ArrowType type = getArrowType();

        if (type == ArrowType.RAIN_SCOUT) {
            if (level() instanceof ServerLevel) {
                int targetId = (result.getEntity() instanceof LivingEntity) ? result.getEntity().getId() : -1;
                triggerArrowRain(result.getEntity().getX(), result.getEntity().getY(), result.getEntity().getZ(), targetId);
            }
            discard();
            return;
        }

        // Rain arrows bypass invulnerability frames
        if (type == ArrowType.RAIN) {
            if (result.getEntity() instanceof LivingEntity target) {
                target.invulnerableTime = 0;
            }
            CombatLog.nextSource = "Arrow Rain";
            super.onHitEntity(result);
            if (result.getEntity() instanceof LivingEntity target) {
                target.invulnerableTime = 0; // Allow subsequent rain arrows to hit
            }
            discard();
            return;
        }

        // Tag the damage source for combat log
        switch (type) {
            case BOMB -> CombatLog.nextSource = "Arrow Bomb";
            case COVERING_FIRE -> CombatLog.nextSource = "Covering Fire";
            case HURRICANE -> CombatLog.nextSource = "Hurricane";
            default -> {}
        }
        super.onHitEntity(result);

        if (!(level() instanceof ServerLevel serverLevel)) return;

        if (type == ArrowType.BOMB) {
            aoeExplosion(serverLevel);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        ArrowType type = getArrowType();

        if (type == ArrowType.RAIN_SCOUT) {
            if (level() instanceof ServerLevel) {
                triggerArrowRain(result.getLocation().x, result.getLocation().y, result.getLocation().z, -1);
            }
            discard();
            return;
        }

        super.onHitBlock(result);

        if (!(level() instanceof ServerLevel serverLevel)) return;

        if (type == ArrowType.BOMB) {
            aoeExplosion(serverLevel);
        }

        // Skill arrows don't persist in the world
        discard();
    }

    private void aoeExplosion(ServerLevel serverLevel) {
        LivingEntity ownerEntity = (getOwner() instanceof LivingEntity le) ? le : null;
        DamageSource src = ownerEntity != null
                ? damageSources().mobProjectile(this, ownerEntity)
                : SkillDamageSource.get(level());

        AABB area = new AABB(
                getX() - aoeRadius, getY() - aoeRadius, getZ() - aoeRadius,
                getX() + aoeRadius, getY() + aoeRadius, getZ() + aoeRadius);

        List<Monster> mobs = serverLevel.getEntitiesOfClass(Monster.class, area);
        List<Monster> hitMobs = new java.util.ArrayList<>();
        CombatLog.suppressDamageLog = true;
        for (Monster mob : mobs) {
            // Don't double-damage the mob we directly hit
            if (mob.hurtTime > 0) continue;
            mob.hurt(src, (float) getBaseDamage());
            hitMobs.add(mob);
            if (stunTicks > 0) {
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stunTicks, 3, false, true));
            }
        }
        CombatLog.suppressDamageLog = false;
        // Combat log
        if (!hitMobs.isEmpty()) {
            ServerPlayer logPlayer = null;
            if (ownerEntity instanceof ServerPlayer sp) logPlayer = sp;
            else if (ownerEntity instanceof ShadowPartnerEntity partner) logPlayer = partner.getOwnerPlayer();
            if (logPlayer != null) {
                String name = (ownerEntity instanceof ShadowPartnerEntity) ? "Shadow Arrow Bomb" : "Arrow Bomb";
                float totalFinal = 0;
                for (Monster m : hitMobs) totalFinal += CombatLog.afterArmor(m, src, (float) getBaseDamage());
                CombatLog.aoe(logPlayer, name, totalFinal / hitMobs.size(), hitMobs.size());
            }
        }

        SkillParticles.explosion(serverLevel, getX(), getY(), getZ(), aoeRadius,
                ParticleTypes.FLAME, ParticleTypes.LARGE_SMOKE);
        SkillSounds.playAt(serverLevel, getX(), getY(), getZ(),
                SoundEvents.GENERIC_EXPLODE, 0.7f, 1.0f);
    }

    private void triggerArrowRain(double x, double y, double z, int targetEntityId) {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        if (!(getOwner() instanceof ServerPlayer owner)) return;

        owner.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            SkillData sd = stats.getSkillData();
            sd.setArrowRainPos(x, y, z);
            sd.setArrowRainTicks(stunTicks); // repurposed: rain duration in ticks
            sd.setArrowRainDamage((float) getBaseDamage());
            sd.setArrowRainRadius(aoeRadius);
            sd.setArrowRainTargetId(targetEntityId);
        });

        SkillParticles.disc(serverLevel, x, y + 10, z, aoeRadius, 15, ParticleTypes.CLOUD);
        SkillSounds.playAt(serverLevel, x, y, z, SoundEvents.CROSSBOW_SHOOT, 0.6f, 0.7f);
    }

    @Override
    public void tick() {
        super.tick();
        // Auto-discard after 5 seconds
        if (tickCount > 100) discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("arrowType", getArrowType().ordinal());
        tag.putFloat("aoeRadius", aoeRadius);
        tag.putInt("stunTicks", stunTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_TYPE, tag.getInt("arrowType"));
        this.aoeRadius = tag.getFloat("aoeRadius");
        this.stunTicks = tag.getInt("stunTicks");
    }
}
