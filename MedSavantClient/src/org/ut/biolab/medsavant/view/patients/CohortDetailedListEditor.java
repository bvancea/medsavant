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
package org.ut.biolab.medsavant.view.patients;

import java.util.List;

import org.ut.biolab.medsavant.MedSavantClient;
import org.ut.biolab.medsavant.login.LoginController;
import org.ut.biolab.medsavant.model.Cohort;
import org.ut.biolab.medsavant.util.ClientMiscUtils;
import org.ut.biolab.medsavant.util.MiscUtils;
import org.ut.biolab.medsavant.view.dialog.IndeterminateProgressDialog;
import org.ut.biolab.medsavant.view.list.DetailedListEditor;
import org.ut.biolab.medsavant.view.util.DialogUtils;

/**
 *
 * @author mfiume
 */
public class CohortDetailedListEditor extends DetailedListEditor {

    @Override
    public boolean doesImplementAdding() {
        return true;
    }

    @Override
    public boolean doesImplementDeleting() {
        return true;
    }

    @Override
    public void addItems() {
        new CohortWizard().setVisible(true);
    }

    @Override
    public void deleteItems(final List<Object[]> items) {

        int result;

        if (items.size() == 1) {
            String name = ((Cohort) items.get(0)[0]).getName();
            result = DialogUtils.askYesNo("Confirm", "Are you sure you want to remove %s?\nThis cannot be undone.", name);
        } else {
            result = DialogUtils.askYesNo("Confirm", "Are you sure you want to remove these %d cohorts?\nThis cannot be undone.", items.size());
        }


        if (result == DialogUtils.YES) {

            new IndeterminateProgressDialog("Removing Cohort(s)", items.size() + " cohort(s) being removed.  Please wait.") {
                @Override
                public void run() {
                    int numCouldntRemove = 0;
                    for (Object[] v : items) {
                        int id = ((Cohort) v[0]).getId();
                        try {
                            MedSavantClient.CohortQueryUtilAdapter.removeCohort(LoginController.sessionId, id);
                        } catch (Throwable ex) {
                            numCouldntRemove++;
                            ClientMiscUtils.reportError("Error removing " + ((Cohort)v[0]).getName() + ": " + MiscUtils.getMessage(ex), ex);
                        }
                    }

                    if (numCouldntRemove != items.size()) {
                        DialogUtils.displayMessage("Successfully removed " + (items.size() - numCouldntRemove) + " cohort(s)");
                    }
                }
            }.setVisible(true);
        }
    }
}