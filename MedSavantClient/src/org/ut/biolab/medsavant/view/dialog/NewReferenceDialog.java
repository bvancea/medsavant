/*
 *    Copyright 2011-2012 University of Toronto
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.ut.biolab.medsavant.view.dialog;

import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.DefaultTableModel;

import org.ut.biolab.medsavant.MedSavantClient;
import org.ut.biolab.medsavant.controller.LoginController;
import org.ut.biolab.medsavant.controller.ReferenceController;
import org.ut.biolab.medsavant.model.Chromosome;
import org.ut.biolab.medsavant.util.MiscUtils;
import org.ut.biolab.medsavant.view.util.DialogUtils;

/**
 *
 * @author mfiume
 */
public class NewReferenceDialog extends javax.swing.JDialog {
    
    private DefaultTableModel model;

    /** Creates new form NewProjectDialog */
    public NewReferenceDialog() {
        super(DialogUtils.getMainWindow(), Dialog.ModalityType.APPLICATION_MODAL);
        setTitle("Create a reference");
        initComponents();
        getRootPane().setDefaultButton(okButton);
        MiscUtils.registerCancelButton(cancelButton);
        setLocationRelativeTo(getParent());
        
        //setup table
        model = new DefaultTableModel(){
            @Override
            public boolean isCellEditable(int row, int col) {  
                return true;        
            }  
        };
        model.addColumn("Contig Name");
        model.addColumn("Length");
        model.addColumn("Centromere Position");
        
        for(Chromosome c : Chromosome.getHG19Chromosomes()){
            model.addRow(new Object[]{c.getName(), c.getLength(), c.getCentromerepos()});
        }
        
        table.setModel(model);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        
        
        //setup addRowButton
        addRowButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addRowButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                addRow();
            }
        });
        
        
        //setup clearButton
        clearButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                clearTable();
            }
        });
        
    }
    
    private void addRow(){
        model.addRow(new Object[3]);
        table.setModel(model);
    }
    
    private void clearTable(){
        for(int i = 0; i < model.getRowCount(); i++){
            for(int j = 0; j < model.getColumnCount(); j++){
                model.setValueAt(null, i, j);
            }
        }
        table.setModel(model);
    }
    
    private List<Chromosome> getContigs() throws NumberFormatException, Exception {
        List<Chromosome> result = new ArrayList<Chromosome>();
        List<String> names = new ArrayList<String>();
        for(int i = 0; i < model.getRowCount(); i++){
            
            //contig name
            String name = (String) model.getValueAt(i, 0);
            if(name == null || name.equals("")) continue;
            if(names.contains(name)){
                throw new Exception(); //can't have duplicates
            }
            
            //length
            long length = Long.parseLong(model.getValueAt(i, 1).toString());
            
            //centromere
            long centromere = Long.parseLong(model.getValueAt(i, 2).toString());
            if(centromere > length){
                throw new Exception(); //centromere can't be greater than length
            }

            names.add(name);
            result.add(new Chromosome(name, null, centromere, length));
        }
        return result;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.JLabel jLabel1 = new javax.swing.JLabel();
        nameField = new javax.swing.JTextField();
        cancelButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();
        javax.swing.JSeparator jSeparator1 = new javax.swing.JSeparator();
        javax.swing.JScrollPane jScrollPane1 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        javax.swing.JLabel jLabel2 = new javax.swing.JLabel();
        clearButton = new javax.swing.JLabel();
        addRowButton = new javax.swing.JLabel();
        javax.swing.JLabel jLabel3 = new javax.swing.JLabel();
        urlField = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setModal(true);
        setResizable(false);

        jLabel1.setText("Reference Name: ");

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        table.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane1.setViewportView(table);

        jLabel2.setText("Contig Information:");

        clearButton.setFont(new java.awt.Font("Tahoma", 1, 11));
        clearButton.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        clearButton.setText("clear");

        addRowButton.setFont(new java.awt.Font("Tahoma", 1, 11));
        addRowButton.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        addRowButton.setText("add row");

        jLabel3.setText("Sequence URL (optional):");

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(nameField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 332, Short.MAX_VALUE)
                    .add(jLabel1)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .add(okButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(cancelButton))
                    .add(jLabel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 272, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(urlField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 332, Short.MAX_VALUE)
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 332, Short.MAX_VALUE)
                    .add(jSeparator1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 332, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .add(jLabel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 166, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 57, Short.MAX_VALUE)
                        .add(addRowButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 64, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(clearButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 37, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel1)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(nameField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel3)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(urlField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jSeparator1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(clearButton)
                    .add(addRowButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 338, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(cancelButton)
                    .add(okButton))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        this.setVisible(false);
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        try {
            String referenceName = nameField.getText();
            String referenceURL = urlField.getText();
            List<Chromosome> contigs = getContigs();

            if (referenceName == null || referenceName.equals("")){
                DialogUtils.displayMessage("Reference name required");
            } else if (MedSavantClient.ReferenceQueryUtilAdapter.containsReference(LoginController.sessionId, referenceName)) {
                DialogUtils.displayMessage("Reference already exists");
            } else {
                ReferenceController.getInstance().addReference(referenceName, contigs, referenceURL);
                this.dispose();
            }
        } catch (Exception ex) {
            DialogUtils.displayException("Error Creating References",
                    "<HTML>There was a problem reading your contig values.<BR>"
                    + "Make sure there are no duplicate names, length and centromere contain only numbers, <BR>"
                    + "and centromere &lt; length.</HTML>", ex);
        }

    }//GEN-LAST:event_okButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel addRowButton;
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel clearButton;
    private javax.swing.JTextField nameField;
    private javax.swing.JButton okButton;
    private javax.swing.JTable table;
    private javax.swing.JTextField urlField;
    // End of variables declaration//GEN-END:variables
}
