package com.monody.projectleveling.event;

import com.monody.projectleveling.ProjectLeveling;
import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.capability.PlayerStatsCapability;
import com.monody.projectleveling.network.ModNetwork;
import com.monody.projectleveling.network.S2CSyncStatsPacket;
import com.monody.projectleveling.skill.CombatLog;
import com.monody.projectleveling.skill.SkillData;
import com.monody.projectleveling.skill.SkillExecutor;
import com.monody.projectleveling.entity.archer.SkillArrowEntity;
import com.monody.projectleveling.entity.assassin.ShadowPartnerEntity;
import com.monody.projectleveling.entity.mage.SkillFireballEntity;
import com.monody.projectleveling.entity.necromancer.SkeletonMinionEntity;
import com.monody.projectleveling.skill.SkillDamageSource;
import com.monody.projectleveling.skill.SkillParticles;
import com.monody.projectleveling.skill.classes.*;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
    // Recursion guard for Final Attack
    private static boolean finalAttackInProgress = false;

    private static final UUID STRENGTH_UUID = UUID.fromString("a3d5b8c1-1234-4a5b-9c6d-7e8f9a0b1c2d");
    private static final UUID VITALITY_UUID = UUID.fromString("b4e6c9d2-2345-4b6c-ad7e-8f9a0b1c2d3e");
    private static final UUID AGILITY_UUID = UUID.fromString("c5f7dae3-3456-4c7d-be8f-9a0b1c2d3e4f");
    private static final UUID LUCK_ATK_UUID = UUID.fromString("d6a8ebf4-4567-4d8e-cf90-ab1c2d3e4f5a");
    private static final UUID DEX_ATK_UUID = UUID.fromString("e7b9fc05-5678-4e9f-d0a1-bc2d3e4f5a6b");
    // Passive skill modifier UUIDs
    private static final UUID ENDURANCE_HP_UUID = UUID.fromString("f8ca0d16-6789-4fa0-e1b2-cd3e4f5a6b7c");
    private static final UUID WARRIOR_MASTERY_HP_UUID = UUID.fromString("a1b2c3d4-5678-4e9f-a1b2-c3d4e5f6a7b8");
    private static final UUID WARRIOR_MASTERY_KB_UUID = UUID.fromString("b2c3d4e5-6789-4fa0-b2c3-d4e5f6a7b8c9");

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        lastPositions.remove(event.getEntity().getUUID());
        // Despawn Shadow Partner on logout
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel sl) {
            AssassinSkills.despawnShadowPartner(player, sl);
            NinjaSkills.despawnShadowClone(player, sl);
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
                float hpRestore = player.getMaxHealth() * (0.5f + ssLv * 0.15f) / 100f;
                int mpRestore = (int) (stats.getMaxMp() * (1 + ssLv * 0.2f) / 100f);
                player.heal(hpRestore);
                CombatLog.heal(player, "Soul Siphon", hpRestore);
                stats.setCurrentMp(Math.min(stats.getCurrentMp() + mpRestore, stats.getMaxMp()));
            }

            // Death Mark: check if killed mob is the marked target
            if (sd.getDeathMarkTicks() > 0 && sd.getDeathMarkTargetId() >= 0) {
                if (event.getEntity().getId() == sd.getDeathMarkTargetId()) {
                    NecromancerSkills.onDeathMarkTargetDeath(player, stats, sd, event.getEntity());
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

            // Slash Blast buff expiry (4s)
            if (sd.isSlashBlastActive() && sd.getSlashBlastTicks() > 0) {
                sd.setSlashBlastTicks(sd.getSlashBlastTicks() - 1);
                if (sd.getSlashBlastTicks() <= 0) {
                    sd.setSlashBlastActive(false);
                    sd.setSlashBlastPct(0);
                    player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Slash Blast expired."));
                    syncToClient(player);
                }
            }

            // War Cry buff tick
            if (sd.getWarCryTicks() > 0) {
                sd.setWarCryTicks(sd.getWarCryTicks() - 1);
                if (sd.getWarCryTicks() <= 0) {
                    sd.setWarCryAtkBonus(0);
                    player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77War Cry faded."));
                    syncToClient(player);
                }
            }

            // Spirit Blade buff tick
            if (sd.getSpiritBladeTicks() > 0) {
                sd.setSpiritBladeTicks(sd.getSpiritBladeTicks() - 1);
                if (sd.getSpiritBladeTicks() <= 0) {
                    sd.setSpiritBladeAtk(0);
                    sd.setSpiritBladeDefActive(false);
                    player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Spirit Blade faded."));
                    syncToClient(player);
                }
            }

            // Unbreakable cooldown tick
            if (sd.getUnbreakableCooldown() > 0) {
                sd.setUnbreakableCooldown(sd.getUnbreakableCooldown() - 1);
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
                    ArcherSkills.tickPhoenix(player, stats, sd);
                }
            }

            // Arrow Rain zone tick (every tick while active)
            if (sd.getArrowRainTicks() > 0) {
                sd.setArrowRainTicks(sd.getArrowRainTicks() - 1);
                if (sd.getArrowRainTicks() <= 0) {
                    player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Arrow Rain ended."));
                    syncToClient(player);
                } else {
                    ArcherSkills.tickArrowRain(player, sd);
                }
            }

            // Benediction zone tick
            if (sd.getBenedictionTicks() > 0) {
                sd.setBenedictionTicks(sd.getBenedictionTicks() - 1);
                if (sd.getBenedictionTicks() <= 0) {
                    player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Benediction faded."));
                    syncToClient(player);
                } else if (player.tickCount % 20 == 0) {
                    HealerSkills.tickBenediction(player, stats, sd);
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
                    MageSkills.tickPoisonMist(player, stats, sd);
                }
            }

            // Infinity buff tick (damage ramp every 4 seconds)
            if (sd.getInfinityTicks() > 0) {
                sd.setInfinityTicks(sd.getInfinityTicks() - 1);
                if (sd.getInfinityTicks() <= 0) {
                    sd.setInfinityStacks(0);
                    player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Infinity ended."));
                    syncToClient(player);
                } else if (player.tickCount % 80 == 0) {
                    MageSkills.tickInfinity(player, sd);
                }
            }

            // Army of the Dead duration (despawn army minions when expired)
            if (sd.getArmyTicks() > 0) {
                sd.setArmyTicks(sd.getArmyTicks() - 1);
                if (sd.getArmyTicks() <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        NecromancerSkills.despawnArmyMinions(player, sl);
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
                    NecromancerSkills.tickDeathMark(player, stats, sd);
                }
            }

            // Undying Will cooldown tick
            if (sd.getUndyingWillCooldown() > 0) {
                sd.setUndyingWillCooldown(sd.getUndyingWillCooldown() - 1);
            }

            // Substitution Jutsu tick countdown
            if (sd.getSubstitutionTicks() > 0) {
                sd.setSubstitutionTicks(sd.getSubstitutionTicks() - 1);
                if (sd.getSubstitutionTicks() <= 0) {
                    player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Substitution expired."));
                    syncToClient(player);
                }
            }

            // Eight Inner Gates invuln cooldown
            if (sd.getGatesInvulnCooldown() > 0) {
                sd.setGatesInvulnCooldown(sd.getGatesInvulnCooldown() - 1);
            }

            // Rasengan buff tick countdown + hand particles
            if (sd.isRasenganBuffActive()) {
                sd.setRasenganBuffTicks(sd.getRasenganBuffTicks() - 1);
                if (sd.getRasenganBuffTicks() <= 0) {
                    sd.setRasenganBuffActive(false);
                    player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Rasengan charge expired."));
                    syncToClient(player);
                } else if (player.level() instanceof ServerLevel sl && player.tickCount % 4 == 0) {
                    // Spiral particles around main hand
                    double angle = (player.tickCount % 20) * (2 * Math.PI / 20);
                    double hx = player.getX() + Math.cos(angle) * 0.4;
                    double hy = player.getY() + 1.0 + Math.sin(angle) * 0.3;
                    double hz = player.getZ() + Math.sin(angle) * 0.4;
                    sl.sendParticles(ParticleTypes.END_ROD, hx, hy, hz, 1, 0, 0, 0, 0);
                }
            }

            // Flying Raijin: Ground countdown + marker particles
            if (sd.getFrgPhase() == 1) {
                sd.setFrgTicks(sd.getFrgTicks() - 1);
                if (sd.getFrgTicks() <= 0) {
                    // Remove the ground kunai entity
                    if (player.level() instanceof ServerLevel sl) {
                        NinjaSkills.removeFrgKunai(sl, sd);
                    }
                    sd.setFrgPhase(0);
                    int frgLv = sd.getLevel(SkillType.FLYING_RAIJIN_GROUND);
                    sd.startCooldown(SkillType.FLYING_RAIJIN_GROUND, frgLv);
                    player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Flying Raijin: Ground expired."));
                    syncToClient(player);
                } else if (player.level() instanceof ServerLevel sl && player.tickCount % 10 == 0) {
                    sl.sendParticles(ParticleTypes.END_ROD, sd.getFrgX(), sd.getFrgY() + 0.3, sd.getFrgZ(),
                            2, 0.1, 0.2, 0.1, 0.01);
                }
            }

            // Unholy Fervor buff tick
            if (sd.getFervorTicks() > 0) {
                sd.setFervorTicks(sd.getFervorTicks() - 1);
                if (sd.getFervorTicks() <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        List<SkeletonMinionEntity> minions = NecromancerSkills.findAllSkeletonMinions(player, sl);
                        for (SkeletonMinionEntity m : minions) {
                            m.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(0.3);
                        }
                    }
                    player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Unholy Fervor expired."));
                    syncToClient(player);
                }
            }

            // Beast Master: buff expiry (Tiger Claw / Bear Paw / Phoenix Wings)
            if (sd.getBmActiveBuff() != null && sd.getBmBuffTicks() > 0) {
                sd.setBmBuffTicks(sd.getBmBuffTicks() - 1);
                if (sd.getBmBuffTicks() <= 0) {
                    String expiredName = sd.getBmActiveBuff().getDisplayName();
                    sd.setBmActiveBuff(null);
                    sd.setBmEnhanced(false);
                    sd.setPhoenixLifestealHits(0);
                    sd.setPhoenixLifestealPct(0);
                    player.sendSystemMessage(Component.literal(
                            "\u00a7b[System]\u00a7r \u00a77" + expiredName + " expired."));
                    syncToClient(player);
                }
            }

            // Beast Master: Turtle Shell absorption expiry (5s)
            if (sd.getTurtleShellTicks() > 0) {
                sd.setTurtleShellTicks(sd.getTurtleShellTicks() - 1);
                if (sd.getTurtleShellTicks() <= 0) {
                    player.setAbsorptionAmount(0);
                    player.sendSystemMessage(Component.literal(
                            "\u00a7b[System]\u00a7r \u00a77Turtle Shell faded."));
                    syncToClient(player);
                }
            }

            // Beast Master: Tiger Claw delayed hits (0.2s apart)
            if (sd.getTigerClawHitsLeft() > 0) {
                sd.setTigerClawTimer(sd.getTigerClawTimer() - 1);
                if (sd.getTigerClawTimer() <= 0) {
                    net.minecraft.world.entity.Entity targetEnt =
                            player.level().getEntity(sd.getTigerClawTargetId());
                    if (targetEnt instanceof LivingEntity target && target.isAlive()) {
                        target.invulnerableTime = 0;
                        CombatLog.nextSource = sd.isTigerClawEnhanced() ? "Enhanced Tiger Claw" : "Tiger Claw";
                        target.hurt(player.damageSources().playerAttack(player), sd.getTigerClawDmg());
                        if (player.level() instanceof ServerLevel sl) {
                            SkillParticles.slash(sl,
                                    player.position(),
                                    target.position().add(0, target.getBbHeight() * 0.5, 0),
                                    ParticleTypes.CRIT);
                            SkillSounds.playAt(sl,
                                    target.getX(), target.getY(), target.getZ(),
                                    SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f,
                                    1.1f + sl.random.nextFloat() * 0.3f);
                        }
                        sd.setTigerClawHitsLeft(sd.getTigerClawHitsLeft() - 1);
                        if (sd.getTigerClawHitsLeft() > 0) {
                            sd.setTigerClawTimer(4); // next hit in 0.2s
                        } else {
                            // All hits dealt, clear state
                            sd.setTigerClawTargetId(-1);
                            sd.setTigerClawDmg(0);
                            sd.setTigerClawTimer(0);
                            syncToClient(player);
                        }
                    } else {
                        // Target gone, clear state
                        sd.setTigerClawHitsLeft(0);
                        sd.setTigerClawTargetId(-1);
                        sd.setTigerClawDmg(0);
                        sd.setTigerClawTimer(0);
                        syncToClient(player);
                    }
                }
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

                // Stealth drain
                if (sd.isToggleActive(SkillType.STEALTH)) {
                    int level = sd.getLevel(SkillType.STEALTH);
                    int drain = SkillType.STEALTH.getToggleMpPerSecond(level, stats.getMaxMp());
                    if (stats.getCurrentMp() >= drain) {
                        stats.setCurrentMp(stats.getCurrentMp() - drain);
                        AssassinSkills.applyStealthEffects(player, level);
                        changed = true;
                    } else {
                        AssassinSkills.breakStealth(player, sd);
                        changed = true;
                    }
                }

                // Soul Arrow drain
                if (sd.isToggleActive(SkillType.SOUL_ARROW)) {
                    int level = sd.getLevel(SkillType.SOUL_ARROW);
                    int drain = SkillType.SOUL_ARROW.getToggleMpPerSecond(level, stats.getMaxMp());
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
                    int drain = SkillType.SHADOW_PARTNER.getToggleMpPerSecond(level, stats.getMaxMp());
                    if (stats.getCurrentMp() >= drain) {
                        stats.setCurrentMp(stats.getCurrentMp() - drain);
                        changed = true;
                        // Shadow Legion auto-attack: partners attack every 2 seconds
                        int slLv = sd.getLevel(SkillType.SHADOW_LEGION);
                        if (slLv > 0 && player.tickCount % 40 == 0 && player.level() instanceof ServerLevel sl) {
                            float autoAtkDmg = (float) player.getAttributeValue(
                                    net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * 0.2f;
                            List<ShadowPartnerEntity> partners = AssassinSkills.findAllShadowPartners(player, sl);
                            for (ShadowPartnerEntity partner : partners) {
                                List<Monster> nearby = sl.getEntitiesOfClass(Monster.class,
                                        partner.getBoundingBox().inflate(4));
                                if (!nearby.isEmpty()) {
                                    Monster target = nearby.get(0);
                                    target.hurt(player.damageSources().mobAttack(partner), autoAtkDmg);
                                    partner.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                                }
                            }
                        }
                    } else {
                        sd.setToggleActive(SkillType.SHADOW_PARTNER, false);
                        if (player.level() instanceof ServerLevel sl) {
                            AssassinSkills.despawnShadowPartner(player, sl);
                        }
                        player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Shadow Partner deactivated (no MP)."));
                        changed = true;
                    }
                }

                // Magic Guard drain
                if (sd.isToggleActive(SkillType.MAGIC_GUARD)) {
                    int level = sd.getLevel(SkillType.MAGIC_GUARD);
                    int drain = SkillType.MAGIC_GUARD.getToggleMpPerSecond(level, stats.getMaxMp());
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
                    int drain = SkillType.HURRICANE.getToggleMpPerSecond(level, stats.getMaxMp());
                    if (stats.getCurrentMp() >= drain) {
                        stats.setCurrentMp(stats.getCurrentMp() - drain);
                        ArcherSkills.tickHurricane(player, stats, sd);
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
                    int drain = SkillType.RAISE_SKELETON.getToggleMpPerSecond(level, stats.getMaxMp());
                    if (stats.getCurrentMp() >= drain) {
                        stats.setCurrentMp(stats.getCurrentMp() - drain);
                        changed = true;
                    } else {
                        sd.setToggleActive(SkillType.RAISE_SKELETON, false);
                        if (player.level() instanceof ServerLevel sl) {
                            NecromancerSkills.despawnSkeletonMinion(player, sl);
                        }
                        player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Raise Skeleton deactivated (no MP)."));
                        changed = true;
                    }
                }

                // Bone Shield drain
                if (sd.isToggleActive(SkillType.BONE_SHIELD)) {
                    int level = sd.getLevel(SkillType.BONE_SHIELD);
                    int drain = SkillType.BONE_SHIELD.getToggleMpPerSecond(level, stats.getMaxMp());
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

                // Soul Link drain
                if (sd.isToggleActive(SkillType.SOUL_LINK)) {
                    int level = sd.getLevel(SkillType.SOUL_LINK);
                    int drain = SkillType.SOUL_LINK.getToggleMpPerSecond(level, stats.getMaxMp());
                    if (stats.getCurrentMp() >= drain) {
                        stats.setCurrentMp(stats.getCurrentMp() - drain);
                        if (player.tickCount % 40 == 0 && player.level() instanceof ServerLevel sl) {
                            SkillParticles.playerAura(player, 5, 0.6, ParticleTypes.SOUL);
                        }
                        changed = true;
                    } else {
                        sd.setToggleActive(SkillType.SOUL_LINK, false);
                        player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Soul Link deactivated (no MP)."));
                        changed = true;
                    }
                }

                // Shadow Clone drain
                if (sd.isToggleActive(SkillType.SHADOW_CLONE)) {
                    int level = sd.getLevel(SkillType.SHADOW_CLONE);
                    int drain = SkillType.SHADOW_CLONE.getToggleMpPerSecond(level, stats.getMaxMp());
                    if (stats.getCurrentMp() >= drain) {
                        stats.setCurrentMp(stats.getCurrentMp() - drain);
                        changed = true;
                    } else {
                        sd.setToggleActive(SkillType.SHADOW_CLONE, false);
                        if (player.level() instanceof ServerLevel sl) {
                            NinjaSkills.despawnShadowClone(player, sl);
                        }
                        player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Shadow Clone deactivated (no MP)."));
                        changed = true;
                    }
                }

                // Sage Mode drain + effects
                if (sd.isToggleActive(SkillType.SAGE_MODE)) {
                    int level = sd.getLevel(SkillType.SAGE_MODE);
                    int drain = SkillType.SAGE_MODE.getToggleMpPerSecond(level, stats.getMaxMp());
                    if (stats.getCurrentMp() >= drain) {
                        stats.setCurrentMp(stats.getCurrentMp() - drain);
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 25, 0, false, false));
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 25, 0, false, false));
                        if (player.tickCount % 40 == 0) {
                            SkillParticles.playerAura(player, 10, 1.0, ParticleTypes.END_ROD);
                        }
                        changed = true;
                    } else {
                        sd.setToggleActive(SkillType.SAGE_MODE, false);
                        player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Sage Mode deactivated (no MP)."));
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

                // Passive: Chakra Control — bonus MP regen
                float ccRegenBonus = NinjaSkills.getChakraControlRegenBonus(stats);
                if (ccRegenBonus > 0) {
                    int maxMp = stats.getMaxMp();
                    int ccRegen = (int) (maxMp * ccRegenBonus / 100.0f);
                    if (ccRegen > 0 && stats.getCurrentMp() < maxMp) {
                        stats.setCurrentMp(Math.min(stats.getCurrentMp() + ccRegen, maxMp));
                        changed = true;
                    }
                }

                // Passive: Eight Inner Gates — buffs below 30% HP
                int gatesLv = sd.getLevel(SkillType.EIGHT_INNER_GATES);
                if (gatesLv > 0) {
                    float healthPct = player.getHealth() / player.getMaxHealth();
                    if (healthPct <= 0.3f) {
                        int speedAmp = Math.min(gatesLv / 5, 2);
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 25, speedAmp, false, false));
                        if (player.tickCount % 40 == 0) {
                            SkillParticles.playerAura(player, 8, 0.8, ParticleTypes.FLAME);
                        }
                        // Max level: brief invuln at 10% HP (60s CD)
                        if (gatesLv >= 20 && healthPct <= 0.1f && sd.getGatesInvulnCooldown() <= 0) {
                            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 4, false, true));
                            sd.setGatesInvulnCooldown(1200);
                            player.sendSystemMessage(Component.literal(
                                    "\u00a7b[System]\u00a7r \u00a75Eight Inner Gates: Invulnerability!"));
                            if (player.level() instanceof ServerLevel sl) {
                                SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 30, 1.0, ParticleTypes.FLAME);
                                SkillSounds.playAt(player, SoundEvents.GENERIC_EXPLODE, 0.5f, 0.8f);
                            }
                        }
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
            ShadowPartnerEntity partner = AssassinSkills.findShadowPartner(player, sl);
            if (partner == null) return;

            int spLv = sd.getLevel(SkillType.SHADOW_PARTNER);
            float multiplier = AssassinSkills.getShadowPartnerDamageMultiplier(spLv);

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
                float bonus = AssassinSkills.getShadowStrikeDamage(stats);
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
                AssassinSkills.breakStealth(player, sd);
                syncToClient(player);
            }

            // Venom: apply poison on hit (Wither for undead since they're immune to Poison)
            if (sd.isVenomActive() && event.getEntity() instanceof Monster mob) {
                int venomLv = sd.getLevel(SkillType.VENOM);
                int poisonDur = AssassinSkills.getVenomPoisonDuration(venomLv);
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

            // Slash Blast: consume on next melee, boost damage + hit 3 nearby
            if (sd.isSlashBlastActive() && !event.getSource().isIndirect()
                    && !SkillExecutor.executingSkill) {
                float sbBonus = sd.getSlashBlastPct();
                amount *= (1.0f + sbBonus);
                sd.setSlashBlastActive(false);
                sd.setSlashBlastPct(0);
                LivingEntity mainTarget = event.getEntity();
                AABB splashArea = mainTarget.getBoundingBox().inflate(3.0);
                List<LivingEntity> nearby = player.level().getEntitiesOfClass(
                        LivingEntity.class, splashArea,
                        e -> e != mainTarget && e != player && e instanceof Monster);
                // Suppress splash individual logs, accumulate true damage
                CombatLog.suppressDamageLog = true;
                CombatLog.pendingAoeSplashDmg = 0;
                CombatLog.pendingAoeSplashCount = 0;
                int splashHits = 0;
                for (LivingEntity mob : nearby) {
                    if (splashHits >= 3) break;
                    mob.invulnerableTime = 0;
                    mob.hurt(player.damageSources().playerAttack(player), amount);
                    splashHits++;
                }
                CombatLog.suppressDamageLog = false;
                // Tag main hit — onLivingDamage will log AoE summary with true damage
                CombatLog.pendingAoe = "Slash Blast";
                if (player.level() instanceof ServerLevel sl) {
                    SkillParticles.burst(sl, mainTarget.getX(), mainTarget.getY() + 1, mainTarget.getZ(),
                            12, 0.5, ParticleTypes.SWEEP_ATTACK);
                    SkillSounds.playAt(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.9f);
                }
                syncToClient(player);
            }

            // War Cry: flat ATK bonus during buff
            if (sd.getWarCryTicks() > 0 && !event.getSource().isIndirect()) {
                amount += sd.getWarCryAtkBonus();
            }

            // Spirit Blade: flat ATK bonus (applied after % buffs)
            if (sd.getSpiritBladeTicks() > 0 && !event.getSource().isIndirect()) {
                amount += sd.getSpiritBladeAtk();
            }

            // Passive: Kunai Mastery — +AGI melee damage
            float kmBonus = NinjaSkills.getKunaiMasteryMeleeBonus(stats);
            if (kmBonus > 0 && !event.getSource().isIndirect()) {
                amount += kmBonus;
            }

            // Sage Mode: damage multiplier
            amount *= NinjaSkills.getSageModeDamageMultiplier(sd);

            // Eight Inner Gates: damage multiplier below 30% HP
            float gatesHealthPct = player.getHealth() / player.getMaxHealth();
            amount *= NinjaSkills.getGatesDamageMultiplier(stats, gatesHealthPct);

            // Rasengan buff: explode in 2-block radius around target, bonus AGI+LUK damage + 30% splash
            // Only triggers on normal melee attacks, not during skill execution
            if (sd.isRasenganBuffActive() && !event.getSource().isIndirect()
                    && !com.monody.projectleveling.skill.SkillExecutor.executingSkill) {
                sd.setRasenganBuffActive(false);
                sd.setRasenganBuffTicks(0);
                int rsgLv = sd.getLevel(SkillType.RASENGAN);
                float rasenganBonus = NinjaSkills.getRasenganBonusDamage(stats, rsgLv);
                amount += rasenganBonus;
                float splashDmg = amount * 0.3f;
                net.minecraft.world.entity.LivingEntity mainTarget = (net.minecraft.world.entity.LivingEntity) event.getEntity();
                AABB splashArea = mainTarget.getBoundingBox().inflate(2.0);
                List<net.minecraft.world.entity.LivingEntity> nearby = player.level().getEntitiesOfClass(
                        net.minecraft.world.entity.LivingEntity.class, splashArea,
                        e -> e != mainTarget && e != player && e instanceof Monster);
                // Suppress splash individual logs, accumulate true damage
                CombatLog.suppressDamageLog = true;
                CombatLog.pendingAoeSplashDmg = 0;
                CombatLog.pendingAoeSplashCount = 0;
                for (net.minecraft.world.entity.LivingEntity mob : nearby) {
                    mob.hurt(player.damageSources().playerAttack(player), splashDmg);
                }
                CombatLog.suppressDamageLog = false;
                // Tag main hit — onLivingDamage will log combined AoE summary
                CombatLog.pendingAoe = "Rasengan";
                if (player.level() instanceof ServerLevel sl) {
                    SkillParticles.explosion(sl, mainTarget.getX(), mainTarget.getY() + 1, mainTarget.getZ(),
                            2.0f, ParticleTypes.END_ROD, ParticleTypes.ENCHANTED_HIT);
                    SkillSounds.playAt(sl, mainTarget.getX(), mainTarget.getY(), mainTarget.getZ(),
                            SoundEvents.GENERIC_EXPLODE, 0.6f, 1.3f);
                }
                player.sendSystemMessage(Component.literal(
                        "\u00a7b[System]\u00a7r \u00a76Rasengan! " + (nearby.size() + 1) + " enemies hit."));
                syncToClient(player);
            }

            // Beast Master: Tiger Claw — schedule delayed extra hits (0.2s apart)
            if (sd.getBmActiveBuff() == SkillType.TIGER_CLAW && !event.getSource().isIndirect()
                    && !SkillExecutor.executingSkill) {
                int tcLv = sd.getLevel(SkillType.TIGER_CLAW);
                boolean enhanced = sd.isBmEnhanced();
                int extraHits = BeastMasterSkills.getExtraHits(tcLv);
                int tcmLv = sd.getLevel(SkillType.TIGER_CLAW_MASTERY);
                int tcm2Lv = sd.getLevel(SkillType.TIGER_CLAW_MASTERY_2);
                extraHits += BeastMasterSkills.getTigerClawMasteryHits(tcmLv);
                extraHits += BeastMasterSkills.getTigerClawMasteryHits(tcm2Lv);
                if (enhanced) extraHits *= 2; // Enhanced doubles hit count
                float hitPct = BeastMasterSkills.getHitDamagePct(tcLv);
                float tcmDmgBonus = BeastMasterSkills.getTigerClawMasteryDmg(tcmLv)
                                  + BeastMasterSkills.getTigerClawMasteryDmg(tcm2Lv);
                // Master of Nature: +1% enhanced damage per level
                if (enhanced) {
                    int monLv = sd.getLevel(SkillType.MASTER_OF_NATURE);
                    if (monLv > 0) tcmDmgBonus += monLv * 0.01f;
                }
                // Boost the triggering melee hit with mastery damage
                if (tcmDmgBonus > 0) {
                    amount *= (1.0f + tcmDmgBonus);
                    event.setAmount(amount);
                }
                // Extra hits based on the boosted amount (no double-apply)
                float extraDmg = amount * hitPct;
                // Schedule delayed hits
                sd.setTigerClawHitsLeft(extraHits);
                sd.setTigerClawTargetId(event.getEntity().getId());
                sd.setTigerClawDmg(extraDmg);
                sd.setTigerClawTimer(4); // first hit after 0.2s
                sd.setTigerClawEnhanced(enhanced);
                // Clear buff
                sd.setBmActiveBuff(null);
                sd.setBmBuffTicks(0);
                sd.setBmEnhanced(false);
                String name = enhanced ? "Enhanced Tiger Claw" : "Tiger Claw";
                // Tag the initial melee hit so onLivingDamage shows the combination name
                CombatLog.nextSource = name;
                player.sendSystemMessage(Component.literal(
                        "\u00a7b[System]\u00a7r \u00a76" + name + "! " + extraHits + " extra hits!"));
                syncToClient(player);
            }

            // Beast Master: Bear Paw — stun + debuffs on next melee
            if (sd.getBmActiveBuff() == SkillType.BEAR_PAW && !event.getSource().isIndirect()
                    && !SkillExecutor.executingSkill) {
                LivingEntity target = event.getEntity();
                int bpLv = sd.getLevel(SkillType.BEAR_PAW);
                boolean enhanced = sd.isBmEnhanced();
                float stunSec = BeastMasterSkills.getStunDuration(bpLv);
                int bpmLv = sd.getLevel(SkillType.BEAR_PAW_MASTERY);
                int bpm2Lv = sd.getLevel(SkillType.BEAR_PAW_MASTERY_2);
                int bpmTotal = bpmLv + bpm2Lv;
                if (bpmTotal > 0) stunSec += bpmTotal * 0.1f;
                stunSec = Math.min(stunSec, 3.0f); // Hard cap before PoN
                if (enhanced) stunSec = Math.min(stunSec * 2, 4.0f); // Cap at 4s with PoN
                int stunTicks = (int) (stunSec * 20);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stunTicks, 99, false, true));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, stunTicks, 0, false, true));
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, stunTicks, 0, false, true));
                // Final damage boost from Bear Paw Mastery (T2+T3) + Master of Nature
                float bpDmgBonus = BeastMasterSkills.getBearPawMasteryDmg(bpmLv)
                                 + BeastMasterSkills.getBearPawMasteryDmg(bpm2Lv);
                if (enhanced) {
                    int monLv = sd.getLevel(SkillType.MASTER_OF_NATURE);
                    if (monLv > 0) bpDmgBonus += monLv * 0.01f;
                }
                if (bpDmgBonus > 0) {
                    amount *= (1.0f + bpDmgBonus);
                    event.setAmount(amount);
                }
                if (player.level() instanceof ServerLevel sl) {
                    SkillParticles.burst(sl, target.getX(), target.getY() + 1, target.getZ(),
                            10, 0.4, ParticleTypes.ENCHANTED_HIT);
                    SkillSounds.playAt(sl, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.RAVAGER_STUNNED, 0.7f, 1.0f);
                }
                String name = enhanced ? "Enhanced Bear Paw" : "Bear Paw";
                // Tag the initial melee hit so onLivingDamage shows the combination name
                CombatLog.nextSource = name;
                sd.setBmActiveBuff(null);
                sd.setBmBuffTicks(0);
                sd.setBmEnhanced(false);
                player.sendSystemMessage(Component.literal(
                        "\u00a7b[System]\u00a7r \u00a76" + name + "! Stunned for " + String.format("%.1f", stunSec) + "s!"));
                syncToClient(player);
            }

            // Beast Master: Phoenix Wings lifesteal on melee hit
            if (sd.getPhoenixLifestealHits() > 0 && !event.getSource().isIndirect()
                    && !SkillExecutor.executingSkill) {
                float lifestealPct = sd.getPhoenixLifestealPct();
                float healAmount = amount * lifestealPct;
                if (healAmount > 0) {
                    player.heal(healAmount);
                    CombatLog.heal(player, sd.isPhoenixLifestealEnhanced() ? "Enhanced Phoenix Wings" : "Phoenix Wings", healAmount);
                    if (player.level() instanceof ServerLevel sl) {
                        SkillSounds.playAt(sl, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.CHICKEN_HURT, 0.7f, 1.2f + sl.random.nextFloat() * 0.3f);
                    }
                }
                sd.setPhoenixLifestealHits(sd.getPhoenixLifestealHits() - 1);
                if (sd.getPhoenixLifestealHits() <= 0) {
                    sd.setPhoenixLifestealPct(0);
                    if (sd.getBmActiveBuff() == SkillType.PHOENIX_WINGS) {
                        sd.setBmActiveBuff(null);
                        sd.setBmBuffTicks(0);
                        sd.setBmEnhanced(false);
                    }
                    player.sendSystemMessage(Component.literal(
                            "\u00a7b[System]\u00a7r \u00a77Phoenix Wings lifesteal consumed."));
                    syncToClient(player);
                }
            }

            // Passive: Final Attack — chance to repeat hit at 50% damage
            int faLv = sd.getLevel(SkillType.FINAL_ATTACK);
            if (faLv > 0 && !finalAttackInProgress) {
                float faChance = WarriorSkills.getFinalAttackChance(faLv);
                int bsLv2 = sd.getLevel(SkillType.BERSERKER_SPIRIT);
                if (bsLv2 > 0) faChance += WarriorSkills.getBerserkerFinalAttackBonus(bsLv2);
                if (player.getRandom().nextFloat() * 100 < faChance) {
                    float repeatDmg = amount * 0.5f;
                    LivingEntity faTarget = event.getEntity();
                    finalAttackInProgress = true;
                    faTarget.invulnerableTime = 0;
                    CombatLog.nextSource = "Final Attack";
                    faTarget.hurt(player.damageSources().playerAttack(player), repeatDmg);
                    finalAttackInProgress = false;
                    if (player.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                                faTarget.getX(), faTarget.getY() + faTarget.getBbHeight() * 0.5, faTarget.getZ(),
                                1, 0, 0, 0, 0);
                        SkillSounds.playAt(sl, faTarget.getX(), faTarget.getY(), faTarget.getZ(),
                                SoundEvents.PLAYER_ATTACK_SWEEP, 0.6f, 1.3f);
                    }
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
                float debuffBonus = Math.min(debuffCount * 0.02f * edLv, 0.50f);
                amount *= 1.0f + debuffBonus;
            }

            // Infinity: +5% damage per stack (stacks every 4s)
            if (sd.getInfinityTicks() > 0 && sd.getInfinityStacks() > 0) {
                amount *= 1.0f + sd.getInfinityStacks() * 0.05f;
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
                        amount *= 2.0f; // Execute: 2x damage
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
                        amount *= 2.0f; // Execute: 2x damage
                    }
                }
            }

            // Crit calculation: Sight-based + Critical Edge + Sharp Eyes + Arcane Overdrive + Evasion crit ready + Berserker Spirit
            double critRate = 0.15;
            double critDmgBonus = 1.5;

            int sig = stats.getSight();
            if (sig > 1) {
                critRate += (sig - 1) * 0.001;
                critDmgBonus += (sig - 1) * 0.002;
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
                // Lifesteal
                float bsHeal = amount * bsLv * 0.003f;
                player.heal(bsHeal);
                CombatLog.heal(player, "Berserker", bsHeal);
            }

            // Kunai Mastery crit bonus
            float kmCrit = NinjaSkills.getKunaiMasteryCritBonus(stats);
            if (kmCrit > 0) {
                critRate += kmCrit / 100.0;
            }

            // Equipment crit rate (Dagger, etc.)
            var critInst = player.getAttribute(com.monody.projectleveling.item.ModAttributes.CRIT_RATE.get());
            if (critInst != null && critInst.getValue() > 0) {
                critRate += critInst.getValue() / 100.0;
            }

            // Equipment crit damage
            var cdInst = player.getAttribute(com.monody.projectleveling.item.ModAttributes.CRIT_DAMAGE.get());
            if (cdInst != null && cdInst.getValue() > 0) {
                critDmgBonus += cdInst.getValue() / 100.0;
            }

            // Evasion crit ready (guaranteed crit on next hit after dodge)
            if (sd.isEvasionCritReady()) {
                critRate = 1.0;
                sd.setEvasionCritReady(false);
            }

            if (critRate > 0 && player.getRandom().nextDouble() < critRate) {
                amount = (float) (amount * critDmgBonus);
                // Critical Edge lifesteal on crit: 0.5% per level
                if (ceLv > 0) {
                    float ceHeal = amount * ceLv * 0.005f;
                    player.heal(ceHeal);
                    CombatLog.heal(player, "Critical Edge", ceHeal);
                }
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
                // Kunai Mastery projectile damage
                projMult *= NinjaSkills.getKunaiMasteryProjectileMultiplier(stats);
                amount *= projMult;
            }

            // Shadow Partner mirror attack — teleport to target, hit, teleport back
            if (sd.isToggleActive(SkillType.SHADOW_PARTNER) && event.getEntity() instanceof Monster mob) {
                int spLv = sd.getLevel(SkillType.SHADOW_PARTNER);
                float mirrorDmg = amount * AssassinSkills.getShadowPartnerDamageMultiplier(spLv);
                if (player.level() instanceof ServerLevel sl) {
                    ShadowPartnerEntity partner = AssassinSkills.findShadowPartner(player, sl);
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

            // Equipment ATK% — multiplies physical (non-indirect) damage
            if (!event.getSource().isIndirect()) {
                var atkPctInst = player.getAttribute(com.monody.projectleveling.item.ModAttributes.ATTACK_PERCENT.get());
                if (atkPctInst != null && atkPctInst.getValue() > 0) {
                    amount *= 1.0f + (float) (atkPctInst.getValue() / 100.0);
                }
            }

            // MATK% is applied in PlayerStats.getMagicAttack(Player) — all magic skills use the boosted value naturally

            // Equipment Final Damage% — multiplies ALL damage as the very last step
            var fdInst = player.getAttribute(com.monody.projectleveling.item.ModAttributes.FINAL_DAMAGE.get());
            if (fdInst != null && fdInst.getValue() > 0) {
                amount *= 1.0f + (float) (fdInst.getValue() / 100.0);
            }

            event.setAmount(amount);
        });
    }

    // === Skeleton minion attack buffs: Unholy Fervor + Soul Link ===

    @SubscribeEvent
    public static void onMinionAttack(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof SkeletonMinionEntity minion)) return;
        ServerPlayer owner = minion.getOwnerPlayer();
        if (owner == null) return;

        owner.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            SkillData sd = stats.getSkillData();
            float amount = event.getAmount();

            // Unholy Fervor: damage boost while buff active
            if (sd.getFervorTicks() > 0) {
                int ufLv = sd.getLevel(SkillType.UNHOLY_FERVOR);
                if (ufLv > 0) {
                    amount *= 1.0f + NecromancerSkills.getUnholyFervorDamageBonus(ufLv);
                }
            }

            // Soul Link: minions deal bonus damage while linked
            if (sd.isToggleActive(SkillType.SOUL_LINK)) {
                int slLv = sd.getLevel(SkillType.SOUL_LINK);
                if (slLv > 0) {
                    amount *= 1.0f + NecromancerSkills.getSoulLinkMinionDamageBonus(slLv);
                }
            }

            event.setAmount(amount);
        });
    }

    // === Skeletal Mastery: minion damage reduction ===

    @SubscribeEvent
    public static void onMinionHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof SkeletonMinionEntity minion)) return;
        ServerPlayer owner = minion.getOwnerPlayer();
        if (owner == null) return;

        owner.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            SkillData sd = stats.getSkillData();
            int smLv = sd.getLevel(SkillType.SKELETAL_MASTERY);
            if (smLv > 0) {
                float reduction = NecromancerSkills.getSkeletalMasteryDamageReduction(smLv);
                event.setAmount(event.getAmount() * (1 - reduction));
            }
        });
    }

    // === Final damage log (fires after armor, toughness, enchant reduction) ===

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getAmount() <= 0) return;

        // AoE suppression: accumulate true damage for later summary
        if (CombatLog.suppressDamageLog) {
            if (CombatLog.nextSource != null) {
                // Named source during AoE (e.g. Final Attack proc on splash target) — log it
                String src = CombatLog.nextSource;
                CombatLog.nextSource = null;
                if (event.getSource().getEntity() instanceof ServerPlayer p) {
                    CombatLog.damage(p, src, event.getAmount(), event.getEntity());
                }
            } else {
                CombatLog.pendingAoeSplashDmg += event.getAmount();
                CombatLog.pendingAoeSplashCount++;
            }
            return;
        }

        // Skeleton minion damage → log to owner + Skeletal Mastery lifesteal
        if (event.getSource().getEntity() instanceof SkeletonMinionEntity minion) {
            ServerPlayer owner = minion.getOwnerPlayer();
            if (owner != null) {
                CombatLog.damage(owner, "Skeleton", event.getAmount(), event.getEntity());
                // Skeletal Mastery: lifesteal for the minion
                owner.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                    int smLv = stats.getSkillData().getLevel(SkillType.SKELETAL_MASTERY);
                    if (smLv > 0) {
                        float healAmount = event.getAmount() * NecromancerSkills.getSkeletalMasteryLifesteal(smLv);
                        if (healAmount > 0 && minion.isAlive()) {
                            minion.heal(healAmount);
                        }
                    }
                });
            }
            return;
        }

        // Shadow Clone melee damage → log to owner
        if (event.getSource().getEntity() instanceof com.monody.projectleveling.entity.ninja.ShadowCloneEntity clone) {
            ServerPlayer owner = clone.getOwnerPlayer();
            if (owner != null) {
                CombatLog.damage(owner, "Clone", event.getAmount(), event.getEntity());
            }
            return;
        }

        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        // Shadow Partner melee has its own logging
        if (event.getSource().getDirectEntity() instanceof ShadowPartnerEntity) return;

        // Named single-target source (e.g. Final Attack)
        String source = CombatLog.nextSource;
        if (source != null) {
            CombatLog.nextSource = null;
            CombatLog.damage(player, source, event.getAmount(), event.getEntity());
            return;
        }

        // Pending AoE summary (e.g. Slash Blast main hit + accumulated splash)
        if (CombatLog.pendingAoe != null) {
            String aoeName = CombatLog.pendingAoe;
            CombatLog.pendingAoe = null;
            float totalDmg = CombatLog.pendingAoeSplashDmg + event.getAmount();
            int totalHits = CombatLog.pendingAoeSplashCount + 1;
            if (totalHits > 1) {
                CombatLog.aoe(player, aoeName, totalDmg / totalHits, totalHits);
            } else {
                CombatLog.damage(player, aoeName, event.getAmount(), event.getEntity());
            }
            return;
        }

        String label = event.getSource().isIndirect() ? "Ranged Attack" : "Melee Attack";
        CombatLog.damage(player, label, event.getAmount(), event.getEntity());
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewTarget() instanceof ServerPlayer player)) return;

        player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            SkillData sd = stats.getSkillData();
            if (!sd.isToggleActive(SkillType.STEALTH)) return;

            int level = sd.getLevel(SkillType.STEALTH);
            double detectionRange = AssassinSkills.getStealthDetectionRange(level);
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
                AssassinSkills.breakStealth(player, sd);
                syncToClient(player);
            }

            // Substitution Jutsu: negate first hit, teleport behind attacker
            if (sd.getSubstitutionTicks() > 0) {
                sd.setSubstitutionTicks(0);
                event.setCanceled(true);
                if (event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker) {
                    Vec3 behind = attacker.position().subtract(attacker.getLookAngle().scale(2.0));
                    player.teleportTo(behind.x, behind.y, behind.z);
                    // Look at the attacker
                    double dx = attacker.getX() - player.getX();
                    double dy = (attacker.getEyeY()) - player.getEyeY();
                    double dz = attacker.getZ() - player.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    float yRot = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
                    float xRot = (float) -(Math.atan2(dy, dist) * (180.0 / Math.PI));
                    player.setYRot(yRot);
                    player.setXRot(xRot);
                    player.setYHeadRot(yRot);
                }
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20, 1, false, false));
                if (player.level() instanceof ServerLevel sl) {
                    SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 15, 0.5, ParticleTypes.CLOUD);
                    SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 10, 0.3, ParticleTypes.SMOKE);
                    SkillSounds.playAt(player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.5f, 1.5f);
                }
                player.sendSystemMessage(Component.literal(
                        "\u00a7b[System]\u00a7r \u00a7eSubstitution! Dodged and repositioned."));
                syncToClient(player);
                return;
            }

            // Passive: Evasion — 2% dodge per level, dodge guarantees next crit
            int evLv = sd.getLevel(SkillType.EVASION);
            if (evLv > 0 && player.getRandom().nextFloat() < evLv * 0.02f) {
                event.setCanceled(true);
                sd.setEvasionCritReady(true);
                // Shadow Legion: partners counter-attack the attacker on dodge
                int slLv = sd.getLevel(SkillType.SHADOW_LEGION);
                if (slLv > 0 && sd.isToggleActive(SkillType.SHADOW_PARTNER)
                        && event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker
                        && player.level() instanceof ServerLevel sl) {
                    float counterDmg = (float) player.getAttributeValue(
                            net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * 0.5f;
                    List<ShadowPartnerEntity> partners = AssassinSkills.findAllShadowPartners(player, sl);
                    for (ShadowPartnerEntity partner : partners) {
                        attacker.hurt(player.damageSources().mobAttack(partner), counterDmg);
                        partner.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    }
                }
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
                float ratio = MageSkills.getMagicGuardRedirectRatio(mgLv);
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
                float reduction = NecromancerSkills.getBoneShieldReduction(bsLv) / 100.0f;
                event.setAmount(event.getAmount() * (1 - reduction));
            }

            // Spirit Blade: self damage reduction
            if (sd.isSpiritBladeDefActive() && sd.getSpiritBladeTicks() > 0) {
                float sbDefPct = WarriorSkills.getSpiritBladeDefPct(sd.getLevel(SkillType.SPIRIT_BLADE));
                event.setAmount(event.getAmount() * (1 - sbDefPct));
            }

            // Warrior Mastery: passive damage reduction
            int wmLv = sd.getLevel(SkillType.WARRIOR_MASTERY);
            if (wmLv > 0) {
                float wmReduction = WarriorSkills.getWarriorMasteryDmgReduction(wmLv);
                event.setAmount(event.getAmount() * (1 - wmReduction));
            }

            // Soul Link: redirect portion of damage to nearest minion
            if (sd.isToggleActive(SkillType.SOUL_LINK)) {
                int slLv = sd.getLevel(SkillType.SOUL_LINK);
                if (slLv > 0 && player.level() instanceof ServerLevel sl) {
                    SkeletonMinionEntity nearest = NecromancerSkills.findNearestMinion(player, sl);
                    if (nearest != null && nearest.isAlive()) {
                        float redirectRatio = NecromancerSkills.getSoulLinkRedirectRatio(slLv);
                        float redirected = event.getAmount() * redirectRatio;
                        nearest.hurt(player.damageSources().generic(), redirected);
                        event.setAmount(event.getAmount() - redirected);
                        if (player.tickCount % 20 == 0 && sl != null) {
                            SkillParticles.burst(sl, nearest.getX(), nearest.getY() + 1, nearest.getZ(),
                                    5, 0.3, ParticleTypes.SOUL);
                        }
                    }
                }
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

            // Unbreakable: revive on fatal damage (Warrior passive)
            int ubLv = sd.getLevel(SkillType.UNBREAKABLE);
            if (ubLv > 0 && sd.getUnbreakableCooldown() <= 0) {
                event.setAmount(0);
                WarriorSkills.triggerUnbreakable(player, stats, sd);
                syncToClient(player);
                return;
            }

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

        // Warrior Mastery: +2% max HP per level
        int wmLv = sd.getLevel(SkillType.WARRIOR_MASTERY);
        double wmHpBonus = wmLv > 0 ? wmLv * 0.02 * (20 + (stats.getVitality() - 1) * 0.5) : 0;
        applyModifier(player, Attributes.MAX_HEALTH, WARRIOR_MASTERY_HP_UUID, "Warrior Mastery HP",
                wmHpBonus);

        // Warrior Mastery: +2% knockback resist per level
        double wmKbBonus = wmLv > 0 ? wmLv * 0.02 : 0;
        applyModifier(player, Attributes.KNOCKBACK_RESISTANCE, WARRIOR_MASTERY_KB_UUID, "Warrior Mastery KB",
                wmKbBonus);

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

    /** Called from skill execution to trigger Final Attack on skill damage. */
    public static void tryFinalAttack(ServerPlayer player, LivingEntity target, float damage) {
        if (finalAttackInProgress) return;
        player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            SkillData sd = stats.getSkillData();
            int faLv = sd.getLevel(SkillType.FINAL_ATTACK);
            if (faLv <= 0) return;
            float faChance = WarriorSkills.getFinalAttackChance(faLv);
            int bsLv = sd.getLevel(SkillType.BERSERKER_SPIRIT);
            if (bsLv > 0) faChance += WarriorSkills.getBerserkerFinalAttackBonus(bsLv);
            if (player.getRandom().nextFloat() * 100 < faChance) {
                float repeatDmg = damage * 0.5f;
                finalAttackInProgress = true;
                target.invulnerableTime = 0;
                CombatLog.nextSource = "Final Attack";
                target.hurt(player.damageSources().playerAttack(player), repeatDmg);
                finalAttackInProgress = false;
                if (player.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                            target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                            1, 0, 0, 0, 0);
                    SkillSounds.playAt(sl, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.PLAYER_ATTACK_SWEEP, 0.6f, 1.3f);
                }
            }
        });
    }

    public static void syncToClient(ServerPlayer player) {
        player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            ModNetwork.sendToPlayer(new S2CSyncStatsPacket(stats), player);
        });
    }
}
