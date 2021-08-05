package dk.kb.alma.client.utils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Utils {
    public static <T> T withDefault(T value, T defaultValue) {
        return nullable(value).orElse(defaultValue);
    }
    
    public static <T> Optional<T> nullable(@Nullable T t) {
        return Optional.ofNullable(t);
    }
    
    public static <E> List<E> toModifiableList(List<E> list) {
        try {
            list.addAll(Collections.emptyList());
            int size = list.size();
            list.add(null);
            list.remove(size);
        } catch (java.lang.UnsupportedOperationException e){
            list = new ArrayList<>(list);
        }
        
        return list;
    }
}
