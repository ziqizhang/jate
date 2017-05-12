package uk.ac.shef.dcs.jate.eval;

import dragon.nlp.tool.lemmatiser.EngLemmatiser;
import opennlp.tools.util.eval.FMeasure;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.jate.PunctuationRemover;
import org.json.simple.parser.ParseException;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.nlp.Lemmatiser;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * compute scores
 */
public class Scorer {
    private final static String PATTERN_DIGITALS = "\\d+";
    private final static String PATTERN_SYMBOLS = "\\p{Punct}";
    public final static String FILE_TYPE_JSON = "json";
    public final static String FILE_TYPE_CSV = "csv";

    private static int EVAL_CONDITION_MIN_TERM_CONTEXT_FREQUENCY = 1;
    private static int EVAL_CONDITION_MIN_TERM_TOTAL_FREQUENCY = 1;
    private static int EVAL_CONDITION_CUTOFF_TOP_K_PERCENT = 1;
    private static boolean EVAL_CONDITION_IGNORE_SYMBOL = true;
    private static boolean EVAL_CONDITION_IGNORE_DIGITS = false;
    private static boolean EVAL_CONDITION_CASE_INSENSITIVE = true;
    private static int EVAL_CONDITION_CHAR_RANGE_MIN = 1;
    private static int EVAL_CONDITION_CHAR_RANGE_MAX = -1;
    private static int EVAL_CONDITION_TOKEN_RANGE_MIN = 1;
    private static int EVAL_CONDITION_TOKEN_RANGE_MAX = -1;
    private static int[] EVAL_CONDITION_TOP_N = {50, 100, 300, 500, 800, 1000, 1500,
            2000, 3000, 4000, 5000, 6000, 7000, 8000,9000,10000, 15000, 20000, 25000, 30000};

    // top K percentage of candidates
    private static int[] EVAL_CONDITION_TOP_K = {1, 2, 3, 4, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 75, 80};


