// Entry point — starts the login screen.
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // "Metal" look — standard Java buttons show text clearly on Windows.
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception e) {
                System.out.println("Using default look and feel.");
            }
            new LoginPage();
        });
    }
}
