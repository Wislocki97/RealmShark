package realmshark.ui.components;

import realmshark.model.PlayerSnapshot;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.DecimalFormat;

/**
 * Compact UI card that visualises a single tracked player's performance metrics.
 */
public class PlayerCard extends JPanel {

    private static final DecimalFormat RATE_FORMAT = new DecimalFormat("#,##0.0");
    private static final DecimalFormat TOTAL_FORMAT = new DecimalFormat("#,##0");

    private final JLabel nameLabel = new JLabel("--", SwingConstants.LEFT);
    private final JLabel guildLabel = new JLabel("", SwingConstants.LEFT);
    private final JLabel classLabel = new JLabel("", SwingConstants.LEFT);
    private final JLabel dpsLabel = new JLabel("DPS: 0", SwingConstants.LEFT);
    private final JLabel hpsLabel = new JLabel("HPS: 0", SwingConstants.LEFT);
    private final JLabel totalDamageLabel = new JLabel("Total Damage: 0", SwingConstants.LEFT);
    private final JLabel totalHealingLabel = new JLabel("Total Healing: 0", SwingConstants.LEFT);
    private final JLabel statusLabel = new JLabel("Inactive", SwingConstants.LEFT);
    private final JProgressBar hpBar = new JProgressBar();

    public PlayerCard() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 63, 65)),
                new EmptyBorder(12, 16, 12, 16)));
        setBackground(new Color(43, 45, 48));

        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 18f));
        nameLabel.setForeground(Color.WHITE);
        guildLabel.setForeground(new Color(173, 186, 199));
        classLabel.setForeground(new Color(173, 186, 199));
        statusLabel.setForeground(new Color(120, 200, 120));

        JPanel header = new JPanel(new GridLayout(0, 1));
        header.setOpaque(false);
        header.add(nameLabel);
        header.add(guildLabel);
        header.add(classLabel);
        add(header, BorderLayout.NORTH);

        hpBar.setStringPainted(true);
        hpBar.setMinimum(0);
        hpBar.setMaximum(1);
        hpBar.setValue(0);
        hpBar.setForeground(new Color(94, 167, 255));
        add(hpBar, BorderLayout.CENTER);

        JPanel metrics = new JPanel(new GridLayout(0, 1, 0, 4));
        metrics.setOpaque(false);
        dpsLabel.setForeground(Color.WHITE);
        hpsLabel.setForeground(Color.WHITE);
        totalDamageLabel.setForeground(new Color(200, 200, 200));
        totalHealingLabel.setForeground(new Color(200, 200, 200));
        statusLabel.setForeground(new Color(173, 186, 199));
        metrics.add(dpsLabel);
        metrics.add(hpsLabel);
        metrics.add(totalDamageLabel);
        metrics.add(totalHealingLabel);
        metrics.add(statusLabel);
        add(metrics, BorderLayout.SOUTH);
    }

    public void update(PlayerSnapshot snapshot) {
        if (snapshot == null) {
            renderPlaceholder();
            return;
        }
        String displayName = snapshot.name == null ? "Unknown" : snapshot.name;
        nameLabel.setText(displayName + (snapshot.level > 0 ? "  •  Lv." + snapshot.level : ""));
        guildLabel.setText(snapshot.guild == null || snapshot.guild.isEmpty() ? "" : snapshot.guild);
        classLabel.setText(snapshot.playerClass == null || snapshot.playerClass.isEmpty()
                ? "Object #" + snapshot.objectType
                : snapshot.playerClass);
        statusLabel.setText(snapshot.active ? "Active" : "Last seen " + formatLastSeen(snapshot.lastSeen));
        statusLabel.setForeground(snapshot.active ? new Color(140, 220, 140) : new Color(200, 170, 120));

        hpBar.setMaximum(Math.max(snapshot.maxHp, 1));
        hpBar.setValue(Math.max(snapshot.currentHp, 0));
        hpBar.setString(formatHp(snapshot.currentHp, snapshot.maxHp));

        dpsLabel.setText("DPS: " + formatRate(snapshot.dps));
        hpsLabel.setText("HPS: " + formatRate(snapshot.hps));
        totalDamageLabel.setText("Total Damage: " + formatTotal(snapshot.totalDamage));
        totalHealingLabel.setText("Total Healing: " + formatTotal(snapshot.totalHealing));
    }

    private void renderPlaceholder() {
        nameLabel.setText("No player selected");
        guildLabel.setText("Choose a player from the roster to begin tracking.");
        classLabel.setText("");
        statusLabel.setText("Idle");
        statusLabel.setForeground(new Color(173, 186, 199));
        hpBar.setMaximum(1);
        hpBar.setValue(0);
        hpBar.setString("0 / 0");
        dpsLabel.setText("DPS: 0");
        hpsLabel.setText("HPS: 0");
        totalDamageLabel.setText("Total Damage: 0");
        totalHealingLabel.setText("Total Healing: 0");
    }

    private String formatHp(int hp, int maxHp) {
        if (hp < 0 || maxHp <= 0) {
            return "--";
        }
        return hp + " / " + maxHp;
    }

    private String formatRate(double rate) {
        if (Double.isNaN(rate) || Double.isInfinite(rate)) {
            return "0";
        }
        if (rate >= 1000) {
            return RATE_FORMAT.format(rate / 1000d) + "k";
        }
        return RATE_FORMAT.format(rate);
    }

    private String formatTotal(long value) {
        if (value <= 0) {
            return "0";
        }
        return TOTAL_FORMAT.format(value);
    }

    private String formatLastSeen(long lastSeen) {
        long diff = Math.max(0, System.currentTimeMillis() - lastSeen);
        long seconds = diff / 1000L;
        if (seconds < 5) {
            return "just now";
        }
        if (seconds < 60) {
            return seconds + "s ago";
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "m ago";
        }
        long hours = minutes / 60;
        return hours + "h ago";
    }
}
