package com.monody.projectleveling.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class StaffItem extends Item {
    private static final UUID BASE_ATTACK_DAMAGE_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    private static final UUID BASE_ATTACK_SPEED_UUID = UUID.fromString("FA235E1C-4180-4865-B01B-BCCE9785ACA3");
    private static final UUID MAGIC_ATTACK_UUID = UUID.fromString("7E0292F2-9434-48D5-A29F-9583BF305A97");

    private final Tier tier;
    private final float attackDamage;
    private final float magicAttack;
    private final Multimap<Attribute, AttributeModifier> attributeModifiers;

    public StaffItem(Tier tier, float attackDamage, float magicAttack, Properties properties) {
        super(properties.defaultDurability(tier.getUses()));
        this.tier = tier;
        this.attackDamage = attackDamage;
        this.magicAttack = magicAttack;

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", attackDamage, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                BASE_ATTACK_SPEED_UUID, "Weapon modifier", -3.2, AttributeModifier.Operation.ADDITION));
        builder.put(ModAttributes.MAGIC_ATTACK.get(), new AttributeModifier(
                MAGIC_ATTACK_UUID, "Weapon modifier", magicAttack, AttributeModifier.Operation.ADDITION));
        this.attributeModifiers = builder.build();
    }

    public float getMagicAttack() {
        return magicAttack;
    }

    public Tier getTier() {
        return tier;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.attributeModifiers : super.getDefaultAttributeModifiers(slot);
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
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("\u00a79+" + String.format("%.0f", magicAttack) + " Magic Attack")
                .withStyle(ChatFormatting.BLUE));
    }
}
