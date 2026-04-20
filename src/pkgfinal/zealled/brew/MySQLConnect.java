package pkgfinal.zealled.brew;
import java.sql.*;
import javax.swing.JOptionPane;
public class MySQLConnect {
    private static final String URL = "jdbc:mysql://localhost:3306/milkteapt2_db";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    
    public static Connection getConnection() {
        Connection conn = null;
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            //JOptionPane.showMessageDialog(null, "Connected! ");
        }catch(ClassNotFoundException | SQLException err){
            JOptionPane.showMessageDialog(null, err);
        }
        return conn;
    }
}
