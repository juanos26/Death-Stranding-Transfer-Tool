import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Swing UI work should start on the Event Dispatch Thread.
        SwingUtilities.invokeLater(TransferToolUI::new);
    }
}
