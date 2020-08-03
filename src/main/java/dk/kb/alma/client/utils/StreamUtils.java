package dk.kb.alma.client.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

public class StreamUtils {
    
    /**
     *
     * Use in a filter like {@code
     *  .filter(StreamUtils.distinctByKey(cd -> cd.getNatBibNR()))
     *  }
     *
     *  https://stackoverflow.com/a/27872086/11532838
     *
     *
      * @param keyExtractor
     * @param <T>
     * @return
     */
    public static <T> Predicate<T> distinctByKey(Function<? super T,Object> keyExtractor) {
        Map<Object,Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }
}
