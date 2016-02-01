package uk.ac.shef.dcs.jate.eval;

import opennlp.tools.util.eval.FMeasure;
import uk.ac.shef.dcs.jate.JATEException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * compute scores
 */
public class Scorer {

    private static String digitsPattern = "\\d+";
    private static String symbolsPattern = "\\p{Punct}";

    public static void calculateACLRDJate1(String ateOutputFile, String gsFile, String outFile,
                                           boolean ignoreSymbols, boolean ignoreDigits,boolean lowercase,
                                           int minChar, int maxChar, int minTokens, int maxTokens,
                                           int... ranks) throws IOException {
        PrintWriter p = new PrintWriter(outFile);
        List<String> gs = GSLoader.loadACLRD(gsFile, true, true);

        List<List<String>> terms = ATEResultLoader.loadJATE1(ateOutputFile);

        gs = prune(gs, ignoreSymbols, ignoreDigits, lowercase, minChar, maxChar, minTokens, maxTokens);

        for (int i = 0; i < ranks.length; i++) {
            int correct = 0;
            int rank=ranks[i];
            for (int j = 0; j < rank && j < terms.size() && j < gs.size(); j++) {
                List<String> variants = terms.get(j);
                variants.retainAll(gs);
                if (variants.size() > 0)
                    correct++;
            }
            System.out.println("rank"+rank+"="+ (double) correct / rank);

        }

    }

