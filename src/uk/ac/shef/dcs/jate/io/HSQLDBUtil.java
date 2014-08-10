package uk.ac.shef.dcs.jate.io;
import uk.ac.shef.dcs.jate.model.Document;
import uk.ac.shef.dcs.jate.model.DocumentImpl;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * An utility class for reading and writing data in a GlobalIndex object into HSQL persistence
 * Outputting jate term extraction data as database is no longer supported. the current hsql database outputter can still be used with hsqldb2.2.3 but code will not be maintained
 */
@Deprecated public class HSQLDBUtil {

    public static final String VALUE_SEPARATOR=",";

    /**
     * Establish a database connection to a HSQL database (file system)
     * @param hsqldbName the file path to the HSQL database (file system). See HSQL documentation at http://hsqldb.org/doc/2.0/guide/running-chapt.html
     * @return a database connection
     */
    public static Connection createHSQLDBConnection(String hsqldbName) {
        try {
            Class.forName("org.hsqldb.jdbcDriver");
            Connection conn = DriverManager.getConnection("jdbc:hsqldb:file:"
                    + hsqldbName,    // filenames
                    "sa",            // username
                    "");             // pwd
            return conn;
        } catch (Exception e) {
            System.err.println("ERROR SEVERE - Cannot establish database connection to JATR internal database. Please check your " +
                    "claspath to HSQL library, and database path: " + hsqldbName);
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    public static void closeHSQLDBConnection(Connection conn) {
        Statement st = null;
        try {
            st = conn.createStatement();
            st.execute("SHUTDOWN");
            conn.close();
        } catch (SQLException e) {
            System.err.println("Error - Database may not be closed properly.");
            e.printStackTrace();
        }
    }

    public static void createJATRGlobalIndexDB(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute("DROP TABLE IF EXISTS " + DatabaseTables.TABLE_DOCID_2_TERMIDS.getTableName());
        stmt.execute("DROP TABLE IF EXISTS " + DatabaseTables.TABLE_TERMID_2_DOCIDS.getTableName());
        stmt.execute("DROP TABLE IF EXISTS " + DatabaseTables.TABLE_TERMID_2_TERMVARIDS.getTableName());
        stmt.execute("DROP TABLE IF EXISTS " + DatabaseTables.TABLE_TERMVARID_2_TERMID.getTableName());
        stmt.execute("DROP TABLE IF EXISTS " + DatabaseTables.TABLE_DOC_2_ID.getTableName());
        stmt.execute("DROP TABLE IF EXISTS " + DatabaseTables.TABLE_TERM_2_ID.getTableName());
        stmt.execute("DROP TABLE IF EXISTS " + DatabaseTables.TABLE_TERMVARIANT_2_ID.getTableName());

        conn.commit();
        stmt.execute("create table " + DatabaseTables.TABLE_TERM_2_ID.getTableName() + " (" +
                DatabaseTables.TABLE_TERM_2_ID.getPrimaryKeyField()+" "+DatabaseTables.TABLE_TERM_2_ID.getPrimaryKeyDatatype()+", "+
                DatabaseTables.TABLE_TERM_2_ID.getValueField()+" "+DatabaseTables.TABLE_TERM_2_ID.getValueDatatype()+")");
        stmt.execute("create table " + DatabaseTables.TABLE_DOC_2_ID.getTableName() + " (" +
                DatabaseTables.TABLE_DOC_2_ID.getPrimaryKeyField()+" "+DatabaseTables.TABLE_DOC_2_ID.getPrimaryKeyDatatype()+", "+
                DatabaseTables.TABLE_DOC_2_ID.getValueField()+" "+DatabaseTables.TABLE_DOC_2_ID.getValueDatatype()+")");
        stmt.execute("create table " + DatabaseTables.TABLE_TERMVARIANT_2_ID.getTableName() + " (" +
                DatabaseTables.TABLE_TERMVARIANT_2_ID.getPrimaryKeyField()+" "+DatabaseTables.TABLE_TERMVARIANT_2_ID.getPrimaryKeyDatatype()+", "+
                DatabaseTables.TABLE_TERMVARIANT_2_ID.getValueField()+" "+DatabaseTables.TABLE_TERMVARIANT_2_ID.getValueDatatype()+")");
        stmt.execute("create table " + DatabaseTables.TABLE_TERMID_2_DOCIDS.getTableName() + " (" +
                DatabaseTables.TABLE_TERMID_2_DOCIDS.getPrimaryKeyField()+" "+DatabaseTables.TABLE_TERMID_2_DOCIDS.getPrimaryKeyDatatype()+", "+
                DatabaseTables.TABLE_TERMID_2_DOCIDS.getValueField()+" "+DatabaseTables.TABLE_TERMID_2_DOCIDS.getValueDatatype()+")");
        stmt.execute("create table " + DatabaseTables.TABLE_DOCID_2_TERMIDS.getTableName() + " (" +
                DatabaseTables.TABLE_DOCID_2_TERMIDS.getPrimaryKeyField()+" "+DatabaseTables.TABLE_DOCID_2_TERMIDS.getPrimaryKeyDatatype()+", "+
                DatabaseTables.TABLE_DOCID_2_TERMIDS.getValueField()+" "+DatabaseTables.TABLE_DOCID_2_TERMIDS.getValueDatatype()+")");
        stmt.execute("create table " + DatabaseTables.TABLE_TERMID_2_TERMVARIDS.getTableName() + " (" +
                DatabaseTables.TABLE_TERMID_2_TERMVARIDS.getPrimaryKeyField()+" "+DatabaseTables.TABLE_TERMID_2_TERMVARIDS.getPrimaryKeyDatatype()+", "+
                DatabaseTables.TABLE_TERMID_2_TERMVARIDS.getValueField()+" "+DatabaseTables.TABLE_TERMID_2_TERMVARIDS.getValueDatatype()+")");
        stmt.execute("create table " + DatabaseTables.TABLE_TERMVARID_2_TERMID.getTableName() + " (" +
                DatabaseTables.TABLE_TERMVARID_2_TERMID.getPrimaryKeyField()+" "+DatabaseTables.TABLE_TERMVARID_2_TERMID.getPrimaryKeyDatatype()+", "+
                DatabaseTables.TABLE_TERMVARID_2_TERMID.getValueField()+" "+DatabaseTables.TABLE_TERMVARID_2_TERMID.getValueDatatype()+")");
        conn.commit();
    }

    public static void addTermString2Id(String termString, int id, Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("INSERT INTO " + DatabaseTables.TABLE_TERM_2_ID.getTableName() + " VALUES(" +
                "'" + termString.replaceAll("'", "\\\\'") + "'," +
                "'" + id + "')");
        conn.commit();
    }

    public static void addDoc2Id(Document doc, int id, Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("INSERT INTO " + DatabaseTables.TABLE_DOC_2_ID.getTableName() + " VALUES(" +
                "'" + doc.getUrl().toString().replaceAll("'", "\\\\'") + "'," +
                "'" + id + "')");
        conn.commit();
    }

    public static void addTermVarString2Id(String termVarString, int id, Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("INSERT INTO " + DatabaseTables.TABLE_TERMVARIANT_2_ID.getTableName() + " VALUES(" +
                "'" + termVarString.replaceAll("'", "\\\\'") + "'," +
                "'" + id + "')");
        conn.commit();
    }

    public static void addTermId2DocId(Connection conn, int termid, int... docids) throws SQLException {
        Set<Integer> alreadyExist = getDocIdsContainTermId(termid, conn);
        Statement stmt = conn.createStatement();
        if (alreadyExist != null) {
            stmt.executeUpdate("DELETE FROM " + DatabaseTables.TABLE_TERMID_2_DOCIDS.getTableName() + " WHERE "+
                    DatabaseTables.TABLE_TERMID_2_DOCIDS.getPrimaryKeyField()+"='" + termid + "'");
        }

        String docidstr = "";
        for (int i : docids)
            docidstr = docidstr + VALUE_SEPARATOR + i;
        if (docidstr.startsWith(VALUE_SEPARATOR))
            docidstr = docidstr.substring(1);

        stmt.executeUpdate("INSERT INTO " + DatabaseTables.TABLE_TERMID_2_DOCIDS.getTableName() + " VALUES(" +
                "'" + termid + "'," +
                "'" + docidstr + "')");
        conn.commit();
    }

    public static void addTermId2VariantIds(Connection conn, int termid, int... varids) throws SQLException {
        Set<Integer> alreadyExist = getTermVarIds4TermId(termid, conn);
        Statement stmt = conn.createStatement();
        if (alreadyExist != null) {
            stmt.executeUpdate("DELETE FROM " + DatabaseTables.TABLE_TERMID_2_TERMVARIDS.getTableName() + " WHERE "+
                    DatabaseTables.TABLE_TERMID_2_TERMVARIDS.getPrimaryKeyField()+"='" + termid + "'");
        }

        String varidstr = "";
        for (int i : varids)
            varidstr = varidstr + VALUE_SEPARATOR + i;
        if (varidstr.startsWith(VALUE_SEPARATOR))
            varidstr = varidstr.substring(1);

        stmt.executeUpdate("INSERT INTO " + DatabaseTables.TABLE_TERMID_2_TERMVARIDS.getTableName() + " VALUES(" +
                "'" + termid + "'," +
                "'" + varidstr + "')");
        conn.commit();
    }

    public static void addVariantId2TermId(Connection conn, int varid, int termid) throws SQLException {
        Set<Integer> alreadyExist = getTermVarIds4TermId(termid, conn);
        Statement stmt = conn.createStatement();
        if (alreadyExist != null) {
            stmt.executeUpdate("DELETE FROM " + DatabaseTables.TABLE_TERMVARID_2_TERMID.getTableName() + " WHERE "+
                    DatabaseTables.TABLE_TERMVARID_2_TERMID.getPrimaryKeyField()+"='" + varid + "'");
        }

        stmt.executeUpdate("INSERT INTO " + DatabaseTables.TABLE_TERMVARID_2_TERMID.getTableName() + " VALUES(" +
                "'" + varid + "'," +
                "'" + termid + "')");
        conn.commit();
    }


    public static void addDocId2TermIds(Connection conn, int docid, int... termids) throws SQLException {
        Set<Integer> alreadyExist = getTermIdsInDocId(docid, conn);
        Statement stmt = conn.createStatement();
        if (alreadyExist != null) {
            stmt.executeUpdate("DELETE FROM " + DatabaseTables.TABLE_DOCID_2_TERMIDS.getTableName()+ " WHERE "+
                    DatabaseTables.TABLE_DOCID_2_TERMIDS.getPrimaryKeyField()+"='" + docid + "'");
        }

        String termidstr = "";
        for (int i : termids)
            termidstr = termidstr + VALUE_SEPARATOR + i;
        if (termidstr.startsWith(VALUE_SEPARATOR))
            termidstr = termidstr.substring(1);

        stmt.executeUpdate("INSERT INTO " + DatabaseTables.TABLE_DOCID_2_TERMIDS.getTableName() + " VALUES(" +
                "'" + docid + "'," +
                "'" + termidstr + "')");
        conn.commit();
    }


    /* Query values */
    public static int getCanonicalTermId4VarId(Connection conn, int varid) throws SQLException{
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT "+DatabaseTables.TABLE_TERMVARID_2_TERMID.getValueField()+" FROM " +
                    DatabaseTables.TABLE_TERMVARID_2_TERMID.getTableName() + " WHERE "+
                    DatabaseTables.TABLE_TERMVARID_2_TERMID.getPrimaryKeyField()+"='" + varid + "'");
            if (rs.next()) {
                return rs.getInt(1);
            }
            return -1;
        }


    public static int getTermId4Term(String termString, Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM " + DatabaseTables.TABLE_TERM_2_ID.getTableName() + " WHERE "+
                DatabaseTables.TABLE_TERM_2_ID.getPrimaryKeyField()+"='" + termString.replaceAll("'", "\\\\'") + "'");
        if (rs.next()) {
            return rs.getInt(2);
        }
        return -1;
    }

