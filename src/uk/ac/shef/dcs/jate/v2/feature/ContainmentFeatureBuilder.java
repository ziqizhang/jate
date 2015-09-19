package uk.ac.shef.dcs.jate.v2.feature;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.index.*;
import org.apache.lucene.util.BytesRef;
import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.JATEProperties;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;

/**
 * Created by zqz on 17/09/2015.
 */
public class ContainmentFeatureBuilder extends AbstractFeatureBuilder {

    private static final Logger LOG = Logger.getLogger(ContainmentFeatureBuilder.class.getName());

    public ContainmentFeatureBuilder(IndexReader index, JATEProperties properties) {
        super(index, properties);
    }

    @Override
    public AbstractFeature build() throws JATEException {
        ContainmentFeature feature = new ContainmentFeature();
        try {
            Fields fields = MultiFields.getFields(indexReader);
            boolean foundJATETextField = false;
            for (String field : fields) {
                foundJATETextField = true;
                if (field.equals(properties.getSolrFieldnameJATETermsAll())) {
                    Map<String, Integer> term2NumTokens = new HashMap<>();
                    Terms terms = fields.terms(field);
                    TermsEnum termsEnum = terms.iterator();
                    while (termsEnum.next() != null) {
                        String t = termsEnum.term().utf8ToString();
                        int tokens = t.split("\\s+").length;
                        term2NumTokens.put(t, tokens);
                    }
                    //start workers
                    int cores = Runtime.getRuntime().availableProcessors();
                    cores = (int) (cores * properties.getFeatureBuilderMaxCPUsage());
                    cores = cores == 0 ? 1 : cores;
                    List<String> termSorted = new ArrayList<>(term2NumTokens.keySet());
                    Collections.sort(termSorted);
                    ContainmentFeatureBuilderWorker worker = new
                            ContainmentFeatureBuilderWorker(properties, termSorted,
                            term2NumTokens,
                            feature, properties.getFeatureBuilderMaxTermsPerWorker());
                    ForkJoinPool forkJoinPool = new ForkJoinPool(cores);
                    int[] total = forkJoinPool.invoke(worker);
                    StringBuilder sb = new StringBuilder("Complete building features. Total=");
                    sb.append(total[1]).append(" success=").append(total[0]);
                    LOG.info(sb.toString());
                }
            }

            if(!foundJATETextField){
                throw new JATEException("Cannot find expected field: "+properties.getSolrFieldnameJATETermsAll());
            }
        }catch (IOException ioe){
            StringBuilder sb = new StringBuilder("Failed to build features!");
            sb.append("\n").append(ExceptionUtils.getFullStackTrace(ioe));
            LOG.severe(sb.toString());
        }

        return feature;
    }
}
