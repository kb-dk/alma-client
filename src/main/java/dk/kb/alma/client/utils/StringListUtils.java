package dk.kb.alma.client.utils;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StringListUtils {
    /**
     * Turn an iterator into a stream
     *
     * @param iterator the iterator
     * @param <T>      the the type of elements
     * @return A stream of the iterator elements in the same order
     */
    public static <T> Stream<T> asStream(Iterator<T> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false);
    }
    
    /**
     * Return the string or "" if null
     *
     * @param string the string
     * @return same or "" if null
     */
    public static String notNull(String string) {
        if (string == null) {
            return "";
        }
        return string;
        
    }
    
    public static String firstOf(List<String> list, String defaultValue) {
        if (list.isEmpty()) {
            return defaultValue;
        } else {
            return list.get(0);
        }
    }
    
    public static String getParamOrProps(Properties props, String param, String propKey) {
        if (param == null) {
            
            if (!props.contains(propKey)) {
                param = props.getProperty(propKey);
            } else {
                param = null;
            }
        }
        return param;
    }
    
    public static List<String> removeEmpties(List<String> list) {
        return list.stream()
                   .parallel()
                   .filter(Objects::nonNull)
                   .filter(number -> !number.trim().isEmpty())
                   .distinct()
                   .collect(Collectors.toList());
    }
    
    public static <T extends Iterable<String>> T removeSubstrings(T list) {
        
        Iterator<String> firstIterator = list.iterator();
        while (firstIterator.hasNext()) {
            String s = firstIterator.next();
            for (String t : list) {
                if (s.equals(t)) {
                    continue;
                }
                if (t.contains(s)) {
                    firstIterator.remove();
                    break;
                }
            }
        }
        
        return list;
    }
    
    @SafeVarargs
    public static <T> Set<T> setOf(T... objects) {
        return new HashSet<T>(Arrays.asList(objects));
    }
    
    //
    //public static Set<String> removeSubstrings(Set<String> list) {
    //
    //    Set<String> result = new HashSet<>();
    //    for (String s : list) {
    //        boolean substring = false;
    //        for (String t : list) {
    //            if (s.equals(t)){
    //                continue;
    //            }
    //            if (t.contains(s)){
    //                substring = true;
    //                break;
    //            }
    //        }
    //        if (!substring){
    //            result.add(s);
    //        }
    //    }
    //    return result;
    //}
    private static List<String> removeEmpties(String... list) {
        return Arrays.stream(list)
                     .filter(Objects::nonNull)
                     .filter(number -> !number.trim().isEmpty())
                     .distinct()
                     .collect(Collectors.toList());
    }
    
    public static String orDefault(String value, String default_value) {
        if (value == null) {
            return default_value;
        } else {
            return value;
        }
    }
    
    /**
     * If the string contains /, return the part BEFORE the last /. Otherwise return the entire string
     *
     * @param string the string
     * @return see above
     */
    public static String firstPart(String string) {
        string = notNull(string);
        
        if (string.contains("/")) {
            string = string.substring(0, string.indexOf("/"));
        }
        return string;
    }
    
}
