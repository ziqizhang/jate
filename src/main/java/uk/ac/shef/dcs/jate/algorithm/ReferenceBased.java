package uk.ac.shef.dcs.jate.algorithm;

import uk.ac.shef.dcs.jate.feature.FrequencyTermBased;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public abstract class ReferenceBased extends Algorithm{
    protected double nullWordProbInReference;
    
    //if a word is not found int the reference/general corpus,
    //what probability should we use? Note this value has inverse relation wrt prob(wi) where wi is a word in the
    //domain corpus. a value of 0.1 has the effect of raise prob(wi) by 10x.
    //so a term that contains a word which does not exist in reference/general corpus can get an extremely high score
    //by default we set it to the smallest word prob in the reference corpus
    protected boolean matchOOM=true;

    public ReferenceBased(boolean matchOOM){
        this.matchOOM=matchOOM;
    }

    static double matchOrdersOfMagnitude(FrequencyTermBased fFeatureWords, FrequencyTermBased fFeatureRef) {
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

    static double setNullWordProbInReference(FrequencyTermBased ref) {
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
