/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package pkgfinal.zealled.brew;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import java.util.HashMap;
import java.util.Map;
import java.sql.*;
import javax.swing.Timer;
import java.sql.SQLException;
/**
 *
 * @author ASUS
 */
public class POS1 extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(POS1.class.getName());
    
    // POS variables (like first POS)
   private double subtotal = 0;
    private double taxRate = 0.10;
    private double total = 0;
    private String cashInput = "";
    private String receiptItems = "";
    private Map<String, Integer> itemQtyMap = new HashMap<>();
    private Map<String, Double> itemPriceMap = new HashMap<>();
    private Map<String, Integer> productIdMap = new HashMap<>();
    private Map<String, String[]> itemAddonsMap = new HashMap<>();
    private DefaultTableModel productModel;
    /**
     * Creates new form POS
     */
    public POS1() {
        initComponents();
       setupModels();
        loadPOSTables();
        showWelcomeDisplay();
        setupEventListeners();
        loadAddonsToComboBox();
        setupCombos();
    }
    
    private void setupEventListeners() {
        // Search
        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                searchProducts(txtSearch.getText().trim());
            }
        });
        
        // Color coding
        jTableProducts.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int column) {
                java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                try {
                    int stock = Integer.parseInt(table.getValueAt(row, 5).toString());
                    if (stock <= 0) c.setForeground(java.awt.Color.RED);
                    else if (stock <= 5) c.setForeground(java.awt.Color.ORANGE);
                    else c.setForeground(new java.awt.Color(0, 128, 0));
                } catch (Exception ignored) {}
                return c;
            }
        });
        
        txtDisplayPOS.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 16));
        txtDisplayPOS.setMargin(new java.awt.Insets(10, 20, 10, 10));
        txtDisplayPOS.setEditable(false);
    }
    
    private void setupCombos() {
        cmbOrderType.removeAllItems();
        cmbOrderType.addItem("Dine In");
        cmbOrderType.addItem("Take Out");
        cmbAddons.setSelectedIndex(0);
    }
    // 🔥 Find ProductID by display name (handles addons)
    private int findProductIdByName(String displayName) {
    try (Connection con = ConnectorXampp.connect();
         PreparedStatement ps = con.prepareStatement(
             "SELECT ProductID FROM products WHERE Name LIKE ? LIMIT 1")) {
        
        ps.setString(1, "%" + displayName.split("\\$")[0] + "%"); // Extract base name
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt("ProductID");
        }
    } catch (Exception e) {
        System.out.println("Find ProductID error: " + e.getMessage());
    }
    return -1;
}



