package com.monody.projectleveling.network;

import com.monody.projectleveling.capability.PlayerStatsCapability;
import com.monody.projectleveling.skill.SkillExecutor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SActivateSkillPacket {
    private final int slotIndex;

    public C2SActivateSkillPacket(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    public C2SActivateSkillPacket(FriendlyByteBuf buf) {
        this.slotIndex = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(slotIndex);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;

        player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            SkillExecutor.activateSlot(player, stats, slotIndex);
        });

        ctx.get().setPacketHandled(true);
    }
}
