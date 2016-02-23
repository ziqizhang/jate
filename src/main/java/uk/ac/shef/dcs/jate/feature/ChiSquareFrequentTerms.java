package uk.ac.shef.dcs.jate.feature;

import java.util.HashMap;
import java.util.Map;

/**
 *Specifically used by ChiSquare. This feature keeps expected probability (p_g) of frequent terms. See page 3, 1st bullet point on the left
 */
public class ChiSquareFrequentTerms extends AbstractFeature {
    private Map<String, Double> expProb;
    private double sumExpProb=0.0;
    private double maxExpProb=0.0;

    public ChiSquareFrequentTerms(){
        expProb =new HashMap<>();
    }

    public synchronized void add(String freqTerm, double prob){
        expProb.put(freqTerm, prob);
        sumExpProb+=prob;
        if(prob>maxExpProb)
            maxExpProb=prob;
    }

    public double get(String freqTerm){
        Double d = expProb.get(freqTerm);
        if(d==null) {
            System.err.println("no such ref term:"+freqTerm);
            return 0.0;
        }
        return d;
    }

    public double getSumExpProb(){
        return sumExpProb;
    }

    public double getMaxExpProb(){return maxExpProb;}

}
