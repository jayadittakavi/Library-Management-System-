package librarysystem.util;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserDashboard extends JFrame {
    private JTable booksTable;
    private String userId;
    private BookTransaction bookTransaction;

    // UI Components
    private JPanel userDetailsPanel;
    private JPanel activityPanel;
    private JPanel activityButtonsPanel;

    public UserDashboard(String userId) {
        this.userId = userId;
        this.bookTransaction = new BookTransaction(userId);

        setTitle("Library Management - User Dashboard");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // **Top Panel (Welcome + Logout)**
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel welcomeLabel = new JLabel("Welcome, User " + userId, SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 36));
        topPanel.add(welcomeLabel, BorderLayout.CENTER);

        JButton logoutButton = new JButton("Logout");
        logoutButton.setFont(new Font("Arial", Font.BOLD, 16));
        logoutButton.addActionListener(e -> logout());
        JPanel logoutPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        logoutPanel.add(logoutButton);
        topPanel.add(logoutPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // **Left Panel (User Details & Activity Buttons)**
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setPreferredSize(new Dimension(300, getHeight()));

        // **View Details Button**
        JButton viewDetailsButton = new JButton("View Details");
        viewDetailsButton.setFont(new Font("Arial", Font.BOLD, 16));
        viewDetailsButton.addActionListener(e -> toggleUserDetails());

        // **User Details Panel (Initially Hidden)**
        userDetailsPanel = new JPanel();
        userDetailsPanel.setLayout(new BorderLayout());
        userDetailsPanel.setVisible(false);

        JTextArea detailsArea = new JTextArea();
        detailsArea.setFont(new Font("Arial", Font.PLAIN, 16));
        detailsArea.setEditable(false);
        userDetailsPanel.add(new JScrollPane(detailsArea), BorderLayout.CENTER);

        // **View Activity Button**
        JButton viewActivityButton = new JButton("View Activity");
        viewActivityButton.setFont(new Font("Arial", Font.BOLD, 16));
        viewActivityButton.addActionListener(e -> toggleActivityPanel());

        // **Activity Buttons Panel (Initially Hidden)**
        activityButtonsPanel = new JPanel();
        activityButtonsPanel.setLayout(new GridLayout(3, 1));
        activityButtonsPanel.setVisible(false);

        JButton borrowedHistoryButton = new JButton("Borrowed Books History");
        borrowedHistoryButton.addActionListener(e -> bookTransaction.viewBorrowedBooksHistory());

        JButton returnedHistoryButton = new JButton("Returned Books History");
        returnedHistoryButton.addActionListener(e -> bookTransaction.viewBooksToBeReturned());

        JButton pendingDuesButton = new JButton("Pending Dues");
        pendingDuesButton.addActionListener(e -> bookTransaction.viewPendingFine());

        activityButtonsPanel.add(borrowedHistoryButton);
        activityButtonsPanel.add(returnedHistoryButton);
        activityButtonsPanel.add(pendingDuesButton);

        // **Add Components to Left Panel**
        leftPanel.add(viewDetailsButton);
        leftPanel.add(userDetailsPanel);
        leftPanel.add(viewActivityButton);
        leftPanel.add(activityButtonsPanel);

        add(leftPanel, BorderLayout.WEST);

        // **Books Table (Right Side)**
        String[] columnNames = {"Book ID", "Title", "Author", "Available Copies", "Borrow"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
        fetchAvailableBooks(tableModel);

        booksTable = new JTable(tableModel);
        booksTable.setFont(new Font("Arial", Font.PLAIN, 18));
        booksTable.setRowHeight(30);
        JTableHeader tableHeader = booksTable.getTableHeader();
        tableHeader.setFont(new Font("Arial", Font.BOLD, 20));

        JScrollPane scrollPane = new JScrollPane(booksTable);
        add(scrollPane, BorderLayout.CENTER);

        // **Bottom Panel (Only Borrow Button Now)**
        JPanel bottomPanel = new JPanel();
        JButton borrowButton = new JButton("Borrow Selected Book");
        borrowButton.setFont(new Font("Arial", Font.BOLD, 20));
        borrowButton.addActionListener(e -> borrowBook());
        bottomPanel.add(borrowButton);

        add(bottomPanel, BorderLayout.SOUTH);

        // Load User Details Initially
        detailsArea.setText(getStudentDetails(userId));

        setVisible(true);
    }

    private void toggleUserDetails() {
        userDetailsPanel.setVisible(!userDetailsPanel.isVisible());
        revalidate();
        repaint();
    }

    private void toggleActivityPanel() {
        activityButtonsPanel.setVisible(!activityButtonsPanel.isVisible());
        revalidate();
        repaint();
    }

    private String getStudentDetails(String userId) {
        StringBuilder details = new StringBuilder();
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT student_id, name, email, phone, department FROM students WHERE student_id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                details.append("Student ID: ").append(rs.getString("student_id")).append("\n");
                details.append("Name: ").append(rs.getString("name")).append("\n");
                details.append("Email: ").append(rs.getString("email")).append("\n");
                details.append("Phone: ").append(rs.getString("phone")).append("\n");
                details.append("Department: ").append(rs.getString("department")).append("\n");
            } else {
                details.append("No student details found!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            details.append("Error fetching details.");
        }
        return details.toString();
    }

    private void fetchAvailableBooks(DefaultTableModel tableModel) {
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT book_id, title, author, available_copies FROM books WHERE available_copies > 0";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int bookId = rs.getInt("book_id");
                String title = rs.getString("title");
                String author = rs.getString("author");
                int availableCopies = rs.getInt("available_copies");

                tableModel.addRow(new Object[]{bookId, title, author, availableCopies, "Borrow"});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void borrowBook() {
        int selectedRow = booksTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a book to borrow.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int bookId = (int) booksTable.getValueAt(selectedRow, 0);
        bookTransaction.borrowBook(bookId);  // Call borrowBook from BookTransaction

        dispose();
        new UserDashboard(userId); // Refresh UI
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to logout?", "Logout", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            new LoginPage(); // Redirect to Login Page
        }
    }
}
