package uk.ac.shef.dcs.jate.feature;

import uk.ac.shef.dcs.jate.JATERecursiveTaskWorker;

import java.util.List;
import java.util.Map;
import java.util.Set;


class ChiSquareFrequentTermsFBWorker extends JATERecursiveTaskWorker<String, Integer> {

    private static final long serialVersionUID = -1208424489000405973L;

    private final Map<ContextWindow, Integer> ctx2TTF;
    private final Map<String, Set<ContextWindow>> term2Ctx;
    private final ChiSquareFrequentTerms feature;
    private final int ttfInCorpus;

    ChiSquareFrequentTermsFBWorker(List<String> tasks, int maxTasksPerWorker,
                                          Map<ContextWindow, Integer> ctx2TTF,
                                          Map<String, Set<ContextWindow>> term2Ctx,
                                          ChiSquareFrequentTerms feature,
                                          int ttfInCorpus) {
        super(tasks, maxTasksPerWorker);
        this.ctx2TTF = ctx2TTF;
        this.term2Ctx = term2Ctx;
        this.feature = feature;
        this.ttfInCorpus = ttfInCorpus;
    }

    @Override
    protected JATERecursiveTaskWorker<String, Integer> createInstance(List<String> splitTasks) {
        return new ChiSquareFrequentTermsFBWorker(splitTasks, maxTasksPerThread, ctx2TTF,
                term2Ctx, feature, ttfInCorpus
        );
    }

    @Override
    protected Integer mergeResult(List<JATERecursiveTaskWorker<String, Integer>> jateRecursiveTaskWorkers) {
        int totalSuccess = 0;
        for (JATERecursiveTaskWorker<String, Integer> worker : jateRecursiveTaskWorkers) {
            int rs = worker.join();
            totalSuccess += rs;
        }
        return totalSuccess;
    }

    @Override
    protected Integer computeSingleWorker(List<String> tasks) {

        int count=0;
        for (String refTerm : tasks) {
            Set<ContextWindow> ctx_g = term2Ctx.get(refTerm);
            if (ctx_g == null) {
                continue;//this is possible if during co-occurrence computing this term is skipped
                //because it did not satisfy minimum thresholds
            }
            int g_w = 0;
            for (ContextWindow ctx : ctx_g)
                g_w += ctx2TTF.get(ctx);

            double p_g = (double) g_w / ttfInCorpus;
            feature.add(refTerm, p_g);
            count++;
        }
        return count;
    }
}
