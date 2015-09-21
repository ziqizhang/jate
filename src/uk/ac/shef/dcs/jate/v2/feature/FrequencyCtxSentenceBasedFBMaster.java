package uk.ac.shef.dcs.jate.v2.feature;

import org.apache.lucene.index.IndexReader;
import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.JATEProperties;

/**
 * Created by zqz on 21/09/2015.
 */
public class FrequencyCtxSentenceBasedFBMaster extends AbstractFeatureBuilder {
    public FrequencyCtxSentenceBasedFBMaster(IndexReader index, JATEProperties properties) {
        super(index, properties);
    }

    @Override
    public AbstractFeature build() throws JATEException {
        return null;
    }
}
