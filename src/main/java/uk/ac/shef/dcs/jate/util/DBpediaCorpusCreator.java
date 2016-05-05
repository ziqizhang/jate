package uk.ac.shef.dcs.jate.util;
//
//import org.apache.jena.query.*;
//import org.apache.jena.rdf.model.RDFNode;
//
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.PrintWriter;
//import java.util.ArrayList;
//import java.util.List;

/**
 * Created by - on 04/05/2016.
 */
public class DBpediaCorpusCreator {
//    private static final String DBPEDIA_NAMESPACE = "http://dbpedia.org/ontology/";
//    private static final String DBPEDIA_ABSTRACT = "http://dbpedia.org/ontology/abstract";
//    private static final String DBPEDIA_SPARQL_ENDPOINT = "http://dbpedia.org/sparql";
//    private static final int MIN_WORDS_IN_ABSTRACT = 200;
//
//    public static void main(String[] args) throws FileNotFoundException {
//        DBpediaCorpusCreator creator = new DBpediaCorpusCreator();
//        creator.createCorpus("Amphibian",
//                "/Users/-/work/jate/src/test/resource/eval/DBpedia",
//                50000);
//    }
//
//    public void createCorpus(String conceptName,
//                             String outFolder,
//                             int limit) throws FileNotFoundException {
//        List<String> instances = queryForInstances(conceptName, limit);
//
//        System.out.println("Total to process="+instances.size());
//        int count=0;
//        for (String i : instances) {
//            count++;
//            String abs=queryForAbstract(i, MIN_WORDS_IN_ABSTRACT);
//            if(abs.length()>0) {
//                String name = i.substring(i.lastIndexOf("/") + 1);
//                name = name.replaceAll("[^a-zA-Z0-9]", "_") + ".txt";
//                PrintWriter p = new PrintWriter(outFolder + File.separator + name);
//                p.println(abs);
//                p.close();
//            }
//            System.out.println(count);
//        }
//    }
//
//    protected List<String> queryForInstances(String conceptName, int limit) {
//        String qStr = "select distinct ?s where {?s a " +
//                "<" + DBPEDIA_NAMESPACE + conceptName + ">} LIMIT " + limit;
//        Query query = QueryFactory.create(qStr);
//        QueryExecution qexec = QueryExecutionFactory.sparqlService(DBPEDIA_SPARQL_ENDPOINT,
//                query);
//
//        ResultSet results = qexec.execSelect();
//
//        List<String> result = new ArrayList<>();
//        while (results.hasNext()) {
//            QuerySolution qs = results.next();
//            RDFNode n = qs.get("?s");
//            if (n != null)
//                result.add(n.toString());
//        }
//        return result;
//    }
//
//    protected String queryForAbstract(String instanceURI, int minWords) {
//        String qStr = "select ?o where {<" + instanceURI + "> <" + DBPEDIA_ABSTRACT + "> " +
//                "?o}";
//        Query query = QueryFactory.create(qStr);
//        QueryExecution qexec = QueryExecutionFactory.sparqlService(DBPEDIA_SPARQL_ENDPOINT,
//                query);
//
//        ResultSet results = qexec.execSelect();
//
//        if (results.hasNext()) {
//            QuerySolution qs = results.next();
//            RDFNode n = qs.get("?o");
//            String value = n.toString();
//            if (value.startsWith("\"") || value.startsWith("\'"))
//                value = value.substring(1).trim();
//            if (value.endsWith("\"") || value.endsWith("\'"))
//                value = value.substring(0, value.length() - 1).trim();
//
//            if (value.endsWith("@en")) {
//                int end = value.lastIndexOf("@en");
//                value = value.substring(0, end);
//                int length = value.split("\\s+").length;
//                if (length >= minWords)
//                    return value;
//            }
//        }
//        return "";
//    }
}
