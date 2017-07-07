package uk.ac.shef.dcs.jate.util;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.SolrCore;
import org.json.simple.parser.ParseException;
import uk.ac.shef.dcs.jate.eval.ATEResultLoader;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class Statistics {
    public static void main(String[] args) throws IOException, ParseException {
        String outFolder="/home/zqz/GDrive/papers/termrank/results/final-results/kcr_terms_compare";
        List<String> termskcr = ATEResultLoader.load("/home/zqz/GDrive/papers/termrank/results/final-results/kcr_terms_compare/KeyConceptRelatedness.txt");
        PrintWriter p = new PrintWriter(outFolder+"/kcr.txt");
        for(String t: termskcr)
            p.println(t);
        p.close();
        List<String> termskcrtr = ATEResultLoader.load("/home/zqz/GDrive/papers/termrank/results/final-results/kcr_terms_compare/KeyConceptRelatedness-em_g-uni-sg-100-w3-m1,g_dn-pn-top1-pnl0-t0.5.json");
        p = new PrintWriter(outFolder+"/kcr_tr.txt");
        for(String t: termskcrtr)
            p.println(t);
        p.close();
        System.exit(0);


        countCorpusWords(args[0]);
    }
    public static void countCorpusWords(String inFileFolder) throws IOException {

        List<Integer> stats = new ArrayList<>();
        int total = 0;
        Collection<File> files=FileUtils.listFiles(new File(inFileFolder), new String[]{"txt"},true);
        for (File f : files) {
            if (f.isDirectory())
                continue;
            String content = FileUtils.readFileToString(f);
            String[] words = content.split("\\s+");
            stats.add(words.length);
            total += words.length;
        }

        Collections.sort(stats);
        System.out.println("docs=" + stats.size() + ", total=" + total + ", min=" + stats.get(0) +
                ", max=" + stats.get(stats.size() - 1) + ", avg=" + (total / stats.size()));
    }

}
