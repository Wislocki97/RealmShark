package realmshark.service;

import assets.IdToAsset;
import packets.data.ObjectData;
import packets.data.ObjectStatusData;
import packets.data.StatData;
import packets.data.enums.StatType;
import packets.incoming.DamagePacket;
import packets.incoming.NewTickPacket;
import packets.incoming.UpdatePacket;
import realmshark.model.PlayerSnapshot;
import realmshark.model.TrackedPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Aggregates player state from incoming packets and exposes immutable snapshots for the UI.
 */
public class PlayerTracker {

    private final Map<Integer, TrackedPlayer> playersByObjectId = new HashMap<>();
    private final Map<String, Integer> nameToObjectId = new HashMap<>();
    private final Set<Integer> trackedObjectIds = new LinkedHashSet<>();

    public synchronized void handleUpdate(UpdatePacket packet) {
        for (ObjectData data : packet.newObjects) {
            if (data == null || data.status == null) {
                continue;
            }
            if (!isPlayer(data.status.stats)) {
                continue;
            }
            TrackedPlayer player = playersByObjectId.computeIfAbsent(data.status.objectId, TrackedPlayer::new);
            player.setObjectType(data.objectType, IdToAsset.objectName(data.objectType));
            player.updateFromStats(data.status.stats);
            String currentName = player.getName();
            if (currentName != null) {
                nameToObjectId.put(currentName.toLowerCase(), data.status.objectId);
            }
        }

        for (int drop : packet.drops) {
            TrackedPlayer removed = playersByObjectId.get(drop);
            if (removed != null) {
                removed.markInactive();
            }
        }
    }

    public synchronized void handleNewTick(NewTickPacket packet) {
        for (ObjectStatusData status : packet.status) {
            TrackedPlayer player = playersByObjectId.get(status.objectId);
            if (player == null) {
                if (!isPlayer(status.stats)) {
                    continue;
                }
                player = new TrackedPlayer(status.objectId);
                playersByObjectId.put(status.objectId, player);
            }
            player.updateFromStats(status.stats);
        }
    }

    public synchronized void handleDamage(DamagePacket packet) {
        TrackedPlayer attacker = playersByObjectId.get(packet.objectId);
        if (attacker != null) {
            attacker.recordDamage(packet.damageAmount);
        }
        // Track healing via HP updates in the tick stream.
    }

    public synchronized void markTracked(int objectId, boolean tracked) {
        if (tracked) {
            trackedObjectIds.add(objectId);
        } else {
            trackedObjectIds.remove(objectId);
        }
    }

    public synchronized void markTrackedByName(String name, boolean tracked) {
        if (name == null) {
            return;
        }
        Integer objectId = nameToObjectId.get(name.toLowerCase());
        if (objectId != null) {
            markTracked(objectId, tracked);
        }
    }

    public synchronized List<PlayerSnapshot> getTrackedPlayers() {
        return trackedObjectIds.stream()
                .map(playersByObjectId::get)
                .filter(p -> p != null)
                .map(p -> p.toSnapshot(true))
                .sorted(Comparator.comparingLong((PlayerSnapshot s) -> s.lastSeen).reversed())
                .collect(Collectors.toList());
    }

    public synchronized List<PlayerSnapshot> getAllPlayers() {
        List<PlayerSnapshot> snapshots = new ArrayList<>();
        for (TrackedPlayer player : playersByObjectId.values()) {
            boolean tracked = trackedObjectIds.contains(player.getObjectId());
            snapshots.add(player.toSnapshot(tracked));
        }
        snapshots.sort(Comparator.comparing((PlayerSnapshot s) -> !s.active)
                .thenComparing((PlayerSnapshot s) -> !s.tracked)
                .thenComparing((PlayerSnapshot s) -> s.name == null ? "" : s.name.toLowerCase()));
        return snapshots;
    }

    public synchronized void clearInactive() {
        playersByObjectId.values().removeIf(player -> !player.isActive());
        trackedObjectIds.removeIf(id -> !playersByObjectId.containsKey(id));
        rebuildNameIndex();
    }

    public synchronized void reset() {
        playersByObjectId.clear();
        trackedObjectIds.clear();
        nameToObjectId.clear();
    }

    private boolean isPlayer(StatData[] stats) {
        if (stats == null) {
            return false;
        }
        for (StatData stat : stats) {
            if (stat.statType == StatType.ACCOUNT_ID_STAT) {
                return true;
            }
        }
        return false;
    }

    private void rebuildNameIndex() {
        nameToObjectId.clear();
        for (TrackedPlayer player : playersByObjectId.values()) {
            String name = player.getName();
            if (name != null) {
                nameToObjectId.put(name.toLowerCase(), player.getObjectId());
            }
        }
    }
}