// 🔥 Extract addon from display name
    private String extractAddonName(String displayName) {
    if (displayName.contains("+")) {
        return displayName.split("\\+")[1].trim();
    }
    return "None";
}
    
   private void setupModels() {
    // 🔥 MATCH Products form EXACTLY
    String[] columns = {"ID", "Name", "Category", "Size", "Selling Price", "Quantity"};
    productModel = new DefaultTableModel(columns, 0) { 
        public boolean isCellEditable(int row, int col) { return false; } 
    };
    jTableProducts.setModel(productModel);
    
    // 🔥 Add formatting like Products form
    formatPOSTableDisplay();
}
   
   private void formatPOSTableDisplay() {
    // 🔥 Selling Price column (index 4) - ₱0.00 format
    java.text.NumberFormat currencyFormat = new java.text.DecimalFormat("₱#,##0.00");
    javax.swing.table.TableCellRenderer priceRenderer = (javax.swing.JTable table, Object value, 
        boolean isSelected, boolean hasFocus, int row, int column) -> {
        javax.swing.JLabel label = new javax.swing.JLabel();
        if (value != null && value instanceof Integer) {
            double price = (Integer) value;
            label.setText(currencyFormat.format(price));
        } else {
            label.setText(value != null ? value.toString() : "₱0.00");
        }
        label.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        if (isSelected) {
            label.setBackground(table.getSelectionBackground());
            label.setForeground(table.getSelectionForeground());
        } else {
            label.setBackground(table.getBackground());
            label.setForeground(table.getForeground());
        }
        label.setOpaque(true);
        return label;
    };
    jTableProducts.getColumnModel().getColumn(4).setCellRenderer(priceRenderer);
    
    // 🔥 Quantity column (index 5) - Stock colors
    javax.swing.table.TableCellRenderer quantityRenderer = (javax.swing.JTable table, Object value, 
        boolean isSelected, boolean hasFocus, int row, int column) -> {
        javax.swing.JLabel label = new javax.swing.JLabel();
        if (value != null && value instanceof Integer) {
            int qty = (Integer) value;
            label.setText(qty == 0 ? "Out of Stock" : String.valueOf(qty));
            if (qty == 0) label.setForeground(java.awt.Color.RED);
            else if (qty <= 5) label.setForeground(java.awt.Color.ORANGE);
        } else {
            label.setText(value != null ? value.toString() : "0");
        }
        label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        if (isSelected) {
            label.setBackground(table.getSelectionBackground());
            label.setForeground(table.getSelectionForeground());
        } else {
            label.setBackground(table.getBackground());
            label.setForeground(table.getForeground());
        }
        label.setOpaque(true);
        return label;
    };
    jTableProducts.getColumnModel().getColumn(5).setCellRenderer(quantityRenderer);
}
    
    public void loadPOSTables() {
    productModel.setRowCount(0);
    String sql = "SELECT p.ProductID, p.Name, c.category_name, p.Size, p.Price, p.Quantity " +
                "FROM products p LEFT JOIN category c ON p.Category = c.category_name ORDER BY p.Name";
    try (Connection con = ConnectorXampp.connect(); Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
        while (rs.next()) {
            productModel.addRow(new Object[]{
                rs.getInt("ProductID"), 
                rs.getString("Name"), 
                rs.getString("category_name"),
                rs.getString("Size"), 
                rs.getInt("Price"),      // 🔥 RAW INT for formatting
                rs.getInt("Quantity")    // 🔥 RAW INT for formatting
            });
        }
        formatPOSTableDisplay();  // 🔥 Apply formatting
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage());
    }
}

    private void searchProducts(String keyword) {
    productModel.setRowCount(0);
    if (keyword.isEmpty()) { 
        loadPOSTables(); 
        return; 
    }
    String sql = "SELECT p.ProductID, p.Name, c.category_name, p.Size, p.Price, p.Quantity " +
                "FROM products p LEFT JOIN category c ON p.Category = c.category_name " +
                "WHERE p.Quantity > 0 AND (p.Name LIKE ? OR c.category_name LIKE ?) ORDER BY p.Name";
    try (Connection con = ConnectorXampp.connect(); PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setString(1, "%" + keyword + "%"); 
        ps.setString(2, "%" + keyword + "%");
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            productModel.addRow(new Object[]{
                rs.getInt("ProductID"), 
                rs.getString("Name"), 
                rs.getString("category_name"),
                rs.getString("Size"), 
                rs.getInt("Price"),      // 🔥 RAW INT
                rs.getInt("Quantity")    // 🔥 RAW INT
            });
        }
        formatPOSTableDisplay();  // 🔥 Apply formatting
    } catch (Exception e) {
        System.out.println("Search failed: " + e.getMessage());
    }
}
    
    private void loadAddonsToComboBox() {
        cmbAddons.removeAllItems();
        cmbAddons.addItem("None");
        String sql = "SELECT Name FROM addons ORDER BY Name";
        try (Connection con = ConnectorXampp.connect(); Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) cmbAddons.addItem(rs.getString("Name"));
        } catch (Exception e) {
            System.out.println("Addons load failed: " + e.getMessage());
        }
    }
    
    private double getAddonPrice(String addonName) {  // 🔥 Changed to DOUBLE
    if (addonName.equals("None")) return 0;
    
    String sql = "SELECT Price FROM addons WHERE Name = ?";  // 🔥 Exact match
    try (Connection con = ConnectorXampp.connect();
         PreparedStatement pst = con.prepareStatement(sql)) {
        pst.setString(1, addonName);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return rs.getDouble("Price");  // 🔥 Use getDouble() not getInt()
        }
    } catch (Exception e) {
        System.out.println("Addon price error: " + e.getMessage());
    }
    return 0;
}
    /** 🔥 MAIN PRODUCT SELECTION - WORKS PERFECTLY */
    
  private void updateReceiptDisplay() {
    receiptItems = "";
    for (String item : itemQtyMap.keySet()) {
        int qty = itemQtyMap.get(item);
        double price = itemPriceMap.get(item);
        receiptItems += String.format("%-25s x%-2d ₱%.2f\n", item, qty, price * qty);
    }
    
    double tax = subtotal * taxRate;
    total = subtotal + tax;
    
    // 🔥 ADD ORDER TYPE TO RECEIPT
    String orderType = cmbOrderType.getSelectedItem() != null ? 
                       cmbOrderType.getSelectedItem().toString() : "Dine In";
    
    txtDisplayPOS.setText(
        "☕ ZEALLED BREWS ☕\n" +
        "============================\n" +
        "Type: " + orderType + " \n\n" +  // 🔥 ORDER TYPE DISPLAYED
        receiptItems +
        "\n----------------------------\n" +
        "Subtotal: ₱" + String.format("%.2f", subtotal) + "\n" +
        "Tax(10%):  ₱" + String.format("%.2f", tax) + "\n" +
        "TOTAL:     ₱" + String.format("%.2f", total) + "\n" +
        (cashInput.isEmpty() ? "" : "\n💵 Cash: ₱" + cashInput)
    );
    txtDisplayPOS.setCaretPosition(txtDisplayPOS.getDocument().getLength());
}
    
  private void showWelcomeDisplay() {
    txtDisplayPOS.setText(
        "☕ ZEALLED BREWS POS ☕\n" +
        "============================\n" +
        "Type: Dine In/Take Out\n\n" +  // 🔥 Consistent format
        "👆 CLICK PRODUCT\n" +
        "➕ SELECT ADDON (optional)\n" +
        "🔢 ENTER QUANTITY\n" +
        "💰 USE CALCULATOR\n" +
        "✅ PRESS PAY\n\n" +
        "----------------------------\nReady!"
    );
}
    
    private void appendCash(String digit) {
        cashInput += digit;
        updateReceiptDisplay();
    }
    
   private void processPayment() {
    if (itemQtyMap.isEmpty()) {
        JOptionPane.showMessageDialog(this, "📦 Add items first!");
        return;
    }
    if (cashInput.isEmpty()) {
        JOptionPane.showMessageDialog(this, "💰 Enter cash amount!");
        return;
    }
    
    try {
        double cash = Double.parseDouble(cashInput);
        if (cash < total) {
            JOptionPane.showMessageDialog(this, String.format("💳 Need ₱%.2f more!", total - cash));
            return;
        }
        
        if (JOptionPane.showConfirmDialog(this, 
            "💰 Total: ₱" + String.format("%.2f", total) + "\n" +
            "💵 Cash:  ₱" + String.format("%.2f", cash) + "\n" +
            "🔄 Change:₱" + String.format("%.2f", cash - total),
            "Confirm Payment", JOptionPane.YES_OPTION) == JOptionPane.YES_OPTION) {
            
            saveOrder(cash, cash - total);
            
            // ✅ FIXED: Show COMPLETE receipt first, THEN reset
            String finalReceipt = txtDisplayPOS.getText() + 
                "\n============================\n" +
                "✅ PAID! THANK YOU! ☕\n" +
                "🔔 Receipt printed\n" +
                "🔄 Resetting POS...";
            
            txtDisplayPOS.setText(finalReceipt);
            txtDisplayPOS.setCaretPosition(txtDisplayPOS.getDocument().getLength());
            
            // Auto reset after 3 seconds (longer delay)
            new Timer(3000, e -> {
                resetPOS();
                ((Timer)e.getSource()).stop(); // Stop timer
            }).start();
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "❌ Invalid cash amount!");
    }
}
    
    private void saveOrder(double cash, double change) {
    java.sql.Connection con = null;  // 🔥 DECLARE con HERE
    try {
        con = ConnectorXampp.connect();  // 🔥 ASSIGN con
        con.setAutoCommit(false);
        
        // 1. Save ORDER
        PreparedStatement ps = con.prepareStatement(
            "INSERT INTO orders (`OrderDate`, `TotalAmount`, `OrderType`, `Cash`, `Change`) VALUES(NOW(), ?, ?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS
        );
        ps.setDouble(1, total);
        ps.setString(2, cmbOrderType.getSelectedItem().toString());
        ps.setDouble(3, cash);
        ps.setDouble(4, change);
        ps.executeUpdate();
        
        ResultSet rs = ps.getGeneratedKeys();
        int orderId = rs.next() ? rs.getInt(1) : -1;
        
        // 🔥 2. LOOP THROUGH ALL ITEMS & DEDUCT STOCK + SAVE DETAILS
        for (Map.Entry<String, Integer> entry : productIdMap.entrySet()) {
            String itemKey = entry.getKey();  // "ID_5"
            int productId = entry.getValue(); // 5
            
            // 🔥 Find matching displayName
            String displayName = null;
            for (String name : itemQtyMap.keySet()) {
                if (name.contains(itemKey.replace("ID_", ""))) {
                    displayName = name;
                    break;
                }
            }
            
            if (displayName == null) {
                System.out.println("❌ No displayName for ProductID: " + productId);
                continue;
            }
            
            int qty = itemQtyMap.get(displayName);
            
            // 🔥 STEP 1: DEDUCT STOCK FIRST!
            String stockSql = "UPDATE products SET Quantity = Quantity - ? WHERE ProductID = ?";
            try (PreparedStatement stockPs = con.prepareStatement(stockSql)) {
                stockPs.setInt(1, qty);
                stockPs.setInt(2, productId);
                int updated = stockPs.executeUpdate();
                System.out.println("📦 STOCK UPDATED: ProductID=" + productId + ", Sold=" + qty);
            }
            
            // 🔥 STEP 2: Get addon info (FIRST addon only for DB)
            String[] allAddons = itemAddonsMap.get(displayName);
            String firstAddon = (allAddons != null && allAddons.length > 0) ? allAddons[0] : "None";
            double addonPrice = getAddonPrice(firstAddon);
            double basePrice = getBasePrice(productId);
            double unitPrice = itemPriceMap.get(displayName);
            
            // 🔥 STEP 3: Save order detail
            String detailSql = "INSERT INTO order_details (OrderID, ProductID, AddonName, AddonPrice, BasePrice, Quantity, Subtotal) VALUES(?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement detailPs = con.prepareStatement(detailSql)) {
                detailPs.setInt(1, orderId);
                detailPs.setInt(2, productId);
                detailPs.setString(3, firstAddon);
                detailPs.setDouble(4, addonPrice);
                detailPs.setDouble(5, basePrice);
                detailPs.setInt(6, qty);
                detailPs.setDouble(7, unitPrice * qty);
                detailPs.executeUpdate();
            }
        }
        
        // 🔥 COMMIT TRANSACTION
        con.commit();
        
        JOptionPane.showMessageDialog(this, 
            "✅ Order #" + orderId + " PAID!\n📦 STOCK DEDUCTED!\n💾 SAVED!"
        );
        
        Products.refreshAllStock();
        
    } catch (Exception e) {
        // 🔥 ROLLBACK - con is now in scope!
        if (con != null) {
            try {
                con.rollback();
                System.out.println("🔄 Transaction rolled back");
            } catch (Exception rollbackEx) {
                System.out.println("Rollback failed: " + rollbackEx.getMessage());
            }
        }
        JOptionPane.showMessageDialog(this, "❌ Error: " + e.getMessage());
        e.printStackTrace();
    } finally {
        // 🔥 CLOSE CONNECTION
        if (con != null) {
            try {
                con.close();
            } catch (Exception closeEx) {
                System.out.println("Close failed: " + closeEx.getMessage());
            }
        }
    }
}

 // 🔥 Helper method (add this anywhere in POS class)
    private String findDisplayNameForProductId(int productId) {
    String idStr = "ID_" + productId;
    for (String displayName : itemQtyMap.keySet()) {
        if (displayName.contains(idStr.replace("ID_", ""))) {
            return displayName;
        }
    }
    return null;
}
 
