package librarysystem.util;
import java.sql.Connection;

public class DBConnection {
    private static final String URL = "jdbc:oracle:thin:@localhost:1521:XE";
    private static final String USER = "system"; 
    private static final String PASSWORD = "manager";

    public static Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            conn = java.sql.DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Database connected successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn;
    }

    public static void main(String[] args) {
        Connection conn = getConnection();
        if (conn != null) {
            System.out.println("Connection successful!");
        } else {
            System.out.println("Connection failed!");
        }
    }
}
