package uk.ac.shef.dcs.jate.deprecated.core.feature;

import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.deprecated.JATEProperties;
import uk.ac.shef.dcs.jate.deprecated.core.feature.indexer.GlobalIndex;

import java.util.*;
import java.util.logging.Logger;

/**
 * Serves the same goal as FeatureBuilderTermNest. Uses multiple threads for counting
 */
public class FeatureBuilderTermNestMultiThread extends AbstractFeatureBuilder {
    private static Logger _logger = Logger.getLogger(FeatureBuilderTermNestMultiThread.class.getName());

    /**
     * Default constructor
     */
    public FeatureBuilderTermNestMultiThread() {
        super(null, null, null);
    }

    /**
     * Build an instance of FeatureTermNest from an instance of GlobalIndexMem
     *
     * @param index
     * @return
     * @throws uk.ac.shef.dcs.jate.v2.JATEException
     *
     */
    public FeatureTermNest build(GlobalIndex index) throws JATEException {
        FeatureTermNest _feature = new FeatureTermNest(index);
        if (index.getTermsCanonical().size() == 0 || index.getDocuments().size() == 0) throw new
                JATEException("No resource indexed!");

        _logger.info("About to build FeatureTermNest...");
        startMutiThreadChecking(index.getTermsCanonical(), _feature, JATEProperties.getInstance().getMultithreadCounterNumbers());
        return _feature;
    }

    private void startMutiThreadChecking(Set<String> allnps, FeatureTermNest feature, int totalThreads) {
        int size = allnps.size() / totalThreads + allnps.size() % totalThreads;
        Iterator<String> it = allnps.iterator();
        int count = 0;
        Set<String> seg = new HashSet<String>();
        List<TermNestCheckerSingleThread> allcheckers = new ArrayList<TermNestCheckerSingleThread>();

        while (it.hasNext()) {
            if (count >= size) {
                count = 0;
                allcheckers.add(new TermNestCheckerSingleThread(new HashSet<String>(seg), allnps, feature));
                seg.clear();
            }

            if (count < size) {
                seg.add(it.next());
                count++;
            }
        }
        if (seg.size() > 0)
            allcheckers.add(new TermNestCheckerSingleThread(new HashSet<String>(seg), allnps, feature));

        //start all
        for (TermNestCheckerSingleThread t : allcheckers)
            t.start();

        //until finished
        boolean finished = false;
        while (!finished) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }

            finished = true;
            for (TermNestCheckerSingleThread t : allcheckers) {
                if (!t.isFinished())
                    finished = false;
            }
        }
    }

    private class TermNestCheckerSingleThread extends Thread {
        private Set<String> nps;
        private Set<String> allnps;
        private FeatureTermNest feature;
        private boolean finished;

        public TermNestCheckerSingleThread(Set<String> nps, Set<String> allnps, FeatureTermNest feature) {
            this.nps = nps;
            this.allnps = allnps;
            this.feature = feature;
        }

        public void run() {
            check(nps, allnps, feature);
            finished = true;
        }

        public boolean isFinished() {
            return finished;
        }

        public void check(Set<String> nps, Set<String> allnps, FeatureTermNest feature) {
            int counter = 0;


            _logger.info("Total in batch:" + nps.size());
            for (String np : nps) {
                for (String anp : allnps) {
                    if (anp.length() <= np.length()) continue;
                    if (anp.indexOf(" " + np) != -1 || anp.indexOf(np + " ") != -1) //e.g., np=male, anp=the male
                        feature.termNestIn(np, anp);
                }
                counter++;
                if (counter % 500 == 0) _logger.info("Batch done" + counter + " end: " + np);
            }
        }
    }
}
