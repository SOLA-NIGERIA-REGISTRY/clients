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
package org.sola.clients.swing.desktop.application;

import java.awt.ComponentOrientation;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JTextField;
import org.sola.clients.beans.application.ApplicationBean;
import org.sola.clients.beans.application.ApplicationSearchParamsBean;
import org.sola.clients.beans.application.ApplicationSearchResultBean;
import org.sola.clients.beans.application.ApplicationSearchResultsListBean;
import org.sola.clients.beans.referencedata.StatusConstants;
import org.sola.clients.beans.security.SecurityBean;
import org.sola.clients.swing.common.laf.LafManager;
import org.sola.clients.swing.common.controls.CalendarForm;
import org.sola.clients.swing.common.tasks.SolaTask;
import org.sola.clients.swing.common.tasks.TaskManager;
import org.sola.clients.swing.desktop.MainForm;
import org.sola.clients.swing.ui.ContentPanel;
import org.sola.clients.swing.ui.MainContentPanel;
import org.sola.clients.swing.ui.renderers.BooleanCellRenderer;
import org.sola.common.RolesConstants;
import org.sola.common.WindowUtility;
import org.sola.common.messaging.ClientMessage;
import org.sola.common.messaging.MessageUtility;

/**
 * This form provides parameterized application search capabilities.
 * <p>
 * The following list of beans is used to bind the data on the form:<br />
 * {@link ApplicationSearchResultsListBean},<br
 * />{@link ApplicationSearchParamsBean}</p>
 */
public class ApplicationSearchPanel extends ContentPanel {

