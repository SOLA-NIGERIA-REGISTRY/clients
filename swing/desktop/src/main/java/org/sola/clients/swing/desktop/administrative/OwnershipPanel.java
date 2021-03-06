/**
 * ******************************************************************************************
 * Copyright (C) 2014 - Food and Agriculture Organization of the United Nations
 * (FAO). All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,this
 * list of conditions and the following disclaimer. 2. Redistributions in binary
 * form must reproduce the above copyright notice,this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. 3. Neither the name of FAO nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT,STRICT LIABILITY,OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 * IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * *********************************************************************************************
 */
package org.sola.clients.swing.desktop.administrative;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JFormattedTextField;
import javax.validation.groups.Default;
import org.sola.clients.beans.administrative.LeaseConditionsTemplateBean;
import org.sola.clients.beans.administrative.RrrBean;
import org.sola.clients.beans.administrative.RrrShareBean;
import org.sola.clients.beans.administrative.validation.OwnershipValidationGroup;
import org.sola.clients.beans.application.ApplicationBean;
import org.sola.clients.beans.application.ApplicationServiceBean;
import org.sola.clients.beans.referencedata.RequestTypeBean;
import org.sola.clients.beans.referencedata.StatusConstants;
import org.sola.clients.beans.security.SecurityBean;
import static org.sola.clients.reports.ReportManager.getSettingValue;
import org.sola.clients.swing.common.laf.LafManager;
import org.sola.clients.swing.common.controls.CalendarForm;
import org.sola.clients.swing.desktop.MainForm;
import org.sola.clients.swing.desktop.source.DocumentsManagementExtPanel;
import org.sola.clients.swing.ui.ContentPanel;
import org.sola.clients.swing.ui.MainContentPanel;
import org.sola.clients.swing.ui.renderers.TableCellListRenderer;
import org.sola.clients.swing.ui.security.SecurityClassificationDialog;
import org.sola.common.RolesConstants;
import org.sola.common.WindowUtility;
import org.sola.common.messaging.ClientMessage;
import org.sola.common.messaging.MessageUtility;

/**
 * Form for managing ownership right. {@link RrrBean} is used to bind the data
 * on the form.
 */
public class OwnershipPanel extends ContentPanel {

