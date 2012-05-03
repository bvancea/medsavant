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
package org.ut.biolab.medsavant.view.list;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.jidesoft.grid.TableModelWrapperUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.ut.biolab.medsavant.util.MiscUtils;
import org.ut.biolab.medsavant.view.component.ListViewTablePanel;
import org.ut.biolab.medsavant.view.images.IconFactory;
import org.ut.biolab.medsavant.view.util.DialogUtils;
import org.ut.biolab.medsavant.view.util.PeekingPanel;
import org.ut.biolab.medsavant.view.util.ViewUtil;
import org.ut.biolab.medsavant.view.util.WaitPanel;

/**
 *
 * @author mfiume
 */
public class SplitScreenView extends JPanel {
    private static final Log LOG = LogFactory.getLog(SplitScreenView.class);

    private final DetailedListModel detailedListModel;
    private final DetailedView detailedView;
    private final DetailedListEditor detailedEditor;
    private ListView listView;
    //TODO: handle limits better!
    private static final int limit = 10000;

    public SplitScreenView(DetailedListModel model, DetailedView view) {
        this(model, view, new DetailedListEditor());
    }

    public SplitScreenView(DetailedListModel model, DetailedView view, DetailedListEditor editor) {
        detailedListModel = model;
        detailedView = view;
        detailedEditor = editor;
        initGUI();
    }

    private void initGUI() {
        setLayout(new BorderLayout());

        listView = new ListView();

        PeekingPanel pp = new PeekingPanel("List", BorderLayout.EAST, (JComponent)listView, true, 330);
        pp.setToggleBarVisible(false);
        add(pp, BorderLayout.WEST);
        add(detailedView, BorderLayout.CENTER);
        detailedView.setSplitScreenParent(this);
    }

    public void refresh() {
        listView.refreshList();
    }

    public Object[][] getList() {
        return listView.data;
    }

    public void selectInterval(int start, int end){
        start = TableModelWrapperUtils.getRowAt(listView.stp.getTable().getModel(), start);
        end = TableModelWrapperUtils.getRowAt(listView.stp.getTable().getModel(), end);
        listView.stp.getTable().getSelectionModel().setSelectionInterval(start, end);
        listView.stp.scrollToIndex(start);
    }

    private class ListView extends JPanel {

        private static final String CARD_WAIT = "wait";
        private static final String CARD_SHOW = "show";
        private static final String CARD_ERROR = "error";

        private Object[][] data;
        private final JPanel showCard;
        private final JLabel errorMessage;
        private ListViewTablePanel stp;
        //private int limit = 10000;
        private RowSelectionGrabber selectionGrabber;
        private JPanel buttonPanel;

