/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * NodeModifyForm.java
 *
 * Created on Jan 14, 2012, 11:57:32 AM
 */
package org.sola.clients.swing.gis.ui.control;

import java.text.DecimalFormat;
import org.sola.clients.swing.gis.Messaging;
import org.sola.common.messaging.GisMessage;

/**
 *
 * @author Elton Manoku
 */
public class NodeModifyForm extends javax.swing.JDialog {

    public enum Status {

        RemoveNode,
        ModifyNode,
        DoNothing
    }
    private Status status = Status.DoNothing;
    private DecimalFormat decimalFormat = new DecimalFormat("#.##");

    /** Creates new form NodeModifyForm */
    public NodeModifyForm() {
        initComponents();
        this.setAlwaysOnTop(true);
        this.setModalityType(ModalityType.APPLICATION_MODAL);
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setCoordinateX(Double x) {
        this.txtX.setText(decimalFormat.format(x));
    }

    public void setCoordinateY(Double y) {
        this.txtY.setText(decimalFormat.format(y));
    }

    public Double getCoordinateX() {
        return Double.valueOf(this.txtX.getText());
    }

    public Double getCoordinateY() {
        return Double.valueOf(this.txtY.getText());
    }

    public void setRemoveButtonVisibility(boolean visible){
        this.cmdRemove.setVisible(visible);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        txtY = new javax.swing.JTextField();
        txtX = new javax.swing.JTextField();
        lblY = new javax.swing.JLabel();
        lblX = new javax.swing.JLabel();
        cmdModify = new javax.swing.JButton();
        cmdRemove = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        txtY.setName("txtY"); // NOI18N

        txtX.setName("txtX"); // NOI18N

        lblY.setText("Y (Northing)");
        lblY.setName("lblY"); // NOI18N

        lblX.setText("X (Easting)");
        lblX.setName("lblX"); // NOI18N

        cmdModify.setText("Modify");
        cmdModify.setName("cmdModify"); // NOI18N
        cmdModify.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmdModifyActionPerformed(evt);
            }
        });

        cmdRemove.setText("Remove");
        cmdRemove.setName("cmdRemove"); // NOI18N
        cmdRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmdRemoveActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblX)
                            .addComponent(txtX, javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtY, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
                            .addComponent(lblY)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(cmdRemove)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmdModify)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblX)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblY)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cmdModify)
                    .addComponent(cmdRemove))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void cmdModifyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmdModifyActionPerformed

    try {
        Double.parseDouble(this.txtX.getText());
        Double.parseDouble(this.txtY.getText());
        this.status = Status.ModifyNode;
        this.setVisible(false);
    } catch (NumberFormatException ex) {
        Messaging.getInstance().show(GisMessage.CADASTRE_REDEFINITION_COORDS_NOT_VALID);
    }

}//GEN-LAST:event_cmdModifyActionPerformed

private void cmdRemoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmdRemoveActionPerformed
    this.status = Status.RemoveNode;
    this.setVisible(false);

}//GEN-LAST:event_cmdRemoveActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cmdModify;
    private javax.swing.JButton cmdRemove;
    private javax.swing.JLabel lblX;
    private javax.swing.JLabel lblY;
    private javax.swing.JTextField txtX;
    private javax.swing.JTextField txtY;
    // End of variables declaration//GEN-END:variables
}
