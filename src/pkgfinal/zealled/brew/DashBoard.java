/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package pkgfinal.zealled.brew;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.util.ArrayList;
/**
 *
 * @author ASUS
 */
public class DashBoard extends javax.swing.JFrame {
    private String userName;

    /**
     * Creates new form DashBoard
     */
    
    // ========================= CONSTRUCTOR SECTION =========================
    public DashBoard() {
        initComponents();
        setupTable();
        setupTopSellersTable();
        setupTopAddonsTable();
        loadDashboardData();  
        
       
    }
    
    public DashBoard(String fullName) {
    initComponents();
    this.userName = fullName;
    lblWelcome.setText("Welcome " + fullName + "!");
    setupTable();              // 🔥 Now matches Products
    setupTopSellersTable();
    setupTopAddonsTable();
    loadDashboardData();       // 🔥 Calls formatDashboardTable()
}
    
    
    // ========================= TABLE SETUP SECTION =========================
    private void setupTable() {
    // 🔥 EXACTLY like Products form (keeps jTable1 connection)
    String[] columns = {"ID", "Name", "Category", "Size", "Selling Price", "Quantity", "Status"};
    DefaultTableModel model = new DefaultTableModel(columns, 0);
    jTable1.setModel(model);
    jTable1.getColumnModel().getColumn(5).setMinWidth(80);  // Quantity
    jTable1.getColumnModel().getColumn(6).setMinWidth(120); // Status
}

