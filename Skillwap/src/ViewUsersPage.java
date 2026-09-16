// Shows other users in a table (simple SELECT query).
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ViewUsersPage extends JFrame {

    private final int userId;
    private final String userName;

    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField searchField;

    public ViewUsersPage(int userId, String userName) {
        this.userId = userId;
        this.userName = userName;

        setTitle("SkillSwap - All Users");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        buildUI();
        loadUsers("");
        setVisible(true);
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JPanel north = new JPanel(new BorderLayout());
        JLabel title = new JLabel("All Users (except you)", SwingConstants.CENTER);
        title.setFont(new Font("Dialog", Font.BOLD, 16));
        north.add(title, BorderLayout.NORTH);

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.add(new JLabel("Search by name:"));
        searchField = new JTextField(15);
        topBar.add(searchField);
        JButton searchBtn = new JButton("Search");
        JButton refreshBtn = new JButton("Refresh");
        topBar.add(searchBtn);
        topBar.add(refreshBtn);
        north.add(topBar, BorderLayout.SOUTH);
        main.add(north, BorderLayout.NORTH);

        String[] cols = {"User ID", "Name", "City"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        table = new JTable(tableModel);
        main.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        JButton backBtn = new JButton("Back to Dashboard");
        bottom.add(backBtn);
        main.add(bottom, BorderLayout.SOUTH);

        add(main);

        searchBtn.addActionListener(e -> loadUsers(searchField.getText().trim()));
        refreshBtn.addActionListener(e -> {
            searchField.setText("");
            loadUsers("");
        });
        backBtn.addActionListener(e -> dispose());
        searchField.addActionListener(e -> loadUsers(searchField.getText().trim()));
    }

    private void loadUsers(String keyword) {
        tableModel.setRowCount(0);
        String like = "%" + keyword + "%";

        String sql = "SELECT user_id, name, city FROM USERS "
                + "WHERE user_id <> ? AND name LIKE ? ORDER BY name";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, like);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("user_id"),
                        rs.getString("name"),
                        rs.getString("city")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }
}
