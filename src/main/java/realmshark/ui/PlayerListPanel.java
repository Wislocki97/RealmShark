package realmshark.ui;

import realmshark.model.PlayerSnapshot;
import realmshark.service.SnifferController;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Sidebar displaying the available players and offering quick tracking toggles.
 */
public class PlayerListPanel extends JPanel {

    private final PlayerTableModel tableModel;
    private final JTable table;
    private final TableRowSorter<PlayerTableModel> sorter;
    private final JTextField filterField;
    private final SnifferController controller;

    public PlayerListPanel(SnifferController controller) {
        this.controller = controller;
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(16, 12, 16, 16));
        setBackground(new Color(26, 27, 30));
        setPreferredSize(new Dimension(340, 0));

        JLabel title = new JLabel("Guild & Friends", SwingConstants.LEFT);
        title.setForeground(new Color(215, 218, 224));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setBorder(new EmptyBorder(0, 0, 12, 0));
        add(title, BorderLayout.NORTH);

        tableModel = new PlayerTableModel();
        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setRowHeight(28);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setBackground(new Color(37, 38, 41));
        table.setForeground(new Color(220, 220, 220));
        table.setOpaque(true);
        table.getTableHeader().setBackground(new Color(45, 47, 50));
        table.getTableHeader().setForeground(new Color(200, 200, 200));
        table.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(3).setCellRenderer(center);
        table.getColumnModel().getColumn(4).setCellRenderer(center);
        table.getColumnModel().getColumn(5).setCellRenderer(center);

        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);

        filterField = new JTextField();
        filterField.setToolTipText("Filter by player or guild name");
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });
        filterField.setBackground(new Color(37, 38, 41));
        filterField.setForeground(new Color(220, 220, 220));
        filterField.setCaretColor(new Color(220, 220, 220));
        filterField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 63, 65)),
                new EmptyBorder(6, 8, 6, 8)));

        JLabel filterLabel = new JLabel("Search", SwingConstants.LEFT);
        filterLabel.setForeground(new Color(160, 165, 172));
        filterLabel.setBorder(new EmptyBorder(12, 0, 6, 0));

        JPanel filterPanel = new JPanel(new BorderLayout());
        filterPanel.setOpaque(false);
        filterPanel.add(filterLabel, BorderLayout.NORTH);
        filterPanel.add(filterField, BorderLayout.CENTER);
        add(filterPanel, BorderLayout.SOUTH);
    }

    public void setPlayers(List<PlayerSnapshot> players) {
        SwingUtilities.invokeLater(() -> {
            tableModel.setPlayers(players);
            applyFilter();
        });
    }

    private void applyFilter() {
        String text = filterField.getText();
        if (text == null || text.trim().isEmpty()) {
            sorter.setRowFilter(null);
            return;
        }
        String lower = text.trim().toLowerCase();
        sorter.setRowFilter(new RowFilter<PlayerTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends PlayerTableModel, ? extends Integer> entry) {
                PlayerSnapshot snapshot = tableModel.getPlayerAt(entry.getModelRow());
                if (snapshot == null) {
                    return true;
                }
                String name = snapshot.name == null ? "" : snapshot.name.toLowerCase();
                String guild = snapshot.guild == null ? "" : snapshot.guild.toLowerCase();
                return name.contains(lower) || guild.contains(lower);
            }
        });
    }

    private class PlayerTableModel extends AbstractTableModel {
        private final String[] columns = {"Track", "Player", "Guild", "DPS", "HPS", "Status"};
        private final DecimalFormat rateFormat = new DecimalFormat("#,##0");
        private List<PlayerSnapshot> players = new ArrayList<>();

        public void setPlayers(List<PlayerSnapshot> players) {
            this.players = players == null ? new ArrayList<>() : new ArrayList<>(players);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return players.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) {
                return Boolean.class;
            }
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            PlayerSnapshot snapshot = players.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return snapshot.tracked;
                case 1:
                    String name = snapshot.name == null ? "Unknown" : snapshot.name;
                    return snapshot.level > 0 ? name + "  (Lv." + snapshot.level + ")" : name;
                case 2:
                    return snapshot.guild == null || snapshot.guild.isEmpty() ? "-" : snapshot.guild;
                case 3:
                    return formatRate(snapshot.dps);
                case 4:
                    return formatRate(snapshot.hps);
                case 5:
                    return snapshot.active ? "Active" : "Idle";
                default:
                    return "";
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex != 0 || rowIndex >= players.size()) {
                return;
            }
            PlayerSnapshot snapshot = players.get(rowIndex);
            boolean track = Boolean.TRUE.equals(aValue);
            controller.getPlayerTracker().markTracked(snapshot.objectId, track);
            setPlayers(controller.getPlayerTracker().getAllPlayers());
        }

        private String formatRate(double value) {
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                return "0";
            }
            return rateFormat.format(value);
        }

        public PlayerSnapshot getPlayerAt(int index) {
            if (index < 0 || index >= players.size()) {
                return null;
            }
            return players.get(index);
        }
    }
}
