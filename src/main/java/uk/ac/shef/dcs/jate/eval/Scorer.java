package uk.ac.shef.dcs.jate.eval;

import dragon.nlp.tool.lemmatiser.EngLemmatiser;
import opennlp.tools.util.eval.FMeasure;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.jate.PunctuationRemover;
import org.apache.solr.common.util.Pair;
import org.json.simple.parser.ParseException;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.nlp.Lemmatiser;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * compute scores
 */
public class Scorer {

    private final static String PATTERN_DIGITALS = "\\d+";
    private final static String PATTERN_SYMBOLS = "\\p{Punct}";

    public static void outputPrunedTerms(Lemmatiser lemmatiser, String gsFile, String outFile,
                                         boolean ignoreSymbols, boolean ignoreDigits, boolean lowercase,
                                         int minChar, int maxChar, int minTokens, int maxTokens) throws IOException {
        List<String> gs = GSLoader.loadGenia(gsFile);
        gs = prune(gs, ignoreSymbols, ignoreDigits, lowercase, minChar, maxChar, minTokens, maxTokens);
        gs = normalize(gs, lemmatiser);
        PrintWriter p = new PrintWriter(outFile);
        for (String t : gs) {
            p.println(t);
        }
        p.close();
    }

    public static void createReportGenia(Lemmatiser lemmatiser, String ateOutputFolder, String gsFile, String outFile,
                                         boolean ignoreSymbols, boolean ignoreDigits, boolean lowercase,
                                         int minChar, int maxChar, int minTokens, int maxTokens,
                                         int... ranks) throws IOException, ParseException {
        PrintWriter p = new PrintWriter(outFile);
        List<String> gs = GSLoader.loadGenia(gsFile);
        Map<String, double[]> pAtN = new TreeMap<>();
        Map<String, double[]> prf = new TreeMap<>();

        List<File> all = Arrays.asList(new File(ateOutputFolder).listFiles());
        Collections.sort(all);
        for (File f : all) {
            String name = f.getName();
            if (name.charAt(0) == '.')
                continue;
            System.out.println(f);
            List<String> terms = ATEResultLoader.load(f.toString());
            //List<String> terms = ATEResultLoader.loadToTermsRawText(f.toString());

            List<double[]> result = computePrecisionAtRank(
                    lemmatiser, gs, terms, ignoreSymbols, ignoreDigits, lowercase, minChar,
                    maxChar, minTokens, maxTokens, ranks);

            pAtN.put(name, result.get(0));
            pAtN.put(name.replaceAll(",", "_")+"_swe",result.get(2));
            pAtN.put(name.replaceAll(",", "_")+"_mwe",result.get(3));
            prf.put(name, result.get(1));
            //System.out.println("recall="+result.getValue());
        }

        //generate report

        StringBuilder sb = new StringBuilder();
        for (int i : ranks) {
            sb.append(",").append(i);
        }
        sb.append("\n");

        for (Map.Entry<String, double[]> en : pAtN.entrySet()) {
            sb.append(en.getKey().replaceAll(",", "_")).append(",");
            double[] s = en.getValue();
            for (int i = 0; i < ranks.length; i++) {
                sb.append(s[i]).append(",");

            }
            sb.append("\n");
        }
        p.println(sb.toString());
        p.println("\n\n\nPRF, P, R, F");
        sb = new StringBuilder();
        for (Map.Entry<String, double[]> en : prf.entrySet()) {
            sb.append(en.getKey().replaceAll(",", "_")).append(",");
            double[] s = en.getValue();
            for (int i = 0; i < s.length; i++) {
                sb.append(s[i]).append(",");
            }
            sb.append("\n");
        }
        p.println(sb.toString());
        p.close();
    }


