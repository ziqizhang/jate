package uk.ac.shef.dcs.jate.feature;

import org.apache.lucene.index.IndexReader;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;

/**
 * Created by zqz on 16/09/2015.
 */
public abstract class AbstractFeatureBuilder {

    protected IndexReader indexReader;
    protected JATEProperties properties;

    public AbstractFeatureBuilder(IndexReader index, JATEProperties properties){
        this.indexReader=index;
        this.properties=properties;
    }

    public abstract AbstractFeature build() throws JATEException;

}
