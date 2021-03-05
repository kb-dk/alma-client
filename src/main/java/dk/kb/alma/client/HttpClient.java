package dk.kb.alma.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dk.kb.alma.client.exceptions.AlmaConnectionException;
import dk.kb.alma.client.exceptions.AlmaKnownException;
import dk.kb.alma.client.exceptions.AlmaNotFoundException;
import dk.kb.alma.client.exceptions.AlmaUnknownException;
import dk.kb.alma.gen.web_service_result.Error;
import dk.kb.alma.gen.web_service_result.WebServiceResult;
import dk.kb.util.xml.XML;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.impl.UriBuilderImpl;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.RedirectionException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.JAXBException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public abstract class HttpClient {
    
    protected final static Logger log = LoggerFactory.getLogger(HttpClient.class);
    
    
    private final Cache<URI, Object> cache;
    
    private final Map<String, String> globalParams;
    
    private final long minSleepMillis;
    private final long sleepVariationMillis;
    
    
    private final int connectTimeout;
    private final int readTimeout;
    
    private boolean cachingEnabled = true;
    
    private boolean retryOnTimeouts = true;
    
    private boolean retryOnSocketExceptions = true;
    
    private int maxRetries;
    
    private boolean retryOn429 = true;
    
    private final String target;
    
    public HttpClient(String target,
                          long minSleep,
                          long sleepVariation,
                          Map<String,String> globalParams,
                          int connectTimeout,
                          int readTimeout,
                          long cacheTimeMillis,
                      Integer maxRetries)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        this.target = target;
        this.minSleepMillis = minSleep;
        this.sleepVariationMillis = sleepVariation;
        this.globalParams = globalParams;
    
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.maxRetries = Optional.ofNullable(maxRetries).orElse(3);
        
        int cacheSize = 1000;
        cache = CacheBuilder.newBuilder()
                            .maximumSize(cacheSize)
                            .expireAfterAccess(cacheTimeMillis, TimeUnit.MILLISECONDS)
                            .build();
    }
    
    //GETTERS + SETTERS
    
    public boolean isCachingEnabled() {
        return cachingEnabled;
    }
    
    /**
     * Controls whether or not we use caching for GET requests. Non-GET requests never use caching. If this is true
     * (default), GET requests use caching.
     * @param cachingEnabled should caching be enabled for GET requests
     */
    public void setCachingEnabled(boolean cachingEnabled) {
        this.cachingEnabled = cachingEnabled;
    }
    
    public boolean isRetryOnTimeouts() {
        return retryOnTimeouts;
    }
    
    /**
     * retryOnTimeouts control whether or not we automatically retry non-GET requests that time out.
     * This is always enabled for GET requests. This parameter controls whether or not we also retry for non-GET requests
     * @param retryOnTimeouts should we retry non-GET requests that time out?
     */
    public void setRetryOnTimeouts(boolean retryOnTimeouts) {
        this.retryOnTimeouts = retryOnTimeouts;
    }
    
    
    public boolean isRetryOnSocketExceptions() {
        return retryOnSocketExceptions;
    }
    
    /**
     * retryOnSocketExceptions control whether or not we automatically retry non-GET requests that fail on a socket exception.
     * This is always enabled for GET requests. This parameter controls whether or not we also retry for non-GET requests
     * @param retryOnSocketExceptions should we retry non-GET requests that fail out?
     */
    public void setRetryOnSocketExceptions(boolean retryOnSocketExceptions) {
        this.retryOnSocketExceptions = retryOnSocketExceptions;
    }
    
    public boolean isRetryOn429() {
        return retryOn429;
    }
    
    /**
     * retryOn429 control whether or not we automatically retry non-GET requests that receive ALMA HTTP 429 (rate limit)
     * errors.
     * This is always enabled for GET requests. This parameter controls whether or not we also retry for non-GET requests
     * @param retryOn429 should we retry non-GET requests that fail on rate limit.
     */
    public void setRetryOn429(boolean retryOn429) {
        this.retryOn429 = retryOn429;
    }
    
    
    //PUBLIC METHODS
    
    public WebClient constructLink() {
        return getWebClient(target);
    }
    
    public WebClient getWebClient(URI link) {
        URI host = new UriBuilderImpl(link).replaceQuery(null).replacePath(null).replaceMatrix(null).build();
        
        
        JacksonJaxbJsonProvider jacksonJaxbJsonProvider = new JacksonJaxbJsonProvider();
        jacksonJaxbJsonProvider.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);
        jacksonJaxbJsonProvider.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        
        
        final List<?> providers = Arrays.asList(jacksonJaxbJsonProvider);
        WebClient client = WebClient.create(host.toString(), providers);
        
        
        HTTPConduit conduit = WebClient.getConfig(client).getHttpConduit();
        conduit.getClient().setConnectionTimeout(connectTimeout);
        conduit.getClient().setConnectionRequestTimeout(connectTimeout);
        conduit.getClient().setReceiveTimeout(readTimeout);
        
        if (link.getPath() != null) {
            client = client.path(link.getPath());
        }
        if (link.getQuery() != null) {
            client = client.replaceQuery(link.getQuery());
        }
        
        client = client.accept(MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE)
                       .type(MediaType.APPLICATION_XML_TYPE);
        
        if (globalParams != null) {
            for (Map.Entry<String, String> globalParam : globalParams.entrySet()) {
                client = client.replaceQueryParam(globalParam.getKey(), globalParam.getValue());
            }
        }
        
        
        return client;
    }
    
    public WebClient getWebClient(String link) {
        return getWebClient(UriBuilder.fromUri(link).build());
    }
    
    public enum Operation {
        POST, PUT, DELETE, GET
    }
    
    public <T> T get(final WebClient link, Class<T> type)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return get(link, type, cachingEnabled);
    }
    
    public <T> T get(final WebClient link, Class<T> type, boolean useCache)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return invokeCache(link, type, null, useCache, Operation.GET);
    }
    
    public <T, E> T put(final WebClient link, Class<T> type, E entity)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return invokeDirect(link, type, entity, Operation.PUT,0);
    }
    
    public <T, E> T post(final WebClient link, Class<T> type, E entity)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return invokeDirect(link, type, entity, Operation.POST,0);
    }
    
    public <T> T delete(final WebClient link, Class<T> type)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return invokeDirect(link, type, null, Operation.DELETE,0);
    }
    
    
    //Actual implementation
    
    /**
     * Attempt to fetch the requested resource from the cache. If not found, fetches it from ACTUAL
     * @param uri the URI to fetch
     * @param type the Class of the result
     * @param entity the body. Can be null
     * @param useCache if false, will bypass cache
     * @param operation the HTTP operation (GET, POST,...)
     * @param <T> the type of the result
     * @param <E> the type of the body entity
     * @return the resulting java object, either from cache or from the actual server
     * @throws AlmaConnectionException if we failed on a deeper level, like the connection
     * @throws AlmaKnownException if we failed on a documented API error code
     * @throws AlmaUnknownException if we failed on a higher level, but not in a documented way
     */
    protected <T, E> T invokeCache(final WebClient uri,
                                   Class<T> type,
                                   E entity,
                                   boolean useCache,
                                   Operation operation)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        
        //Only use cache on getRequests
        useCache &= operation == Operation.GET;
        
        //Remove the api key from the query string. This is something we handle here, not something you should set
        removeAuth(uri);
        URI currentURI = uri.getCurrentURI();
        
        try {
            if (useCache) {
                Object cacheValue = cache.getIfPresent(currentURI);
                if (type.isInstance(cacheValue)) {
                    log.debug("cache hit on {}", currentURI);
                    return (T) cacheValue;
                }
            }
            
            //It is possible for multiple threads to run here, potentially getting the same URI concurrently
            T value = invokeDirect(uri, type, entity, operation,0);
            
            if (useCache) {
                cache.put(currentURI, value);
            }
            return value;
        } finally {
            uri.close();
        }
    }
    
    protected abstract WebClient removeAuth(WebClient link);
    
    protected abstract WebClient addAuth(WebClient link);
    
    /**
     * Invoke the actual server and return the result.
     *
     * @param uri the uri to invoke
     * @param type the class of the result
     * @param entity the body entity. Can be null
     * @param operation the operation (GET, POST,...)
     * @param retryCount A counter keeping track of which retry we are at. Just provide 0 if this does not make sense.
     * @param <T> the type of the result
     * @param <E> the type of the entity
     * @return the result
     * @throws AlmaConnectionException if we failed on a deeper level, like the connection
     * @throws AlmaKnownException if we failed on a documented API error code
     * @throws AlmaUnknownException if we failed on a higher level, but not in a documented way
     */
    protected <T,E> T invokeDirect(final WebClient uri, Class<T> type, E entity, Operation operation, int retryCount)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        
        //Remove the api key from the query string. This is something we handle here, not something you should set
        removeAuth(uri);
    
        URI currentURI = uri.getCurrentURI();
        log.debug("{}ing on {}", operation, currentURI);
        T value;
        try {
            WebClient webClient = addAuth(uri);
            value = webClient.invoke(operation.name(), entity, type);
            log.trace("{}ed on {}", operation, currentURI);
        } catch (Fault | ProcessingException e) {
            //I am not entirely sure that Fault can reach this far, without being converted to a ProcessingException,
            // but better safe than sorry
            
            // If maxRetries have been set (to 0+), limit the number of retries
            // if maxRetries < 0, retry forever, with no limit
            // if maxRetries == 0, fail immediately, rather than attempt to retry
            if (maxRetries >= 0 && retryCount >= maxRetries){
                throw new AlmaConnectionException("Failed to " + operation.name() + "ing '" + currentURI + "'", e);
            } else {
                //Increment retrycount here so we do not forget it in one of the branches below
                retryCount+=1;
            }
            
            //This checks if any exception in the hierachy is a socket timeout exception.
            List<Throwable> causes = getCauses(e);
            if (shouldRetryOnTimeout(operation) &&
                causes.stream().anyMatch(cause -> cause instanceof SocketTimeoutException)) {
                //Multiple things, like SSL and ordinary reads and connects can cause SocketTimeouts, but at
                // different levels of the hierachy
                log.trace("Socket timeout for " + operation.name() + " on " + currentURI, e);
                sleep("Socket timeout exception for '" + currentURI + "'");
                HTTPClientPolicy clientPolicy = WebClient.getConfig(uri).getHttpConduit().getClient();
                clientPolicy.setConnectionTimeout(clientPolicy.getConnectionTimeout() * 2);
                clientPolicy.setReceiveTimeout(clientPolicy.getReceiveTimeout() * 2);
                clientPolicy.setConnectionRequestTimeout(clientPolicy.getConnectionRequestTimeout() * 2);
                log.debug("Increased timeouts to connect={}ms and receive={}ms for the {}ing of {}",
                          clientPolicy.getConnectionTimeout(),
                          clientPolicy.getReceiveTimeout(),
                          operation.name(),
                          currentURI);
    
    
                return invokeDirect(uri, type, entity, operation, retryCount);
            } else if (shouldRetryOnSocketException(operation) && causes
                    .stream()
                    .anyMatch(cause -> cause instanceof SocketException)) {
    
                log.trace("Socket Exception for " + operation.name() + " on " + currentURI, e);
                sleep("Socket exception for '" + currentURI + "'");
    
                return invokeDirect(uri, type, entity, operation, retryCount);
            } else {
                throw new AlmaConnectionException("Failed to " + operation.name() + "ing '" + currentURI + "'", e);
            }
        } catch (RedirectionException e) {
            URI redirectLocation = e.getLocation();
            log.debug("Redirecting {} to {}", operation, redirectLocation.getPath());
            if (redirectLocation.isAbsolute()) {
                return invokeDirect(getWebClient(redirectLocation), type, entity, operation, retryCount);
            } else {
                WebClient newLink = constructLink();
                newLink = newLink.replacePath(redirectLocation.getPath());
                String redirectQueryString = redirectLocation.getQuery();
                if (redirectQueryString != null) {
                    String currentQueryString = newLink.getCurrentURI().getQuery();
                    if (currentQueryString != null) {
                        newLink = newLink.replaceQuery(currentQueryString + "&" + redirectQueryString);
                    } else {
                        newLink = newLink.replaceQuery(redirectQueryString);
                    }
                }
                return invokeDirect(newLink, type, entity, operation, retryCount);
            }
        } catch (WebApplicationException e) {
            if (shouldRetryOn429(operation) && rateLimitSleep(e, currentURI)) {
                //Do not increment retryCount as 429's should be retried forever. They do not count as errors with
                // a limited number of retries
                return invokeDirect(uri, type, entity, operation, retryCount);
            }
            
            String entityMessage = "";
            if (entity != null) {
                try {
                    entityMessage = "with entity '" + XML.marshall(entity) + "' ";
                } catch (JAXBException jaxbException) {
                    throw new AlmaConnectionException(jaxbException+": Failed to parse entity '" + entity + "' as xml, but throwing the original WebApplicationException",
                                                      e);
                }
            }
            
            Response response = e.getResponse();
            //Buffer entity so we can read the response multiple times
            response.bufferEntity();
            WebServiceResult result;
            try {
                result = response.readEntity(WebServiceResult.class);
            } catch (Exception e2) {
                log.error(
                        "Failed to parse response {} as WebServiceResult, but throwing based on the original exception {}",
                        response.readEntity(String.class),
                        e,
                        e2);
                throw new AlmaUnknownException(operation.name(),
                                               entityMessage,
                                               currentURI,
                                               response,
                                               e);
            }
            Error error = null;
            try {
                error = result.getErrorList().getErrors().get(0);
            } catch (RuntimeException noErrorInErrorlist) {
                //ignore, just contiune
            }
            if (error != null) {
                switch (error.getErrorCode()) {
                    case "NOT_FOUND":
                        throw new AlmaNotFoundException(operation.name(),
                                                        entityMessage,
                                                        currentURI,
                                                        response,
                                                        result,
                                                        e);
                }
            }
            throw new AlmaKnownException(operation.name(),
                                         entityMessage,
                                         currentURI,
                                         response,
                                         result,
                                         e);
            
        } finally {
            uri.close();
        }
        return value;
        
    }
    
    private boolean shouldRetryOn429(Operation operation) {
        switch (operation){
            case GET:
                return true;
            case POST:
            case PUT:
            case DELETE:
                return retryOn429;
            default:
                return false;
        }    }
    
    private boolean shouldRetryOnTimeout(Operation operation) {
        switch (operation){
            case GET:
                return true;
            case POST:
            case PUT:
            case DELETE:
                return retryOnTimeouts;
            default:
                return false;
        }
    }
    
    private boolean shouldRetryOnSocketException(Operation operation) {
        switch (operation){
            case GET:
                return true;
            case POST:
            case PUT:
            case DELETE:
                return retryOnSocketExceptions;
            default:
                return false;
        }
    }
    
    
    /**
     * Walk through the Throwable cause-tree and return it as a list
     * @param throwable the throwable
     * @return a list of all the throwables that led to this throwables
     */
    private List<Throwable> getCauses(Throwable throwable) {
        List<Throwable> result = new ArrayList<>();
        Throwable current = throwable;
        while (current != null) {
            result.add(current);
            current = current.getCause();
        }
        return result;
    }
    
    
    /**
     * If the exception is a rate-limit, then sleep for the defined time and return true.
     * Otherwise return false immediately
     * <p>
     * The duration of sleep will be
     * <p>
     * long sleepTimeMillis = minSleepMillis + Math.round(Math.random() * sleepVariationMillis);
     *
     * @param e the web application exception
     * @return true if the error was a rate limit
     * @see #minSleepMillis
     * @see #sleepVariationMillis
     */
    private boolean rateLimitSleep(WebApplicationException e, URI currentURI) {
        if (429 == e.getResponse().getStatusInfo().getStatusCode()) {
            sleep("Received response status 429(rate-limiting) for '" + currentURI + "'");
            return true;
        }
        return false;
    }
    
    
    private void sleep(String s) {
        long sleepTimeMillis = minSleepMillis + Math.round(Math.random() * sleepVariationMillis);
        log.warn(s + ", so backing off for {} seconds", Math.round(sleepTimeMillis / 1000.0));
        try {
            Thread.sleep(sleepTimeMillis);
        } catch (InterruptedException ex) {
        }
    }
    
    /**
     * Use this if you need invalidate a possible cache entry.
     * An example usecase would be:
     *  after a DELETE request, you want to invalidate the cache entry for the same resource
     *  so that future GET requests will not get the not-deleted cached copy.
     *
     * @param currentURI the URI to invalidate cache for.
     */
    protected void invalidateCacheEntry(URI currentURI) {
        cache.invalidate(currentURI);
    }
    
    
}
