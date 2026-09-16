// Main menu after login — opens other screens.
import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class Dashboard extends JFrame {

    private final int userId;
    private final String userName;

    public Dashboard(int userId, String userName) {
        this.userId = userId;
        this.userName = userName;

        setTitle("SkillSwap - Dashboard");
        setSize(450, 420);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        buildUI();
        setVisible(true);
    }

    private void buildUI() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel welcome = new JLabel("Welcome, " + userName + " (ID: " + userId + ")",
                SwingConstants.CENTER);
        welcome.setFont(new Font("Dialog", Font.BOLD, 16));
        panel.add(welcome, BorderLayout.NORTH);

        JLabel stats = new JLabel(getSkillCounts(), SwingConstants.CENTER);
        panel.add(stats, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(6, 1, 8, 8));

        JButton addSkillBtn = new JButton("Add Skills I Have");
        JButton addWantBtn = new JButton("Add Skills I Want");
        JButton viewUsersBtn = new JButton("View All Users");
        JButton sendReqBtn = new JButton("Send Exchange Request");
        JButton viewReqBtn = new JButton("View My Requests");
        JButton logoutBtn = new JButton("Logout");

        buttons.add(addSkillBtn);
        buttons.add(addWantBtn);
        buttons.add(viewUsersBtn);
        buttons.add(sendReqBtn);
        buttons.add(viewReqBtn);
        buttons.add(logoutBtn);

        panel.add(buttons, BorderLayout.SOUTH);
        add(panel);

        addSkillBtn.addActionListener(e -> new AddSkillPage(userId, userName));
        addWantBtn.addActionListener(e -> new AddWantPage(userId, userName));
        viewUsersBtn.addActionListener(e -> new ViewUsersPage(userId, userName));
        sendReqBtn.addActionListener(e -> new RequestPage(userId, userName));
        viewReqBtn.addActionListener(e -> new ViewRequestsPage(userId, userName));
        logoutBtn.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "Logout?", "Confirm",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                dispose();
                new LoginPage();
            }
        });
    }

    private String getSkillCounts() {
        int offered = 0;
        int wanted = 0;
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            return "";
        }
        try {
            PreparedStatement ps1 = conn.prepareStatement(
                    "SELECT COUNT(*) FROM USER_SKILLS WHERE user_id = ?");
            ps1.setInt(1, userId);
            ResultSet rs1 = ps1.executeQuery();
            if (rs1.next()) {
                offered = rs1.getInt(1);
            }

            PreparedStatement ps2 = conn.prepareStatement(
                    "SELECT COUNT(*) FROM USER_WANTS WHERE user_id = ?");
            ps2.setInt(1, userId);
            ResultSet rs2 = ps2.executeQuery();
            if (rs2.next()) {
                wanted = rs2.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Skills offered: " + offered + "  |  Skills wanted: " + wanted;
    }
}
