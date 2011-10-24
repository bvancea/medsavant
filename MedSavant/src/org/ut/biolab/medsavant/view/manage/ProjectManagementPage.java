/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ut.biolab.medsavant.view.manage;

import com.jidesoft.utils.SwingWorker;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.ut.biolab.medsavant.controller.ProjectController;
import org.ut.biolab.medsavant.db.model.ProjectDetails;
import org.ut.biolab.medsavant.db.util.query.ProjectQueryUtil;
import org.ut.biolab.medsavant.listener.ProjectListener;
import org.ut.biolab.medsavant.view.subview.SectionView;
import org.ut.biolab.medsavant.view.subview.SubSectionView;
import org.ut.biolab.medsavant.view.MainFrame;
import org.ut.biolab.medsavant.view.patients.DetailedListModel;
import org.ut.biolab.medsavant.view.patients.DetailedView;
import org.ut.biolab.medsavant.view.patients.SplitScreenView;
import org.ut.biolab.medsavant.view.util.ViewUtil;

/**
 *
 * @author mfiume
 */
public class ProjectManagementPage extends SubSectionView implements ProjectListener {

    public void projectAdded(String projectName) {
        if (panel != null) {
            /*try {
                int projectid = ProjectController.getInstance().getProjectId(projectName);

                NewVariantTableDialog d = new NewVariantTableDialog(projectid, MainFrame.getInstance(), true);
                d.setCancellable(false);
                d.setVisible(true);

            } catch (SQLException ex) {
                ex.printStackTrace();
                Logger.getLogger(ProjectManagementPage.class.getName()).log(Level.SEVERE, null, ex);
            }*/

            panel.refresh();
        }
    }

    public void projectRemoved(String projectName) {
        if (panel != null) {
            panel.refresh();
        }
    }

    public void projectChanged(String projectName) {
        if (panel != null) {
            panel.refresh();
        }
    }

    public void projectTableRemoved(int projid, int refid) {}

    private static class ProjectsDetailedView extends DetailedView implements ProjectListener {

        private final static JPanel details = ViewUtil.getClearPanel();
        private final JPanel content;
        private String projectName;
        private ProjectDetailsSW sw;

        public ProjectsDetailedView() {

            content = this.getContentPanel();

            this.addBottomComponent(deleteProjectButton());
            this.addBottomComponent(addTableButton());

            content.setLayout(new BorderLayout());

            content.add(details, BorderLayout.CENTER);

            ProjectController.getInstance().addProjectListener(this);

        }
        
        /*
        public final JButton editPatientButton() {

            JButton b = new JButton("Edit patient fields");
            b.setOpaque(false);
            b.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent ae) {
                    try {
                        int projectid = ProjectController.getInstance().getProjectId(projectName);

                        EditPatientTableDialog d = new EditPatientTableDialog(projectid, MainFrame.getInstance(), true);
                        d.setVisible(true);

                        //refreshSelectedProject();

                    } catch (SQLException ex) {
                        Logger.getLogger(ProjectManagementPage.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });

            return b;
        }
        
         */

        public final JButton addTableButton() {

            JButton b = new JButton("Add table for different reference");
            b.setOpaque(false);
            b.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent ae) {
                    try {
                        int projectid = ProjectController.getInstance().getProjectId(projectName);

                        NewVariantTableDialog d = new NewVariantTableDialog(projectid, MainFrame.getInstance(), true);
                        d.setVisible(true);

                        refreshSelectedProject();

                    } catch (SQLException ex) {
                        Logger.getLogger(ProjectManagementPage.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });

            return b;
        }

