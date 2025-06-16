package librarysystem.util;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.sql.*;

public class AdminDashboard extends JFrame {
    private String adminId;

    public AdminDashboard(String adminId) {
        this.adminId = adminId;
        setTitle("Library Management - Admin Dashboard");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // **Top Panel (Heading + Logout)**
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(createLabel("Admin Dashboard", 36, SwingConstants.CENTER), BorderLayout.CENTER);

        JButton logoutButton = createButton("Logout", 16, e -> logout());
        JPanel logoutPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        logoutPanel.add(logoutButton);
        topPanel.add(logoutPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // **Main Panel with Buttons**
        JPanel mainPanel = new JPanel(new GridLayout(3, 2, 20, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        String[] buttonNames = {"View Books", "Add Book", "Remove Book", "View Student Details", "View Borrowed/Returned Books", "Manage Fines"};
        ActionListener[] actions = {e -> viewBooks(), e -> addBook(), e -> removeBook(), e -> viewStudents(), e -> viewDailyTransactions(), e -> manageFines()};

        for (int i = 0; i < buttonNames.length; i++) {
            mainPanel.add(createButton(buttonNames[i], 20, actions[i]));
        }

        add(mainPanel, BorderLayout.CENTER);
        setVisible(true);
    }

    // **Creates Buttons with Action**
    private JButton createButton(String text, int fontSize, ActionListener action) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, fontSize));
        button.addActionListener(action);
        return button;
    }

    // **Creates Labels with Font**
    private JLabel createLabel(String text, int fontSize, int alignment) {
        JLabel label = new JLabel(text, alignment);
        label.setFont(new Font("Arial", Font.BOLD, fontSize));
        return label;
    }

