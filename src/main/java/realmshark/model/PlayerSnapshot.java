package realmshark.model;

/**
 * Immutable view of the tracked player that can safely be shared with the UI layer.
 */
public class PlayerSnapshot {
    public final int objectId;
    public final String accountId;
    public final String name;
    public final String guild;
    public final String playerClass;
    public final int level;
    public final int currentHp;
    public final int maxHp;
    public final long totalDamage;
    public final long totalHealing;
    public final double dps;
    public final double hps;
    public final boolean tracked;
    public final boolean active;
    public final long lastSeen;
    public final int objectType;

    public PlayerSnapshot(int objectId,
                          String accountId,
                          String name,
                          String guild,
                          String playerClass,
                          int level,
                          int currentHp,
                          int maxHp,
                          long totalDamage,
                          long totalHealing,
                          double dps,
                          double hps,
                          boolean tracked,
                          boolean active,
                          long lastSeen,
                          int objectType) {
        this.objectId = objectId;
        this.accountId = accountId;
        this.name = name;
        this.guild = guild;
        this.playerClass = playerClass;
        this.level = level;
        this.currentHp = currentHp;
        this.maxHp = maxHp;
        this.totalDamage = totalDamage;
        this.totalHealing = totalHealing;
        this.dps = dps;
        this.hps = hps;
        this.tracked = tracked;
        this.active = active;
        this.lastSeen = lastSeen;
        this.objectType = objectType;
    }
}
