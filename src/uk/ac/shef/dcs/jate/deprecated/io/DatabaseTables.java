package uk.ac.shef.dcs.jate.deprecated.io;

/**
 * Outputting jate term extraction data as database is no longer supported. the current hsql database outputter can still be used with hsqldb2.2.3 but code will not be maintained
 */
@Deprecated public enum DatabaseTables {

    TABLE_TERM_2_ID("tableTerm2Id","termString","termId","VARCHAR(255)","INTEGER"),
    TABLE_TERMVARIANT_2_ID("tableTermVar2Id","termVarString","varId","VARCHAR(255)","INTEGER"),
    TABLE_DOC_2_ID("tableDocUrl2Id","docUrl","docId", "LONGVARCHAR","INTEGER"),
    TABLE_TERMID_2_DOCIDS("tableTermId2DocIds","termId","docIds","INTEGER","LONGVARCHAR"),
    TABLE_DOCID_2_TERMIDS("tableDocId2TermIds","docId","termIds","INTEGER","LONGVARCHAR"),
    TABLE_TERMID_2_TERMVARIDS("tableTermId2TermVarIds","termId","varIds","INTEGER","LONGVARCHAR"),
    TABLE_TERMVARID_2_TERMID("tableTermVarId2TermId","varId","termId","INTEGER","INTEGER");

    private String tableName;
    private String primaryKeyField;
    private String valueField;
    private String primaryKeyDatatype;
    private String valueDatatype;

    DatabaseTables(String tableName, String primaryKeyField, String valueField, String primaryKeyDatatype, String valueDatatype){
        this.tableName=tableName;
        this.primaryKeyField = primaryKeyField;
        this.valueField=valueField;
        this.primaryKeyDatatype=primaryKeyDatatype;
        this.valueDatatype=valueDatatype;
    }

    public String getPrimaryKeyField() {
        return primaryKeyField;
    }

    public String getValueField() {
        return valueField;
    }

    public String getPrimaryKeyDatatype() {
        return primaryKeyDatatype;
    }

    public String getValueDatatype() {
        return valueDatatype;
    }

    public String getTableName() {
        return tableName;
    }
}
