package dk.kb.alma.client.locks;

import java.util.Objects;

public class AutoClosableLock<T> implements AutoCloseable {
    
    private final T key;
    private final AutoClosableLocks<T> pool;
    
    protected AutoClosableLock(T key, dk.kb.alma.client.locks.AutoClosableLocks<T> pool) {
        this.key = key;
        this.pool = pool;
    }
    
    @Override
    public void close() {
        pool.unlock(key);
    }
    
    public T getKey() {
        return key;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AutoClosableLock<?> that = (AutoClosableLock<?>) o;
        return key.equals(that.key);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}
