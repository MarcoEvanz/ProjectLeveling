package com.monody.projectleveling.network;

import com.monody.projectleveling.dimension.DungeonHelper;
import com.monody.projectleveling.dimension.ModDimensions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SReturnFromDungeonPacket {

    public C2SReturnFromDungeonPacket() {}

    public C2SReturnFromDungeonPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (!ModDimensions.isDungeon(player.level().dimension())) return;

            DungeonHelper.returnToOverworld(player);
            player.sendSystemMessage(Component.literal(
                    "\u00a76[Dungeon]\u00a7r Returned to the Overworld."));
        });
        ctx.get().setPacketHandled(true);
    }
}
