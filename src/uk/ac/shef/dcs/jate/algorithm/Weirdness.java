package uk.ac.shef.dcs.jate.algorithm;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.feature.AbstractFeature;
import uk.ac.shef.dcs.jate.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.model.JATETerm;
import uk.ac.shef.dcs.jate.model.TermInfo;

import java.util.*;
import java.util.logging.Logger;

/**
 * An implementation of the word weirdness algorithm applied to term recognition algorithm. See
 * Ahmad et al 1999, <i>Surrey Participation in TREC8: Weirdness Indexing for Logical Document Extrapolation
 * and Retrieval</i>
 */
public class Weirdness extends ReferenceBased {
    private static final Logger LOG = Logger.getLogger(Weirdness.class.getName());
    public static final String SUFFIX_REF ="_REF";
    public static final String SUFFIX_WORD ="_WORD";

    public Weirdness(){
        super(true);
    }

    public Weirdness(boolean matchOOM){
        super(matchOOM);
    }

    @Override
    public List<JATETerm> execute(Set<String> candidates) throws JATEException {
        AbstractFeature feature1 = features.get(FrequencyTermBased.class.getName()+SUFFIX_WORD);
        validateFeature(feature1, FrequencyTermBased.class);
        FrequencyTermBased fFeatureWords = (FrequencyTermBased) feature1;

        AbstractFeature feature2 = features.get(FrequencyTermBased.class.getName()+ SUFFIX_REF);
        validateFeature(feature2, FrequencyTermBased.class);
        FrequencyTermBased fFeatureRef = (FrequencyTermBased) feature2;
        List<JATETerm> result = new ArrayList<>();
        double totalWordsInCorpus = fFeatureWords.getCorpusTotal();

        StringBuilder msg = new StringBuilder("Beginning computing TermEx values,");
        msg.append(", total terms=" + candidates.size());
        LOG.info(msg.toString());

        nullWordProbInReference = setNullWordProbInReference(fFeatureRef);
        double refScalar = matchOrdersOfMagnitude(fFeatureWords, fFeatureRef);

        for(String tString: candidates) {
            JATETerm term = new JATETerm(tString);

            String[] elements = tString.split(" ");
            double T = (double) elements.length;
            double SUMwi = 0.0;

            for (int i = 0; i < T; i++) {
                String wi = elements[i];
                double pc_wi = fFeatureRef.getTTFNorm(wi);
                if (pc_wi == 0)
                    pc_wi = nullWordProbInReference; //
                pc_wi*=refScalar;

                double v = (double) fFeatureWords.getTTF(wi) / totalWordsInCorpus / pc_wi;
                //SUMwi += Math.log((double) gFeatureStore.getWordFreq(wi) / (double) gFeatureStore.getTotalCorpusWordFreq() / gFeatureStore.getRefWordFreqNorm(wi));
                SUMwi += Math.log(v);
            }

            double TD = SUMwi / T;
            term.setScore(TD);

            result.add(term);
        }
        Collections.sort(result);
        LOG.info("Complete");
        return result;
    }

}
