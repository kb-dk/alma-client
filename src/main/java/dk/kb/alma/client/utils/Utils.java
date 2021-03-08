package dk.kb.alma.client.utils;

import javax.annotation.Nullable;
import java.util.Optional;

public class Utils {
    public static <T> T withDefault(T value, T defaultValue) {
        return nullable(value).orElse(defaultValue);
    }
    
    public static <T> Optional<T> nullable(@Nullable T t) {
        return Optional.ofNullable(t);
    }
}
