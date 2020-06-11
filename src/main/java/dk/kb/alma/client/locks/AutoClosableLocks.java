package dk.kb.alma.client.locks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AutoClosableLocks<T> {
    protected final static Logger log = LoggerFactory.getLogger(AutoClosableLocks.class);
    
    private final Map<T, ReentrantLock> currentLocks = Collections.synchronizedMap(new HashMap<>());
    
   
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
    public dk.kb.alma.client.locks.AutoClosableLock<T> lock(T key) {
        Lock logForKey = findLock(key);
        log.debug("Thread {} attempting to lock {}",Thread.currentThread().getName(),key);
        logForKey.lock();
        log.debug("Thread {} locked {}",Thread.currentThread().getName(),key);
        return new dk.kb.alma.client.locks.AutoClosableLock<>(key, this);
    }
    
    private synchronized Lock findLock(T key) {
        ReentrantLock logForKey = currentLocks.get(key);
        if (logForKey == null) {
            logForKey = new ReentrantLock();
            currentLocks.put(key, logForKey);
        }
        return logForKey;
    }
    
    
    public void unlock(T key) {
        //globalLock.lock();
        ReentrantLock lockForKey = currentLocks.get(key);
        if (lockForKey != null) {
            if (lockForKey.tryLock()) {
                try {
                    currentLocks.remove(key);
                } finally {
                    lockForKey.unlock();
                }
            } else {
                log.error("Tried to unlock lock {} for key {}, but we are not the owner...",lockForKey,key);
            }
            log.debug("Thread {} unlocked lock for {}", Thread.currentThread().getName(), key);
        } else {
            log.error("Attempted to unlock key {}, but the lock was null",key);
        }
    }
}
