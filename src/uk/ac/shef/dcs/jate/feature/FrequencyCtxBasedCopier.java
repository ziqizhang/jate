package uk.ac.shef.dcs.jate.feature;

import org.apache.solr.search.SolrIndexSearcher;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;

import java.util.*;

/**
 * Created by - on 08/10/2015.
 */
public class FrequencyCtxBasedCopier extends AbstractFeatureBuilder {

    private FrequencyCtxBased source;
    private FrequencyTermBased frequencyFeature;
    private int frequencyThreshold;


    public FrequencyCtxBasedCopier(SolrIndexSearcher solrIndexSearcher, JATEProperties properties,
                                   FrequencyCtxBased source, FrequencyTermBased frequencyFeature,
                                   double topFraction) {
        super(solrIndexSearcher, properties);
        this.source=source;
        this.frequencyFeature =frequencyFeature;
        List<Integer> frequencies = new ArrayList<>(frequencyFeature.getMapTerm2TTF().values());
        Collections.sort(frequencies);
        int pos = (int)(frequencies.size()*(1-topFraction));
        this.frequencyThreshold=frequencies.get(pos);
    }

    @Override
    public AbstractFeature build() throws JATEException {
        FrequencyCtxBased result = new FrequencyCtxBased();

        Set<String> filteredTerms = new HashSet<>();
        for(Map.Entry<String, Integer> en: frequencyFeature.getMapTerm2TTF().entrySet()){
            if(en.getValue()<frequencyThreshold)
                continue;
            filteredTerms.add(en.getKey());
        }

        for(String ft: filteredTerms){
            Set<String> ctxx = source.getContextIds(ft);
            for(String ctxid: ctxx){
                int tfInCtx=source.getMapCtx2TFIC().get(ctxid).get(ft);
                result.increment(ctxid,ft, tfInCtx);

            }
        }


        return result;
    }
}
