package realmshark.ui;

import realmshark.model.PlayerSnapshot;
import realmshark.service.PlayerTracker;
import realmshark.service.SnifferController;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * Root application frame that composes the refreshed RealmShark UI.
 */
public class RealmSharkFrame extends JFrame {

    private final SnifferController controller;
    private final PlayerDashboardPanel dashboardPanel;
    private final PlayerListPanel listPanel;
    private final JLabel statusLabel;
    private final Timer refreshTimer;

    public RealmSharkFrame(SnifferController controller) {
        super("RealmShark – Friends & Guild Dashboard");
        this.controller = controller;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(1200, 720));
        setMinimumSize(new Dimension(960, 600));

        JPanel header = buildHeader();
        add(header, BorderLayout.NORTH);

        dashboardPanel = new PlayerDashboardPanel();
        add(dashboardPanel, BorderLayout.CENTER);

        listPanel = new PlayerListPanel(controller);
        add(listPanel, BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);

        controller.addStatusListener(this::updateStatus);

        refreshTimer = new Timer(750, e -> refreshData());
        refreshTimer.setRepeats(true);
        refreshTimer.start();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                controller.shutdown();
                refreshTimer.stop();
            }
        });
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(new EmptyBorder(16, 20, 16, 20));
        header.setBackground(new Color(24, 24, 26));

        JLabel title = new JLabel("RealmShark", SwingConstants.LEFT);
        title.setForeground(new Color(224, 225, 228));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        header.add(title, BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        controls.setOpaque(false);

        JButton startButton = new JButton("Start Sniffer");
        startButton.addActionListener(e -> controller.startSniffer());
        JButton stopButton = new JButton("Stop");
        stopButton.addActionListener(e -> controller.stopSniffer());

        styleButton(startButton, new Color(76, 175, 80));
        styleButton(stopButton, new Color(229, 115, 115));

        controls.add(startButton);
        controls.add(stopButton);

        statusLabel = new JLabel("Sniffer stopped", SwingConstants.RIGHT);
        statusLabel.setForeground(new Color(200, 200, 200));
        statusLabel.setBorder(new EmptyBorder(0, 16, 0, 0));
        controls.add(statusLabel);

        header.add(controls, BorderLayout.EAST);
        return header;
    }

    private void styleButton(JButton button, Color background) {
        button.setFocusPainted(false);
        button.setForeground(Color.WHITE);
        button.setBackground(background);
        button.setOpaque(true);
        button.setBorder(new EmptyBorder(8, 16, 8, 16));
    }

    private void refreshData() {
        PlayerTracker tracker = controller.getPlayerTracker();
        List<PlayerSnapshot> trackedPlayers = tracker.getTrackedPlayers();
        dashboardPanel.updatePlayers(trackedPlayers);
        listPanel.setPlayers(tracker.getAllPlayers());
    }

    private void updateStatus(boolean running) {
        SwingUtilities.invokeLater(() -> {
            if (running) {
                statusLabel.setText("Sniffer running");
                statusLabel.setForeground(new Color(129, 199, 132));
            } else {
                statusLabel.setText("Sniffer stopped");
                statusLabel.setForeground(new Color(239, 154, 154));
            }
        });
    }
}
