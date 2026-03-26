package com.monody.projectleveling.network;

import com.monody.projectleveling.capability.PlayerStatsCapability;
import com.monody.projectleveling.event.StatEventHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SAllocateStatPacket {
    private final String statName;
    private final int count;

    public C2SAllocateStatPacket(String statName) {
        this(statName, 1);
    }

    public C2SAllocateStatPacket(String statName, int count) {
        this.statName = statName;
        this.count = Math.max(1, count);
    }

    public C2SAllocateStatPacket(FriendlyByteBuf buf) {
        this.statName = buf.readUtf(32);
        this.count = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(statName, 32);
        buf.writeInt(count);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;

        player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            boolean changed = false;
            for (int i = 0; i < count; i++) {
                if (!stats.allocateStat(statName)) break;
                changed = true;
            }
            if (changed) {
                StatEventHandler.applyStatModifiers(player, stats);
                StatEventHandler.syncToClient(player);
            }
        });

        ctx.get().setPacketHandled(true);
    }
}
