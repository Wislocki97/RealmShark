package realmshark;

import realmshark.service.SnifferController;
import realmshark.ui.RealmSharkFrame;

import javax.swing.SwingUtilities;

/**
 * Application entry point that boots the modern RealmShark UI.
 */
public class RealmShark {

    public static void main(String[] args) {
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            SnifferController controller = new SnifferController();
            RealmSharkFrame frame = new RealmSharkFrame(controller);
            frame.setVisible(true);
        });
    }
}
