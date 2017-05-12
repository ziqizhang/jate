package uk.ac.shef.dcs.jate.util;

import dragon.nlp.tool.lemmatiser.EngLemmatiser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.index.PostingsEnum;
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
import uk.ac.shef.dcs.jate.eval.Scorer;
import uk.ac.shef.dcs.jate.io.FileBasedOutputWriter;
import uk.ac.shef.dcs.jate.nlp.Lemmatiser;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.*;

/**
 *
 */
public class ScienceIECorpusParser {
    public static void mergeGS(String inFolder, String outFile) throws IOException {
        Set<String> all = new HashSet<>();
        for (File f : new File(inFolder).listFiles()) {
            if (f.getName().endsWith(".ann")) {
                List<String> lines = FileUtils.readLines(f);
                for (String l : lines) {
                    String[] parts = l.split("\t");
                    if (parts.length < 3)
                        continue;
                    all.add(parts[2].trim());
                }
            }
        }
        List<String> sort = new ArrayList<>(all);
        Collections.sort(sort);
        PrintWriter p = new PrintWriter(outFile);
        for (String l : sort)
            p.println(l);
        p.close();
    }

    /**
     * takes ATE output for the whole corpus, and the KEA gold standard, generate the corresponding
     * KEA output given the ATE output
     */
    public static void transformToKEAOutput(String ateOutFile, int topN, String KEAGSFolder,
                                            String keaPredictionOutFolder, Lemmatiser lem,
                                            SolrCore core,
                                            JATEProperties properties) throws IOException, ParseException {
        List<String> ateCandidates = ATEResultLoader.loadFromJSON(ateOutFile);
        if (topN > ateCandidates.size() || topN < 1) {
            //automatically determine topN
            Set<String> uniqueKeywords = new HashSet<>();
            for (File f : new File(KEAGSFolder).listFiles()) {
                if (f.getName().endsWith(".ann")) {
                    List<String> lines = FileUtils.readLines(f);
                    for (String l : lines) {
                        String[] parts = l.split("\t");
                        if (parts.length < 3)
                            continue;
                        String norm = Scorer.normaliseTerm(parts[2].trim(), lem);
                        if (norm.length() > 1)
                            uniqueKeywords.add(norm);
                    }
                }
            }
            if (uniqueKeywords.size() > ateCandidates.size())
                topN = ateCandidates.size();
            else
                topN = uniqueKeywords.size();
            System.out.println("topn="+topN);
        }

        List<String> selectedTerms = new ArrayList<>();
        for (int i = 0; i < topN; i++)
            selectedTerms.add(ateCandidates.get(i));

        SolrIndexSearcher searcher = core.getSearcher().get();
        Map<String, Integer> fileNameToIndexId = new HashMap<>();
        for (int i = 0; i < searcher.maxDoc(); i++) {
            String fileName = searcher.doc(i).get("id");
            int lastSlash = fileName.lastIndexOf("/");
            int lastDot = fileName.lastIndexOf(".");
            fileName = fileName.substring(lastSlash + 1, lastDot);
            fileNameToIndexId.put(fileName + ".ann", i);
        }

        File folder = new File(keaPredictionOutFolder);
        folder.mkdirs();
        for (File f : new File(KEAGSFolder).listFiles()) {
            if (f.getName().endsWith(".ann")) {
                PrintWriter p = new PrintWriter(keaPredictionOutFolder + File.separator
                        + f.getName());
                //check what are in the prediction, and also in GS
                Set<String> selectedCorrect = new HashSet<>();
                List<String> lines = FileUtils.readLines(f);
                for (String l : lines) {
                    String[] parts = l.split("\t");
                    if (parts.length < 3)
                        continue;
                    String norm = Scorer.normaliseTerm(parts[2].trim(), lem);
                    if (norm.length() > 1 && selectedTerms.contains(norm)) {
                        selectedCorrect.add(norm);
                        p.println(l);
                    }
                }
                //check what are in the prediction, but not in GS
                //read index
                int docId = fileNameToIndexId.get(f.getName());
                System.out.println("\t" + docId + "=" + f.getName());
                try {
                    Terms lookupVector = SolrUtil.getTermVector(docId, properties.getSolrFieldNameJATENGramInfo(), searcher);
                    if (lookupVector != null) {
                        TermsEnum tiRef = lookupVector.iterator();
                        BytesRef luceneTerm = tiRef.next();
                        while (luceneTerm != null) {
                            if (luceneTerm.length == 0) {
                                luceneTerm = tiRef.next();
                                continue;
                            }
                            String tString = luceneTerm.utf8ToString();
                            if (selectedCorrect.contains(tString)) {
                                luceneTerm = tiRef.next();
                                continue;
                            } else if (selectedTerms.contains(tString)) {
                                PostingsEnum postingsInDoc = tiRef.postings(null, PostingsEnum.OFFSETS);
                                int doc = postingsInDoc.nextDoc();
                                int freq = postingsInDoc.freq();
                                for (int i = 0; i < freq; i++) {
                                    int occurance = postingsInDoc.nextPosition();
                                    StringBuilder line = new StringBuilder();
                                    line.append("T0\t").append("Process ").append(postingsInDoc.startOffset()).
                                            append(" ").append(postingsInDoc.endOffset()).append("\t")
                                            .append(tString);
                                    p.println(line.toString());
                                }
                            }
                            luceneTerm = tiRef.next();
                        }
                    }
                } catch (JATEException je) {
                    je.printStackTrace();
                }
                p.close();
            }

        }
        //find document
        //get term candidates
        //for each term candidate, if in selectedTerm but not in correctGS, print them

    }

    public static void main(String[] args) throws IOException, ParseException, JATEException {
        /*mergeGS("/home/zqz/Work/data/semeval2017-scienceie/scienceie2017_test/scienceie2017_test_gs",
                "/home/zqz/Work/data/semeval2017-scienceie/scienceie2017_test/all_key_phrases.txt");*/
        Lemmatiser lem = new Lemmatiser(new EngLemmatiser(args[0],
                false, false));
        String solrHomePath = args[4];
        String solrCoreName = args[5];
        final EmbeddedSolrServer solrServer = new EmbeddedSolrServer(Paths.get(solrHomePath), solrCoreName);
        JATEProperties jateProp = App.getJateProperties(args[6]);
        for (File f : new File(args[1]).listFiles()) {
            File outFolder = new File(args[2] + "/" + f.getName() + "/");
            outFolder.mkdirs();
            System.out.println(outFolder);

            transformToKEAOutput(f.toString(), 0, args[3], outFolder.toString(), lem,
                    solrServer.getCoreContainer().getCore(solrCoreName), jateProp);
        }
        solrServer.close();
        System.exit(0);
    }
}
