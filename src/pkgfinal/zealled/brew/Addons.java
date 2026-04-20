/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package pkgfinal.zealled.brew;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
import javax.swing.JOptionPane;

/**
 *
 * @author ASUS
 */
public class Addons extends javax.swing.JFrame {
     Connection conn;
     PreparedStatement pst;
     ResultSet rs;
     private int editingAddonId = -1;

    /**
     * Creates new form DashBoard
     */
     
     // ========================= CONSTRUCTOR & FIELDS =========================
    public Addons() {
        initComponents();
        loadTableData();
        
       
        
        // Initial Button States
        setFieldsEnabled(false); 
        BTNAdd.setEnabled(true);
        BtnEdit.setEnabled(false);
        BtnUpdate.setEnabled(false);
        BtnDelete.setEnabled(false);
        BtnReset.setEnabled(false);

        // Table click listener
        jTable1.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && jTable1.getSelectedRow() != -1) {
                BtnEdit.setEnabled(true);
                BtnDelete.setEnabled(true);
                BTNAdd.setEnabled(false);
            }
        });
    }
    
    // ========================= TABLE DATA LOADERS =========================
    private void loadTableData() {
    try {
        String sql = "SELECT * FROM addons";
        try (Connection con = ConnectorXampp.connect();
             PreparedStatement pst = con.prepareStatement(sql)) {

            rs = pst.executeQuery();
            DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
            model.setColumnCount(0);  // Clear columns first
            model.addColumn("ID");
            model.addColumn("Name");
            model.addColumn("Price");
            model.setRowCount(0);

            while (rs.next()) {
                double priceValue = rs.getDouble("Price");
                String formattedPrice = String.format("₱%.2f", priceValue);
                Object[] row = {rs.getInt("AddonID"), rs.getString("Name"), formattedPrice};
                model.addRow(row);
            }
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error loading data: " + e.getMessage());
    }
}
    
    private void loadTableData(String search) {
    try {
        String sql = "SELECT * FROM addons WHERE name LIKE ?";
        try (Connection con = ConnectorXampp.connect();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, "%" + search + "%");
            rs = pst.executeQuery();
            DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
            model.setColumnCount(0);  // Clear columns first
            model.addColumn("ID");
            model.addColumn("Name");
            model.addColumn("Price");
            model.setRowCount(0);

            while (rs.next()) {
                double priceValue = rs.getDouble("Price");
                String formattedPrice = String.format("₱%.2f", priceValue);
                Object[] row = {rs.getInt("AddonID"), rs.getString("name"), formattedPrice};
                model.addRow(row);
            }
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error searching: " + e.getMessage());
    }
}

     // ========================= CRUD OPERATIONS  =========================
    private void addAddon() {
    String name = txtAddonName.getText().trim();
    String price = txtAddonPrice.getText().trim();

    if (name.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Addon Name is Required");
        return;
    }
    if (price.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Price is Required");
        return;
    }

    // Check for duplicate name
    if (isAddonNameExists(name)) {
        JOptionPane.showMessageDialog(this, "❌ Addon '" + name + "' already exists!");
        return;
    }

    try {
        String sql = "INSERT INTO addons (Name, Price) VALUES (?, ?)";
        try (Connection con = ConnectorXampp.connect();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, name);
            pst.setString(2, price);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "✅ Addon Added Successfully!");
            loadTableData();
            
            // 🔥 DISABLE FIELDS AFTER ADDING
            clearFields();
            setFieldsEnabled(false);  // 🔥 ADD THIS LINE
            resetButtons();           // 🔥 ADD THIS LINE
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error adding addon: " + e.getMessage());
    }
}

    private void updateAddon() {
    if (editingAddonId == -1) {
        JOptionPane.showMessageDialog(this, "No addon selected to update");
        return;
    }

    String name = txtAddonName.getText().trim();
    String price = txtAddonPrice.getText().trim();

    if (name.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Addon Name is Required");
        return;
    }
    if (price.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Price is Required");
        return;
    }

    // Get current name before update for duplicate check
    String currentName = "";
    try {
        String sqlCheck = "SELECT Name FROM addons WHERE AddonID = ?";
        try (Connection con = ConnectorXampp.connect();
             PreparedStatement pstCheck = con.prepareStatement(sqlCheck)) {
            pstCheck.setInt(1, editingAddonId);
            ResultSet rsCheck = pstCheck.executeQuery();
            if (rsCheck.next()) {
                currentName = rsCheck.getString("Name");
            }
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error checking current name: " + e.getMessage());
        return;
    }

    // Check for duplicate (exclude current addon)
    if (!name.equals(currentName) && isAddonNameExists(name)) {
        JOptionPane.showMessageDialog(this, "❌ Addon '" + name + "' already exists!");
        return;
    }

    try {
        String sql = "UPDATE addons SET Name = ?, Price = ? WHERE AddonID = ?";
        try (Connection con = ConnectorXampp.connect();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, name);
            pst.setString(2, price);
            pst.setInt(3, editingAddonId);
            int rowsAffected = pst.executeUpdate();

            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this, "✅ Addon Updated Successfully!");
            } else {
                JOptionPane.showMessageDialog(this, "⚠️ No changes made or addon not found");
            }
            
            // 🔥 AFTER UPDATE: DISABLE FIELDS + RESET
            loadTableData();
            clearFields();
            setFieldsEnabled(false);     // 🔥 DISABLE TEXT FIELDS
            editingAddonId = -1;
            resetButtons();              // 🔥 RESET BUTTONS
            
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error updating addon: " + e.getMessage());
        e.printStackTrace();
    }
}
    
    private void deleteAddon() {
        int selectedRow = jTable1.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an addon to delete");
            return;
        }

        // Get ID from the first column
        int id = (int) jTable1.getValueAt(selectedRow, 0);
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete this addon?", 
            "Confirm Delete", 
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                String sql = "DELETE FROM addons WHERE AddonID = ?";
                try (Connection con = ConnectorXampp.connect();
                     PreparedStatement pst = con.prepareStatement(sql)) {

                    pst.setInt(1, id);
                    pst.executeUpdate();

                    JOptionPane.showMessageDialog(this, "Addon Deleted Successfully");
                    loadTableData();
                    resetButtons();

                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error deleting addon: " + e.getMessage());
            }
        }
    }
    
    // ========================= VALIDATION  =========================
    private boolean isAddonNameExists(String addonName) {
    try {
        String sql = "SELECT COUNT(*) FROM addons WHERE Name = ?";
        try (Connection con = ConnectorXampp.connect();
             PreparedStatement pst = con.prepareStatement(sql)) {
            
            pst.setString(1, addonName);
            ResultSet rs = pst.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error checking duplicate: " + e.getMessage());
        return false;
    }
}
    
    // ========================= UI CONTROL METHODS  =========================
    private void clearFields() {
        txtAddonName.setText("");
        txtAddonPrice.setText("");
        editingAddonId = -1;
    }

    private void setFieldsEnabled(boolean enabled) {
        txtAddonName.setEnabled(enabled);
        txtAddonPrice.setEnabled(enabled);
    }

    private void resetButtons() {
        BTNAdd.setEnabled(true);
        BtnEdit.setEnabled(false);
        BtnUpdate.setEnabled(false);
        BtnDelete.setEnabled(false);
        BtnReset.setEnabled(false);
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
        btnSize = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        txtAddonName = new javax.swing.JTextField();
        txtAddonPrice = new javax.swing.JTextField();
        txtSearchaddons = new javax.swing.JTextField();
        BTNAdd = new javax.swing.JButton();
        BtnUpdate = new javax.swing.JButton();
        BtnEdit = new javax.swing.JButton();
        BtnDelete = new javax.swing.JButton();
        BtnReset = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();

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
                .addContainerGap(218, Short.MAX_VALUE)
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

        jLabel7.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        jLabel7.setText("Search:");

        jLabel8.setFont(new java.awt.Font("Serif", 1, 24)); // NOI18N
        jLabel8.setText("ADDONS");

        jLabel9.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        jLabel9.setText("Addons Name:");

        jLabel10.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        jLabel10.setText("Addons Price:");

        txtAddonName.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N

        txtAddonPrice.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N

        txtSearchaddons.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        txtSearchaddons.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtSearchaddonsActionPerformed(evt);
            }
        });

        BTNAdd.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        BTNAdd.setText("ADD");
        BTNAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BTNAddActionPerformed(evt);
            }
        });

        BtnUpdate.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        BtnUpdate.setText("UPDATE");
        BtnUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BtnUpdateActionPerformed(evt);
            }
        });

        BtnEdit.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        BtnEdit.setText("EDIT");
        BtnEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BtnEditActionPerformed(evt);
            }
        });

        BtnDelete.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        BtnDelete.setText("DELETE");
        BtnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BtnDeleteActionPerformed(evt);
            }
        });

        BtnReset.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        BtnReset.setText("RESET");
        BtnReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BtnResetActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(17, 17, 17)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel9)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel7)
                                    .addComponent(jLabel10))
                                .addGap(5, 5, 5)))
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(txtAddonPrice, javax.swing.GroupLayout.DEFAULT_SIZE, 239, Short.MAX_VALUE)
                            .addComponent(txtAddonName))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(BtnEdit, javax.swing.GroupLayout.DEFAULT_SIZE, 104, Short.MAX_VALUE)
                            .addComponent(BTNAdd, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(BtnUpdate)
                            .addComponent(BtnReset, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(455, 455, 455)
                        .addComponent(BtnDelete))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(115, 115, 115)
                        .addComponent(txtSearchaddons, javax.swing.GroupLayout.PREFERRED_SIZE, 275, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(753, Short.MAX_VALUE))
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addGap(10, 10, 10)
                    .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(1252, Short.MAX_VALUE)))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addGap(57, 57, 57)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtAddonName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(BTNAdd)
                    .addComponent(BtnUpdate))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtAddonPrice, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(BtnEdit)
                    .addComponent(BtnReset))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(BtnDelete)
                        .addGap(18, 18, 18)
                        .addComponent(txtSearchaddons, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addGap(10, 10, 10)
                    .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(184, Short.MAX_VALUE)))
        );

        jTable1.setFont(new java.awt.Font("Serif", 0, 18)); // NOI18N
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "ID", "Name", "Price"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 1370, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 390, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1)
                .addContainerGap())
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

    private void BTNAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BTNAddActionPerformed
        // TODO add your handling code here:
        if (!txtAddonName.isEnabled()) {
        setFieldsEnabled(true);
        BtnReset.setEnabled(true);
        BTNAdd.setEnabled(true);
        BtnEdit.setEnabled(false);
        BtnUpdate.setEnabled(false);
        BtnDelete.setEnabled(false);
        clearFields();
        return;
    }
    addAddon();
    }//GEN-LAST:event_BTNAddActionPerformed

    private void BtnEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BtnEditActionPerformed
        // TODO add your handling code here:
         int selectedRow = jTable1.getSelectedRow();
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(this, "Please select an addon to edit");
        return;
    }

    // Store the ID for the update query
    editingAddonId = (int) jTable1.getValueAt(selectedRow, 0);
    
    // Populate fields
    String name = (String) jTable1.getValueAt(selectedRow, 1);
    
    // 🔥 REMOVE ₱ SYMBOL AND .00
    String priceWithSymbol = (String) jTable1.getValueAt(selectedRow, 2);
    String cleanPrice = priceWithSymbol
        .replace("₱", "")           // Remove ₱
        .replace(".00", "")         // Remove .00
        .trim();
    
    txtAddonName.setText(name);
    txtAddonPrice.setText(cleanPrice);  // 🔥 CLEAN PRICE (no ₱, no .00)

    // Enable fields and set buttons
    setFieldsEnabled(true);
    BtnUpdate.setEnabled(true);
    BtnReset.setEnabled(true);
    BtnEdit.setEnabled(false);
    BTNAdd.setEnabled(false);
    BtnDelete.setEnabled(false);
    }//GEN-LAST:event_BtnEditActionPerformed

    private void BtnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BtnDeleteActionPerformed
        // TODO add your handling code here:
        deleteAddon();
    }//GEN-LAST:event_BtnDeleteActionPerformed

    private void BtnUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BtnUpdateActionPerformed
        // TODO add your handling code here:
         updateAddon();
    }//GEN-LAST:event_BtnUpdateActionPerformed

    private void BtnResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BtnResetActionPerformed
        // TODO add your handling code here:
        clearFields();
        setFieldsEnabled(false);
        resetButtons();
    }//GEN-LAST:event_BtnResetActionPerformed

    private void txtSearchaddonsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtSearchaddonsActionPerformed
        // TODO add your handling code here:
         String search = txtSearchaddons.getText().trim();
        if (search.isEmpty()) {
            loadTableData();
        } else {
            loadTableData(search);
        }
    }//GEN-LAST:event_txtSearchaddonsActionPerformed

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
            java.util.logging.Logger.getLogger(Addons.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Addons.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Addons.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Addons.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Addons().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton BTNAdd;
    private javax.swing.JButton BtnDelete;
    private javax.swing.JButton BtnEdit;
    private javax.swing.JButton BtnReset;
    private javax.swing.JButton BtnUpdate;
    private javax.swing.JButton btnAddons;
    private javax.swing.JButton btnCategory;
    private javax.swing.JButton btnDashBoard;
    private javax.swing.JButton btnHistory;
    private javax.swing.JButton btnLogOut;
    private javax.swing.JButton btnProducts;
    private javax.swing.JButton btnSize;
    private javax.swing.JButton btnUtilities;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField txtAddonName;
    private javax.swing.JTextField txtAddonPrice;
    private javax.swing.JTextField txtSearchaddons;
    // End of variables declaration//GEN-END:variables
}
