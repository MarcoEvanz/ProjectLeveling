package com.monody.projectleveling.skill;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.List;

/**
 * Utility for sending combat log messages to the player's chat.
 * Damage values shown are post-armor/enchantment reduction.
 */
public class CombatLog {

    // Flag to suppress LivingDamageEvent logging during AoE loops (entity code logs summary instead)
    public static boolean suppressDamageLog = false;

    // --- Armor reduction utility ---

    public static float afterArmor(LivingEntity target, DamageSource src, float rawDamage) {
        float armor = target.getArmorValue();
        float toughness = (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float afterArmor = CombatRules.getDamageAfterAbsorb(rawDamage, armor, toughness);
        int protLevel = EnchantmentHelper.getDamageProtection(target.getArmorSlots(), src);
        return CombatRules.getDamageAfterMagicAbsorb(afterArmor, (float) protLevel);
    }

    // --- Skill damage logging (calculates post-armor automatically) ---

    public static void aoeSkill(ServerPlayer player, String source, float rawDamage,
                                List<? extends LivingEntity> targets) {
        aoeSkill(player, source, rawDamage, targets, SkillDamageSource.get(player.level()));
    }

    public static void aoeSkill(ServerPlayer player, String source, float rawDamage,
                                List<? extends LivingEntity> targets, DamageSource src) {
        if (targets.isEmpty()) return;
        float totalFinal = 0;
        for (LivingEntity t : targets) {
            totalFinal += afterArmor(t, src, rawDamage);
        }
        aoe(player, source, totalFinal / targets.size(), targets.size());
    }

    public static void damageSkill(ServerPlayer player, String source, float rawDamage, LivingEntity target) {
        damageSkill(player, source, rawDamage, target, SkillDamageSource.get(player.level()));
    }

    public static void damageSkill(ServerPlayer player, String source, float rawDamage,
                                   LivingEntity target, DamageSource src) {
        float finalDmg = afterArmor(target, src, rawDamage);
        damage(player, source, finalDmg, target);
    }

    // --- Raw logging methods (use when damage is already post-reduction) ---

    public static void damage(ServerPlayer player, float amount, LivingEntity target) {
        player.sendSystemMessage(Component.literal(
                "\u00a77\u2694 " + String.format("%.1f", amount) + " \u00a78\u2192 " + target.getName().getString()));
    }

    public static void damage(ServerPlayer player, String source, float amount, LivingEntity target) {
        player.sendSystemMessage(Component.literal(
                "\u00a7d\u2726 " + source + " \u00a77" + String.format("%.1f", amount)
                        + " \u00a78\u2192 " + target.getName().getString()));
    }

    public static void aoe(ServerPlayer player, String source, float perHit, int hitCount) {
        player.sendSystemMessage(Component.literal(
                "\u00a7d\u2726 " + source + " \u00a77" + String.format("%.1f", perHit)
                        + " \u00a78x" + hitCount + " (" + String.format("%.1f", perHit * hitCount) + " total)"));
    }

    public static void heal(ServerPlayer player, String source, float amount) {
        player.sendSystemMessage(Component.literal(
                "\u00a7a\u2764 " + source + " \u00a7a+" + String.format("%.1f", amount) + " HP"));
    }

    public static void healTarget(ServerPlayer player, String source, float amount, LivingEntity target) {
        player.sendSystemMessage(Component.literal(
                "\u00a7a\u2764 " + source + " \u00a7a+" + String.format("%.1f", amount)
                        + " HP \u00a78\u2192 " + target.getName().getString()));
    }

    public static void pet(ServerPlayer player, float amount, LivingEntity target) {
        player.sendSystemMessage(Component.literal(
                "\u00a78\u2694 Shadow \u00a77" + String.format("%.1f", amount)
                        + " \u00a78\u2192 " + target.getName().getString()));
    }

    public static void petRanged(ServerPlayer player, String type, float amount) {
        player.sendSystemMessage(Component.literal(
                "\u00a78\u2694 Shadow " + type + " \u00a77" + String.format("%.1f", amount)));
    }
}
