package realmshark.ui;

import realmshark.model.PlayerSnapshot;
import realmshark.ui.components.PlayerCard;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Grid based container that showcases the currently tracked players.
 */
public class PlayerDashboardPanel extends JPanel {

    private static final int MAX_PLAYERS = 4;

    private final List<PlayerCard> playerCards = new ArrayList<>();
    private final JLabel emptyState;

    public PlayerDashboardPanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(16, 16, 16, 12));
        setBackground(new Color(30, 31, 34));

        JPanel grid = new JPanel(new GridLayout(2, 2, 16, 16));
        grid.setOpaque(false);

        for (int i = 0; i < MAX_PLAYERS; i++) {
            PlayerCard card = new PlayerCard();
            card.update(null);
            playerCards.add(card);
            grid.add(card);
        }

        emptyState = new JLabel("No players tracked yet", SwingConstants.CENTER);
        emptyState.setFont(emptyState.getFont().deriveFont(Font.PLAIN, 18f));
        emptyState.setForeground(new Color(180, 189, 198));
        emptyState.setBorder(new EmptyBorder(32, 0, 32, 0));

        add(emptyState, BorderLayout.NORTH);
        add(grid, BorderLayout.CENTER);
        toggleEmptyState(true);
    }

    public void updatePlayers(List<PlayerSnapshot> players) {
        int count = players == null ? 0 : players.size();
        toggleEmptyState(count == 0);
        for (int i = 0; i < playerCards.size(); i++) {
            PlayerCard card = playerCards.get(i);
            if (players != null && i < players.size()) {
                card.update(players.get(i));
            } else {
                card.update(null);
            }
        }
    }

    private void toggleEmptyState(boolean show) {
        emptyState.setVisible(show);
    }
}
