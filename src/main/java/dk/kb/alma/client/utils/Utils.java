package dk.kb.alma.client.utils;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
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
    /**
     * Return a modifiable list with the same content, or the same list if it is already modifiable
     *
     * This method is NOT threadsafe, but the returned list should be (as threadsafe as the input). So
     * ensure no other thread is accessing/modifying the list while we make it modifiable.
     *
     * @param list the list to get as modifiable
     * @param <E>  the type
     * @return the same list, if it is modifiable or a new Arraylist with the same content
     */
    public static <E> List<E> toModifiableList(@NotNull final List<E> list) {
        try {
            //First we check
            list.addAll(Collections.emptyList());
            //Not all immutable lists trigger on this... Collections.emptyList() is one that accepts this
            
            //So to catch such fraggers, we try to add an element
            int size = list.size();
            if (!list.isEmpty()) {
                //If the list is not empty, try to add the first element as a new last element
                list.add(list.get(0));
            } else {
                //Otherwise just add null
                list.add(null);
            } //We cannot just add whatever because we do not know the type of the list
            
            //Remove the newly added element
            list.remove(size);
            
            //TODO There is a risk that the add will complete, but the remove will fail.
            //That would basically be a append-only list...
            //If so, the list will end up with an additional null
            
            return list;
        } catch (java.lang.UnsupportedOperationException e) {
            return new ArrayList<>(list);
        }
    }
}
