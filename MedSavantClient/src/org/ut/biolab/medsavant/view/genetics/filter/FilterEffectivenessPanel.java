package org.ut.biolab.medsavant.view.genetics.filter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import org.ut.biolab.medsavant.controller.FilterController;
import org.ut.biolab.medsavant.controller.ReferenceController;
import org.ut.biolab.medsavant.controller.ResultController;
import org.ut.biolab.medsavant.db.exception.FatalDatabaseException;
import org.ut.biolab.medsavant.db.exception.NonFatalDatabaseException;
import org.ut.biolab.medsavant.listener.ReferenceListener;
import org.ut.biolab.medsavant.model.event.FiltersChangedListener;
import org.ut.biolab.medsavant.view.component.ProgressPanel;
import org.ut.biolab.medsavant.view.dialog.IndeterminateProgressDialog;
import org.ut.biolab.medsavant.view.util.ViewUtil;
import org.ut.biolab.medsavant.view.util.WaitPanel;

/**
 *
 * @author mfiume
 */
public class FilterEffectivenessPanel extends JLayeredPane implements FiltersChangedListener, ReferenceListener {

    Color bg = new Color(139, 149, 164);
    private final ProgressPanel pp;
    private final JLabel labelVariantsRemaining;
    //private final FilterHistoryPanel historyPanel;
    private GridBagConstraints c;
    private WaitPanel waitPanel;
    private int waitCounter = 0;
    private JPanel panel;

    public FilterEffectivenessPanel() {

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;

        this.setBorder(ViewUtil.getMediumBorder());

        this.setLayout(new GridBagLayout());

        panel = ViewUtil.getClearPanel();
        //panel.setBackground(bg);
        //panel.setBorder(BorderFactory.createCompoundBorder(ViewUtil.getTopLineBorder(),ViewUtil.getBigBorder()));
        panel.setLayout(new BorderLayout());
        this.add(panel, c, JLayeredPane.DEFAULT_LAYER);

        waitPanel = new WaitPanel("Applying Filters");
        waitPanel.setVisible(false);
        this.add(waitPanel, c, JLayeredPane.DRAG_LAYER);

        labelVariantsRemaining = ViewUtil.getWhiteLabel("");

        JPanel infoPanel = ViewUtil.getClearPanel();
        ViewUtil.applyVerticalBoxLayout(infoPanel);
        infoPanel.add(ViewUtil.center(labelVariantsRemaining));
        infoPanel.add(ViewUtil.center(ViewUtil.getWhiteLabel("pass search conditions")));
        infoPanel.setBorder(ViewUtil.getMediumTopHeavyBorder());

        panel.add(infoPanel,BorderLayout.NORTH);

        pp = new ProgressPanel();
        //pp.setBorder(ViewUtil.getBigBorder());
        panel.add(pp, BorderLayout.SOUTH);

        FilterController.addFilterListener(this);
        ReferenceController.getInstance().addReferenceListener(this);

        Thread t = new Thread(){
            @Override
            public void run() {
                setMaxValues();
            }
        };
        t.start();
    }

    @Override
    public void filtersChanged() throws SQLException, FatalDatabaseException, NonFatalDatabaseException {

        //final IndeterminateProgressDialog dialog = new IndeterminateProgressDialog(
        //        "Applying Filter",
        //        "Filter is being applied. Please wait.",
        //        true);

        final FilterEffectivenessPanel instance = this;

        Thread thread = new Thread() {

            @Override
            public void run() {
                instance.showWaitCard();
                try {
                    int numLeft = ResultController.getInstance().getNumFilteredVariants();
                    instance.showShowCard();
                    //dialog.close();
                    setNumLeft(numLeft);
                } catch (NonFatalDatabaseException ex) {
                    instance.showShowCard();
                    Logger.getLogger(FilterHistoryPanel.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        };

        thread.start();


        //dialog.setVisible(true);

    }

    long numLeft = 1;
    long numTotal = 1;

    private void setNumLeft(int num) {
        numLeft = num;
        refreshProgressLabel();

        pp.animateToValue(num);
    }

    private void setMaxValues() {

        labelVariantsRemaining.setText("Calculating...");
        updateUI();

        int maxRecords = -1;

        try {
            maxRecords = ResultController.getInstance().getNumTotalVariants();
        } catch (Exception ex) {
            Logger.getLogger(FilterHistoryPanel.class.getName()).log(Level.SEVERE, null, ex);
        }

        numTotal = maxRecords;

        if (maxRecords != -1) {
            pp.setMaxValue(maxRecords);
            pp.setToValue(maxRecords);
            //labelVariantsTotal.setText("TOTAL: " + ViewUtil.numToString(maxRecords));

            setNumLeft(maxRecords);
        }
    }

    @Override
    public void referenceAdded(String name) {}

    @Override
    public void referenceRemoved(String name) {}

    @Override
    public void referenceChanged(String name) {
        setMaxValues();
    }

    public synchronized void showWaitCard() {
        waitCounter++;
        waitPanel.setVisible(true);
        this.setLayer(waitPanel, JLayeredPane.DRAG_LAYER);
        waitPanel.repaint();
    }

    public synchronized void showShowCard() {
        waitCounter--;
        if(waitCounter <= 0){
            waitPanel.setVisible(false);
            waitCounter = 0;
        }
    }

    private void refreshProgressLabel() {
        labelVariantsRemaining.setText(
                ViewUtil.numToString(numLeft)
                //+ " of "
                //+ ViewUtil.numToString(numTotal)
                + " (" + ((numLeft*100)/numTotal) + "%)"
                //+ "<br>variants pass search conditions"
                );
    }
}
