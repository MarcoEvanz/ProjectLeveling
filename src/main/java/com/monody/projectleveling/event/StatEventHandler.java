package com.monody.projectleveling.event;

import com.monody.projectleveling.ProjectLeveling;
import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.capability.PlayerStatsCapability;
import com.monody.projectleveling.network.ModNetwork;
import com.monody.projectleveling.network.S2CSyncStatsPacket;
import com.monody.projectleveling.skill.CombatLog;
import com.monody.projectleveling.skill.SkillData;
import com.monody.projectleveling.entity.SkeletonMinionEntity;
import com.monody.projectleveling.entity.SkillArrowEntity;
import com.monody.projectleveling.entity.SkillFireballEntity;
import com.monody.projectleveling.entity.ShadowPartnerEntity;
import com.monody.projectleveling.skill.SkillDamageSource;
import com.monody.projectleveling.skill.SkillExecutor;
import com.monody.projectleveling.skill.SkillParticles;
import com.monody.projectleveling.skill.SkillSounds;
import com.monody.projectleveling.skill.SkillType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.event.TickEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ProjectLeveling.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StatEventHandler {
    // Track last position per player for walk distance calculation
    private static final Map<UUID, double[]> lastPositions = new HashMap<>();

    private static final UUID STRENGTH_UUID = UUID.fromString("a3d5b8c1-1234-4a5b-9c6d-7e8f9a0b1c2d");
    private static final UUID VITALITY_UUID = UUID.fromString("b4e6c9d2-2345-4b6c-ad7e-8f9a0b1c2d3e");
    private static final UUID AGILITY_UUID = UUID.fromString("c5f7dae3-3456-4c7d-be8f-9a0b1c2d3e4f");
    private static final UUID LUCK_ATK_UUID = UUID.fromString("d6a8ebf4-4567-4d8e-cf90-ab1c2d3e4f5a");
    private static final UUID DEX_ATK_UUID = UUID.fromString("e7b9fc05-5678-4e9f-d0a1-bc2d3e4f5a6b");
    // Passive skill modifier UUIDs
    private static final UUID ENDURANCE_HP_UUID = UUID.fromString("f8ca0d16-6789-4fa0-e1b2-cd3e4f5a6b7c");

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        lastPositions.remove(event.getEntity().getUUID());
        // Despawn Shadow Partner on logout
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel sl) {
            SkillExecutor.despawnShadowPartner(player, sl);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                stats.checkNewDay(player.level().getDayTime() / 24000L);
                applyStatModifiers(player, stats);
                syncToClient(player);
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                stats.getSkillData().clearTransient();
                applyStatModifiers(player, stats);
                syncToClient(player);
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                applyStatModifiers(player, stats);
                syncToClient(player);
            });
        }
    }

    // === Daily Quest Tracking ===

    @SubscribeEvent
    public static void onMobKill(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Monster)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            // Quest tracking
            if (!stats.isQuestCompleted()) {
                stats.addQuestKill();
                checkQuestCompletion(player, stats);
            }

            SkillData sd = stats.getSkillData();

            // Passive: MP Recovery — restore MP on kills
            int mpRecovLv = sd.getLevel(SkillType.MP_RECOVERY);
            if (mpRecovLv > 0) {
                int mpRestore = 2 + mpRecovLv;
                stats.setCurrentMp(Math.min(stats.getCurrentMp() + mpRestore, stats.getMaxMp()));
            }

            // Passive: Soul Siphon — restore HP% and MP% on kill
            int ssLv = sd.getLevel(SkillType.SOUL_SIPHON);
            if (ssLv > 0) {
                float hpRestore = player.getMaxHealth() * (1 + ssLv * 0.2f) / 100f;
                int mpRestore = (int) (stats.getMaxMp() * (2 + ssLv * 0.3f) / 100f);
                player.heal(hpRestore);
                CombatLog.heal(player, "Soul Siphon", hpRestore);
                stats.setCurrentMp(Math.min(stats.getCurrentMp() + mpRestore, stats.getMaxMp()));
            }

            // Death Mark: check if killed mob is the marked target
            if (sd.getDeathMarkTicks() > 0 && sd.getDeathMarkTargetId() >= 0) {
                if (event.getEntity().getId() == sd.getDeathMarkTargetId()) {
                    SkillExecutor.onDeathMarkTargetDeath(player, stats, sd, event.getEntity());
                }
            }

            // Passive: Blessed Ensemble — +5% XP per nearby player
            int beLv = sd.getLevel(SkillType.BLESSED_ENSEMBLE);
            if (beLv > 0) {
                List<ServerPlayer> nearby = player.level().getEntitiesOfClass(ServerPlayer.class,
                        player.getBoundingBox().inflate(10), p -> p != player);
                if (!nearby.isEmpty()) {
                    int bonusExp = (int) (nearby.size() * beLv * 0.05 * 10); // approximate XP bonus
                    if (bonusExp > 0) {
                        stats.addExp(bonusExp);
                    }
                }
            }

            syncToClient(player);
        });
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;

        player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            if (stats.isQuestCompleted()) return;
            stats.addQuestBlockMined();
            checkQuestCompletion(player, stats);
            syncToClient(player);
        });
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        // === Skill tick (every tick) ===
        player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            SkillData sd = stats.getSkillData();
            sd.tickCooldowns();

            // Shadow Strike expiry
            if (sd.isShadowStrikeActive()) {
                sd.setShadowStrikeTicks(sd.getShadowStrikeTicks() - 1);
                if (sd.getShadowStrikeTicks() <= 0) {
                    sd.setShadowStrikeActive(false);
                    player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Shadow Strike expired."));
                    syncToClient(player);
                }
            }

            // Domain of Monarch tick
            if (sd.getDomainTicks() > 0) {
                sd.setDomainTicks(sd.getDomainTicks() - 1);
                if (sd.getDomainTicks() <= 0) {
                    player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Domain faded."));
                    syncToClient(player);
                } else if (player.tickCount % 20 == 0) {
                    SkillExecutor.tickDomain(player, stats, sd);
                }
            }

            // Venom expiry
            if (sd.isVenomActive()) {
                sd.setVenomTicks(sd.getVenomTicks() - 1);
                if (sd.getVenomTicks() <= 0) {
                    sd.setVenomActive(false);
                    player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Venom wore off."));
                    syncToClient(player);
                }
            }

            // Phoenix tick (attacks every second)
            if (sd.getPhoenixTicks() > 0) {
                sd.setPhoenixTicks(sd.getPhoenixTicks() - 1);
                if (sd.getPhoenixTicks() <= 0) {
                    player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Phoenix departed."));
                    syncToClient(player);
                } else if (player.tickCount % 20 == 0) {
                    SkillExecutor.tickPhoenix(player, stats, sd);
                }
            }

            // Arrow Rain zone tick (every tick while active)
            if (sd.getArrowRainTicks() > 0) {
                sd.setArrowRainTicks(sd.getArrowRainTicks() - 1);
                if (sd.getArrowRainTicks() <= 0) {
                    player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Arrow Rain ended."));
                    syncToClient(player);
                } else {
                    SkillExecutor.tickArrowRain(player, sd);
                }
            }

            // Benediction zone tick
            if (sd.getBenedictionTicks() > 0) {
                sd.setBenedictionTicks(sd.getBenedictionTicks() - 1);
                if (sd.getBenedictionTicks() <= 0) {
                    player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Benediction faded."));
                    syncToClient(player);
                } else if (player.tickCount % 20 == 0) {
                    SkillExecutor.tickBenediction(player, stats, sd);
                }
            }

            // Poison Mist zone tick
            if (sd.isMistActive()) {
                sd.setMistTicks(sd.getMistTicks() - 1);
                if (sd.getMistTicks() <= 0) {
                    sd.setMistActive(false);
                    player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Poison Mist dissipated."));
                    syncToClient(player);
                } else if (player.tickCount % 20 == 0) {
                    SkillExecutor.tickPoisonMist(player, stats, sd);
                }
            }

            // Infinity buff tick (damage ramp every 4 seconds)
            if (sd.getInfinityTicks() > 0) {
                sd.setInfinityTicks(sd.getInfinityTicks() - 1);
                if (sd.getInfinityTicks() <= 0) {
                    player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Infinity ended."));
                    syncToClient(player);
                } else if (player.tickCount % 80 == 0) {
                    SkillExecutor.tickInfinity(player, sd);
                }
            }

            // Army of the Dead duration (despawn army minions when expired)
            if (sd.getArmyTicks() > 0) {
                sd.setArmyTicks(sd.getArmyTicks() - 1);
                if (sd.getArmyTicks() <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        SkillExecutor.despawnArmyMinions(player, sl);
                    }
                    player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Army of the Dead faded."));
                    syncToClient(player);
                }
            }

            // Death Mark tick
            if (sd.getDeathMarkTicks() > 0) {
                sd.setDeathMarkTicks(sd.getDeathMarkTicks() - 1);
                if (sd.getDeathMarkTicks() <= 0) {
                    sd.setDeathMarkTargetId(-1);
                    player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Death Mark expired."));
                    syncToClient(player);
                } else if (player.tickCount % 20 == 0) {
                    SkillExecutor.tickDeathMark(player, stats, sd);
                }
            }

            // Undying Will cooldown tick
            if (sd.getUndyingWillCooldown() > 0) {
                sd.setUndyingWillCooldown(sd.getUndyingWillCooldown() - 1);
            }
        });

        // === Regeneration effect tick logging ===
        MobEffectInstance regenEffect = player.getEffect(MobEffects.REGENERATION);
        if (regenEffect != null) {
            int regenInterval = Math.max(1, 50 >> regenEffect.getAmplifier());
            if (player.tickCount % regenInterval == 0) {
                CombatLog.heal(player, "Regen", 1.0f);
            }
        }

        // === Toggle skill MP drain + effect refresh (every second) ===
        if (player.tickCount % 20 == 0) {
            player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                SkillData sd = stats.getSkillData();
                boolean changed = false;

                // Iron Will drain
                if (sd.isToggleActive(SkillType.IRON_WILL)) {
                    int level = sd.getLevel(SkillType.IRON_WILL);
                    int drain = SkillType.IRON_WILL.getToggleMpPerSecond(level);
                    if (stats.getCurrentMp() >= drain) {
                        stats.setCurrentMp(stats.getCurrentMp() - drain);
                        SkillExecutor.applyIronWillEffects(player, stats, level);
                        if (player.tickCount % 40 == 0) {
                            SkillParticles.playerAura(player, 8, 1.0, ParticleTypes.ENCHANTED_HIT);
                        }
                        changed = true;
                    } else {
                        sd.setToggleActive(SkillType.IRON_WILL, false);
                        player.removeEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE);
                        player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Iron Will deactivated (no MP)."));
                        changed = true;
                    }
                }

                // Stealth drain
                if (sd.isToggleActive(SkillType.STEALTH)) {
                    int level = sd.getLevel(SkillType.STEALTH);
                    int drain = SkillType.STEALTH.getToggleMpPerSecond(level);
                    if (stats.getCurrentMp() >= drain) {
                        stats.setCurrentMp(stats.getCurrentMp() - drain);
                        SkillExecutor.applyStealthEffects(player, level);
                        changed = true;
                    } else {
                        SkillExecutor.breakStealth(player, sd);
                        changed = true;
                    }
                }

                // Soul Arrow drain
                if (sd.isToggleActive(SkillType.SOUL_ARROW)) {
                    int level = sd.getLevel(SkillType.SOUL_ARROW);
                    int drain = SkillType.SOUL_ARROW.getToggleMpPerSecond(level);
                    if (stats.getCurrentMp() >= drain) {
                        stats.setCurrentMp(stats.getCurrentMp() - drain);
                        // Ensure player always has at least 1 arrow
                        boolean hasArrow = false;
                        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                            if (player.getInventory().getItem(i).is(Items.ARROW)) {
                                hasArrow = true;
                                break;
                            }
                        }
                        if (!hasArrow) {
                            player.getInventory().add(new ItemStack(Items.ARROW, 1));
                        }
                        if (player.tickCount % 40 == 0) {
                            SkillParticles.playerAura(player, 5, 0.6, ParticleTypes.END_ROD);
                        }
                        changed = true;
                    } else {
                        sd.setToggleActive(SkillType.SOUL_ARROW, false);
                        player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Soul Arrow deactivated (no MP)."));
                        changed = true;
                    }
                }

                // Shadow Partner drain
                if (sd.isToggleActive(SkillType.SHADOW_PARTNER)) {
                    int level = sd.getLevel(SkillType.SHADOW_PARTNER);
                    int drain = SkillType.SHADOW_PARTNER.getToggleMpPerSecond(level);
                    if (stats.getCurrentMp() >= drain) {
                        stats.setCurrentMp(stats.getCurrentMp() - drain);
                        changed = true;
                    } else {
                        sd.setToggleActive(SkillType.SHADOW_PARTNER, false);
                        if (player.level() instanceof ServerLevel sl) {
                            SkillExecutor.despawnShadowPartner(player, sl);
                        }
                        player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Shadow Partner deactivated (no MP)."));
                        changed = true;
                    }
                }

                // Magic Guard drain
                if (sd.isToggleActive(SkillType.MAGIC_GUARD)) {
                    int level = sd.getLevel(SkillType.MAGIC_GUARD);
                    int drain = SkillType.MAGIC_GUARD.getToggleMpPerSecond(level);
                    if (stats.getCurrentMp() >= drain) {
                        stats.setCurrentMp(stats.getCurrentMp() - drain);
                        if (player.tickCount % 40 == 0) {
                            SkillParticles.playerFeet(player, 8, 0.8, ParticleTypes.ENCHANTED_HIT);
                        }
                        changed = true;
                    } else {
                        sd.setToggleActive(SkillType.MAGIC_GUARD, false);
                        player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Magic Guard deactivated (no MP)."));
                        changed = true;
                    }
                }

                // Hurricane drain + tick
                if (sd.isToggleActive(SkillType.HURRICANE)) {
                    int level = sd.getLevel(SkillType.HURRICANE);
                    int drain = SkillType.HURRICANE.getToggleMpPerSecond(level);
                    if (stats.getCurrentMp() >= drain) {
                        stats.setCurrentMp(stats.getCurrentMp() - drain);
                        SkillExecutor.tickHurricane(player, stats, sd);
                        changed = true;
                    } else {
                        sd.setToggleActive(SkillType.HURRICANE, false);
                        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                        player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Hurricane deactivated (no MP)."));
                        changed = true;
                    }
                }

                // Raise Skeleton drain
                if (sd.isToggleActive(SkillType.RAISE_SKELETON)) {
                    int level = sd.getLevel(SkillType.RAISE_SKELETON);
                    int drain = SkillType.RAISE_SKELETON.getToggleMpPerSecond(level);
                    if (stats.getCurrentMp() >= drain) {
                        stats.setCurrentMp(stats.getCurrentMp() - drain);
                        changed = true;
                    } else {
                        sd.setToggleActive(SkillType.RAISE_SKELETON, false);
                        if (player.level() instanceof ServerLevel sl) {
                            SkillExecutor.despawnSkeletonMinion(player, sl);
                        }
                        player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Raise Skeleton deactivated (no MP)."));
                        changed = true;
                    }
                }

                // Bone Shield drain
                if (sd.isToggleActive(SkillType.BONE_SHIELD)) {
                    int level = sd.getLevel(SkillType.BONE_SHIELD);
                    int drain = SkillType.BONE_SHIELD.getToggleMpPerSecond(level);
                    if (stats.getCurrentMp() >= drain) {
                        stats.setCurrentMp(stats.getCurrentMp() - drain);
                        if (player.tickCount % 40 == 0 && player.level() instanceof ServerLevel sl) {
                            SkillParticles.ring(sl, player.getX(), player.getY() + 1, player.getZ(), 1.2, 8, ParticleTypes.ENCHANTED_HIT);
                        }
                        changed = true;
                    } else {
                        sd.setToggleActive(SkillType.BONE_SHIELD, false);
                        player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Bone Shield deactivated (no MP)."));
                        changed = true;
                    }
                }

                // Passive: Endurance HP regen (+5% HP regen per level, applies as Regen I every few seconds)
                int enduranceLv = sd.getLevel(SkillType.ENDURANCE);
                if (enduranceLv > 0 && player.tickCount % 60 == 0) {
                    float regenAmount = player.getMaxHealth() * enduranceLv * 0.005f;
                    player.heal(regenAmount);
                }

                // Passive: MP Recovery (restore MP passively)
                int mpRecovLv = sd.getLevel(SkillType.MP_RECOVERY);
                if (mpRecovLv > 0) {
                    int maxMp = stats.getMaxMp();
                    int mpRegen = (int) (maxMp * mpRecovLv * 0.002);
                    if (mpRegen > 0 && stats.getCurrentMp() < maxMp) {
                        stats.setCurrentMp(Math.min(stats.getCurrentMp() + mpRegen, maxMp));
                        changed = true;
                    }
                }

                if (changed) syncToClient(player);
            });
        }

        // Check for new day every 5 seconds
        if (player.tickCount % 100 == 0) {
            player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                long currentDay = player.level().getDayTime() / 24000L;
                if (stats.checkNewDay(currentDay)) {
                    player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a7eA new Daily Quest has arrived."));
                    syncToClient(player);
                }
            });
        }

        // MP regen every second (always, even during toggle drain)
        if (player.tickCount % 20 == 0) {
            player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                if (stats.regenMp()) {
                    syncToClient(player);
                }
            });
        }

        // Walk tracking every second
        if (player.tickCount % 20 == 0) {
            UUID pid = player.getUUID();
            double[] last = lastPositions.get(pid);
            double cx = player.getX();
            double cz = player.getZ();

            if (last != null) {
                double dx = cx - last[0];
                double dz = cz - last[1];
                int dist = (int) Math.sqrt(dx * dx + dz * dz);
                if (dist > 0) {
                    player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                        if (stats.isQuestCompleted()) return;
                        stats.addQuestBlockWalked(dist);
                        checkQuestCompletion(player, stats);
                        syncToClient(player);
                    });
                }
            }
            lastPositions.put(pid, new double[]{cx, cz});
        }
    }

    // === Soul Arrow: prevent arrow consumption ===

    @SubscribeEvent
    public static void onArrowLoose(ArrowLooseEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            SkillData sd = stats.getSkillData();
            if (sd.isToggleActive(SkillType.SOUL_ARROW)) {
                // Give back 1 arrow to offset the consumption that happens after this event
                player.getInventory().add(new ItemStack(Items.ARROW, 1));
            }
        });
    }

    // === Shadow Partner: mirror ranged attacks (bow, trident) ===

    @SubscribeEvent
    public static void onProjectileJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;

        // Check if a player-owned arrow or trident just spawned
        net.minecraft.world.entity.Entity entity = event.getEntity();
        // Skip our own skill arrows to avoid infinite loops
        if (entity instanceof SkillArrowEntity) return;

        // Extract player owner from projectile
        final ServerPlayer player;
        if (entity instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow
                && arrow.getOwner() instanceof ServerPlayer sp) {
            player = sp;
        } else {
            return;
        }

        player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            SkillData sd = stats.getSkillData();
            if (!sd.isToggleActive(SkillType.SHADOW_PARTNER)) return;

            if (!(player.level() instanceof ServerLevel sl)) return;
            ShadowPartnerEntity partner = SkillExecutor.findShadowPartner(player, sl);
            if (partner == null) return;

            int spLv = sd.getLevel(SkillType.SHADOW_PARTNER);
            float multiplier = SkillExecutor.getShadowPartnerDamageMultiplier(spLv);

            // Check trident first since ThrownTrident extends AbstractArrow
            if (entity instanceof net.minecraft.world.entity.projectile.ThrownTrident) {
                net.minecraft.world.entity.projectile.ThrownTrident shadowTrident =
                        new net.minecraft.world.entity.projectile.ThrownTrident(sl, partner,
                                new ItemStack(Items.TRIDENT));
                shadowTrident.shootFromRotation(partner, player.getXRot(), player.getYRot(), 0, 2.5f, 3.0f);
                shadowTrident.setBaseDamage(8.0 * multiplier);
                shadowTrident.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED;
                sl.addFreshEntity(shadowTrident);
            } else if (entity instanceof net.minecraft.world.entity.projectile.AbstractArrow origArrow) {
                net.minecraft.world.entity.projectile.Arrow shadowArrow =
                        new net.minecraft.world.entity.projectile.Arrow(sl, partner);
                shadowArrow.shootFromRotation(partner, player.getXRot(), player.getYRot(), 0, 2.5f, 3.0f);
                shadowArrow.setBaseDamage(origArrow.getBaseDamage() * multiplier);
                shadowArrow.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED;
                sl.addFreshEntity(shadowArrow);
            } else {
                return;
            }
            partner.swing(InteractionHand.MAIN_HAND);
            SkillParticles.burst(sl, partner.getX(), partner.getY() + 1, partner.getZ(), 3, 0.2, ParticleTypes.PORTAL);

            // Combat log
            String type = (entity instanceof net.minecraft.world.entity.projectile.ThrownTrident) ? "Trident" : "Arrow";
            float logDmg = (entity instanceof net.minecraft.world.entity.projectile.AbstractArrow a) ? (float)(a.getBaseDamage() * multiplier) : 8.0f * multiplier;
            CombatLog.petRanged(player, type, logDmg);
        });
    }

    // === Skill Combat Events ===

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            SkillData sd = stats.getSkillData();
            float amount = event.getAmount();

            // Shadow Strike bonus damage
            if (sd.isShadowStrikeActive()) {
                float bonus = SkillExecutor.getShadowStrikeDamage(stats);
                amount += bonus;
                sd.setShadowStrikeActive(false);
                sd.setShadowStrikeTicks(0);
                if (player.level() instanceof ServerLevel sl) {
                    net.minecraft.world.entity.LivingEntity target = (net.minecraft.world.entity.LivingEntity) event.getEntity();
                    SkillParticles.burst(sl, target.getX(), target.getY() + 1, target.getZ(), 10, 0.4, ParticleTypes.CRIT);
                    SkillParticles.burst(sl, target.getX(), target.getY() + 1, target.getZ(), 8, 0.3, ParticleTypes.ENCHANTED_HIT);
                    SkillSounds.playAt(player, SoundEvents.PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
                }
                player.sendSystemMessage(Component.literal(
                        "\u00a7b[System]\u00a7r \u00a7eShadow Strike hit! +" + String.format("%.1f", bonus) + " damage"));
                syncToClient(player);
            }

            // Stealth breaks on attack
            if (sd.isToggleActive(SkillType.STEALTH)) {
                SkillExecutor.breakStealth(player, sd);
                syncToClient(player);
            }

            // Venom: apply poison on hit (Wither for undead since they're immune to Poison)
            if (sd.isVenomActive() && event.getEntity() instanceof Monster mob) {
                int venomLv = sd.getLevel(SkillType.VENOM);
                int poisonDur = SkillExecutor.getVenomPoisonDuration(venomLv);
                int poisonAmp = Math.min(venomLv / 4, 2);
                if (mob.getMobType() == net.minecraft.world.entity.MobType.UNDEAD) {
                    mob.addEffect(new MobEffectInstance(MobEffects.WITHER, poisonDur, poisonAmp, false, true));
                } else {
                    mob.addEffect(new MobEffectInstance(MobEffects.POISON, poisonDur, poisonAmp, false, true));
                }
                // Direct magic damage on every hit (bypasses armor intentionally)
                float venomDmg = 0.5f + venomLv * 0.15f;
                mob.hurt(player.damageSources().magic(), venomDmg);
                if (player.level() instanceof ServerLevel sl) {
                    SkillParticles.burst(sl, mob.getX(), mob.getY() + 1, mob.getZ(), 8, 0.4, ParticleTypes.ITEM_SLIME);
                }
            }

            // Passive: Weapon Mastery — +1% melee damage per level
            int wmLv = sd.getLevel(SkillType.WEAPON_MASTERY);
            if (wmLv > 0 && !event.getSource().isIndirect()) {
                amount *= 1.0f + wmLv * 0.01f;
            }

            // Passive: Rage — +2% damage per level when below 50% HP, +0.5% lifesteal
            int rageLv = sd.getLevel(SkillType.RAGE);
            if (rageLv > 0) {
                if (player.getHealth() < player.getMaxHealth() * 0.5f) {
                    amount *= 1.0f + rageLv * 0.02f;
                }
                float lifesteal = amount * rageLv * 0.005f;
                if (lifesteal > 0) {
                    player.heal(lifesteal);
                    CombatLog.heal(player, "Rage", lifesteal);
                }
            }

            // Passive: Elemental Drain — +5% damage per active debuff on target (max +25%)
            int edLv = sd.getLevel(SkillType.ELEMENTAL_DRAIN);
            if (edLv > 0) {
                net.minecraft.world.entity.LivingEntity target = (net.minecraft.world.entity.LivingEntity) event.getEntity();
                int debuffCount = 0;
                for (MobEffectInstance effect : target.getActiveEffects()) {
                    if (!effect.getEffect().isBeneficial()) debuffCount++;
                }
                float debuffBonus = Math.min(debuffCount * 0.05f * edLv, 0.25f);
                amount *= 1.0f + debuffBonus;
            }

            // Passive: Element Amplification — +3% skill damage per level (magic damage sources)
            int eaLv = sd.getLevel(SkillType.ELEMENT_AMPLIFICATION);
            if (eaLv > 0 && event.getSource().is(net.minecraft.tags.DamageTypeTags.WITCH_RESISTANT_TO)) {
                amount *= 1.0f + eaLv * 0.03f;
            }

            // Passive: Fatal Blow — +2% damage vs targets below 30% HP, execute chance
            int fbLv = sd.getLevel(SkillType.FATAL_BLOW);
            if (fbLv > 0) {
                net.minecraft.world.entity.LivingEntity target = (net.minecraft.world.entity.LivingEntity) event.getEntity();
                if (target.getHealth() < target.getMaxHealth() * 0.3f) {
                    amount *= 1.0f + fbLv * 0.02f;
                    if (player.getRandom().nextFloat() < fbLv * 0.005f) {
                        amount = target.getHealth() + 100; // Execute
                    }
                }
            }

            // Passive: Mortal Blow — same as Fatal Blow but for projectiles
            int mbLv = sd.getLevel(SkillType.MORTAL_BLOW);
            if (mbLv > 0 && event.getSource().isIndirect()) {
                net.minecraft.world.entity.LivingEntity target = (net.minecraft.world.entity.LivingEntity) event.getEntity();
                if (target.getHealth() < target.getMaxHealth() * 0.3f) {
                    amount *= 1.0f + mbLv * 0.02f;
                    if (player.getRandom().nextFloat() < mbLv * 0.005f) {
                        amount = target.getHealth() + 100;
                    }
                }
            }

            // Crit calculation: base LUK crit + Critical Edge + Sharp Eyes + Arcane Overdrive + Evasion crit ready + Berserker Spirit
            double critRate = 0;
            double critDmgBonus = 1.5;

            int luk = stats.getLuck();
            if (luk > 1) {
                critRate += (luk - 1) * 0.001;
                critDmgBonus += ((luk - 1) / 50) * 0.10;
            }

            int ceLv = sd.getLevel(SkillType.CRITICAL_EDGE);
            if (ceLv > 0) {
                critRate += ceLv * 0.01;
                critDmgBonus += ceLv * 0.02;
            }

            int seLv = sd.getLevel(SkillType.SHARP_EYES);
            if (seLv > 0) {
                critRate += seLv * 0.015;
                critDmgBonus += seLv * 0.05;
            }

            int aoLv = sd.getLevel(SkillType.ARCANE_OVERDRIVE);
            if (aoLv > 0) {
                critRate += aoLv * 0.015;
                critDmgBonus += aoLv * 0.01;
            }

            int bsLv = sd.getLevel(SkillType.BERSERKER_SPIRIT);
            if (bsLv > 0) {
                critRate += bsLv * 0.01;
                // Double strike chance
                if (player.getRandom().nextFloat() < bsLv * 0.01f) {
                    amount *= 1.5f;
                }
                // Lifesteal
                float bsHeal = amount * bsLv * 0.003f;
                player.heal(bsHeal);
                CombatLog.heal(player, "Berserker", bsHeal);
            }

            // Evasion crit ready (guaranteed crit on next hit after dodge)
            if (sd.isEvasionCritReady()) {
                critRate = 1.0;
                sd.setEvasionCritReady(false);
            }

            if (critRate > 0 && player.getRandom().nextDouble() < critRate) {
                amount = (float) (amount * critDmgBonus);
                if (player.level() instanceof ServerLevel sl) {
                    net.minecraft.world.entity.LivingEntity target = (net.minecraft.world.entity.LivingEntity) event.getEntity();
                    SkillParticles.burst(sl, target.getX(), target.getY() + 1.5, target.getZ(), 12, 0.4, ParticleTypes.CRIT);
                    SkillParticles.burst(sl, target.getX(), target.getY() + 1, target.getZ(), 6, 0.3, ParticleTypes.ENCHANTED_HIT);
                }
                player.sendSystemMessage(Component.literal(
                        "\u00a76\u2726 Critical Hit! \u00a7r(" + String.format("%.0f%%", critDmgBonus * 100) + ")"));
            }

            // Projectile damage multiplier: applies to ALL arrows (player bow + skill arrows)
            if (event.getSource().isIndirect()) {
                float projMult = 1.0f;
                // DEX: +0.1% per point
                int dex = stats.getDexterity();
                if (dex > 1) projMult += (dex - 1) * 0.001f;
                // Soul Arrow: +5-15% when active
                if (sd.isToggleActive(SkillType.SOUL_ARROW)) {
                    int saLv = sd.getLevel(SkillType.SOUL_ARROW);
                    projMult += 0.05f + saLv * 0.01f;
                }
                amount *= projMult;
            }

            // Shadow Partner mirror attack — teleport to target, hit, teleport back
            if (sd.isToggleActive(SkillType.SHADOW_PARTNER) && event.getEntity() instanceof Monster mob) {
                int spLv = sd.getLevel(SkillType.SHADOW_PARTNER);
                float mirrorDmg = amount * SkillExecutor.getShadowPartnerDamageMultiplier(spLv);
                if (player.level() instanceof ServerLevel sl) {
                    ShadowPartnerEntity partner = SkillExecutor.findShadowPartner(player, sl);
                    if (partner != null && !event.getSource().isIndirect()) {
                        // Melee: partner teleports to target, attacks, returns
                        partner.performMirrorAttack(mob, mirrorDmg);
                    } else {
                        // Ranged/no partner found: apply bonus magic damage directly (bypasses armor)
                        mob.hurt(player.damageSources().magic(), mirrorDmg);
                        SkillParticles.burst(sl, mob.getX(), mob.getY() + 1, mob.getZ(), 6, 0.3, ParticleTypes.PORTAL);
                    }
                }
            }

            event.setAmount(amount);
        });
    }

    // === Final damage log (fires after armor, toughness, enchant reduction) ===

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getAmount() <= 0) return;
        if (CombatLog.suppressDamageLog) return;

        // Skeleton minion damage → log to owner
        if (event.getSource().getEntity() instanceof SkeletonMinionEntity minion) {
            ServerPlayer owner = minion.getOwnerPlayer();
            if (owner != null) {
                CombatLog.damage(owner, "Skeleton", event.getAmount(), event.getEntity());
            }
            return;
        }

        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        // Shadow Partner melee has its own logging
        if (event.getSource().getDirectEntity() instanceof ShadowPartnerEntity) return;
        CombatLog.damage(player, event.getAmount(), event.getEntity());
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewTarget() instanceof ServerPlayer player)) return;

        player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            SkillData sd = stats.getSkillData();
            if (!sd.isToggleActive(SkillType.STEALTH)) return;

            int level = sd.getLevel(SkillType.STEALTH);
            double detectionRange = SkillExecutor.getStealthDetectionRange(level);
            double dist = event.getEntity().distanceTo(player);
            if (dist > detectionRange) {
                event.setCanceled(true);
            }
        });
    }

    // === DOT damage logging (poison/fire/wither ticks on mobs near player) ===

    @SubscribeEvent
    public static void onMobDotDamage(LivingHurtEvent event) {
        // Only handle DOT with no direct attacker entity
        if (event.getSource().getEntity() != null) return;
        if (!(event.getEntity() instanceof Monster mob)) return;
        if (!(mob.level() instanceof ServerLevel sl)) return;

        String dotType = null;
        String msgId = event.getSource().getMsgId();
        if ("onFire".equals(msgId)) dotType = "Burn";
        else if ("magic".equals(msgId) && mob.hasEffect(MobEffects.POISON)) dotType = "Poison";
        else if ("wither".equals(msgId)) dotType = "Wither";
        if (dotType == null) return;

        // Find nearest player within 32 blocks who has relevant skills
        List<ServerPlayer> nearby = sl.getEntitiesOfClass(ServerPlayer.class,
                mob.getBoundingBox().inflate(32));
        if (nearby.isEmpty()) return;
        ServerPlayer player = nearby.get(0);
        String dot = dotType;
        player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            SkillData sd = stats.getSkillData();
            boolean relevant = sd.isVenomActive() || sd.isMistActive()
                    || sd.getCooldownRemaining(SkillType.FLAME_ORB) > 0
                    || sd.getCooldownRemaining(SkillType.MIST_ERUPTION) > 0
                    || sd.getPhoenixTicks() > 0;
            if (relevant) {
                CombatLog.damage(player, dot, event.getAmount(), mob);
            }
        });
    }

    // === Player taking damage: Stealth break, Magic Guard, Evasion, Divine Protection ===

    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            SkillData sd = stats.getSkillData();

            // Stealth breaks on taking damage
            if (sd.isToggleActive(SkillType.STEALTH)) {
                SkillExecutor.breakStealth(player, sd);
                syncToClient(player);
            }

            // Passive: Evasion — 2% dodge per level, dodge guarantees next crit
            int evLv = sd.getLevel(SkillType.EVASION);
            if (evLv > 0 && player.getRandom().nextFloat() < evLv * 0.02f) {
                event.setCanceled(true);
                sd.setEvasionCritReady(true);
                if (player.level() instanceof ServerLevel sl) {
                    SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 8, 0.4, ParticleTypes.CLOUD);
                    SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 5, 0.3, ParticleTypes.POOF);
                }
                player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a7eDodged!"));
                syncToClient(player);
                return;
            }

            // Passive: Evasion Boost (Archer) — 2% dodge per level
            int ebLv = sd.getLevel(SkillType.EVASION_BOOST);
            if (ebLv > 0 && player.getRandom().nextFloat() < ebLv * 0.02f) {
                event.setCanceled(true);
                sd.setEvasionCritReady(true);
                if (player.level() instanceof ServerLevel sl) {
                    SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 8, 0.4, ParticleTypes.CLOUD);
                    SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 5, 0.3, ParticleTypes.POOF);
                }
                player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a7eDodged!"));
                syncToClient(player);
                return;
            }

            // Magic Guard: redirect damage to MP
            if (sd.isToggleActive(SkillType.MAGIC_GUARD)) {
                int mgLv = sd.getLevel(SkillType.MAGIC_GUARD);
                float ratio = SkillExecutor.getMagicGuardRedirectRatio(mgLv);
                float redirected = event.getAmount() * ratio;
                int mpCost = (int) (redirected * 2); // 2 MP per 1 damage redirected
                if (stats.getCurrentMp() >= mpCost) {
                    stats.setCurrentMp(stats.getCurrentMp() - mpCost);
                    event.setAmount(event.getAmount() - redirected);
                    SkillParticles.playerAura(player, 6, 0.8, ParticleTypes.ENCHANTED_HIT);
                    syncToClient(player);
                }
            }

            // Bone Shield: reduce incoming damage
            if (sd.isToggleActive(SkillType.BONE_SHIELD)) {
                int bsLv = sd.getLevel(SkillType.BONE_SHIELD);
                float reduction = SkillExecutor.getBoneShieldReduction(bsLv) / 100.0f;
                event.setAmount(event.getAmount() * (1 - reduction));
            }

            // Passive: Divine Protection — auto-cleanse chance (3% per level per tick, checked on hit)
            int dpLv = sd.getLevel(SkillType.DIVINE_PROTECTION);
            if (dpLv > 0 && player.getRandom().nextFloat() < dpLv * 0.03f) {
                player.removeEffect(MobEffects.POISON);
                player.removeEffect(MobEffects.WITHER);
                player.removeEffect(MobEffects.WEAKNESS);
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a7aDivine Protection cleansed debuffs!"));
            }
        });
    }

    // === Undying Will: prevent fatal damage (post-armor) ===

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void onPlayerDamagePost(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.getHealth() - event.getAmount() > 0) return; // not fatal

        player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            SkillData sd = stats.getSkillData();
            int uwLv = sd.getLevel(SkillType.UNDYING_WILL);
            if (uwLv <= 0 || sd.getUndyingWillCooldown() > 0) return;

            float revivalPct = (10 + uwLv) / 100.0f;
            float revivalHp = Math.max(player.getMaxHealth() * revivalPct, 1);
            event.setAmount(0);
            player.setHealth(revivalHp);
            int cooldownTicks = Math.max(3600 - uwLv * 90, 1800);
            sd.setUndyingWillCooldown(cooldownTicks);

            if (player.level() instanceof ServerLevel sl) {
                SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 20, 1.0, ParticleTypes.SOUL_FIRE_FLAME);
                SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 15, 0.8, ParticleTypes.TOTEM_OF_UNDYING);
                SkillSounds.playAt(player, SoundEvents.TOTEM_USE, 0.8f, 0.9f);
            }
            int cdSecs = cooldownTicks / 20;
            player.sendSystemMessage(Component.literal(
                    "\u00a7b[System]\u00a7r \u00a75Undying Will! Revived with " + (int)(revivalPct * 100) + "% HP. (" + cdSecs + "s cooldown)"));
            syncToClient(player);
        });
    }

    private static void checkQuestCompletion(ServerPlayer player, PlayerStats stats) {
        if (stats.isQuestCompleted() || !stats.areQuestObjectivesMet()) return;
        stats.setQuestCompleted(true);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7aDaily Quest Complete!\u00a7r Open the Quest screen to claim your reward."
        ));
    }

    public static void claimQuestReward(ServerPlayer player) {
        player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            if (!stats.isQuestCompleted() || stats.isQuestRewardClaimed()) return;
            stats.setQuestRewardClaimed(true);

            // EXP reward
            int expReward = stats.getQuestExpReward();
            int[] spBefore = stats.getSkillData().getTierSPArray().clone();
            int levelsGained = stats.addExp(expReward);

            // Status Recovery: full HP, MP, hunger, clear debuffs
            player.setHealth(player.getMaxHealth());
            stats.setCurrentMp(stats.getMaxMp());
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(5.0f);
            player.removeAllEffects();

            if (levelsGained > 0) {
                applyStatModifiers(player, stats);
            }

            player.sendSystemMessage(Component.literal(
                    "\u00a7b[System]\u00a7r \u00a7aReward Claimed!\u00a7r +" + expReward + " EXP. Status Recovery applied."
            ));
            if (levelsGained > 0) {
                int[] spAfter = stats.getSkillData().getTierSPArray();
                StringBuilder spMsg = new StringBuilder();
                for (int t = 0; t < 4; t++) {
                    int gained = spAfter[t] - spBefore[t];
                    if (gained > 0) {
                        if (spMsg.length() > 0) spMsg.append(", ");
                        spMsg.append("+").append(gained).append(" T").append(t).append(" SP");
                    }
                }
                player.sendSystemMessage(Component.literal(
                        "\u00a7b[System]\u00a7r \u00a76Level Up!\u00a7r You are now Level " + stats.getLevel()
                                + ". (+" + (levelsGained * 4) + " stat points"
                                + (spMsg.length() > 0 ? ", " + spMsg : "") + ")"
                ));
            }

            syncToClient(player);
        });
    }

    // === Stat Modifiers ===

    public static void applyStatModifiers(ServerPlayer player, PlayerStats stats) {
        // STR: 0.1 atk per point
        applyModifier(player, Attributes.ATTACK_DAMAGE, STRENGTH_UUID, "Project Leveling Strength",
                (stats.getStrength() - 1) * 0.1);
        applyModifier(player, Attributes.MAX_HEALTH, VITALITY_UUID, "Project Leveling Vitality",
                (stats.getVitality() - 1) * 0.5);
        applyModifier(player, Attributes.ATTACK_SPEED, AGILITY_UUID, "Project Leveling Agility",
                (stats.getAgility() - 1) * 0.01);
        // LUK: 0.05 atk per point
        applyModifier(player, Attributes.ATTACK_DAMAGE, LUCK_ATK_UUID, "Project Leveling Luck ATK",
                (stats.getLuck() - 1) * 0.05);
        // DEX: 0.05 atk per point
        applyModifier(player, Attributes.ATTACK_DAMAGE, DEX_ATK_UUID, "Project Leveling Dex ATK",
                (stats.getDexterity() - 1) * 0.05);

        // Passive: Endurance — +2% max HP per level
        SkillData sd = stats.getSkillData();
        int enduranceLv = sd.getLevel(SkillType.ENDURANCE);
        double enduranceHpBonus = enduranceLv * 0.02 * (20 + (stats.getVitality() - 1) * 0.5);
        applyModifier(player, Attributes.MAX_HEALTH, ENDURANCE_HP_UUID, "Endurance HP",
                enduranceHpBonus);

        float maxHealth = player.getMaxHealth();
        if (player.getHealth() > maxHealth) {
            player.setHealth(maxHealth);
        }
    }

    private static void applyModifier(ServerPlayer player, net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                       UUID uuid, String name, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;

        AttributeModifier existing = instance.getModifier(uuid);
        if (existing != null) {
            instance.removeModifier(uuid);
        }

        if (amount > 0) {
            instance.addPermanentModifier(new AttributeModifier(uuid, name, amount, AttributeModifier.Operation.ADDITION));
        }
    }

    public static void syncToClient(ServerPlayer player) {
        player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            ModNetwork.sendToPlayer(new S2CSyncStatsPacket(stats), player);
        });
    }
}
