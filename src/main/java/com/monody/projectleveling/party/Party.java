package com.monody.projectleveling.party;

import java.util.*;

public class Party {
    public static final int MAX_MEMBERS = 8;

    private final UUID partyId;
    private UUID leaderUUID;
    private final Set<UUID> members = new LinkedHashSet<>();
    private final Set<UUID> pendingInvites = new HashSet<>();

    public Party(UUID leaderUUID) {
        this.partyId = UUID.randomUUID();
        this.leaderUUID = leaderUUID;
        this.members.add(leaderUUID);
    }

    public UUID getPartyId()             { return partyId; }
    public UUID getLeaderUUID()          { return leaderUUID; }
    public Set<UUID> getMembers()        { return Collections.unmodifiableSet(members); }
    public int size()                    { return members.size(); }
    public boolean isFull()              { return members.size() >= MAX_MEMBERS; }
    public boolean isMember(UUID uuid)   { return members.contains(uuid); }
    public boolean isLeader(UUID uuid)   { return leaderUUID.equals(uuid); }

    boolean addMember(UUID uuid) {
        if (isFull()) return false;
        return members.add(uuid);
    }

    /** @return true if party is now empty and should be removed */
    boolean removeMember(UUID uuid) {
        members.remove(uuid);
        pendingInvites.remove(uuid);
        if (leaderUUID.equals(uuid) && !members.isEmpty()) {
            leaderUUID = members.iterator().next();
        }
        return members.isEmpty();
    }

    void addInvite(UUID uuid)    { pendingInvites.add(uuid); }
    void removeInvite(UUID uuid) { pendingInvites.remove(uuid); }
    boolean hasInvite(UUID uuid) { return pendingInvites.contains(uuid); }
}
