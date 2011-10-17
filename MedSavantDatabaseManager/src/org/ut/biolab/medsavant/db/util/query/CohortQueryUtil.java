/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ut.biolab.medsavant.db.util.query;

import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.DeleteQuery;
import com.healthmarketscience.sqlbuilder.InsertQuery;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import org.ut.biolab.medsavant.db.model.Cohort;
import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.ut.biolab.medsavant.db.model.structure.CustomTables;
import org.ut.biolab.medsavant.db.model.structure.MedSavantDatabase;
import org.ut.biolab.medsavant.db.model.structure.MedSavantDatabase.CohortTableSchema;
import org.ut.biolab.medsavant.db.model.structure.MedSavantDatabase.CohortmembershipTableSchema;
import org.ut.biolab.medsavant.db.model.structure.MedSavantDatabase.DefaultpatientTableSchema;
import org.ut.biolab.medsavant.db.model.structure.MedSavantDatabase.PatienttablemapTableSchema;
import org.ut.biolab.medsavant.db.model.structure.TableSchema;
import org.ut.biolab.medsavant.db.table.CohortTable;
import org.ut.biolab.medsavant.db.util.ConnectionController;

/**
 *
 * @author Andrew
 */
public class CohortQueryUtil {
    
    public static List<Integer> getIndividualsInCohort(int cohortId) throws SQLException {
        
        TableSchema table = MedSavantDatabase.CohortmembershipTableSchema;
        SelectQuery query = new SelectQuery();
        query.addFromTable(table.getTable());
        query.addColumns(table.getDbColumn(CohortmembershipTableSchema.COLUMNNAME_OF_PATIENT_ID));
        query.addCondition(BinaryCondition.equalTo(table.getDbColumn(CohortmembershipTableSchema.COLUMNNAME_OF_COHORT_ID), cohortId));
        
        ResultSet rs = ConnectionController.connect().createStatement().executeQuery(query.toString());
        
        List<Integer> result = new ArrayList<Integer>();
        while(rs.next()){
            result.add(rs.getInt(1));
        }
        return result;
    }
    
    public static List<String> getDNAIdsInCohort(int cohortId) throws SQLException {

        Connection c = ConnectionController.connect();
        TableSchema patientMapTable = MedSavantDatabase.PatienttablemapTableSchema;
        TableSchema cohortTable = MedSavantDatabase.CohortTableSchema;
        TableSchema cohortMembershipTable = MedSavantDatabase.CohortmembershipTableSchema;
        
        //get patient tablename
        SelectQuery query1 = new SelectQuery();
        query1.addFromTable(patientMapTable.getTable());
        query1.addFromTable(cohortTable.getTable());
        query1.addColumns(patientMapTable.getDbColumn(PatienttablemapTableSchema.COLUMNNAME_OF_PATIENT_TABLENAME));
        query1.addCondition(BinaryCondition.equalTo(cohortTable.getDbColumn(CohortTableSchema.COLUMNNAME_OF_COHORT_ID), cohortId));
        query1.addCondition(BinaryCondition.equalTo(cohortTable.getDbColumn(CohortTableSchema.COLUMNNAME_OF_PROJECT_ID), patientMapTable.getDbColumn(PatienttablemapTableSchema.COLUMNNAME_OF_PROJECT_ID)));
        
        ResultSet rs = c.createStatement().executeQuery(query1.toString());
        rs.next();
        String patientTablename = rs.getString(1);
        
        //get dna id lists
        TableSchema patientTable = CustomTables.getPatientTableSchema(patientTablename);
        SelectQuery query2 = new SelectQuery();
        query2.addFromTable(cohortMembershipTable.getTable());
        query2.addFromTable(patientTable.getTable());
        query2.addColumns(patientTable.getDbColumn(DefaultpatientTableSchema.COLUMNNAME_OF_DNA_IDS));
        query2.addCondition(BinaryCondition.equalTo(cohortMembershipTable.getDbColumn(CohortmembershipTableSchema.COLUMNNAME_OF_COHORT_ID), cohortId));
        query2.addCondition(BinaryCondition.equalTo(cohortMembershipTable.getDbColumn(CohortmembershipTableSchema.COLUMNNAME_OF_PATIENT_ID), DefaultpatientTableSchema.COLUMNNAME_OF_PATIENT_ID));
        
        rs = c.createStatement().executeQuery(query2.toString());
        
        List<String> result = new ArrayList<String>();
        while(rs.next()){          
            String[] dnaIds = rs.getString(1).split(",");
            for(String id : dnaIds){
                if(!result.contains(id)){
                    result.add(id);
                }
            }
        }
        return result;
    }
    
    public static void addPatientsToCohort(int[] patientIds, int cohortId) throws SQLException {
        
        TableSchema table = MedSavantDatabase.CohortmembershipTableSchema;
        
        Connection c = ConnectionController.connect();
        c.setAutoCommit(false);
        
        for(int id : patientIds){
            try {
                InsertQuery query = new InsertQuery(table.getTable());
                query.addColumn(table.getDbColumn(CohortmembershipTableSchema.COLUMNNAME_OF_COHORT_ID), cohortId);
                query.addColumn(table.getDbColumn(CohortmembershipTableSchema.COLUMNNAME_OF_PATIENT_ID), id);             
                c.createStatement().executeUpdate(query.toString());
            } catch (MySQLIntegrityConstraintViolationException e){
                //duplicate entry, ignore
            }
        }
 
        c.commit();
        c.setAutoCommit(true);
    }
    
