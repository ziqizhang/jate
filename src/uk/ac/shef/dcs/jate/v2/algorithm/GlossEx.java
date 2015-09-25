package uk.ac.shef.dcs.jate.v2.algorithm;

import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.feature.AbstractFeature;
import uk.ac.shef.dcs.jate.v2.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.v2.model.JATETerm;
import uk.ac.shef.dcs.jate.v2.model.TermInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * An implementation of the GlossEx term recognition algorithm. See Park, et. al 2002, <i>
 * Automatic Glossary Extraction: beyond terminology identification</i>
 *. This is the implementation of the scoring formula <b>only</b>, and does not include the filtering algorithm as mentioned
 * in the paper.
 * <p>
 * In the equation C(T) = a* TD(T) + B*TC(T), default a=0.2, B = 0.8.
 * </p>
 *
 * You might need to modify the value of B by increasing it substaintially when the reference corpus is relatively
 * much bigger than the target corpus, such as the BNC corpus. For details, please refer to the paper.
 *
 */
public class GlossEx extends Algorithm {
    protected final double alpha;
    protected final double beta;
    protected double nullWordProbInReference;//if a word is not found int the reference/general corpus,
    //what probability should we use? Note this value has inverse relation wrt prob(wi) where wi is a word in the
    //domain corpus. a value of 0.1 has the effect of raise prob(wi) by 10x.
    //so a term that contains a word which does not exist in reference/general corpus can get an extremely high score
    //by default we set it to the smallest word prob in the reference corpus
    protected boolean matchOOM=true;

    public static final String SUFFIX_REF ="_REF";
    public static final String SUFFIX_WORD ="_WORD";

    public GlossEx(){
        this(0.2,0.8,true);
    }

    public GlossEx(double alpha, double beta, boolean matchOOM){
        this.alpha = alpha;
        this.beta = beta;
        this.matchOOM=matchOOM;
    }

    public List<JATETerm> execute(Set<String> candidates) throws JATEException {
        AbstractFeature feature = features.get(FrequencyTermBased.class.getName());
        validateFeature(feature, FrequencyTermBased.class);
        FrequencyTermBased fFeatureTerms = (FrequencyTermBased) feature;

        AbstractFeature feature2 = features.get(FrequencyTermBased.class.getName() + SUFFIX_WORD);
        validateFeature(feature2, FrequencyTermBased.class);
        FrequencyTermBased fFeatureWords = (FrequencyTermBased) feature2;

        AbstractFeature feature3 = features.get(FrequencyTermBased.class.getName() + SUFFIX_REF);
        validateFeature(feature3, FrequencyTermBased.class);
        FrequencyTermBased fFeatureRef = (FrequencyTermBased) feature3;
        nullWordProbInReference = setMinWordProbInReference(fFeatureRef);
        double refScalar = matchOrdersOfMagnitude(fFeatureWords, fFeatureRef);

        List<JATETerm> result = new ArrayList<>();
        boolean collectInfo = termInfoCollector != null;
        double totalWordsInCorpus = fFeatureWords.getCorpusTotal();
        for (String tString : candidates) {
            int ttf = fFeatureTerms.getTTF(tString);
            double score;
            String[] elements = tString.split(" ");
            double T = (double) elements.length;
            double SUMwi = 0.0;
            double SUMfwi = 0.0;
            //some terms are artificially incremented by 1 to avoid division by 0 and also to
            for (int i = 0; i < T; i++) {
                String wi = elements[i];
                double pc_wi = fFeatureRef.getTTFNorm(wi);
                if (pc_wi == 0)
                    pc_wi = nullWordProbInReference; //
                pc_wi*=refScalar;
                SUMwi += /*Math.log(*/(double) fFeatureWords.getTTF(wi) / totalWordsInCorpus / pc_wi/*)*/;
                SUMfwi += (double) fFeatureWords.getTTF(wi);
            }

            double TD = SUMwi / T;
            double TC = (T * Math.log10(ttf + 1) * ttf) / (SUMfwi + 1);

            if (T == 1) score = 0.9 * TD + 0.1 * TC;
            else score = alpha * TD + beta * TC;

            JATETerm term = new JATETerm(tString, score);
            if (collectInfo) {
                TermInfo termInfo = termInfoCollector.collect(tString);
                term.setTermInfo(termInfo);
            }
            result.add(term);
        }

        Collections.sort(result);
        return result;
    }

    private double matchOrdersOfMagnitude(FrequencyTermBased fFeatureWords, FrequencyTermBased fFeatureRef) {
        double totalScore=0, totalWords=0;
        for(String t: fFeatureRef.getMapTerm2TTF().keySet()){
            totalWords++;
            totalScore+=fFeatureRef.getTTFNorm(t);
        }
        double meanRef=totalScore/totalWords;

        totalScore=0; totalWords=0;
        for(String t: fFeatureWords.getMapTerm2TTF().keySet()){
            totalWords++;
            totalScore+=fFeatureWords.getTTFNorm(t);
        }
        double mean = totalScore/totalWords;

        if(Double.isFinite(meanRef)){
            int oomRef=(int)Math.log10(meanRef);
            int oom = (int) Math.log10(mean);

            double s = Math.pow(10, (oom-oomRef));
            return s;
        }
        else
            return 1.0;
    }

    public double setMinWordProbInReference(FrequencyTermBased ref) {
        List<Integer> freq = new ArrayList<>(ref.getMapTerm2TTF().values());
        Collections.sort(freq);
        if(freq.size()>0) {
            int min = freq.get(0);
            return (double)min/ref.getCorpusTotal();
        }
        else
            return 0.1;
    }

}
