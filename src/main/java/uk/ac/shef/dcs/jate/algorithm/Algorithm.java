package uk.ac.shef.dcs.jate.algorithm;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.feature.AbstractFeature;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.util.*;

public abstract class Algorithm {

    protected Map<String, AbstractFeature> features = new HashMap<>();

    public void registerFeature(String featureClassName, AbstractFeature feature){
        features.put(featureClassName, feature);
    }

    public abstract List<JATETerm> execute(Collection<String> candidates) throws JATEException;

    protected void validateFeature(AbstractFeature feature, Class<? extends AbstractFeature> type) throws JATEException {
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
