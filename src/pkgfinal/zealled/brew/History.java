/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package pkgfinal.zealled.brew;
import javax.swing.JOptionPane;
import java.sql.*;
/**
 *
 * @author ASUS
 */
public class History extends javax.swing.JFrame {
    private javax.swing.table.DefaultTableModel historyModel;
    /**
     * Creates new form DashBoard
     */
    
    // ========================= CONSTRUCTOR & FIELDS =========================
    public History() {
        initComponents();
        setupHistoryTable();
        loadOrders();
    }
    
    // ========================= TABLE SETUP =========================
     private void setupHistoryTable(){
    String[] columns = {
        "OrderID", "Date", "Type", "Products", "Subtotal", "Tax(12%)", "Total", "Cashier"  // 🔥 Added Cashier
    };

    historyModel = new javax.swing.table.DefaultTableModel(columns,0){
        @Override
        public boolean isCellEditable(int row, int column){
            return false;
        }
    };

    jTableHistory.setModel(historyModel);
    
    // 🔥 Make Cashier column narrower
    jTableHistory.getColumnModel().getColumn(7).setPreferredWidth(120);
}
     
     // ========================= DATA LOADERS =========================
    private void loadOrders(){
    historyModel.setRowCount(0);

    String sql = "SELECT o.OrderID, o.OrderDate, o.OrderType, " +
                 "GROUP_CONCAT(p.Name SEPARATOR ', ') AS Products, " +
                 "SUM(od.Subtotal) AS OrderSubtotal, " +
                 "o.TotalAmount " +
                 "FROM orders o " +
                 "JOIN order_details od ON o.OrderID = od.OrderID " +
                 "JOIN products p ON od.ProductID = p.ProductID " +
                 "GROUP BY o.OrderID " +
                 "ORDER BY o.OrderID DESC";

    try(Connection con = ConnectorXampp.connect();
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql)){

        while(rs.next()){
            double subtotal = rs.getDouble("OrderSubtotal");
            double tax = subtotal * 0.12;
            double total = rs.getDouble("TotalAmount");
            
            String cashierName = getCashierNameForOrder(rs.getInt("OrderID"));
            
            historyModel.addRow(new Object[]{
                rs.getInt("OrderID"),
                rs.getString("OrderDate"),
                rs.getString("OrderType"),
                rs.getString("Products"),
                "₱" + String.format("%.2f", subtotal),
                "₱" + String.format("%.2f", tax),
                "₱" + String.format("%.2f", total),
                cashierName
            });
        }
    }catch(Exception e){
        JOptionPane.showMessageDialog(this,e.getMessage());
    }
}
  
    private void showOrderDetails(int orderId){
    StringBuilder receipt = new StringBuilder();

    String orderSql = """
        SELECT o.OrderDate, o.OrderType, o.TotalAmount, o.`Cash`, o.`Change`,
               u.full_name AS CashierName 
        FROM orders o 
        LEFT JOIN users u ON o.UserID = u.id 
        WHERE o.OrderID = ?
        """;
    
    String detailsSql = """
        SELECT p.Name, p.Size, od.AddonName, od.Quantity, od.Subtotal, 
               od.BasePrice, od.AddonPrice 
        FROM order_details od 
        JOIN products p ON od.ProductID = p.ProductID 
        WHERE od.OrderID = ? ORDER BY od.Subtotal DESC
        """;

    try(Connection con = ConnectorXampp.connect();
        PreparedStatement orderPs = con.prepareStatement(orderSql);
        PreparedStatement detailsPs = con.prepareStatement(detailsSql)){

        orderPs.setInt(1, orderId);
        ResultSet orderRs = orderPs.executeQuery();
        
        double cashAmount = 0;
        double changeAmount = 0;
        
        if (orderRs.next()) {
            String orderDate = orderRs.getString("OrderDate").substring(0, 16);
            String orderType = orderRs.getString("OrderType");
            double totalAmount = orderRs.getDouble("TotalAmount");
            cashAmount = orderRs.getDouble("Cash");
            changeAmount = orderRs.getDouble("Change");
            
            String cashierName = orderRs.getString("CashierName");
            
            receipt.append("☕ ZEALLED BREWS ☕\n")
                   .append("============================\n")
                   .append("Order #").append(orderId).append("\n")
                   .append("Cashier: ").append(cashierName != null ? cashierName : "Cashier").append("\n")
                   .append("Date: ").append(orderDate).append("\n")
                   .append("Type: ").append(orderType).append("\n\n");
        }

        detailsPs.setInt(1, orderId);
        ResultSet rs = detailsPs.executeQuery();

        double subtotal = 0;
        while(rs.next()){
            String name = rs.getString("Name");
            String size = rs.getString("Size");
            String addon = rs.getString("AddonName");
            int qty = rs.getInt("Quantity");
            double itemSubtotal = rs.getDouble("Subtotal");
            
            subtotal += itemSubtotal;
            String itemLine = String.format("%-25s x%-2d ₱%.2f", 
                name + " (" + size + ")" + (addon.equals("None") ? "" : " +" + addon), 
                qty, itemSubtotal);
            receipt.append(itemLine).append("\n");
        }

        double tax = subtotal * 0.12;
        receipt.append("\n----------------------------\n")
               .append("Subtotal: ₱").append(String.format("%.2f", subtotal)).append("\n")
               .append("Tax(12%):  ₱").append(String.format("%.2f", tax)).append("\n")
               .append("TOTAL:     ₱").append(String.format("%.2f", subtotal + tax)).append("\n")
               .append("💵 Cash:    ₱").append(String.format("%.2f", cashAmount)).append("\n")
               .append("🔄 Change:  ₱").append(String.format("%.2f", changeAmount)).append("\n")
               .append("============================\n")
               .append("✅ Order Completed!\n")
               .append("Thank you for choosing\n")
               .append("   ZEALLED BREWS ☕");

        JOptionPane.showMessageDialog(
            this, 
            new javax.swing.JScrollPane(new javax.swing.JTextArea(receipt.toString())),
            "Receipt #"+orderId, 
            JOptionPane.INFORMATION_MESSAGE
        );

    }catch(Exception e){
        JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        e.printStackTrace();
    }
}
    
    private String getCashierNameForOrder(int orderId) {
    String sql = """
        SELECT u.full_name 
        FROM orders o 
        JOIN users u ON o.UserID = u.id 
        WHERE o.OrderID = ? AND u.role = 'Cashier'
        """;
    
    try (Connection con = ConnectorXampp.connect();
         PreparedStatement ps = con.prepareStatement(sql)) {
        
        ps.setInt(1, orderId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            String fullName = rs.getString("full_name");
            return fullName != null ? fullName : "Cashier";
        }
    } catch (Exception e) {
        System.out.println("Cashier lookup failed for Order #" + orderId + ": " + e.getMessage());
    }
    return "Cashier";
}
  

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
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        txtSearch = new javax.swing.JTextField();
        btnLoad = new javax.swing.JButton();
        btnViewDetails = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTableHistory = new javax.swing.JTable();

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

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        jLabel1.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        jLabel1.setText("Search:");

        txtSearch.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtSearchKeyReleased(evt);
            }
        });

        btnLoad.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnLoad.setText("Load");
        btnLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLoadActionPerformed(evt);
            }
        });

        btnViewDetails.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnViewDetails.setText("View Details");
        btnViewDetails.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnViewDetailsActionPerformed(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        jLabel2.setText("HISTORY");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 334, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(27, 27, 27)
                        .addComponent(btnLoad)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnViewDetails)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jLabel2)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addComponent(jLabel2)
                .addGap(27, 27, 27)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnLoad)
                    .addComponent(btnViewDetails))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTableHistory.setFont(new java.awt.Font("Serif", 0, 18)); // NOI18N
        jTableHistory.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "OrderID", "Date", "OrderDate", "OrderType", "Products", "TotalAmount"
            }
        ));
        jScrollPane1.setViewportView(jTableHistory);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1368, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(0, 394, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 674, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(39, 39, 39))
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

    private void btnViewDetailsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnViewDetailsActionPerformed
        // TODO add your handling code here:
        int row = jTableHistory.getSelectedRow();
    
    if(row == -1){
        JOptionPane.showMessageDialog(this, "📋 Select an order first!");
        return;
    }
    
    try {
        int orderId = (Integer) historyModel.getValueAt(row, 0);
        showOrderDetails(orderId);
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error viewing details: " + e.getMessage());
        e.printStackTrace();
    }
    }//GEN-LAST:event_btnViewDetailsActionPerformed

    private void btnLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLoadActionPerformed
        // TODO add your handling code here:
         loadOrders();
    }//GEN-LAST:event_btnLoadActionPerformed

    private void txtSearchKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtSearchKeyReleased
        // TODO add your handling code here:
        String search = txtSearch.getText().trim();
    historyModel.setRowCount(0);

    if(search.isEmpty()){
        loadOrders();
        return;
    }

    String sql = "SELECT o.OrderID, o.OrderDate, o.OrderType, " +
                 "GROUP_CONCAT(p.Name SEPARATOR ', ') AS Products, " +
                 "SUM(od.Subtotal) AS OrderSubtotal, " +
                 "o.TotalAmount " +
                 "FROM orders o " +
                 "JOIN order_details od ON o.OrderID = od.OrderID " +
                 "JOIN products p ON od.ProductID = p.ProductID " +
                 "WHERE o.OrderID LIKE ? " +
                 "   OR o.OrderDate LIKE ? " +
                 "   OR o.OrderType LIKE ? " +
                 "   OR p.Name LIKE ? " +
                 "GROUP BY o.OrderID " +
                 "ORDER BY o.OrderID DESC";

    try(Connection con = ConnectorXampp.connect();
        PreparedStatement pst = con.prepareStatement(sql)){
        
        String searchPattern = "%" + search + "%";
        pst.setString(1, searchPattern);
        pst.setString(2, searchPattern);
        pst.setString(3, searchPattern);
        pst.setString(4, searchPattern);
        
        ResultSet rs = pst.executeQuery();

        while(rs.next()){
            double subtotal = rs.getDouble("OrderSubtotal");
            double tax = subtotal * 0.12;
            double total = rs.getDouble("TotalAmount");
            
            String cashierName = getCashierNameForOrder(rs.getInt("OrderID"));
            
            historyModel.addRow(new Object[]{
                rs.getInt("OrderID"),
                rs.getString("OrderDate"),
                rs.getString("OrderType"),
                rs.getString("Products"),
                "₱" + String.format("%.2f", subtotal),
                "₱" + String.format("%.2f", tax),
                "₱" + String.format("%.2f", total),
                cashierName
            });
        }
    }catch(Exception e){
        JOptionPane.showMessageDialog(this,e.getMessage());
    }
    }//GEN-LAST:event_txtSearchKeyReleased

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
            java.util.logging.Logger.getLogger(History.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(History.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(History.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(History.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new History().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddons;
    private javax.swing.JButton btnCategory;
    private javax.swing.JButton btnDashBoard;
    private javax.swing.JButton btnHistory;
    private javax.swing.JButton btnLoad;
    private javax.swing.JButton btnLogOut;
    private javax.swing.JButton btnProducts;
    private javax.swing.JButton btnSize;
    private javax.swing.JButton btnUtilities;
    private javax.swing.JButton btnViewDetails;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTableHistory;
    private javax.swing.JLabel lblWelcome;
    private javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables
}
