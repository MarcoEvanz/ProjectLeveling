package com.monody.projectleveling.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class ModBowItem extends BowItem implements TaggedWeapon {
    private static final UUID BASE_ATTACK_DAMAGE_UUID = UUID.fromString("E1F2A3B4-C5D6-4E7F-8091-A2B3C4D5E6F7");
    private static final UUID PROJECTILE_DAMAGE_UUID = UUID.fromString("F2A3B4C5-D6E7-4F80-91A2-B3C4D5E6F7A8");

    private final Tier tier;
    private final float attackDamage;
    private final float projectileDamage;
    private final Multimap<Attribute, AttributeModifier> bowModifiers;

    public ModBowItem(Tier tier, float attackDamage, float projectileDamage, Properties properties) {
        super(properties.defaultDurability(tier.getUses()));
        this.tier = tier;
        this.attackDamage = attackDamage;
        this.projectileDamage = projectileDamage;

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", attackDamage, AttributeModifier.Operation.ADDITION));
        builder.put(ModAttributes.PROJECTILE_DAMAGE.get(), new AttributeModifier(
                PROJECTILE_DAMAGE_UUID, "Weapon modifier", projectileDamage, AttributeModifier.Operation.ADDITION));
        this.bowModifiers = builder.build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.bowModifiers : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public int getEnchantmentValue() {
        return tier.getEnchantmentValue();
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairItem) {
        return tier.getRepairIngredient().test(repairItem) || super.isValidRepairItem(stack, repairItem);
    }

    @Override
    public WeaponTag getWeaponTag() { return WeaponTag.BOW; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("\u00a7a+" + String.format("%.0f", projectileDamage) + "% Projectile Damage")
                .withStyle(ChatFormatting.GREEN));
    }
}
