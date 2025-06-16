package librarysystem.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginPage extends JFrame {
    private JTextField userIdField;
    private JPasswordField passwordField;
    private JLabel notificationLabel;

    public LoginPage() {
        setTitle("Library Management System - Login");
        setSize(700, 400); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Notification Bar
        notificationLabel = new JLabel("Library Timings: Monday - Friday: 9 AM - 8 PM | Saturday - Sunday: 10 AM - 6 PM | Closed on Public Holidays", JLabel.CENTER);
        notificationLabel.setFont(new Font("Arial", Font.BOLD, 14));
        notificationLabel.setOpaque(true);
        notificationLabel.setBackground(Color.CYAN);
        notificationLabel.setForeground(Color.RED);
        add(notificationLabel, BorderLayout.NORTH);

        // Main Panel for Login Form
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Library Management");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 26)); 
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);
        gbc.gridwidth = 1; 

        JLabel userLabel = new JLabel("User ID:");
        userLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(userLabel, gbc);

        userIdField = new JTextField(15);
        gbc.gridx = 1;
        panel.add(userIdField, gbc);

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(passwordLabel, gbc);

        passwordField = new JPasswordField(15);
        gbc.gridx = 1;
        panel.add(passwordField, gbc);

        JButton loginButton = new JButton("Login");
        JButton cancelButton = new JButton("Cancel");

        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(loginButton, gbc);

        gbc.gridx = 1;
        panel.add(cancelButton, gbc);

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                authenticateUser();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0); 
            }
        });

        add(panel, BorderLayout.CENTER);
        setVisible(true);
    }

    private void authenticateUser() {
        String userId = userIdField.getText();
        String password = new String(passwordField.getPassword());

        if (userId.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter User ID and Password", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            // Check in admins table
            String adminQuery = "SELECT * FROM admins WHERE admin_id = ? AND password = ?";
            try (PreparedStatement adminStmt = conn.prepareStatement(adminQuery)) {
                adminStmt.setString(1, userId);
                adminStmt.setString(2, password);
                ResultSet adminRs = adminStmt.executeQuery();
                if (adminRs.next()) {
                    JOptionPane.showMessageDialog(this, "Admin Login Successful!");
                    new AdminDashboard(userId);
                    dispose();
                    return;
                }
            }

            // Check in students table
            String studentQuery = "SELECT * FROM students WHERE student_id = ? AND password = ?";
            try (PreparedStatement studentStmt = conn.prepareStatement(studentQuery)) {
                studentStmt.setString(1, userId);
                studentStmt.setString(2, password);
                ResultSet studentRs = studentStmt.executeQuery();
                if (studentRs.next()) {
                    JOptionPane.showMessageDialog(this, "User Login Successful!");
                    new UserDashboard(userId);
                    dispose();
                    return;
                }
            }

            // Check in faculty table
            String facultyQuery = "SELECT * FROM faculty WHERE faculty_id = ? AND password = ?";
            try (PreparedStatement facultyStmt = conn.prepareStatement(facultyQuery)) {
                facultyStmt.setString(1, userId);
                facultyStmt.setString(2, password);
                ResultSet facultyRs = facultyStmt.executeQuery();
                if (facultyRs.next()) {
                    JOptionPane.showMessageDialog(this, "User Login Successful!");
                    new UserDashboard(userId); // Faculty logs in as a user
                    dispose();
                    return;
                }
            }

            // If no match found
            JOptionPane.showMessageDialog(this, "Invalid Credentials!", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database Connection Error", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        new LoginPage();
    }
}
