package uk.ac.shef.dcs.jate.feature;

import org.apache.log4j.Logger;
import org.apache.solr.search.SolrIndexSearcher;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;

import java.util.*;

/**
 * Created by - on 08/10/2015.
 */
public class FrequencyCtxBasedCopier extends AbstractFeatureBuilder {

    private static final Logger LOG = Logger.getLogger(FrequencyCtxBasedCopier.class.getName());
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
        int pos = (int)(frequencies.size()*topFraction);
        this.frequencyThreshold=frequencies.get(frequencies.size()-pos);
    }

    @Override
    public AbstractFeature build() throws JATEException {
        FrequencyCtxBased result = new FrequencyCtxBased();

        LOG.info("Copying features using 1 core, filtering "+frequencyFeature.getMapTerm2TTF().size()+" terms.");
        Set<String> filteredTerms = new HashSet<>();

        int count=0;
        for(Map.Entry<String, Integer> en: frequencyFeature.getMapTerm2TTF().entrySet()){
            count++;
            if(count%100000==0)
                LOG.debug(count+"/"+frequencyFeature.getMapTerm2TTF().size());
            if(en.getValue()<frequencyThreshold)
                continue;
            filteredTerms.add(en.getKey());
        }

        count=0;
        int countContext=0;
        LOG.info("Complete filtering, copying for "+filteredTerms.size()+" terms.");
        for(String ft: filteredTerms){
            Set<ContextWindow> ctxx = source.getContexts(ft);
            if(ctxx==null)
                continue;//this is possible because candidate term may be incorrectly generated across context (e.g., sentence) boundaries
            countContext+=ctxx.size();
            for(ContextWindow ctx: ctxx){
                int tfInCtx=source.getMapCtx2TFIC().get(ctx).get(ft);
                result.increment(ctx,ft, tfInCtx);
                result.increment(ctx,tfInCtx);
            }

            count++;
            if(count%100000==0) {
                LOG.debug(count + "/" + filteredTerms.size() + ", ctxx=" + countContext);
                countContext=0;
            }
        }
        LOG.info("Complete copying features.");

        return result;
    }
}
