package dk.kb.alma.client.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

public class ParallelUtils {
    
    private static final ForkJoinPool forkJoinPool = new ForkJoinPool(4,
                                                                      pool -> {
                                                                          ForkJoinWorkerThread thread
                                                                                  = ForkJoinPool.defaultForkJoinWorkerThreadFactory
                                                                                            .newThread(pool);
                                                                          thread.setName("SpecialNameToCheckInLog-"
                                                                                         + thread.getPoolIndex());
                                                                          return thread;
                                                                      },
                                                                      null,
                                                                      false);
    
    //TODO do we want an explicit thread pool for the api communication?
    public static <T> T inParallel(Callable<T> producer) {
        try {
            return forkJoinPool.submit(producer).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw new RuntimeException(cause);
        }
    }
    
}
