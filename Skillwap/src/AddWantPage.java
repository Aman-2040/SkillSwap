// Add skills the user wants to learn (USER_WANTS table).

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class AddWantPage extends JFrame {

    private final int    userId;
    private final String userName;

    private JTextField     skillField;
    private JButton        addBtn;
    private JButton        removeBtn;
    private JButton        backBtn;
    private JTable         table;
    private DefaultTableModel tableModel;

    public AddWantPage(int userId, String userName) {
        this.userId   = userId;
        this.userName = userName;

        setTitle("SkillSwap – Skills I Want to Learn  [" + userName + "]");
        setSize(520, 480);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        buildUI();
        loadWantedSkills();
        setVisible(true);
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        main.setBackground(new Color(252, 243, 255));

        // Title
        JLabel title = new JLabel("Skills I Want to Learn", SwingConstants.CENTER);
        title.setFont(new Font("Dialog", Font.BOLD, 16));
        main.add(title, BorderLayout.NORTH);

        // Input row
        JPanel centre = new JPanel(new BorderLayout(8, 8));
        centre.setOpaque(false);

        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        inputRow.setOpaque(false);
        inputRow.add(new JLabel("Skill Name:"));
        skillField = new JTextField(20);
        skillField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        inputRow.add(skillField);
        addBtn = new JButton("Add Want");
        inputRow.add(addBtn);
        centre.add(inputRow, BorderLayout.NORTH);

        // Table
        String[] cols = {"Skill ID", "Skill Name"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(24);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createTitledBorder("Skills I Want to Learn"));
        centre.add(scroll, BorderLayout.CENTER);

        main.add(centre, BorderLayout.CENTER);

        // Bottom buttons
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        bottom.setOpaque(false);
        removeBtn = new JButton("Remove Selected");
        backBtn   = new JButton("Back to Dashboard");
        bottom.add(removeBtn);
        bottom.add(backBtn);
        main.add(bottom, BorderLayout.SOUTH);

        add(main);

        addBtn.addActionListener(e -> addWantedSkill());
        removeBtn.addActionListener(e -> removeWantedSkill());
        backBtn.addActionListener(e -> dispose());
        skillField.addActionListener(e -> addWantedSkill());
    }

    // ── Load skills user wants ─────────────────────────────────
    /**
     * SELECT s.skill_id, s.skill_name
     * FROM   USER_WANTS uw
     * JOIN   SKILLS s ON uw.skill_id = s.skill_id
     * WHERE  uw.user_id = ?
     * ORDER  BY s.skill_name
     */
    private void loadWantedSkills() {
        tableModel.setRowCount(0);

        String sql = "SELECT s.skill_id, s.skill_name "
                   + "FROM USER_WANTS uw "
                   + "JOIN SKILLS s ON uw.skill_id = s.skill_id "
                   + "WHERE uw.user_id = ? "
                   + "ORDER BY s.skill_name";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("skill_id"),
                    rs.getString("skill_name")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // ── Add wanted skill ──────────────────────────────────────
    /**
     * Same getOrCreateSkill pattern as AddSkillPage,
     * but INSERT IGNORE into USER_WANTS.
     */
    private void addWantedSkill() {
        String skillName = skillField.getText().trim();
        if (skillName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a skill name.");
            return;
        }

        Connection conn = DBConnection.getConnection();
        if (conn == null) return;

        try {
            int skillId = getOrCreateSkill(conn, skillName);

            String sql = "INSERT IGNORE INTO USER_WANTS (user_id, skill_id) VALUES (?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setInt(2, skillId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                JOptionPane.showMessageDialog(this,
                    "'" + skillName + "' added to your wishlist! 🎯");
            } else {
                JOptionPane.showMessageDialog(this,
                    "'" + skillName + "' is already in your wishlist.");
            }

            skillField.setText("");
            loadWantedSkills();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Remove wanted skill ───────────────────────────────────
    /**
     * DELETE FROM USER_WANTS WHERE user_id = ? AND skill_id = ?
     */
    private void removeWantedSkill() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a skill to remove.");
            return;
        }

        int    skillId   = (int)    tableModel.getValueAt(row, 0);
        String skillName = (String) tableModel.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Remove '" + skillName + "' from your wishlist?", "Confirm",
            JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM USER_WANTS WHERE user_id = ? AND skill_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, skillId);
            ps.executeUpdate();
            loadWantedSkills();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // ── Helper ────────────────────────────────────────────────
    private int getOrCreateSkill(Connection conn, String skillName) throws SQLException {
        String findSql = "SELECT skill_id FROM SKILLS WHERE skill_name = ?";
        PreparedStatement ps = conn.prepareStatement(findSql);
        ps.setString(1, skillName);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt("skill_id");

        String insertSql = "INSERT INTO SKILLS (skill_name) VALUES (?)";
        PreparedStatement ps2 = conn.prepareStatement(
            insertSql, Statement.RETURN_GENERATED_KEYS);
        ps2.setString(1, skillName);
        ps2.executeUpdate();
        ResultSet keys = ps2.getGeneratedKeys();
        keys.next();
        return keys.getInt(1);
    }

}