    public static void createReportACLRD(Lemmatiser lemmatiser, String ateOutputFolder, String gsFile, String outFile,
                                         boolean ignoreSymbols, boolean ignoreDigits, boolean lowercase,
                                         int minChar, int maxChar, int minTokens, int maxTokens,
                                         int... ranks) throws IOException, JATEException, ParseException {
        PrintWriter p = new PrintWriter(outFile);
        List<String> gs = GSLoader.loadACLRD(gsFile);

        Map<String, double[]> scores = new TreeMap<>();
        List<File> all = Arrays.asList(new File(ateOutputFolder).listFiles());
        Collections.sort(all);

        for (File f : all) {
            String name = f.getName();
            if (String.valueOf(name.charAt(0)).equals("."))
                continue;
            System.out.println(f);
            List<String> terms = ATEResultLoader.loadFromJSON(f.toString());

            double[] s = computePrecisionAtRank(lemmatiser, gs, terms, ignoreSymbols, ignoreDigits, lowercase, minChar,
                    maxChar, minTokens, maxTokens, ranks);
            scores.put(name.replace(",","_"), s);

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

    public static void createReportGenia(Lemmatiser lemmatiser, String ateOutputFolder, String ateOutputType, String gsFile, String outFile,
                                         boolean ignoreSymbols, boolean ignoreDigits, boolean caseInsensitive,
                                         int minChar, int maxChar, int minTokens, int maxTokens,
                                         int[] topNRanks, int[] topKRanks) throws IOException, ParseException {
        PrintWriter p = new PrintWriter(outFile);
        List<String> gsTerms = GSLoader.loadGenia(gsFile);

        List<String> normGS = prune(gsTerms, ignoreSymbols, ignoreDigits, caseInsensitive,
                minChar, maxChar,minTokens, maxTokens);
        normGS = normalize(normGS, lemmatiser, true);

        Map<String, double[]> scores = new HashMap<>();

        List<File> all = Arrays.asList(new File(ateOutputFolder).listFiles());
        Collections.sort(all);
        for (File f : all) {
            String name = f.getName();

            if (!name.contains(ateOutputType)) {
                continue;
            }

            System.out.println("evaluating "+name+" ...");

            if (String.valueOf(name.charAt(0)) .equals("."))
                continue;
            //System.out.println(f);
            List<String> rankedTerms = new ArrayList<>();
            if (FILE_TYPE_JSON.equals(ateOutputType)) {
                rankedTerms = ATEResultLoader.loadFromJSON(f.toString());
            } else if (FILE_TYPE_CSV.equals(ateOutputType)) {
                rankedTerms = ATEResultLoader.loadFromCSV(f.toString());
            }

            List<String> normCandidates = prune(rankedTerms, ignoreSymbols, ignoreDigits, caseInsensitive, minChar, maxChar, minTokens, maxTokens);
            normCandidates = normalize(normCandidates, lemmatiser, true);

            double[] values = new double[topNRanks.length+2];

            double[] topNPrecisions = computePrecisionAtRank(normGS, normCandidates, topNRanks);

            int[] topNRanksFromTopK = getTopNFromTopK(rankedTerms.size(), topKRanks);

            double[] topKPrecision = computePrecisionAtRank(normGS, normCandidates, topNRanksFromTopK);
            double[] topKRecall = computeRecallAtRank(normGS, normCandidates, topNRanksFromTopK);

            double overallPrecision = computeOverallPrecision(normGS, normCandidates);

            double overallRecall = computeOverallRecall(normGS, normCandidates);

            double overallF = getFMeasure(overallPrecision, overallRecall);

            values = ArrayUtils.addAll(topNPrecisions, topKPrecision);
            values = ArrayUtils.addAll(values, topKRecall);
            values = ArrayUtils.addAll(values, new double[]{overallPrecision, overallRecall, overallF, rankedTerms.size()});

            scores.put(name, values);
        }

        //generate report

        StringBuilder sb = new StringBuilder();
        //heads
        for (int i : topNRanks) {
            sb.append(",").append(i);
        }
        for (int k : topKRanks) {
            sb.append(",").append(k+"%_P");
        }
        for (int k : topKRanks) {
            sb.append(",").append(k+"%_R");
        }
        sb.append(",").append("Overall_P");
        sb.append(",").append("Overall_R");
        sb.append(",").append("Overall_F");
        sb.append(",").append("Total_Size");
        sb.append("\n");

        for (Map.Entry<String, double[]> en : scores.entrySet()) {
            sb.append(en.getKey()).append(",");
            double[] values = en.getValue();
            for (int i = 0; i < topNRanks.length; i++) {
                sb.append(values[i]).append(",");
            }
            for (int i = 0; i < topKRanks.length; i++) {
                sb.append(values[i+topNRanks.length]).append(",");
            }
            for (int i = 0; i < topKRanks.length; i++) {
                sb.append(values[i+topNRanks.length+topKRanks.length]).append(",");
            }
            sb.append(values[values.length-4]).append(",");
            sb.append(values[values.length-3]).append(",");
            sb.append(values[values.length-2]).append(",");
            sb.append(values[values.length-1]);
            sb.append("\n");
        }
        p.println(sb.toString());
        p.close();
        System.out.println(String.format("complete. Check the latest evaluation in [%s]", outFile));
    }

    private static int[] getTopNFromTopK(int totalCandidateSize, int[] topKs) {
        int[] topNs = new int[topKs.length];
        int topKIndex = 0;
        for (int topK : topKs) {
            topNs[topKIndex] = Math.round(totalCandidateSize * ((float)topK/100));
            ++topKIndex;
        }
        return topNs;
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
     * @param ranks         list of ranking for evaluation
     * @return double[]  list of precision@K
     */
    public static double[] computePrecisionAtRank(Lemmatiser lemmatiser, List<String> gs, List<String> terms,
                                                  boolean ignoreSymbols, boolean ignoreDigits, boolean lowercase,
                                                  int minChar, int maxChar, int minTokens, int maxTokens,
                                                  int... ranks) {
        List<String> normGS = prune(gs, ignoreSymbols, ignoreDigits, lowercase, minChar, maxChar, minTokens, maxTokens);
        normGS = normalize(normGS, lemmatiser, true);

        List<String> normCandidates = prune(terms, ignoreSymbols, ignoreDigits, lowercase, minChar, maxChar, minTokens, maxTokens);
        normCandidates = normalize(normCandidates, lemmatiser, true);

        return computePrecisionAtRank(normGS, normCandidates, ranks);
    }

    public static double[] computePrecisionAtRank(List<String> normGS, List<String> normCandidates, int[] ranks) {
        double[] scores = new double[ranks.length];
        for (int i = 0; i < ranks.length; i++) {
            if (normCandidates.size() > ranks[i]) {
                scores[i] = computeOverallPrecision(normGS, normCandidates.subList(0, ranks[i]));
            }
        }

        return scores;
    }

    public static double computeOverallPrecision(Lemmatiser lemmatiser, List<String> gs, List<String> terms,
                                                  boolean ignoreSymbols, boolean ignoreDigits, boolean lowercase,
                                                  int minChar, int maxChar, int minTokens, int maxTokens) {
        List<String> normGS = prune(gs, ignoreSymbols, ignoreDigits, lowercase, minChar, maxChar, minTokens, maxTokens);
        normGS = normalize(normGS, lemmatiser, true);

        List<String> normCandidates = prune(terms, ignoreSymbols, ignoreDigits, lowercase, minChar, maxChar, minTokens, maxTokens);
        normCandidates = normalize(normCandidates, lemmatiser, true);

        return computeOverallPrecision(normGS, normCandidates);
    }

    public static double[] computeRecallAtRank(List<String> normGS, List<String> normCandidates, int[] ranks) {
        double[] scores = new double[ranks.length];
        for (int i = 0; i < ranks.length; i++) {
            if (normCandidates.size() > ranks[i]) {
                scores[i] = computeOverallRecall(normGS, normCandidates.subList(0, ranks[i]));
            }
        }

        return scores;
    }

    public static double computeOverallPrecision(List<String> normGS, List<String> normCandidates) {
        double p = precision(normGS, normCandidates);
        return round(p, 2);
    }


    /**
     * @param lemmatiser,    for lemmatisation
     * @param gsTerms,       reference term list
     * @param terms,         tagged/predicted terms
     * @param ignoreSymbols, ignore symbols,
     * @param ignoreDigits,  ignore digits
     * @param lowercase,     lower case
     * @param minChar,       min char for pruning
     * @param maxChar,       max char for pruning
     * @param minTokens,     min tokens for pruning
     * @param maxTokens,     max tokens for pruning
     * @return FMeasure, final measurement
     */
    public static FMeasure computeFMeasureWithNormalisation(Lemmatiser lemmatiser, List<String> gsTerms, List<String> terms,
                                                            boolean ignoreSymbols, boolean ignoreDigits, boolean lowercase,
                                                            int minChar, int maxChar, int minTokens, int maxTokens) {

        Set<String> finalGSTermSet = termSetNormalisation(gsTerms, lemmatiser, ignoreSymbols,
                ignoreDigits, lowercase, minChar, maxChar, minTokens, maxTokens);
        Set<String> finalTagTermSet = termSetNormalisation(terms, lemmatiser, ignoreSymbols,
                ignoreDigits, lowercase, minChar, maxChar, minTokens, maxTokens);

        FMeasure fMeasure = new FMeasure();
        fMeasure.updateScores(finalGSTermSet.toArray(), finalTagTermSet.toArray());
        return fMeasure;
    }

    public static Set<String> termSetNormalisation(List<String> terms, Lemmatiser lemmatiser,
                                                   boolean ignoreSymbols, boolean ignoreDigits, boolean lowercase,
                                                   int minChar, int maxChar, int minTokens, int maxTokens) {
        List<String> normalisedTerms = prune(terms, ignoreSymbols, ignoreDigits, lowercase, minChar, maxChar, minTokens, maxTokens);
        normalisedTerms = normalize(normalisedTerms, lemmatiser, true);

        Set<String> finalTerms = new HashSet<>();
        finalTerms.addAll(normalisedTerms);
        return finalTerms;
    }

    /**
     * term normlisation including pruning the term as the first step and then lemmatisation
     *
     * @param term term surface form
     * @param lemmatiser lemmatiser to be used
     * @param ignoreSymbols strip symbols in the term
     * @param ignoreDigits strip digits in the term
     * @param lowercase lowercase the term
     * @param minChar strip the term if contain min char
     * @param maxChar strip the term if contain max char
     * @param minTokens strip the term if contain min token
     * @param maxTokens strip the term if contain max token
     * @return String normed term
     */
    public static String termNormalisation(String term, Lemmatiser lemmatiser,
                                           boolean ignoreSymbols, boolean ignoreDigits, boolean lowercase,
                                           int minChar, int maxChar, int minTokens, int maxTokens) {
        String prunedTerm = prune(term, ignoreSymbols, ignoreDigits, lowercase, minChar, maxChar, minTokens, maxTokens);
        String normedTerm = normaliseTerm(prunedTerm, lemmatiser);

        return normedTerm;
    }


//    /**
//     * compute Top K precision with strict or lenient benchmarking
//     *
//     * @return
//     */

    /**
     * RESTORED Previous precision method. As it is redundant to normalise terms for every top K, and it is very
     * computationally expensive for large list.
     * <p>
     * compute Top K precision with strict or lenient benchmarking
     */
    /*public static double computePrecisionWithNormalisation(List<String> gsTerms, List<String> terms,
                                                             boolean ignoreSymbols, boolean ignoreDigits,
                                                             boolean lowercase, int topK) throws JATEException {
        double score = 0.0;

        if (terms == null || terms.size() ==0) {
            return score;
        }

        if (gsTerms == null || gsTerms.size() == 0) {
            throw new JATEException("Gold Standard Terms is null!");
        }

        int defaultMinChar = 2; //originall 1. should use 2 at least
        int defaultMaxChar = 1000;
        int defaultMinTokens=1;
        int defaultMaxTokens=10;

        gsTerms = prune(gsTerms, ignoreSymbols, ignoreDigits, lowercase,
                defaultMinChar, defaultMaxChar, defaultMinTokens, defaultMaxTokens);
        terms = prune(terms, ignoreSymbols, ignoreDigits, lowercase,
                defaultMinChar, defaultMaxChar, defaultMinTokens, defaultMaxTokens);

        List<String> topKTerms = terms.subList(0, topK);
        score = precision(gsTerms, topKTerms);

        //score=computePrecisionAtRank(gsTerms, terms,topK);

        return round(score, 2);
    }*/
    public static double round(double value, int scale) {
        return new BigDecimal(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * calculate
     *
     * @param gsTerms   the gold standard spans
     * @param topKTerms the predicted/extracted and ranked spans, top K results can be selected for scoring
     * @return double  precision score
     */
    public static double precision(List<String> gsTerms, List<String> topKTerms) {
        return FMeasure.precision(gsTerms.toArray(), topKTerms.toArray());
    }


    /**
     *
     * @param gsTerms GS terms
     * @param termResults, ranked term candidates
     * @param lemmatiser lemmatiser to normalise both GS terms and ranked term candidates
     * @param ignoreSymbols bool value whether to ignore symbols for the evaluation
     * @param ignoreDigits bool value whether to ignore digits for the evaluation
     * @param lowercase bool value whether to ignore case for the evaluation
     * @param minChar min char for whether to filter GS terms and candidate within a length range for the evaluation
     * @param maxChar max char for whether to filter GS terms and candidate within a length range for the evaluation
     * @param minTokens min token for whether to filter GS terms and candidate within a length range for the evaluation
     * @param maxTokens max token for whether to filter GS terms and candidate within a length range for the evaluation
     * @return computeOverallRecall
     */
    public static double computeOverallRecall(List<String> gsTerms, List<String> termResults, Lemmatiser lemmatiser,
                                              boolean ignoreSymbols, boolean ignoreDigits, boolean lowercase,
                                              int minChar, int maxChar, int minTokens, int maxTokens) {
        List<String> normGS = prune(gsTerms, ignoreSymbols, ignoreDigits, lowercase, minChar, maxChar, minTokens, maxTokens);
        normGS = normalize(normGS, lemmatiser, true);
        List<String> normTerms = prune(termResults, ignoreSymbols, ignoreDigits, lowercase, minChar, maxChar, minTokens, maxTokens);
        normTerms = normalize(normTerms, lemmatiser, true);

        return computeOverallRecall(normGS, normTerms);
    }

    public static double computeOverallRecall(List<String> normGS, List<String> normTerms) {
        return round(FMeasure.recall(normGS.toArray(), normTerms.toArray()), 2);
    }

    public static double[] topNRecall(List<String> normGSTerms, List<String> normRankedTerms, int[] topN) {
        double[] topNRecalls = new double[topN.length];
        for (int i = 0; i < topN.length; i++) {
            if (normRankedTerms.size() > topN[i]) {
                double r = FMeasure.recall(normGSTerms.toArray(), normRankedTerms.subList(0, topN[i]).toArray());
                topNRecalls[i] = round(r, 2);
            }
        }
        return topNRecalls;
    }

    public static List<String> synonymNormalisation4Genia(List<String> terms) {
        List<String> synonymNormList = new ArrayList<>();
        for (String term : terms) {
            String normTerm = term.replace("mouse", "mice");
            normTerm = normTerm.replace("Mouse", "Mice");
            normTerm = normTerm.replace("analyses", "analysis");
            normTerm = normTerm.replace("Analyses", "Analysis");
            normTerm = normTerm.replace("women", "woman");
            normTerm = normTerm.replace("l cell resistance", "Lymphoid cell resistance");
            normTerm = normTerm.replace("DS lymphocyte", "DS ones");

            synonymNormList.add(normTerm);
        }
        return synonymNormList;
    }



    public static double fmeasure(List<String> gsTerms, List<String> termResults) {
        FMeasure fMeasure = new FMeasure();
        fMeasure.updateScores(gsTerms.toArray(), termResults.toArray());
        return fMeasure.getFMeasure();
    }

    /**
     * Retrieves the f-measure score.
     *
     * f-measure = 2 * precision * computeOverallRecall / (precision + computeOverallRecall)
     * @return the rounded f-measure or -1 if precision + computeOverallRecall &lt;= 0
     */
    public static double getFMeasure(Double precision, Double recall) {

        if (precision + recall > 0) {
            double fmeasure = 2 * (precision * recall)
                    / (precision + recall);
            return round(fmeasure, 2);
        } else {
            // cannot divide by zero, return error code
            return -1;
        }
    }

    /**
     * e.g., try to lemmatize "highly purified monocytes/macrophages" into "highly purified monocyte/macrophage"
     *
     * @param symbolCompound compound word
     * @param symbol given symbol to split compound into two term units
     * @param lemmatiser lemmatiser
     * @return lemmatized form of compound
     */
    public static String lemmatizeSymbolCompoundTerm(String symbolCompound, String symbol, Lemmatiser lemmatiser) {
        String[] splittedCompound = symbolCompound.split(symbol);
        String lemmatizedCompound = "";
        if (splittedCompound.length == 2) {
            lemmatizedCompound = normaliseTerm(splittedCompound[0], lemmatiser) + symbol + normaliseTerm(splittedCompound[1], lemmatiser);
            return lemmatizedCompound;
        }
        return symbolCompound;
    }

    /**
     * normalise term list
     *
     * remove duplicates while retaining order
     * @param termlist term list to be lemmatized
     * @param lemmatiser lemmatiser initialised
     * @param isTokenLevel is to apply lemmatization to token level
     * @return normalised term list
     */
    public static List<String> normalize(List<String> termlist, Lemmatiser lemmatiser, boolean isTokenLevel) {
        List<String> result = new ArrayList<>();
        for (String term : termlist) {
            String[] termUnits = term.split(" ");
            String normalisedTerm ="";

            if (isTokenLevel) {
                for (String termUnit : termUnits) {
                    // for terms such like "highly purified monocytes/macrophages"
                    if (termUnit.contains("/")) {
                        normalisedTerm += lemmatizeSymbolCompoundTerm(termUnit, "/", lemmatiser) + " ";
                    } else {
                        normalisedTerm += normaliseTerm(termUnit, lemmatiser) + " ";
                    }
                }
                normalisedTerm = normalisedTerm.trim();
            } else {
                normalisedTerm = normaliseTerm(term, lemmatiser);
            }

            if (StringUtils.isNotEmpty(normalisedTerm) && !result.contains(normalisedTerm)) {
                result.add(normalisedTerm);
            }
        }
        return result;

    }

    public static String normaliseTerm(String term, Lemmatiser lemmatiser) {
        String normalisedTerm = lemmatiser.normalize(term, "NN").trim();
        return normalisedTerm;
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
            if (StringUtils.isNotEmpty(prunedTerm) && !result.contains(prunedTerm)) {
                result.add(prunedTerm);
            }
        }
        return result;
    }

    /**
     * Prune term surface form
     * see also @code {@link PunctuationRemover#stripPunctuations(String, boolean, boolean, boolean)}
     *
     * @param term, term surface form to be pruned
     * @param ignoreSymbols, if strip symbols in the term
     * @param ignoreDigits, if strip digits in the term
     * @param lowercase, if lower case the term
     * @param minChar, min char to strip
     * @param maxChar, max char to strip
     * @param minTokens, min token to strip
     * @param maxTokens, max token to strip
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
        if (args == null || args.length != 4) {
            StringBuilder sb = new StringBuilder("Usage:\n");
            sb.append("java -cp 'jate.jar' ").append(Scorer.class.getName()).append(" ")
                    .append("[CORPUS_NAME] [ATE_OUTPUT_DIR] [ATE_OUTPUT_FILE_TYPE] ").append("\n\n");
            sb.append("Example: java -cp 'jate.jar' /c/jate/outputDir/ csv genia_eval.csv \n\n");
            sb.append("[OPTIONS]:\n")
                    .append("\t\targs[0]:\t\t 'genia' or any other dataset name.\n")
                    .append("\t\targs[1]:\t\t ATE algorithms output folder that contains one or more ranked term candidates output.\n")
                    .append("\t\targs[2]:\t\t ATE algorithms output file type. Two options are 'csv' and 'json'. If file type is 'csv', it should contain a header row. \n")
                    .append("\t\targs[3]:\t\t A file name & path to save evaluation output (should not be the same folder of ATE algorithm output.\n");

            System.out.println(sb);
            System.exit(-1);
        }
        String workingDir = System.getProperty("user.dir");
        Lemmatiser lemmatiser = new Lemmatiser(new EngLemmatiser(
                Paths.get(workingDir, "src", "test", "resource", "lemmatiser").toString(), false, false
        ));

        String datasetName = args[0];
        String ateOutputFolder = args[1];
        String ateOutputType = args[2];
        String outFile = args[3];

        if (datasetName.equals("genia")) {
            Path GENIA_CORPUS_CONCEPT_FILE = Paths.get(workingDir, "src", "test", "resource",
                    "eval", "GENIA", "concept.txt");
            createReportGenia(lemmatiser, ateOutputFolder, ateOutputType, GENIA_CORPUS_CONCEPT_FILE.toString(), outFile,
                    EVAL_CONDITION_IGNORE_SYMBOL, EVAL_CONDITION_IGNORE_DIGITS, EVAL_CONDITION_CASE_INSENSITIVE,
                    EVAL_CONDITION_CHAR_RANGE_MIN, EVAL_CONDITION_CHAR_RANGE_MAX,
                    EVAL_CONDITION_TOKEN_RANGE_MIN, EVAL_CONDITION_TOKEN_RANGE_MAX,
                    EVAL_CONDITION_TOP_N, EVAL_CONDITION_TOP_K);


        /*calculateACLRDJate1("/Users/-/work/jate/experiment/CValue_ALGORITHM.txt", args[1], args[2], true, false, true,
                2, 150, 1, 5,
                50, 100, 500, 1000, 5000, 10000);
        System.exit(1);*/
/**
        Lemmatiser lem = new Lemmatiser(new EngLemmatiser(args[4],
                false, false));
        if (args[3].equals("genia")) {
            createReportGenia(lem, args[0], args[1], args[2], true, false, true, 2, 100, 1, 5,
                    50, 100, 500, 1000, 2000, 3000);
*/
        } else {
            createReportACLRD(lemmatiser, args[0], args[1], args[2], true, false, true, 2, 100, 1, 10,
                    50, 100, 300, 500, 800, 1000, 1500, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000);
        }
    }
}
