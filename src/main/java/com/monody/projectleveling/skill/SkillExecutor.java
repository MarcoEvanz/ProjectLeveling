package com.monody.projectleveling.skill;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.entity.assassin.ShadowPartnerEntity;
import com.monody.projectleveling.event.StatEventHandler;
import com.monody.projectleveling.skill.classes.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class SkillExecutor {

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

        if (sd.isOnCooldown(skill)) {
            int secs = (sd.getCooldownRemaining(skill) + 19) / 20;
            player.sendSystemMessage(Component.literal(
                    "\u00a7c" + skill.getDisplayName() + " is on cooldown (" + secs + "s)"));
            return;
        }

        int level = sd.getLevel(skill);
        if (level <= 0) return;

        int mpCost = skill.getMpCost(level);
        // Infinity: all skills cost 0 MP
        if (sd.getInfinityTicks() > 0 && skill != SkillType.INFINITY) {
            mpCost = 0;
        }
        // Element Amplification: +20% MP cost
        int eaLv = sd.getLevel(SkillType.ELEMENT_AMPLIFICATION);
        if (eaLv > 0 && mpCost > 0) {
            mpCost = (int) (mpCost * 1.2);
        }
        if (!skill.isToggle() && stats.getCurrentMp() < mpCost) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough MP!"));
            return;
        }

        // Dispatch execution to per-class file
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
                case NECROMANCER -> NecromancerSkills.execute(player, stats, sd, skill, level);
                default -> { /* passives handled elsewhere */ }
            }
        }

        // Shadow Partner mirrors the skill
        if (sd.isToggleActive(SkillType.SHADOW_PARTNER) && skill != SkillType.SHADOW_PARTNER) {
            mirrorSkillFromPartner(player, stats, skill);
        }

        StatEventHandler.syncToClient(player);
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
                case NECROMANCER -> NecromancerSkills.deactivateToggle(player, sd, skill);
                default -> player.sendSystemMessage(Component.literal(
                        "\u00a7b[System]\u00a7r \u00a77" + skill.getDisplayName() + " deactivated."));
            }
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
                case NECROMANCER -> NecromancerSkills.mirrorSkill(sl, partner, player, stats, skill, level, multiplier);
                default -> { /* no mirror */ }
            }
        }
    }
}
