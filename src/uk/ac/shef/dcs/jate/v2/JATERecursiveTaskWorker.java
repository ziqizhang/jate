package uk.ac.shef.dcs.jate.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;

/**
 * Created by zqz on 15/09/2015.
 */
public abstract class JATERecursiveTaskWorker<S, T> extends RecursiveTask<T>{
    protected List<S> tasks;
    protected int maxTasksPerThread;

    public JATERecursiveTaskWorker(List<S> tasks, int maxTasksPerWorker){
        this.tasks = tasks;
        this.maxTasksPerThread=maxTasksPerWorker;
    }

    protected abstract JATERecursiveTaskWorker<S, T> createInstance(List<S> splitTasks);

    protected abstract T mergeResult(List<JATERecursiveTaskWorker<S, T>> workers);

    protected abstract T computeSingleWorker(List<S> tasks);

    @Override
    protected T compute() {
        if (this.tasks.size() > maxTasksPerThread) {
            List<JATERecursiveTaskWorker<S, T>> subWorkers =
                    new ArrayList<>();
            subWorkers.addAll(createSubWorkers());
            for (JATERecursiveTaskWorker<S, T> subWorker : subWorkers)
                subWorker.fork();
            return mergeResult(subWorkers);
        } else{
            return computeSingleWorker(tasks);
        }
    }

    protected List<JATERecursiveTaskWorker<S, T>> createSubWorkers() {
        List<JATERecursiveTaskWorker<S, T>> subWorkers =
                new ArrayList<>();

        int total = tasks.size() / 2;
        List<S> splitTask1 = new ArrayList<>();
        for (int i = 0; i < total; i++)
            splitTask1.add(tasks.get(i));
        JATERecursiveTaskWorker<S, T> subWorker1 = createInstance(splitTask1);

        List<S> splitTask2 = new ArrayList<>();
        for (int i = total; i < tasks.size(); i++)
            splitTask2.add(tasks.get(i));
        JATERecursiveTaskWorker<S, T> subWorker2 = createInstance(splitTask2);

        subWorkers.add(subWorker1);
        subWorkers.add(subWorker2);

        return subWorkers;
    }
}
