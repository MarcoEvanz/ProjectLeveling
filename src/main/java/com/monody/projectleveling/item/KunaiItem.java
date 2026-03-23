package com.monody.projectleveling.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.monody.projectleveling.entity.kunai.ThrownKunaiEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class KunaiItem extends SwordItem {
    private static final UUID CRIT_RATE_UUID = UUID.fromString("C9A0E5F3-7D4B-4C13-AF89-5E3B201D6C74");
    private static final UUID CRIT_DAMAGE_UUID = UUID.fromString("D0B1F604-8E5C-4D24-B09A-6F4C302E7D85");

    private final Multimap<Attribute, AttributeModifier> kunaiModifiers;

    public KunaiItem(Tier tier, double critRate, double critDamage, Properties properties) {
        // attackDamageModifier = 1, attackSpeedModifier = -2.2f (speed 1.8)
        super(tier, 1, -2.2f, properties);

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.putAll(super.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND));
        builder.put(ModAttributes.CRIT_RATE.get(), new AttributeModifier(
                CRIT_RATE_UUID, "Weapon modifier", critRate, AttributeModifier.Operation.ADDITION));
        builder.put(ModAttributes.CRIT_DAMAGE.get(), new AttributeModifier(
                CRIT_DAMAGE_UUID, "Weapon modifier", critDamage, AttributeModifier.Operation.ADDITION));
        this.kunaiModifiers = builder.build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.kunaiModifiers : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            ThrownKunaiEntity kunai = new ThrownKunaiEntity(level, player, stack.copy());
            kunai.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 2.5f, 1.0f);
            level.addFreshEntity(kunai);

            if (!player.getAbilities().instabuild) {
                player.setItemInHand(hand, ItemStack.EMPTY);
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0f, 1.5f);
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        player.getCooldowns().addCooldown(this, 20); // 1 second cooldown
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click to throw").withStyle(ChatFormatting.GRAY));
    }
}
