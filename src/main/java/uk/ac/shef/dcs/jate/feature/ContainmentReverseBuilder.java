package uk.ac.shef.dcs.jate.feature;

import org.apache.log4j.Logger;
import org.apache.solr.search.SolrIndexSearcher;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by zqz on 24/05/17.
 */
public class ContainmentReverseBuilder extends AbstractFeatureBuilder{

    private static final long serialVersionUID = -1208424775291405913L;
    private static final Logger LOG = Logger.getLogger(ContainmentReverseBuilder.class.getName());
    private Containment fContainment;

    public ContainmentReverseBuilder(SolrIndexSearcher solrIndexSearcher, JATEProperties properties,
                                     Containment fContainment) {
        super(solrIndexSearcher, properties);
        this.fContainment=fContainment;
    }

    @Override
    public AbstractFeature build() throws JATEException {
        Map<String, Set<String>> term2ParentMap = fContainment.getTerm2ParentsMap();

        Map<String, Set<String>> reverseMap = new HashMap<>();
        LOG.info("Total terms to process: "+term2ParentMap.size());
        int count=0;
        for(Map.Entry<String, Set<String>> e: term2ParentMap.entrySet()){
            String term=e.getKey();
            Set<String> parents = e.getValue();
            count++;
            if(count%5000==0)
                LOG.info("\tprocessed="+count);

            for(String p: parents){
                Set<String> children = reverseMap.get(p);
                if(children==null)
                    children=new HashSet<>();
                children.add(term);
                reverseMap.put(p, children);
            }
        }

        Containment rc = new Containment();
        rc.getTerm2ParentsMap().putAll(reverseMap);
        return rc;
    }
}