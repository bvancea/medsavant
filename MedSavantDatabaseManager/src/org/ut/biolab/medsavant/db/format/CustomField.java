/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ut.biolab.medsavant.db.format;

import java.sql.Date;
import org.ut.biolab.medsavant.db.model.structure.TableSchema;
import org.ut.biolab.medsavant.db.model.structure.TableSchema.ColumnType;
import org.ut.biolab.medsavant.db.util.DBUtil;

/**
 *
 * @author Andrew
 */
public class CustomField {

    private String columnName;
    private String columnType;
    private boolean filterable;
    private String alias;
    private String description;
    private ColumnType fieldType;
    private Category category;

    public static enum Category {PATIENT, GENOTYPE, VARIANT, GENOME_COORDS, PLUGIN}

    public static String categoryToString(Category cat){
        switch(cat){
            case PATIENT:
                return "Patient & Phenotype";
            case VARIANT:
                return "Variant Annotation";
            case GENOME_COORDS:
                return "Genomic Coordinates";
            case PLUGIN:
                return "Plugins";
            default:
                return "undefined";
        }
    }

    public CustomField(String name, String type, boolean filterable, String alias, String description){
        this(name, type, filterable, alias, description, Category.VARIANT);
    }

    public CustomField(String name, String type, boolean filterable, String alias, String description, Category category){
        this.columnName = name;
        this.columnType = type;
        this.filterable = filterable;
        this.alias = alias;
        this.description = description;
        setColumnType(columnType);
        this.category = category;
    }

    public String getAlias() {
        return alias;
    }

    public String getColumnName() {
        return columnName;
    }

    public String getColumnTypeString() {
        return columnType;
    }

    public String getDescription() {
        return description;
    }

    public boolean isFilterable() {
        return filterable;
    }

    private void setColumnType(String type){
        fieldType = TableSchema.convertStringToColumnType(type);
    }

    public ColumnType getColumnType(){
        return fieldType;
    }

    public Class getColumnClass(){
        switch(fieldType){
            case BOOLEAN:
                return Boolean.class;
            case INTEGER:
                return Integer.class;
            case DATE:
                return Date.class;
            case DECIMAL:
                return Double.class;
            case FLOAT:
                return Float.class;
            case VARCHAR:
            default:
                return String.class;
        }
    }

    public String getColumnLength(){
        int posLeft = columnType.indexOf("(");
        if(posLeft == -1){
            return "";
        }
        int posRight = columnType.indexOf(")");
        return columnType.substring(posLeft+1, posRight);
    }

    public String generateSchema(){
        return generateSchema(false);
    }

    public String generateSchema(boolean forceLowerCase){
        return "`" + (forceLowerCase ? columnName.toLowerCase() : columnName) + "` " + columnType + " DEFAULT NULL,";
    }

    public Category getCategory() {
        return category;
    }

    public boolean isNumeric() {
        return this.fieldType == ColumnType.DECIMAL || fieldType == ColumnType.FLOAT || fieldType == ColumnType.INTEGER;
    }

    @Override
    public String toString(){
        return alias;
    }

    @Override
    public boolean equals(Object o){
        if(o == null || !o.getClass().equals(CustomField.class)) return false;
        CustomField other = (CustomField)o;
        return this.getColumnName().equals(other.getColumnName()) &&
                this.getColumnTypeString().equals(other.getColumnTypeString());
    }

}
