package com.monody.projectleveling.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

import java.util.UUID;

public class DaggerItem extends SwordItem {
    private static final UUID CRIT_RATE_UUID = UUID.fromString("A7E8C3D1-5B2F-4A91-8D67-3C1F0E9B4A52");
    private static final UUID CRIT_DAMAGE_UUID = UUID.fromString("B8F9D4E2-6C3A-4B02-9E78-4D2A1F0C5B63");

    private final Multimap<Attribute, AttributeModifier> daggerModifiers;

    public DaggerItem(Tier tier, double critRate, double critDamage, Properties properties) {
        // attackDamageModifier = 1 (sword uses 3), attackSpeedModifier = -2.4f (speed 1.6)
        super(tier, 1, -2.4f, properties);

        // Build modifiers: inherit sword's ATK/SPD + add crit rate & crit damage
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.putAll(super.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND));
        builder.put(ModAttributes.CRIT_RATE.get(), new AttributeModifier(
                CRIT_RATE_UUID, "Weapon modifier", critRate, AttributeModifier.Operation.ADDITION));
        builder.put(ModAttributes.CRIT_DAMAGE.get(), new AttributeModifier(
                CRIT_DAMAGE_UUID, "Weapon modifier", critDamage, AttributeModifier.Operation.ADDITION));
        this.daggerModifiers = builder.build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.daggerModifiers : super.getDefaultAttributeModifiers(slot);
    }

    public static boolean isDagger(ItemStack stack) {
        return stack.getItem() instanceof DaggerItem;
    }
}
