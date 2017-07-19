package uk.ac.shef.dcs.jate.algorithm;

import org.apache.log4j.Logger;
import org.apache.solr.common.util.Pair;
import uk.ac.shef.dcs.jate.JATERecursiveTaskWorker;
import uk.ac.shef.dcs.jate.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.feature.TermComponentIndex;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by - on 25/02/2016.
 */
public class RAKEWorker extends JATERecursiveTaskWorker<String, List<JATETerm>> {

    private static final Logger LOG = Logger.getLogger(RAKEWorker.class.getName());
    private static final long serialVersionUID = 6429950650561513335L;
    protected FrequencyTermBased fFeatureWords;
    protected FrequencyTermBased fFeatureTerms;
    protected TermComponentIndex fTermCompIndex;

    public RAKEWorker(List<String> candidates, int maxTasksPerWorker,
                      FrequencyTermBased fFeature, FrequencyTermBased fFeatureTerms,
                      TermComponentIndex fTermCompIndex) {
        super(candidates, maxTasksPerWorker);
        this.fFeatureWords=fFeature;
        this.fFeatureTerms=fFeatureTerms;
        this.fTermCompIndex=fTermCompIndex;
    }

    @Override
    protected JATERecursiveTaskWorker<String, List<JATETerm>> createInstance(List<String> candidates) {
        return new RAKEWorker(candidates, maxTasksPerThread, fFeatureWords, fFeatureTerms,fTermCompIndex
        );
    }

    @Override
    protected List<JATETerm> mergeResult(List<JATERecursiveTaskWorker<String, List<JATETerm>>> jateRecursiveTaskWorkers) {
        List<JATETerm> result = new ArrayList<>();
        for(JATERecursiveTaskWorker<String, List<JATETerm>> worker: jateRecursiveTaskWorkers){
            result.addAll(worker.join());
        }

        return result;
    }

    @Override
    protected List<JATETerm> computeSingleWorker(List<String> candidates) {
        List<JATETerm> result = new ArrayList<>();

        int count=0;
        for (String tString : candidates) {
/*            if(tString.equals("receptors in human mononuclear leukocyte"))
                System.out.println();*/
            String[] elements = tString.split(" ");
            double score = 0;
            //a term's RAKE score is the sum of its elements
            for (String e : elements) {
                //now compute RAKE score of individual words
/*                if(e.equals("t"))
                    System.out.printf("here");*/

                //first, frequency
                int freq = fFeatureWords.getTTF(e);
                if(freq==0)    //composing word can be stop words that have been filtered
                    continue;

                //second, degree. Degree adds up frequency
                int degree = freq;

                //for the remaining part of degree, it depends on terms (parent term) that contain this element
                List<Pair<String, Integer>> parentTerms=fTermCompIndex.getSorted(e);
                for(Pair<String, Integer> pTerm: parentTerms){
                    String pTermStr = pTerm.first();
                    if(pTerm.second()==1) //we are only interested in multi-word expressions for computing degree
                        continue;

                    int pTF = fFeatureTerms.getTTF(pTermStr); //how many times this parent term appear in corpus

                    String[] pTermElements = pTermStr.split(" "); //components of this parent term
                    for(String ep: pTermElements){
                        if (ep.equals(e)) //discount the word element itself
                            continue;
                        //does stop words matter?
                        degree+=pTF;
                    }

                }

                double wScore = (double) degree / freq; //score of this element word
                score += wScore;
            }

            JATETerm term = new JATETerm(tString, score);
            result.add(term);
            count++;

            if(count%2000==0) {
                LOG.info("done ="+count+"/"+candidates.size());
            }
        }
        return result;
    }
}
