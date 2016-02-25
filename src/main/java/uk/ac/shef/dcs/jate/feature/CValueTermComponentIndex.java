package uk.ac.shef.dcs.jate.feature;

import org.apache.solr.common.util.Pair;

import java.util.*;

/**
 * Specific feature to be used by CValue for efficient computation
 * <p/>
 * The index is a map from uni-grams to a list of pairs. Each pair contains a candidate term containing that unigram and the length of the term in terms of
 * number of tokens (uni-grams) in that term
 */
public class CValueTermComponentIndex extends AbstractFeature {
    private Map<String, List<Pair<String, Integer>>> index = new HashMap<>();

    public synchronized void add(String unigram, String term, int numTokens) {
        List<Pair<String, Integer>> contained = index.get(unigram);
        if (contained == null)
            contained = new ArrayList<>();
        contained.add(new Pair<>(term, numTokens));
        index.put(unigram, contained);
    }

    public List<Pair<String, Integer>> getSorted(String unigram) {
        List<Pair<String, Integer>> sorted = new ArrayList<>();
        List<Pair<String, Integer>> values=index.get(unigram);
        if (values!=null)
            sorted.addAll(values);

        Collections.sort(sorted, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));
        return sorted;
    }
}
