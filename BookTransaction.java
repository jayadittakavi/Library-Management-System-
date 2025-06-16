package librarysystem.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JOptionPane;

public class BookTransaction {
    private String userId;

    public BookTransaction(String userId) {
        this.userId = userId;
    }

    // Borrow book
    public void borrowBook(int bookId) {
        try (Connection conn = DBConnection.getConnection()) {
            // Check available copies
            String checkCopiesQuery = "SELECT available_copies, title FROM books WHERE book_id = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkCopiesQuery)) {
                checkStmt.setInt(1, bookId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        int availableCopies = rs.getInt("available_copies");
                        String title = rs.getString("title");

                        if (availableCopies <= 0) {
                            JOptionPane.showMessageDialog(null, "No copies available for this book!", "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }

                        // Determine user type (Student or Faculty)
                        String userTypeQuery = "SELECT 'Student' FROM students WHERE student_id = ? " +
                                               "UNION " +
                                               "SELECT 'Faculty' FROM faculty WHERE faculty_id = ?";
                        try (PreparedStatement userTypeStmt = conn.prepareStatement(userTypeQuery)) {
                            userTypeStmt.setString(1, userId);
                            userTypeStmt.setString(2, userId);
                            try (ResultSet userTypeRs = userTypeStmt.executeQuery()) {
                                String userType = (userTypeRs.next()) ? userTypeRs.getString(1) : "Unknown";

                                // Insert into borrowed_books table
                                String insertQuery = "INSERT INTO borrowed_books (transaction_id, user_id, user_type, book_id, title, borrow_date, fine_amount) " +
                                                     "VALUES (TRANSACTION_SEQ.NEXTVAL, ?, ?, ?, ?, SYSDATE, 0)";
                                try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                                    insertStmt.setString(1, userId);
                                    insertStmt.setString(2, userType);
                                    insertStmt.setInt(3, bookId);
                                    insertStmt.setString(4, title);
                                    insertStmt.executeUpdate();
                                }

                                // Update available copies
                                String updateBookQuery = "UPDATE books SET available_copies = available_copies - 1 WHERE book_id = ?";
                                try (PreparedStatement updateStmt = conn.prepareStatement(updateBookQuery)) {
                                    updateStmt.setInt(1, bookId);
                                    int rowsAffected = updateStmt.executeUpdate();

                                    if (rowsAffected > 0) {
                                        JOptionPane.showMessageDialog(null, "Request sent successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                                    } else {
                                        JOptionPane.showMessageDialog(null, "Failed to update book availability!", "Error", JOptionPane.ERROR_MESSAGE);
                                    }
                                }
                            }
                        }
                    } else {
                        JOptionPane.showMessageDialog(null, "Book not found!", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error processing the request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Return book
    public void returnBook(int bookId) {
        try (Connection conn = DBConnection.getConnection()) {
            // Check if the user has borrowed this book
            String checkQuery = "SELECT transaction_id, borrow_date FROM borrowed_books WHERE user_id = ? AND book_id = ? AND return_date IS NULL";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                checkStmt.setString(1, userId);
                checkStmt.setInt(2, bookId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        int transactionId = rs.getInt("transaction_id");
                        java.sql.Date borrowDate = rs.getDate("borrow_date");

                        // Calculate fine if returned late (assuming 5 days allowed)
                        String fineQuery = "SELECT CASE WHEN SYSDATE - borrow_date > 5 THEN (SYSDATE - borrow_date - 5) * 10 ELSE 0 END AS fine FROM borrowed_books WHERE transaction_id = ?";
                        int fineAmount = 0;
                        try (PreparedStatement fineStmt = conn.prepareStatement(fineQuery)) {
                            fineStmt.setInt(1, transactionId);
                            try (ResultSet fineRs = fineStmt.executeQuery()) {
                                if (fineRs.next()) {
                                    fineAmount = fineRs.getInt("fine");
                                }
                            }
                        }

                        // Update return date and fine
                        String updateQuery = "UPDATE borrowed_books SET return_date = SYSDATE, fine_amount = ? WHERE transaction_id = ?";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                            updateStmt.setInt(1, fineAmount);
                            updateStmt.setInt(2, transactionId);
                            updateStmt.executeUpdate();
                        }

                        // Increase available copies in books table
                        String updateBookQuery = "UPDATE books SET available_copies = available_copies + 1 WHERE book_id = ?";
                        try (PreparedStatement updateBookStmt = conn.prepareStatement(updateBookQuery)) {
                            updateBookStmt.setInt(1, bookId);
                            updateBookStmt.executeUpdate();
                        }

                        JOptionPane.showMessageDialog(null, "Book returned successfully! Fine: ₹" + fineAmount, "Success", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(null, "You have not borrowed this book or it has already been returned!", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error processing the request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // View borrowed books history
    public void viewBorrowedBooksHistory() {
        showActivity("SELECT book_id, title, borrow_date FROM borrowed_books WHERE user_id = ?", "Books Borrowed History");
    }

    // View books to be returned
    public void viewBooksToBeReturned() {
        showActivity("SELECT book_id, title, borrow_date FROM borrowed_books WHERE user_id = ? AND return_date IS NULL", "Books to be Returned");
    }

    // View pending fines
    public void viewPendingFine() {
        showActivity("SELECT book_id, title, fine_amount FROM borrowed_books WHERE user_id = ? AND fine_amount > 0", "Pending Fine");
    }

    // Common method for showing activity
    private void showActivity(String query, String title) {
        StringBuilder activity = new StringBuilder();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    activity.append("Book ID: ").append(rs.getString("book_id")).append("\n");
                    activity.append("Title: ").append(rs.getString("title")).append("\n");

                    if (query.contains("borrow_date")) {
                        activity.append("Borrow Date: ").append(rs.getString("borrow_date")).append("\n\n");
                    } else if (query.contains("fine_amount")) {
                        activity.append("Fine Amount: ₹").append(rs.getString("fine_amount")).append("\n\n");
                    }
                }

                if (activity.length() == 0) {
                    activity.append("No records found.");
                }

                JOptionPane.showMessageDialog(null, activity.toString(), title, JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error fetching data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

	public void requestBook(int bookId) {
		// TODO Auto-generated method stub
		
	}
}
