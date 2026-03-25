package com.monody.projectleveling.entity.warrior;

import com.monody.projectleveling.capability.PlayerStatsCapability;
import com.monody.projectleveling.entity.ModEntities;
import com.monody.projectleveling.event.StatEventHandler;
import com.monody.projectleveling.skill.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;
import java.util.UUID;

/**
 * A giant sword entity that falls from the sky and deals AoE damage on impact.
 * Spawned by the Heaven Sword (T4 Warrior) skill.
 */
public class HeavenSwordEntity extends Entity {

    private static final int FALL_TICKS = 15; // 0.75 seconds to reach ground
    private static final int LINGER_TICKS = 40; // 2 seconds after impact before disappearing
    private static final double SPAWN_HEIGHT = 50.0; // blocks above impact

    private UUID ownerUUID;
    private float damage;
    private double impactY;
    private int ticksAlive = 0;
    private boolean impacted = false;

    // Client constructor (required by entity registry)
    public HeavenSwordEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    // Server spawn constructor
    public HeavenSwordEntity(Level level, UUID owner, float damage,
                             double impactX, double impactY, double impactZ) {
        super(ModEntities.HEAVEN_SWORD.get(), level);
        this.noPhysics = true;
        this.ownerUUID = owner;
        this.damage = damage;
        this.impactY = impactY;
        setPos(impactX, impactY + SPAWN_HEIGHT, impactZ);
        setDeltaMovement(0, -SPAWN_HEIGHT / FALL_TICKS, 0); // constant fall speed
    }

    @Override
    protected void defineSynchedData() {
        // No synced data needed — client just renders at position
    }

    @Override
    public void tick() {
        super.tick();
        ticksAlive++;

        if (!impacted) {
            // Falling phase: move down
            move(MoverType.SELF, getDeltaMovement());

            // Trail particles
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.END_ROD,
                        getX(), getY(), getZ(), 3, 0.2, 0.5, 0.2, 0.01);
            }

            // Impact check (server only)
            if (!level().isClientSide() && (ticksAlive >= FALL_TICKS || getY() <= impactY)) {
                impacted = true;
                ticksAlive = 0; // reset for linger countdown
                setDeltaMovement(0, 0, 0); // stop moving
                doImpact();
            }
        } else {
            // Linger phase: sit still, then discard
            if (!level().isClientSide() && ticksAlive >= LINGER_TICKS) {
                discard();
            }
        }
    }

    private void doImpact() {
        if (!(level() instanceof ServerLevel sl)) return;

        ServerPlayer owner = ownerUUID != null
                ? sl.getServer().getPlayerList().getPlayer(ownerUUID) : null;

        double radius = 10;
        AABB area = new AABB(getX() - radius, getY() - radius, getZ() - radius,
                getX() + radius, getY() + radius, getZ() + radius);
        List<Monster> mobs = sl.getEntitiesOfClass(Monster.class, area);
        for (Monster mob : mobs) {
            mob.hurt(SkillDamageSource.get(sl), damage);
            if (owner != null) {
                StatEventHandler.tryFinalAttack(owner, mob, damage);
            }
        }

        // Impact visuals
        SkillParticles.explosion(sl, getX(), getY() + 0.5, getZ(),
                radius, ParticleTypes.SWEEP_ATTACK, ParticleTypes.ENCHANTED_HIT);
        SkillParticles.disc(sl, getX(), getY() + 0.1, getZ(),
                radius, 80, ParticleTypes.CRIT);
        SkillParticles.ring(sl, getX(), getY() + 0.1, getZ(),
                radius, 24, ParticleTypes.EXPLOSION);
        SkillSounds.playAt(sl, getX(), getY(), getZ(),
                SoundEvents.GENERIC_EXPLODE, 1.0f, 0.5f);
        SkillSounds.playAt(sl, getX(), getY(), getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, 0.8f, 1.5f);

        // Combat log
        if (owner != null) {
            CombatLog.aoeSkill(owner, "Heaven Sword", damage, mobs);
            owner.sendSystemMessage(Component.literal(
                    "\u00a7b[System]\u00a7r \u00a7eHeaven Sword! " + mobs.size() + " enemies hit."));
        }
    }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean isPickable() { return false; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) { /* transient */ }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) { /* transient */ }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