    public static String getTermString4TermId(int termId, Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM " + DatabaseTables.TABLE_TERM_2_ID.getTableName()+ " WHERE "
                +DatabaseTables.TABLE_TERM_2_ID.getValueField()+"='" + termId + "'");
        if (rs.next()) {
            return rs.getString(1);
        }
        return null;
    }


    public static int getDocId4Doc(Document doc, Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT "+DatabaseTables.TABLE_DOC_2_ID.getValueField()+
                " FROM " + DatabaseTables.TABLE_DOC_2_ID.getTableName() + " WHERE "+
                DatabaseTables.TABLE_DOC_2_ID.getPrimaryKeyField()+"='" +
                doc.getUrl().toString().replaceAll("'","\\\\'")+ "'");
        if (rs.next()) {
            return rs.getInt(1);
        }
        return -1;
    }

    public static Document getDoc4DocId(int docId, Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM " + DatabaseTables.TABLE_DOC_2_ID.getTableName() + " WHERE "+
                DatabaseTables.TABLE_DOC_2_ID.getValueField()+"='" + docId + "'");
        if (rs.next()) {
            String url = rs.getString(1);
            try {
                return new DocumentImpl(new URL(url));
            } catch (MalformedURLException e) {
                return null;
            }
        }
        return null;
    }

    public static int getTermVarId4TermVar(String termVarString, Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM " + DatabaseTables.TABLE_TERMVARIANT_2_ID.getTableName() + " WHERE "+
                DatabaseTables.TABLE_TERMVARIANT_2_ID.getPrimaryKeyField()+"='" + termVarString.replaceAll("'", "\\\\'") + "'");
        if (rs.next()) {
            return rs.getInt(2);
        }
        return -1;
    }

    public static String getTermVarString4TermVarId(int termVarId, Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM " + DatabaseTables.TABLE_TERMVARIANT_2_ID.getTableName() +" WHERE "+
                DatabaseTables.TABLE_TERMVARIANT_2_ID.getValueField()+"='" + termVarId + "'");
        if (rs.next()) {
            return rs.getString(1);
        }
        return null;
    }

    public static Set<Integer> getTermIdsInDocId(int docId, Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM " + DatabaseTables.TABLE_DOCID_2_TERMIDS.getTableName() + " WHERE "+
                DatabaseTables.TABLE_DOCID_2_TERMIDS.getPrimaryKeyField()+"='" + docId + "'");
        if (rs.next()) {
            String row = rs.getString(2);
            Set<Integer> result = new HashSet<Integer>();
            for (String v : row.split(VALUE_SEPARATOR)) {
                result.add(Integer.valueOf(v));
            }
            return result;
        }
        return new HashSet<Integer>();
    }

    public static Set<Integer> getDocIdsContainTermId(int termId, Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM " + DatabaseTables.TABLE_TERMID_2_DOCIDS.getTableName() + " WHERE "+
                DatabaseTables.TABLE_TERMID_2_DOCIDS.getPrimaryKeyField()+"='" + termId + "'");
        if (rs.next()) {
            String row = rs.getString(2);
            Set<Integer> result = new HashSet<Integer>();
            for (String v : row.split(VALUE_SEPARATOR)) {
                result.add(Integer.valueOf(v));
            }
            return result;
        }
        return null;
    }

    public static Set<Integer> getTermVarIds4TermId(int termId, Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM " + DatabaseTables.TABLE_TERMID_2_TERMVARIDS.getTableName() + " WHERE "+
                DatabaseTables.TABLE_TERMID_2_TERMVARIDS.getPrimaryKeyField()+"='" + termId + "'");
        if (rs.next()) {
            String row = rs.getString(2);
            Set<Integer> result = new HashSet<Integer>();
            for (String v : row.split(VALUE_SEPARATOR)) {
                result.add(Integer.valueOf(v));
            }
            return result;
        }

        return null;
    }

    public static Set<Integer> getAllTermIds(Connection conn) {
        try {
            Statement stmt = conn.createStatement();
            Set<Integer> result = new HashSet<Integer>();
            int start = 0, limit = 10000;
            boolean hasresult=false;
            do {
                hasresult=false;
                ResultSet rs = stmt.executeQuery("SELECT "+DatabaseTables.TABLE_TERM_2_ID.getValueField()+" FROM " +
                        DatabaseTables.TABLE_TERM_2_ID.getTableName()+" LIMIT "+limit+" OFFSET "+start);
                while (rs.next()) {
                    result.add(rs.getInt(1));
                    hasresult=true;
                }
                start = start + limit;
            } while(hasresult);

            return result;
        } catch (SQLException se) {
            se.printStackTrace();
            return null;
        }
    }

    public static Set<String> getAllTermStrings(Connection conn) {
        try {
            Statement stmt = conn.createStatement();
            Set<String> result = new HashSet<String>();
            int start = 0, limit = 10000;
            boolean hasresult=false;
            do {
                hasresult=false;
                ResultSet rs = stmt.executeQuery("SELECT "+DatabaseTables.TABLE_TERM_2_ID.getPrimaryKeyField()+" FROM " +
                        DatabaseTables.TABLE_TERM_2_ID.getTableName()+" LIMIT "+limit+" OFFSET "+start);
                while (rs.next()) {
                    result.add(rs.getString(1));
                    hasresult=true;
                }
                start = start + limit;
            } while(hasresult);

            return result;
        } catch (SQLException se) {
            se.printStackTrace();
            return null;
        }
    }


    public static Set<Integer> getAllTermVariantIds(Connection conn) {
        try {
            Statement stmt = conn.createStatement();
            Set<Integer> result = new HashSet<Integer>();
            int start = 0, limit = 10000;
            boolean hasresult=false;
            do {
                hasresult=false;
                ResultSet rs = stmt.executeQuery("SELECT "+DatabaseTables.TABLE_TERMVARIANT_2_ID.getValueField()+" FROM " +
                        DatabaseTables.TABLE_TERMVARIANT_2_ID.getTableName()+" LIMIT "+limit+" OFFSET "+start);
                while (rs.next()) {
                    result.add(rs.getInt(1));
                    hasresult=true;
                }
                start = start + limit;
            } while(hasresult);

            return result;
        } catch (SQLException se) {
            se.printStackTrace();
            return null;
        }
    }

    public static Set<String> getAllTermVariantStrings(Connection conn) {
        try {
            Statement stmt = conn.createStatement();
            Set<String> result = new HashSet<String>();
            int start = 0, limit = 10000;
            boolean hasresult=false;
            do {
                hasresult=false;
                ResultSet rs = stmt.executeQuery("SELECT "+DatabaseTables.TABLE_TERMVARIANT_2_ID.getPrimaryKeyField()+" FROM " +
                        DatabaseTables.TABLE_TERMVARIANT_2_ID.getTableName()+" LIMIT "+limit+" OFFSET "+start);
                while (rs.next()) {
                    result.add(rs.getString(1));
                    hasresult=true;
                }
                start = start + limit;
            } while(hasresult);

            return result;
        } catch (SQLException se) {
            se.printStackTrace();
            return null;
        }
    }


    public static Set<Integer> getAllDocIds(Connection conn) {
        try {
            Statement stmt = conn.createStatement();
            Set<Integer> result = new HashSet<Integer>();
            int start = 0, limit = 10000;
            boolean hasresult=false;
            do {
                hasresult=false;
                ResultSet rs = stmt.executeQuery("SELECT "+DatabaseTables.TABLE_DOC_2_ID.getValueField()+" FROM " +
                        DatabaseTables.TABLE_DOC_2_ID.getTableName()+" LIMIT "+limit+" OFFSET "+start);
                while (rs.next()) {
                    result.add(rs.getInt(1));
                    hasresult=true;
                }
                start = start + limit;
            } while(hasresult);

            return result;
        } catch (SQLException se) {
            se.printStackTrace();
            return null;
        }
    }

    public static Set<Document> getAllDocs(Connection conn) {
        try {
            Statement stmt = conn.createStatement();
            Set<Document> result = new HashSet<Document>();
            int start = 0, limit = 10000;
            boolean hasresult=false;
            do {
                hasresult=false;
                ResultSet rs = stmt.executeQuery("SELECT "+DatabaseTables.TABLE_DOC_2_ID.getPrimaryKeyField()+" FROM " +
                        DatabaseTables.TABLE_DOC_2_ID.getTableName()+" LIMIT "+limit+" OFFSET "+start);
                while (rs.next()) {
                    String url = rs.getString(1);
                    Document doc = new DocumentImpl(new URL(url));
                    result.add(doc);
                    hasresult=true;
                }
                start = start + limit;
            } while(hasresult);

            return result;
        } catch (SQLException se) {
            se.printStackTrace();
            return null;
        } catch(MalformedURLException me){
            return null;
        }
    }
}
