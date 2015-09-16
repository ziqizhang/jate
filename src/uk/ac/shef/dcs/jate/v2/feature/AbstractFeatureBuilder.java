package uk.ac.shef.dcs.jate.v2.feature;

import org.apache.solr.client.solrj.SolrClient;

/**
 * Created by zqz on 16/09/2015.
 */
public class AbstractFeatureBuilder {

    protected SolrClient solrBackend;

    public void setSolrBackend(SolrClient solr){
        this.solrBackend=solr;
    }
}
