// ============================================================
//  FILE: RequestPage.java
//  PURPOSE: Lets the logged-in user send a skill exchange request
//           to another user.
//
//  FLOW:
//   1. Dropdowns auto-populate from DB:
//      - Receiver  → all other USERS
//      - Skill Offered  → skills the sender HAS (USER_SKILLS)
//      - Skill Requested → skills the receiver HAS (loaded when
//        receiver is selected)
//   2. On "Send Request" → INSERT into REQUESTS
//   3. Prevents duplicate Pending requests for the same pair+skills.
// ============================================================

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class RequestPage extends JFrame {

    private final int    userId;
    private final String userName;

    // Dropdowns
    private JComboBox<String> receiverBox;
    private JComboBox<String> offeredBox;
    private JComboBox<String> requestedBox;
    private JButton           sendBtn;
    private JButton           backBtn;
    private JLabel            statusLbl;

    // Maps: display string → DB id  (for easy retrieval)
    private final Map<String, Integer> userMap      = new HashMap<>();
    private final Map<String, Integer> offeredMap   = new HashMap<>();
    private final Map<String, Integer> requestedMap = new HashMap<>();

    public RequestPage(int userId, String userName) {
        this.userId   = userId;
        this.userName = userName;

        setTitle("SkillSwap – Send Exchange Request  [" + userName + "]");
        setSize(500, 420);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        buildUI();
        populateReceivers();
        populateOfferedSkills();
        setVisible(true);
    }

    // ── UI Layout ─────────────────────────────────────────────
    private void buildUI() {
        JPanel main = new JPanel(new GridBagLayout());
        main.setBackground(new Color(255, 248, 235));
        main.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 8, 10, 8);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        // Title
        JLabel title = new JLabel("Send Skill Exchange Request", SwingConstants.CENTER);
        title.setFont(new Font("Dialog", Font.BOLD, 16));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        main.add(title, gbc);

        // Info
        JLabel info = new JLabel(
            "<html><center>You are: <b>" + userName
            + "</b> (ID " + userId + ")</center></html>",
            SwingConstants.CENTER);
        info.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        info.setForeground(Color.GRAY);
        gbc.gridy = 1;
        main.add(info, gbc);

        // ── Select Receiver ───────────────────────────────────
        gbc.gridwidth = 1; gbc.gridy = 2; gbc.gridx = 0;
        main.add(new JLabel("Send To (Receiver):"), gbc);
        receiverBox = new JComboBox<>();
        receiverBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        gbc.gridx = 1;
        main.add(receiverBox, gbc);

        // ── Skill I Offer ─────────────────────────────────────
        gbc.gridy = 3; gbc.gridx = 0;
        main.add(new JLabel("Skill I'm Offering:"), gbc);
        offeredBox = new JComboBox<>();
        offeredBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        gbc.gridx = 1;
        main.add(offeredBox, gbc);

        // ── Skill I Want (from receiver) ──────────────────────
        gbc.gridy = 4; gbc.gridx = 0;
        main.add(new JLabel("Skill I Want from Them:"), gbc);
        requestedBox = new JComboBox<>();
        requestedBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        gbc.gridx = 1;
        main.add(requestedBox, gbc);

        // ── Send button ───────────────────────────────────────
        sendBtn = new JButton("Send Request");
        gbc.gridy = 5; gbc.gridx = 0; gbc.gridwidth = 2;
        main.add(sendBtn, gbc);

        backBtn = new JButton("Back to Dashboard");
        gbc.gridy = 6;
        main.add(backBtn, gbc);

        // Status label
        statusLbl = new JLabel("", SwingConstants.CENTER);
        statusLbl.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        gbc.gridy = 7;
        main.add(statusLbl, gbc);

        add(main);

        // When receiver changes, reload their skills
        receiverBox.addActionListener(e -> {
            String selected = (String) receiverBox.getSelectedItem();
            if (selected != null && userMap.containsKey(selected)) {
                loadReceiverSkills(userMap.get(selected));
            }
        });

        sendBtn.addActionListener(e -> sendRequest());
        backBtn.addActionListener(e -> dispose());
    }

    // ── Populate Receivers dropdown ───────────────────────────
    /**
     * SELECT user_id, name, city FROM USERS WHERE user_id <> ?
     * ORDER BY name
     */
    private void populateReceivers() {
        userMap.clear();
        receiverBox.removeAllItems();

        String sql = "SELECT user_id, name, city FROM USERS WHERE user_id <> ? ORDER BY name";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String label = rs.getString("name") + " (" + rs.getString("city") + ")";
                userMap.put(label, rs.getInt("user_id"));
                receiverBox.addItem(label);
            }
            // Load the first receiver's skills immediately
            if (receiverBox.getItemCount() > 0) {
                String first = (String) receiverBox.getSelectedItem();
                if (first != null) loadReceiverSkills(userMap.get(first));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading users: " + e.getMessage());
        }
    }

    // ── Populate Offered Skills (sender's skills) ─────────────
    /**
     * SELECT s.skill_id, s.skill_name
     * FROM USER_SKILLS us JOIN SKILLS s ON us.skill_id = s.skill_id
     * WHERE us.user_id = ?
     */
    private void populateOfferedSkills() {
        offeredMap.clear();
        offeredBox.removeAllItems();

        String sql = "SELECT s.skill_id, s.skill_name "
                   + "FROM USER_SKILLS us JOIN SKILLS s ON us.skill_id = s.skill_id "
                   + "WHERE us.user_id = ? ORDER BY s.skill_name";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String label = rs.getString("skill_name");
                offeredMap.put(label, rs.getInt("skill_id"));
                offeredBox.addItem(label);
            }
            if (offeredBox.getItemCount() == 0) {
                offeredBox.addItem("(No skills added yet)");
                statusLbl.setText("⚠ Add skills first via 'Add Skills I Have'");
                statusLbl.setForeground(Color.RED);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // ── Load selected receiver's offered skills ────────────────
    /**
     * Same query but for the receiver's user_id.
     * These are the skills the sender is requesting.
     */
    private void loadReceiverSkills(int receiverId) {
        requestedMap.clear();
        requestedBox.removeAllItems();

        String sql = "SELECT s.skill_id, s.skill_name "
                   + "FROM USER_SKILLS us JOIN SKILLS s ON us.skill_id = s.skill_id "
                   + "WHERE us.user_id = ? ORDER BY s.skill_name";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, receiverId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String label = rs.getString("skill_name");
                requestedMap.put(label, rs.getInt("skill_id"));
                requestedBox.addItem(label);
            }
            if (requestedBox.getItemCount() == 0) {
                requestedBox.addItem("(This user has no skills listed)");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // ── Send the request ──────────────────────────────────────
    /**
     * Validates selections then INSERTs into REQUESTS.
     *
     * INSERT INTO REQUESTS
     *   (sender_id, receiver_id, skill_offered, skill_requested, status)
     * VALUES (?, ?, ?, ?, 'Pending')
     *
     * Also checks for duplicate Pending request before inserting.
     */
    private void sendRequest() {
        String receiverLabel  = (String) receiverBox.getSelectedItem();
        String offeredLabel   = (String) offeredBox.getSelectedItem();
        String requestedLabel = (String) requestedBox.getSelectedItem();

        if (receiverLabel  == null || !userMap.containsKey(receiverLabel) ||
            offeredLabel   == null || !offeredMap.containsKey(offeredLabel) ||
            requestedLabel == null || !requestedMap.containsKey(requestedLabel)) {
            JOptionPane.showMessageDialog(this,
                "Please make sure all dropdowns have valid selections.\n"
              + "Ensure you have added skills and the receiver has skills listed.");
            return;
        }

        int receiverId   = userMap.get(receiverLabel);
        int skillOffered = offeredMap.get(offeredLabel);
        int skillRequested = requestedMap.get(requestedLabel);

        Connection conn = DBConnection.getConnection();
        if (conn == null) return;

        try {
            // Check for existing Pending request for same combination
            String checkSql =
                "SELECT request_id FROM REQUESTS "
              + "WHERE sender_id = ? AND receiver_id = ? "
              + "  AND skill_offered = ? AND skill_requested = ? "
              + "  AND status = 'Pending'";
            PreparedStatement check = conn.prepareStatement(checkSql);
            check.setInt(1, userId);
            check.setInt(2, receiverId);
            check.setInt(3, skillOffered);
            check.setInt(4, skillRequested);
            ResultSet rs = check.executeQuery();
            if (rs.next()) {
                JOptionPane.showMessageDialog(this,
                    "A Pending request with this exact combination already exists.");
                return;
            }

            // Insert the request
            String insertSql =
                "INSERT INTO REQUESTS "
              + "(sender_id, receiver_id, skill_offered, skill_requested, status) "
              + "VALUES (?, ?, ?, ?, 'Pending')";
            PreparedStatement ps = conn.prepareStatement(
                insertSql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, userId);
            ps.setInt(2, receiverId);
            ps.setInt(3, skillOffered);
            ps.setInt(4, skillRequested);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            int reqId = keys.next() ? keys.getInt(1) : -1;

            JOptionPane.showMessageDialog(this,
                "Request sent. Request ID: " + reqId,
                "Success", JOptionPane.INFORMATION_MESSAGE);

            statusLbl.setText("Request #" + reqId + " sent.");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