    public static void removePatientsFromCohort(int[] patientIds, int cohortId) throws SQLException {
        
        TableSchema table = MedSavantDatabase.CohortmembershipTableSchema;
        
        Connection c = ConnectionController.connect();
        c.setAutoCommit(false);
        
        for(int id : patientIds){
            DeleteQuery query = new DeleteQuery(table.getTable());
            query.addCondition(BinaryCondition.equalTo(table.getDbColumn(CohortmembershipTableSchema.COLUMNNAME_OF_COHORT_ID), cohortId));
            query.addCondition(BinaryCondition.equalTo(table.getDbColumn(CohortmembershipTableSchema.COLUMNNAME_OF_PATIENT_ID), id));
            c.createStatement().executeUpdate(query.toString());
        }
 
        c.commit();
        c.setAutoCommit(true);
    }
    
    public static List<Cohort> getCohorts(int projectId) throws SQLException {
        
        TableSchema table = MedSavantDatabase.CohortTableSchema;
        SelectQuery query = new SelectQuery();
        query.addFromTable(table.getTable());
        query.addAllColumns();
        query.addCondition(BinaryCondition.equalTo(table.getDbColumn(CohortTableSchema.COLUMNNAME_OF_PROJECT_ID), projectId));
        
        ResultSet rs = ConnectionController.connect().createStatement().executeQuery(query.toString());
      
        List<Cohort> result = new ArrayList<Cohort>();
        while(rs.next()){
            result.add(new Cohort(rs.getInt(CohortTable.FIELDNAME_ID), rs.getString(CohortTable.FIELDNAME_NAME)));
        }
        return result;
    }

    public static void addCohort(int projectId, String name) throws SQLException {
        
        TableSchema table = MedSavantDatabase.CohortTableSchema;
        InsertQuery query = new InsertQuery(table.getTable());
        query.addColumn(table.getDbColumn(CohortTableSchema.COLUMNNAME_OF_PROJECT_ID), projectId);
        query.addColumn(table.getDbColumn(CohortTableSchema.COLUMNNAME_OF_NAME), name);
        
        ConnectionController.connect().createStatement().executeUpdate(query.toString());
    }
    
    public static void removeCohort(int cohortId) throws SQLException {
        
        TableSchema cohortMembershipTable = MedSavantDatabase.CohortmembershipTableSchema;
        TableSchema cohortTable = MedSavantDatabase.CohortTableSchema;
        Connection c = ConnectionController.connect();
        
        //remove all entries from membership
        DeleteQuery query1 = new DeleteQuery(cohortMembershipTable.getTable());
        query1.addCondition(BinaryCondition.equalTo(cohortMembershipTable.getDbColumn(CohortmembershipTableSchema.COLUMNNAME_OF_COHORT_ID), cohortId));
        c.createStatement().execute(query1.toString());
        
        //remove from cohorts
        DeleteQuery query2 = new DeleteQuery(cohortTable.getTable());
        query2.addCondition(BinaryCondition.equalTo(cohortTable.getDbColumn(CohortTableSchema.COLUMNNAME_OF_COHORT_ID), cohortId));
        c.createStatement().execute(query2.toString());
        
    }
    
    public static void removeCohorts(Cohort[] cohorts) throws SQLException {
        for(Cohort c : cohorts){
            removeCohort(c.getId());
        }
    }
    
    public static List<Integer> getCohortIds(int projectId) throws SQLException {
        
        TableSchema table = MedSavantDatabase.CohortTableSchema;
        SelectQuery query = new SelectQuery();
        query.addFromTable(table.getTable());
        query.addColumns(table.getDbColumn(CohortTableSchema.COLUMNNAME_OF_COHORT_ID));
        query.addCondition(BinaryCondition.equalTo(table.getDbColumn(CohortTableSchema.COLUMNNAME_OF_PROJECT_ID), projectId));
        
        ResultSet rs = ConnectionController.connect().createStatement().executeQuery(query.toString());
        
        List<Integer> result = new ArrayList<Integer>();
        while(rs.next()){
            result.add(rs.getInt(1));
        }
        return result;
    }
    
    public static void removePatientReferences(int projectId, int patientId) throws SQLException {
        
        List<Integer> cohortIds = getCohortIds(projectId);
        
        TableSchema table = MedSavantDatabase.CohortmembershipTableSchema;
        Connection c = ConnectionController.connect();
        
        for(Integer cohortId : cohortIds){
            DeleteQuery query = new DeleteQuery(table.getTable());
            query.addCondition(BinaryCondition.equalTo(table.getDbColumn(CohortmembershipTableSchema.COLUMNNAME_OF_COHORT_ID), cohortId));
            query.addCondition(BinaryCondition.equalTo(table.getDbColumn(CohortmembershipTableSchema.COLUMNNAME_OF_PATIENT_ID), patientId));
            c.createStatement().executeUpdate(query.toString());
        }
    }

}
