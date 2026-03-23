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

public class CestusItem extends SwordItem implements TaggedWeapon {
    private static final UUID MELEE_DAMAGE_UUID = UUID.fromString("C3D4E5F6-A7B8-4C9D-0E1F-2A3B4C5D6E7F");

    private final float meleeDamage;
    private final Multimap<Attribute, AttributeModifier> cestusModifiers;

    public CestusItem(Tier tier, float meleeDamage, Properties properties) {
        // Low damage (modifier 1), very fast speed (-1.8f → 2.2 attacks/sec)
        super(tier, 1, -1.8f, properties);
        this.meleeDamage = meleeDamage;

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.putAll(super.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND));
        builder.put(ModAttributes.MELEE_DAMAGE.get(), new AttributeModifier(
                MELEE_DAMAGE_UUID, "Weapon modifier", meleeDamage, AttributeModifier.Operation.ADDITION));
        this.cestusModifiers = builder.build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.cestusModifiers : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public WeaponTag getWeaponTag() { return WeaponTag.CESTUS; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("\u00a7a+" + String.format("%.0f", meleeDamage) + "% Melee Damage")
                .withStyle(ChatFormatting.GREEN));
    }
}