    // **Method to Add a Book**
    private void addBook() {
        String[] labels = {"Book ID:", "Title:", "Author:", "Publisher:", "Publication Year:", "ISBN:", "Total Copies:", "Available Copies:"};
        JTextField[] fields = new JTextField[labels.length];
        JPanel panel = new JPanel(new GridLayout(labels.length, 2, 10, 10));

        for (int i = 0; i < labels.length; i++) {
            panel.add(createLabel(labels[i], 16, SwingConstants.LEFT));
            fields[i] = new JTextField();
            fields[i].setFont(new Font("Arial", Font.PLAIN, 16));
            panel.add(fields[i]);
        }

        JDialog dialog = new JDialog(this, "Enter Book Details", true);
        dialog.setLayout(new BorderLayout());
        dialog.add(panel, BorderLayout.CENTER);

        JButton submitButton = createButton("Add Book", 16, e -> {
            if (addBookToDatabase(fields)) {
                JOptionPane.showMessageDialog(dialog, "Book added successfully!");
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Failed to add book.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(submitButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        dialog.setSize(screenSize.width / 2, screenSize.height / 2);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private boolean addBookToDatabase(JTextField[] fields) {
        try (Connection conn = DBConnection.getConnection()) {
            String query = "INSERT INTO books (BOOK_ID, TITLE, AUTHOR, PUBLISHER, PUBLICATION_YEAR, ISBN, TOTAL_COPIES, AVAILABLE_COPIES) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            for (int i = 0; i < fields.length; i++) {
                stmt.setString(i + 1, fields[i].getText());
            }
            return stmt.executeUpdate() > 0;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    // **Method to Remove a Book**
    private void removeBook() {
        String bookId = JOptionPane.showInputDialog(this, "Enter Book ID to Remove:");
        if (bookId != null && !bookId.trim().isEmpty()) {
            try (Connection conn = DBConnection.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement("DELETE FROM books WHERE BOOK_ID = ?");
                stmt.setString(1, bookId);
                JOptionPane.showMessageDialog(this, stmt.executeUpdate() > 0 ? "Book removed successfully!" : "Book ID not found.");
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error removing book", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void viewBooks() {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM books");
             ResultSet rs = stmt.executeQuery()) {

            String[] columnNames = {"BOOK_ID", "TITLE", "AUTHOR", "PUBLISHER", "PUBLICATION_YEAR", "ISBN", "TOTAL_COPIES", "AVAILABLE_COPIES", "BORROWED_BY"};
            java.util.List<String[]> data = new java.util.ArrayList<>();

            while (rs.next()) {
                data.add(new String[]{
                        rs.getString("BOOK_ID"),
                        rs.getString("TITLE"),
                        rs.getString("AUTHOR"),
                        rs.getString("PUBLISHER"),
                        String.valueOf(rs.getInt("PUBLICATION_YEAR")),
                        rs.getString("ISBN"),
                        String.valueOf(rs.getInt("TOTAL_COPIES")),
                        String.valueOf(rs.getInt("AVAILABLE_COPIES")),
                        rs.getString("BORROWED_BY") != null ? rs.getString("BORROWED_BY") : "None"
                });
            }

            JTable table = new JTable(data.toArray(new String[0][]), columnNames);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // Prevent automatic shrinking
            table.setFillsViewportHeight(true);

            // Adjust column widths based on content
            int[] columnWidths = {80, 200, 150, 150, 120, 130, 100, 120, 150}; // Set preferred column widths
            for (int i = 0; i < columnWidths.length; i++) {
                table.getColumnModel().getColumn(i).setPreferredWidth(columnWidths[i]);
            }

            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setPreferredSize(new Dimension(1500, 00)); // Set preferred scroll pane size

            // Create a JDialog instead of JOptionPane
            JDialog dialog = new JDialog(this, "Book List", true);
            dialog.setLayout(new BorderLayout());
            dialog.add(scrollPane, BorderLayout.CENTER);

            // Get screen size and set dialog size dynamically
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int dialogWidth = Math.min(900, screenSize.width / 2); // Half of the screen or max 900px
            int dialogHeight = Math.min(500, screenSize.height / 2); // Half of the screen or max 500px
            dialog.setSize(dialogWidth, dialogHeight);
            dialog.setLocationRelativeTo(this);

            dialog.setVisible(true);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error fetching book details", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void viewStudents() {
        JDialog dialog = new JDialog(this, "Student List", true);
        dialog.setLayout(new BorderLayout());

        JTextField searchField = new JTextField(20);
        JButton searchButton = new JButton("Search");
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search: "));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        String[] columnNames = {"STUDENT_ID", "NAME", "EMAIL", "PHONE", "DEPARTMENT"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);
        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);

        ActionListener loadData = e -> {
            model.setRowCount(0);
            String searchQuery = searchField.getText().trim();
            String sql = "SELECT STUDENT_ID, NAME, EMAIL, PHONE, DEPARTMENT FROM students " +
                         (searchQuery.isEmpty() ? "" : "WHERE NAME LIKE ? OR STUDENT_ID LIKE ?");

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                if (!searchQuery.isEmpty()) {
                    stmt.setString(1, "%" + searchQuery + "%");
                    stmt.setString(2, "%" + searchQuery + "%");
                }
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) model.addRow(new Object[]{
                        rs.getString("STUDENT_ID"), rs.getString("NAME"),
                        rs.getString("EMAIL"), rs.getString("PHONE"), rs.getString("DEPARTMENT")});
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error fetching students", "Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        searchButton.addActionListener(loadData);
        searchField.addActionListener(loadData);
        loadData.actionPerformed(null);

        dialog.add(searchPanel, BorderLayout.NORTH);
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.setSize(900, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void viewDailyTransactions() {
        String searchDate = JOptionPane.showInputDialog(this, "Enter Date (DD-MON-YY):");
        if (searchDate == null || searchDate.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Invalid date entered", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String query = "SELECT 'Borrowed' AS TYPE, BOOK_ID, USER_ID, BORROW_DATE AS TRANSACTION_DATE " +
                       "FROM borrowed_books WHERE TRUNC(BORROW_DATE) = TO_DATE(?, 'DD-MON-YY') " +
                       "UNION " +
                       "SELECT 'Returned' AS TYPE, BOOK_ID, USER_ID, RETURN_DATE AS TRANSACTION_DATE " +
                       "FROM returned_books WHERE TRUNC(RETURN_DATE) = TO_DATE(?, 'DD-MON-YY') " +
                       "ORDER BY TRANSACTION_DATE";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, searchDate);
            stmt.setString(2, searchDate);
            ResultSet rs = stmt.executeQuery();

            String[] columnNames = {"TYPE", "BOOK_ID", "USER_ID", "TRANSACTION_DATE"};
            DefaultTableModel model = new DefaultTableModel(columnNames, 0);

            boolean hasData = false;
            while (rs.next()) {
                hasData = true;
                System.out.println("Fetched: " + rs.getString("TYPE") + ", " + rs.getString("BOOK_ID")); // Debugging
                model.addRow(new Object[]{
                        rs.getString("TYPE"),
                        rs.getString("BOOK_ID"),
                        rs.getString("USER_ID"),
                        rs.getDate("TRANSACTION_DATE")
                });
            }

            if (!hasData) {
                JOptionPane.showMessageDialog(this, "No records found for " + searchDate, "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            JTable table = new JTable(model);
            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setPreferredSize(new Dimension(800, 400));

            JDialog dialog = new JDialog(this, "Borrowed & Returned Books", true);
            dialog.setLayout(new BorderLayout());
            dialog.add(scrollPane, BorderLayout.CENTER);
            dialog.setSize(900, 500);
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error fetching transaction details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void manageFines() {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT FINE_ID, USER_ID, USER_TYPE, TRANSACTION_ID, FINE_AMOUNT, STATUS FROM fines WHERE STATUS = 'Unpaid'");
             ResultSet rs = stmt.executeQuery()) {

            String[] columnNames = {"FINE_ID", "USER_ID", "USER_TYPE", "TRANSACTION_ID", "FINE_AMOUNT", "STATUS"};
            DefaultTableModel model = new DefaultTableModel(columnNames, 0);

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("FINE_ID"),
                    rs.getString("USER_ID"),
                    rs.getString("USER_TYPE"),
                    rs.getInt("TRANSACTION_ID"),
                    rs.getDouble("FINE_AMOUNT"),
                    rs.getString("STATUS")
                });
            }

            JTable table = new JTable(model);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            JScrollPane scrollPane = new JScrollPane(table);
            
            int option = JOptionPane.showConfirmDialog(this, scrollPane, "Unpaid Fines", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (option == JOptionPane.OK_OPTION) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow != -1) {
                    int fineId = (int) model.getValueAt(selectedRow, 0);
                    markFineAsPaid(fineId);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error fetching fine details", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void markFineAsPaid(int fineId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE fines SET STATUS = 'Paid' WHERE FINE_ID = ?")) {

            stmt.setInt(1, fineId);
            stmt.executeUpdate();

            JOptionPane.showMessageDialog(this, "Fine marked as Paid!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating fine status", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void logout() {
        if (JOptionPane.showConfirmDialog(this, "Are you sure you want to logout?", "Logout", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            dispose();
            new LoginPage(); // Redirect to login page
        }
    }
}
