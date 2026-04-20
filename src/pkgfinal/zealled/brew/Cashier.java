/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package pkgfinal.zealled.brew;
import javax.swing.JOptionPane;
/**
 *
 * @author ASUS
 */
public class Cashier extends javax.swing.JFrame {
    private int editingAddonId = -1;

    // Model for the main product table
   // Model for the main product table
    private javax.swing.table.DefaultTableModel productModel;
    
    // Model for the cart table
    private javax.swing.table.DefaultTableModel cartModel;
    
    // Order variables - MAKE THESE CLASS VARIABLES
    private double totalAmount = 0;
    private double payment = 0;  // <-- CLASS VARIABLE
    private double change = 0;
    private int orderCount = 1;
    /**
     * Creates new form Cashier
     */
    public Cashier() {
        initComponents(); 
        setupProductModel();
        setupCartModel();
        loadAddonsToComboBox();
        loadAllProducts();
        cmbOrderType.removeAllItems();
        cmbOrderType.addItem("Dine In");
        cmbOrderType.addItem("Take Out");
        lblOrderNumber.setText("Order #" + orderCount);
        calculateTotal();
    }
    
    
   
    private String getOrderType(){
    return cmbOrderType.getSelectedItem().toString();
}
    
    private void loadAddonsToComboBox() {
    cmbAddons.removeAllItems();
    cmbAddons.addItem("None");
    
    String sql = "SELECT Name FROM addons";
    try (java.sql.Connection con = ConnectorXampp.connect();
         java.sql.Statement st = con.createStatement();
         java.sql.ResultSet rs = st.executeQuery(sql)) {
        while (rs.next()) {
            cmbAddons.addItem(rs.getString("Name"));
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error loading addons: " + e.getMessage());
    }
}
    
    private void setupProductModel() {
        // Column name changed from "Stock" to "Quantity"
        String[] columnNames = {
            "ProductID", "Name", "Category", "Size", "Price", "Quantity"
        };
        
        productModel = new javax.swing.table.DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        jTableProducts.setModel(productModel);
    }
    
    private void setupCartModel() {
        String[] columnNames = {
            "Item No", "ProductID", "Name", "Category", "Size", "Selling Price", "Addon", "Addon Price", "Quantity", "Subtotal"
        };
        
        cartModel = new javax.swing.table.DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        jTableCart.setModel(cartModel);
    }
    
   private void loadAllProducts() {
    loadAllProducts("");  // Call with empty string
}

    private void loadAllProducts(String search) {
    productModel.setRowCount(0);
    
    // ✅ Fixed to match your schema
    String sql = "SELECT p.ProductID, p.Name, c.category_name, p.Size, p.Price, p.Quantity " +
                 "FROM products p " +
                 "LEFT JOIN category c ON p.Category = c.category_name " +
                 "WHERE p.Quantity > 0 " +
                 "AND (p.Name LIKE ? OR c.category_name LIKE ?) " +
                 "ORDER BY c.category_name, p.Name";
    
    try (java.sql.Connection con = ConnectorXampp.connect();
         java.sql.PreparedStatement pst = con.prepareStatement(sql)) {
        
        pst.setString(1, "%" + search + "%");
        pst.setString(2, "%" + search + "%");
        java.sql.ResultSet rs = pst.executeQuery();
        
        while (rs.next()) {
            productModel.addRow(new Object[]{
                rs.getInt("ProductID"),
                rs.getString("Name"),
                rs.getString("category_name"),  // ✅ Changed from CategoryName
                rs.getString("Size"),
                rs.getInt("Price"),
                rs.getInt("Quantity")
            });
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error loading products: " + e.getMessage());
    }
}
    
    private int getAvailableStock(int productId) {
        String sql = "SELECT Quantity FROM products WHERE ProductID = ?";
        
        try (java.sql.Connection con = ConnectorXampp.connect();
             java.sql.PreparedStatement pst = con.prepareStatement(sql)) {
            
            pst.setInt(1, productId);
            java.sql.ResultSet rs = pst.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("Quantity");
            }
            
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Error checking stock: " + e.getMessage());
        }
        
        return 0;
    }
    
   private int getAddonPrice() {
    String selectedAddon = String.valueOf(cmbAddons.getSelectedItem());
    
    // Return 0 if "None" is selected
    if (selectedAddon.equals("None")) {
        return 0;
    }
    
    String sql = "SELECT Price FROM addons WHERE Name = ?";
    
    try (java.sql.Connection con = ConnectorXampp.connect();
         java.sql.PreparedStatement pst = con.prepareStatement(sql)) {
        
        pst.setString(1, selectedAddon);
        java.sql.ResultSet rs = pst.executeQuery();
        
        if (rs.next()) {
            return rs.getInt("Price");  // ✅ Verify column name is "Price"
        }
        
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error getting addon price: " + e.getMessage());
    }
    
    return 0;  // ✅ Default return if not found
}
    
    private String getAddonName() {
    String selectedAddon = String.valueOf(cmbAddons.getSelectedItem());
    if (selectedAddon.equals("None")) return "None";
    return selectedAddon;  // ✅ No database query needed
}
    
    private void addToCart() {
    int selectedRow = jTableProducts.getSelectedRow();
    if (selectedRow == -1) {
        javax.swing.JOptionPane.showMessageDialog(this, "Please select a product to add");
        return;
    }
    
    // ✅ Product Table has 6 columns (0-5)
    int productId = (int) productModel.getValueAt(selectedRow, 0);
    String name = (String) productModel.getValueAt(selectedRow, 1);
    String category = (String) productModel.getValueAt(selectedRow, 2);
    String size = (String) productModel.getValueAt(selectedRow, 3);
    int basePrice = (int) productModel.getValueAt(selectedRow, 4);
    int availableStock = (int) productModel.getValueAt(selectedRow, 5);
    
    // Get addon details
    String addonName = getAddonName();
    int addonPrice = getAddonPrice();
    
    // Validate quantity
    int quantity;
    try {
        quantity = Integer.parseInt(txtQuantity.getText());
        if (quantity <= 0) {
            javax.swing.JOptionPane.showMessageDialog(this, "Please enter a valid quantity");
            return;
        }
    } catch (NumberFormatException e) {
        javax.swing.JOptionPane.showMessageDialog(this, "Please enter a valid quantity");
        return;
    }
    
    // Check available stock
    int currentStock = getAvailableStock(productId);
    
    // Check if item already in cart
    int alreadyInCart = 0;
    for (int i = 0; i < cartModel.getRowCount(); i++) {
        int existingProductId = (int) cartModel.getValueAt(i, 1);  // ✅ Index 1
        String existingAddon = (String) cartModel.getValueAt(i, 6);  // ✅ Index 6
        
        if (existingProductId == productId && existingAddon.equals(addonName)) {
            alreadyInCart = (int) cartModel.getValueAt(i, 8);  // ✅ Index 8 (Quantity)
            break;
        }
    }
    
    // Check stock availability
    int remainingStock = currentStock - alreadyInCart;
    
    if (quantity > remainingStock) {
        javax.swing.JOptionPane.showMessageDialog(this, 
            "Insufficient stock!\n\n" +
            "Product: " + name + "\n" +
            "Available Stock: " + remainingStock + "\n" +
            "Requested: " + quantity,
            "Stock Alert",
            javax.swing.JOptionPane.WARNING_MESSAGE);
        return;
    }
    
    // Calculate totals
    int totalPrice = basePrice + addonPrice;
    double subtotal = totalPrice * quantity;
    
    // Check if item with same product and addon already exists in cart
    for (int i = 0; i < cartModel.getRowCount(); i++) {
        int existingProductId = (int) cartModel.getValueAt(i, 1);  // ✅ Index 1
        String existingAddon = (String) cartModel.getValueAt(i, 6);  // ✅ Index 6
        
        if (existingProductId == productId && existingAddon.equals(addonName)) {
            // Update existing item quantity
            int currentQty = (int) cartModel.getValueAt(i, 8);  // ✅ Index 8
            int newQty = currentQty + quantity;
            double newSubtotal = totalPrice * newQty;
            
            cartModel.setValueAt(newQty, i, 8);  // ✅ Index 8 (Quantity)
            cartModel.setValueAt(newSubtotal, i, 9);  // ✅ Index 9 (Subtotal)
            
            calculateTotal();
            
            // Reset addon and quantity
            cmbAddons.setSelectedIndex(0);
            txtQuantity.setText("1");
            loadAllProducts();
            return;
        }
    }
    
    // Add new item to cart
    int itemNo = cartModel.getRowCount() + 1;
    cartModel.addRow(new Object[]{
        itemNo,           // 0 - Item No
        productId,        // 1 - ProductID
        name,             // 2 - Name
        category,         // 3 - Category
        size,             // 4 - Size
        basePrice,        // 5 - Base Price
        addonName,        // 6 - Addon
        addonPrice,       // 7 - Addon Price
        quantity,         // 8 - Quantity
        subtotal          // 9 - Subtotal
    });
    
    calculateTotal();
    
    // Reset addon and quantity
    cmbAddons.setSelectedIndex(0);
    txtQuantity.setText("1");
    loadAllProducts();
}
    
    private void removeFromCart() {
        int selectedRow = jTableCart.getSelectedRow();
        if (selectedRow == -1) {
            javax.swing.JOptionPane.showMessageDialog(this, "Please select an item to remove");
            return;
        }
        
        cartModel.removeRow(selectedRow);
        
        // Renumber items
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            cartModel.setValueAt(i + 1, i, 0);
        }
        
        calculateTotal();
        loadAllProducts();
    }
    
