package dk.kb.alma.client.utils;

import dk.kb.alma.client.utils.NamedThread;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

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
    
    public static <T, R> Function<T, R> namedThread(Function<T, R> function, Function<T,String> name) {
        return t -> {
            try (NamedThread namedThread = new NamedThread(name.apply(t))){
                return function.apply(t);
            }
        };
    }
    
    public static <T> Consumer<T> namedThread(Consumer<T> consumer, Function<T,String> name) {
        return t -> {
            try (NamedThread namedThread = new NamedThread(name.apply(t))){
                consumer.accept(t);
            }
        };
        
    }
    
    public static <T> Predicate<T> namedThread(Predicate<T> predicate, Function<T,String> name) {
        return t -> {
            try (NamedThread namedThread = new NamedThread(name.apply(t))){
                return predicate.test(t);
            }
        };
        
    }
    
}
