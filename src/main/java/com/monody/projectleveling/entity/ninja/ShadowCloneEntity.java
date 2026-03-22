package com.monody.projectleveling.entity.ninja;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.capability.PlayerStatsCapability;
import com.monody.projectleveling.entity.ModEntities;
import com.monody.projectleveling.skill.*;
import com.monody.projectleveling.skill.classes.NinjaSkills;
import java.util.ArrayList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ShadowCloneEntity extends PathfinderMob {

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
            SynchedEntityData.defineId(ShadowCloneEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    // AI skill usage state
    private float statMultiplier = 0.2f;
    private boolean rasenganReady = false;
    private int shurikenCooldown = 0;
    private int rasenganCooldown = 0;

    public ShadowCloneEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoGravity(false);
        this.setPersistenceRequired();
    }

    /** Server spawn constructor with owner stats scaling. */
    public ShadowCloneEntity(Level level, ServerPlayer owner, PlayerStats ownerStats, float statMult) {
        super(ModEntities.SHADOW_CLONE.get(), level);
        this.entityData.set(DATA_OWNER_UUID, Optional.of(owner.getUUID()));
        this.setNoGravity(false);
        this.setPersistenceRequired();
        this.statMultiplier = statMult;

        // Set stats based on owner with multiplier
        double maxHp = Math.max(10, owner.getMaxHealth() * statMult);
        double attackDmg = Math.max(1, (1 + (ownerStats.getStrength() - 1) * 0.1
                + (ownerStats.getAgility() - 1) * 0.08) * statMult);
        double moveSpeed = 0.35 * (0.8 + 0.2 * statMult);

        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHp);
        this.setHealth((float) maxHp);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(attackDmg);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(moveSpeed);

        // Copy equipment from owner (visual + armor protection)
        copyEquipment(owner);
    }

    private void copyEquipment(Player source) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setItemSlot(slot, source.getItemBySlot(slot).copy());
            this.setDropChance(slot, 0.0f);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_OWNER_UUID, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_OWNER_UUID).orElse(null);
    }

    @Nullable
    public ServerPlayer getOwnerPlayer() {
        UUID uuid = getOwnerUUID();
        if (uuid == null) return null;
        if (level() instanceof ServerLevel sl) {
            Player p = sl.getPlayerByUUID(uuid);
            if (p instanceof ServerPlayer sp) return sp;
        }
        return null;
    }

    // === Rasengan buff for clone (set by sync or AI) ===

    public boolean isRasenganReady() { return rasenganReady; }
    public void setRasenganReady(boolean ready) { this.rasenganReady = ready; }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) return;

        ServerPlayer owner = getOwnerPlayer();
        if (owner == null || !owner.isAlive() || owner.isRemoved()) {
            despawnWithSmoke();
            return;
        }

        // Check if Shadow Clone toggle is still active
        owner.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            SkillData sd = stats.getSkillData();
            if (!sd.isToggleActive(SkillType.SHADOW_CLONE)) {
                despawnWithSmoke();
            }
        });

        if (isRemoved()) return;

        // Tick cooldowns
        if (shurikenCooldown > 0) shurikenCooldown--;
        if (rasenganCooldown > 0) rasenganCooldown--;

        // AI skill usage
        if (level() instanceof ServerLevel sl) {
            tickCloneSkills(sl, owner);
        }

        // Sync equipment from owner every second
        if (tickCount % 20 == 0) {
            copyEquipment(owner);
        }

        // Follow owner if no target and far away
        if (getTarget() == null || !getTarget().isAlive()) {
            double dist = distanceTo(owner);
            if (dist > 16.0) {
                Vec3 behind = owner.position().subtract(owner.getLookAngle().scale(2.0));
                this.teleportTo(behind.x, behind.y, behind.z);
            } else if (dist > 6.0) {
                this.getNavigation().moveTo(owner, 1.2);
            }
        }

        // Ambient smoke particles every 2 seconds
        if (tickCount % 40 == 0 && level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, getX(), getY() + 1, getZ(), 3, 0.3, ParticleTypes.CLOUD);
        }
    }

    /** Clone AI: independently use Shuriken Jutsu and Rasengan. */
    private void tickCloneSkills(ServerLevel sl, ServerPlayer owner) {
        owner.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            SkillData sd = stats.getSkillData();

            // Shuriken Jutsu: use every 3-5 seconds when enemies are nearby and owner has the skill
            int sjLv = sd.getLevel(SkillType.SHURIKEN_JUTSU);
            if (sjLv > 0 && shurikenCooldown <= 0 && getTarget() != null && getTarget().isAlive()) {
                float range = 5 + sjLv * 0.3f;
                double dist = distanceTo(getTarget());
                if (dist <= range) {
                    cloneShuriken(sl, stats, sjLv);
                    shurikenCooldown = 60 + random.nextInt(40); // 3-5 seconds
                }
            }

            // Rasengan: self-activate every 8-12 seconds when in combat and owner has the skill
            int rsgLv = sd.getLevel(SkillType.RASENGAN);
            if (rsgLv > 0 && rasenganCooldown <= 0 && !rasenganReady
                    && getTarget() != null && getTarget().isAlive()) {
                rasenganReady = true;
                rasenganCooldown = 160 + random.nextInt(80); // 8-12 seconds
                SkillParticles.spiral(sl, getX(), getY() + 0.8, getZ(),
                        0.5, 1.5, 3, 8, ParticleTypes.END_ROD);
                SkillSounds.playAt(sl, getX(), getY(), getZ(),
                        SoundEvents.FIREWORK_ROCKET_BLAST, 0.4f, 0.8f);
            }
        });
    }

    /** Clone uses Shuriken Jutsu independently. */
    private void cloneShuriken(ServerLevel sl, PlayerStats ownerStats, int level) {
        float dmg = (2 + level * 0.5f + ownerStats.getAgility() * 0.1f + ownerStats.getLuck() * 0.05f
                + SkillExecutor.getWeaponDamage(this)) * statMultiplier;
        float range = 5 + level * 0.3f;
        Vec3 look = getLookAngle();
        Vec3 eye = getEyePosition();
        AABB area = getBoundingBox().inflate(range);
        List<Monster> mobs = sl.getEntitiesOfClass(Monster.class, area);
        List<Monster> hitMobs = new ArrayList<>();
        for (Monster mob : mobs) {
            Vec3 toMob = mob.position().add(0, mob.getBbHeight() / 2, 0).subtract(eye).normalize();
            if (look.dot(toMob) < 0.3) continue;
            mob.hurt(SkillDamageSource.get(sl), dmg);
            hitMobs.add(mob);
        }
        for (int i = 0; i < 8; i++) {
            double spread = (random.nextDouble() - 0.5) * 0.6;
            Vec3 dir = look.add(look.yRot((float) spread)).normalize();
            Vec3 end = eye.add(dir.scale(range));
            SkillParticles.line(sl, eye.add(dir.scale(1.0)), end, 0.8, ParticleTypes.CRIT);
        }
        SkillSounds.playAt(sl, getX(), getY(), getZ(), SoundEvents.ARROW_SHOOT, 0.5f, 1.5f);
        this.swing(InteractionHand.MAIN_HAND);
        // Log to owner
        ServerPlayer owner = getOwnerPlayer();
        if (owner != null && !hitMobs.isEmpty()) {
            CombatLog.aoeSkill(owner, "Clone Shuriken", dmg, hitMobs);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean result = super.doHurtTarget(target);

        // Rasengan on-hit: AoE explosion around target
        if (result && rasenganReady && target instanceof LivingEntity hitTarget && !level().isClientSide()) {
            rasenganReady = false;
            ServerPlayer owner = getOwnerPlayer();
            if (owner != null && level() instanceof ServerLevel sl) {
                owner.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                    int rsgLv = stats.getSkillData().getLevel(SkillType.RASENGAN);
                    float rasenganDmg = (NinjaSkills.getRasenganBonusDamage(stats, rsgLv) + SkillExecutor.getWeaponDamage(ShadowCloneEntity.this)) * statMultiplier;
                    float splashDmg = rasenganDmg * 0.3f;

                    // Damage main target with rasengan bonus
                    hitTarget.hurt(SkillDamageSource.get(sl), rasenganDmg);

                    // AoE splash to nearby
                    AABB splashArea = hitTarget.getBoundingBox().inflate(2.0);
                    List<LivingEntity> nearby = sl.getEntitiesOfClass(LivingEntity.class, splashArea,
                            e -> e != hitTarget && e != this && !(e instanceof Player) && e instanceof Monster);
                    for (LivingEntity mob : nearby) {
                        mob.hurt(SkillDamageSource.get(sl), splashDmg);
                    }

                    SkillParticles.explosion(sl, hitTarget.getX(), hitTarget.getY() + 1, hitTarget.getZ(),
                            2.0f, ParticleTypes.END_ROD, ParticleTypes.ENCHANTED_HIT);
                    SkillSounds.playAt(sl, hitTarget.getX(), hitTarget.getY(), hitTarget.getZ(),
                            SoundEvents.GENERIC_EXPLODE, 0.4f, 1.3f);

                    // Log to owner
                    CombatLog.damageSkill(owner, "Clone Rasengan", rasenganDmg, hitTarget);
                    if (!nearby.isEmpty()) {
                        CombatLog.aoeSkill(owner, "Clone Rasengan Splash", splashDmg, nearby);
                    }
                });
            }
        }

        return result;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Immune to owner's damage
        if (source.getEntity() instanceof Player p) {
            UUID ownerUUID = getOwnerUUID();
            if (ownerUUID != null && ownerUUID.equals(p.getUUID())) {
                return false;
            }
        }
        // Immune to other clones of same owner
        if (source.getEntity() instanceof ShadowCloneEntity otherClone) {
            UUID myOwner = getOwnerUUID();
            if (myOwner != null && myOwner.equals(otherClone.getOwnerUUID())) {
                return false;
            }
        }

        boolean result = super.hurt(source, amount);

        if (this.isDeadOrDying() && !level().isClientSide()) {
            onCloneDeath();
            despawnWithSmoke();
        }

        return result;
    }

    private void onCloneDeath() {
        ServerPlayer owner = getOwnerPlayer();
        if (owner == null) return;
        if (!(owner.level() instanceof ServerLevel sl)) return;

        // Check if any other clones still exist (exclude this dying one)
        List<ShadowCloneEntity> remaining = sl.getEntitiesOfClass(ShadowCloneEntity.class,
                owner.getBoundingBox().inflate(64),
                e -> owner.getUUID().equals(e.getOwnerUUID()) && e != this && !e.isDeadOrDying());

        if (remaining.isEmpty()) {
            // Last clone died → 10s cooldown
            owner.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                SkillData sd = stats.getSkillData();
                sd.setToggleActive(SkillType.SHADOW_CLONE, false);
                sd.setCooldown(SkillType.SHADOW_CLONE, 200); // 10 seconds
                owner.sendSystemMessage(Component.literal(
                        "\u00a7b[System]\u00a7r \u00a7cAll Shadow Clones destroyed! 10s cooldown."));
                com.monody.projectleveling.event.StatEventHandler.syncToClient(owner);
            });
        } else {
            owner.sendSystemMessage(Component.literal(
                    "\u00a7b[System]\u00a7r \u00a7cA Shadow Clone was destroyed! "
                            + remaining.size() + " remaining."));
        }
    }

    public void despawnWithSmoke() {
        if (level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, getX(), getY() + 1, getZ(), 20, 0.6, ParticleTypes.CLOUD);
            SkillParticles.burst(sl, getX(), getY() + 1, getZ(), 10, 0.4, ParticleTypes.SMOKE);
            SkillSounds.playAt(sl, getX(), getY(), getZ(),
                    SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.5f, 1.2f);
        }
        this.discard();
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (other instanceof Player p) {
            UUID ownerUUID = getOwnerUUID();
            return ownerUUID != null && ownerUUID.equals(p.getUUID());
        }
        if (other instanceof ShadowCloneEntity otherClone) {
            UUID myOwner = getOwnerUUID();
            return myOwner != null && myOwner.equals(otherClone.getOwnerUUID());
        }
        return super.isAlliedTo(other);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean removeWhenFarAway(double distToPlayer) { return false; }

    @Override
    protected boolean shouldDropLoot() { return false; }

    @Override
    public int getExperienceReward() { return 0; }

    @Override
    public boolean shouldDropExperience() { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID ownerUUID = getOwnerUUID();
        if (ownerUUID != null) {
            tag.putUUID("OwnerUUID", ownerUUID);
        }
        tag.putFloat("StatMult", statMultiplier);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OwnerUUID")) {
            this.entityData.set(DATA_OWNER_UUID, Optional.of(tag.getUUID("OwnerUUID")));
        }
        if (tag.contains("StatMult")) {
            this.statMultiplier = tag.getFloat("StatMult");
        }
    }
}
