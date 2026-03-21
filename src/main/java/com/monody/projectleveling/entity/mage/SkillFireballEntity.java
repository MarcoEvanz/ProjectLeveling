package com.monody.projectleveling.entity.mage;

import com.monody.projectleveling.entity.ModEntities;
import com.monody.projectleveling.entity.assassin.ShadowPartnerEntity;
import com.monody.projectleveling.skill.CombatLog;
import com.monody.projectleveling.skill.SkillParticles;
import com.monody.projectleveling.skill.SkillSounds;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import com.monody.projectleveling.skill.SkillDamageSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import java.util.List;

public class SkillFireballEntity extends AbstractHurtingProjectile implements ItemSupplier {

    public enum FireballType { FLAME_ORB, ANGEL_RAY }

    private static final EntityDataAccessor<Integer> DATA_TYPE =
            SynchedEntityData.defineId(SkillFireballEntity.class, EntityDataSerializers.INT);

    private float damage = 4.0f;
    private float aoeRadius = 3.0f;
    private int skillLevel = 1;

    // Required for entity registration (client-side spawn)
    public SkillFireballEntity(EntityType<? extends AbstractHurtingProjectile> type, Level level) {
        super(type, level);
    }

    // Server spawn constructor
    public SkillFireballEntity(Level level, LivingEntity owner,
                               double xPower, double yPower, double zPower,
                               FireballType type, float damage, float aoeRadius, int skillLevel) {
        super(ModEntities.SKILL_FIREBALL.get(), owner, xPower, yPower, zPower, level);
        this.entityData.set(DATA_TYPE, type.ordinal());
        this.damage = damage;
        this.aoeRadius = aoeRadius;
        this.skillLevel = skillLevel;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_TYPE, 0);
    }

    public FireballType getFireballType() {
        return FireballType.values()[Math.min(this.entityData.get(DATA_TYPE), FireballType.values().length - 1)];
    }

    @Override
    public ItemStack getItem() {
        return getFireballType() == FireballType.FLAME_ORB
                ? new ItemStack(Items.FIRE_CHARGE)
                : new ItemStack(Items.NETHER_STAR);
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return getFireballType() == FireballType.FLAME_ORB
                ? ParticleTypes.FLAME
                : ParticleTypes.END_ROD;
    }

    @Override
    protected float getInertia() {
        return 1.0f; // No drag
    }

    @Override
    protected boolean shouldBurn() {
        return false; // Don't set fire to blocks
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        explodeAndDamage();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        explodeAndDamage();
    }

    private void explodeAndDamage() {
        if (!(level() instanceof ServerLevel serverLevel)) return;

        LivingEntity ownerEntity = (getOwner() instanceof LivingEntity le) ? le : null;
        DamageSource src = ownerEntity != null
                ? damageSources().mobProjectile(this, ownerEntity)
                : SkillDamageSource.get(level());

        AABB area = new AABB(
                getX() - aoeRadius, getY() - aoeRadius, getZ() - aoeRadius,
                getX() + aoeRadius, getY() + aoeRadius, getZ() + aoeRadius);

        FireballType type = getFireballType();

        // Resolve owning player for combat log (owner may be ShadowPartnerEntity)
        ServerPlayer logPlayer = null;
        if (ownerEntity instanceof ServerPlayer sp) {
            logPlayer = sp;
        } else if (ownerEntity instanceof ShadowPartnerEntity partner) {
            logPlayer = partner.getOwnerPlayer();
        }

        if (type == FireballType.FLAME_ORB) {
            // Damage mobs and set on fire
            List<Monster> mobs = serverLevel.getEntitiesOfClass(Monster.class, area);
            CombatLog.suppressDamageLog = true;
            for (Monster mob : mobs) {
                mob.hurt(src, damage);
                mob.setSecondsOnFire(3 + skillLevel / 3);
            }
            CombatLog.suppressDamageLog = false;
            if (!mobs.isEmpty() && logPlayer != null) {
                String name = (ownerEntity instanceof ShadowPartnerEntity) ? "Shadow Flame Orb" : "Flame Orb";
                float totalFinal = 0;
                for (Monster m : mobs) totalFinal += CombatLog.afterArmor(m, src, damage);
                CombatLog.aoe(logPlayer, name, totalFinal / mobs.size(), mobs.size());
            }
            SkillParticles.explosion(serverLevel, getX(), getY(), getZ(), aoeRadius,
                    ParticleTypes.FLAME, ParticleTypes.LARGE_SMOKE);
            SkillSounds.playAt(serverLevel, getX(), getY(), getZ(),
                    SoundEvents.GENERIC_EXPLODE, 0.8f, 1.0f);
        } else {
            // ANGEL_RAY: damage mobs + heal allies
            List<Monster> mobs = serverLevel.getEntitiesOfClass(Monster.class, area);
            CombatLog.suppressDamageLog = true;
            for (Monster mob : mobs) {
                mob.hurt(src, damage);
            }
            CombatLog.suppressDamageLog = false;
            if (!mobs.isEmpty() && logPlayer != null) {
                String name = (ownerEntity instanceof ShadowPartnerEntity) ? "Shadow Angel Ray" : "Angel Ray";
                float totalFinal = 0;
                for (Monster m : mobs) totalFinal += CombatLog.afterArmor(m, src, damage);
                CombatLog.aoe(logPlayer, name, totalFinal / mobs.size(), mobs.size());
            }
            // Heal nearby allies
            float healAmount = damage * 0.3f;
            List<ServerPlayer> allies = serverLevel.getEntitiesOfClass(ServerPlayer.class, area);
            for (ServerPlayer p : allies) {
                p.heal(healAmount);
                SkillParticles.burst(serverLevel, p.getX(), p.getY() + 1, p.getZ(),
                        5, 0.3, ParticleTypes.HEART);
                CombatLog.heal(p, "Angel Ray", healAmount);
            }
            SkillParticles.explosion(serverLevel, getX(), getY(), getZ(), aoeRadius,
                    ParticleTypes.END_ROD, ParticleTypes.ENCHANTED_HIT);
            SkillSounds.playAt(serverLevel, getX(), getY(), getZ(),
                    SoundEvents.BEACON_DEACTIVATE, 0.6f, 1.5f);
        }

        discard();
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
        tag.putInt("fireballType", getFireballType().ordinal());
        tag.putFloat("skillDamage", damage);
        tag.putFloat("aoeRadius", aoeRadius);
        tag.putInt("skillLevel", skillLevel);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_TYPE, tag.getInt("fireballType"));
        this.damage = tag.getFloat("skillDamage");
        this.aoeRadius = tag.getFloat("aoeRadius");
        this.skillLevel = tag.getInt("skillLevel");
    }
}
