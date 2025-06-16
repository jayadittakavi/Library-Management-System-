package librarysystem.util;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.*;

public class BorrowRequests extends JFrame {
    private JTable requestTable;
    private DefaultTableModel requestTableModel;

    public BorrowRequests() {
        setTitle("Borrow Requests");
        setSize(600, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        String[] columnNames = {"Request ID", "User ID", "Book ID", "Status", "Action"};
        requestTableModel = new DefaultTableModel(columnNames, 0);
        requestTable = new JTable(requestTableModel);
        JScrollPane scrollPane = new JScrollPane(requestTable);
        
        add(scrollPane, BorderLayout.CENTER);
        loadRequests();
        
        setVisible(true);
    }

    private void loadRequests() {
        try (Connection con = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:XE", "username", "password");
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM borrow_requests")) {

            requestTableModel.setRowCount(0);
            while (rs.next()) {
                int requestId = rs.getInt("REQUEST_ID"); // Store request ID
                int userId = rs.getInt("USER_ID");
                int bookId = rs.getInt("BOOK_ID");
                String status = rs.getString("STATUS");

                JButton approveButton = new JButton("Approve");
                approveButton.addActionListener(e -> approveRequest(requestId)); // Use stored request ID

                requestTableModel.addRow(new Object[]{requestId, userId, bookId, status, approveButton});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void approveRequest(int requestId) {
        try (Connection con = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:XE", "username", "password");
             PreparedStatement pstmt = con.prepareStatement("UPDATE borrow_requests SET STATUS = 'Approved' WHERE REQUEST_ID = ?")) {
            
            pstmt.setInt(1, requestId);
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Request Approved!");
            loadRequests();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

