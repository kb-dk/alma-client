package dk.kb.alma.locks;

import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AutoClosableLocks<T> {
    
    private final HashMap<T, Lock> currentLocks = new HashMap<>();
    
   
    public AutoClosableLocks() {
        //TODO implement a way to
    }
    
    /**
     * Acquire a lock for the given key, and return an AutoClosable instance, that will unlock when closed.
     * <p>
     * If some other thread is currently trying to acquire the lock for another URI, wait for that thread to finish.
     * If the key is already locked by someone, you will wait until you can acquire the lock again
     *
     * @param key the key to lock for
     * @return
     */
    public AutoClosableLock<T> lock(T key) {
        Lock logForKey = findLock(key);
        logForKey.lock();
        return new AutoClosableLock<>(key, this);
    }
    
    private synchronized Lock findLock(T key) {
        Lock logForKey = currentLocks.get(key);
        if (logForKey == null) {
            logForKey = new ReentrantLock();
            currentLocks.put(key, logForKey);
        }
        return logForKey;
    }
    
    
    public synchronized void unlock(T key) {
        //globalLock.lock();
        Lock lockForKey = currentLocks.get(key);
        if (lockForKey != null) {
            lockForKey.unlock();
            currentLocks.remove(key);
        }
    }
}
