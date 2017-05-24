package uk.ac.shef.dcs.jate.feature;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class Containment extends AbstractFeature {
    private Map<String, Set<String>> term2Parents = new ConcurrentHashMap<>();

    public void add(String term, String parentTerm){
        Set<String> parentTerms = term2Parents.get(term);
        if(parentTerms==null)
            parentTerms=new HashSet<>();
        parentTerms.add(parentTerm);
        term2Parents.put(term, parentTerms);
    }

    public Set<String> getTermParents(String term){
        Set<String> parents = term2Parents.get(term);
        if(parents==null)
            parents=new HashSet<>();
        return parents;
    }

    Map<String, Set<String>> getTerm2ParentsMap(){
        return term2Parents;
    }
}
