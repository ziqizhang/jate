package uk.ac.shef.dcs.jate.v2.feature;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.JATEProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;

/**
 * Created by zqz on 21/09/2015.
 */
public class FrequencyCtxSentenceBasedFBMaster extends AbstractFeatureBuilder {
    private String termTargetField;
    private String sentenceTargetField;

    private static final Logger LOG = Logger.getLogger(FrequencyCtxSentenceBasedFBMaster.class.getName());

    public FrequencyCtxSentenceBasedFBMaster(IndexReader index, JATEProperties properties,
                                             String termTargetField, String sentenceTargetField) {
        super(index, properties);
        this.termTargetField = termTargetField;
        this.sentenceTargetField = sentenceTargetField;
    }

    @Override
    public AbstractFeature build() throws JATEException {
        FrequencyCtxBased feature = new FrequencyCtxBased();
        List<Integer> allDocs = new ArrayList<>();
        for (int i = 0; i < indexReader.maxDoc(); i++) {
            allDocs.add(i);
        }

        //start workers
        int cores = Runtime.getRuntime().availableProcessors();
        cores = (int) (cores * properties.getFeatureBuilderMaxCPUsage());
        cores = cores == 0 ? 1 : cores;
        try {
            Fields fields = MultiFields.getFields(indexReader);
            Terms info = fields.terms(properties.getSolrFieldnameJATETermInfo());
            if (info == null)
                throw new JATEException("Cannot find expected field: " + properties.getSolrFieldnameJATETermInfo());

            FrequencyCtxSentenceBasedFBWorker worker = new
                    FrequencyCtxSentenceBasedFBWorker(properties, allDocs,
                    indexReader, properties.getFeatureBuilderMaxDocsPerWorker(), termTargetField,
                    sentenceTargetField, info.iterator());
            StringBuilder sb = new StringBuilder("Building features using cpu cores=");
            sb.append(cores).append(", total docs=").append(allDocs.size()).append(", max per worker=")
                    .append(properties.getFeatureBuilderMaxDocsPerWorker());
            LOG.info(sb.toString());
            ForkJoinPool forkJoinPool = new ForkJoinPool(cores);
            feature = forkJoinPool.invoke(worker);
            sb = new StringBuilder("Complete building features. Total sentence ctx=");
            sb.append(feature.getMapCtx2TTF().size());
            LOG.info(sb.toString());
        } catch (IOException ioe) {
            StringBuilder sb = new StringBuilder("Failed to build features!");
            sb.append("\n").append(ExceptionUtils.getFullStackTrace(ioe));
            LOG.severe(sb.toString());
        }
        return feature;
    }
}
