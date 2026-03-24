package com.monody.projectleveling.client;

import com.monody.projectleveling.ProjectLeveling;
import com.monody.projectleveling.capability.PlayerStatsCapability;
import com.monody.projectleveling.entity.ModEntities;
import com.monody.projectleveling.entity.archer.SkillArrowRenderer;
import com.monody.projectleveling.entity.assassin.ShadowPartnerRenderer;
import com.monody.projectleveling.entity.mage.SkillFireballRenderer;
import com.monody.projectleveling.entity.necromancer.SkeletonMinionRenderer;
import com.monody.projectleveling.entity.kunai.ThrownKunaiRenderer;
import com.monody.projectleveling.entity.kunai.ThrownShurikenRenderer;
import com.monody.projectleveling.entity.ninja.FlyingRaijinKunaiRenderer;
import com.monody.projectleveling.entity.ninja.ShadowCloneRenderer;
import com.monody.projectleveling.item.ModItems;
import com.monody.projectleveling.network.C2SActivateSkillPacket;
import com.monody.projectleveling.network.C2SRequestSyncPacket;
import com.monody.projectleveling.network.C2SSkillHoldPacket;
import com.monody.projectleveling.network.ModNetwork;
import com.monody.projectleveling.skill.SkillData;
import com.monody.projectleveling.skill.SkillType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientEvents {
    @Mod.EventBusSubscriber(modid = ProjectLeveling.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                RegistryObject<Item>[] bows = new RegistryObject[]{
                        ModItems.WOOD_BOW, ModItems.STONE_BOW, ModItems.IRON_BOW,
                        ModItems.GOLD_BOW, ModItems.DIAMOND_BOW, ModItems.NETHERITE_BOW
                };
                for (RegistryObject<Item> bow : bows) {
                    ItemProperties.register(bow.get(), new ResourceLocation("pulling"), (stack, level, entity, seed) ->
                            entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
                    ItemProperties.register(bow.get(), new ResourceLocation("pull"), (stack, level, entity, seed) -> {
                        if (entity == null) return 0.0F;
                        return entity.getUseItem() != stack ? 0.0F
                                : (float)(stack.getUseDuration() - entity.getUseItemRemainingTicks()) / 20.0F;
                    });
                }
            });
        }

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(KeyBindings.STATUS_SCREEN);
            event.register(KeyBindings.QUEST_SCREEN);
            event.register(KeyBindings.SKILL_TREE_SCREEN);
            event.register(KeyBindings.SKILL_SLOT_1);
            event.register(KeyBindings.SKILL_SLOT_2);
            event.register(KeyBindings.SKILL_SLOT_3);
            event.register(KeyBindings.SKILL_SLOT_4);
            event.register(KeyBindings.SKILL_SLOT_5);
            event.register(KeyBindings.SKILL_SLOT_6);
            event.register(KeyBindings.SKILL_SLOT_7);
        }

        @SubscribeEvent
        public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("skill_hud", new SkillHud());
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.SKILL_FIREBALL.get(), SkillFireballRenderer::new);
            event.registerEntityRenderer(ModEntities.SKILL_ARROW.get(), SkillArrowRenderer::new);
            event.registerEntityRenderer(ModEntities.SHADOW_PARTNER.get(), ShadowPartnerRenderer::new);
            event.registerEntityRenderer(ModEntities.SKELETON_MINION.get(), SkeletonMinionRenderer::new);
            event.registerEntityRenderer(ModEntities.SHADOW_CLONE.get(), ShadowCloneRenderer::new);
            event.registerEntityRenderer(ModEntities.FLYING_RAIJIN_KUNAI.get(), FlyingRaijinKunaiRenderer::new);
            event.registerEntityRenderer(ModEntities.THROWN_KUNAI.get(), ThrownKunaiRenderer::new);
            event.registerEntityRenderer(ModEntities.THROWN_SHURIKEN.get(), ThrownShurikenRenderer::new);
        }
    }

    @Mod.EventBusSubscriber(modid = ProjectLeveling.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeBusEvents {
        private static final EquipmentSlot[] ARMOR_SLOTS = {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        private static final Map<UUID, ItemStack[]> stealthArmorCache = new HashMap<>();

        // Hold detection for Cursed Technique: Blue
        private static final int HOLD_THRESHOLD = 5; // ticks before hold activates
        private static int holdSlot = -1;
        private static int holdTicks = 0;
        private static boolean holdSent = false;

        @SubscribeEvent
        public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
            Player player = event.getEntity();
            player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                if (stats.getSkillData().isToggleActive(SkillType.STEALTH)) {
                    ItemStack[] saved = new ItemStack[4];
                    for (int i = 0; i < ARMOR_SLOTS.length; i++) {
                        saved[i] = player.getItemBySlot(ARMOR_SLOTS[i]).copy();
                        player.setItemSlot(ARMOR_SLOTS[i], ItemStack.EMPTY);
                    }
                    stealthArmorCache.put(player.getUUID(), saved);
                }
            });
        }

        @SubscribeEvent
        public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
            Player player = event.getEntity();
            ItemStack[] saved = stealthArmorCache.remove(player.getUUID());
            if (saved != null) {
                for (int i = 0; i < ARMOR_SLOTS.length; i++) {
                    player.setItemSlot(ARMOR_SLOTS[i], saved[i]);
                }
            }
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            if (KeyBindings.STATUS_SCREEN.consumeClick()) {
                ModNetwork.sendToServer(new C2SRequestSyncPacket());
                mc.setScreen(new StatusScreen());
            }
            if (KeyBindings.QUEST_SCREEN.consumeClick()) {
                ModNetwork.sendToServer(new C2SRequestSyncPacket());
                mc.setScreen(new QuestScreen());
            }
            if (KeyBindings.SKILL_TREE_SCREEN.consumeClick()) {
                ModNetwork.sendToServer(new C2SRequestSyncPacket());
                mc.setScreen(new SkillTreeScreen());
            }

            // Skill activation (only when no screen is open)
            if (mc.screen == null) {
                // Hold tracking: if currently tracking a hold, count ticks
                if (holdSlot >= 0) {
                    boolean stillDown = com.mojang.blaze3d.platform.InputConstants.isKeyDown(
                            mc.getWindow().getWindow(), getSlotKey(holdSlot).getKey().getValue());
                    if (stillDown) {
                        holdTicks++;
                        if (holdTicks == HOLD_THRESHOLD && !holdSent) {
                            // Threshold reached: send hold-start
                            ModNetwork.sendToServer(new C2SSkillHoldPacket(holdSlot, true));
                            holdSent = true;
                        }
                        // Drain ALL pending clicks so they don't fire as tap after release
                        for (int i = 0; i < 7; i++) {
                            while (getSlotKey(i).consumeClick()) { /* drain */ }
                        }
                    } else {
                        // Released
                        if (holdSent) {
                            // Was holding: send hold-end
                            ModNetwork.sendToServer(new C2SSkillHoldPacket(holdSlot, false));
                        } else {
                            // Released before threshold: send normal tap
                            ModNetwork.sendToServer(new C2SActivateSkillPacket(holdSlot));
                        }
                        holdSlot = -1;
                        holdTicks = 0;
                        holdSent = false;
                        // Drain ALL leftover clicks from auto-repeat during hold
                        for (int i = 0; i < 7; i++) {
                            while (getSlotKey(i).consumeClick()) { /* drain */ }
                        }
                    }
                } else {
                    // Check for new skill key presses
                    for (int i = 0; i < 7; i++) {
                        if (getSlotKey(i).consumeClick()) {
                            if (isHoldableSlot(mc.player, i)) {
                                // Start tracking hold
                                holdSlot = i;
                                holdTicks = 0;
                                holdSent = false;
                            } else {
                                ModNetwork.sendToServer(new C2SActivateSkillPacket(i));
                            }
                            break;
                        }
                    }
                }
            }
        }

        private static net.minecraft.client.KeyMapping getSlotKey(int index) {
            return switch (index) {
                case 0 -> KeyBindings.SKILL_SLOT_1;
                case 1 -> KeyBindings.SKILL_SLOT_2;
                case 2 -> KeyBindings.SKILL_SLOT_3;
                case 3 -> KeyBindings.SKILL_SLOT_4;
                case 4 -> KeyBindings.SKILL_SLOT_5;
                case 5 -> KeyBindings.SKILL_SLOT_6;
                case 6 -> KeyBindings.SKILL_SLOT_7;
                default -> KeyBindings.SKILL_SLOT_1;
            };
        }

        private static boolean isHoldableSlot(Player player, int slotIndex) {
            return player.getCapability(PlayerStatsCapability.PLAYER_STATS)
                    .map(stats -> {
                        SkillType skill = stats.getSkillData().getEquipped(slotIndex);
                        return skill == SkillType.CURSED_TECHNIQUE_BLUE;
                    })
                    .orElse(false);
        }
    }
}
