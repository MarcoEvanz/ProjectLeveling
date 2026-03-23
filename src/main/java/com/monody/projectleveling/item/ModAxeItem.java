package com.monody.projectleveling.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class ModAxeItem extends AxeItem implements TaggedWeapon {
    private static final UUID MELEE_DAMAGE_UUID = UUID.fromString("B2C3D4E5-F6A7-4B8C-9D0E-1F2A3B4C5D6E");

    private final float meleeDamage;
    private final Multimap<Attribute, AttributeModifier> axeModifiers;

    public ModAxeItem(Tier tier, float attackDamage, float attackSpeed, float meleeDamage, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
        this.meleeDamage = meleeDamage;

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.putAll(super.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND));
        builder.put(ModAttributes.MELEE_DAMAGE.get(), new AttributeModifier(
                MELEE_DAMAGE_UUID, "Weapon modifier", meleeDamage, AttributeModifier.Operation.ADDITION));
        this.axeModifiers = builder.build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.axeModifiers : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public WeaponTag getWeaponTag() { return WeaponTag.AXE; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("\u00a7a+" + String.format("%.0f", meleeDamage) + "% Melee Damage")
                .withStyle(ChatFormatting.GREEN));
    }
}
