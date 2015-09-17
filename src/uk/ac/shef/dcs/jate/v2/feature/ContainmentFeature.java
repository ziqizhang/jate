package uk.ac.shef.dcs.jate.v2.feature;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zqz on 17/09/2015.
 */
public class ContainmentFeature extends AbstractFeature {
    private Map<String, Set<String>> term2Parents = new ConcurrentHashMap<>();

    public void add(String term, String parentTerm){

    }
}
