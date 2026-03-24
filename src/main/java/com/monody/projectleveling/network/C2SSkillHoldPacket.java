package com.monody.projectleveling.network;

import com.monody.projectleveling.capability.PlayerStatsCapability;
import com.monody.projectleveling.skill.SkillExecutor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SSkillHoldPacket {
    private final int slotIndex;
    private final boolean start; // true = hold started, false = hold ended

    public C2SSkillHoldPacket(int slotIndex, boolean start) {
        this.slotIndex = slotIndex;
        this.start = start;
    }

    public C2SSkillHoldPacket(FriendlyByteBuf buf) {
        this.slotIndex = buf.readInt();
        this.start = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(slotIndex);
        buf.writeBoolean(start);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;

        player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            SkillExecutor.handleHold(player, stats, slotIndex, start);
        });

        ctx.get().setPacketHandled(true);
    }
}
