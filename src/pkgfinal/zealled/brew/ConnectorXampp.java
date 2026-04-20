package pkgfinal.zealled.brew;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.JOptionPane;

public class ConnectorXampp {
    
    // Note: The space is allowed in the URL, but the table name in SQL needs backticks
    private static final String URL = "jdbc:mysql://localhost:3306/milkteapt2_db";
    private static final String USER = "root";
    private static final String PASSWORD = ""; 

    public static Connection connect() {
          try {
            // ✅ MUST match your exact database name
            String url = "jdbc:mysql://localhost:3306/zealled_db";
            String user = "root";
            String password = "";  // Update if you have password
            
            // ✅ Load MySQL driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // ✅ Create connection
            Connection con = DriverManager.getConnection(url, user, password);
            
            System.out.println("✅ Database Connected Successfully!");
            return con;
            
        } catch (Exception e) {
            System.out.println("❌ Connection Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}