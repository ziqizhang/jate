package uk.ac.shef.dcs.jate.v2.feature;

import org.apache.lucene.util.BytesRef;
import uk.ac.shef.dcs.jate.v2.JATERecursiveTaskWorker;

import java.util.List;

/**
 * Created by zqz on 20/09/2015.
 * todo
 */
public class CooccurrenceDocBasedFBWorker extends JATERecursiveTaskWorker<BytesRef, Cooccurrence> {
    public CooccurrenceDocBasedFBWorker(List<BytesRef> tasks, int maxTasksPerWorker) {
        super(tasks, maxTasksPerWorker);
    }

    @Override
    protected JATERecursiveTaskWorker<BytesRef, Cooccurrence> createInstance(List<BytesRef> splitTasks) {
        return null;
    }

    @Override
    protected Cooccurrence mergeResult(List<JATERecursiveTaskWorker<BytesRef, Cooccurrence>> jateRecursiveTaskWorkers) {
        return null;
    }

    @Override
    protected Cooccurrence computeSingleWorker(List<BytesRef> tasks) {
        return null;
    }
}
