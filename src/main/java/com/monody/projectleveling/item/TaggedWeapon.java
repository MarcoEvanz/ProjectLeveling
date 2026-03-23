package com.monody.projectleveling.item;

import net.minecraft.world.item.ItemStack;

public interface TaggedWeapon {
    WeaponTag getWeaponTag();

    static WeaponTag getTag(ItemStack stack) {
        if (stack.getItem() instanceof TaggedWeapon tagged) {
            return tagged.getWeaponTag();
        }
        return null;
    }

    static boolean hasTag(ItemStack stack, WeaponTag tag) {
        return tag == getTag(stack);
    }
}
