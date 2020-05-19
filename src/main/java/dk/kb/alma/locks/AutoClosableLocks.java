package dk.kb.alma.locks;

import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AutoClosableLocks<T> {
    
    private final HashMap<T, Lock> currentLocks = new HashMap<>();
    
    private final Lock globalLock = new ReentrantLock(true);
    
    public AutoClosableLocks() {
        //TODO implement a way to
    }
    
    /**
     * Acquire a lock for the given key, and return an AutoClosable instance, that will unlock when closed.
     *
     * If some other thread is currently trying to acquire the lock for another URI, wait for that thread to finish.
     * If the key is already locked by someone, you will wait until you can acquire the lock again
     * @param key the key to lock for
     * @return
     */
    public AutoClosableLock<T> lock(T key){
        globalLock.lock();
        try {
            Lock logForKey = currentLocks.get(key);
            if (logForKey == null) {
                logForKey = new ReentrantLock();
            }
            logForKey.lock();
            //Do not put, before you actually have acquired the lock. If you are stuck here, and someone else gets
            // the lock and unlocks it again, we want to ENSURE that your put will happen afterwards, not before the
            currentLocks.put(key, logForKey);
            return new AutoClosableLock<>(key, this);
        } finally {
            globalLock.unlock();
        }
    }
    
   
    public void unlock(T key){
        globalLock.lock();
        try {
            Lock lockForKey = currentLocks.get(key);
            if (lockForKey != null) {
                lockForKey.unlock();
                currentLocks.remove(key);
            }
        } finally {
            globalLock.unlock();
        }
    }
}
