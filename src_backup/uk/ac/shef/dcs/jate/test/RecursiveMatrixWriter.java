/*
package uk.ac.shef.dcs.jate.test;

import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.impl.SparseCCDoubleMatrix2D;
import cern.colt.matrix.tdouble.impl.SparseDoubleMatrix2D;
import uk.ac.shef.dcs.jate.JATERecursiveTaskWorker;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

*/
/**
 * Created by - on 10/10/2015.
 *//*

public class RecursiveMatrixWriter extends JATERecursiveTaskWorker<int[], Integer>{
    private DoubleMatrix2D matrix;

    public RecursiveMatrixWriter(DoubleMatrix2D matrix, List<int[]> tasks, int maxTasksPerWorker) {
        super(tasks, maxTasksPerWorker);
        this.matrix=matrix;
    }

    @Override
    protected JATERecursiveTaskWorker<int[], Integer> createInstance(List<int[]> splitTasks) {
        return new RecursiveMatrixWriter(matrix,splitTasks, maxTasksPerThread);
    }

    @Override
    protected Integer mergeResult(List<JATERecursiveTaskWorker<int[], Integer>> jateRecursiveTaskWorkers) {
        int total=0;
        for(JATERecursiveTaskWorker<int[], Integer> one: jateRecursiveTaskWorkers){
            total+=one.join();
        }
        return total;
    }



    @Override
    protected Integer computeSingleWorker(List<int[]> tasks) {
        System.out.println("worker begins");
        for(int[] i: tasks)
            matrix.set(i[0], i[1], 2);
        return 9;
    }

    public static void main(String[] args) {
        DoubleMatrix2D matrix = new SparseDoubleMatrix2D(10,10);
        List<int[]> indexes = new ArrayList<>();
        for(int r=0; r<2; r++){

            for(int c=0; c<10; c++){
                indexes.add(new int[]{r, c});
            }

        }
        int batch = indexes.size()/2;
        RecursiveMatrixWriter writer = new RecursiveMatrixWriter(matrix, indexes,batch);

        ForkJoinPool forkJoinPool = new ForkJoinPool(2);
        Integer total = forkJoinPool.invoke(writer);
        System.out.println(writer.matrix);
        System.out.println("end");
    }
}
*/
