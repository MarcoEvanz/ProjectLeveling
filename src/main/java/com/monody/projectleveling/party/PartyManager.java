package com.monody.projectleveling.party;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PartyManager {

    private static final Map<UUID, Party> parties = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> playerToParty = new ConcurrentHashMap<>();

    // ======== Query ========

    @Nullable
    public static Party getParty(UUID playerUUID) {
        UUID partyId = playerToParty.get(playerUUID);
        return partyId != null ? parties.get(partyId) : null;
    }

    public static List<ServerPlayer> getOnlineMembers(Party party, MinecraftServer server) {
        List<ServerPlayer> result = new ArrayList<>();
        for (UUID uuid : party.getMembers()) {
            ServerPlayer sp = server.getPlayerList().getPlayer(uuid);
            if (sp != null) result.add(sp);
        }
        return result;
    }

    public static List<ServerPlayer> getMembersInDimension(Party party, MinecraftServer server,
                                                            ResourceKey<Level> dimension) {
        List<ServerPlayer> result = new ArrayList<>();
        for (UUID uuid : party.getMembers()) {
            ServerPlayer sp = server.getPlayerList().getPlayer(uuid);
            if (sp != null && sp.level().dimension().equals(dimension)) {
                result.add(sp);
            }
        }
        return result;
    }

    @Nullable
    public static Party getPendingInviteParty(UUID playerUUID) {
        for (Party party : parties.values()) {
            if (party.hasInvite(playerUUID)) return party;
        }
        return null;
    }

    // ======== Create / Disband ========

    @Nullable
    public static Party createParty(UUID leaderUUID) {
        if (playerToParty.containsKey(leaderUUID)) return null;
        Party party = new Party(leaderUUID);
        parties.put(party.getPartyId(), party);
        playerToParty.put(leaderUUID, party.getPartyId());
        return party;
    }

    /** @return true on success */
    public static boolean disband(UUID requesterUUID) {
        Party party = getParty(requesterUUID);
        if (party == null || !party.isLeader(requesterUUID)) return false;
        for (UUID member : party.getMembers()) {
            playerToParty.remove(member);
        }
        parties.remove(party.getPartyId());
        return true;
    }

    // ======== Invite / Accept / Decline ========

    /** 0=success, 1=not leader, 2=target in party, 3=full, 4=already invited, 5=no party */
    public static int invite(UUID inviterUUID, UUID targetUUID) {
        Party party = getParty(inviterUUID);
        if (party == null) return 5;
        if (!party.isLeader(inviterUUID)) return 1;
        if (playerToParty.containsKey(targetUUID)) return 2;
        if (party.isFull()) return 3;
        if (party.hasInvite(targetUUID)) return 4;
        party.addInvite(targetUUID);
        return 0;
    }

    @Nullable
    public static Party acceptInvite(UUID playerUUID) {
        if (playerToParty.containsKey(playerUUID)) return null;
        Party party = getPendingInviteParty(playerUUID);
        if (party == null || party.isFull()) return null;
        party.removeInvite(playerUUID);
        party.addMember(playerUUID);
        playerToParty.put(playerUUID, party.getPartyId());
        return party;
    }

    public static boolean declineInvite(UUID playerUUID) {
        Party party = getPendingInviteParty(playerUUID);
        if (party == null) return false;
        party.removeInvite(playerUUID);
        return true;
    }

    // ======== Leave / Kick ========

    public static boolean leave(UUID playerUUID) {
        Party party = getParty(playerUUID);
        if (party == null) return false;
        playerToParty.remove(playerUUID);
        boolean empty = party.removeMember(playerUUID);
        if (empty) {
            parties.remove(party.getPartyId());
        }
        return true;
    }

    /** 0=success, 1=not leader, 2=not in party, 3=can't kick self, 4=no party */
    public static int kick(UUID leaderUUID, UUID targetUUID) {
        Party party = getParty(leaderUUID);
        if (party == null) return 4;
        if (!party.isLeader(leaderUUID)) return 1;
        if (leaderUUID.equals(targetUUID)) return 3;
        if (!party.isMember(targetUUID)) return 2;
        playerToParty.remove(targetUUID);
        party.removeMember(targetUUID);
        return 0;
    }

    // ======== Lifecycle ========

    public static void clearAll() {
        parties.clear();
        playerToParty.clear();
    }

    /** Clean up pending invites on disconnect; player stays in party. */
    public static void onPlayerDisconnect(UUID playerUUID) {
        for (Party party : parties.values()) {
            party.removeInvite(playerUUID);
        }
    }
}
