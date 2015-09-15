package uk.ac.shef.dcs.jate.deprecated.io;

import uk.ac.shef.dcs.jate.deprecated.core.feature.indexer.GlobalIndexMem;
import uk.ac.shef.dcs.jate.deprecated.model.Document;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

/**
 * This is a helper class to output in-memory GlobalIndex objects to database HSQL persistence
 *
 * Outputting jate term extraction data as database is no longer supported. the current hsql database outputter can still be used with hsqldb2.2.3 but code will not be maintained
 */
@Deprecated public class GlobalIndexWriterHSQL {
    private static Connection conn;
    private static final int MAX_IN_A_BATCH=5000;

    public static void persist(GlobalIndexMem index, String dbpathFileSystem) {
        conn = HSQLDBUtil.createHSQLDBConnection(dbpathFileSystem);
        try {
            HSQLDBUtil.createJATRGlobalIndexDB(conn);
            HSQLDBUtil.createJATRGlobalIndexDB(conn);
        } catch (SQLException e) {
            System.err.println("ERROR - SEVERE - Cannot establish connection to HSQL database.");
            e.printStackTrace();
            System.exit(1);
        }

        //persist termIdMap
        try {
            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO " + DatabaseTables.TABLE_TERM_2_ID.getTableName() + " VALUES (?, ?)");
            int count=0;
            for (Map.Entry<String, Integer> e : index.getTermIdMap().entrySet()) {
                pstmt.setString(1, e.getKey());
                pstmt.setInt(2, e.getValue());
                pstmt.addBatch();

                count++;
                if(count>=MAX_IN_A_BATCH){
                    pstmt.executeBatch();
                    pstmt.clearBatch();
                    conn.commit();
                    count=0;
                }
            }

            if(count>0)
                pstmt.executeBatch();
            conn.commit();
            pstmt.close();
        } catch (SQLException e) {
            System.err.println("SEVERE - Exception encourted while persisting Term-ID map, may have affected data integrity.");
            e.printStackTrace();
        }

        //persist docIdMap
        try {
            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO " + DatabaseTables.TABLE_DOC_2_ID.getTableName() + " VALUES (?, ?)");
            int count=0;
            for (Map.Entry<Document, Integer> e : index.getDocIdMap().entrySet()) {
                pstmt.setString(1, e.getKey().getUrl().toString());
                pstmt.setInt(2, e.getValue());
                pstmt.addBatch();

                count++;
                if(count>=MAX_IN_A_BATCH){
                    pstmt.executeBatch();
                    pstmt.clearBatch();
                    conn.commit();
                    count=0;
                }
            }
            if(count>0)
                pstmt.executeBatch();
            conn.commit();
            pstmt.close();
        } catch (SQLException e) {
            System.err.println("SEVERE - Exception encourted while persisting Term-ID map, may have affected data integrity.");
            e.printStackTrace();
        }


        //persist termVariantMap
        try {
            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO " + DatabaseTables.TABLE_TERMVARIANT_2_ID.getTableName() + " VALUES (?, ?)");
            int count=0;
            for (Map.Entry<String, Integer> e : index.getVariantIdMap().entrySet()) {
                pstmt.setString(1, e.getKey().toString());
                pstmt.setInt(2, e.getValue());
                pstmt.addBatch();

                count++;
                if(count>=MAX_IN_A_BATCH){
                    pstmt.executeBatch();
                    pstmt.clearBatch();
                    conn.commit();
                    count=0;
                }
            }
            if(count>0)
                pstmt.executeBatch();
            conn.commit();
            pstmt.close();
        } catch (SQLException e) {
            System.err.println("SEVERE - Exception encourted while persisting Term-ID map, may have affected data integrity.");
            e.printStackTrace();
        }


        //persist term2DocMap
        try {
            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO " + DatabaseTables.TABLE_TERMID_2_DOCIDS.getTableName() + " VALUES (?, ?)");
            int count=0;
            for (Map.Entry<Integer, Set<Integer>> e : index.getTerm2Docs().entrySet()) {
                pstmt.setInt(1, e.getKey());
                String docIdStr = "";
                for(Integer i: e.getValue()){
                    docIdStr+=HSQLDBUtil.VALUE_SEPARATOR+i;
                }
                if(docIdStr.startsWith(HSQLDBUtil.VALUE_SEPARATOR))
                    docIdStr=docIdStr.substring(1);
                pstmt.setString(2, docIdStr);
                pstmt.addBatch();

                count++;
                if(count>=MAX_IN_A_BATCH){
                    pstmt.executeBatch();
                    pstmt.clearBatch();
                    conn.commit();
                    count=0;
                }
            }
            if(count>0)
                pstmt.executeBatch();
            conn.commit();
            pstmt.close();
        } catch (SQLException e) {
            System.err.println("SEVERE - Exception encourted while persisting Term-ID map, may have affected data integrity.");
            e.printStackTrace();
        }

        //persist doc2TermMap
        try {
            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO " + DatabaseTables.TABLE_DOCID_2_TERMIDS.getTableName() + " VALUES (?, ?)");
            int count=0;
            for (Map.Entry<Integer, Set<Integer>> e : index.getDoc2Terms().entrySet()) {
                pstmt.setInt(1, e.getKey());
                String termIdStr = "";
                for(Integer i: e.getValue()){
                    termIdStr=HSQLDBUtil.VALUE_SEPARATOR+i;
                }
                if(termIdStr.startsWith(HSQLDBUtil.VALUE_SEPARATOR))
                    termIdStr=termIdStr.substring(1);
                pstmt.setString(2, termIdStr);
                pstmt.addBatch();

                count++;
                if(count>=MAX_IN_A_BATCH){
                    pstmt.executeBatch();
                    pstmt.clearBatch();
                    conn.commit();
                    count=0;
                }
            }
            if(count>0)
                pstmt.executeBatch();
            conn.commit();
            pstmt.close();
        } catch (SQLException e) {
            System.err.println("SEVERE - Exception encourted while persisting Term-ID map, may have affected data integrity.");
            e.printStackTrace();
        }

        //persist termId2VarIds map
        try {
            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO " + DatabaseTables.TABLE_TERMID_2_TERMVARIDS.getTableName() + " VALUES (?, ?)");
            int count=0;
            for (Map.Entry<Integer, Set<Integer>> e : index.getTerm2Variants().entrySet()) {
                pstmt.setInt(1, e.getKey());
                String varIdStr = "";
                for(Integer i: e.getValue()){
                    varIdStr=HSQLDBUtil.VALUE_SEPARATOR+i;
                }
                if(varIdStr.startsWith(HSQLDBUtil.VALUE_SEPARATOR))
                    varIdStr=varIdStr.substring(1);
                pstmt.setString(2, varIdStr);
                pstmt.addBatch();

                count++;
                if(count>=MAX_IN_A_BATCH){
                    pstmt.executeBatch();
                    pstmt.clearBatch();
                    conn.commit();
                    count=0;
                }
            }
            if(count>0)
                pstmt.executeBatch();
            conn.commit();
            pstmt.close();
        } catch (SQLException e) {
            System.err.println("SEVERE - Exception encourted while persisting Term-ID map, may have affected data integrity.");
            e.printStackTrace();
        }


        //persist termVarId2TermId map
        try {
            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO " + DatabaseTables.TABLE_TERMVARID_2_TERMID.getTableName() + " VALUES (?, ?)");
            int count=0;
            for (Map.Entry<Integer, Integer> e : index.getVariant2Term().entrySet()) {
                pstmt.setInt(1, e.getKey());
                pstmt.setInt(2, e.getValue());
                pstmt.addBatch();

                count++;
                if(count>=MAX_IN_A_BATCH){
                    pstmt.executeBatch();
                    pstmt.clearBatch();
                    conn.commit();
                    count=0;
                }
            }
            if(count>0)
                pstmt.executeBatch();
            conn.commit();
            pstmt.close();
        } catch (SQLException e) {
            System.err.println("SEVERE - Exception encourted while persisting Term-ID map, may have affected data integrity.");
            e.printStackTrace();
        }


        HSQLDBUtil.closeHSQLDBConnection(conn);
    }


}
