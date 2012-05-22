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
package org.ut.biolab.medsavant.view.genetics;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.JPanel;

import org.ut.biolab.medsavant.controller.FilterController;
import org.ut.biolab.medsavant.controller.ReferenceController;
import org.ut.biolab.medsavant.controller.ThreadController;
import org.ut.biolab.medsavant.listener.ReferenceListener;
import org.ut.biolab.medsavant.model.event.FiltersChangedListener;
import org.ut.biolab.medsavant.util.ClientMiscUtils;
import org.ut.biolab.medsavant.view.genetics.charts.ChartView;
import org.ut.biolab.medsavant.view.subview.SectionView;
import org.ut.biolab.medsavant.view.subview.SubSectionView;


/**
 *
 * @author mfiume
 */
public class GeneticsChartPage extends SubSectionView implements FiltersChangedListener, ReferenceListener {

    private JPanel panel;
    //private ChartContainer cc;
    private ChartView cc;
    private boolean isLoaded = false;

    public GeneticsChartPage(SectionView parent) {
        super(parent);
        FilterController.addFilterListener(this);
        ReferenceController.getInstance().addReferenceListener(this);
    }

    @Override
    public String getName() {
        return "Chart";
    }

    @Override
    public JPanel getView(boolean update) {
        try {
            if (panel == null || update) {
                setPanel();
            }
            cc.updateIfRequired();
        } catch (Exception ex) {
            ClientMiscUtils.reportError("Error creating chart view.", ex);
        }
        return panel;
    }

    private void setPanel() throws RemoteException, SQLException {

        panel = new JPanel();
        panel.setLayout(new BorderLayout());

        //PeekingPanel detailView = new PeekingPanel("Filters", BorderLayout.EAST, new FilterPanel(), true,400);
        //panel.add(detailView, BorderLayout.WEST);

        cc = new ChartView(getName());
        panel.add(cc, BorderLayout.CENTER);
    }

    @Override
    public Component[] getSubSectionMenuComponents() {
        /*
        Component[] cs = new Component[1];
        JButton addButton = new JButton("Add chart");
        addButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                cc.addChart();
                cc.updateUI();
            }
        });
        cs[0] = addButton;
        return cs;
         *
         */
        return null;
    }

    @Override
    public void viewDidLoad() {
        isLoaded = true;
    }

    @Override
    public void viewDidUnload() {
        isLoaded = false;
        ThreadController.getInstance().cancelWorkers(getName());
    }

    @Override
    public void filtersChanged() {
        ThreadController.getInstance().cancelWorkers(getName());
        tryUpdate();
    }

    @Override
    public void referenceAdded(String name) {}

    @Override
    public void referenceRemoved(String name) {}

    @Override
    public void referenceChanged(String prnameojectName) {
        tryUpdate();
    }

    private void tryUpdate() {
        if (cc != null) {
            cc.setUpdateRequired(true);
            if (isLoaded) {
                cc.updateIfRequired();
            }
        }
    }
}