    private class AssignmentPanelListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent e) {
            if (e.getPropertyName().equals(ApplicationAssignmentDialog.ASSIGNMENT_CHANGED)) {
                find();
            }
        }
    }
    private AssignmentPanelListener assignmentPanelListener;

    /**
     * Default constructor to create form and initialize parameters.
     */
    public ApplicationSearchPanel() {
        initComponents();
        setHeaderPanel(headerPanel1);
        assignmentPanelListener = new AssignmentPanelListener();
        this.appList.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(ApplicationSearchResultsListBean.SELECTED_APPLICATION_PROPERTY)) {
                    customizeOpenButton((ApplicationSearchResultBean) evt.getNewValue());
                    customizeAssignedAppButtons((ApplicationSearchResultBean) evt.getNewValue());
                }
            }
        });
        customizeOpenButton(null);
        customizeAssignedAppButtons(null);

        btnFind.setEnabled(SecurityBean.isInRole(RolesConstants.APPLICATION_VIEW_APPS));

    }

    private void customizeOpenButton(ApplicationSearchResultBean searchResult) {
        btnOpenApplication.setEnabled(searchResult != null);
        menuOpenApplication.setEnabled(btnOpenApplication.isEnabled());
    }

    private void customizeAssignedAppButtons(ApplicationSearchResultBean app) {
        boolean isUnassignEnabled = false;
        boolean isAssignEnabled = false;

        if (app != null && (StatusConstants.LODGED.equalsIgnoreCase(app.getStatus())
                || StatusConstants.REQUISITIONED.equalsIgnoreCase(app.getStatus()))) {
            if (app.getAssigneeId() != null) {
                isUnassignEnabled = SecurityBean.isInRole(RolesConstants.APPLICATION_UNASSIGN_FROM_OTHERS);
                if (!isUnassignEnabled && SecurityBean.getCurrentUser().getId().equals(app.getAssigneeId())) {
                    isUnassignEnabled = SecurityBean.isInRole(RolesConstants.APPLICATION_UNASSIGN_FROM_YOURSELF);
                }
            } else {
                isAssignEnabled = app.isFeePaid()
                        && SecurityBean.isInRole(RolesConstants.APPLICATION_ASSIGN_TO_YOURSELF,
                                RolesConstants.APPLICATION_ASSIGN_TO_OTHERS);
            }
        }
        btnAssign.setEnabled(isAssignEnabled);
        btnUnassign.setEnabled(isUnassignEnabled);

    }

    private void showCalendar(JFormattedTextField dateField) {
        CalendarForm calendar = new CalendarForm(null, true, dateField);
        calendar.setVisible(true);
    }

    /**
     * Opens application assignment form with selected applications.
     *
     * @param appList Selected applications to assign or unassign.
     * @param assign Indicates whether to assign or unassign applications
     */
    private void assignUnassign(final ApplicationSearchResultBean app, final boolean assign) {
        if (app == null) {
            return;
        }
        if (!app.isFeePaid()) {
            MessageUtility.displayMessage(ClientMessage.CHECK_FEES_NOT_PAID, new Object[]{app.getNr()});
            return;
        }

        if (assign) {
            List<ApplicationSearchResultBean> tmpList = new ArrayList<ApplicationSearchResultBean>();
            tmpList.add(app);
            ApplicationAssignmentDialog form = new ApplicationAssignmentDialog(tmpList, MainForm.getInstance(), true);
            WindowUtility.centerForm(form);
            form.addPropertyChangeListener(assignmentPanelListener);
            form.setVisible(true);
        } else {
            if (MessageUtility.displayMessage(ClientMessage.APPLICATION_CONFIRM_UNASSIGN) == MessageUtility.BUTTON_ONE) {
                String assigneeId = app.getAssigneeId();
                if (assigneeId != null && assigneeId.equals(SecurityBean.getCurrentUser().getId())
                        && !SecurityBean.isInRole(RolesConstants.APPLICATION_UNASSIGN_FROM_YOURSELF,
                                RolesConstants.APPLICATION_UNASSIGN_FROM_OTHERS)) {
                    // Can't unassign from yourself
                    MessageUtility.displayMessage(ClientMessage.APPLICATION_UNASSIGN_FROM_SELF_FORBIDDEN,
                            new Object[]{app.getNr()});
                    return;
                }
                if (assigneeId != null && !assigneeId.equals(SecurityBean.getCurrentUser().getId())
                        && !SecurityBean.isInRole(RolesConstants.APPLICATION_UNASSIGN_FROM_OTHERS)) {
                    MessageUtility.displayMessage(ClientMessage.APPLICATION_UNASSIGN_FROM_OTHERS_FORBIDDEN,
                            new Object[]{app.getNr()});
                    return;
                }
                ApplicationBean.assignUser(app, null);
                find(); 
            }
        }
    }

    private void find() {
        SolaTask t = new SolaTask<Void, Void>() {

            @Override
            public Void doTask() {
                setMessage(MessageUtility.getLocalizedMessageText(ClientMessage.PROGRESS_MSG_APP_SEARCHING));
                appList.searchApplications(searchParams);
                return null;
            }

            @Override
            public void taskDone() {
                if (appList.getApplicationSearchResultsList().size() <= 0) {
                    MessageUtility.displayMessage(ClientMessage.SEARCH_NO_RESULTS);
                }

                if (appList.getApplicationSearchResultsList().size() > 99) {
                    Object[] parms = {100};
                    MessageUtility.displayMessage(ClientMessage.SEARCH_TOO_MANY_RESULTS, parms);
                }
                labResults.setText(String.format("(%s)", appList.getApplicationSearchResultsList().size()));
                tbAppList.setVisible(true);
                txtAppNumber.requestFocus();
            }
        };

        TaskManager.getInstance().runTask(t);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

        appList = new org.sola.clients.beans.application.ApplicationSearchResultsListBean();
        searchParams = new org.sola.clients.beans.application.ApplicationSearchParamsBean();
        popupSearchResults = new javax.swing.JPopupMenu();
        menuOpenApplication = new javax.swing.JMenuItem();
        appListPanel = new javax.swing.JScrollPane();
        tbAppList = new org.sola.clients.swing.common.controls.JTableWithDefaultStyles();
        jToolBar1 = new javax.swing.JToolBar();
        btnFind = new javax.swing.JButton();
        btnClear = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        btnOpenApplication = new javax.swing.JButton();
        btnAssign = new javax.swing.JButton();
        btnUnassign = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        lblSearchResults = new javax.swing.JLabel();
        labResults = new javax.swing.JLabel();
        headerPanel1 = new org.sola.clients.swing.ui.HeaderPanel();
        jPanel4 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        labAppNumber = new javax.swing.JLabel();
        txtAppNumber = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        labContactPerson = new javax.swing.JLabel();
        txtContactPerson = new javax.swing.JTextField();
        jPanel6 = new javax.swing.JPanel();
        labFrom = new javax.swing.JLabel();
        btnShowCalendarFrom = new javax.swing.JButton();
        txtFromDate = new org.sola.clients.swing.common.controls.WatermarkDate();
        jPanel3 = new javax.swing.JPanel();
        labTo = new javax.swing.JLabel();
        btnShowCalendarTo = new javax.swing.JButton();
        txtToDate = new org.sola.clients.swing.common.controls.WatermarkDate();
        jPanel2 = new javax.swing.JPanel();
        labAgentName = new javax.swing.JLabel();
        txtAgentName = new javax.swing.JTextField();
        jPanel17 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        txtDocumentNumber = new javax.swing.JTextField();
        jPanel16 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        txtDocumentReference = new javax.swing.JTextField();
        jPanel5 = new javax.swing.JPanel();

        popupSearchResults.setName("popupSearchResults"); // NOI18N

        menuOpenApplication.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/common/folder-open-document.png"))); // NOI18N
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/sola/clients/swing/desktop/application/Bundle"); // NOI18N
        menuOpenApplication.setText(bundle.getString("ApplicationSearchPanel.menuOpenApplication.text")); // NOI18N
        menuOpenApplication.setName("menuOpenApplication"); // NOI18N
        menuOpenApplication.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuOpenApplicationActionPerformed(evt);
            }
        });
        popupSearchResults.add(menuOpenApplication);

        setHelpTopic("application_search"); // NOI18N
        setMinimumSize(new java.awt.Dimension(512, 351));
        setName("Form"); // NOI18N

        appListPanel.setBorder(null);
        appListPanel.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        appListPanel.setName("appListPanel"); // NOI18N
        appListPanel.setComponentOrientation(ComponentOrientation.getOrientation(Locale.getDefault()));

        tbAppList.setComponentPopupMenu(popupSearchResults);
        tbAppList.setName("tbAppList"); // NOI18N
        tbAppList.setComponentOrientation(ComponentOrientation.getOrientation(Locale.getDefault()));
        tbAppList.getTableHeader().setReorderingAllowed(false);

        org.jdesktop.beansbinding.ELProperty eLProperty = org.jdesktop.beansbinding.ELProperty.create("${applicationSearchResultsList}");
        org.jdesktop.swingbinding.JTableBinding jTableBinding = org.jdesktop.swingbinding.SwingBindings.createJTableBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, appList, eLProperty, tbAppList);
        org.jdesktop.swingbinding.JTableBinding.ColumnBinding columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${nr}"));
        columnBinding.setColumnName("Nr");
        columnBinding.setColumnClass(Integer.class);
        columnBinding.setEditable(false);
        columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${lodgingDatetime}"));
        columnBinding.setColumnName("Lodging Datetime");
        columnBinding.setColumnClass(java.util.Date.class);
        columnBinding.setEditable(false);
        columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${expectedCompletionDate}"));
        columnBinding.setColumnName("Expected Completion Date");
        columnBinding.setColumnClass(java.util.Date.class);
        columnBinding.setEditable(false);
        columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${serviceList}"));
        columnBinding.setColumnName("Service List");
        columnBinding.setColumnClass(String.class);
        columnBinding.setEditable(false);
        columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${contactPerson}"));
        columnBinding.setColumnName("Contact Person");
        columnBinding.setColumnClass(String.class);
        columnBinding.setEditable(false);
        columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${agent}"));
        columnBinding.setColumnName("Agent");
        columnBinding.setColumnClass(String.class);
        columnBinding.setEditable(false);
        columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${status}"));
        columnBinding.setColumnName("Status");
        columnBinding.setColumnClass(String.class);
        columnBinding.setEditable(false);
        columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${feePaid}"));
        columnBinding.setColumnName("Fee Paid");
        columnBinding.setColumnClass(Boolean.class);
        columnBinding.setEditable(false);
        bindingGroup.addBinding(jTableBinding);
        jTableBinding.bind();org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, appList, org.jdesktop.beansbinding.ELProperty.create("${selectedApplication}"), tbAppList, org.jdesktop.beansbinding.BeanProperty.create("selectedElement"));
        bindingGroup.addBinding(binding);

        tbAppList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tbAppListMouseClicked(evt);
            }
        });
        appListPanel.setViewportView(tbAppList);
        if (tbAppList.getColumnModel().getColumnCount() > 0) {
            tbAppList.getColumnModel().getColumn(0).setPreferredWidth(120);
            tbAppList.getColumnModel().getColumn(0).setMaxWidth(120);
            tbAppList.getColumnModel().getColumn(0).setHeaderValue(bundle.getString("ApplicationSearchPanel.tbAppList.columnModel.title0_1")); // NOI18N
            tbAppList.getColumnModel().getColumn(1).setPreferredWidth(100);
            tbAppList.getColumnModel().getColumn(1).setMaxWidth(110);
            tbAppList.getColumnModel().getColumn(1).setHeaderValue(bundle.getString("ApplicationSearchPanel.tbAppList.columnModel.title1_1")); // NOI18N
            tbAppList.getColumnModel().getColumn(2).setPreferredWidth(100);
            tbAppList.getColumnModel().getColumn(2).setMaxWidth(110);
            tbAppList.getColumnModel().getColumn(2).setHeaderValue(bundle.getString("ApplicationSearchPanel.tbAppList.columnModel.title2_1")); // NOI18N
            tbAppList.getColumnModel().getColumn(3).setMinWidth(200);
            tbAppList.getColumnModel().getColumn(3).setHeaderValue(bundle.getString("ApplicationSearchPanel.tbAppList.columnModel.title3_1")); // NOI18N
            tbAppList.getColumnModel().getColumn(3).setCellRenderer(new org.sola.clients.swing.ui.renderers.CellDelimitedListRenderer());
            tbAppList.getColumnModel().getColumn(4).setHeaderValue(bundle.getString("ApplicationSearchPanel.tbAppList.columnModel.title4_1")); // NOI18N
            tbAppList.getColumnModel().getColumn(5).setHeaderValue(bundle.getString("ApplicationSearchPanel.tbAppList.columnModel.title7")); // NOI18N
            tbAppList.getColumnModel().getColumn(6).setMaxWidth(80);
            tbAppList.getColumnModel().getColumn(6).setHeaderValue(bundle.getString("ApplicationSearchPanel.tbAppList.columnModel.title5_1")); // NOI18N
            tbAppList.getColumnModel().getColumn(7).setMaxWidth(50);
            tbAppList.getColumnModel().getColumn(7).setHeaderValue(bundle.getString("ApplicationSearchPanel.tbAppList.columnModel.title6_1")); // NOI18N
            tbAppList.getColumnModel().getColumn(7).setCellRenderer(new BooleanCellRenderer());
        }

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);
        jToolBar1.setName("jToolBar1"); // NOI18N

        btnFind.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/common/search.png"))); // NOI18N
        btnFind.setText(bundle.getString("ApplicationSearchPanel.btnFind.text")); // NOI18N
        btnFind.setName("btnFind"); // NOI18N
        btnFind.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFindActionPerformed(evt);
            }
        });
        jToolBar1.add(btnFind);

        btnClear.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/common/eraser.png"))); // NOI18N
        btnClear.setText(bundle.getString("ApplicationSearchPanel.btnClear.text")); // NOI18N
        btnClear.setName("btnClear"); // NOI18N
        btnClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClearActionPerformed(evt);
            }
        });
        jToolBar1.add(btnClear);

        jSeparator2.setName("jSeparator2"); // NOI18N
        jToolBar1.add(jSeparator2);

        btnOpenApplication.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/common/folder-open-document.png"))); // NOI18N
        btnOpenApplication.setText(bundle.getString("ApplicationSearchPanel.btnOpenApplication.text")); // NOI18N
        btnOpenApplication.setFocusable(false);
        btnOpenApplication.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        btnOpenApplication.setName("btnOpenApplication"); // NOI18N
        btnOpenApplication.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnOpenApplication.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenApplicationActionPerformed(evt);
            }
        });
        jToolBar1.add(btnOpenApplication);

        btnAssign.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/common/assign.png"))); // NOI18N
        btnAssign.setText(bundle.getString("ApplicationSearchPanel.btnAssign.text_1")); // NOI18N
        btnAssign.setFocusable(false);
        btnAssign.setName("btnAssign"); // NOI18N
        btnAssign.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnAssign.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAssignActionPerformed(evt);
            }
        });
        jToolBar1.add(btnAssign);

        btnUnassign.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/common/unassign.png"))); // NOI18N
        btnUnassign.setText(bundle.getString("ApplicationSearchPanel.btnUnassign.text_1")); // NOI18N
        btnUnassign.setFocusable(false);
        btnUnassign.setName("btnUnassign"); // NOI18N
        btnUnassign.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnUnassign.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUnassignActionPerformed(evt);
            }
        });
        jToolBar1.add(btnUnassign);

        jSeparator1.setName("jSeparator1"); // NOI18N
        jToolBar1.add(jSeparator1);

        lblSearchResults.setFont(LafManager.getInstance().getLabFontBold());
        lblSearchResults.setText(bundle.getString("ApplicationSearchPanel.lblSearchResults.text")); // NOI18N
        lblSearchResults.setName("lblSearchResults"); // NOI18N
        jToolBar1.add(lblSearchResults);

        labResults.setText(bundle.getString("ApplicationSearchPanel.labResults.text")); // NOI18N
        labResults.setName("labResults"); // NOI18N
        labResults.setComponentOrientation(ComponentOrientation.getOrientation(Locale.getDefault()));
        labResults.setHorizontalAlignment(JLabel.LEADING);
        jToolBar1.add(labResults);

        headerPanel1.setName("headerPanel1"); // NOI18N
        headerPanel1.setTitleText(bundle.getString("ApplicationSearchPanel.headerPanel1.titleText")); // NOI18N

        jPanel4.setName("jPanel4"); // NOI18N
        jPanel4.setLayout(new java.awt.GridLayout(2, 4, 15, 0));

        jPanel7.setName("jPanel7"); // NOI18N

        labAppNumber.setText(bundle.getString("ApplicationSearchPanel.labAppNumber.text")); // NOI18N
        labAppNumber.setName("labAppNumber"); // NOI18N

        txtAppNumber.setName("txtAppNumber"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, searchParams, org.jdesktop.beansbinding.ELProperty.create("${nr}"), txtAppNumber, org.jdesktop.beansbinding.BeanProperty.create("text"));
        bindingGroup.addBinding(binding);

        txtAppNumber.setComponentOrientation(ComponentOrientation.getOrientation(Locale.getDefault()));
        txtAppNumber.setHorizontalAlignment(JTextField.LEADING);

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(txtAppNumber)
            .addComponent(labAppNumber, javax.swing.GroupLayout.DEFAULT_SIZE, 127, Short.MAX_VALUE)
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addComponent(labAppNumber)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtAppNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4.add(jPanel7);

        jPanel1.setName("jPanel1"); // NOI18N

        labContactPerson.setText(bundle.getString("ApplicationSearchPanel.labContactPerson.text")); // NOI18N
        labContactPerson.setName("labContactPerson"); // NOI18N

        txtContactPerson.setName("txtContactPerson"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, searchParams, org.jdesktop.beansbinding.ELProperty.create("${contactPerson}"), txtContactPerson, org.jdesktop.beansbinding.BeanProperty.create("text"));
        bindingGroup.addBinding(binding);

        txtContactPerson.setComponentOrientation(ComponentOrientation.getOrientation(Locale.getDefault()));
        txtContactPerson.setHorizontalAlignment(JTextField.LEADING);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(labContactPerson, javax.swing.GroupLayout.DEFAULT_SIZE, 127, Short.MAX_VALUE)
            .addComponent(txtContactPerson)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(labContactPerson, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtContactPerson, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jPanel4.add(jPanel1);

        jPanel6.setName("jPanel6"); // NOI18N

        labFrom.setText(bundle.getString("ApplicationSearchPanel.labFrom.text")); // NOI18N
        labFrom.setName("labFrom"); // NOI18N

        btnShowCalendarFrom.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/common/calendar.png"))); // NOI18N
        btnShowCalendarFrom.setText(bundle.getString("ApplicationSearchPanel.btnShowCalendarFrom.text")); // NOI18N
        btnShowCalendarFrom.setBorder(null);
        btnShowCalendarFrom.setName("btnShowCalendarFrom"); // NOI18N
        btnShowCalendarFrom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnShowCalendarFromActionPerformed(evt);
            }
        });

        txtFromDate.setName(bundle.getString("ApplicationSearchPanel.txtFromDate.name")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, searchParams, org.jdesktop.beansbinding.ELProperty.create("${fromDate}"), txtFromDate, org.jdesktop.beansbinding.BeanProperty.create("value"));
        bindingGroup.addBinding(binding);

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addComponent(txtFromDate, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnShowCalendarFrom))
            .addComponent(labFrom, javax.swing.GroupLayout.DEFAULT_SIZE, 127, Short.MAX_VALUE)
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addComponent(labFrom, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtFromDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnShowCalendarFrom))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4.add(jPanel6);

        jPanel3.setName("jPanel3"); // NOI18N

        labTo.setText(bundle.getString("ApplicationSearchPanel.labTo.text")); // NOI18N
        labTo.setName("labTo"); // NOI18N

        btnShowCalendarTo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/common/calendar.png"))); // NOI18N
        btnShowCalendarTo.setText(bundle.getString("ApplicationSearchPanel.btnShowCalendarTo.text")); // NOI18N
        btnShowCalendarTo.setBorder(null);
        btnShowCalendarTo.setName("btnShowCalendarTo"); // NOI18N
        btnShowCalendarTo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnShowCalendarToActionPerformed(evt);
            }
        });

        txtToDate.setName(bundle.getString("ApplicationSearchPanel.txtToDate.name")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, searchParams, org.jdesktop.beansbinding.ELProperty.create("${toDate}"), txtToDate, org.jdesktop.beansbinding.BeanProperty.create("value"));
        bindingGroup.addBinding(binding);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(txtToDate, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnShowCalendarTo))
            .addComponent(labTo, javax.swing.GroupLayout.DEFAULT_SIZE, 127, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(labTo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtToDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnShowCalendarTo))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4.add(jPanel3);

        jPanel2.setName("jPanel2"); // NOI18N

        labAgentName.setText(bundle.getString("ApplicationSearchPanel.labAgentName.text")); // NOI18N
        labAgentName.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        labAgentName.setName("labAgentName"); // NOI18N

        txtAgentName.setName("txtAgentName"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, searchParams, org.jdesktop.beansbinding.ELProperty.create("${agent}"), txtAgentName, org.jdesktop.beansbinding.BeanProperty.create("text"));
        bindingGroup.addBinding(binding);

        txtAgentName.setComponentOrientation(ComponentOrientation.getOrientation(Locale.getDefault()));
        txtAgentName.setHorizontalAlignment(JTextField.LEADING);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(labAgentName, javax.swing.GroupLayout.DEFAULT_SIZE, 127, Short.MAX_VALUE)
            .addComponent(txtAgentName)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(labAgentName)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtAgentName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4.add(jPanel2);

        jPanel17.setName(bundle.getString("ApplicationSearchPanel.jPanel17.name")); // NOI18N

        jLabel4.setText(bundle.getString("ApplicationSearchPanel.jLabel4.text")); // NOI18N
        jLabel4.setName(bundle.getString("ApplicationSearchPanel.jLabel4.name")); // NOI18N

        txtDocumentNumber.setToolTipText(bundle.getString("ApplicationSearchPanel.txtDocumentNumber.toolTipText")); // NOI18N
        txtDocumentNumber.setName(bundle.getString("ApplicationSearchPanel.txtDocumentNumber.name")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, searchParams, org.jdesktop.beansbinding.ELProperty.create("${documentNumber}"), txtDocumentNumber, org.jdesktop.beansbinding.BeanProperty.create("text"));
        bindingGroup.addBinding(binding);

        javax.swing.GroupLayout jPanel17Layout = new javax.swing.GroupLayout(jPanel17);
        jPanel17.setLayout(jPanel17Layout);
        jPanel17Layout.setHorizontalGroup(
            jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, 127, Short.MAX_VALUE)
            .addComponent(txtDocumentNumber)
        );
        jPanel17Layout.setVerticalGroup(
            jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel17Layout.createSequentialGroup()
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtDocumentNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4.add(jPanel17);

        jPanel16.setName(bundle.getString("ApplicationSearchPanel.jPanel16.name")); // NOI18N

        jLabel3.setText(bundle.getString("ApplicationSearchPanel.jLabel3.text")); // NOI18N
        jLabel3.setName(bundle.getString("ApplicationSearchPanel.jLabel3.name")); // NOI18N

        txtDocumentReference.setToolTipText(bundle.getString("ApplicationSearchPanel.txtDocumentReference.toolTipText")); // NOI18N
        txtDocumentReference.setName(bundle.getString("ApplicationSearchPanel.txtDocumentReference.name")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, searchParams, org.jdesktop.beansbinding.ELProperty.create("${documentReference}"), txtDocumentReference, org.jdesktop.beansbinding.BeanProperty.create("text"));
        bindingGroup.addBinding(binding);

        javax.swing.GroupLayout jPanel16Layout = new javax.swing.GroupLayout(jPanel16);
        jPanel16.setLayout(jPanel16Layout);
        jPanel16Layout.setHorizontalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(txtDocumentReference)
            .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, 127, Short.MAX_VALUE)
        );
        jPanel16Layout.setVerticalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel16Layout.createSequentialGroup()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtDocumentReference, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4.add(jPanel16);

        jPanel5.setName("jPanel5"); // NOI18N

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 127, Short.MAX_VALUE)
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 51, Short.MAX_VALUE)
        );

        jPanel4.add(jPanel5);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(headerPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 575, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(appListPanel, javax.swing.GroupLayout.Alignment.LEADING))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(headerPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(appListPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 223, Short.MAX_VALUE)
                .addContainerGap())
        );

        bindingGroup.bind();
    }// </editor-fold>//GEN-END:initComponents

    private void tbAppListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tbAppListMouseClicked
        if (evt.getClickCount() == 2) {
            openApplication();
        }
    }//GEN-LAST:event_tbAppListMouseClicked

    private void btnShowCalendarFromActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnShowCalendarFromActionPerformed
        showCalendar(txtFromDate);
    }//GEN-LAST:event_btnShowCalendarFromActionPerformed

    private void btnShowCalendarToActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnShowCalendarToActionPerformed
        showCalendar(txtToDate);
    }//GEN-LAST:event_btnShowCalendarToActionPerformed

    private void btnFindActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFindActionPerformed
        find();
    }//GEN-LAST:event_btnFindActionPerformed

    private void btnClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearActionPerformed
        txtAppNumber.setText(null);
        txtAgentName.setText(null);
        txtFromDate.setValue(null);
        txtToDate.setValue(null);
        txtContactPerson.setText(null);
        txtDocumentReference.setText(null);
        txtDocumentNumber.setText(null);
        labResults.setText(null);
        tbAppList.setVisible(false);
        txtAppNumber.requestFocus();
    }//GEN-LAST:event_btnClearActionPerformed

    private void btnOpenApplicationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenApplicationActionPerformed
        openApplication();
    }//GEN-LAST:event_btnOpenApplicationActionPerformed

    private void menuOpenApplicationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuOpenApplicationActionPerformed
        openApplication();
    }//GEN-LAST:event_menuOpenApplicationActionPerformed

    private void btnAssignActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAssignActionPerformed
        assignUnassign(appList.getSelectedApplication(), true);
    }//GEN-LAST:event_btnAssignActionPerformed

    private void btnUnassignActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUnassignActionPerformed
        assignUnassign(appList.getSelectedApplication(), false);
    }//GEN-LAST:event_btnUnassignActionPerformed

    public void clickFind() {
        find();
    }

    /**
     * Opens {@link ApplicationForm} for selected application in search results.
     */
    private void openApplication() {
        if (!SecurityBean.isInRole(RolesConstants.APPLICATION_VIEW_APPS)
                || appList.getSelectedApplication() == null) {
            return;
        }

        SolaTask t = new SolaTask<Void, Void>() {

            @Override
            public Void doTask() {
                setMessage(MessageUtility.getLocalizedMessageText(ClientMessage.PROGRESS_MSG_OPEN_APP));
                if (getMainContentPanel() != null) {
                    ApplicationPanel applicationPanel = new ApplicationPanel(
                            appList.getSelectedApplication().getId());
                    getMainContentPanel().addPanel(applicationPanel, MainContentPanel.CARD_APPLICATION, true);
                }
                return null;
            }
        };
        TaskManager.getInstance().runTask(t);
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.sola.clients.beans.application.ApplicationSearchResultsListBean appList;
    private javax.swing.JScrollPane appListPanel;
    private javax.swing.JButton btnAssign;
    private javax.swing.JButton btnClear;
    public javax.swing.JButton btnFind;
    private javax.swing.JButton btnOpenApplication;
    private javax.swing.JButton btnShowCalendarFrom;
    private javax.swing.JButton btnShowCalendarTo;
    private javax.swing.JButton btnUnassign;
    private org.sola.clients.swing.ui.HeaderPanel headerPanel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator2;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JLabel labAgentName;
    private javax.swing.JLabel labAppNumber;
    private javax.swing.JLabel labContactPerson;
    private javax.swing.JLabel labFrom;
    private javax.swing.JLabel labResults;
    private javax.swing.JLabel labTo;
    private javax.swing.JLabel lblSearchResults;
    private javax.swing.JMenuItem menuOpenApplication;
    private javax.swing.JPopupMenu popupSearchResults;
    private org.sola.clients.beans.application.ApplicationSearchParamsBean searchParams;
    private org.sola.clients.swing.common.controls.JTableWithDefaultStyles tbAppList;
    private javax.swing.JTextField txtAgentName;
    private javax.swing.JTextField txtAppNumber;
    private javax.swing.JTextField txtContactPerson;
    private javax.swing.JTextField txtDocumentNumber;
    private javax.swing.JTextField txtDocumentReference;
    private org.sola.clients.swing.common.controls.WatermarkDate txtFromDate;
    private org.sola.clients.swing.common.controls.WatermarkDate txtToDate;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables
}
