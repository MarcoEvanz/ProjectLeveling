package com.monody.projectleveling.network;

import com.monody.projectleveling.ProjectLeveling;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ProjectLeveling.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        CHANNEL.messageBuilder(C2SAllocateStatPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SAllocateStatPacket::encode)
                .decoder(C2SAllocateStatPacket::new)
                .consumerMainThread(C2SAllocateStatPacket::handle)
                .add();

        CHANNEL.messageBuilder(C2SRequestSyncPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SRequestSyncPacket::encode)
                .decoder(C2SRequestSyncPacket::new)
                .consumerMainThread(C2SRequestSyncPacket::handle)
                .add();

        CHANNEL.messageBuilder(C2SClaimQuestRewardPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SClaimQuestRewardPacket::encode)
                .decoder(C2SClaimQuestRewardPacket::new)
                .consumerMainThread(C2SClaimQuestRewardPacket::handle)
                .add();

        CHANNEL.messageBuilder(S2CSyncStatsPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CSyncStatsPacket::encode)
                .decoder(S2CSyncStatsPacket::new)
                .consumerMainThread(S2CSyncStatsPacket::handle)
                .add();
    }

    public static void sendToServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }

    public static void sendToPlayer(Object msg, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }
}