    private class ShareFormListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals(SharePanel.UPDATED_RRR_SHARE)
                    && evt.getNewValue() != null) {
                rrrBean.updateListItem((RrrShareBean) evt.getNewValue(),
                        rrrBean.getRrrShareList(), true);
                tableShares.clearSelection();
            }
        }
    }

    private ApplicationBean applicationBean;
    private ApplicationServiceBean appService;
    private RrrBean.RRR_ACTION rrrAction;
    public static final String UPDATED_RRR = "updatedRRR";

    private DocumentsManagementExtPanel createDocumentsPanel() {
        if (rrrBean == null) {
            rrrBean = new RrrBean();
        }
        if (applicationBean == null) {
            applicationBean = new ApplicationBean();
        }

        boolean allowEdit = true;
        if (rrrAction == RrrBean.RRR_ACTION.VIEW) {
            allowEdit = false;
        }

        DocumentsManagementExtPanel panel = new DocumentsManagementExtPanel(
                rrrBean.getSourceList(), applicationBean, allowEdit);
        return panel;
    }

    private RrrBean CreateRrrBean() {
        if (rrrBean == null) {
            rrrBean = new RrrBean();
        }
        return rrrBean;
    }

    public OwnershipPanel(RrrBean rrrBean, RrrBean.RRR_ACTION rrrAction) {
        this(rrrBean, null, null, rrrAction);
    }

    public OwnershipPanel(RrrBean rrrBean, ApplicationBean applicationBean,
            ApplicationServiceBean applicationService, RrrBean.RRR_ACTION rrrAction) {
        this.applicationBean = applicationBean;
        this.appService = applicationService;
        this.rrrAction = rrrAction;
        prepareRrrBean(rrrBean, rrrAction);
        initComponents();
        postInit();
    }

    private void postInit() {
        templates.loadList(true, rrrBean.getRrrType().getCode());
        this.txtCofO.setEnabled(false);
        if (this.appService != null) {
            if (this.appService.getRequestTypeCode().equalsIgnoreCase(RequestTypeBean.CODE_NEW_DIGITAL_TITLE)) {
                this.txtCofO.setEnabled(true);
            } else {
                this.txtCofO.setEnabled(false);
            }
        }

        String state = getSettingValue("state");
        if (state.equalsIgnoreCase("Katsina")) {
            this.cbxEstate.setVisible(false);
            this.cbxZone.setVisible(false);
            this.labEstate.setVisible(false);
            this.labZone.setVisible(false);
        }

        customizeForm();
        customizeSharesButtons(null);
        saveRrrState();
    }

    private void prepareRrrBean(RrrBean rrrBean, RrrBean.RRR_ACTION rrrAction) {
        if (rrrBean == null) {
            this.rrrBean = new RrrBean();
            this.rrrBean.setStatusCode(StatusConstants.PENDING);
        } else {
            this.rrrBean = rrrBean.makeCopyByAction(rrrAction);
        }
        this.rrrBean.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(RrrBean.SELECTED_SHARE_PROPERTY)) {
                    customizeSharesButtons((RrrShareBean) evt.getNewValue());
                }
            }
        });
    }

    private void customizeSharesButtons(RrrShareBean rrrShare) {
        boolean isChangesAllowed = false;
        if (rrrAction == RrrBean.RRR_ACTION.VARY || rrrAction == RrrBean.RRR_ACTION.EDIT
                || rrrAction == RrrBean.RRR_ACTION.NEW) {
            isChangesAllowed = true;
        }

        btnAddShare.setEnabled(isChangesAllowed);

        if (rrrShare == null) {
            btnRemoveShare.setEnabled(false);
            btnChangeShare.setEnabled(false);
            btnViewShare.setEnabled(false);
        } else {
            btnRemoveShare.setEnabled(isChangesAllowed);
            btnChangeShare.setEnabled(isChangesAllowed);
            btnViewShare.setEnabled(true);
        }

        menuAddShare.setEnabled(btnAddShare.isEnabled());
        menuRemoveShare.setEnabled(btnRemoveShare.isEnabled());
        menuChangeShare.setEnabled(btnChangeShare.isEnabled());
        menuViewShare.setEnabled(btnViewShare.isEnabled());
    }

    private void customizeForm() {
        headerPanel.setTitleText(rrrBean.getRrrType().getDisplayValue());
        if (rrrAction == RrrBean.RRR_ACTION.NEW) {
            btnSave.setText(MessageUtility.getLocalizedMessage(
                    ClientMessage.GENERAL_LABELS_CREATE_AND_CLOSE).getMessage());
        }
        if (rrrAction == RrrBean.RRR_ACTION.CANCEL) {
            btnSave.setText(MessageUtility.getLocalizedMessage(
                    ClientMessage.GENERAL_LABELS_TERMINATE_AND_CLOSE).getMessage());
        }
        
        
        if (appService != null) {
            if (!appService.getRequestTypeCode().contentEquals(RequestTypeBean.CODE_NEW_DIGITAL_TITLE)) {
                this.jLabel13.setIcon(null);
            }
            
             if (!appService.getRequestTypeCode().contentEquals(RequestTypeBean.CODE_NEW_FREEHOLD)) {
                System.out.println("SERVICE   "+appService.getRequestTypeCode());
                this.conditionsPanel.setVisible(false);
                this.conditionsPanel.setEnabled(false);
                this.mainTabbedPanel.remove(conditionsPanel);
            }
        }

        if (rrrAction != RrrBean.RRR_ACTION.EDIT && rrrAction != RrrBean.RRR_ACTION.VIEW
                && appService != null) {
            // Set default noation text from the selected application service
            txtNotationText.setText(appService.getRequestType().getNotationTemplate());
        }

        if (rrrAction == RrrBean.RRR_ACTION.VIEW) {
//            btnSave.setEnabled(false);
            txtNotationText.setEnabled(false);
            txtRegDatetime.setEditable(false);
            btnRegDate.setEnabled(false);
            txtNotationText.setEditable(false);
            txtConditionsText.setEnabled(false);
            cbxConditionsTemplates.setEnabled(false);
            btnInsertConditionsText.setEnabled(false);
        }

        btnSecurity.setVisible(btnSave.isEnabled()
                && SecurityBean.isInRole(RolesConstants.CLASSIFICATION_CHANGE_CLASS));

        if (txtTerm.getText() == "" || txtTerm.getText() == null || txtTerm.getText().equalsIgnoreCase(null) || txtTerm.getText().equalsIgnoreCase("")) {
            txtTerm.setText("99");
        }
        if (txtRevPeriod.getText() == "" || txtRevPeriod.getText() == null || txtRevPeriod.getText().equalsIgnoreCase(null) || txtRevPeriod.getText().equalsIgnoreCase("")) {
            txtRevPeriod.setText("10");
        }
    }

    private void openShareForm(RrrShareBean shareBean, RrrBean.RRR_ACTION rrrAction) {
        SharePanel shareForm = new SharePanel(shareBean, rrrAction);
        ShareFormListener listener = new ShareFormListener();
        shareForm.addPropertyChangeListener(SharePanel.UPDATED_RRR_SHARE, listener);
        getMainContentPanel().addPanel(shareForm, MainContentPanel.CARD_OWNERSHIP_SHARE, true);
    }

    private boolean saveRrr() {
        if (rrrAction == RrrBean.RRR_ACTION.VIEW) {
            close();
            return true;
        } else if (rrrBean.validate(true, Default.class, OwnershipValidationGroup.class).size() < 1) {
            if (appService.getRequestTypeCode().contentEquals(RequestTypeBean.CODE_NEW_FREEHOLD)) {
             if (rrrBean.getLeaseConditions()==null||rrrBean.getLeaseConditions()=="") {  
                MessageUtility.displayMessage(ClientMessage.CHECK_NOTNULL_LEASE_CONDITIONS);
                 return false;  
             }
            
            }
            
            firePropertyChange(UPDATED_RRR, null, rrrBean);
            close();
            return true;
        }
        return false;
    }

    private void saveRrrState() {
        MainForm.saveBeanState(rrrBean);
    }

    @Override
    protected boolean panelClosing() {
        if (btnSave.isEnabled() && MainForm.checkSaveBeforeClose(rrrBean)) {
            return saveRrr();
        }
        return true;
    }

    private void showCalendar(JFormattedTextField dateField) {
        CalendarForm calendar = new CalendarForm(null, true, dateField);
        calendar.setVisible(true);
    }

    private void configureSecurity() {
        SecurityClassificationDialog form = new SecurityClassificationDialog(
                rrrBean, false, MainForm.getInstance(), true);
        WindowUtility.centerForm(form);
        form.setVisible(true);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

        rrrBean = CreateRrrBean();
        popUpShares = new javax.swing.JPopupMenu();
        menuAddShare = new javax.swing.JMenuItem();
        menuRemoveShare = new javax.swing.JMenuItem();
        menuChangeShare = new javax.swing.JMenuItem();
        menuViewShare = new javax.swing.JMenuItem();
        zoneTypeListBean1 = new org.sola.clients.beans.referencedata.ZoneTypeListBean();
        rrrTypeListBean1 = new org.sola.clients.beans.referencedata.RrrTypeListBean();
        rotTypeListBean1 = new org.sola.clients.beans.referencedata.RotTypeListBean();
        templates = new org.sola.clients.beans.administrative.LeaseConditionsTemplateSearchResultsListBean();
        cofoTypeBean1 = new org.sola.clients.beans.referencedata.CofoTypeBean();
        cofoTypeListBean1 = new org.sola.clients.beans.referencedata.CofoTypeListBean();
        headerPanel = new org.sola.clients.swing.ui.HeaderPanel();
        jToolBar2 = new javax.swing.JToolBar();
        btnSave = new javax.swing.JButton();
        btnSecurity = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(7, 0), new java.awt.Dimension(7, 0), new java.awt.Dimension(7, 32767));
        jLabel1 = new javax.swing.JLabel();
        lblStatus = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        txtRegDatetime = new org.sola.clients.swing.common.controls.WatermarkDate();
        btnRegDate = new javax.swing.JButton();
        txtCofO = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        txtTerm = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        txtAdvPayment = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        txtCommDatetime = new org.sola.clients.swing.common.controls.WatermarkDate();
        btnCommDate = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        txtAnnualRent = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        txtRevPeriod = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        txtSignDatetime = new org.sola.clients.swing.common.controls.WatermarkDate();
        btnSignDate = new javax.swing.JButton();
        labZone = new javax.swing.JLabel();
        cbxZone = new javax.swing.JComboBox();
        cbxEstate = new javax.swing.JComboBox();
        labEstate = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        mainTabbedPanel = new javax.swing.JTabbedPane();
        generalPanel = new javax.swing.JPanel();
        jLabel15 = new javax.swing.JLabel();
        txtNotationText = new javax.swing.JTextField();
        jPanel4 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jToolBar1 = new javax.swing.JToolBar();
        btnAddShare = new javax.swing.JButton();
        btnRemoveShare = new javax.swing.JButton();
        btnChangeShare = new javax.swing.JButton();
        btnViewShare = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tableShares = new org.sola.clients.swing.common.controls.JTableWithDefaultStyles();
        groupPanel1 = new org.sola.clients.swing.ui.GroupPanel();
        jPanel1 = new javax.swing.JPanel();
        groupPanel2 = new org.sola.clients.swing.ui.GroupPanel();
        documentsPanel = createDocumentsPanel();
        conditionsPanel = new javax.swing.JPanel();
        jToolBar3 = new javax.swing.JToolBar();
        jLabel9 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox();
        jLabel10 = new javax.swing.JLabel();
        cbxConditionsTemplates = new javax.swing.JComboBox();
        btnInsertConditionsText = new javax.swing.JButton();
        filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(10, 32767));
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(10, 32767));
        jScrollPane2 = new javax.swing.JScrollPane();
        txtConditionsText = new javax.swing.JTextArea();

        popUpShares.setName("popUpShares"); // NOI18N

        menuAddShare.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/common/add.png"))); // NOI18N
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/sola/clients/swing/desktop/administrative/Bundle"); // NOI18N
        menuAddShare.setText(bundle.getString("OwnershipPanel.menuAddShare.text")); // NOI18N
        menuAddShare.setName("menuAddShare"); // NOI18N
        menuAddShare.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuAddShareActionPerformed(evt);
            }
        });
        popUpShares.add(menuAddShare);

        menuRemoveShare.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/common/remove.png"))); // NOI18N
        menuRemoveShare.setText(bundle.getString("OwnershipPanel.menuRemoveShare.text")); // NOI18N
        menuRemoveShare.setName("menuRemoveShare"); // NOI18N
        menuRemoveShare.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuRemoveShareActionPerformed(evt);
            }
        });
        popUpShares.add(menuRemoveShare);

        menuChangeShare.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/common/change-share.png"))); // NOI18N
        menuChangeShare.setText(bundle.getString("OwnershipPanel.menuChangeShare.text")); // NOI18N
        menuChangeShare.setName("menuChangeShare"); // NOI18N
        menuChangeShare.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuChangeShareActionPerformed(evt);
            }
        });
        popUpShares.add(menuChangeShare);

        menuViewShare.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/common/view.png"))); // NOI18N
        menuViewShare.setText(bundle.getString("OwnershipPanel.menuViewShare.text")); // NOI18N
        menuViewShare.setName("menuViewShare"); // NOI18N
        menuViewShare.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuViewShareActionPerformed(evt);
            }
        });
        popUpShares.add(menuViewShare);

        setHeaderPanel(headerPanel);
        setHelpTopic("ownership_rrr"); // NOI18N
        setName("Form"); // NOI18N

        headerPanel.setName("headerPanel"); // NOI18N
        headerPanel.setTitleText(bundle.getString("OwnershipPanel.headerPanel.titleText")); // NOI18N

        jToolBar2.setFloatable(false);
        jToolBar2.setRollover(true);
        jToolBar2.setName("jToolBar2"); // NOI18N

        btnSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/common/confirm-close.png"))); // NOI18N
        btnSave.setText(bundle.getString("OwnershipPanel.btnSave.text")); // NOI18N
        btnSave.setName("btnSave"); // NOI18N
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });
        jToolBar2.add(btnSave);

        btnSecurity.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/common/lock.png"))); // NOI18N
        btnSecurity.setText(bundle.getString("OwnershipPanel.btnSecurity.text")); // NOI18N
        btnSecurity.setFocusable(false);
        btnSecurity.setName("btnSecurity"); // NOI18N
        btnSecurity.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnSecurity.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSecurityActionPerformed(evt);
            }
        });
        jToolBar2.add(btnSecurity);

        jSeparator1.setName("jSeparator1"); // NOI18N
        jToolBar2.add(jSeparator1);

        filler1.setName("filler1"); // NOI18N
        jToolBar2.add(filler1);

        jLabel1.setText(bundle.getString("OwnershipPanel.jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N
        jToolBar2.add(jLabel1);

        lblStatus.setFont(LafManager.getInstance().getLabFontBold());
        lblStatus.setName("lblStatus"); // NOI18N

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, rrrBean, org.jdesktop.beansbinding.ELProperty.create("${status.displayValue}"), lblStatus, org.jdesktop.beansbinding.BeanProperty.create("text"));
        bindingGroup.addBinding(binding);

        jToolBar2.add(lblStatus);

        jPanel3.setName("jPanel3"); // NOI18N

        jLabel13.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/common/red_asterisk.gif"))); // NOI18N
        jLabel13.setText(bundle.getString("OwnershipPanel.jLabel13.text_1")); // NOI18N
        jLabel13.setToolTipText(bundle.getString("OwnershipPanel.jLabel13.toolTipText")); // NOI18N
        jLabel13.setName("jLabel13"); // NOI18N

        txtRegDatetime.setName(bundle.getString("OwnershipPanel.txtRegDatetime.name")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, rrrBean, org.jdesktop.beansbinding.ELProperty.create("${registrationDate}"), txtRegDatetime, org.jdesktop.beansbinding.BeanProperty.create("value"));
        bindingGroup.addBinding(binding);

        btnRegDate.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/common/calendar.png"))); // NOI18N
        btnRegDate.setText(bundle.getString("OwnershipPanel.btnRegDate.text")); // NOI18N
        btnRegDate.setBorder(null);
        btnRegDate.setName(bundle.getString("OwnershipPanel.btnRegDate.name")); // NOI18N
        btnRegDate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRegDateActionPerformed(evt);
            }
        });

        txtCofO.setName("txtCofO"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, rrrBean, org.jdesktop.beansbinding.ELProperty.create("${COfO}"), txtCofO, org.jdesktop.beansbinding.BeanProperty.create("text"));
        bindingGroup.addBinding(binding);

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/common/red_asterisk.gif"))); // NOI18N
        jLabel2.setText(bundle.getString("OwnershipPanel.jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        jLabel3.setText(bundle.getString("OwnershipPanel.jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N

        txtTerm.setName("txtTerm"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, rrrBean, org.jdesktop.beansbinding.ELProperty.create("${term}"), txtTerm, org.jdesktop.beansbinding.BeanProperty.create("text"));
        bindingGroup.addBinding(binding);

        jLabel4.setText(bundle.getString("OwnershipPanel.jLabel4.text")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N

        txtAdvPayment.setName("txtAdvPayment"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, rrrBean, org.jdesktop.beansbinding.ELProperty.create("${advancePayment}"), txtAdvPayment, org.jdesktop.beansbinding.BeanProperty.create("text"));
        bindingGroup.addBinding(binding);

        jLabel5.setText(bundle.getString("OwnershipPanel.jLabel5.text")); // NOI18N
        jLabel5.setName("jLabel5"); // NOI18N

        txtCommDatetime.setName("txtCommDatetime"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, rrrBean, org.jdesktop.beansbinding.ELProperty.create("${dateCommenced}"), txtCommDatetime, org.jdesktop.beansbinding.BeanProperty.create("value"));
        bindingGroup.addBinding(binding);

        btnCommDate.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/common/calendar.png"))); // NOI18N
        btnCommDate.setText(bundle.getString("OwnershipPanel.btnCommDate.text")); // NOI18N
        btnCommDate.setBorder(null);
        btnCommDate.setName("btnCommDate"); // NOI18N
        btnCommDate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCommDateActionPerformed(evt);
            }
        });

        jLabel6.setText(bundle.getString("OwnershipPanel.jLabel6.text")); // NOI18N
        jLabel6.setName("jLabel6"); // NOI18N

        txtAnnualRent.setName("txtAnnualRent"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, rrrBean, org.jdesktop.beansbinding.ELProperty.create("${yearlyRent}"), txtAnnualRent, org.jdesktop.beansbinding.BeanProperty.create("text"));
        bindingGroup.addBinding(binding);

        jLabel7.setText(bundle.getString("OwnershipPanel.jLabel7.text")); // NOI18N
        jLabel7.setName("jLabel7"); // NOI18N

        txtRevPeriod.setName("txtRevPeriod"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, rrrBean, org.jdesktop.beansbinding.ELProperty.create("${reviewPeriod}"), txtRevPeriod, org.jdesktop.beansbinding.BeanProperty.create("text"));
        bindingGroup.addBinding(binding);

        jLabel8.setText(bundle.getString("OwnershipPanel.jLabel8.text")); // NOI18N
        jLabel8.setName("jLabel8"); // NOI18N

        txtSignDatetime.setName("txtSignDatetime"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, rrrBean, org.jdesktop.beansbinding.ELProperty.create("${dateSigned}"), txtSignDatetime, org.jdesktop.beansbinding.BeanProperty.create("value"));
        bindingGroup.addBinding(binding);

        btnSignDate.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/common/calendar.png"))); // NOI18N
        btnSignDate.setText(bundle.getString("OwnershipPanel.btnSignDate.text")); // NOI18N
        btnSignDate.setBorder(null);
        btnSignDate.setName("btnSignDate"); // NOI18N
        btnSignDate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSignDateActionPerformed(evt);
            }
        });

        labZone.setText(bundle.getString("OwnershipPanel.labZone.text")); // NOI18N
        labZone.setName("labZone"); // NOI18N

        cbxZone.setName("cbxZone"); // NOI18N

        org.jdesktop.beansbinding.ELProperty eLProperty = org.jdesktop.beansbinding.ELProperty.create("${zoneTypeList}");
        org.jdesktop.swingbinding.JComboBoxBinding jComboBoxBinding = org.jdesktop.swingbinding.SwingBindings.createJComboBoxBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, zoneTypeListBean1, eLProperty, cbxZone);
        bindingGroup.addBinding(jComboBoxBinding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, rrrBean, org.jdesktop.beansbinding.ELProperty.create("${zoneType}"), cbxZone, org.jdesktop.beansbinding.BeanProperty.create("selectedItem"));
        bindingGroup.addBinding(binding);

        cbxEstate.setName("cbxEstate"); // NOI18N

        eLProperty = org.jdesktop.beansbinding.ELProperty.create("${rotTypeListBean}");
        jComboBoxBinding = org.jdesktop.swingbinding.SwingBindings.createJComboBoxBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, rotTypeListBean1, eLProperty, cbxEstate);
        bindingGroup.addBinding(jComboBoxBinding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, rrrBean, org.jdesktop.beansbinding.ELProperty.create("${rotType}"), cbxEstate, org.jdesktop.beansbinding.BeanProperty.create("selectedItem"));
        bindingGroup.addBinding(binding);

        labEstate.setText(bundle.getString("OwnershipPanel.labEstate.text")); // NOI18N
        labEstate.setName("labEstate"); // NOI18N

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(labZone, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(cbxZone, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtCofO, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(32, 32, 32)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(labEstate, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(cbxEstate, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(txtTerm, javax.swing.GroupLayout.DEFAULT_SIZE, 105, Short.MAX_VALUE)
                                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(64, 64, 64)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel4)
                                    .addComponent(txtAdvPayment, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(45, 45, 45)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(txtAnnualRent)
                                    .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, 105, Short.MAX_VALUE))
                                .addGap(40, 40, 40)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jLabel7)
                                    .addComponent(txtRevPeriod)))))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(txtRegDatetime, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnRegDate))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel13)
                        .addGap(85, 85, 85)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(txtCommDatetime, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnCommDate))
                            .addComponent(jLabel5))
                        .addGap(40, 40, 40)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(txtSignDatetime, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnSignDate))
                            .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4)
                    .addComponent(jLabel6)
                    .addComponent(jLabel7))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtCofO, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtAdvPayment, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtAnnualRent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtRevPeriod, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtTerm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel8)
                        .addComponent(jLabel5))
                    .addComponent(jLabel13, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtRegDatetime, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnRegDate, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(txtSignDatetime, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnCommDate))
                        .addComponent(btnSignDate))
                    .addComponent(txtCommDatetime, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(labZone)
                            .addComponent(labEstate))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cbxEstate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(cbxZone, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(4, 4, 4))
        );

        jScrollPane3.setName("jScrollPane3"); // NOI18N

        mainTabbedPanel.setName("mainTabbedPanel"); // NOI18N

        generalPanel.setName("generalPanel"); // NOI18N

        jLabel15.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/common/red_asterisk.gif"))); // NOI18N
        jLabel15.setText(bundle.getString("OwnershipPanel.jLabel15.text")); // NOI18N
        jLabel15.setName("jLabel15"); // NOI18N

        txtNotationText.setName("txtNotationText"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, rrrBean, org.jdesktop.beansbinding.ELProperty.create("${notation.notationText}"), txtNotationText, org.jdesktop.beansbinding.BeanProperty.create("text"));
        bindingGroup.addBinding(binding);

        jPanel4.setName("jPanel4"); // NOI18N
        jPanel4.setLayout(new java.awt.GridLayout(2, 1, 0, 10));

        jPanel2.setName("jPanel2"); // NOI18N

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);
        jToolBar1.setName("jToolBar1"); // NOI18N

        btnAddShare.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/common/add.png"))); // NOI18N
        btnAddShare.setText(bundle.getString("OwnershipPanel.btnAddShare.text")); // NOI18N
        btnAddShare.setName("btnAddShare"); // NOI18N
        btnAddShare.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddShareActionPerformed(evt);
            }
        });
        jToolBar1.add(btnAddShare);

        btnRemoveShare.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/common/remove.png"))); // NOI18N
        btnRemoveShare.setText(bundle.getString("OwnershipPanel.btnRemoveShare.text")); // NOI18N
        btnRemoveShare.setName("btnRemoveShare"); // NOI18N
        btnRemoveShare.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRemoveShareActionPerformed(evt);
            }
        });
        jToolBar1.add(btnRemoveShare);

        btnChangeShare.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/common/change-share.png"))); // NOI18N
        btnChangeShare.setText(bundle.getString("OwnershipPanel.btnChangeShare.text")); // NOI18N
        btnChangeShare.setName("btnChangeShare"); // NOI18N
        btnChangeShare.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnChangeShareActionPerformed(evt);
            }
        });
        jToolBar1.add(btnChangeShare);

        btnViewShare.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/common/view.png"))); // NOI18N
        btnViewShare.setText(bundle.getString("OwnershipPanel.btnViewShare.text")); // NOI18N
        btnViewShare.setFocusable(false);
        btnViewShare.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        btnViewShare.setName("btnViewShare"); // NOI18N
        btnViewShare.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnViewShare.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnViewShareActionPerformed(evt);
            }
        });
        jToolBar1.add(btnViewShare);

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        tableShares.setComponentPopupMenu(popUpShares);
        tableShares.setName("tableShares"); // NOI18N

        eLProperty = org.jdesktop.beansbinding.ELProperty.create("${filteredRrrShareList}");
        org.jdesktop.swingbinding.JTableBinding jTableBinding = org.jdesktop.swingbinding.SwingBindings.createJTableBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, rrrBean, eLProperty, tableShares);
        org.jdesktop.swingbinding.JTableBinding.ColumnBinding columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${rightHolderList}"));
        columnBinding.setColumnName("Right Holder List");
        columnBinding.setColumnClass(org.jdesktop.observablecollections.ObservableList.class);
        columnBinding.setEditable(false);
        columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${share}"));
        columnBinding.setColumnName("Share");
        columnBinding.setColumnClass(String.class);
        columnBinding.setEditable(false);
        bindingGroup.addBinding(jTableBinding);
        jTableBinding.bind();binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, rrrBean, org.jdesktop.beansbinding.ELProperty.create("${selectedShare}"), tableShares, org.jdesktop.beansbinding.BeanProperty.create("selectedElement"));
        bindingGroup.addBinding(binding);

        tableShares.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tableSharesMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(tableShares);
        if (tableShares.getColumnModel().getColumnCount() > 0) {
            tableShares.getColumnModel().getColumn(0).setHeaderValue(bundle.getString("OwnershipPanel.tableShares.columnModel.title0")); // NOI18N
            tableShares.getColumnModel().getColumn(0).setCellRenderer(new TableCellListRenderer("getName", "getLastName"));
            tableShares.getColumnModel().getColumn(1).setMinWidth(150);
            tableShares.getColumnModel().getColumn(1).setPreferredWidth(150);
            tableShares.getColumnModel().getColumn(1).setMaxWidth(150);
            tableShares.getColumnModel().getColumn(1).setHeaderValue(bundle.getString("OwnershipPanel.tableShares.columnModel.title1")); // NOI18N
        }

        groupPanel1.setName("groupPanel1"); // NOI18N
        groupPanel1.setTitleText(bundle.getString("OwnershipPanel.groupPanel1.titleText")); // NOI18N

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(groupPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 793, Short.MAX_VALUE)
            .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, 793, Short.MAX_VALUE)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 793, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(groupPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 77, Short.MAX_VALUE))
        );

        jPanel4.add(jPanel2);

        jPanel1.setName("jPanel1"); // NOI18N

        groupPanel2.setName("groupPanel2"); // NOI18N
        groupPanel2.setTitleText(bundle.getString("OwnershipPanel.groupPanel2.titleText")); // NOI18N

        documentsPanel.setName(bundle.getString("OwnershipPanel.documentsPanel.name")); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(groupPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(documentsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 793, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(groupPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(documentsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 108, Short.MAX_VALUE))
        );

        jPanel4.add(jPanel1);

        javax.swing.GroupLayout generalPanelLayout = new javax.swing.GroupLayout(generalPanel);
        generalPanel.setLayout(generalPanelLayout);
        generalPanelLayout.setHorizontalGroup(
            generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(generalPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtNotationText)
                    .addComponent(jLabel15)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        generalPanelLayout.setVerticalGroup(
            generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(generalPanelLayout.createSequentialGroup()
                .addGap(7, 7, 7)
                .addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtNotationText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, 267, Short.MAX_VALUE)
                .addContainerGap())
        );

        mainTabbedPanel.addTab(bundle.getString("OwnershipPanel.generalPanel.TabConstraints.tabTitle"), generalPanel); // NOI18N

        conditionsPanel.setName("conditionsPanel"); // NOI18N

        jToolBar3.setFloatable(false);
        jToolBar3.setRollover(true);
        jToolBar3.setName("jToolBar3"); // NOI18N

        jLabel9.setText(bundle.getString("OwnershipPanel.jLabel9.text")); // NOI18N
        jLabel9.setName("jLabel9"); // NOI18N
        jToolBar3.add(jLabel9);

        jComboBox1.setName("jComboBox1"); // NOI18N

        eLProperty = org.jdesktop.beansbinding.ELProperty.create("${cofoTypeList}");
        jComboBoxBinding = org.jdesktop.swingbinding.SwingBindings.createJComboBoxBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cofoTypeListBean1, eLProperty, jComboBox1);
        bindingGroup.addBinding(jComboBoxBinding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, rrrBean, org.jdesktop.beansbinding.ELProperty.create("${cofoTypeBean}"), jComboBox1, org.jdesktop.beansbinding.BeanProperty.create("selectedItem"));
        bindingGroup.addBinding(binding);

        jToolBar3.add(jComboBox1);

        jLabel10.setText(bundle.getString("OwnershipPanel.jLabel10.text")); // NOI18N
        jLabel10.setName("jLabel10"); // NOI18N
        jToolBar3.add(jLabel10);

        cbxConditionsTemplates.setName("cbxConditionsTemplates"); // NOI18N

        eLProperty = org.jdesktop.beansbinding.ELProperty.create("${templates}");
        jComboBoxBinding = org.jdesktop.swingbinding.SwingBindings.createJComboBoxBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, templates, eLProperty, cbxConditionsTemplates);
        bindingGroup.addBinding(jComboBoxBinding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, templates, org.jdesktop.beansbinding.ELProperty.create("${selectedTemplate}"), cbxConditionsTemplates, org.jdesktop.beansbinding.BeanProperty.create("selectedItem"));
        bindingGroup.addBinding(binding);

        jToolBar3.add(cbxConditionsTemplates);

        btnInsertConditionsText.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/common/down.png"))); // NOI18N
        btnInsertConditionsText.setText(bundle.getString("OwnershipPanel.btnInsertConditionsText.text")); // NOI18N
        btnInsertConditionsText.setFocusable(false);
        btnInsertConditionsText.setName("btnInsertConditionsText"); // NOI18N
        btnInsertConditionsText.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnInsertConditionsTextActionPerformed(evt);
            }
        });
        jToolBar3.add(btnInsertConditionsText);

        filler3.setName("filler3"); // NOI18N
        jToolBar3.add(filler3);

        filler2.setName("filler2"); // NOI18N
        jToolBar3.add(filler2);

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        txtConditionsText.setColumns(20);
        txtConditionsText.setRows(5);
        txtConditionsText.setName("txtConditionsText"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, rrrBean, org.jdesktop.beansbinding.ELProperty.create("${leaseConditions}"), txtConditionsText, org.jdesktop.beansbinding.BeanProperty.create("text"));
        bindingGroup.addBinding(binding);

        jScrollPane2.setViewportView(txtConditionsText);

        javax.swing.GroupLayout conditionsPanelLayout = new javax.swing.GroupLayout(conditionsPanel);
        conditionsPanel.setLayout(conditionsPanelLayout);
        conditionsPanelLayout.setHorizontalGroup(
            conditionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, conditionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(conditionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane2)
                    .addComponent(jToolBar3, javax.swing.GroupLayout.DEFAULT_SIZE, 793, Short.MAX_VALUE))
                .addContainerGap())
        );
        conditionsPanelLayout.setVerticalGroup(
            conditionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, conditionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jToolBar3, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 283, Short.MAX_VALUE)
                .addContainerGap())
        );

        mainTabbedPanel.addTab(bundle.getString("OwnershipPanel.conditionsPanel.TabConstraints.tabTitle"), new javax.swing.ImageIcon(getClass().getResource("/images/common/red_asterisk.gif")), conditionsPanel); // NOI18N

        jScrollPane3.setViewportView(mainTabbedPanel);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(headerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jToolBar2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 837, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(headerPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jToolBar2, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 327, Short.MAX_VALUE)
                .addContainerGap())
        );

        bindingGroup.bind();
    }// </editor-fold>//GEN-END:initComponents

    private void tableSharesMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableSharesMouseClicked
        if (evt.getClickCount() > 1) {
            viewShare();
        }
    }//GEN-LAST:event_tableSharesMouseClicked

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        saveRrr();
    }//GEN-LAST:event_btnSaveActionPerformed

    private void btnAddShareActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddShareActionPerformed
        addShare();
    }//GEN-LAST:event_btnAddShareActionPerformed

    private void btnRemoveShareActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRemoveShareActionPerformed
        removeShare();
    }//GEN-LAST:event_btnRemoveShareActionPerformed

    private void btnChangeShareActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnChangeShareActionPerformed
        changeShare();
    }//GEN-LAST:event_btnChangeShareActionPerformed

    private void btnViewShareActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnViewShareActionPerformed
        viewShare();
    }//GEN-LAST:event_btnViewShareActionPerformed

    private void menuAddShareActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuAddShareActionPerformed
        addShare();
    }//GEN-LAST:event_menuAddShareActionPerformed

    private void menuRemoveShareActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuRemoveShareActionPerformed
        removeShare();
    }//GEN-LAST:event_menuRemoveShareActionPerformed

    private void menuChangeShareActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuChangeShareActionPerformed
        changeShare();
    }//GEN-LAST:event_menuChangeShareActionPerformed

    private void menuViewShareActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuViewShareActionPerformed
        viewShare();
    }//GEN-LAST:event_menuViewShareActionPerformed

    private void btnSecurityActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSecurityActionPerformed
        configureSecurity();
    }//GEN-LAST:event_btnSecurityActionPerformed

    private void btnInsertConditionsTextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnInsertConditionsTextActionPerformed
        if (templates.getSelectedTemplate() != null && templates.getSelectedTemplate().getId() != null) {
            LeaseConditionsTemplateBean template = LeaseConditionsTemplateBean
                    .getLeaseConditionsTemplate(templates.getSelectedTemplate().getId());
            if (template != null) {
                txtConditionsText.setText(template.getTemplateText());
            }
        }
    }//GEN-LAST:event_btnInsertConditionsTextActionPerformed

    private void btnCommDateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCommDateActionPerformed
        showCalendar(txtCommDatetime);
    }//GEN-LAST:event_btnCommDateActionPerformed

    private void btnRegDateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegDateActionPerformed
        showCalendar(txtRegDatetime);
    }//GEN-LAST:event_btnRegDateActionPerformed

    private void btnSignDateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSignDateActionPerformed
        showCalendar(txtSignDatetime);
    }//GEN-LAST:event_btnSignDateActionPerformed

    private void changeShare() {
        if (rrrBean.getSelectedShare() != null) {
            openShareForm(rrrBean.getSelectedShare(), RrrBean.RRR_ACTION.VARY);
        }
    }

    private void removeShare() {
        if (rrrBean.getSelectedShare() != null
                && MessageUtility.displayMessage(ClientMessage.CONFIRM_DELETE_RECORD) == MessageUtility.BUTTON_ONE) {
            rrrBean.removeSelectedRrrShare();
        }
    }

    private void addShare() {
        openShareForm(null, RrrBean.RRR_ACTION.NEW);
    }

    private void viewShare() {
        if (rrrBean.getSelectedShare() != null) {
            openShareForm(rrrBean.getSelectedShare(), RrrBean.RRR_ACTION.VIEW);
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddShare;
    private javax.swing.JButton btnChangeShare;
    private javax.swing.JButton btnCommDate;
    private javax.swing.JButton btnInsertConditionsText;
    private javax.swing.JButton btnRegDate;
    private javax.swing.JButton btnRemoveShare;
    private javax.swing.JButton btnSave;
    private javax.swing.JButton btnSecurity;
    private javax.swing.JButton btnSignDate;
    private javax.swing.JButton btnViewShare;
    private javax.swing.JComboBox cbxConditionsTemplates;
    private javax.swing.JComboBox cbxEstate;
    private javax.swing.JComboBox cbxZone;
    private org.sola.clients.beans.referencedata.CofoTypeBean cofoTypeBean1;
    private org.sola.clients.beans.referencedata.CofoTypeListBean cofoTypeListBean1;
    private javax.swing.JPanel conditionsPanel;
    private org.sola.clients.swing.desktop.source.DocumentsManagementExtPanel documentsPanel;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.Box.Filler filler3;
    private javax.swing.JPanel generalPanel;
    private org.sola.clients.swing.ui.GroupPanel groupPanel1;
    private org.sola.clients.swing.ui.GroupPanel groupPanel2;
    private org.sola.clients.swing.ui.HeaderPanel headerPanel;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JToolBar jToolBar2;
    private javax.swing.JToolBar jToolBar3;
    private javax.swing.JLabel labEstate;
    private javax.swing.JLabel labZone;
    private javax.swing.JLabel lblStatus;
    private javax.swing.JTabbedPane mainTabbedPanel;
    private javax.swing.JMenuItem menuAddShare;
    private javax.swing.JMenuItem menuChangeShare;
    private javax.swing.JMenuItem menuRemoveShare;
    private javax.swing.JMenuItem menuViewShare;
    private javax.swing.JPopupMenu popUpShares;
    private org.sola.clients.beans.referencedata.RotTypeListBean rotTypeListBean1;
    private org.sola.clients.beans.administrative.RrrBean rrrBean;
    private org.sola.clients.beans.referencedata.RrrTypeListBean rrrTypeListBean1;
    private org.sola.clients.swing.common.controls.JTableWithDefaultStyles tableShares;
    private org.sola.clients.beans.administrative.LeaseConditionsTemplateSearchResultsListBean templates;
    private javax.swing.JTextField txtAdvPayment;
    private javax.swing.JTextField txtAnnualRent;
    private javax.swing.JTextField txtCofO;
    private org.sola.clients.swing.common.controls.WatermarkDate txtCommDatetime;
    private javax.swing.JTextArea txtConditionsText;
    private javax.swing.JTextField txtNotationText;
    private org.sola.clients.swing.common.controls.WatermarkDate txtRegDatetime;
    private javax.swing.JTextField txtRevPeriod;
    private org.sola.clients.swing.common.controls.WatermarkDate txtSignDatetime;
    private javax.swing.JTextField txtTerm;
    private org.sola.clients.beans.referencedata.ZoneTypeListBean zoneTypeListBean1;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables
}
