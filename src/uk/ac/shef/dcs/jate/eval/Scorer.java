package uk.ac.shef.dcs.jate.eval;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * compute scores
 */
public class Scorer {

    private static String digitsPattern = "\\d+";
    private static String symbolsPattern="\\p{Punct}";

    public static void createReportGenia(String ateOutputFolder, String gsFile, String outFile,
                                         boolean ignoreSymbols, boolean ignoreDigits,
                                    int minChar, int maxChar, int minTokens, int maxTokens,
                                    int... ranks) throws IOException {
        PrintWriter p = new PrintWriter(outFile);
        List<String> gs = GSLoader.loadGenia(gsFile, true, true);
        Map<String, double[]> scores = new HashMap<>();

        for(File f: new File(ateOutputFolder).listFiles()){
            String name =f.getName();
            List<String> terms = ATEResultLoader.load(f.toString());

            double[] s=computePrecisionAtRank(gs,terms, ignoreSymbols,ignoreDigits,minChar,
                    maxChar, minTokens, maxTokens, ranks);
            scores.put(name, s);
        }

        //generate report

        StringBuilder sb=  new StringBuilder();
        for(int i: ranks){
            sb.append(",").append(i);
        }
        sb.append("\n");

        for(Map.Entry<String, double[]> en: scores.entrySet()) {
            sb.append(en.getKey()).append(",");
            double[] s = en.getValue();
            for (int i = 0; i < ranks.length; i++) {
                sb.append(s[i]).append(",");

            }
            sb.append("\n");
        }
        p.println(sb.toString());
        p.close();
    }

    public static double[] computePrecisionAtRank(List<String> gs, List<String> terms,
                                              boolean ignoreSymbols, boolean ignoreDigits,
                                              int minChar, int maxChar, int minTokens, int maxTokens,
                                              int... ranks) throws FileNotFoundException {
        gs = prune(gs, ignoreSymbols, ignoreDigits, minChar, maxChar, minTokens, maxTokens);
        terms=prune(terms, ignoreSymbols, ignoreDigits, minChar,maxChar,minTokens, maxTokens);


        double[] scores = new double[ranks.length];
        for(int i=0; i<ranks.length; i++){
            double p = computePrecisionAtRank(gs, terms, ranks[i]);
            scores[i]=p;

        }

        return scores;
    }

    private static double computePrecisionAtRank(List<String> gs, List<String> terms, int rank){
        int correct=0;
        for(int i=0; i< rank; i++){
            String t = terms.get(i);
            if(gs.contains(t))
                correct++;
        }
        return (double)correct/rank;
    }

    private static List<String> prune(List<String> gs, boolean ignoreSymbols, boolean ignoreDigits, int minChar, int maxChar, int minTokens, int maxTokens) {
        List<String> result = new ArrayList<>();
        for(String g: gs){
            if (ignoreDigits)
                g=g.replaceAll(digitsPattern," ").replaceAll("\\s+"," ").trim();
            if(ignoreSymbols)
                g=g.replaceAll(symbolsPattern," ").replaceAll("\\s+"," ").trim();

            //g=g.trim();
            if(g.length()>=minChar&&g.length()<=maxChar){
                int tokens=g.split("\\s+").length;
                if(tokens>=minTokens&&tokens<=maxTokens)
                    result.add(g);
            }

        }
        return result;
    }

    public static void main(String[] args) throws IOException {

        List<String> gs = GSLoader.loadGenia(args[1],true, true);

        List<String> terms= ATEResultLoader.load(args[0]);
        double[] precisions =computePrecisionAtRank(gs, terms, true, false, 2,150,2,7,
                10, 50, 100, 500, 1000,5000,10000);
        System.out.println();
    }
}