    public static void createReportACLRD(String ateOutputFolder, String gsFile, String outFile,
                                         boolean ignoreSymbols, boolean ignoreDigits,boolean lowercase,
                                         int minChar, int maxChar, int minTokens, int maxTokens,
                                         int... ranks) throws IOException {
        PrintWriter p = new PrintWriter(outFile);
        List<String> gs = GSLoader.loadACLRD(gsFile, true, true);
        Map<String, double[]> scores = new HashMap<>();

        for (File f : new File(ateOutputFolder).listFiles()) {
            String name = f.getName();
            if (name.charAt(0) == '.')
                continue;
            System.out.println(f);
            List<String> terms = ATEResultLoader.load(f.toString());

            double[] s = computePrecisionAtRank(gs, terms, ignoreSymbols, ignoreDigits, lowercase, minChar,
                    maxChar, minTokens, maxTokens, ranks);
            scores.put(name, s);
        }

        //generate report

        StringBuilder sb = new StringBuilder();
        for (int i : ranks) {
            sb.append(",").append(i);
        }
        sb.append("\n");

        for (Map.Entry<String, double[]> en : scores.entrySet()) {
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

    public static void createReportGenia(String ateOutputFolder, String gsFile, String outFile,
                                         boolean ignoreSymbols, boolean ignoreDigits, boolean lowercase,
                                         int minChar, int maxChar, int minTokens, int maxTokens,
                                         int... ranks) throws IOException {
        PrintWriter p = new PrintWriter(outFile);
        List<String> gs = GSLoader.loadGenia(gsFile, true, true);
        Map<String, double[]> scores = new HashMap<>();

        for (File f : new File(ateOutputFolder).listFiles()) {
            String name = f.getName();
            if (name.charAt(0) == '.')
                continue;
            System.out.println(f);
            List<String> terms = ATEResultLoader.load(f.toString());

            double[] s = computePrecisionAtRank(gs, terms, ignoreSymbols, ignoreDigits, lowercase, minChar,
                    maxChar, minTokens, maxTokens, ranks);
            scores.put(name, s);
        }

        //generate report

        StringBuilder sb = new StringBuilder();
        for (int i : ranks) {
            sb.append(",").append(i);
        }
        sb.append("\n");

        for (Map.Entry<String, double[]> en : scores.entrySet()) {
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

    public String generateGENIABenchmarkingReport(List<String> ateTermResults, String geniaCorpusConceptsFile,
                                                  boolean ignoreSymbols, boolean ignoreDigits, boolean lowercase,
                                                  int minChar, int maxChar, int minTokens, int maxTokens,
                                                  int... ranks
                                                  ) throws JATEException {
        //genia goldstandards concepts
        List<String> gsConcepts = null;
        try {
             gsConcepts= GSLoader.loadGenia(geniaCorpusConceptsFile, true, true);
        } catch (IOException e) {
            throw new JATEException("Scorer exception: failed to load GENIS GoldStandards. " +
                    "Check the correct path for GENIAcorpus concept file!");
        }

        Map<String, double[]> scores = new HashMap<>();
        double[] s = computePrecisionAtRank(gsConcepts, ateTermResults, ignoreSymbols, ignoreDigits, lowercase, minChar,
                    maxChar, minTokens, maxTokens, ranks);
        scores.put("doc", s);

        StringBuilder sb = new StringBuilder();
        for (int i : ranks) {
            sb.append(",").append(i);
        }
        sb.append("\n");

        for (Map.Entry<String, double[]> en : scores.entrySet()) {
            sb.append(en.getKey()).append(",");
            double[] dd = en.getValue();
            for (int i = 0; i < ranks.length; i++) {
                sb.append(dd[i]).append(",");

            }
            sb.append("\n");
        }

        return sb.toString();
    }


    /**
     * compute pre-configured top Ks precision with normalisation for both gold standard spans and ranked terms
     *
     * @deprecated
     * @param gs, gold standards
     * @param terms, extracted results
     * @param ignoreSymbols, remove symbols
     * @param ignoreDigits, remove digits
     * @param minChar,
     * @param maxChar
     * @param minTokens
     * @param maxTokens
     * @param ranks
     * @return
     */
    public static double[] computePrecisionAtRank(List<String> gs, List<String> terms,
                                                  boolean ignoreSymbols, boolean ignoreDigits, boolean lowercase,
                                                  int minChar, int maxChar, int minTokens, int maxTokens,
                                                  int... ranks) {
        gs = prune(gs, ignoreSymbols, ignoreDigits, lowercase, minChar, maxChar, minTokens, maxTokens);
        terms = prune(terms, ignoreSymbols, ignoreDigits, lowercase, minChar, maxChar, minTokens, maxTokens);

        double[] scores = new double[ranks.length];
        for (int i = 0; i < ranks.length; i++) {
            double p = computePrecisionAtRank(gs, terms, ranks[i]);
            scores[i] = p;

        }

        return scores;
    }

//    /**
//     * compute Top K precision with strict or lenient benchmarking
//     *
//     * @return
//     */

    /**
     * compute Top K precision with strict or lenient benchmarking
     *
     * @param gsTerms, gold standards terms/spans
     * @param terms, all ranked ATE terms for benchmarking
     * @param ignoreSymbols, remove symbols for two dataset
     * @param ignoreDigits, remove digits for two dataset
     * @param lowercase, lowercase terms for two dataset
     * @param topK, choose topK results in terms for benchmarking
     * @return double, precision
     */
    public static double computePrecisionWithNormalisation(List<String> gsTerms, List<String> terms,
                                                             boolean ignoreSymbols, boolean ignoreDigits,
                                                             boolean lowercase, int topK) throws JATEException {
        double score = 0.0;

        if (terms == null || terms.size() ==0) {
            return score;
        }

        if (gsTerms == null || gsTerms.size() == 0) {
            throw new JATEException("Gold Standard Terms is null!");
        }

        int defaultMinChar = 1;
        int defaultMaxChar = 1000;
        int defaultMinTokens=1;
        int defaultMaxTokens=10;

        gsTerms = prune(gsTerms, ignoreSymbols, ignoreDigits, lowercase,
                defaultMinChar, defaultMaxChar, defaultMinTokens, defaultMaxTokens);
        terms = prune(terms, ignoreSymbols, ignoreDigits, lowercase,
                defaultMinChar, defaultMaxChar, defaultMinTokens, defaultMaxTokens);

        List<String> topKTerms = terms.subList(0, topK);
        score = precision(gsTerms, topKTerms);

        return round(score, 2);
    }

    public static double round(double value, int scale) {
        return new BigDecimal(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    // public static double[] computeTopKPrecision()

    /**
     * calculate top K precision
     * @deprecated use precision() method instead
     *
     * @param gs, gold standards terms
     * @param terms, ranked term
     * @param rank, top K
     * @return precision
     */
    public static double computePrecisionAtRank(List<String> gs, List<String> terms, int rank) {
        int correct = 0;
        for (int i = 0; i < rank && i < terms.size() && i < gs.size(); i++) {
            String t = terms.get(i);
            if (gs.contains(t))
                correct++;
        }
        return (double) correct / rank;
    }

    /**
     * calculate
     * @param gsTerms the gold standard spans
     * @param topKTerms the predicted/extracted and ranked spans, top K results can be selected for scoring
     * @return double , precision score
     */
    public static double precision(List<String> gsTerms, List<String> topKTerms) {
        return FMeasure.precision(gsTerms.toArray(),topKTerms.toArray());
    }

    public static double recall(List<String> gsTerms, List<String> termResults) {
        double recall = FMeasure.recall(gsTerms.toArray(), termResults.toArray());

        return round(recall, 2);
    }

    public static double fmeasure(List<String> gsTerms, List<String> termResults) {
        FMeasure fMeasure = new FMeasure();
        fMeasure.updateScores(gsTerms.toArray(), termResults.toArray());
        return fMeasure.getFMeasure();
    }

    /**
     * prune term list for benchmarking
     *
     * this methods enable to calculate the agreement of two term set on a strict or lenient basis
     *
     *
     * @param terms, term list to be pruned
     * @param ignoreSymbols, remove symbols
     * @param ignoreDigits, remove digits
     * @param minChar, minimum characters allowed
     * @param maxChar, maximum characters allowed
     * @param minTokens, minimum tokens allowed
     * @param maxTokens, maximum tokens allowed
     * @return List, list of normalised term set
     */
    public static List<String> prune(List<String> terms, boolean ignoreSymbols, boolean ignoreDigits, boolean lowercase,
                                     int minChar, int maxChar, int minTokens, int maxTokens) {
        List<String> result = new ArrayList<>();
        for (String term : terms) {
            if (ignoreDigits)
                term = term.replaceAll(digitsPattern, " ").replaceAll("\\s+", " ");
            if (ignoreSymbols)
                term = term.replaceAll(symbolsPattern, " ").replaceAll("\\s+", " ");

            term = term.trim();
            if (term.length() >= minChar && term.length() <= maxChar) {
                int tokens = term.split("\\s+").length;
                if (tokens >= minTokens && tokens <= maxTokens)
                    if (lowercase) {
                        result.add(term.toLowerCase());
                    } else {
                        result.add(term);
                    }
            }
        }
        return result;
    }

    public static void main(String[] args) throws IOException {

        calculateACLRDJate1("/Users/-/work/jate/experiment/CValue_ALGORITHM.txt", args[1], args[2], true, false, true,
                2, 150, 1, 5,
                50, 100, 500, 1000, 5000, 10000);
        System.exit(1);

        /*createReportGenia(args[0],args[1],args[2],true, false, 2,150,1,5,
                50, 100, 500, 1000, 5000,10000);

        System.out.println();*/

        createReportACLRD(args[0], args[1], args[2], true, false, true, 2, 150, 1, 5,
                50, 100, 500, 1000, 5000, 10000);

        System.out.println();
    }
}
