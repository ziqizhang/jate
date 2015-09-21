package uk.ac.shef.dcs.jate.v2.feature;

import org.apache.lucene.index.IndexReader;
import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.JATEProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;

/**
 * Created by zqz on 21/09/2015.
 */
public class FrequencyCtxSentenceBasedFBMaster extends AbstractFeatureBuilder {
    private JATEProperties properties;
    private IndexReader indexReader;
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
        List<Integer> allDocs = new ArrayList<>();
        for (int i = 0; i < indexReader.maxDoc(); i++) {
            allDocs.add(i);
        }

        //start workers
        int cores = Runtime.getRuntime().availableProcessors();
        cores = (int) (cores * properties.getFeatureBuilderMaxCPUsage());
        cores = cores == 0 ? 1 : cores;
        FrequencyCtxSentenceBasedFBWorker worker = new
                FrequencyCtxSentenceBasedFBWorker(properties, allDocs,
                indexReader, properties.getFeatureBuilderMaxTermsPerWorker(), termTargetField,
                sentenceTargetField);
        ForkJoinPool forkJoinPool = new ForkJoinPool(cores);
        FrequencyCtxBased feature = forkJoinPool.invoke(worker);
        StringBuilder sb = new StringBuilder("Complete building features. Total=");
        sb.append(feature.getMapCtx2TTF().size());
        LOG.info(sb.toString());

        return feature;
    }
}
