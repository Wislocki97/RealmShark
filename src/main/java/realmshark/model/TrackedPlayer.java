package realmshark.model;

import packets.data.StatData;
import packets.data.enums.StatType;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Runtime model representing a single player in the sniffer session.
 */
public class TrackedPlayer {

    private static final long WINDOW_MS = 10_000L;

    private final int objectId;
    private String accountId = "";
    private String name = "Unknown";
    private String guild = "";
    private String playerClass = "";
    private int level = 0;
    private int currentHp = -1;
    private int maxHp = -1;
    private boolean active = true;
    private long lastSeen = System.currentTimeMillis();
    private int objectType;

    private final Deque<Sample> damageSamples = new ArrayDeque<>();
    private final Deque<Sample> healingSamples = new ArrayDeque<>();
    private long totalDamage;
    private long totalHealing;
    private int rollingDamage;
    private int rollingHealing;

    public TrackedPlayer(int objectId) {
        this.objectId = objectId;
    }

    public int getObjectId() {
        return objectId;
    }

    public synchronized String getName() {
        return name;
    }

    public synchronized boolean isActive() {
        return active;
    }

    public synchronized void markInactive() {
        this.active = false;
    }

    public synchronized void updateFromStats(StatData[] stats) {
        long now = System.currentTimeMillis();
        lastSeen = now;
        active = true;

        for (StatData stat : stats) {
            StatType type = stat.statType;
            if (type == null) {
                continue;
            }
            switch (type) {
                case NAME_STAT:
                    if (stat.stringStatValue != null && !stat.stringStatValue.isEmpty()) {
                        name = stat.stringStatValue;
                    }
                    break;
                case GUILD_NAME_STAT:
                    guild = stat.stringStatValue == null ? "" : stat.stringStatValue;
                    break;
                case ACCOUNT_ID_STAT:
                    accountId = stat.stringStatValue == null ? accountId : stat.stringStatValue;
                    break;
                case HP_STAT:
                    handleHp(stat.statValue, now);
                    break;
                case MAX_HP_STAT:
                    maxHp = stat.statValue;
                    break;
                case LEVEL_STAT:
                    level = stat.statValue;
                    break;
                case SKIN_ID:
                    // fallthrough intended. The skin determines the class name; we expect objectType to be provided elsewhere.
                    break;
                default:
                    break;
            }
        }
    }

    public synchronized void setObjectType(int objectType, String resolvedName) {
        this.objectType = objectType;
        if (resolvedName != null && !resolvedName.isEmpty()) {
            this.playerClass = resolvedName;
        }
    }

    private void handleHp(int newHp, long now) {
        if (currentHp >= 0 && newHp > currentHp) {
            recordHealing(newHp - currentHp, now);
        }
        currentHp = newHp;
    }

    public synchronized void recordDamage(int amount) {
        if (amount <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        damageSamples.addLast(new Sample(now, amount));
        rollingDamage += amount;
        totalDamage += amount;
        prune(damageSamples, now, true);
        lastSeen = now;
    }

    private void recordHealing(int amount, long now) {
        if (amount <= 0) {
            return;
        }
        healingSamples.addLast(new Sample(now, amount));
        rollingHealing += amount;
        totalHealing += amount;
        prune(healingSamples, now, false);
    }

    private void prune(Deque<Sample> samples, long now, boolean damageQueue) {
        while (!samples.isEmpty() && now - samples.peekFirst().timestamp > WINDOW_MS) {
            Sample removed = samples.removeFirst();
            if (damageQueue) {
                rollingDamage -= removed.amount;
            } else {
                rollingHealing -= removed.amount;
            }
        }
    }

    public synchronized double getDps() {
        long now = System.currentTimeMillis();
        prune(damageSamples, now, true);
        if (damageSamples.isEmpty()) {
            return 0d;
        }
        long window = Math.max(1000L, Math.min(WINDOW_MS, now - damageSamples.peekFirst().timestamp + 1));
        return rollingDamage * 1000d / window;
    }

    public synchronized double getHps() {
        long now = System.currentTimeMillis();
        prune(healingSamples, now, false);
        if (healingSamples.isEmpty()) {
            return 0d;
        }
        long window = Math.max(1000L, Math.min(WINDOW_MS, now - healingSamples.peekFirst().timestamp + 1));
        return rollingHealing * 1000d / window;
    }

    public synchronized PlayerSnapshot toSnapshot(boolean tracked) {
        long now = System.currentTimeMillis();
        prune(damageSamples, now, true);
        prune(healingSamples, now, false);
        return new PlayerSnapshot(
                objectId,
                accountId,
                name,
                guild,
                playerClass,
                level,
                currentHp,
                maxHp,
                totalDamage,
                totalHealing,
                getDps(),
                getHps(),
                tracked,
                active,
                lastSeen,
                objectType
        );
    }

    public synchronized void markSeen() {
        lastSeen = System.currentTimeMillis();
    }

    private static class Sample {
        private final long timestamp;
        private final int amount;

        private Sample(long timestamp, int amount) {
            this.timestamp = timestamp;
            this.amount = amount;
        }
    }
}
