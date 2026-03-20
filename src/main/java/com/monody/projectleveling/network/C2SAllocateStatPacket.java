package com.monody.projectleveling.network;

import com.monody.projectleveling.capability.PlayerStatsCapability;
import com.monody.projectleveling.event.StatEventHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SAllocateStatPacket {
    private final String statName;

    public C2SAllocateStatPacket(String statName) {
        this.statName = statName;
    }

    public C2SAllocateStatPacket(FriendlyByteBuf buf) {
        this.statName = buf.readUtf(32);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(statName, 32);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;

        player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            if (stats.allocateStat(statName)) {
                StatEventHandler.applyStatModifiers(player, stats);
                StatEventHandler.syncToClient(player);
            }
        });

        ctx.get().setPacketHandled(true);
    }
}
