package uk.ac.shef.dcs.jate.feature;

import org.apache.solr.common.util.Pair;
import uk.ac.shef.dcs.jate.JATERecursiveTaskWorker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by - on 25/02/2016.
 */
public class TermComponentIndexFBWorker extends JATERecursiveTaskWorker<String, TermComponentIndex> {
	private static final long serialVersionUID = -2181124313058459305L;

    public TermComponentIndexFBWorker(List<String> tasks, int maxTasksPerWorker) {
        super(tasks, maxTasksPerWorker);
    }

    @Override
    protected JATERecursiveTaskWorker<String, TermComponentIndex> createInstance(List<String> splitTasks) {
        return new TermComponentIndexFBWorker(splitTasks, maxTasksPerThread);
    }

    @Override
    protected TermComponentIndex mergeResult(List<JATERecursiveTaskWorker<String, TermComponentIndex>> jateRecursiveTaskWorkers) {
        TermComponentIndex merged=new TermComponentIndex();
        for (JATERecursiveTaskWorker<String, TermComponentIndex> worker : jateRecursiveTaskWorkers) {
            TermComponentIndex featureIndex = worker.join();
            for (Map.Entry<String, List<Pair<String, Integer>>> e :
                featureIndex.getComponentIndex().entrySet()){
                String tok = e.getKey();
                List<Pair<String, Integer>> parentTerms = merged.getComponentIndex().get(tok);
                if (parentTerms==null)
                    parentTerms = new ArrayList<>();

                parentTerms.addAll(e.getValue());
                merged.getComponentIndex().put(tok, parentTerms);
            }
        }
        return merged;
    }

    @Override
    protected TermComponentIndex computeSingleWorker(List<String> tasks) {
        TermComponentIndex ctciFeature=new TermComponentIndex();
        for(String tString : tasks){
            String[] tokens = tString.split(" ");

            for(String tok: tokens){
                ctciFeature.add(tok, tString, tokens.length);
            }
        }
        return ctciFeature;
    }
}
