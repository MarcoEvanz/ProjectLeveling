package com.monody.projectleveling.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class ModSwordItem extends SwordItem implements TaggedWeapon {
    private static final UUID MELEE_DAMAGE_UUID = UUID.fromString("A1B2C3D4-E5F6-4A7B-8C9D-0E1F2A3B4C5D");

    private final float meleeDamage;
    private final Multimap<Attribute, AttributeModifier> swordModifiers;

    public ModSwordItem(Tier tier, float meleeDamage, Properties properties) {
        // Greatsword stats: modifier=4 (vs vanilla sword 3), speed=-2.8f (1.2, vs vanilla 1.6)
        super(tier, 4, -2.8f, properties);
        this.meleeDamage = meleeDamage;

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.putAll(super.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND));
        builder.put(ModAttributes.MELEE_DAMAGE.get(), new AttributeModifier(
                MELEE_DAMAGE_UUID, "Weapon modifier", meleeDamage, AttributeModifier.Operation.ADDITION));
        this.swordModifiers = builder.build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.swordModifiers : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public WeaponTag getWeaponTag() { return WeaponTag.SWORD; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("\u00a7a+" + String.format("%.0f", meleeDamage) + "% Melee Damage")
                .withStyle(ChatFormatting.GREEN));
    }
}
