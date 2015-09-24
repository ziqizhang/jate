package uk.ac.shef.dcs.jate.v2.algorithm;

import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.feature.AbstractFeature;
import uk.ac.shef.dcs.jate.v2.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.v2.model.JATETerm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public abstract class Algorithm {

    protected Map<String, AbstractFeature> features = new HashMap<>();
    protected TermInfoCollector termInfoCollector;

    public void registerFeature(String featureClassName, AbstractFeature feature){
        features.put(featureClassName, feature);
    }

    public void setTermInfoCollector(TermInfoCollector tiCollector){
        this.termInfoCollector=tiCollector;
    }

    public abstract List<JATETerm> execute(Set<String> candidates) throws JATEException;

    protected void validateFeature(AbstractFeature feature, Class type) throws JATEException {
        if (feature == null || !(feature.getClass().equals(type))) {
            StringBuilder sb = new StringBuilder();
            sb.append("requires feature type:").append(type).append(",")
                    .append(" provided:");
            if (feature == null)
                sb.append("null");
            else
                sb.append(feature.getClass().getName());
            throw new JATEException(sb.toString());
        }
    }
}
