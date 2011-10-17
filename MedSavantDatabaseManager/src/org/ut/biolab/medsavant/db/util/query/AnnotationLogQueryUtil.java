/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ut.biolab.medsavant.db.util.query;

import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.InsertQuery;
import com.healthmarketscience.sqlbuilder.OrderObject;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import com.healthmarketscience.sqlbuilder.UpdateQuery;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import org.ut.biolab.medsavant.db.model.structure.MedSavantDatabase;
import org.ut.biolab.medsavant.db.model.structure.MedSavantDatabase.VariantpendingupdateTableSchema;
import org.ut.biolab.medsavant.db.model.structure.TableSchema;
import org.ut.biolab.medsavant.db.util.ConnectionController;
import org.ut.biolab.medsavant.db.util.DBUtil;

/**
 *
 * @author Andrew
 */
public class AnnotationLogQueryUtil {

   
    public static enum Action {ADD_VARIANTS, UPDATE_TABLE};
    public static enum Status {PREPROCESS, PENDING, INPROGRESS, ERROR, COMPLETE}; 
    
    private static int actionToInt(Action action){
        switch(action){
            case UPDATE_TABLE:
                return 0;
            case ADD_VARIANTS:
                return 1;
            default:
                return -1;
        }
    }
    
    public static Action intToAction(int action){
        switch(action){
            case 0:
                return Action.UPDATE_TABLE;
            case 1:
                return Action.ADD_VARIANTS;
            default:
                return null;
        }
    }
    
    private static int statusToInt(Status status){
        switch(status){
            case PREPROCESS:
                return 0;
            case PENDING:
                return 1;
            case INPROGRESS:
                return 2;
            case ERROR:
                return 3;
            case COMPLETE:
                return 4;
            default:
                return -1;
        }
    }
    
    public static Status intToStatus(int status){
        switch(status){
            case 0:
                return Status.PREPROCESS;
            case 1:
                return Status.PENDING;
            case 2:
                return Status.INPROGRESS;
            case 3:
                return Status.ERROR;
            case 4:
                return Status.COMPLETE;
            default:
                return null;
        }
    }
    
    public static int addAnnotationLogEntry(int projectId, int referenceId, Action action) throws SQLException{    
        return addAnnotationLogEntry(projectId,referenceId,action,Status.PREPROCESS);
    }
    
    public static int addAnnotationLogEntry(int projectId, int referenceId, Action action, Status status) throws SQLException {
        Timestamp sqlDate = DBUtil.getCurrentTimestamp();
        
        TableSchema table = MedSavantDatabase.VariantpendingupdateTableSchema;
        InsertQuery query = new InsertQuery(table.getTable());
        query.addColumn(table.getDbColumn(VariantpendingupdateTableSchema.COLUMNNAME_OF_PROJECT_ID), projectId);
        query.addColumn(table.getDbColumn(VariantpendingupdateTableSchema.COLUMNNAME_OF_REFERENCE_ID), referenceId);
        query.addColumn(table.getDbColumn(VariantpendingupdateTableSchema.COLUMNNAME_OF_ACTION), actionToInt(action));
        query.addColumn(table.getDbColumn(VariantpendingupdateTableSchema.COLUMNNAME_OF_STATUS), statusToInt(status));
        query.addColumn(table.getDbColumn(VariantpendingupdateTableSchema.COLUMNNAME_OF_TIMESTAMP), sqlDate);

        PreparedStatement stmt = (ConnectionController.connect()).prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS);
        stmt.execute();
        
        ResultSet rs = stmt.getGeneratedKeys();
        rs.next();
        return rs.getInt(1);
    }
    
    public static ResultSet getPendingUpdates() throws SQLException, IOException{
        
        TableSchema table = MedSavantDatabase.VariantpendingupdateTableSchema;
        SelectQuery query = new SelectQuery();
        query.addFromTable(table.getTable());
        query.addAllColumns();
        query.addCondition(BinaryCondition.equalTo(table.getDbColumn(VariantpendingupdateTableSchema.COLUMNNAME_OF_STATUS), statusToInt(Status.PENDING)));
        query.addOrdering(table.getDbColumn(VariantpendingupdateTableSchema.COLUMNNAME_OF_ACTION), OrderObject.Dir.ASCENDING);
        
        ResultSet rs = ConnectionController.connect().createStatement().executeQuery(query.toString());
        
        return rs;
    }
    
    public static void setAnnotationLogStatus(int updateId, Status status) throws SQLException {
        
        TableSchema table = MedSavantDatabase.VariantpendingupdateTableSchema;
        UpdateQuery query = new UpdateQuery(table.getTable());
        query.addSetClause(table.getDbColumn(VariantpendingupdateTableSchema.COLUMNNAME_OF_STATUS), statusToInt(status));
        query.addCondition(BinaryCondition.equalTo(table.getDbColumn(VariantpendingupdateTableSchema.COLUMNNAME_OF_UPDATE_ID), updateId));
        
        ConnectionController.connect().createStatement().executeUpdate(query.toString());
    }
    
    public static void setAnnotationLogStatus(int updateId, Status status, Timestamp sqlDate) throws SQLException {
        
        TableSchema table = MedSavantDatabase.VariantpendingupdateTableSchema;
        UpdateQuery query = new UpdateQuery(table.getTable());
        query.addSetClause(table.getDbColumn(VariantpendingupdateTableSchema.COLUMNNAME_OF_STATUS), statusToInt(status));
        query.addSetClause(table.getDbColumn(VariantpendingupdateTableSchema.COLUMNNAME_OF_TIMESTAMP), sqlDate);
        query.addCondition(BinaryCondition.equalTo(table.getDbColumn(VariantpendingupdateTableSchema.COLUMNNAME_OF_UPDATE_ID), updateId));
        
        ConnectionController.connect().createStatement().executeUpdate(query.toString());
    }
    
}
