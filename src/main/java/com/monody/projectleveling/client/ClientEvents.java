package com.monody.projectleveling.client;

import com.monody.projectleveling.ProjectLeveling;
import com.monody.projectleveling.network.C2SRequestSyncPacket;
import com.monody.projectleveling.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class ClientEvents {
    @Mod.EventBusSubscriber(modid = ProjectLeveling.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(KeyBindings.STATUS_SCREEN);
            event.register(KeyBindings.QUEST_SCREEN);
        }
    }

    @Mod.EventBusSubscriber(modid = ProjectLeveling.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeBusEvents {
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
        }
    }
}