    private void setupTopSellersTable() {
        String[] columns = {"Rank", "Product", "Category", "Size", "Sold Today", "Revenue"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        jTableTopSellers.setModel(model);
        jTableTopSellers.setDefaultEditor(Object.class, null);
        jTableTopSellers.getTableHeader().setReorderingAllowed(false);
    }

    private void setupTopAddonsTable() {
        String[] columns = {"Rank", "Addon Name", "Times Added", "Revenue"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        jTableTopAddons.setModel(model);
        jTableTopAddons.setDefaultEditor(Object.class, null);
        jTableTopAddons.getTableHeader().setReorderingAllowed(false);
    }
     // ==================================================

    // ========================= MAIN DASHBOARD DATA LOADING OR MAIN DATA LOADER=========================
    private void loadDashboardData() {
    try {
        // 1. Product stats & low stock (share connection)
        try (Connection con = ConnectorXampp.connect()) {
            loadProductStats(con);
            loadLowStockTable(con);
        }
        
        // 2. Independent methods (own connections)
        loadTodaySalesData();
        loadTopAddon();
        loadTopAddonsTable();  // ← REMOVED DUPLICATE loadTopAddon()
        loadBestSellingProducts();
        
        // 3. Top sellers (own connection)
        try (Connection con = ConnectorXampp.connect()) {
            loadTopSellersTable(con);
        }
        
        // 4. Time-based stats
        loadWeeklySalesData();
        loadMonthlySalesData();
        
    } catch (Exception e) {
        javax.swing.JOptionPane.showMessageDialog(this, "Error loading dashboard: " + e.getMessage());
        e.printStackTrace();
    }
}
     // ==================================================

   // ========================= PRODUCT STATS SECTION (Top labels: Total, Low Stock counts) =========================
    private void loadProductStats(Connection con) throws Exception {
    // 🔥 1. TOTAL PRODUCTS
    String sqlTotal = "SELECT COUNT(*) as total FROM products";
    try (PreparedStatement pst = con.prepareStatement(sqlTotal);
         ResultSet rs = pst.executeQuery()) {
        if (rs.next()) {
            lblTotalProducts.setText("Total Products: " + rs.getInt("total"));
        }
    }

    // 🔥 2. CRITICAL LOW (Quantity <= 5 AND > 0)
    String sqlCritical = """
        SELECT COUNT(*) as critical_count 
        FROM products 
        WHERE Quantity > 0 AND Quantity <= 5
    """;
    try (PreparedStatement pst = con.prepareStatement(sqlCritical);
         ResultSet rs = pst.executeQuery()) {
        if (rs.next()) {
            lblCriticalLowCount.setText("Critical Low: " + rs.getInt("critical_count"));
        }
    }

    // 🔥 3. LOW STOCK (Quantity 6-10)
    String sqlLow = """
        SELECT COUNT(*) as low_count 
        FROM products 
        WHERE Quantity >= 6 AND Quantity <= 10
    """;
    try (PreparedStatement pst = con.prepareStatement(sqlLow);
         ResultSet rs = pst.executeQuery()) {
        if (rs.next()) {
            lblLowStockCount.setText("Low Stock: " + rs.getInt("low_count"));
        }
    }

    // 🔥 4. OUT OF STOCK (Quantity = 0)
    String sqlOut = "SELECT COUNT(*) as out_count FROM products WHERE Quantity = 0";
    try (PreparedStatement pst = con.prepareStatement(sqlOut);
         ResultSet rs = pst.executeQuery()) {
        if (rs.next()) {
            lblOutOfStockCount.setText("Out of Stock: " + rs.getInt("out_count"));
        }
    }
}
    
    private void loadLowStockTable(Connection con) throws Exception {
    String sql = """
        SELECT ProductID, Name, Category, Size, Price, Quantity,
        CASE 
            WHEN Quantity = 0 THEN 'Out of Stock'
            WHEN Quantity <= 5 THEN 'Critical Low'
            WHEN Quantity <= 10 THEN 'Low Stock'
            WHEN Quantity <= 20 THEN 'Normal'
            ELSE 'In Stock'
        END as Status
        FROM products
        ORDER BY 
            CASE 
                WHEN Quantity = 0 THEN 1
                WHEN Quantity <= 5 THEN 2
                WHEN Quantity <= 10 THEN 3
                ELSE 4
            END, Quantity ASC
    """;

    try (PreparedStatement pst = con.prepareStatement(sql);
         ResultSet rs = pst.executeQuery()) {

        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);

        while (rs.next()) {
            model.addRow(new Object[]{
                rs.getInt("ProductID"),
                rs.getString("Name"),
                rs.getString("Category"),
                rs.getString("Size"),
                rs.getInt("Price"),
                rs.getInt("Quantity"),
                rs.getString("Status")
            });
        }
        
    }
}
     // ==================================================

    // ========================= TODAY'S DATA SECTION =========================
    private void loadTodaySalesData() {
        try (Connection con = ConnectorXampp.connect()) {
            String sql = """
                SELECT COUNT(*) as orders,
                       COALESCE(SUM(TotalAmount), 0) as total,
                       COALESCE(AVG(TotalAmount), 0) as avg
                FROM orders
                WHERE DATE(OrderDate) = CURDATE()
            """;

            try (PreparedStatement pst = con.prepareStatement(sql);
                 ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    lblTodayOrders.setText("Today's Orders: " + rs.getInt("orders"));
                    lblTodaySales.setText("Today's Sales: ₱" + String.format("%.2f", rs.getDouble("total")));
                    lblTodayAvgOrder.setText("Avg Order: ₱" + String.format("%.2f", rs.getDouble("avg")));
                }
            }

            // Today's Revenue (same as sales)
            lblTodayRevenue.setText(lblTodaySales.getText().replace("Sales", "Revenue"));
            
            // Today's Profit (40% margin estimate)
            String sqlProfit = """
                SELECT COALESCE(SUM(od.Subtotal), 0) as revenue
                FROM order_details od
                JOIN orders o ON od.OrderID = o.OrderID
                WHERE DATE(o.OrderDate)=CURDATE()
            """;
            try (PreparedStatement pst = con.prepareStatement(sqlProfit);
                 ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    double revenue = rs.getDouble("revenue");
                    double profit = revenue * 0.4;
                    lblTodayProfit.setText("Today's Profit: ₱" + String.format("%.2f", profit));
                }
            }
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Sales error: " + e.getMessage());
        }
    }

    // ========================= BEST SELLING PRODUCT (LABEL) =========================
    private void loadBestSellingProducts() {
        try (Connection con = ConnectorXampp.connect()) {
            String sql = """
                SELECT p.Name, p.Size, COALESCE(SUM(od.Quantity), 0) as sold
                FROM order_details od
                JOIN products p ON od.ProductID = p.ProductID
                JOIN orders o ON od.OrderID = o.OrderID
                WHERE DATE(o.OrderDate)=CURDATE()
                GROUP BY p.ProductID
                ORDER BY sold DESC
                LIMIT 1
            """;

            try (PreparedStatement pst = con.prepareStatement(sql);
                 ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    lblTopSeller.setText("Top Seller: " 
                            + rs.getString("Name") + " (" 
                            + rs.getString("Size") + ") - " 
                            + rs.getInt("sold") + " sold");
                } else {
                    lblTopSeller.setText("Top Seller: No sales today");
                }
            }
        } catch (Exception e) {
            lblTopSeller.setText("Error loading top seller");
        }
    }

    // ========================= TOP SELLERS TABLE (DAILY) =========================
    private void loadTopSellersTable(Connection con) throws Exception {
        String sql = """
            SELECT p.Name, p.Category, p.Size,
                   COALESCE(SUM(od.Quantity), 0) as sold,
                   COALESCE(SUM(od.Subtotal), 0) as revenue
            FROM order_details od
            JOIN products p ON od.ProductID = p.ProductID
            JOIN orders o ON od.OrderID = o.OrderID
            WHERE DATE(o.OrderDate)=CURDATE()
            GROUP BY p.ProductID
            ORDER BY sold DESC
            LIMIT 10
        """;

        try (PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            DefaultTableModel model = (DefaultTableModel) jTableTopSellers.getModel();
            model.setRowCount(0);

            int rank = 1;
            while (rs.next()) {
                model.addRow(new Object[]{
                    rank++,
                    rs.getString("Name"),
                    rs.getString("Category"),
                    rs.getString("Size"),
                    rs.getInt("sold"),
                    "₱" + String.format("%.2f", rs.getDouble("revenue"))
                });
            }
        }
    }

    // ========================= TOP ADDON (SINGLE - DAILY) =========================
    private void loadTopAddon() {
        try (Connection con = ConnectorXampp.connect()) {
            String sql = """
                SELECT a.Name, COALESCE(COUNT(*), 0) as times_added, 
                       COALESCE(SUM(od.AddonPrice * od.Quantity), 0) as revenue
                FROM order_details od
                JOIN addons a ON od.AddonName = a.Name
                JOIN orders o ON od.OrderID = o.OrderID
                WHERE DATE(o.OrderDate) = CURDATE() AND od.AddonName != 'None'
                GROUP BY a.Name
                ORDER BY times_added DESC
                LIMIT 1
            """;

            try (PreparedStatement pst = con.prepareStatement(sql);
                 ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    lblTopAddon.setText("Top Addon: " 
                            + rs.getString("Name") + " - " 
                            + rs.getInt("times_added") + " times (₱" 
                            + String.format("%.2f", rs.getDouble("revenue")) + ")");
                } else {
                    lblTopAddon.setText("Top Addon: None today");
                }
            }
        } catch (Exception e) {
            lblTopAddon.setText("Error loading top addon");
        }
    }

    // ========================= TOP ADDONS TABLE (DAILY) =========================
    private void loadTopAddonsTable() {
        try (Connection con = ConnectorXampp.connect()) {
            String sql = """
                SELECT a.Name as AddonName,
                       COALESCE(COUNT(*), 0) as times_added,
                       COALESCE(SUM(od.AddonPrice * od.Quantity), 0) as revenue
                FROM order_details od
                JOIN addons a ON od.AddonName = a.Name
                JOIN orders o ON od.OrderID = o.OrderID
                WHERE DATE(o.OrderDate) = CURDATE() AND od.AddonName != 'None'
                GROUP BY a.Name
                ORDER BY times_added DESC
                LIMIT 10
            """;

            try (PreparedStatement pst = con.prepareStatement(sql);
                 ResultSet rs = pst.executeQuery()) {

                DefaultTableModel model = (DefaultTableModel) jTableTopAddons.getModel();
                model.setRowCount(0);

                int rank = 1;
                while (rs.next()) {
                    model.addRow(new Object[]{
                        rank++,
                        rs.getString("AddonName"),
                        rs.getInt("times_added"),
                        "₱" + String.format("%.2f", rs.getDouble("revenue"))
                    });
                }
            }
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Error loading top addons: " + e.getMessage());
        }
    }
    
    // ==================================================

    
    // ========================= TIME PERIOD SECTION (Week/Month summaries) =========================
    
    // ========================= WEEKLY SALES =========================
    private void loadWeeklySalesData() {
        try (Connection con = ConnectorXampp.connect()) {
            String sql = """
                SELECT COUNT(*) as orders,
                       COALESCE(SUM(TotalAmount), 0) as total,
                       COALESCE(AVG(TotalAmount), 0) as avg
                FROM orders
                WHERE YEARWEEK(OrderDate, 1) = YEARWEEK(CURDATE(), 1)
            """;

            try (PreparedStatement pst = con.prepareStatement(sql);
                 ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    lblWeekOrders.setText("This Week's Orders: " + rs.getInt("orders"));
                    lblWeekSales.setText("This Week's Sales: ₱" + String.format("%.2f", rs.getDouble("total")));
                    lblWeekAvgOrder.setText("Avg Order: ₱" + String.format("%.2f", rs.getDouble("avg")));
                }
            }

            lblWeekRevenue.setText(lblWeekSales.getText().replace("Sales", "Revenue"));

            // Weekly Profit
            String sqlProfit = """
                SELECT COALESCE(SUM(od.Subtotal), 0) as revenue
                FROM order_details od
                JOIN orders o ON od.OrderID = o.OrderID
                WHERE YEARWEEK(o.OrderDate, 1) = YEARWEEK(CURDATE(), 1)
            """;
            try (PreparedStatement pst = con.prepareStatement(sqlProfit);
                 ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    double revenue = rs.getDouble("revenue");
                    double profit = revenue * 0.4;
                    lblWeekProfit.setText("This Week's Profit: ₱" + String.format("%.2f", profit));
                }
            }
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Weekly sales error: " + e.getMessage());
        }
    }

    // ========================= MONTHLY SALES =========================
    private void loadMonthlySalesData() {
        try (Connection con = ConnectorXampp.connect()) {
            String sql = """
                SELECT COUNT(*) as orders,
                       COALESCE(SUM(TotalAmount), 0) as total,
                       COALESCE(AVG(TotalAmount), 0) as avg
                FROM orders
                WHERE YEAR(OrderDate) = YEAR(CURDATE())
                AND MONTH(OrderDate) = MONTH(CURDATE())
            """;

            try (PreparedStatement pst = con.prepareStatement(sql);
                 ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    lblMonthOrders.setText("This Month's Orders: " + rs.getInt("orders"));
                    lblMonthSales.setText("This Month's Sales: ₱" + String.format("%.2f", rs.getDouble("total")));
                    lblMonthAvgOrder.setText("Avg Order: ₱" + String.format("%.2f", rs.getDouble("avg")));
                }
            }

            lblMonthRevenue.setText(lblMonthSales.getText().replace("Sales", "Revenue"));

            // Monthly Profit
            String sqlProfit = """
                SELECT COALESCE(SUM(od.Subtotal), 0) as revenue
                FROM order_details od
                JOIN orders o ON od.OrderID = o.OrderID
                WHERE YEAR(o.OrderDate)=YEAR(CURDATE()) 
                AND MONTH(o.OrderDate)=MONTH(CURDATE())
            """;
            try (PreparedStatement pst = con.prepareStatement(sqlProfit);
                 ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    double revenue = rs.getDouble("revenue");
                    double profit = revenue * 0.4;
                    lblMonthProfit.setText("This Month's Profit: ₱" + String.format("%.2f", profit));
                }
            }
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Monthly sales error: " + e.getMessage());
        }
    }
    // ==================================================



    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        btnDashBoard = new javax.swing.JButton();
        btnProducts = new javax.swing.JButton();
        btnCategory = new javax.swing.JButton();
        btnAddons = new javax.swing.JButton();
        btnHistory = new javax.swing.JButton();
        btnLogOut = new javax.swing.JButton();
        btnUtilities = new javax.swing.JButton();
        lblWelcome = new javax.swing.JLabel();
        btnSize = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel4 = new javax.swing.JPanel();
        lblTopSeller = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTableTopSellers = new javax.swing.JTable();
        jPanel5 = new javax.swing.JPanel();
        lblTopAddonsTitle = new javax.swing.JLabel();
        lblTopAddon = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTableTopAddons = new javax.swing.JTable();
        jPanel2 = new javax.swing.JPanel();
        lblTotalProducts = new javax.swing.JLabel();
        lblLowStockCount = new javax.swing.JLabel();
        lblOutOfStockCount = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        lblCriticalLowCount = new javax.swing.JLabel();
        btnRefresh = new javax.swing.JButton();
        jTabbedPane2 = new javax.swing.JTabbedPane();
        jPanel6 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        lblTodaySales = new javax.swing.JLabel();
        lblTodayOrders = new javax.swing.JLabel();
        lblTodayProfit = new javax.swing.JLabel();
        lblTodayRevenue = new javax.swing.JLabel();
        lblTodayAvgOrder = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        lblWeekOrders = new javax.swing.JLabel();
        lblWeekAvgOrder = new javax.swing.JLabel();
        lblWeekSales = new javax.swing.JLabel();
        lblWeekRevenue = new javax.swing.JLabel();
        lblWeekProfit = new javax.swing.JLabel();
        jPanel8 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        lblMonthOrders = new javax.swing.JLabel();
        lblMonthSales = new javax.swing.JLabel();
        lblMonthAvgOrder = new javax.swing.JLabel();
        lblMonthRevenue = new javax.swing.JLabel();
        lblMonthProfit = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setBackground(new java.awt.Color(0, 0, 0));

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));

        btnDashBoard.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnDashBoard.setText("Dashboard");
        btnDashBoard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDashBoardActionPerformed(evt);
            }
        });

        btnProducts.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnProducts.setText("Products");
        btnProducts.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnProductsActionPerformed(evt);
            }
        });

        btnCategory.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnCategory.setText("Category");
        btnCategory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCategoryActionPerformed(evt);
            }
        });

        btnAddons.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnAddons.setText("Addons");
        btnAddons.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddonsActionPerformed(evt);
            }
        });

        btnHistory.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnHistory.setText("History");
        btnHistory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHistoryActionPerformed(evt);
            }
        });

        btnLogOut.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnLogOut.setText("Log Out");
        btnLogOut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLogOutActionPerformed(evt);
            }
        });

        btnUtilities.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnUtilities.setText("Utilities");
        btnUtilities.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUtilitiesActionPerformed(evt);
            }
        });

        btnSize.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnSize.setText("Size");
        btnSize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSizeActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblWelcome, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(16, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnSize, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(btnLogOut, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnHistory, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnAddons, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnCategory, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnProducts, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnDashBoard, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnUtilities, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGap(17, 17, 17))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(lblWelcome, javax.swing.GroupLayout.DEFAULT_SIZE, 117, Short.MAX_VALUE)
                .addGap(83, 83, 83)
                .addComponent(btnDashBoard)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnProducts)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnCategory)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnAddons)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnSize)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnHistory)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnUtilities)
                .addGap(228, 228, 228)
                .addComponent(btnLogOut)
                .addGap(57, 57, 57))
        );

        lblTopSeller.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblTopSeller.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTopSeller.setText(".");

        jTableTopSellers.setFont(new java.awt.Font("Serif", 0, 18)); // NOI18N
        jTableTopSellers.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "Rank", "Product", "Category", "Size", "Sold", "Revenue"
            }
        ));
        jScrollPane3.setViewportView(jTableTopSellers);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblTopSeller, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 998, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblTopSeller)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 720, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Products", jPanel4);

        lblTopAddonsTitle.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblTopAddonsTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTopAddonsTitle.setText("Top Addons Today");
        lblTopAddonsTitle.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        lblTopAddon.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblTopAddon.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTopAddon.setText(".");

        jTableTopAddons.setFont(new java.awt.Font("Serif", 0, 18)); // NOI18N
        jTableTopAddons.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Rank", "Addon", "Sold", "Revenue", "Avg Price"
            }
        ));
        jScrollPane2.setViewportView(jTableTopAddons);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2)
                .addContainerGap())
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(249, 249, 249)
                .addComponent(lblTopAddon, javax.swing.GroupLayout.PREFERRED_SIZE, 538, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(223, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblTopAddonsTitle)
                .addGap(417, 417, 417))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addComponent(lblTopAddonsTitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblTopAddon)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 702, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Addons", jPanel5);

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        lblTotalProducts.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblTotalProducts.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTotalProducts.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        lblLowStockCount.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblLowStockCount.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblLowStockCount.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        lblOutOfStockCount.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblOutOfStockCount.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblOutOfStockCount.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jTable1.setFont(new java.awt.Font("Serif", 0, 18)); // NOI18N
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null}
            },
            new String [] {
                "ID", "Product", "Category", "Size", "Price", "Quantity", "Status"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        lblCriticalLowCount.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblCriticalLowCount.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblCriticalLowCount.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 994, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(lblTotalProducts, javax.swing.GroupLayout.PREFERRED_SIZE, 229, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lblLowStockCount, javax.swing.GroupLayout.PREFERRED_SIZE, 229, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lblCriticalLowCount, javax.swing.GroupLayout.PREFERRED_SIZE, 229, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lblOutOfStockCount, javax.swing.GroupLayout.PREFERRED_SIZE, 229, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblLowStockCount, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblTotalProducts, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblCriticalLowCount, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblOutOfStockCount, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 695, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Stocks", jPanel2);

        btnRefresh.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnRefresh.setText("Refresh");
        btnRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefreshActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        jLabel1.setText("Today's Sales");

        lblTodaySales.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblTodaySales.setText(".");

        lblTodayOrders.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblTodayOrders.setText(".");

        lblTodayProfit.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblTodayProfit.setText(".");

        lblTodayRevenue.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblTodayRevenue.setText(".");

        lblTodayAvgOrder.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblTodayAvgOrder.setText(".");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblTodayOrders, javax.swing.GroupLayout.PREFERRED_SIZE, 318, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblTodayRevenue, javax.swing.GroupLayout.PREFERRED_SIZE, 318, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblTodayProfit, javax.swing.GroupLayout.PREFERRED_SIZE, 318, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblTodayAvgOrder, javax.swing.GroupLayout.PREFERRED_SIZE, 318, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGap(111, 111, 111)
                        .addComponent(jLabel1))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblTodaySales, javax.swing.GroupLayout.PREFERRED_SIZE, 318, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(18, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(11, 11, 11)
                .addComponent(lblTodayOrders)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblTodaySales)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblTodayRevenue)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblTodayProfit)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblTodayAvgOrder)
                .addContainerGap(25, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Today", jPanel6);

        jLabel3.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        jLabel3.setText("Weekly Sales");

        lblWeekOrders.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblWeekOrders.setText(".");

        lblWeekAvgOrder.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblWeekAvgOrder.setText(".");

        lblWeekSales.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblWeekSales.setText(".");

        lblWeekRevenue.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblWeekRevenue.setText(".");

        lblWeekProfit.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblWeekProfit.setText(".");

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGap(113, 113, 113)
                .addComponent(jLabel3)
                .addContainerGap(127, Short.MAX_VALUE))
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblWeekOrders, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblWeekAvgOrder, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblWeekSales, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblWeekRevenue, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblWeekProfit, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblWeekOrders)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblWeekSales)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblWeekAvgOrder)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblWeekRevenue)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblWeekProfit)
                .addContainerGap(24, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Weekly", jPanel7);

        jLabel2.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        jLabel2.setText("Monthly's Sales");

        lblMonthOrders.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblMonthOrders.setText(".");

        lblMonthSales.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblMonthSales.setText(".");

        lblMonthAvgOrder.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblMonthAvgOrder.setText(".");

        lblMonthRevenue.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblMonthRevenue.setText(".");

        lblMonthProfit.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        lblMonthProfit.setText(".");

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblMonthOrders, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblMonthSales, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblMonthAvgOrder, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblMonthRevenue, javax.swing.GroupLayout.DEFAULT_SIZE, 330, Short.MAX_VALUE)
                    .addComponent(lblMonthProfit, javax.swing.GroupLayout.DEFAULT_SIZE, 330, Short.MAX_VALUE))
                .addContainerGap())
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addGap(101, 101, 101)
                .addComponent(jLabel2)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblMonthOrders)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblMonthSales)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblMonthRevenue)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblMonthProfit)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblMonthAvgOrder)
                .addContainerGap(24, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Monthly", jPanel8);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTabbedPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 342, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(129, 129, 129)
                        .addComponent(btnRefresh)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 1010, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(404, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 803, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jTabbedPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 269, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnRefresh)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1920, 820));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnDashBoardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDashBoardActionPerformed
        // TODO add your handling code here:
        DashBoard a = new DashBoard();
       a.setVisible(true);
       this.dispose();
    }//GEN-LAST:event_btnDashBoardActionPerformed

    private void btnProductsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnProductsActionPerformed
        // TODO add your handling code here:
        Products c = new Products();
       c.setVisible(true);
       this.dispose();
    }//GEN-LAST:event_btnProductsActionPerformed

    private void btnCategoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCategoryActionPerformed
        // TODO add your handling code here:
        Category d = new Category();
       d.setVisible(true);
       this.dispose();
    }//GEN-LAST:event_btnCategoryActionPerformed

    private void btnAddonsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddonsActionPerformed
        // TODO add your handling code here:
        Addons e = new Addons();
       e.setVisible(true);
       this.dispose();
    }//GEN-LAST:event_btnAddonsActionPerformed

    private void btnHistoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHistoryActionPerformed
        // TODO add your handling code here:
        History f = new History();
       f.setVisible(true);
       this.dispose();
    }//GEN-LAST:event_btnHistoryActionPerformed

    private void btnUtilitiesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUtilitiesActionPerformed
        // TODO add your handling code here:
        Utilities g = new Utilities();
       g.setVisible(true);
       this.dispose();
    }//GEN-LAST:event_btnUtilitiesActionPerformed

    private void btnLogOutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLogOutActionPerformed
        // TODO add your handling code here:
        Login h = new Login();
       h.setVisible(true);
       this.dispose();
    }//GEN-LAST:event_btnLogOutActionPerformed

    private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshActionPerformed
        // TODO add your handling code here:
      loadDashboardData();
        
    }//GEN-LAST:event_btnRefreshActionPerformed

    private void btnSizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSizeActionPerformed
        // TODO add your handling code here:
        Size b = new Size();
        b.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnSizeActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(DashBoard.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(DashBoard.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(DashBoard.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(DashBoard.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new DashBoard().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddons;
    private javax.swing.JButton btnCategory;
    private javax.swing.JButton btnDashBoard;
    private javax.swing.JButton btnHistory;
    private javax.swing.JButton btnLogOut;
    private javax.swing.JButton btnProducts;
    private javax.swing.JButton btnRefresh;
    private javax.swing.JButton btnSize;
    private javax.swing.JButton btnUtilities;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTabbedPane jTabbedPane2;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTableTopAddons;
    private javax.swing.JTable jTableTopSellers;
    private javax.swing.JLabel lblCriticalLowCount;
    private javax.swing.JLabel lblLowStockCount;
    private javax.swing.JLabel lblMonthAvgOrder;
    private javax.swing.JLabel lblMonthOrders;
    private javax.swing.JLabel lblMonthProfit;
    private javax.swing.JLabel lblMonthRevenue;
    private javax.swing.JLabel lblMonthSales;
    private javax.swing.JLabel lblOutOfStockCount;
    private javax.swing.JLabel lblTodayAvgOrder;
    private javax.swing.JLabel lblTodayOrders;
    private javax.swing.JLabel lblTodayProfit;
    private javax.swing.JLabel lblTodayRevenue;
    private javax.swing.JLabel lblTodaySales;
    private javax.swing.JLabel lblTopAddon;
    private javax.swing.JLabel lblTopAddonsTitle;
    private javax.swing.JLabel lblTopSeller;
    private javax.swing.JLabel lblTotalProducts;
    private javax.swing.JLabel lblWeekAvgOrder;
    private javax.swing.JLabel lblWeekOrders;
    private javax.swing.JLabel lblWeekProfit;
    private javax.swing.JLabel lblWeekRevenue;
    private javax.swing.JLabel lblWeekSales;
    private javax.swing.JLabel lblWelcome;
    // End of variables declaration//GEN-END:variables

    
}