// 🔥 Helper: Get base price from DB
    private double getBasePrice(int productId) {  // 🔥 Changed to double
    try (Connection con = ConnectorXampp.connect();
         PreparedStatement ps = con.prepareStatement("SELECT Price FROM products WHERE ProductID = ?")) {
        ps.setInt(1, productId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt("Price");  // Products Price is INT
    } catch (Exception e) {
        System.out.println("Base price error: " + e.getMessage());
    }
    return 0;
}

// Helper method to find product row by ID
    private int getProductRow(int productId) {
    for (int i = 0; i < jTableProducts.getRowCount(); i++) {
        if ((Integer) jTableProducts.getValueAt(i, 0) == productId) {
            return i;
        }
    }
    return -1;
}
    
   private void resetPOS() {
    subtotal = total = 0; 
    cashInput = receiptItems = "";
    itemQtyMap.clear(); 
    itemPriceMap.clear(); 
    productIdMap.clear();
     itemAddonsMap.clear(); 
    
    txtSearch.setText(""); 
    cmbAddons.setSelectedIndex(0);
    
    // Smooth transition
    txtDisplayPOS.setText("🔄 POS Reset Complete!\nReady for next order...");
    new Timer(1000, e -> {
        showWelcomeDisplay();
        loadPOSTables();
        ((Timer)e.getSource()).stop();
    }).start();
}
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane3 = new javax.swing.JScrollPane();
        jTableCart = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtDisplayPOS = new javax.swing.JTextArea();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTableProducts = new javax.swing.JTable();
        jPanel2 = new javax.swing.JPanel();
        btnPAY = new javax.swing.JButton();
        btnErase = new javax.swing.JButton();
        btn0 = new javax.swing.JButton();
        btn1 = new javax.swing.JButton();
        btn2 = new javax.swing.JButton();
        btn3 = new javax.swing.JButton();
        btn4 = new javax.swing.JButton();
        btn5 = new javax.swing.JButton();
        btn6 = new javax.swing.JButton();
        btn7 = new javax.swing.JButton();
        btn8 = new javax.swing.JButton();
        btn9 = new javax.swing.JButton();
        btnAC = new javax.swing.JButton();
        txtSearch = new javax.swing.JTextField();
        cmbOrderType = new javax.swing.JComboBox<>();
        cmbAddons = new javax.swing.JComboBox<>();
        btnLogOut = new javax.swing.JButton();
        btnAddAddons = new javax.swing.JButton();

        jTableCart.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null}
            },
            new String [] {
                "Item No", "ProductID", "Name", "Category", "Size", "Selling Price", "addonName", "addonPrice", "Price", "Quantity", "Subtotal"
            }
        ));
        jScrollPane3.setViewportView(jTableCart);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(0, 0, 0));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        txtDisplayPOS.setColumns(20);
        txtDisplayPOS.setRows(5);
        jScrollPane1.setViewportView(txtDisplayPOS);

        jPanel1.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(6, 6, 446, 570));

        jTableProducts.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "ProductID", "Name", "Category", "Size", "Price", "Quantity"
            }
        ));
        jTableProducts.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTableProductsMouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(jTableProducts);

        jPanel1.add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(458, 6, 1010, 480));

        jPanel2.setForeground(new java.awt.Color(51, 51, 51));

        btnPAY.setText("Pay");
        btnPAY.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPAYActionPerformed(evt);
            }
        });

        btnErase.setText("<--");
        btnErase.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEraseActionPerformed(evt);
            }
        });

        btn0.setText("0");
        btn0.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn0ActionPerformed(evt);
            }
        });

        btn1.setText("1");
        btn1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn1ActionPerformed(evt);
            }
        });

        btn2.setText("2");
        btn2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn2ActionPerformed(evt);
            }
        });

        btn3.setText("3");
        btn3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn3ActionPerformed(evt);
            }
        });

        btn4.setText("4");
        btn4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn4ActionPerformed(evt);
            }
        });

        btn5.setText("5");
        btn5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn5ActionPerformed(evt);
            }
        });

        btn6.setText("6");
        btn6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn6ActionPerformed(evt);
            }
        });

        btn7.setText("7");
        btn7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn7ActionPerformed(evt);
            }
        });

        btn8.setText("8");
        btn8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn8ActionPerformed(evt);
            }
        });

        btn9.setText("9");
        btn9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn9ActionPerformed(evt);
            }
        });

        btnAC.setText("AC");
        btnAC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnACActionPerformed(evt);
            }
        });

        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtSearchKeyReleased(evt);
            }
        });

        cmbOrderType.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Dine In", "Take Out" }));

        cmbAddons.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "None", "Black Pearl", "Topping Boba", "Fruity Jelly", "Yakult", "Oreo", "Lychee" }));

        btnLogOut.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnLogOut.setText("Log Out");
        btnLogOut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLogOutActionPerformed(evt);
            }
        });

        btnAddAddons.setText("Add");
        btnAddAddons.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddAddonsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(14, 14, 14)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 477, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 30, Short.MAX_VALUE)
                                .addComponent(btn1, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(cmbOrderType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(cmbAddons, javax.swing.GroupLayout.PREFERRED_SIZE, 164, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(btnAddAddons)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btn4, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(164, 164, 164)
                        .addComponent(btnPAY, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn7, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnLogOut, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnAC, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(btn5, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btn6, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(btn2, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btn3, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btn8, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn0, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnErase, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn9, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(81, 81, 81))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(35, 35, 35)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btn1, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn2, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn3, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btn4, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn5, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn6, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btn7, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn8, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn9, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnPAY, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnAC, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnErase, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn0, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(cmbOrderType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(cmbAddons, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnAddAddons))
                        .addGap(103, 103, 103)
                        .addComponent(btnLogOut)))
                .addContainerGap(112, Short.MAX_VALUE))
        );

        jPanel1.add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 500, 890, 330));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 880, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btn1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn1ActionPerformed
        // TODO add your handling code here:
        appendCash("1");
    }//GEN-LAST:event_btn1ActionPerformed

    private void btn2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn2ActionPerformed
        // TODO add your handling code here:
        appendCash("2");
    }//GEN-LAST:event_btn2ActionPerformed

    private void btn3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn3ActionPerformed
        // TODO add your handling code here:
        appendCash("3");
    }//GEN-LAST:event_btn3ActionPerformed

    private void btn4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn4ActionPerformed
        // TODO add your handling code here:
        appendCash("4");
    }//GEN-LAST:event_btn4ActionPerformed

    private void btn5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn5ActionPerformed
        // TODO add your handling code here:
        appendCash("5");
    }//GEN-LAST:event_btn5ActionPerformed

    private void btn6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn6ActionPerformed
        // TODO add your handling code here:
        appendCash("6");
    }//GEN-LAST:event_btn6ActionPerformed

    private void btn7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn7ActionPerformed
        // TODO add your handling code here:
        appendCash("7");
    }//GEN-LAST:event_btn7ActionPerformed

    private void btn8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn8ActionPerformed
        // TODO add your handling code here:
        appendCash("8");
    }//GEN-LAST:event_btn8ActionPerformed

    private void btn9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn9ActionPerformed
        // TODO add your handling code here:
        appendCash("9");
    }//GEN-LAST:event_btn9ActionPerformed

    private void btn0ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn0ActionPerformed
        // TODO add your handling code here:
        appendCash("0");
    }//GEN-LAST:event_btn0ActionPerformed

    private void btnEraseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEraseActionPerformed
        // TODO add your handling code here:
           if (cashInput.length() > 0) cashInput = cashInput.substring(0, cashInput.length() - 1);
        updateReceiptDisplay();
    }//GEN-LAST:event_btnEraseActionPerformed

    private void btnACActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnACActionPerformed
        // TODO add your handling code here:
        resetPOS();
    }//GEN-LAST:event_btnACActionPerformed

    private void btnPAYActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPAYActionPerformed
        // TODO add your handling code here:
         processPayment();
    }//GEN-LAST:event_btnPAYActionPerformed

    private void txtSearchKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtSearchKeyReleased
        // TODO add your handling code here:
          searchProducts(txtSearch.getText().trim());
    }//GEN-LAST:event_txtSearchKeyReleased

    private void jTableProductsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTableProductsMouseClicked
        // TODO add your handling code here:
     int row = jTableProducts.getSelectedRow();
    if (row < 0) return;
    
    int id = (Integer) jTableProducts.getValueAt(row, 0);
    String name = jTableProducts.getValueAt(row, 1).toString();
    String size = jTableProducts.getValueAt(row, 3).toString();
    int price = (Integer) jTableProducts.getValueAt(row, 4);  // Base price: 45
    int stock = (Integer) jTableProducts.getValueAt(row, 5);
    
    if (stock <= 0) {
        JOptionPane.showMessageDialog(this, name + " ❌ OUT OF STOCK");
        return;
    }
    
    String addon = (String) cmbAddons.getSelectedItem();
    double addonPrice = getAddonPrice(addon);  // 🔥 Now returns DOUBLE
    
    // 🔥 DEBUG: Check what prices we're getting
    System.out.println("🧾 DEBUG - Base: ₱" + price + ", Addon: '" + addon + "' = ₱" + addonPrice);
    
    String qtyText = JOptionPane.showInputDialog(
        this, 
        "🍵 " + name + "\n📏 Size: " + size + "\n💰 Price: ₱" + price + 
        (addon.equals("None") ? "" : "\n➕ " + addon + ": ₱" + String.format("%.2f", addonPrice)) + 
        "\n📦 Stock: " + stock + "\n\nEnter Quantity:"
    );
    
    if (qtyText == null || qtyText.trim().isEmpty()) return;
    
    try {
        int qty = Integer.parseInt(qtyText.trim());
        if (qty <= 0 || qty > stock) {
            JOptionPane.showMessageDialog(this, "❌ Quantity 1 to " + stock);
            return;
        }
        
        String itemKey = "ID_" + id;
        String displayName = name + " (" + size + ")" + (addon.equals("None") ? "" : " +" + addon);
        double unitPrice = price + addonPrice;  // 🔥 FIXED: 45 + 20 = 65
        
        // 🔥 DEBUG FINAL PRICE
        System.out.println("💰 FINAL unitPrice: ₱" + String.format("%.2f", unitPrice) + " x " + qty);
        
        subtotal += unitPrice * qty;
        itemQtyMap.put(displayName, qty);
        itemPriceMap.put(displayName, unitPrice);
        productIdMap.put(itemKey, id);
        
        if (!addon.equals("None")) {
            itemAddonsMap.put(displayName, new String[]{addon});
        }
        
        System.out.println("🛒 ADDED: " + displayName + " @ ₱" + unitPrice + " x " + qty);
        updateReceiptDisplay();
        cmbAddons.setSelectedIndex(0);
        
    } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(this, "❌ Enter number only!");
    }
    }//GEN-LAST:event_jTableProductsMouseClicked

    private void btnLogOutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLogOutActionPerformed
        // TODO add your handling code here:
        Login h = new Login();
        h.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnLogOutActionPerformed

    private void btnAddAddonsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddAddonsActionPerformed
        // TODO add your handling code here:
      int row = jTableProducts.getSelectedRow();
    if (row < 0) {
        JOptionPane.showMessageDialog(this, "👆 Select a product first!");
        return;
    }
    
    int id = (Integer) jTableProducts.getValueAt(row, 0);
    String name = jTableProducts.getValueAt(row, 1).toString();
    String size = jTableProducts.getValueAt(row, 3).toString();
    String baseItemKey = "ID_" + id;
    
    String addon = (String) cmbAddons.getSelectedItem();
    if (addon.equals("None")) {
        JOptionPane.showMessageDialog(this, "➕ Select an addon first!");
        return;
    }
    
    // 🔥 Find product already in cart
    String displayName = name + " (" + size + ")";
    String currentAddonsStr = "";
    
    // Check if product exists in cart
    for (String cartItem : itemQtyMap.keySet()) {
        if (cartItem.contains(baseItemKey.replace("ID_", ""))) {
            displayName = cartItem;
            String[] existingAddons = itemAddonsMap.get(cartItem);
            if (existingAddons != null) {
                currentAddonsStr = String.join(" + ", existingAddons);
            }
            break;
        }
    }
    
    // 🔥 Show preview
    String newAddonStr = currentAddonsStr.isEmpty() ? addon : currentAddonsStr + " + " + addon;
    String finalDisplayName = displayName + " +" + newAddonStr;
    
    // 🔥 CALCULATE NEW PRICE
    double basePrice = getBasePrice(id);
    double totalAddonPrice = getAddonPrice(addon);  // New addon
    
    // Add existing addons prices
    String[] existingAddons = itemAddonsMap.get(displayName);
    if (existingAddons != null) {
        for (String exAddon : existingAddons) {
            totalAddonPrice += getAddonPrice(exAddon);
        }
    }
    
    double newUnitPrice = basePrice + totalAddonPrice;
    
    int confirm = JOptionPane.showConfirmDialog(
        this, 
        "☕ " + displayName + "\n" +
        "➕ Current: " + currentAddonsStr + "\n" +
        "➕ Add: " + addon + "\n\n" +
        "💰 New Price: ₱" + String.format("%.2f", newUnitPrice) + "\n\n" +
        "Confirm?",
        "Add Addon", 
        JOptionPane.YES_NO_OPTION
    );
    
    if (confirm == JOptionPane.YES_OPTION) {
        // 🔥 Update or create addon array
        String[] addonsArray;
        if (existingAddons != null) {
            addonsArray = new String[existingAddons.length + 1];
            System.arraycopy(existingAddons, 0, addonsArray, 0, existingAddons.length);
            addonsArray[existingAddons.length] = addon;
        } else {
            addonsArray = new String[]{addon};
        }
        
        // 🔥 UPDATE CART WITH NEW PRICE
        if (itemQtyMap.containsKey(displayName)) {
            int qty = itemQtyMap.get(displayName);
            double oldPrice = itemPriceMap.get(displayName);
            
            // 🔥 Update subtotal
            subtotal += (newUnitPrice - oldPrice) * qty;
            
            itemQtyMap.remove(displayName);
            itemPriceMap.remove(displayName);
            itemAddonsMap.remove(displayName);
            
            itemQtyMap.put(finalDisplayName, qty);
            itemPriceMap.put(finalDisplayName, newUnitPrice);  // ✅ NEW PRICE
            itemAddonsMap.put(finalDisplayName, addonsArray);
        }
        
        // Update productIdMap
        if (productIdMap.containsKey(baseItemKey)) {
            productIdMap.put(finalDisplayName, productIdMap.get(baseItemKey));
        }
        
        System.out.println("➕ ADDED: " + finalDisplayName + " @ ₱" + newUnitPrice);
        updateReceiptDisplay();  // 🔥 AUTO UPDATE RECEIPT
        cmbAddons.setSelectedIndex(0);
    }
    }//GEN-LAST:event_btnAddAddonsActionPerformed

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
            java.util.logging.Logger.getLogger(POS1.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(POS1.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(POS1.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(POS1.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new POS1().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn0;
    private javax.swing.JButton btn1;
    private javax.swing.JButton btn2;
    private javax.swing.JButton btn3;
    private javax.swing.JButton btn4;
    private javax.swing.JButton btn5;
    private javax.swing.JButton btn6;
    private javax.swing.JButton btn7;
    private javax.swing.JButton btn8;
    private javax.swing.JButton btn9;
    private javax.swing.JButton btnAC;
    private javax.swing.JButton btnAddAddons;
    private javax.swing.JButton btnErase;
    private javax.swing.JButton btnLogOut;
    private javax.swing.JButton btnPAY;
    private javax.swing.JComboBox<String> cmbAddons;
    private javax.swing.JComboBox<String> cmbOrderType;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTable jTableCart;
    private javax.swing.JTable jTableProducts;
    private javax.swing.JTextArea txtDisplayPOS;
    private javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables
}
