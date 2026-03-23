package com.monody.projectleveling;

import com.mojang.logging.LogUtils;
import com.monody.projectleveling.command.ModCommands;
import com.monody.projectleveling.entity.ModEntities;
import com.monody.projectleveling.entity.assassin.ShadowPartnerEntity;
import com.monody.projectleveling.entity.necromancer.SkeletonMinionEntity;
import com.monody.projectleveling.entity.ninja.ShadowCloneEntity;
import com.monody.projectleveling.item.ModAttributes;
import com.monody.projectleveling.item.ModItems;
import com.monody.projectleveling.network.ModNetwork;
import com.monody.projectleveling.sound.ModSounds;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(ProjectLeveling.MOD_ID)
public class ProjectLeveling {
    public static final String MOD_ID = "projectleveling";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ProjectLeveling() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register network packets
        ModNetwork.register();

        // Register entities
        ModEntities.ENTITIES.register(modEventBus);

        // Register items and creative tabs
        ModItems.ITEMS.register(modEventBus);
        ModItems.CREATIVE_TABS.register(modEventBus);

        // Register custom attributes
        ModAttributes.ATTRIBUTES.register(modEventBus);

        // Register custom sounds
        ModSounds.SOUNDS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onEntityAttributeCreation);
        modEventBus.addListener(this::onEntityAttributeModification);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Project Leveling common setup");
    }

    private void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SHADOW_PARTNER.get(), ShadowPartnerEntity.createAttributes().build());
        event.put(ModEntities.SKELETON_MINION.get(), SkeletonMinionEntity.createAttributes().build());
        event.put(ModEntities.SHADOW_CLONE.get(), ShadowCloneEntity.createAttributes().build());
    }

    private void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        // Add custom attributes to players so equipment modifiers work
        event.add(EntityType.PLAYER, ModAttributes.MAGIC_ATTACK.get());
        event.add(EntityType.PLAYER, ModAttributes.CRIT_RATE.get());
        event.add(EntityType.PLAYER, ModAttributes.CRIT_DAMAGE.get());
        event.add(EntityType.PLAYER, ModAttributes.ATTACK_PERCENT.get());
        event.add(EntityType.PLAYER, ModAttributes.MAGIC_ATTACK_PERCENT.get());
        event.add(EntityType.PLAYER, ModAttributes.FINAL_DAMAGE.get());
        event.add(EntityType.PLAYER, ModAttributes.PROJECTILE_DAMAGE.get());
        event.add(EntityType.PLAYER, ModAttributes.MELEE_DAMAGE.get());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Project Leveling server starting");
    }
}
