package uk.ac.shef.dcs.jate.v2.algorithm;

import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.feature.AbstractFeature;
import uk.ac.shef.dcs.jate.v2.model.JATETerm;
import uk.ac.shef.dcs.jate.v2.model.TermInfoType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public abstract class Algorithm {

    protected Map<String, AbstractFeature> features = new HashMap<>();
    protected List<TermInfoCollector> termInfoCollectors = new ArrayList<>();

    public void registerFeature(String featureClassName, AbstractFeature feature){
        features.put(featureClassName, feature);
    }

    public void registerTermInfoCollector(TermInfoCollector tiCollector){
        termInfoCollectors.add(tiCollector);
    }

    public abstract List<JATETerm> execute() throws JATEException;
}