        public final JButton deleteProjectButton() {
            JButton b = new JButton("Delete Project");
            b.setOpaque(false);
            b.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent ae) {

                    int result = JOptionPane.showConfirmDialog(MainFrame.getInstance(),
                            "Are you sure you want to delete " + projectName + "?\nThis cannot be undone.",
                            "Confirm", JOptionPane.YES_NO_OPTION);
                    if (result == JOptionPane.YES_OPTION) {
                        ProjectController.getInstance().removeProject(projectName);
                    }
                }
            });
            return b;
        }

        @Override
        public void setSelectedItem(Vector item) {

            projectName = (String) item.get(0);
            refreshSelectedProject();


        }

        public void projectAdded(String projectName) {
        }

        public void projectRemoved(String projectName) {
        }

        public void projectChanged(String projectName) {
        }

        public void projectTableRemoved(int projid, int refid) {
            refreshSelectedProject();
        }

        private void refreshSelectedProject() {
            setTitle(projectName);

            details.removeAll();
            details.updateUI();

            if (sw != null) {
                sw.cancel(true);
            }
            sw = new ProjectDetailsSW(projectName);
            sw.execute();
        }

        private class ProjectDetailsSW extends SwingWorker {

            private String projectName;

            public ProjectDetailsSW(String projectName) {
                this.projectName = projectName;
            }
            Dimension buttonDim = new Dimension(100, 23);

            @Override
            protected Object doInBackground() throws Exception {
                final int projectId = ProjectController.getInstance().getProjectId(projectName);

                List<ProjectDetails> projectDetails = ProjectQueryUtil.getProjectDetails(projectId);
                
                JPanel p = ViewUtil.getClearPanel();
                ViewUtil.applyVerticalBoxLayout(p);

                int numTables = 0;
                p.add(ViewUtil.getLeftAlignedComponent(ViewUtil.getDetailHeaderLabel("Variant Tables:")));
                

                JPanel tablePanel = ViewUtil.getClearPanel();
                tablePanel.setLayout(new GridBagLayout());
                GridBagConstraints c = new GridBagConstraints();
                c.gridx = 0;
                c.gridy = 0;
                c.ipadx = 10;
                c.anchor = GridBagConstraints.WEST;
                c.fill = GridBagConstraints.NONE;
                c.weightx = 0;
                c.weighty = 0;
                
                JButton removeTable = null;

                //while (rs.next()) {
                for(ProjectDetails pd : projectDetails){
                    numTables++;

                    c.gridx = 0;

                    //defaultTableBox.addItem(rs.getString("name"));

                    //tablePanel.add(ViewUtil.getDetailLabel("  " + ));
                    
                    //final int refId = rs.getInt("reference_id");
                    //final String refName = rs.getString("name");
                    final int refId = pd.getReferenceId();
                    final String refName = pd.getReferenceName();
                    tablePanel.add(ViewUtil.getDetailLabel(refName),c);
                    c.gridx++;
                    //tablePanel.add(Box.createHorizontalGlue());

                    //int numAnnotations = 0;
                    //final String annotationIds = rs.getString("annotation_ids");
                    //if (annotationIds != null) {
                    //    numAnnotations = annotationIds.length() - annotationIds.replaceAll(",", "").length() + 1;
                    //}
                    final String annotationIds = pd.getAnnotationIds();
                    int numAnnotations = pd.getNumAnnotations();

                    tablePanel.add(ViewUtil.getDetailLabel(numAnnotations + " annotation(s) applied"), c);
                    c.gridx++;

                    JButton editTable = new JButton("Change");
                    editTable.setOpaque(false);
                    editTable.addMouseListener(new MouseAdapter() {
                        public void mouseReleased(MouseEvent e) {
                            try {
                                new ChangeVariantDialog(MainFrame.getInstance(), true, projectId, refId, refName, annotationIds).setVisible(true);
                                refreshSelectedProject();
                            } catch (SQLException ex) {
                                Logger.getLogger(ProjectManagementPage.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    });
                    editTable.setPreferredSize(buttonDim);
                    editTable.setMaximumSize(buttonDim);

                    //final int project_id = rs.getInt("project_id");

                    tablePanel.add(editTable, c);
                    c.gridx++;

                    //tablePanel.add(Box.createHorizontalStrut(strutwidth));

                    removeTable = new JButton("Delete");
                    removeTable.setOpaque(false);
                    removeTable.setPreferredSize(buttonDim);
                    removeTable.setMaximumSize(buttonDim);


                    removeTable.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent ae) {
                            int result = JOptionPane.showConfirmDialog(MainFrame.getInstance(),
                                    "Are you sure you want to delete " + refName + "?\nThis cannot be undone.",
                                    "Confirm", JOptionPane.YES_NO_OPTION);
                            if (result == JOptionPane.YES_OPTION) {
                                ProjectController.getInstance().removeVariantTable(projectId, refId);
                            }
                        }
                    });

                    tablePanel.add(removeTable, c);

                    c.gridx++;
                    
                    tablePanel.add(Box.createHorizontalGlue(),c);
                    
                    c.gridy++;
                }


                if (numTables == 0) {
                    p.add(ViewUtil.alignLeft(ViewUtil.getDetailLabel("No variant tables")));
                } else {

                    if (numTables == 1 && removeTable != null) {
                        removeTable.setEnabled(false);
                    }
                    
                    // TODO: this isn't the best way to force GridBagLayout to top-left
                    JPanel p0 = ViewUtil.getClearPanel();
                    p0.setLayout(new BorderLayout());
                    p0.add(tablePanel,BorderLayout.WEST);
                    JPanel p1 = ViewUtil.getClearPanel();
                    p1.setLayout(new BorderLayout());
                    p1.add(p0,BorderLayout.NORTH);
                    p.add(p1);
                }

                p.add(Box.createVerticalGlue());

                return p;
            }

            @Override
            protected void done() {
                try {
                    JPanel p = (JPanel) get();
                    updateDetails(p);
                } catch (java.util.concurrent.CancellationException ex1) {
                } catch (Exception ex) {
                    ex.printStackTrace();
                    updateDetails(ViewUtil.getMessagePanel("Problem getting project details"));
                }

            }
        }

        @Override
        public void setMultipleSelections(List<Vector> items) {
        }

        public static synchronized void updateDetails(JPanel p) {

            if (details == null) {
                return;
            }
            details.removeAll();

            details.setLayout(new BorderLayout());
            //.setLayout(new BoxLayout(details,BoxLayout.Y_AXIS));
        /*
            details.add(ViewUtil.getKeyValuePairPanel("Patients in cohort", patients.size() + ""), BorderLayout.NORTH);
            DefaultListModel lm = new DefaultListModel();
            for (Vector v : patients) {
            JLabel l = new JLabel(v.get(CohortViewTableSchema.INDEX_HOSPITALID-1).toString()); l.setForeground(Color.white);
            //details.add(l);
            lm.addElement((String) v.get(CohortViewTableSchema.INDEX_HOSPITALID-1));
            }
            list = (JList) ViewUtil.clear(new JList(lm));
            list.setBackground(ViewUtil.getDetailsBackgroundColor());
            list.setForeground(Color.white);
             * 
             */
            JScrollPane jsp = ViewUtil.getClearBorderlessJSP(p);
            details.add(jsp, BorderLayout.CENTER);
            //list.setOpaque(false);

            details.updateUI();





        }
    }

    private static class ProjectsListModel implements DetailedListModel {

        private ArrayList<String> cnames;
        private ArrayList<Class> cclasses;
        private ArrayList<Integer> chidden;

        public ProjectsListModel() {
        }

        public List<Vector> getList(int limit) throws Exception {
            List<String> projects = ProjectController.getInstance().getProjectNames();
            List<Vector> projectVector = new ArrayList<Vector>();
            for (String p : projects) {
                Vector v = new Vector();
                v.add(p);
                projectVector.add(v);
            }
            return projectVector;
        }

        public List<String> getColumnNames() {
            if (cnames == null) {
                cnames = new ArrayList<String>();
                cnames.add("Project");
            }
            return cnames;
        }

        public List<Class> getColumnClasses() {
            if (cclasses == null) {
                cclasses = new ArrayList<Class>();
                cclasses.add(String.class);
            }
            return cclasses;
        }

        public List<Integer> getHiddenColumns() {
            if (chidden == null) {
                chidden = new ArrayList<Integer>();
            }
            return chidden;
        }
    }
    private SplitScreenView panel;

    public ProjectManagementPage(SectionView parent) {
        super(parent);
        ProjectController.getInstance().addProjectListener(this);
    }

    public String getName() {
        return "Projects";
    }

    public JPanel getView(boolean update) {
        if (panel == null) {
            setPanel();
        }
        return panel;
    }

    public void setPanel() {
        panel = new SplitScreenView(
                new ProjectsListModel(),
                new ProjectsDetailedView());
    }

    @Override
    public Component[] getBanner() {
        Component[] result = new Component[1];
        result[0] = getAddPatientsButton();
        
        return result;
    }

    private JButton getAddPatientsButton() {
        JButton button = new JButton("New Project");
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                new NewProjectWizard();
                
            }
        });
        return button;
    }
    
    @Override
    public void viewDidLoad() {
    }

    @Override
    public void viewDidUnload() {
    }
}
