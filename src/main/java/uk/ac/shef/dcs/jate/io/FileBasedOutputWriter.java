package uk.ac.shef.dcs.jate.io;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexSearcher;
import org.json.simple.parser.ParseException;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.app.App;
import uk.ac.shef.dcs.jate.eval.ATEResultLoader;
import uk.ac.shef.dcs.jate.model.JATETerm;
import uk.ac.shef.dcs.jate.util.ATR4SFormatConvertor;
import uk.ac.shef.dcs.jate.util.SolrUtil;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

/**
 * This class outputs the term candidates and their scores on a per-file basis.
 *
 *
 */
public class FileBasedOutputWriter {

    public static void main(String[] args) throws JATEException, IOException, ParseException {
        String outFolder=args[0];
        String solrHomePath = args[1];
        String solrCoreName = args[2];
        final EmbeddedSolrServer solrServer = new EmbeddedSolrServer(Paths.get(solrHomePath), solrCoreName);
        JATEProperties jateProp = App.getJateProperties(args[3]);
        List<String> predictions=new ArrayList<>();
        if(args.length>4){
            //predictions=ATEResultLoader.load(args[4]);
            predictions.addAll(ATR4SFormatConvertor.readAllTermStrings(new File(args[4])));
        }

        output(outFolder, solrServer.getCoreContainer().getCore(solrCoreName),
                jateProp, predictions);
    }
    /**
     *
     * @param outFolder
     * @param predictions if provided, will be used as a filter to only select these that are found within each file
     */
    public static void output(String outFolder, SolrCore core, JATEProperties properties, List<String> predictions){
        SolrIndexSearcher searcher = core.getSearcher().get();
        Set<String> selected = new HashSet<>();
        List<Integer> docIds = new ArrayList<>();
        for (int i = 0; i < searcher.maxDoc(); i++) {
            docIds.add(i);
        }

        Set<String> predictionStrings =new HashSet<>();
        if(predictions!=null){
            predictionStrings.addAll(predictions);
        }

        int count=0;

        for (int docId : docIds) {
            count++;

            try {
                Terms lookupVector = SolrUtil.getTermVector(docId, properties.getSolrFieldNameJATENGramInfo(), searcher);
                if(lookupVector==null){
                    //LOG.error("Term vector for document id="+count+" is null. The document may be empty");
                    System.err.println("Term vector for document id="+count+" is null. The document may be empty");
                    continue;
                }

                String filename=outFolder+ File.separator+new File(searcher.doc(docId).get("id")).getName();
                System.out.println(count+","+filename);
                Set<String> terms = collectTerms(
                        lookupVector);
                List<String> sorted = new ArrayList<>();

                if(predictions==null|| predictionStrings.size()==0)
                    sorted.addAll(terms);
                else{
                    for(String t: terms){
                        if(predictionStrings.contains(t)) {
                            sorted.add(t);
                            selected.add(t);
                        }
                       /* else
                            System.err.println("This candidate term in this document does not exist: {"+t+" @ "+filename);
                    */}
                }
                Collections.sort(sorted);
                PrintWriter p = new PrintWriter(filename);
                for(String s: sorted)
                    p.println(s);
                p.close();
            } catch (IOException ioe) {
                StringBuilder sb = new StringBuilder("Unable to build feature for document id:");
                sb.append(docId).append("\n");
                sb.append(ExceptionUtils.getFullStackTrace(ioe));
                //LOG.error(sb.toString());
            } catch (JATEException je) {
                StringBuilder sb = new StringBuilder("Unable to build feature for document id:");
                sb.append(docId).append("\n");
                sb.append(ExceptionUtils.getFullStackTrace(je));
                //LOG.error(sb.toString());
            }
        }
        core.close();
        System.out.println(selected.size());
        System.exit(0);
    }

    private static Set<String> collectTerms(Terms termVectorLookup) throws IOException {
        Set<String> result = new HashSet<>();

        TermsEnum tiRef= termVectorLookup.iterator();
        BytesRef luceneTerm = tiRef.next();
        while (luceneTerm != null) {
            if (luceneTerm.length == 0) {
                luceneTerm = tiRef.next();
                continue;
            }
            String tString = luceneTerm.utf8ToString();

            result.add(tString);
            luceneTerm = tiRef.next();
        }

        return result;
    }
}
