package com.monody.projectleveling.network;

import com.monody.projectleveling.capability.PlayerStatsCapability;
import com.monody.projectleveling.event.StatEventHandler;
import com.monody.projectleveling.skill.SkillType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SEquipSkillPacket {
    private final String skillId;
    private final int slotIndex;

    public C2SEquipSkillPacket(String skillId, int slotIndex) {
        this.skillId = skillId;
        this.slotIndex = slotIndex;
    }

    public C2SEquipSkillPacket(FriendlyByteBuf buf) {
        this.skillId = buf.readUtf(32);
        this.slotIndex = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(skillId, 32);
        buf.writeInt(slotIndex);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;

        player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            if (skillId.isEmpty()) {
                stats.getSkillData().unequip(slotIndex);
            } else {
                SkillType skill = SkillType.fromId(skillId);
                if (skill != null) {
                    stats.getSkillData().equip(skill, slotIndex);
                }
            }
            StatEventHandler.syncToClient(player);
        });

        ctx.get().setPacketHandled(true);
    }
}
