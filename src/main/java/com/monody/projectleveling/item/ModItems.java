package com.monody.projectleveling.item;

import com.monody.projectleveling.ProjectLeveling;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ProjectLeveling.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ProjectLeveling.MOD_ID);

    // ================================================================
    // Staffs — 20% sword ATK, high MATK, slow speed (0.8)
    // ATK modifier: total ATK = 1(base) + modifier
    // Wood/Stone/Iron/Gold → 0 modifier (1 ATK), Diamond → 0 (1 ATK), Netherite → 1 (2 ATK)
    // ================================================================
    public static final RegistryObject<Item> WOOD_STAFF = ITEMS.register("wood_staff",
            () -> new StaffItem(Tiers.WOOD, 0f, 4f, new Item.Properties()));
    public static final RegistryObject<Item> STONE_STAFF = ITEMS.register("stone_staff",
            () -> new StaffItem(Tiers.STONE, 0f, 5f, new Item.Properties()));
    public static final RegistryObject<Item> IRON_STAFF = ITEMS.register("iron_staff",
            () -> new StaffItem(Tiers.IRON, 0f, 7f, new Item.Properties()));
    public static final RegistryObject<Item> GOLD_STAFF = ITEMS.register("gold_staff",
            () -> new StaffItem(Tiers.GOLD, 0f, 9f, new Item.Properties()));
    public static final RegistryObject<Item> DIAMOND_STAFF = ITEMS.register("diamond_staff",
            () -> new StaffItem(Tiers.DIAMOND, 0f, 10f, new Item.Properties()));
    public static final RegistryObject<Item> NETHERITE_STAFF = ITEMS.register("netherite_staff",
            () -> new StaffItem(Tiers.NETHERITE, 1f, 12f, new Item.Properties().fireResistant()));

    // ================================================================
    // Kunai — fast (1.8), low damage, throwable on right-click
    // SwordItem(tier, attackDamageModifier=1, attackSpeedModifier=-2.2f)
    // Total ATK: Wood 2, Stone 3, Iron 4, Gold 2, Diamond 5, Netherite 6
    // Crit Rate: 4/6/10/8/15/20%  |  Crit Damage: 8/12/20/16/28/40%
    // ================================================================
    public static final RegistryObject<Item> WOOD_KUNAI = ITEMS.register("wood_kunai",
            () -> new KunaiItem(Tiers.WOOD, 4, 8, new Item.Properties()));
    public static final RegistryObject<Item> STONE_KUNAI = ITEMS.register("stone_kunai",
            () -> new KunaiItem(Tiers.STONE, 6, 12, new Item.Properties()));
    public static final RegistryObject<Item> IRON_KUNAI = ITEMS.register("iron_kunai",
            () -> new KunaiItem(Tiers.IRON, 10, 20, new Item.Properties()));
    public static final RegistryObject<Item> GOLD_KUNAI = ITEMS.register("gold_kunai",
            () -> new KunaiItem(Tiers.GOLD, 8, 16, new Item.Properties()));
    public static final RegistryObject<Item> DIAMOND_KUNAI = ITEMS.register("diamond_kunai",
            () -> new KunaiItem(Tiers.DIAMOND, 15, 28, new Item.Properties()));
    public static final RegistryObject<Item> NETHERITE_KUNAI = ITEMS.register("netherite_kunai",
            () -> new KunaiItem(Tiers.NETHERITE, 20, 40, new Item.Properties().fireResistant()));

    // ================================================================
    // Daggers — speed 1.6, low damage, +10% crit chance
    // SwordItem(tier, attackDamageModifier=1, attackSpeedModifier=-2.4f)
    // Total ATK: Wood 2, Stone 3, Iron 4, Gold 2, Diamond 5, Netherite 6
    // Crit Rate: 4/6/10/8/15/20%  |  Crit Damage: 8/12/20/16/28/40%
    // ================================================================
    public static final RegistryObject<Item> WOOD_DAGGER = ITEMS.register("wood_dagger",
            () -> new DaggerItem(Tiers.WOOD, 4, 8, new Item.Properties()));
    public static final RegistryObject<Item> STONE_DAGGER = ITEMS.register("stone_dagger",
            () -> new DaggerItem(Tiers.STONE, 6, 12, new Item.Properties()));
    public static final RegistryObject<Item> IRON_DAGGER = ITEMS.register("iron_dagger",
            () -> new DaggerItem(Tiers.IRON, 10, 20, new Item.Properties()));
    public static final RegistryObject<Item> GOLD_DAGGER = ITEMS.register("gold_dagger",
            () -> new DaggerItem(Tiers.GOLD, 8, 16, new Item.Properties()));
    public static final RegistryObject<Item> DIAMOND_DAGGER = ITEMS.register("diamond_dagger",
            () -> new DaggerItem(Tiers.DIAMOND, 15, 28, new Item.Properties()));
    public static final RegistryObject<Item> NETHERITE_DAGGER = ITEMS.register("netherite_dagger",
            () -> new DaggerItem(Tiers.NETHERITE, 20, 40, new Item.Properties().fireResistant()));

    // ================================================================
    // Shuriken — fast (1.8), low damage, throwable with spinning
    // Same stats as Kunai
    // ================================================================
    public static final RegistryObject<Item> WOOD_SHURIKEN = ITEMS.register("wood_shuriken",
            () -> new ShurikenItem(Tiers.WOOD, new Item.Properties()));
    public static final RegistryObject<Item> STONE_SHURIKEN = ITEMS.register("stone_shuriken",
            () -> new ShurikenItem(Tiers.STONE, new Item.Properties()));
    public static final RegistryObject<Item> IRON_SHURIKEN = ITEMS.register("iron_shuriken",
            () -> new ShurikenItem(Tiers.IRON, new Item.Properties()));
    public static final RegistryObject<Item> GOLD_SHURIKEN = ITEMS.register("gold_shuriken",
            () -> new ShurikenItem(Tiers.GOLD, new Item.Properties()));
    public static final RegistryObject<Item> DIAMOND_SHURIKEN = ITEMS.register("diamond_shuriken",
            () -> new ShurikenItem(Tiers.DIAMOND, new Item.Properties()));
    public static final RegistryObject<Item> NETHERITE_SHURIKEN = ITEMS.register("netherite_shuriken",
            () -> new ShurikenItem(Tiers.NETHERITE, new Item.Properties().fireResistant()));

    // ================================================================
    // Creative Tab
    // ================================================================
    public static final RegistryObject<CreativeModeTab> PROJECT_LEVELING_TAB = CREATIVE_TABS.register(
            "projectleveling_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.literal("Project Leveling"))
                    .icon(() -> IRON_STAFF.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        // Staffs
                        output.accept(WOOD_STAFF.get());
                        output.accept(STONE_STAFF.get());
                        output.accept(IRON_STAFF.get());
                        output.accept(GOLD_STAFF.get());
                        output.accept(DIAMOND_STAFF.get());
                        output.accept(NETHERITE_STAFF.get());
                        // Kunai
                        output.accept(WOOD_KUNAI.get());
                        output.accept(STONE_KUNAI.get());
                        output.accept(IRON_KUNAI.get());
                        output.accept(GOLD_KUNAI.get());
                        output.accept(DIAMOND_KUNAI.get());
                        output.accept(NETHERITE_KUNAI.get());
                        // Daggers
                        output.accept(WOOD_DAGGER.get());
                        output.accept(STONE_DAGGER.get());
                        output.accept(IRON_DAGGER.get());
                        output.accept(GOLD_DAGGER.get());
                        output.accept(DIAMOND_DAGGER.get());
                        output.accept(NETHERITE_DAGGER.get());
                        // Shuriken
                        output.accept(WOOD_SHURIKEN.get());
                        output.accept(STONE_SHURIKEN.get());
                        output.accept(IRON_SHURIKEN.get());
                        output.accept(GOLD_SHURIKEN.get());
                        output.accept(DIAMOND_SHURIKEN.get());
                        output.accept(NETHERITE_SHURIKEN.get());
                    })
                    .build()
    );
}
