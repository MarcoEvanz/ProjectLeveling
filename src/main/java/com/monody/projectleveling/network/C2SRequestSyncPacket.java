package com.monody.projectleveling.network;

import com.monody.projectleveling.event.StatEventHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SRequestSyncPacket {
    public C2SRequestSyncPacket() {}

    public C2SRequestSyncPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;

        StatEventHandler.syncToClient(player);

        ctx.get().setPacketHandled(true);
    }
}
