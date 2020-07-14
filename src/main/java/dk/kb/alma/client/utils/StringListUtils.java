package dk.kb.alma.client.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StringListUtils {
    
    
    public static final Predicate<String> notNull = Predicate.not(Objects::isNull);
    public static final Predicate<String> notEmptyString = Predicate.not(String::isEmpty);
    public static final Predicate<String> isNotEmpty = notNull.and(notEmptyString);
    
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
        return Optional.ofNullable(string).orElse("");
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
        if (list == null) {
            return Collections.emptyList();
        }
        return list.stream()
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
        if (value == null || value.isBlank()) {
            return default_value;
        } else {
            return value;
        }
    }
    
    
    public static <T> T orDefault(T value, T default_value) {
        return Optional.ofNullable(value).orElse(default_value);
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
    
    public static <T> List<T> toList(T... strings) {
        if (strings == null) {
            return new ArrayList<>();
            //Important that this is not a unmodifiable list
        } else {
            return Arrays.asList(strings);
        }
    }
    
    /**
     * Trims all the strings in the list and filters out any blank strings and nulls.
     * @param listOfStrings the strings to be cleaned. If null, returns empty list
     * @return the resulting list.
     */
    public static List<String> cleanList(List<String> listOfStrings) {
        if (listOfStrings == null) {
            return Collections.emptyList();
        }
        return listOfStrings.stream()
                            .filter(Objects::nonNull)
                            .map(string -> string.trim())
                            .filter(string -> !string.isBlank())
                            .collect(Collectors.toList());
    }
    
    public static String substring(String string, int startIndex, int endIndex) {
        string = notNull(string);
        
        while (startIndex < 0){
            if (string.length() > 0) {
                startIndex = startIndex + string.length();
            } else {
                startIndex = string.length();
            }
        }
    
        while (endIndex < 0){
            if (string.length() > 0) {
                endIndex = endIndex + string.length();
            } else {
                endIndex = string.length();
            }
        }
    
        if (startIndex > string.length()){
            startIndex = string.length();
        }
        int endindex = Math.min(endIndex, string.length());
        
        return string.substring(startIndex, endindex);
    }
    
    public static String cutMiddle(String string, int maxLength) {
        
        string = notNull(string);
        if (string.length() < maxLength){
            return string;
        }
        String truncateString = "...";
        maxLength = maxLength - truncateString.length();
    
        int startStringLength = maxLength / 2;
        int endStringLength = Math.floorDiv(maxLength, 2);
        String startString = substring(string, 0, startStringLength);
        String endString = substring(string, string.length()-1-endStringLength, string.length());
    
        int numberRemovedChars = string.length() - maxLength;
        return startString + truncateString + endString;
        
    }
    
    
    public static String cutEnd(String string, int maxLength) {
        string = notNull(string);
        if (string.length() < maxLength){
            return string;
        }
        String truncateString = "...";
        maxLength = maxLength - truncateString.length();
        
        String startString = substring(string, 0, maxLength);
        
        return startString + truncateString;
        
    }
}
