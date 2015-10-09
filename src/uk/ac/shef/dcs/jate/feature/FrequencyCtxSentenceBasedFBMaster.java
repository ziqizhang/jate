package uk.ac.shef.dcs.jate.feature;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.search.SolrIndexSearcher;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.util.SolrUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;

/**
 * Created by zqz on 21/09/2015.
 */
public class FrequencyCtxSentenceBasedFBMaster extends AbstractFeatureBuilder {
    private String sentenceTargetField;

    private static final Logger LOG = Logger.getLogger(FrequencyCtxSentenceBasedFBMaster.class.getName());

    public FrequencyCtxSentenceBasedFBMaster(SolrIndexSearcher solrIndexSearcher, JATEProperties properties,
                                             String termTargetField, String sentenceTargetField) {
        super(solrIndexSearcher, properties);
        this.sentenceTargetField = sentenceTargetField;
    }

    @Override
    public AbstractFeature build() throws JATEException {
        FrequencyCtxBased feature;
        List<Integer> allDocs = new ArrayList<>();
        for (int i = 0; i < solrIndexSearcher.maxDoc(); i++) {
            allDocs.add(i);
        }

        try {
            Terms terms = SolrUtil.getTermVector(properties.getSolrFieldnameJATECTerms(), solrIndexSearcher);

            TermsEnum termsEnum = terms.iterator();
            Set<String> allCandidates = new HashSet<>();

            while (termsEnum.next() != null) {
                BytesRef t = termsEnum.term();
                if (t.length == 0)
                    continue;
                allCandidates.add(t.utf8ToString());
            }

            //start workers
            int cores = properties.getCandidateScoringRankingMaxCPUCores();
            cores = cores == 0 ? 1 : cores;
            int maxPerThread = allDocs.size()/cores;

            FrequencyCtxSentenceBasedFBWorker worker = new
                    FrequencyCtxSentenceBasedFBWorker(properties, allDocs, allCandidates,
                    solrIndexSearcher, maxPerThread,
                    sentenceTargetField);
            StringBuilder sb = new StringBuilder("Building features using cpu cores=");
            sb.append(cores).append(", total docs=").append(allDocs.size()).append(", max per worker=")
                    .append(maxPerThread);
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
            throw new JATEException(sb.toString());
        }
        return feature;
    }
}
