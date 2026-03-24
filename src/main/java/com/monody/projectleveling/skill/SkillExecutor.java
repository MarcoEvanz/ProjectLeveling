package com.monody.projectleveling.skill;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.entity.assassin.ShadowPartnerEntity;
import com.monody.projectleveling.event.StatEventHandler;
import com.monody.projectleveling.skill.classes.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.monody.projectleveling.item.TaggedWeapon;
import com.monody.projectleveling.item.WeaponTag;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

public class SkillExecutor {

    /** True while a skill is being executed. Prevents Rasengan buff from triggering on skill damage. */
    public static boolean executingSkill = false;

    /** Get weapon's flat attack damage bonus from the entity's main hand item. */
    public static float getWeaponDamage(LivingEntity entity) {
        ItemStack weapon = entity.getMainHandItem();
        if (weapon.isEmpty()) return 0;
        for (AttributeModifier mod : weapon.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) {
            if (mod.getOperation() == AttributeModifier.Operation.ADDITION) {
                return (float) mod.getAmount();
            }
        }
        return 0;
    }

    public static void activateSlot(ServerPlayer player, PlayerStats stats, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= SkillData.MAX_SLOTS) return;
        SkillData sd = stats.getSkillData();
        SkillType skill = sd.getEquipped(slotIndex);
        if (skill == null || skill.isPassive()) return;

        // Toggle skills: press again to deactivate
        if (skill.isToggle() && sd.isToggleActive(skill)) {
            deactivateToggle(player, sd, skill);
            StatEventHandler.syncToClient(player);
            return;
        }

        // SSRZ phases 1-9: skip cooldown check (combo in progress)
        boolean ssrzCombo = skill == SkillType.FLYING_RAIJIN_SSRZ && sd.getSsrzPhase() >= 1;
        if (sd.isOnCooldown(skill) && !ssrzCombo) {
            int secs = (sd.getCooldownRemaining(skill) + 19) / 20;
            player.sendSystemMessage(Component.literal(
                    "\u00a7c" + skill.getDisplayName() + " is on cooldown (" + secs + "s)"));
            return;
        }

        int level = sd.getLevel(skill);
        if (level <= 0) return;

