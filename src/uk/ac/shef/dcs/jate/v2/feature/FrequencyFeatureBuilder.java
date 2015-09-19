package uk.ac.shef.dcs.jate.v2.feature;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.index.*;
import org.apache.lucene.util.BytesRef;
import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.JATEProperties;
import uk.ac.shef.dcs.jate.v2.indexing.SolrParallelIndexingWorker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;

/**
 * Created by zqz on 17/09/2015.
 */
public class FrequencyFeatureBuilder extends AbstractFeatureBuilder {

    private static final Logger LOG=Logger.getLogger(FrequencyFeatureBuilder.class.getName());

    protected int apply2Terms=0; //1 means no= words
    public FrequencyFeatureBuilder(IndexReader index, JATEProperties properties,
                                   int apply2Terms) {
        super(index, properties);
        this.apply2Terms=apply2Terms;
    }

    @Override
    public AbstractFeature build() throws JATEException {
        FrequencyFeature feature = new FrequencyFeature();
        String targetField=apply2Terms==0?properties.getSolrFieldnameJATETermsAll(): properties.getSolrFieldnameJATEWordsAll();
        try {
            Fields fields = MultiFields.getFields(indexReader);
            boolean foundJATETextField = false;
            for (String field : fields) {
                foundJATETextField = true;
                if (field.equals(targetField)) {
                    List<BytesRef> allLuceneTerms = new ArrayList<>();
                    Terms terms = fields.terms(field);
                    TermsEnum termsEnum = terms.iterator();
                    while (termsEnum.next() != null) {
                        BytesRef t = termsEnum.term();
                        allLuceneTerms.add(t);
                    }
                    //start workers
                    int cores = Runtime.getRuntime().availableProcessors();
                    cores = (int) (cores * properties.getFeatureBuilderMaxCPUsage());
                    cores = cores == 0 ? 1 : cores;
                    FrequencyFeatureBuilderWorker worker = new
                            FrequencyFeatureBuilderWorker(properties, allLuceneTerms,
                            indexReader, feature, properties.getFeatureBuilderMaxTermsPerWorker(),targetField);
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
