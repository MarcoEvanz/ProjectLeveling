package com.monody.projectleveling.item;

import com.monody.projectleveling.entity.kunai.ThrownShurikenEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class ShurikenItem extends SwordItem {

    public ShurikenItem(Tier tier, Properties properties) {
        // Same stats as kunai: attackDamageModifier = 1, attackSpeedModifier = -2.2f (speed 1.8)
        super(tier, 1, -2.2f, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            ThrownShurikenEntity shuriken = new ThrownShurikenEntity(level, player, stack.copy());
            shuriken.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 2.5f, 1.0f);
            level.addFreshEntity(shuriken);

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
