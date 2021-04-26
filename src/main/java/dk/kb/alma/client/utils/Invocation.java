package dk.kb.alma.client.utils;

import dk.kb.alma.client.HttpClient;
import org.apache.cxf.jaxrs.client.WebClient;

import java.util.function.Supplier;

public class Invocation<T, E> implements AutoCloseable {
    
    private final WebClient uri;
    
    private final Class<T> type;
    
    private final E entity;
    
    private final HttpClient.Operation operation;
    
    private int retryCount;
    
    public Invocation(WebClient uri, Class<T> type, E entity, HttpClient.Operation operation, int retryCount) {
        this.uri        = uri;
        this.type       = type;
        this.entity     = entity;
        this.operation  = operation;
        if (retryCount < 0){ //if initial retrycount is negative, retry forever
            retryCount = Integer.MAX_VALUE;
        }
        this.retryCount = retryCount;
    }
    
    
    public Invocation<T, E> withNewUri(WebClient newUri) {
        return new Invocation<>(newUri, type, entity, operation, retryCount);
    }
    
    public WebClient getUri() {
        return uri;
    }
    
    public Class<T> getType() {
        return type;
    }
    
    public E getEntity() {
        return entity;
    }
    
    public HttpClient.Operation getOperation() {
        return operation;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    @Override
    public void close() {
        if (uri != null) {
            uri.close();
        }
    }
    
    public synchronized <X extends Throwable> Invocation<T, E> decrementRetryCount(Supplier<? extends X> exceptionSupplier)
            throws X {
        retryCount -= 1;
        if (retryCount <= 0) {
            throw exceptionSupplier.get();
        }
        return this;
    }
    
    @Override
    public String toString() {
        return "Invocation{"
               + "uri="
               + uri.getCurrentURI()
               + ", type="
               + type
               + ", entity="
               + entity
               + ", operation="
               + operation
               + ", retryCount="
               + retryCount
               + '}';
    }
}
