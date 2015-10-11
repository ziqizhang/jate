package uk.ac.shef.dcs.jate.feature;

import org.apache.solr.search.SolrIndexSearcher;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;

/**
 *
 */
public abstract class AbstractFeatureBuilder {

    protected SolrIndexSearcher solrIndexSearcher;

    protected JATEProperties properties;

    public AbstractFeatureBuilder(SolrIndexSearcher solrIndexSearcher, JATEProperties properties){
        this.solrIndexSearcher=solrIndexSearcher;
        this.properties=properties;
    }

    public abstract AbstractFeature build() throws JATEException;

}