    public static void createReportACLRD(Lemmatiser lemmatiser, String ateOutputFolder, String gsFile, String outFile,
                                         boolean ignoreSymbols, boolean ignoreDigits, boolean lowercase,
                                         int minChar, int maxChar, int minTokens, int maxTokens,
                                         int... ranks) throws IOException, JATEException, ParseException {
        PrintWriter p = new PrintWriter(outFile);
        List<String> gs = GSLoader.loadACLRD(gsFile);

        Map<String, double[]> scores = new TreeMap<>();
        Map<String, double[]> prf = new TreeMap<>();
        List<File> all = Arrays.asList(new File(ateOutputFolder).listFiles());
        Collections.sort(all);

        for (File f : all) {
            String name = f.getName();
            if (name.charAt(0) == '.')
                continue;
            System.out.println(f);
            List<String> terms = ATEResultLoader.load(f.toString());
            //List<String> terms = ATEResultLoader.loadToTermsRawText(f.toString());

            List<double[]> result = computePrecisionAtRank(lemmatiser, gs, terms, ignoreSymbols, ignoreDigits, lowercase, minChar,
                    maxChar, minTokens, maxTokens, ranks);
            scores.put(name.replaceAll(",", "_"), result.get(0));
            scores.put(name.replaceAll(",", "_")+"_swe",result.get(2));
            scores.put(name.replaceAll(",", "_")+"_mwe",result.get(3));
            prf.put(name.replaceAll(",", "_"), result.get(1));
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
        p.println("\n\n\nPRF, P, R, F");
        sb = new StringBuilder();
        for (Map.Entry<String, double[]> en : prf.entrySet()) {
            sb.append(en.getKey().replaceAll(",", "_")).append(",");
            double[] s = en.getValue();
            for (int i = 0; i < s.length; i++) {
                sb.append(s[i]).append(",");

            }
            sb.append("\n");
        }
        p.println(sb.toString());
        p.close();
    }

    public static double computeOverallPrecision(Collection<String> normGS, Collection<String> normCandidates) {
        double p = precision(normGS, normCandidates)[0];
        return round(p, 2);
    }

    public static double[] precision(Collection<String> gsTerms, Collection<String> topKTerms) {
        List<String> correct = new ArrayList<>(topKTerms);
        correct.retainAll(gsTerms);

        int sweCorrect = countSWETerms(correct);
        int sweAll = countSWETerms(topKTerms);
        int mweCorrect = correct.size() - sweCorrect;
        int mweAll = topKTerms.size() - sweAll;
        double sweP = sweAll == 0 ? Double.NaN : sweCorrect / (double) sweAll;
        double mweP = mweAll == 0 ? Double.NaN : mweCorrect / (double) mweAll;

        return new double[]{correct.size() / (double) topKTerms.size(),
                sweP, mweP};
    }

    public static double computeOverallRecall(Collection<String> normGS, Collection<String> normTerms) {
        List<String> correct = new ArrayList<>(normGS);
        correct.retainAll(normTerms);
        return (double) correct.size() / normGS.size();
    }


    /**
     * compute pre-configured top Ks precision with normalisation for both gold standard spans and ranked terms
     *
     * @param gs            gold standards
     * @param terms         extracted results
     * @param ignoreSymbols remove symbols
     * @param ignoreDigits  remove digits
     * @param minChar       minimum character length
     * @param maxChar       maximum character length
     * @param minTokens     minimum token size
     * @param maxTokens     maximum token size
     * @param ranks         list of rankinig for evaluation
     * @return double[]  list of precision@K
     */
    public static List<double[]> computePrecisionAtRank(Lemmatiser lemmatiser, List<String> gs, List<String> terms,
                                                                  boolean ignoreSymbols, boolean ignoreDigits, boolean lowercase,
                                                                  int minChar, int maxChar, int minTokens, int maxTokens,
                                                                  int... ranks) {
        gs = prune(gs, ignoreSymbols, ignoreDigits, lowercase, minChar, maxChar, minTokens, maxTokens);
        gs = normalize(gs, lemmatiser);
        terms = prune(terms, ignoreSymbols, ignoreDigits, lowercase, minChar, maxChar, minTokens, maxTokens);

        double[] scores = new double[ranks.length];
        double[] scoresSWE = new double[ranks.length];
        double[] scoresMWE = new double[ranks.length];
        for (int i = 0; i < ranks.length; i++) {
            double[] p = precision(gs, terms.subList(0, ranks[i]));
            scores[i] = round(p[0], 2);
            scoresSWE[i] = p[1];
            scoresMWE[i] = p[2];
        }

        double recall = computeOverallRecall(gs, terms);

        List<String> expectedTerms = new ArrayList<>(gs);
        expectedTerms.retainAll(terms);

        System.out.println("recall=" + recall + ", expectedterms for avg=" + expectedTerms.size());
        double[] prf = computePRF(expectedTerms, terms);

        //return new Pair<>(scores, prf);
        List<double[]> result=new ArrayList<>();
        result.add(scores);
        result.add(prf);
        result.add(scoresSWE);
        result.add(scoresMWE);
        return result;
    }

    public static double[] computePRF(List<String> maxExpectedGS, List<String> normCandidates) {
        List<String> subList = new ArrayList<>();
        int K = maxExpectedGS.size();
        double prevR = 0.0;
        double sum = 0.0;
        for (int i = 0; i < K; i++) {
            /*if(i>2)
                System.out.println();*/
            subList.add(normCandidates.get(i));
        }
        double p = computeOverallPrecision(maxExpectedGS, subList);
        double r = computeOverallRecall(maxExpectedGS, subList);
        double f = 2 * p * r / (p + r);
        if (r == 0)
            f = 0;
        return new double[]{p, r, f};
    }


    public static double round(double value, int scale) {
        return new BigDecimal(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    // public static double[] computeTopKPrecision()

    public static double recall(List<String> gsTerms, List<String> termResults) {
        /*double recall = FMeasure.recall(gsTerms.toArray(), termResults.toArray());
        System.out.println(new Date());*/

        Set<String> copy = new HashSet<>(gsTerms);
        copy.retainAll(new HashSet<>(termResults));

        return round((double) copy.size() / gsTerms.size(), 2);
    }


    public static List<String> normalize(List<String> gsTerms, Lemmatiser lemmatiser) {
        List<String> result = new ArrayList<>();
        for (String term : gsTerms) {
            String normalisedTerm = normaliseTerm(term, lemmatiser);
            if (StringUtils.isNotEmpty(normalisedTerm)) {
                result.add(normalisedTerm);
            }
        }
        return result;

    }

    public static String normaliseTerm(String term, Lemmatiser lemmatiser) {
        String normalisedTerm = lemmatiser.normalize(term, "NN").trim();
        return normalisedTerm;
    }

    public static int countSWETerms(Collection<String> list) {
        int swe = 0;
        for (String t : list) {
            if (t.split("\\s+").length == 1)
                swe++;
        }
        return swe;
    }

    /**
     * prune term list for benchmarking
     * <p>
     * this methods enable to calculate the agreement of two term set on a strict or lenient basis
     *
     * @param terms         term list to be pruned
     * @param ignoreSymbols remove symbols, see also: PunctuationRemover#stripPunctuations()
     * @param ignoreDigits  remove digits
     * @param minChar       minimum characters allowed
     * @param maxChar       maximum characters allowed, set -1 avoid the constraints
     * @param minTokens     minimum tokens allowed
     * @param maxTokens     maximum tokens allowed, set -1 avoid the constraints
     * @return List<String> list of normalised term set
     */
    public static List<String> prune(List<String> terms, boolean ignoreSymbols, boolean ignoreDigits, boolean lowercase,
                                     int minChar, int maxChar, int minTokens, int maxTokens) {
        List<String> result = new ArrayList<>();
        for (String term : terms) {
            String prunedTerm = prune(term, ignoreSymbols, ignoreDigits, lowercase, minChar, maxChar, minTokens, maxTokens);
            if (StringUtils.isNotEmpty(prunedTerm)) {
                result.add(prunedTerm);
            }
        }
        return result;
    }

    /**
     * Prune term surface form
     * see also @code {@link PunctuationRemover#stripPunctuations(String, boolean, boolean, boolean)}
     *
     * @param term,          term surface form to be pruned
     * @param ignoreSymbols, if strip symbols in the term
     * @param ignoreDigits,  if strip digits in the term
     * @param lowercase,     if lower case the term
     * @param minChar,       min char to strip
     * @param maxChar,       max char to strip
     * @param minTokens,     min token to strip
     * @param maxTokens,     max token to strip
     * @return pruned term, return "" if the term is filtered by various constraints
     */
    private static String prune(String term, boolean ignoreSymbols, boolean ignoreDigits, boolean lowercase, int minChar, int maxChar, int minTokens, int maxTokens) {
        String prunedTerm = term;
        if (ignoreDigits)
            prunedTerm = prunedTerm.replaceAll(PATTERN_DIGITALS, " ").replaceAll("\\s+", " ");
        if (ignoreSymbols)
            prunedTerm = prunedTerm.replaceAll(PATTERN_SYMBOLS, " ").replaceAll("\\s+", " ");

        int start = 0, end = prunedTerm.length();
        for (int i = 0; i < prunedTerm.length(); i++) {
            if (Character.isLetterOrDigit(prunedTerm.charAt(i))) {
                start = i;
                break;
            }
        }
        for (int i = prunedTerm.length() - 1; i > -1; i--) {
            if (Character.isLetterOrDigit(prunedTerm.charAt(i))) {
                end = i + 1;
                break;
            }
        }

        prunedTerm = prunedTerm.substring(start, end).trim();

        if (prunedTerm.length() >= minChar && prunedTerm.length() <= maxChar || maxChar == -1) {
            int tokens = prunedTerm.split("\\s+").length;
            if (tokens >= minTokens && tokens <= maxTokens || maxTokens == -1) {
                if (lowercase) {
                    prunedTerm = prunedTerm.toLowerCase();
                }
            } else {
                //System.out.println("missing term (not within the token range):"+term);
                prunedTerm = "";
            }

        } else {
            //System.out.println("missing term (not within the char range):"+term);
            prunedTerm = "";
        }
        return prunedTerm;
    }


    public static void main(String[] args) throws IOException, JATEException, ParseException {

        /*calculateACLRDJate1("/Users/-/work/jate/experiment/CValue_ALGORITHM.txt", args[1], args[2], true, false, true,
                2, 150, 1, 5,
                50, 100, 500, 1000, 5000, 10000);
        System.exit(1);*/

        Lemmatiser lem = new Lemmatiser(new EngLemmatiser(args[4],
                false, false));
        if (args[3].equals("genia")) {
            createReportGenia(lem, args[0], args[1], args[2], true, false, true, 2, 100, 1, 5,
                    50, 100, 500, 1000, 2000, 4000);
        } else {
            createReportACLRD(lem, args[0], args[1], args[2], true, false, true, 2, 100, 1, 10,
                    50, 100, 500, 1000, 2000, 4000);
        }
        //outputPrunedTerms(lem, args[0], args[1], true, false, true, 2, 100, 1, 5);
        System.out.println(new Date());
        //System.exit(0);
    }
}