        int mpCost = skill.getMpCost(level);
        // Infinity: all skills cost 0 MP
        if (sd.getInfinityTicks() > 0 && skill != SkillType.ARCANE_INFINITY) {
            mpCost = 0;
        }
        // Element Amplification: +20% MP cost
        int eaLv = sd.getLevel(SkillType.ELEMENT_AMPLIFICATION);
        if (eaLv > 0 && mpCost > 0) {
            mpCost = (int) (mpCost * 1.2);
        }
        // Chakra Control: reduce MP cost
        float ccMult = NinjaSkills.getChakraControlCostMultiplier(stats);
        if (ccMult < 1.0f && mpCost > 0) {
            mpCost = Math.max(1, (int) (mpCost * ccMult));
        }
        // Six Eyes: reduce MP cost
        float seMult = LimitlessSkills.getSixEyesCostMultiplier(stats);
        if (seMult < 1.0f && mpCost > 0) {
            mpCost = Math.max(1, (int) (mpCost * seMult));
        }
        // Flying Raijin phase 2 (teleport): no MP cost
        if (skill == SkillType.FLYING_RAIJIN && sd.getFlyingRaijinPhase() == 1) {
            mpCost = 0;
        }
        // Flying Raijin: Ground phase 2 (return teleport): no MP cost
        if (skill == SkillType.FLYING_RAIJIN_GROUND && sd.getFrgPhase() == 1) {
            mpCost = 0;
        }
        // SSRZ phases 1-9 (combo strikes): no MP cost
        if (skill == SkillType.FLYING_RAIJIN_SSRZ && sd.getSsrzPhase() >= 1) {
            mpCost = 0;
        }
        if (!skill.isToggle() && stats.getCurrentMp() < mpCost) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough MP!"));
            return;
        }

        // Weapon restriction per class
        String weaponError = checkWeaponRestriction(player, skill);
        if (weaponError != null) {
            player.sendSystemMessage(Component.literal("\u00a7c" + weaponError));
            return;
        }

        // Dispatch execution to per-class file
        executingSkill = true;
        try {
            PlayerClass cls = skill.getRequiredClass();
            if (cls == null) {
                NoviceSkills.execute(player, stats, sd, skill, level);
            } else {
                switch (cls) {
                    case WARRIOR -> WarriorSkills.execute(player, stats, sd, skill, level);
                    case ASSASSIN -> AssassinSkills.execute(player, stats, sd, skill, level);
                    case ARCHER -> ArcherSkills.execute(player, stats, sd, skill, level);
                    case HEALER -> HealerSkills.execute(player, stats, sd, skill, level);
                    case MAGE -> MageSkills.execute(player, stats, sd, skill, level);
                    case NINJA -> NinjaSkills.execute(player, stats, sd, skill, level);
                    case NECROMANCER -> NecromancerSkills.execute(player, stats, sd, skill, level);
                    case BEAST_MASTER -> BeastMasterSkills.execute(player, stats, sd, skill, level);
                    case LIMITLESS -> LimitlessSkills.execute(player, stats, sd, skill, level);
                    default -> { /* passives handled elsewhere */ }
                }
            }

            // Shadow Partner mirrors the skill
            if (sd.isToggleActive(SkillType.SHADOW_PARTNER) && skill != SkillType.SHADOW_PARTNER) {
                mirrorSkillFromPartner(player, stats, skill);
            }

            // Shadow Clones sync the skill
            if (sd.isToggleActive(SkillType.SHADOW_CLONE) && skill != SkillType.SHADOW_CLONE) {
                NinjaSkills.syncSkillToClones(player, stats, skill, level);
            }
        } finally {
            executingSkill = false;
        }

        StatEventHandler.syncToClient(player);
    }

    // ================================================================
    // Weapon restriction check
    // ================================================================

    private static String checkWeaponRestriction(ServerPlayer player, SkillType skill) {
        PlayerClass cls = skill.getRequiredClass();
        if (cls == null) return null; // Novice skills — no restriction

        ItemStack weapon = player.getMainHandItem();
        WeaponTag tag = TaggedWeapon.getTag(weapon);
        boolean bareHand = weapon.isEmpty();

        return switch (cls) {
            case WARRIOR -> {
                // Sword/Axe: mod's Greatsword/Warhammer OR vanilla SwordItem/AxeItem
                if (tag == WeaponTag.SWORD || tag == WeaponTag.AXE
                        || weapon.getItem() instanceof SwordItem
                        || weapon.getItem() instanceof AxeItem)
                    yield null;
                yield "You must hold a sword or axe to use Warrior skills!";
            }
            case ASSASSIN -> {
                // Dagger or bare hand
                if (tag == WeaponTag.DAGGER || bareHand)
                    yield null;
                yield "You must hold a dagger or bare hand to use Assassin skills!";
            }
            case ARCHER -> {
                // Any bow (mod or vanilla)
                if (weapon.getItem() instanceof BowItem)
                    yield null;
                yield "You must hold a bow to use Archer skills!";
            }
            case BEAST_MASTER -> {
                // Bare hand or Cestus
                if (bareHand || tag == WeaponTag.CESTUS)
                    yield null;
                yield "You must use bare hand or cestus to use Beast Master skills!";
            }
            case NINJA -> {
                // Kunai, Shuriken, or bare hand
                if (tag == WeaponTag.KUNAI || tag == WeaponTag.SHURIKEN || bareHand)
                    yield null;
                yield "You must hold a kunai, shuriken, or bare hand to use Ninja skills!";
            }
            case LIMITLESS -> {
                if (bareHand) yield null;
                yield "You must use bare hand to use Limitless skills!";
            }
            // Mage, Necromancer, Healer — no restriction
            case MAGE, NECROMANCER, HEALER -> null;
            default -> null;
        };
    }

    // ================================================================
    // Toggle deactivation
    // ================================================================

    private static void deactivateToggle(ServerPlayer player, SkillData sd, SkillType skill) {
        sd.setToggleActive(skill, false);
        PlayerClass cls = skill.getRequiredClass();
        if (cls == null) {
            // No novice toggles currently, but handle gracefully
            player.sendSystemMessage(Component.literal(
                    "\u00a7b[System]\u00a7r \u00a77" + skill.getDisplayName() + " deactivated."));
        } else {
            switch (cls) {
                case WARRIOR -> WarriorSkills.deactivateToggle(player, sd, skill);
                case ASSASSIN -> AssassinSkills.deactivateToggle(player, sd, skill);
                case ARCHER -> ArcherSkills.deactivateToggle(player, sd, skill);
                case MAGE -> MageSkills.deactivateToggle(player, sd, skill);
                case NINJA -> NinjaSkills.deactivateToggle(player, sd, skill);
                case NECROMANCER -> NecromancerSkills.deactivateToggle(player, sd, skill);
                case BEAST_MASTER -> BeastMasterSkills.deactivateToggle(player, sd, skill);
                case LIMITLESS -> LimitlessSkills.deactivateToggle(player, sd, skill);
                default -> player.sendSystemMessage(Component.literal(
                        "\u00a7b[System]\u00a7r \u00a77" + skill.getDisplayName() + " deactivated."));
            }
        }
    }

    // ================================================================
    // Shadow Partner mirror
    // ================================================================

    // ================================================================
    // Hold-to-cast handler (called from C2SSkillHoldPacket)
    // ================================================================

    public static void handleHold(ServerPlayer player, PlayerStats stats, int slotIndex, boolean start) {
        if (slotIndex < 0 || slotIndex >= SkillData.MAX_SLOTS) return;
        SkillData sd = stats.getSkillData();
        SkillType skill = sd.getEquipped(slotIndex);
        if (skill == null) return;

        if (skill == SkillType.CURSED_TECHNIQUE_BLUE) {
            if (start) {
                LimitlessSkills.startBlueChannel(player, stats, sd);
            } else {
                // If Purple is channeling, releasing Red or Blue cancels it
                if (sd.isPurpleChanneling()) {
                    LimitlessSkills.endPurpleChannel(player, stats, sd);
                } else {
                    LimitlessSkills.endBlueChannel(player, stats, sd);
                }
            }
            StatEventHandler.syncToClient(player);
        } else if (skill == SkillType.CURSED_TECHNIQUE_RED) {
            if (start) {
                LimitlessSkills.startRedChannel(player, stats, sd);
            } else {
                if (sd.isPurpleChanneling()) {
                    LimitlessSkills.endPurpleChannel(player, stats, sd);
                } else {
                    LimitlessSkills.endRedChannel(player, stats, sd);
                }
            }
            StatEventHandler.syncToClient(player);
        }
    }

    // ================================================================
    // Shadow Partner mirror
    // ================================================================

    public static void mirrorSkillFromPartner(ServerPlayer player, PlayerStats stats, SkillType skill) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        ShadowPartnerEntity partner = AssassinSkills.findShadowPartner(player, sl);
        if (partner == null) return;

        int level = stats.getSkillData().getLevel(skill);
        if (level <= 0) return;

        float multiplier = AssassinSkills.getShadowPartnerDamageMultiplier(
                stats.getSkillData().getLevel(SkillType.SHADOW_PARTNER));

        PlayerClass cls = skill.getRequiredClass();
        if (cls == null) {
            NoviceSkills.mirrorSkill(sl, partner, player, stats, skill, level, multiplier);
        } else {
            switch (cls) {
                case WARRIOR -> WarriorSkills.mirrorSkill(sl, partner, player, stats, skill, level, multiplier);
                case ASSASSIN -> AssassinSkills.mirrorSkill(sl, partner, player, stats, skill, level, multiplier);
                case ARCHER -> ArcherSkills.mirrorSkill(sl, partner, player, stats, skill, level, multiplier);
                case HEALER -> HealerSkills.mirrorSkill(sl, partner, player, stats, skill, level, multiplier);
                case MAGE -> MageSkills.mirrorSkill(sl, partner, player, stats, skill, level, multiplier);
                case NINJA -> NinjaSkills.mirrorSkill(sl, partner, player, stats, skill, level, multiplier);
                case NECROMANCER -> NecromancerSkills.mirrorSkill(sl, partner, player, stats, skill, level, multiplier);
                case BEAST_MASTER -> BeastMasterSkills.mirrorSkill(sl, partner, player, stats, skill, level, multiplier);
                case LIMITLESS -> { /* Limitless skills cannot be mirrored */ }
                default -> { /* no mirror */ }
            }
        }
    }
}
