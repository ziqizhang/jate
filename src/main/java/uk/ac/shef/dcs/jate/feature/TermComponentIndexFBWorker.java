package uk.ac.shef.dcs.jate.feature;

import uk.ac.shef.dcs.jate.JATERecursiveTaskWorker;

import java.util.List;

/**
 * Created by - on 25/02/2016.
 */
public class TermComponentIndexFBWorker extends JATERecursiveTaskWorker<String, Integer> {
	private static final long serialVersionUID = -2181124313058459305L;
	
	private TermComponentIndex ctciFeature;
    public TermComponentIndexFBWorker(List<String> tasks, int maxTasksPerWorker,
                                      TermComponentIndex ctciFeature) {
        super(tasks, maxTasksPerWorker);
        this.ctciFeature=ctciFeature;
    }

    @Override
    protected JATERecursiveTaskWorker<String, Integer> createInstance(List<String> splitTasks) {
        return new TermComponentIndexFBWorker(splitTasks, maxTasksPerThread, ctciFeature);
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
        for(String tString : tasks){
            String[] tokens = tString.split(" ");

            for(String tok: tokens){
                ctciFeature.add(tok, tString, tokens.length);
            }
            count++;
        }
        return count;
    }
}
