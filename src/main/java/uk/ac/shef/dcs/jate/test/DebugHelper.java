package uk.ac.shef.dcs.jate.test;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.en.EnglishMinimalStemmer;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.eval.Scorer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by - on 04/02/2016.
 */
public class DebugHelper {

    public static void loop(){
        for (int i= 0; i< 1616135; i++){

        }
    }

    public static void stem(String inFile, String outFile) throws IOException {
        List<String> lines=FileUtils.readLines(new File(inFile));

        Map<String, Integer> freq = new HashMap<>();
        for(String l: lines){
            l=l.trim();
            String[] toks = l.split("\\s+");
            try {
                freq.put(toks[1], Integer.valueOf(toks[0]));
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        EnglishMinimalStemmer stemmer = new EnglishMinimalStemmer();

        PrintWriter p  =new PrintWriter(outFile);
        for(String k: freq.keySet()){
            int trim=stemmer.stem(k.toCharArray(), k.length());
            String newTerm = k.substring(0, trim);

            Integer f = freq.get(newTerm);
            if(f!=null)
                p.println(f + "\t" + k);
            else
                p.println(freq.get(k)+"\t"+newTerm);

        }
        p.close();
    }

    public static String depluralize(String name){


               // first check for already in plural form
                     if (name.endsWith("List") || (name.endsWith("s") && !name.endsWith("ss"))) {
                             return name;
                         }

                     // convert singular form to plural
                     if (name.endsWith("y") && !name.endsWith("ay") && !name.endsWith("ey") && !name.endsWith("iy") &&
                                 !name.endsWith("oy") && !name.endsWith("uy")) {
                             if (name.equalsIgnoreCase("any")) {
                                     return name;
                                 } else {
                                     return name.substring(0, name.length() - 1) + "ies";
                                 }
                         } else if (name.endsWith("ss")) {
                             return name + "es";
                         } else {
                             return name + 's';
                         }

    }

    public static void writeList(List<String> content, String filename){
        List<String> sorted = new ArrayList<>(content);
        //Collections.sort(sorted);
        PrintWriter p = null;
        try {
            p = new PrintWriter(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        for(String c: sorted)
            p.println(c);
        p.close();
    }

    public static void compareList(String inFile1, String inFile2) throws IOException {
        List<String> list1 = FileUtils.readLines(new File(inFile1));
        List<String> list2 = FileUtils.readLines(new File(inFile2));

        int c=0;
        for(String t: list1){
            c++;
            if(!list2.contains(t))
                System.out.println(c+">>\t"+t);

            if(c>50)
                break;
        }

    }


    public static void compareNewAndOldPredictions(String gsFile,
                                                   String newPredictionFile,
                                                   String oldPredictionFile) throws JATEException, IOException {
        List<String> gs = FileUtils.readLines(new File(gsFile));
        List<String> newP = FileUtils.readLines(new File(newPredictionFile));
        List<String> oldP = FileUtils.readLines(new File(oldPredictionFile));

        int defaultMinChar = 2; //originall 1. should use 2 at least
        int defaultMaxChar = 1000;
        int defaultMinTokens=1;
        int defaultMaxTokens=10;

        gs = Scorer.prune(gs, true, false, true,
                defaultMinChar, defaultMaxChar, defaultMinTokens, defaultMaxTokens);
        newP = Scorer.prune(newP, true, false, true,
                defaultMinChar, defaultMaxChar, defaultMinTokens, defaultMaxTokens);
        oldP = Scorer.prune(oldP, true, false, true,
                defaultMinChar, defaultMaxChar, defaultMinTokens, defaultMaxTokens);

       for(int i=0; i<50; i++){
           String old = oldP.get(i);
           if(gs.contains(old)){
               int index_p = newP.indexOf(old);
               System.out.println(i+"\t\t"+old+"\t\t"+index_p);
           }
       }

    }
    public static void main(String[] args) throws IOException, JATEException {
        stem("/Users/-/work/jate/src/test/resource/bnc_unifrqs.normal",
                "/Users/-/work/jate/debug/bnc_unifrqs.normal.stem");
        System.exit(1);

        System.out.println(depluralize("cats"));
        System.exit(1);

        //compareList("/Users/-/work/jate/candidates_old.txt","candidates_new.txt");
        compareNewAndOldPredictions(
                "/Users/-/work/jate/src/test/resource/eval/GENIAcorpus-concept.txt",
                "/Users/-/work/jate/candidates_new.txt",
                "/Users/-/work/jate/candidates_old.txt");
    }
}
