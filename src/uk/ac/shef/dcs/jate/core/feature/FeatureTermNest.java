package uk.ac.shef.dcs.jate.core.feature;


import uk.ac.shef.dcs.jate.core.feature.indexer.GlobalIndex;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A feature store that contains information of nested terms. E.g., "hedgehog" is a
 * nested term in "European hedgehog". See details in K. Frantz et al 2000. This contains following information:
 * <br>
 * <br>- total number of occurrences of all terms found in the corpus, which is the sum of occurrences of each term
 * <br>- number of occurrences of each term found in the corpus
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */


public class FeatureTermNest extends AbstractFeature {

    private Map<Integer, Set<Integer>> _termNested = new ConcurrentHashMap<Integer, Set<Integer>>();

    protected FeatureTermNest(GlobalIndex index) {
        _index = index;
    }

    /**
     * @param nested
     * @return the ids of terms in which the provided term is nested
     */
    public Set<Integer> getNestIdsOf(String nested) {
        int termId = _index.retrieveTermCanonical(nested);
        if (termId == -1) {
            //System.err.println("Term (" + nested + ") has not been indexed! Ignored.");
            return new HashSet<Integer>();
        } else {
            return getNestIdsOf(termId);
        }
    }

    /**
     * @param nested
     * @return the ids of terms in which the provided term with id "nested" is nested
     */
    public Set<Integer> getNestIdsOf(int nested) {
        Set<Integer> res = _termNested.get(nested);
        return res == null ? new HashSet<Integer>() : res;
    }

    /**
     * Index a term "nested" as nested in a term "nest"
     *
     * @param nested
     * @param nest
     */
    public void termNestIn(String nested, String nest) {
        int termId = _index.retrieveTermCanonical(nested);
        int nestId = _index.retrieveTermCanonical(nest);
        if (termId == -1) {
            //System.err.println("Term (" + nested + ") has not been indexed! Ignored.");
        } else if (nestId == -1) {
            //System.err.println("Term (" + nest + ") has not been indexed! Ignored.");
        } else {
            termNestIn(termId, nestId);
        }
    }

    /**
     * Index a term with id "nested" as nested in a term with id "nest"
     *
     * @param nested
     * @param nest
     */
    public void termNestIn(int nested, int nest) {
        Set<Integer> res = _termNested.get(nested);
        res = res == null ? new HashSet<Integer>() : res;
        res.add(nest);
        _termNested.put(nested, res);
    }


}
