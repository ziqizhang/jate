package uk.ac.shef.dcs.jate.feature;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.index.*;
import org.apache.lucene.util.BytesRef;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;

/**
 *
 */
public class FrequencyTermBasedFBMaster extends AbstractFeatureBuilder {

    private static final Logger LOG = Logger.getLogger(FrequencyTermBasedFBMaster.class.getName());

    protected int apply2Terms = 0; //1 means no, i.e, words

    public FrequencyTermBasedFBMaster(IndexReader index, JATEProperties properties,
                                      int apply2Terms) {
        super(index, properties);
        this.apply2Terms = apply2Terms;
    }

    @Override
    public AbstractFeature build() throws JATEException {
        FrequencyTermBased feature = new FrequencyTermBased();
        feature.setTotalDocs(indexReader.numDocs());
        String targetField = apply2Terms == 0 ? properties.getSolrFieldnameJATECTerms() : properties.getSolrFieldnameJATEWords();
        try {
            Fields fields = MultiFields.getFields(indexReader);

            Terms info = fields.terms(properties.getSolrFieldnameJATENGramInfo());
            if (info == null)
                throw new JATEException("Cannot find expected field: " + properties.getSolrFieldnameJATENGramInfo());
            Terms terms = fields.terms(targetField);
            if (terms == null)
                throw new JATEException("Cannot find expected field: " + targetField);

            TermsEnum termsEnum = terms.iterator();
            List<BytesRef> allLuceneTerms = new ArrayList<>();

            while (termsEnum.next() != null) {
                BytesRef t = termsEnum.term();
                if (t.length == 0)
                    continue;
                allLuceneTerms.add(BytesRef.deepCopyOf(t));
            }
            //start workers
            int cores = Runtime.getRuntime().availableProcessors();
            cores = (int) (cores * properties.getFeatureBuilderMaxCPUsage());
            cores = cores == 0 ? 1 : cores;
            StringBuilder sb = new StringBuilder("Building features using cpu cores=");
            sb.append(cores).append(", total=").append(allLuceneTerms.size()).append(", max per worker=")
                    .append(properties.getFeatureBuilderMaxTermsPerWorker());
            LOG.info(sb.toString());
            FrequencyTermBasedFBWorker worker = new
                    FrequencyTermBasedFBWorker(properties, allLuceneTerms,
                    indexReader, feature, properties.getFeatureBuilderMaxTermsPerWorker(),
                    targetField,
                    info.iterator());
            ForkJoinPool forkJoinPool = new ForkJoinPool(cores);
            int[] total = forkJoinPool.invoke(worker);
            sb = new StringBuilder("Complete building features. Total=");
            sb.append(total[1]).append(" success=").append(total[0]);
            LOG.info(sb.toString());


        } catch (IOException ioe) {
            StringBuilder sb = new StringBuilder("Failed to build features!");
            sb.append("\n").append(ExceptionUtils.getFullStackTrace(ioe));
            LOG.severe(sb.toString());
        }
        return feature;
    }
}
