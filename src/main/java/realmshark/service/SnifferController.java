package realmshark.service;

import packets.Packet;
import packets.PacketType;
import packets.incoming.DamagePacket;
import packets.incoming.NewTickPacket;
import packets.incoming.UpdatePacket;
import packets.packetcapture.PacketProcessor;
import packets.packetcapture.register.IPacketListener;
import packets.packetcapture.register.Register;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Coordinates the low level packet processor with the high level player tracking service.
 */
public class SnifferController {

    private final PlayerTracker playerTracker = new PlayerTracker();
    private PacketProcessor processor;
    private boolean running;
    private boolean listenersRegistered;
    private final List<Consumer<Boolean>> statusListeners = new CopyOnWriteArrayList<>();

    private final IPacketListener<Packet> updateListener = packet ->
            playerTracker.handleUpdate((UpdatePacket) packet);
    private final IPacketListener<Packet> tickListener = packet ->
            playerTracker.handleNewTick((NewTickPacket) packet);
    private final IPacketListener<Packet> damageListener = packet ->
            playerTracker.handleDamage((DamagePacket) packet);

    public SnifferController() {
    }

    public PlayerTracker getPlayerTracker() {
        return playerTracker;
    }

    public synchronized void startSniffer() {
        if (running) {
            return;
        }
        registerListeners();
        processor = new PacketProcessor();
        processor.setName("RealmShark-Sniffer");
        processor.start();
        running = true;
        notifyStatus(true);
    }

    public synchronized void stopSniffer() {
        if (!running) {
            return;
        }
        try {
            if (processor != null) {
                processor.stopSniffer();
                processor.closeSniffer();
            }
        } finally {
            running = false;
            processor = null;
            notifyStatus(false);
        }
    }

    public synchronized void shutdown() {
        stopSniffer();
        unregisterListeners();
    }

    private void registerListeners() {
        if (listenersRegistered) {
            return;
        }
        Register.INSTANCE.register(PacketType.UPDATE, updateListener);
        Register.INSTANCE.register(PacketType.NEWTICK, tickListener);
        Register.INSTANCE.register(PacketType.DAMAGE, damageListener);
        listenersRegistered = true;
    }

    private void unregisterListeners() {
        if (!listenersRegistered) {
            return;
        }
        Register.INSTANCE.unregister(PacketType.UPDATE, updateListener);
        Register.INSTANCE.unregister(PacketType.NEWTICK, tickListener);
        Register.INSTANCE.unregister(PacketType.DAMAGE, damageListener);
        listenersRegistered = false;
    }

    public void addStatusListener(Consumer<Boolean> listener) {
        statusListeners.add(listener);
    }

    public void removeStatusListener(Consumer<Boolean> listener) {
        statusListeners.remove(listener);
    }

    private void notifyStatus(boolean running) {
        for (Consumer<Boolean> listener : statusListeners) {
            listener.accept(running);
        }
    }

    public boolean isRunning() {
        return running;
    }
}
