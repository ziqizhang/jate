package uk.ac.shef.dcs.jate.algorithm;

import org.apache.log4j.Logger;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.feature.AbstractFeature;
import uk.ac.shef.dcs.jate.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Peñas, A., Verdejo, F., Gonzalo, J., et al.: Corpus-based terminology extraction applied
 to information access. In: Proceedings of Corpus Linguistics, vol. 2001. Citeseer (2001)

 */
public class Relevance extends ReferenceBased{
    private static final Logger LOG = Logger.getLogger(Relevance.class.getName());
    public static final String SUFFIX_REF ="_REF";
    public static final String SUFFIX_WORD ="_WORD";

    public Relevance(){
        super(true);
    }

    public Relevance(boolean matchOOM){
        super(matchOOM);
    }

    @Override
    public List<JATETerm> execute(Collection<String> candidates) throws JATEException {
        AbstractFeature feature1 = features.get(FrequencyTermBased.class.getName()+SUFFIX_WORD);
        validateFeature(feature1, FrequencyTermBased.class);
        FrequencyTermBased fFeatureWords = (FrequencyTermBased) feature1;

        AbstractFeature feature2 = features.get(FrequencyTermBased.class.getName()+ SUFFIX_REF);
        validateFeature(feature2, FrequencyTermBased.class);
        FrequencyTermBased fFeatureRef = (FrequencyTermBased) feature2;
        List<JATETerm> result = new ArrayList<>();
        double totalWordsInCorpus = fFeatureWords.getCorpusTotal();

        StringBuilder msg = new StringBuilder("Beginning computing Relevance values,");
        msg.append(", total terms=" + candidates.size());
        LOG.info(msg.toString());

        nullWordProbInReference = setNullWordProbInReference(fFeatureRef);
        double refScalar = matchOrdersOfMagnitude(fFeatureWords, fFeatureRef);

        for(String tString: candidates) {
            JATETerm term = new JATETerm(tString);

            String[] elements = tString.split(" ");
            //v1
            /*double T = (double) elements.length;
            double SUMwi = 0.0;

            for (int i = 0; i < T; i++) {
                String wi = elements[i];
                double pc_wi = fFeatureRef.getTTFNorm(wi);
                if (pc_wi == 0)
                    pc_wi = nullWordProbInReference; //
                if(matchOOM)
                    pc_wi*=refScalar;

                int freq=fFeatureWords.getTTF(wi);
                if(freq==0)
                    continue;//composing words can be stopwords and no frequency will be recorded

                double inner_part=(double) freq / totalWordsInCorpus
                        * fFeatureWords.getTermFrequencyInDocument(wi).size()/pc_wi;
                double log_part = Math.log(inner_part+2)/Math.log(2);
                double v = 1.0-(1/log_part);
                //SUMwi += Math.log((double) gFeatureStore.getWordFreq(wi) / (double) gFeatureStore.getTotalCorpusWordFreq() / gFeatureStore.getRefWordFreqNorm(wi));
                SUMwi += v;
            }

            double TD = SUMwi / T;
            term.setScore(TD);*/

            //v2
            double T = (double) elements.length;
            double SUMwi = 0.0;

            for (int i = 0; i < T; i++) {
                String wi = elements[i];
                double pc_wi = fFeatureRef.getTTFNorm(wi);
                if (pc_wi == 0)
                    pc_wi = nullWordProbInReference; //
                if(matchOOM)
                    pc_wi*=refScalar;

                int freq=fFeatureWords.getTTF(wi);
                if(freq==0)
                    continue;//composing words can be stopwords and no frequency will be recorded

                double inner_part=(double) freq / totalWordsInCorpus
                        * fFeatureWords.getTermFrequencyInDocument(wi).size()/pc_wi;
                //SUMwi += Math.log((double) gFeatureStore.getWordFreq(wi) / (double) gFeatureStore.getTotalCorpusWordFreq() / gFeatureStore.getRefWordFreqNorm(wi));
                SUMwi += inner_part;
            }

            double TD = SUMwi / T;
            double log_part = Math.log(TD+2)/Math.log(2);
            double v = 1.0-(1/log_part);

            //v3
            /*double T = (double) elements.length;
            double SUMwi = 0.0;

            double ntf_t=0.0, df_t=0.0, ntf_r=0.0;
            for (int i = 0; i < T; i++) {
                String wi = elements[i];
                double pc_wi = fFeatureRef.getTTFNorm(wi);
                if (pc_wi == 0)
                    pc_wi = nullWordProbInReference; //
                if(matchOOM)
                    pc_wi*=refScalar;

                int freq=fFeatureWords.getTTF(wi);
                if(freq==0)
                    continue;//composing words can be stopwords and no frequency will be recorded

                ntf_t+= freq / totalWordsInCorpus;
                ntf_r+=pc_wi;
                df_t+= fFeatureWords.getTermFrequencyInDocument(wi).size();
            }

            if(ntf_t==0){
                term.setScore(0);
                continue;
            }

            double TD =ntf_t*df_t/ntf_r;
            double log_part = Math.log(TD+2)/Math.log(2);
            double v = 1.0-(1/log_part);
            term.setScore(v);*/

            term.setScore(v);
            result.add(term);
        }
        Collections.sort(result);
        LOG.info("Complete");
        return result;
    }
}
