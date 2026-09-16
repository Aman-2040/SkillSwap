// Add skills the user can teach (USER_SKILLS + SKILLS tables).

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class AddSkillPage extends JFrame {

    private final int    userId;
    private final String userName;

    private JTextField     skillField;
    private JButton        addBtn;
    private JButton        removeBtn;
    private JButton        backBtn;
    private JTable         table;
    private DefaultTableModel tableModel;

    public AddSkillPage(int userId, String userName) {
        this.userId   = userId;
        this.userName = userName;

        setTitle("SkillSwap – Add Skills I Have  [" + userName + "]");
        setSize(520, 480);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        buildUI();
        loadMySkills();   // Populate table on open
        setVisible(true);
    }

    // ── UI Layout ─────────────────────────────────────────────
    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        main.setBackground(new Color(240, 248, 255));

        // ── Top: title ────────────────────────────────────────
        JLabel title = new JLabel("Skills I Can Offer", SwingConstants.CENTER);
        title.setFont(new Font("Dialog", Font.BOLD, 16));
        main.add(title, BorderLayout.NORTH);

        // ── Centre: input + table ─────────────────────────────
        JPanel centre = new JPanel(new BorderLayout(8, 8));
        centre.setOpaque(false);

        // Input row
        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        inputRow.setOpaque(false);
        inputRow.add(new JLabel("Skill Name:"));
        skillField = new JTextField(20);
        skillField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        inputRow.add(skillField);
        addBtn = new JButton("Add Skill");
        inputRow.add(addBtn);
        centre.add(inputRow, BorderLayout.NORTH);

        // Table of existing skills
        String[] cols = {"Skill ID", "Skill Name"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(24);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createTitledBorder("My Current Skills"));
        centre.add(scroll, BorderLayout.CENTER);

        main.add(centre, BorderLayout.CENTER);

        // ── Bottom: Remove + Back ─────────────────────────────
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        bottom.setOpaque(false);
        removeBtn = new JButton("Remove Selected");
        backBtn   = new JButton("Back to Dashboard");
        bottom.add(removeBtn);
        bottom.add(backBtn);
        main.add(bottom, BorderLayout.SOUTH);

        add(main);

        // ── Listeners ─────────────────────────────────────────
        addBtn.addActionListener(e -> addSkill());
        removeBtn.addActionListener(e -> removeSkill());
        backBtn.addActionListener(e -> dispose());

        // Allow pressing Enter in the text field
        skillField.addActionListener(e -> addSkill());
    }

    // ── Load skills already added by this user ─────────────────
    /**
     * JOIN query: USER_SKILLS ⟶ SKILLS, filtered by user_id
     *
     * SELECT s.skill_id, s.skill_name
     * FROM   USER_SKILLS us
     * JOIN   SKILLS s ON us.skill_id = s.skill_id
     * WHERE  us.user_id = ?
     * ORDER  BY s.skill_name
     *
     * Time complexity: O(n) where n = number of skills offered by user
     */
    private void loadMySkills() {
        tableModel.setRowCount(0);   // Clear existing rows

        String sql = "SELECT s.skill_id, s.skill_name "
                   + "FROM USER_SKILLS us "
                   + "JOIN SKILLS s ON us.skill_id = s.skill_id "
                   + "WHERE us.user_id = ? "
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
            JOptionPane.showMessageDialog(this, "Error loading skills: " + e.getMessage());
        }
    }

    // ── Add a skill ───────────────────────────────────────────
    /**
     * Flow:
     *  1. Check if skill_name exists in SKILLS → get skill_id (or INSERT + get id)
     *  2. INSERT IGNORE into USER_SKILLS to link user ↔ skill
     *
     * INSERT IGNORE silently skips if the (user_id, skill_id) pair already exists.
     */
    private void addSkill() {
        String skillName = skillField.getText().trim();
        if (skillName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a skill name.");
            return;
        }

        Connection conn = DBConnection.getConnection();
        if (conn == null) return;

        try {
            // ── Step 1: Get or create the skill ──────────────
            int skillId = getOrCreateSkill(conn, skillName);

            // ── Step 2: Link to user ──────────────────────────
            String linkSql = "INSERT IGNORE INTO USER_SKILLS (user_id, skill_id) VALUES (?, ?)";
            PreparedStatement ps2 = conn.prepareStatement(linkSql);
            ps2.setInt(1, userId);
            ps2.setInt(2, skillId);
            int rows = ps2.executeUpdate();

            if (rows > 0) {
                JOptionPane.showMessageDialog(this,
                    "'" + skillName + "' added to your skill set! ✅");
            } else {
                JOptionPane.showMessageDialog(this,
                    "You already have '" + skillName + "' in your skills.");
            }

            skillField.setText("");
            loadMySkills();   // Refresh table

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Remove selected skill ─────────────────────────────────
    /**
     * Deletes only the USER_SKILLS row (the link), NOT the skill itself
     * from the master SKILLS table.
     *
     * DELETE FROM USER_SKILLS WHERE user_id = ? AND skill_id = ?
     */
    private void removeSkill() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a skill to remove.");
            return;
        }

        int    skillId   = (int)    tableModel.getValueAt(selectedRow, 0);
        String skillName = (String) tableModel.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Remove '" + skillName + "' from your skills?", "Confirm",
            JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM USER_SKILLS WHERE user_id = ? AND skill_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, skillId);
            ps.executeUpdate();
            loadMySkills();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // ── Helper: get existing skill_id or insert new skill ──────
    /**
     * First tries SELECT; if not found, INSERTs and returns generated key.
     * Time complexity: O(1) — index lookup on UNIQUE skill_name
     */
    private int getOrCreateSkill(Connection conn, String skillName) throws SQLException {
        // Try to find existing
        String findSql = "SELECT skill_id FROM SKILLS WHERE skill_name = ?";
        PreparedStatement ps = conn.prepareStatement(findSql);
        ps.setString(1, skillName);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt("skill_id");
        }

        // Not found → insert new skill
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