        private ListView() {
            setLayout(new CardLayout());

            WaitPanel wp = new WaitPanel("Getting list");
            wp.setBackground(ViewUtil.getTertiaryMenuColor());
            add(wp, CARD_WAIT);
            
            showCard = new JPanel();
            add(showCard, CARD_SHOW);
            
            JPanel errorPanel = new JPanel();
            errorPanel.setLayout(new BorderLayout());
            errorMessage = new JLabel("An error occurred:");
            errorPanel.add(errorMessage, BorderLayout.NORTH);

            add(errorPanel, CARD_ERROR);

            buttonPanel = ViewUtil.getClearPanel();
            ViewUtil.applyHorizontalBoxLayout(buttonPanel);

            buttonPanel.setBorder(ViewUtil.getMediumBorder());
            buttonPanel.add(Box.createHorizontalGlue());

            if (detailedEditor.doesImplementAdding()) {

                JLabel butt = ViewUtil.createIconButton(IconFactory.getInstance().getIcon(IconFactory.StandardIcon.ADD_ON_TOOLBAR));
                butt.setToolTipText("Add");
                butt.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        detailedEditor.addItems();
                        refreshList();
                    }
                });
                buttonPanel.add(butt);
                buttonPanel.add(ViewUtil.getSmallSeparator());
            }

            if (detailedEditor.doesImplementImporting()) {

                JLabel butt = ViewUtil.createIconButton(IconFactory.getInstance().getIcon(IconFactory.StandardIcon.IMPORT));
                butt.setToolTipText("Import");
                butt.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        detailedEditor.importItems();
                        refreshList();
                    }
                });
                buttonPanel.add(butt);
                buttonPanel.add(ViewUtil.getSmallSeparator());
            }

            if (detailedEditor.doesImplementDeleting()) {
                JLabel butt = ViewUtil.createIconButton(IconFactory.getInstance().getIcon(IconFactory.StandardIcon.REMOVE_ON_TOOLBAR));
                butt.setToolTipText("Remove selected");
                butt.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        detailedEditor.deleteItems(selectionGrabber.getSelectedItems());
                        refreshList();
                    }
                });
                buttonPanel.add(butt);
                buttonPanel.add(ViewUtil.getSmallSeparator());
            }

            if (detailedEditor.doesImplementEditing()) {
                JLabel butt = ViewUtil.createIconButton(IconFactory.getInstance().getIcon(IconFactory.StandardIcon.EDIT));
                butt.setToolTipText("Edit selected");
                butt.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (selectionGrabber.getSelectedItems().size() > 0) {
                            detailedEditor.editItems(selectionGrabber.getSelectedItems().get(0));
                            refreshList();
                        } else {
                            DialogUtils.displayMessage("Choose one item to edit");
                        }
                    }
                });
                buttonPanel.add(butt);
            }

            buttonPanel.add(Box.createHorizontalGlue());

            showWaitCard();
            fetchList();
        }

        private void showWaitCard() {
            ((CardLayout)getLayout()).show(this, CARD_WAIT);
        }

        private void showShowCard() {
            ((CardLayout)getLayout()).show(this, CARD_SHOW);
        }

        private void showErrorCard(String message) {
            errorMessage.setText(String.format("<html><font color=\"#ff0000\">An error occurred:<br><font size=\"-2\">%s</font></font></html>", message));
            ((CardLayout)getLayout()).show(this, CARD_ERROR);
        }

        private synchronized void setList(Object[][] list) {
            this.data = list;
            updateShowCard();
            showShowCard();
        }

        public void refreshList() {
            showWaitCard();
            fetchList();
        }

        private void fetchList() {

            SwingWorker sw = new SwingWorker<Object[][], Void>() {

                @Override
                protected Object[][] doInBackground() throws Exception {
                    return detailedListModel.getList(limit);
                }

                @Override
                protected void done() {
                    try {
                        setList(get());
                    } catch (Throwable x) {
                        LOG.error("Unable to load detail list.", x);
                        showErrorCard(MiscUtils.getMessage(x));
                    }
                }
            };

            sw.execute();
        }

        private void updateShowCard() {
            showCard.removeAll();

            showCard.setLayout(new BorderLayout());
            showCard.setBackground(ViewUtil.getTertiaryMenuColor());
            showCard.setBorder(ViewUtil.getBigBorder());

            String[] columnNames = detailedListModel.getColumnNames();
            Class[] columnClasses = detailedListModel.getColumnClasses();
            int[] columnVisibility = detailedListModel.getHiddenColumns();

            stp = new ListViewTablePanel(data, columnNames, columnClasses, columnVisibility) {

                @Override
                public void forceRefreshData() {
                    refreshList();
                }
            };

            selectionGrabber = new RowSelectionGrabber(stp.getTable(), data);

            stp.getTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {

                @Override
                public void valueChanged(ListSelectionEvent e) {

                    if(e.getValueIsAdjusting()) return;

                    List<Object[]> selectedItems = selectionGrabber.getSelectedItems();
                    if (selectedItems.size() == 1) {
                        detailedView.setSelectedItem(selectedItems.get(0));
                    } else {
                        detailedView.setMultipleSelections(selectedItems);
                    }

                }
            });

            stp.getTable().addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if(SwingUtilities.isRightMouseButton(e)) {
                        int row = stp.getTable().rowAtPoint(e.getPoint());
                        stp.getTable().getSelectionModel().setSelectionInterval(row, row);
                        detailedView.setRightClick(e);
                    }
                }
            });

            stp.getTable().getSelectionModel().setSelectionInterval(0, 0);

            showCard.add(stp, BorderLayout.CENTER);

            showCard.add(buttonPanel, BorderLayout.SOUTH);

        }

        public RowSelectionGrabber getSelectionGrabber() {
            return selectionGrabber;
        }
    }
}
