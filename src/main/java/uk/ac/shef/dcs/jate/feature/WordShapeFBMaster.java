package uk.ac.shef.dcs.jate.feature;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.index.Terms;
import org.apache.solr.search.SolrIndexSearcher;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.util.SolrUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

/**
 * Created by zqz on 11/01/17.
 */
public class WordShapeFBMaster extends AbstractFeatureBuilder{
    private static final Logger LOG = Logger.getLogger(WordShapeFBMaster.class.getName());

    private int termOrWord; //0 means term; 1 means word
    public final static Integer FEATURE_TYPE_TERM = 0;
    public final static Integer DEFAULT_CPU_CORES = 1;
    private Set<String> gazetteer;

    public WordShapeFBMaster(SolrIndexSearcher solrIndexSearcher, JATEProperties properties,
                             int termOrWord, Set<String> gazetteer) {
        super(solrIndexSearcher, properties);
        this.termOrWord = termOrWord;
        this.gazetteer=gazetteer;
    }

    @Override
    public AbstractFeature build() throws JATEException {
        WordShapeFeature feature = new WordShapeFeature();

        try {
            Terms ngramInfo = SolrUtil.
                    getTermVector(properties.getSolrFieldNameJATENGramInfo(), solrIndexSearcher);
            Set<String> all;
            if (termOrWord == FEATURE_TYPE_TERM)
                all = getUniqueTerms();
            else
                all = getUniqueWords();
            //start workers
            int cores = properties.getMaxCPUCores();
            cores = (cores == 0) ? DEFAULT_CPU_CORES : cores;
            int maxPerThread = all.size() / cores;
            if (maxPerThread == 0)
                maxPerThread = 50;

            StringBuilder sb = new StringBuilder("Building features using cpu cores=");
            sb.append(cores).append(", total=").append(all.size()).append(", max per worker=")
                    .append(maxPerThread);
            LOG.info(sb.toString());
            WordShapeFBWorker worker = new
                    WordShapeFBWorker(properties, new ArrayList<>(all),
                    solrIndexSearcher, feature, maxPerThread,
                    ngramInfo, gazetteer);
            ForkJoinPool forkJoinPool = new ForkJoinPool(cores);
            int[] total = forkJoinPool.invoke(worker);
            sb = new StringBuilder("Complete building features. Total=");
            sb.append(total[1]).append(" success=").append(total[0]);
            LOG.info(sb.toString());


        } catch (IOException ioe) {
            StringBuilder sb = new StringBuilder("Failed to build features!");
            sb.append("\n").append(ExceptionUtils.getFullStackTrace(ioe));
            LOG.error(sb.toString());
            throw new JATEException(sb.toString());
        }
        return feature;
    }
}
