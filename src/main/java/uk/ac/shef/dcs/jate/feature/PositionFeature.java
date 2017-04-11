package uk.ac.shef.dcs.jate.feature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * which sentence/paragraph does the candidate appear in. How far away are they from the beginning, etc.
 * <p>
 * Distance is measured as either the # of paragraphs or sentences between the paragraph/sentence
 * in which a term is found, from the document title, which is always the first sentence or paragraph
 * in the document. So sentence id=0 or paragraph id=0 will have a 0 distance from the title of the document
 * <p>
 * distance can be absolute, or relative (in which case it is the ordinal number of the sent/par
 * divided by the total sentences/pararagraphs
 */
public class PositionFeature extends AbstractFeature{
    private Map<String, Integer> foundInDocTitles = new HashMap<>();
    //distances between the source paragraphs and the title paragraph
    private Map<String, List<Double>> parDistsFromTitle = new HashMap<>();

    //distances between the source sentence and the title paragraph
    private Map<String, List<Double>> sentDistsFromTitle = new HashMap<>();

    //distance between the source sentence and the first sentence in its containing paragraph
    private Map<String, List<Double>> sentDistsFromPar = new HashMap<>();

    public synchronized void incrementFoundInDocTitles(String mwe){
        Integer freq = foundInDocTitles.get(mwe);
        if(freq==null)
            freq=0;
        freq++;
        foundInDocTitles.put(mwe, freq);
    }
    public synchronized void addParDistFromTitle(String mwe, double dist){
        List<Double> dists = parDistsFromTitle.get(mwe);
        if(dists==null)
            dists=new ArrayList<>();
        dists.add(dist);
        parDistsFromTitle.put(mwe, dists);
    }
    public List<Double> getParDistFromTitle(String term) {
        return parDistsFromTitle.get(term);
    }

    public synchronized void addSentDistFromTitle(String mwe, double dist){
        List<Double> dists = sentDistsFromTitle.get(mwe);
        if(dists==null)
            dists=new ArrayList<>();
        dists.add(dist);
        sentDistsFromTitle.put(mwe, dists);
    }
    public List<Double> getSentDistFromTitle(String term) {
        return sentDistsFromTitle.get(term);
    }

    public synchronized void addSentDistFromPar(String mwe, double dist){
        List<Double> dists = sentDistsFromPar.get(mwe);
        if(dists==null)
            dists=new ArrayList<>();
        dists.add(dist);
        sentDistsFromPar.put(mwe, dists);
    }
    public List<Double> getSentDistFromPar(String term) {
        return sentDistsFromPar.get(term);
    }


    public int getFoundInDocTitles(String mwe){
        Integer f= foundInDocTitles.get(mwe);
        if(f==null)
            f= 0;
        return f;
    }
}
