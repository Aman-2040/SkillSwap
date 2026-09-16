// ============================================================
//  FILE: ViewRequestsPage.java
//  PURPOSE: Shows two tabs:
//           1. "Received" — requests sent TO the current user.
//              Can Accept or Reject a Pending request.
//           2. "Sent"     — requests sent BY the current user.
//              Can Cancel a Pending request.
//  Uses JTabbedPane + two JTable widgets.
// ============================================================

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ViewRequestsPage extends JFrame {

    private final int    userId;
    private final String userName;

    // ── Received tab ──────────────────────────────────────────
    private JTable            receivedTable;
    private DefaultTableModel receivedModel;

    // ── Sent tab ──────────────────────────────────────────────
    private JTable            sentTable;
    private DefaultTableModel sentModel;

    private JButton backBtn;

    public ViewRequestsPage(int userId, String userName) {
        this.userId   = userId;
        this.userName = userName;

        setTitle("SkillSwap – My Requests  [" + userName + "]");
        setSize(860, 540);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        buildUI();
        loadReceivedRequests();
        loadSentRequests();
        setVisible(true);
    }

    // ── UI Layout ─────────────────────────────────────────────
    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        main.setBackground(new Color(255, 240, 240));

        // Title
        JLabel title = new JLabel("My Requests", SwingConstants.CENTER);
        title.setFont(new Font("Dialog", Font.BOLD, 16));
        main.add(title, BorderLayout.NORTH);

        // ── Tabs ──────────────────────────────────────────────
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // ── Tab 1: RECEIVED ───────────────────────────────────
        JPanel recPanel = new JPanel(new BorderLayout(8, 8));
        recPanel.setBackground(new Color(255, 245, 245));

        String[] recCols = {
            "Req ID", "From (Sender)", "Skill They Offer",
            "Skill They Want", "Status"
        };
        receivedModel = new DefaultTableModel(recCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        receivedTable = buildTable(receivedModel);
        JScrollPane recScroll = new JScrollPane(receivedTable);
        recScroll.setBorder(BorderFactory.createTitledBorder(
            "Requests You Received  (select a Pending row to Accept/Reject)"));
        recPanel.add(recScroll, BorderLayout.CENTER);

        // Buttons for received tab
        JPanel recBtns = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        recBtns.setOpaque(false);
        JButton acceptBtn  = new JButton("Accept");
        JButton rejectBtn  = new JButton("Reject");
        JButton refreshRec = new JButton("Refresh");
        recBtns.add(acceptBtn);
        recBtns.add(rejectBtn);
        recBtns.add(refreshRec);
        recPanel.add(recBtns, BorderLayout.SOUTH);

        tabs.addTab("Received", recPanel);

        // ── Tab 2: SENT ───────────────────────────────────────
        JPanel sentPanel = new JPanel(new BorderLayout(8, 8));
        sentPanel.setBackground(new Color(245, 255, 245));

        String[] sentCols = {
            "Req ID", "To (Receiver)", "Skill I Offer",
            "Skill I Want", "Status"
        };
        sentModel = new DefaultTableModel(sentCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        sentTable = buildTable(sentModel);
        JScrollPane sentScroll = new JScrollPane(sentTable);
        sentScroll.setBorder(BorderFactory.createTitledBorder(
            "Requests You Sent  (select a Pending row to Cancel)"));
        sentPanel.add(sentScroll, BorderLayout.CENTER);

        JPanel sentBtns = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        sentBtns.setOpaque(false);
        JButton cancelBtn   = new JButton("Cancel Request");
        JButton refreshSent = new JButton("Refresh");
        sentBtns.add(cancelBtn);
        sentBtns.add(refreshSent);
        sentPanel.add(sentBtns, BorderLayout.SOUTH);

        tabs.addTab("Sent", sentPanel);

        main.add(tabs, BorderLayout.CENTER);

        // Bottom back button
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.setOpaque(false);
        backBtn = new JButton("Back to Dashboard");
        bottom.add(backBtn);
        main.add(bottom, BorderLayout.SOUTH);

        add(main);

        // ── Action listeners ──────────────────────────────────
        acceptBtn.addActionListener(e  -> updateRequestStatus(receivedTable, receivedModel, "Accepted"));
        rejectBtn.addActionListener(e  -> updateRequestStatus(receivedTable, receivedModel, "Rejected"));
        refreshRec.addActionListener(e -> loadReceivedRequests());

        cancelBtn.addActionListener(e  -> cancelSentRequest());
        refreshSent.addActionListener(e-> loadSentRequests());

        backBtn.addActionListener(e -> dispose());
    }

    // ── Load RECEIVED requests ─────────────────────────────────
    /**
     * Joins REQUESTS with USERS (twice: sender & receiver) and SKILLS (twice).
     *
     * SELECT r.request_id,
     *        sender.name            AS sender_name,
     *        s_offered.skill_name   AS skill_offered,
     *        s_requested.skill_name AS skill_requested,
     *        r.status
     * FROM   REQUESTS r
     * JOIN   USERS  sender         ON r.sender_id       = sender.user_id
     * JOIN   SKILLS s_offered      ON r.skill_offered   = s_offered.skill_id
     * JOIN   SKILLS s_requested    ON r.skill_requested = s_requested.skill_id
     * WHERE  r.receiver_id = ?
     * ORDER  BY r.request_id DESC
     *
     * Time complexity: O(R) where R = number of requests
     */
    private void loadReceivedRequests() {
        receivedModel.setRowCount(0);

        String sql =
            "SELECT r.request_id, "
          + "       sender.name            AS sender_name, "
          + "       s_offered.skill_name   AS offered_skill, "
          + "       s_requested.skill_name AS requested_skill, "
          + "       r.status "
          + "FROM   REQUESTS r "
          + "JOIN   USERS  sender       ON r.sender_id       = sender.user_id "
          + "JOIN   SKILLS s_offered    ON r.skill_offered   = s_offered.skill_id "
          + "JOIN   SKILLS s_requested  ON r.skill_requested = s_requested.skill_id "
          + "WHERE  r.receiver_id = ? "
          + "ORDER  BY r.request_id DESC";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                receivedModel.addRow(new Object[]{
                    rs.getInt("request_id"),
                    rs.getString("sender_name"),
                    rs.getString("offered_skill"),
                    rs.getString("requested_skill"),
                    rs.getString("status")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // ── Load SENT requests ────────────────────────────────────
    /**
     * Same structure but WHERE sender_id = ? and shows receiver name.
     */
    private void loadSentRequests() {
        sentModel.setRowCount(0);

        String sql =
            "SELECT r.request_id, "
          + "       receiver.name          AS receiver_name, "
          + "       s_offered.skill_name   AS offered_skill, "
          + "       s_requested.skill_name AS requested_skill, "
          + "       r.status "
          + "FROM   REQUESTS r "
          + "JOIN   USERS  receiver      ON r.receiver_id     = receiver.user_id "
          + "JOIN   SKILLS s_offered     ON r.skill_offered   = s_offered.skill_id "
          + "JOIN   SKILLS s_requested   ON r.skill_requested = s_requested.skill_id "
          + "WHERE  r.sender_id = ? "
          + "ORDER  BY r.request_id DESC";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                sentModel.addRow(new Object[]{
                    rs.getInt("request_id"),
                    rs.getString("receiver_name"),
                    rs.getString("offered_skill"),
                    rs.getString("requested_skill"),
                    rs.getString("status")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // ── Accept / Reject a received request ────────────────────
    /**
     * UPDATE REQUESTS SET status = ? WHERE request_id = ?
     * Only allowed if current status is 'Pending'.
     *
     * @param newStatus  "Accepted" or "Rejected"
     */
    private void updateRequestStatus(JTable tbl, DefaultTableModel model, String newStatus) {
        int row = tbl.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a request row.");
            return;
        }

        int    reqId  = (int)    model.getValueAt(row, 0);
        String status = (String) model.getValueAt(row, 4);

        if (!"Pending".equals(status)) {
            JOptionPane.showMessageDialog(this,
                "Only Pending requests can be updated.\nThis request is: " + status);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Mark request #" + reqId + " as " + newStatus + "?", "Confirm",
            JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "UPDATE REQUESTS SET status = ? WHERE request_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, reqId);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this,
                "Request #" + reqId + " marked as " + newStatus + ".");
            loadReceivedRequests();   // Refresh tab
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // ── Cancel a sent request ──────────────────────────────────
    /**
     * DELETE FROM REQUESTS WHERE request_id = ? AND sender_id = ?
     * Only Pending requests can be cancelled.
     */
    private void cancelSentRequest() {
        int row = sentTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a request to cancel.");
            return;
        }

        int    reqId  = (int)    sentModel.getValueAt(row, 0);
        String status = (String) sentModel.getValueAt(row, 4);

        if (!"Pending".equals(status)) {
            JOptionPane.showMessageDialog(this,
                "Can only cancel Pending requests.\nThis request is: " + status);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Cancel request #" + reqId + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        // Use sender_id as extra safety — can only delete own requests
        String sql = "DELETE FROM REQUESTS WHERE request_id = ? AND sender_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, reqId);
            ps.setInt(2, userId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Request #" + reqId + " cancelled.");
                loadSentRequests();
            } else {
                JOptionPane.showMessageDialog(this, "Could not cancel. Check ownership.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // ── Helper: build a styled JTable ─────────────────────────
    private JTable buildTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.setRowHeight(25);
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return t;
    }

}
