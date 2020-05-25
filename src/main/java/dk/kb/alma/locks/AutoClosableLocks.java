package dk.kb.alma.locks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AutoClosableLocks<T> {
    protected final static Logger log = LoggerFactory.getLogger(AutoClosableLocks.class);
    
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
        log.debug("Thread {} attempting to lock {}",Thread.currentThread().getName(),key);
        logForKey.lock();
        log.debug("Thread {} locked {}",Thread.currentThread().getName(),key);
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
            log.debug("Thread {} unlocked lock for {}",Thread.currentThread().getName(),key);
        }
    }
}
