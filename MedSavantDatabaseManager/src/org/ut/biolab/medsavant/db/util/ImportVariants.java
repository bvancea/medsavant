/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ut.biolab.medsavant.db.util;

import java.io.File;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import org.ut.biolab.medsavant.db.util.query.AnnotationLogQueryUtil;
import org.ut.biolab.medsavant.db.util.query.AnnotationLogQueryUtil.Action;
import org.ut.biolab.medsavant.db.util.query.ProjectQueryUtil;
import org.ut.biolab.medsavant.db.util.query.VariantQueryUtil;
import org.ut.biolab.medsavant.vcf.VCFParser;

/**
 *
 * @author Andrew
 */
public class ImportVariants {

    /*
    public static int performImport(File[] vcfFiles, int projectId, int referenceId) throws SQLException {
        return performImport(vcfFiles, projectId, referenceId);
    }
     *
     */

    public static int performImport(File[] vcfFiles, int projectId, int referenceId, JLabel progressLabel) throws SQLException, InterruptedException {

        //add log
        int updateId = AnnotationLogQueryUtil.addAnnotationLogEntry(projectId, referenceId, Action.ADD_VARIANTS);


        //get custom fields for vcf
        /*List<CustomField> customFields = ProjectQueryUtil.getCustomVariantFields(projectId);
        String[] infoFields = new String[customFields.size()];
        Class[] infoClasses = new Class[customFields.size()];
        for(int i = 0; i < customFields.size(); i++){
            infoFields[i] = customFields.get(i).getColumnName();
            infoClasses[i] = customFields.get(i).getColumnClass();
        }*/

        //create the staging table
        String tableName = DBSettings.createVariantStagingTableName(projectId, referenceId, updateId);
        checkInterrupt(updateId, tableName);
        try {
            tableName = ProjectQueryUtil.createVariantTable(projectId, referenceId, updateId, null, true);
        } catch (SQLException ex) {
            //table already exists?
        }

        //add files to staging table
        for(int i = 0; i < vcfFiles.length; i++){

            checkInterrupt(updateId, tableName);

            //update progress
            String progress = "Importing file " + (i+1) + " of " + vcfFiles.length + "...";
            if(progressLabel != null){
                progressLabel.setText(progress);
            }

            //create temp file
            File outfile = new File("temp_tdf"); //TODO: should put this in a temp dir or something

            //parse vcf file
            try {
                VCFParser.parseVariants(vcfFiles[i], outfile, updateId, i);
            } catch (Exception ex) {
                Logger.getLogger(ImportVariants.class.getName()).log(Level.SEVERE, null, ex);
                return -1;
            }

            //add to staging table
            checkInterrupt(updateId, tableName);
            try {
                if(progressLabel != null){
                    progressLabel.setText("Uploading file " + (i+1) + " of " + vcfFiles.length + "...");
                    progressLabel.updateUI();
                }
                VariantQueryUtil.uploadFileToVariantTable(outfile, tableName);
            } catch (SQLException ex) {
                Logger.getLogger(ImportVariants.class.getName()).log(Level.SEVERE, null, ex);
                return -1;
            }

            //cleanup
            //outfile.delete();
            System.gc();
        }

        //set log as pending
        checkInterrupt(updateId, tableName);
        AnnotationLogQueryUtil.setAnnotationLogStatus(updateId, AnnotationLogQueryUtil.Status.PENDING);
        return updateId;
    }

    public static boolean addTagsToUpload(int uploadID, String[][] variantTags) {
        try {
            VariantQueryUtil.addTagsToUpload(uploadID,variantTags);
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private static void checkInterrupt(int updateId, String tableName) throws InterruptedException{
        if(Thread.interrupted()){
            throw new InterruptedException(Integer.toString(updateId) + ";" + tableName);
        }
    }
}
