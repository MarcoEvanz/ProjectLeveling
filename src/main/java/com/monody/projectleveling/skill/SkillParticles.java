package com.monody.projectleveling.skill;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class SkillParticles {

    /** Ring of particles at Y offset from center */
    public static void ring(ServerLevel level, double cx, double cy, double cz,
                            double radius, int count, ParticleOptions particle) {
        ring(level, cx, cy, cz, radius, count, particle, false);
    }

    public static void ring(ServerLevel level, double cx, double cy, double cz,
                            double radius, int count, ParticleOptions particle, boolean force) {
        for (int i = 0; i < count; i++) {
            double angle = 2.0 * Math.PI * i / count;
            double x = cx + radius * Math.cos(angle);
            double z = cz + radius * Math.sin(angle);
            send(level, particle, x, cy, z, 1, 0, 0, 0, 0, force);
        }
    }

    /** Filled circle (disc) of random particles */
    public static void disc(ServerLevel level, double cx, double cy, double cz,
                            double radius, int count, ParticleOptions particle) {
        disc(level, cx, cy, cz, radius, count, particle, false);
    }

    public static void disc(ServerLevel level, double cx, double cy, double cz,
                            double radius, int count, ParticleOptions particle, boolean force) {
        for (int i = 0; i < count; i++) {
            double angle = level.random.nextDouble() * 2 * Math.PI;
            double r = Math.sqrt(level.random.nextDouble()) * radius;
            double x = cx + r * Math.cos(angle);
            double z = cz + r * Math.sin(angle);
            send(level, particle, x, cy, z, 1, 0, 0, 0, 0, force);
        }
    }

    /** Sphere of particles around a point */
    public static void sphere(ServerLevel level, double cx, double cy, double cz,
                              double radius, int count, ParticleOptions particle) {
        for (int i = 0; i < count; i++) {
            double phi = Math.acos(2 * level.random.nextDouble() - 1);
            double theta = level.random.nextDouble() * 2 * Math.PI;
            double x = cx + radius * Math.sin(phi) * Math.cos(theta);
            double y = cy + radius * Math.cos(phi);
            double z = cz + radius * Math.sin(phi) * Math.sin(theta);
            level.sendParticles(particle, x, y, z, 1, 0, 0, 0, 0);
        }
    }

    /** Burst of particles at a point with random spread */
    public static void burst(ServerLevel level, double x, double y, double z,
                             int count, double spread, ParticleOptions particle) {
        burst(level, x, y, z, count, spread, particle, false);
    }

    public static void burst(ServerLevel level, double x, double y, double z,
                             int count, double spread, ParticleOptions particle, boolean force) {
        send(level, particle, x, y, z, count, spread, spread, spread, 0.05, force);
    }

    /** Line of particles from A to B */
    public static void line(ServerLevel level, Vec3 from, Vec3 to,
                            double spacing, ParticleOptions particle) {
        line(level, from, to, spacing, particle, false);
    }

    public static void line(ServerLevel level, Vec3 from, Vec3 to,
                            double spacing, ParticleOptions particle, boolean force) {
        Vec3 dir = to.subtract(from);
        double length = dir.length();
        if (length < 0.01) return;
        dir = dir.normalize();
        for (double d = 0; d < length; d += spacing) {
            Vec3 pos = from.add(dir.scale(d));
            send(level, particle, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0, force);
        }
    }

    /** Rising column of particles */
    public static void column(ServerLevel level, double cx, double cz,
                              double yMin, double yMax, double radius,
                              int count, ParticleOptions particle) {
        column(level, cx, cz, yMin, yMax, radius, count, particle, false);
    }

    public static void column(ServerLevel level, double cx, double cz,
                              double yMin, double yMax, double radius,
                              int count, ParticleOptions particle, boolean force) {
        for (int i = 0; i < count; i++) {
            double angle = level.random.nextDouble() * 2 * Math.PI;
            double r = level.random.nextDouble() * radius;
            double y = yMin + level.random.nextDouble() * (yMax - yMin);
            send(level, particle, cx + r * Math.cos(angle), y,
                    cz + r * Math.sin(angle), 1, 0, 0.05, 0, 0, force);
        }
    }

    /** Spiral pattern (tornado/hurricane) */
    public static void spiral(ServerLevel level, double cx, double cy, double cz,
                              double radius, double height, int turns, int particlesPerTurn,
                              ParticleOptions particle) {
        int total = turns * particlesPerTurn;
        for (int i = 0; i < total; i++) {
            double angle = 2.0 * Math.PI * i / particlesPerTurn;
            double progress = (double) i / total;
            double r = radius * (1.0 - progress * 0.3);
            double x = cx + r * Math.cos(angle);
            double y = cy + progress * height;
            double z = cz + r * Math.sin(angle);
            level.sendParticles(particle, x, y, z, 1, 0, 0, 0, 0);
        }
    }

    /** Particles around a player (aura effect) */
    public static void playerAura(ServerPlayer player, int count,
                                  double radius, ParticleOptions particle) {
        if (player.level() instanceof ServerLevel level) {
            sphere(level, player.getX(), player.getY() + 1.0, player.getZ(),
                    radius, count, particle);
        }
    }

    /** Ring of particles at player feet */
    public static void playerFeet(ServerPlayer player, int count,
                                  double radius, ParticleOptions particle) {
        if (player.level() instanceof ServerLevel level) {
            ring(level, player.getX(), player.getY() + 0.1, player.getZ(),
                    radius, count, particle);
        }
    }

    /** Phoenix bird shape facing a direction (yaw in degrees) */
    public static void phoenix(ServerLevel level, double cx, double cy, double cz,
                               double yawDeg, ParticleOptions body, ParticleOptions wing) {
        double rad = Math.toRadians(yawDeg);
        double fx = -Math.sin(rad);
        double fz = Math.cos(rad);
        double rx = Math.cos(rad);
        double rz = Math.sin(rad);

        // Head
        level.sendParticles(body, cx + fx * 1.5, cy + 0.3, cz + fz * 1.5, 1, 0, 0, 0, 0);

        // Body line
        for (int i = -1; i <= 1; i++) {
            level.sendParticles(body, cx + fx * i * 0.5, cy, cz + fz * i * 0.5, 1, 0, 0, 0, 0);
        }

        // Wings (5 particles each side, arcing up then down)
        for (int side : new int[]{-1, 1}) {
            for (int w = 1; w <= 5; w++) {
                double spread = w * 0.5;
                double back = w * 0.2;
                double up = (w <= 3) ? w * 0.15 : (6 - w) * 0.15;
                level.sendParticles(wing,
                        cx + rx * side * spread - fx * back, cy + up,
                        cz + rz * side * spread - fz * back, 1, 0, 0, 0, 0);
            }
        }

        // Tail feathers (trailing lines spreading outward)
        for (int t = 1; t <= 4; t++) {
            double back = t * 0.6;
            double sf = t * 0.15;
            level.sendParticles(wing, cx - fx * back, cy - t * 0.05,
                    cz - fz * back, 1, 0.02, 0.02, 0.02, 0);
            level.sendParticles(wing, cx - fx * back + rx * sf, cy - t * 0.05,
                    cz - fz * back + rz * sf, 1, 0.02, 0.02, 0.02, 0);
            level.sendParticles(wing, cx - fx * back - rx * sf, cy - t * 0.05,
                    cz - fz * back - rz * sf, 1, 0.02, 0.02, 0.02, 0);
        }
    }

    /** Explosion burst at position */
    public static void explosion(ServerLevel level, double x, double y, double z,
                                 double radius, ParticleOptions primary,
                                 ParticleOptions secondary) {
        explosion(level, x, y, z, radius, primary, secondary, false);
    }

    public static void explosion(ServerLevel level, double x, double y, double z,
                                 double radius, ParticleOptions primary,
                                 ParticleOptions secondary, boolean force) {
        burst(level, x, y, z, 30, radius * 0.5, primary, force);
        ring(level, x, y, z, radius, 20, secondary, force);
        send(level, ParticleTypes.EXPLOSION, x, y, z, 1, 0, 0, 0, 0, force);
    }

    /** Send particle with optional force (512-block render distance) */
    private static void send(ServerLevel level, ParticleOptions particle,
                             double x, double y, double z, int count,
                             double dx, double dy, double dz, double speed, boolean force) {
        if (force) {
            for (ServerPlayer p : level.players()) {
                level.sendParticles(p, particle, true, x, y, z, count, dx, dy, dz, speed);
            }
        } else {
            level.sendParticles(particle, x, y, z, count, dx, dy, dz, speed);
        }
    }

    /** Claw slash arc from attacker toward target at target position */
    public static void slash(ServerLevel level, Vec3 attackerPos, Vec3 targetPos,
                             ParticleOptions particle) {
        Vec3 dir = targetPos.subtract(attackerPos).normalize();
        double perpX = -dir.z;
        double perpZ = dir.x;
        double cy = targetPos.y;

        // Sweep attack particle at target
        level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                targetPos.x, cy + 0.5, targetPos.z, 1, 0, 0, 0, 0);

        // 3-claw arc of particles
        for (int claw = -1; claw <= 1; claw++) {
            double offsetX = perpX * claw * 0.35;
            double offsetZ = perpZ * claw * 0.35;
            for (int i = 0; i < 5; i++) {
                double t = (i - 2) * 0.25;
                double px = targetPos.x + perpX * t + offsetX;
                double py = cy + 0.3 + (1.0 - t * t) * 0.4 + claw * 0.15;
                double pz = targetPos.z + perpZ * t + offsetZ;
                level.sendParticles(particle, px, py, pz, 1, 0.02, 0.02, 0.02, 0);
            }
        }
    }
}