    private void clearCart() {
        int confirm = javax.swing.JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to clear the cart?", 
            "Confirm Clear", 
            javax.swing.JOptionPane.YES_NO_OPTION);
        
        if (confirm == javax.swing.JOptionPane.YES_OPTION) {
            cartModel.setRowCount(0);
            calculateTotal();
            loadAllProducts();
        }
    }
    
   private void calculateTotal() {
    totalAmount = 0;
    
    for (int i = 0; i < cartModel.getRowCount(); i++) {
        totalAmount += (double) cartModel.getValueAt(i, 9);
    }
    
    lblTotal.setText("₱" + String.format("%.2f", totalAmount));  // 👈 ₱ added
}
    
    private void calculateChange() {
    if (txtPayment.getText().isEmpty()) {
        txtChange.setText("₱0.00");  // 👈 ₱ added
        return;
    }

    try {
        payment = Double.parseDouble(txtPayment.getText());
        change = payment - totalAmount;

        if (change < 0) {
            txtChange.setText("Insufficient");
        } else {
            txtChange.setText("₱" + String.format("%.2f", change));  // 👈 ₱ added
        }
    } catch (NumberFormatException e) {
        txtChange.setText("₱0.00");  // 👈 ₱ added
    }
}
    
    private void processPayment() {
        if (cartModel.getRowCount() == 0) {
            javax.swing.JOptionPane.showMessageDialog(this, "Cart is empty!");
            return;
        }
        
        try {
            payment = Double.parseDouble(txtPayment.getText());
            if (payment < totalAmount) {
                javax.swing.JOptionPane.showMessageDialog(this, "Insufficient payment!");
                return;
            }
        } catch (NumberFormatException e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Please enter a valid payment amount");
            return;
        }
        
        change = payment - totalAmount;
        
        // Build receipt
        String receipt = buildReceipt();
        
        int confirm = javax.swing.JOptionPane.showConfirmDialog(this, 
            "Process payment of Php " + String.format("%.2f", payment) + "?",
            "Confirm Payment",
            javax.swing.JOptionPane.YES_NO_OPTION);
        
        if (confirm != javax.swing.JOptionPane.YES_OPTION) {
            return;
        }
        
        // Deduct inventory
        updateInventory();
        
        // Save order to database
        saveOrder();
        
        javax.swing.JOptionPane.showMessageDialog(this, 
            "Payment Successful!\n\n" + receipt,
            "Payment Complete",
            javax.swing.JOptionPane.INFORMATION_MESSAGE);
        
        // Clear cart and start new order
        cartModel.setRowCount(0);
        orderCount++;
        lblOrderNumber.setText("Order #" + orderCount);
        calculateTotal();
        txtPayment.setText("");
        txtChange.setText("Php 0.00");
        loadAllProducts();
    }
    
    private void updateInventory() {
        String updateSql = "UPDATE products SET Quantity = Quantity - ? WHERE ProductID = ?";
        
        try (java.sql.Connection con = ConnectorXampp.connect();
             java.sql.PreparedStatement pst = con.prepareStatement(updateSql)) {
            
            for (int i = 0; i < cartModel.getRowCount(); i++) {
                int productId = (int) cartModel.getValueAt(i, 1);
                int quantity = (int) cartModel.getValueAt(i, 8);
                
                pst.setInt(1, quantity);
                pst.setInt(2, productId);
                pst.executeUpdate();
            }
            
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Error updating inventory: " + e.getMessage());
        }
    }
    
    private void saveOrder() {
        try {
            String orderSql = "INSERT INTO orders (OrderDate, TotalAmount, OrderType) VALUES (NOW(), ?, ?)";
            java.sql.Connection con = ConnectorXampp.connect();
            java.sql.PreparedStatement orderPst = con.prepareStatement(orderSql, java.sql.Statement.RETURN_GENERATED_KEYS);
            orderPst.setDouble(1, totalAmount);
            orderPst.setString(2, getOrderType());
            orderPst.executeUpdate();
            
            java.sql.ResultSet rs = orderPst.getGeneratedKeys();
            int orderId = 1;
            if (rs.next()) {
                orderId = rs.getInt(1);
            }
            
            String detailSql = "INSERT INTO order_details (OrderID, ProductID, AddonName, AddonPrice, BasePrice, Quantity, Subtotal) VALUES (?, ?, ?, ?, ?, ?, ?)";
            java.sql.PreparedStatement detailPst = con.prepareStatement(detailSql);
            
            for (int i = 0; i < cartModel.getRowCount(); i++) {
                int productId = (int) cartModel.getValueAt(i, 1);  // ✅ Add this line
                String addonName = (String) cartModel.getValueAt(i, 6);
                int addonPrice = (int) cartModel.getValueAt(i, 7);
                int basePrice = (int) cartModel.getValueAt(i, 5);
                int quantity = (int) cartModel.getValueAt(i, 8);
                double subtotal = (double) cartModel.getValueAt(i, 9);
                
                detailPst.setInt(1, orderId);
                detailPst.setInt(2, productId);
                detailPst.setString(3, addonName);
                detailPst.setInt(4, addonPrice);
                detailPst.setInt(5, basePrice);
                detailPst.setInt(6, quantity);
                detailPst.setDouble(7, subtotal);
                detailPst.executeUpdate();
            }
            
            con.close();
            
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Error saving order: " + e.getMessage());
        }
    }
    
   private String buildReceipt() {
    StringBuilder receipt = new StringBuilder();
    receipt.append("============================\n");
    receipt.append("      ZEALLED BREWS RECEIPT   \n");
    receipt.append("============================\n");
    receipt.append("Order: ").append(lblOrderNumber.getText()).append("\n");
    receipt.append("Type: ").append(getOrderType()).append("\n");
    receipt.append("Date: ").append(new java.util.Date()).append("\n");
    receipt.append("----------------------------\n");
    
    for (int i = 0; i < cartModel.getRowCount(); i++) {
        String name = (String) cartModel.getValueAt(i, 2);
        String addon = (String) cartModel.getValueAt(i, 6);
        int qty = (int) cartModel.getValueAt(i, 8);
        double sub = (double) cartModel.getValueAt(i, 9);
        
        String item = name + (addon.equals("None") ? "" : " +" + addon);
        receipt.append(String.format("%2d x %-15s ₱%.2f\n", qty, item, sub));
    }
    
    receipt.append("----------------------------\n");
    receipt.append(String.format("TOTAL:              ₱%.2f\n", totalAmount));  // 👈 ₱
    receipt.append(String.format("CASH:               ₱%.2f\n", payment));       // 👈 ₱
    receipt.append(String.format("CHANGE:             ₱%.2f\n", change));        // 👈 ₱
    receipt.append("============================\n");
    receipt.append("    THANK YOU! 😊\n");
    return receipt.toString();
}
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        txtSearch = new javax.swing.JTextField();
        txtQuantity = new javax.swing.JTextField();
        txtPayment = new javax.swing.JTextField();
        txtChange = new javax.swing.JTextField();
        lblTotal = new javax.swing.JLabel();
        lblOrderNumber = new javax.swing.JLabel();
        btnAddToCart = new javax.swing.JButton();
        btnRemoveItem = new javax.swing.JButton();
        btnClearCart = new javax.swing.JButton();
        btnProcessPayment = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTableProducts = new javax.swing.JTable();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTableCart = new javax.swing.JTable();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        cmbAddons = new javax.swing.JComboBox<>();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        cmbOrderType = new javax.swing.JComboBox<>();
        btnlogout = new javax.swing.JButton();

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtSearchKeyReleased(evt);
            }
        });

        txtPayment.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtPaymentKeyReleased(evt);
            }
        });

        lblTotal.setFont(new java.awt.Font("Segoe UI", 0, 36)); // NOI18N
        lblTotal.setText("Php 0.00");

        lblOrderNumber.setText(".\\");

            btnAddToCart.setText("Add");
            btnAddToCart.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    btnAddToCartActionPerformed(evt);
                }
            });

            btnRemoveItem.setText("remove");
            btnRemoveItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    btnRemoveItemActionPerformed(evt);
                }
            });

            btnClearCart.setText("Clear Cart");
            btnClearCart.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    btnClearCartActionPerformed(evt);
                }
            });

            btnProcessPayment.setText("Process Payment");
            btnProcessPayment.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    btnProcessPaymentActionPerformed(evt);
                }
            });

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
            jScrollPane2.setViewportView(jTableProducts);

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

            jLabel1.setText("Quantity");

            jLabel2.setText("Payment");

            jLabel3.setText("Change");

            jLabel4.setText("Search");

            cmbAddons.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "None", "Black Pearl", "Topping Boba", "Fruity Jelly", "Yakult", "Oreo", "Lychee" }));

            jLabel5.setText("Addons");

            jLabel6.setText("Order:");

            cmbOrderType.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Dine In", "Take Out" }));

            btnlogout.setText("Log Out");
            btnlogout.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    btnlogoutActionPerformed(evt);
                }
            });

            javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
            getContentPane().setLayout(layout);
            layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                            .addGap(173, 173, 173)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(jLabel1)
                                        .addComponent(jLabel5))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(cmbAddons, javax.swing.GroupLayout.PREFERRED_SIZE, 164, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                            .addGroup(layout.createSequentialGroup()
                                                .addGap(1, 1, 1)
                                                .addComponent(txtPayment))
                                            .addComponent(txtQuantity, javax.swing.GroupLayout.PREFERRED_SIZE, 164, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                    .addGap(52, 52, 52)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(btnProcessPayment)
                                        .addGroup(layout.createSequentialGroup()
                                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                .addComponent(btnClearCart, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(btnRemoveItem, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(btnAddToCart, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                            .addGap(250, 250, 250)
                                            .addComponent(btnlogout))))
                                .addGroup(layout.createSequentialGroup()
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(jLabel3)
                                        .addComponent(jLabel2))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(txtChange, javax.swing.GroupLayout.PREFERRED_SIZE, 164, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(jLabel6)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(cmbOrderType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                    .addGap(283, 283, 283)
                                    .addComponent(jLabel4)
                                    .addGap(18, 18, 18)
                                    .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(layout.createSequentialGroup()
                                    .addContainerGap()
                                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 489, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGap(104, 104, 104)
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 463, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addContainerGap(267, Short.MAX_VALUE))
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblOrderNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(18, 18, 18)
                    .addComponent(lblTotal, javax.swing.GroupLayout.PREFERRED_SIZE, 202, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(387, 387, 387))
            );
            layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(18, 18, 18)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                            .addGap(8, 8, 8)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(cmbAddons, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel5))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(txtQuantity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel1))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(txtPayment, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel2))
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                    .addGap(18, 18, 18)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(cmbOrderType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel6))
                                    .addGap(0, 57, Short.MAX_VALUE))
                                .addGroup(layout.createSequentialGroup()
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel3)
                                        .addComponent(txtChange, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGap(9, 9, 9))))
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(btnAddToCart)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(btnRemoveItem)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(btnClearCart)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(btnProcessPayment)
                            .addGap(0, 0, Short.MAX_VALUE)))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel4))
                    .addGap(11, 11, 11)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 423, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(25, 25, 25))
                .addGroup(layout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(btnlogout)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblTotal, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblOrderNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGap(18, 18, 18)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(58, 58, 58))
            );

            pack();
        }// </editor-fold>//GEN-END:initComponents

    private void btnAddToCartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddToCartActionPerformed
        // TODO add your handling code here:
         addToCart();
    }//GEN-LAST:event_btnAddToCartActionPerformed

    private void btnRemoveItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRemoveItemActionPerformed
        // TODO add your handling code here:
        removeFromCart();
    }//GEN-LAST:event_btnRemoveItemActionPerformed

    private void btnClearCartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearCartActionPerformed
        // TODO add your handling code here:
        clearCart();
    }//GEN-LAST:event_btnClearCartActionPerformed

    private void btnProcessPaymentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnProcessPaymentActionPerformed
        // TODO add your handling code here:
         processPayment();
    }//GEN-LAST:event_btnProcessPaymentActionPerformed

    private void txtPaymentKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtPaymentKeyReleased
        // TODO add your handling code here:
        calculateChange();
    }//GEN-LAST:event_txtPaymentKeyReleased

    private void txtSearchKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtSearchKeyReleased
        // TODO add your handling code here:
        String search = txtSearch.getText().trim();
        loadAllProducts(search);  // ✅ Always call with parameter
    }//GEN-LAST:event_txtSearchKeyReleased

    private void btnlogoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnlogoutActionPerformed
        // TODO add your handling code here:
        Loginbad a = new Loginbad();
        a.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnlogoutActionPerformed

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
            java.util.logging.Logger.getLogger(Cashier.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Cashier.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Cashier.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Cashier.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        
        
        

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Cashier().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddToCart;
    private javax.swing.JButton btnClearCart;
    private javax.swing.JButton btnProcessPayment;
    private javax.swing.JButton btnRemoveItem;
    private javax.swing.JButton btnlogout;
    private javax.swing.JComboBox<String> cmbAddons;
    private javax.swing.JComboBox<String> cmbOrderType;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTableCart;
    private javax.swing.JTable jTableProducts;
    private javax.swing.JLabel lblOrderNumber;
    private javax.swing.JLabel lblTotal;
    private javax.swing.JTextField txtChange;
    private javax.swing.JTextField txtPayment;
    private javax.swing.JTextField txtQuantity;
    private javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables
}
