package principal;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import service.EstacionamientoService;
import ui.FrmEstacionamiento;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            EstacionamientoService servicio = new EstacionamientoService(15); // 15 espacios iniciales
            FrmEstacionamiento ventana = new FrmEstacionamiento(servicio);
            ventana.setVisible(true);
        });
    }
}