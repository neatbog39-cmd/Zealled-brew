/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package pkgfinal.zealled.brew;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
/**
 *
 * @author ASUS
 */
public class Size extends javax.swing.JFrame {
     private String userName;
    Connection conn;
    PreparedStatement pst;
    ResultSet rs;
    
    private String editingSizeName = null;
    /**
     * Creates new form DashBoard
     */
    
    // ========================= CONSTRUCTOR & FIELDS =========================
    public Size() {
        initComponents();
        loadTableData();
        setFieldsEnabled(false); 
        
        BTNAdd.setEnabled(true);
        BtnEdit.setEnabled(false);
        BtnUpdate.setEnabled(false);
        BtnDelete.setEnabled(false);
        BtnReset.setEnabled(false);

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
            String sql = "SELECT size_name, size_info FROM size ORDER BY size_name";
            try (Connection con = ConnectorXampp.connect();
                 PreparedStatement pst = con.prepareStatement(sql)) {

                rs = pst.executeQuery();
                DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
                model.setColumnCount(0);
                model.addColumn("Size Name");
                model.addColumn("Info");
                model.setRowCount(0);

                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getString("size_name"),
                        rs.getString("size_info") != null ? rs.getString("size_info") : ""
                    });
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading data: " + e.getMessage());
        }
    }
    
    private void loadTableData(String search) {
        try {
            String sql = "SELECT size_name, size_info FROM size WHERE size_name LIKE ? OR size_info LIKE ?";
            try (Connection con = ConnectorXampp.connect();
                 PreparedStatement pst = con.prepareStatement(sql)) {

                pst.setString(1, "%" + search + "%");
                pst.setString(2, "%" + search + "%");
                rs = pst.executeQuery();
                
                DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
                model.setColumnCount(0);
                model.addColumn("Size Name");
                model.addColumn("Info");
                model.setRowCount(0);

                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getString("size_name"),
                        rs.getString("size_info") != null ? rs.getString("size_info") : ""
                    });
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error searching: " + e.getMessage());
        }
    }
    
    
    // ========================= CRUD OPERATIONS  =========================
    private void addSize() {
    String name = txtSizeName.getText().trim();
    String info = txtSizeInfo.getText().trim();
    
    if (name.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Size Name is Required");
        return;
    }

    // Check for duplicate
    if (isSizeExists(name)) {
        JOptionPane.showMessageDialog(this, "❌ Size '" + name + "' already exists!");
        return;
    }

    try {
        String sql = "INSERT INTO size (size_name, size_info) VALUES (?, ?)";
        try (Connection con = ConnectorXampp.connect();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, name);
            pst.setString(2, info.isEmpty() ? null : info);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "✅ Size Added!");
            loadTableData();
            clearFields();
            resetButtons();
            setFieldsEnabled(false); 
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
    }
}

    private void updateSize() {
    if (editingSizeName == null) {
        JOptionPane.showMessageDialog(this, "No size selected");
        return;
    }

    String name = txtSizeName.getText().trim();
    String info = txtSizeInfo.getText().trim();
    
    if (name.isEmpty()) return;

    // Check for duplicate (exclude current editing record)
    if (!name.equals(editingSizeName) && isSizeExists(name)) {
        JOptionPane.showMessageDialog(this, "❌ Size '" + name + "' already exists!");
        return;
    }

    try {
        String sql = "UPDATE size SET size_name = ?, size_info = ? WHERE size_name = ?";
        try (Connection con = ConnectorXampp.connect();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, name);
            pst.setString(2, info.isEmpty() ? null : info);
            pst.setString(3, editingSizeName);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "✅ Size Updated!");
            loadTableData();
            clearFields();
            editingSizeName = null;
            resetButtons();
            setFieldsEnabled(false); 
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
    }
}
    
    private void deleteSize() {
        int selectedRow = jTable1.getSelectedRow();
        if (selectedRow == -1) return;

        String name = (String) jTable1.getValueAt(selectedRow, 0);
        
        int confirm = JOptionPane.showConfirmDialog(this, "Delete '" + name + "' size?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            String sql = "DELETE FROM size WHERE size_name = ?";
            try (Connection con = ConnectorXampp.connect();
                 PreparedStatement pst = con.prepareStatement(sql)) {

                pst.setString(1, name);
                pst.executeUpdate();

                JOptionPane.showMessageDialog(this, "✅ Size Deleted!");
                loadTableData();
                resetButtons();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }
    
    // ========================= VALIDATION  =========================
    private boolean isSizeExists(String sizeName) {
    try {
        String sql = "SELECT COUNT(*) FROM size WHERE size_name = ?";
        try (Connection con = ConnectorXampp.connect();
             PreparedStatement pst = con.prepareStatement(sql)) {
            
            pst.setString(1, sizeName);
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
        txtSizeName.setText("");
        txtSizeInfo.setText("");
        editingSizeName = null;
    }

    private void resetButtons() {
        BTNAdd.setEnabled(true);
        BtnEdit.setEnabled(false);
        BtnUpdate.setEnabled(false);
        BtnDelete.setEnabled(false);
        BtnReset.setEnabled(false);
    }
     
    private void setFieldsEnabled(boolean enabled) {
        txtSizeName.setEnabled(enabled);
        txtSizeInfo.setEnabled(enabled); 
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
        btnSize = new javax.swing.JButton();
        btnProducts = new javax.swing.JButton();
        btnCategory = new javax.swing.JButton();
        btnAddons = new javax.swing.JButton();
        btnHistory = new javax.swing.JButton();
        btnLogOut = new javax.swing.JButton();
        btnUtilities = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        txtSizeName = new javax.swing.JTextField();
        jScrollPane2 = new javax.swing.JScrollPane();
        txtSizeInfo = new javax.swing.JTextArea();
        BTNAdd = new javax.swing.JButton();
        BtnEdit = new javax.swing.JButton();
        BtnDelete = new javax.swing.JButton();
        BtnUpdate = new javax.swing.JButton();
        BtnReset = new javax.swing.JButton();
        txtSearch = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
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

        btnSize.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnSize.setText("Size");
        btnSize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSizeActionPerformed(evt);
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

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(16, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
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
                .addGap(12, 12, 12)
                .addComponent(btnHistory)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnUtilities)
                .addGap(228, 228, 228)
                .addComponent(btnLogOut)
                .addGap(57, 57, 57))
        );

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        txtSizeName.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N

        txtSizeInfo.setColumns(20);
        txtSizeInfo.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        txtSizeInfo.setRows(5);
        jScrollPane2.setViewportView(txtSizeInfo);

        BTNAdd.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        BTNAdd.setText("ADD");
        BTNAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BTNAddActionPerformed(evt);
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

        BtnUpdate.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        BtnUpdate.setText("UPDATE");
        BtnUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BtnUpdateActionPerformed(evt);
            }
        });

        BtnReset.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        BtnReset.setText("RESET");
        BtnReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BtnResetActionPerformed(evt);
            }
        });

        txtSearch.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        txtSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtSearchActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Serif", 1, 24)); // NOI18N
        jLabel1.setText("SIZE");

        jLabel2.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        jLabel2.setText("Size Name:");

        jLabel3.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        jLabel3.setText("Size Info:");

        jLabel4.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        jLabel4.setText("Search:");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3)
                            .addComponent(jLabel4))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(txtSizeName, javax.swing.GroupLayout.PREFERRED_SIZE, 357, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(BTNAdd, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(BtnEdit, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 357, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 357, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(12, 12, 12)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(BtnUpdate, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 112, Short.MAX_VALUE)
                                    .addComponent(BtnDelete, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(BtnReset, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
                    .addComponent(jLabel1))
                .addContainerGap(759, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(4, 4, 4)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtSizeName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel2)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(22, 22, 22)
                        .addComponent(BTNAdd)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(BtnEdit)))
                .addGap(5, 5, 5)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel3))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4)
                            .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(42, 42, 42))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(BtnDelete)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(BtnUpdate)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(BtnReset)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );

        jTable1.setFont(new java.awt.Font("Serif", 0, 18)); // NOI18N
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null},
                {null, null},
                {null, null},
                {null, null}
            },
            new String [] {
                "Size Name", "Info"
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
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1))
                .addGap(0, 421, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
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

    private void btnSizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSizeActionPerformed
        // TODO add your handling code here:
        Size b = new Size();
       b.setVisible(true);
       this.dispose();
    }//GEN-LAST:event_btnSizeActionPerformed

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
        if (!txtSizeName.isEnabled()) {
            setFieldsEnabled(true);
            BtnReset.setEnabled(true);
            BTNAdd.setEnabled(true);
            BtnEdit.setEnabled(false);
            BtnUpdate.setEnabled(false);
            BtnDelete.setEnabled(false);
            clearFields();
            return;
        }
        addSize();
    }//GEN-LAST:event_BTNAddActionPerformed

    private void BtnEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BtnEditActionPerformed
        int selectedRow = jTable1.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a size to edit");
            return;
        }

        editingSizeName = (String) jTable1.getValueAt(selectedRow, 0);
        txtSizeName.setText(editingSizeName);
        txtSizeInfo.setText((String) jTable1.getValueAt(selectedRow, 1));

        txtSizeName.setEnabled(true);
        txtSizeInfo.setEnabled(true);
        
        BtnUpdate.setEnabled(true);
        BtnReset.setEnabled(true);
        BtnEdit.setEnabled(false);
        BTNAdd.setEnabled(false);
        BtnDelete.setEnabled(false);
    }//GEN-LAST:event_BtnEditActionPerformed

    private void BtnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BtnDeleteActionPerformed
        // TODO add your handling code here:
         deleteSize();
    }//GEN-LAST:event_BtnDeleteActionPerformed

    private void BtnUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BtnUpdateActionPerformed
        // TODO add your handling code here:
          updateSize();
    }//GEN-LAST:event_BtnUpdateActionPerformed

    private void BtnResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BtnResetActionPerformed
        // TODO add your handling code here:
       clearFields();
        setFieldsEnabled(false);
        resetButtons();
    }//GEN-LAST:event_BtnResetActionPerformed

    private void txtSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtSearchActionPerformed
        // TODO add your handling code here:
        String search = txtSearch.getText().trim();
        if (search.isEmpty()) {
            loadTableData();
        } else {
            loadTableData(search);
        }
    }//GEN-LAST:event_txtSearchActionPerformed

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
            java.util.logging.Logger.getLogger(Size.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Size.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Size.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Size.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Size().setVisible(true);
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
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField txtSearch;
    private javax.swing.JTextArea txtSizeInfo;
    private javax.swing.JTextField txtSizeName;
    // End of variables declaration//GEN-END:variables
}
