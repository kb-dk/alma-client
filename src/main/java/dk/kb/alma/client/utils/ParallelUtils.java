package dk.kb.alma.client.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

public class ParallelUtils {
    
    public static <T> T inParrallel(int threads, Callable<T> t){
        ForkJoinPool forkJoinPool = null;
        try {
            forkJoinPool = new ForkJoinPool(threads);
            return forkJoinPool.submit(t).get(); //this makes it an overall blocking call
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            if (forkJoinPool != null) {
                forkJoinPool.shutdown(); //always remember to shutdown the pool
            }
        }
    }
}
