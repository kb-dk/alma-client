package dk.kb.alma.client;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dk.kb.alma.client.exceptions.AlmaConnectionException;
import dk.kb.alma.client.exceptions.AlmaKnownException;
import dk.kb.alma.client.exceptions.AlmaNotFoundException;
import dk.kb.alma.client.exceptions.AlmaUnknownException;
import dk.kb.alma.client.utils.HttpUtils;
import dk.kb.alma.client.utils.Invocation;
import dk.kb.alma.gen.web_service_result.Error;
import dk.kb.alma.gen.web_service_result.WebServiceResult;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.impl.UriBuilderImpl;
import org.apache.cxf.transport.http.HTTPConduit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.RedirectionException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

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
                      Map<String, String> globalParams,
                      int connectTimeout,
                      int readTimeout,
                      long cacheTimeMillis,
                      Integer maxRetries) throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        this.target               = target;
        this.minSleepMillis       = minSleep;
        this.sleepVariationMillis = sleepVariation;
        this.globalParams         = globalParams;
        
        this.connectTimeout = connectTimeout;
        this.readTimeout    = readTimeout;
        this.maxRetries     = Optional.ofNullable(maxRetries).orElse(3);
        
        int cacheSize = 1000;
        cache = CacheBuilder
                .newBuilder()
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
     *
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
     * This is always enabled for GET requests. This parameter controls whether or not we also retry for non-GET
     * requests
     *
     * @param retryOnTimeouts should we retry non-GET requests that time out?
     */
    public void setRetryOnTimeouts(boolean retryOnTimeouts) {
        this.retryOnTimeouts = retryOnTimeouts;
    }
    
    
    public boolean isRetryOnSocketExceptions() {
        return retryOnSocketExceptions;
    }
    
    /**
     * retryOnSocketExceptions control whether or not we automatically retry non-GET requests that fail on a socket
     * exception.
     * This is always enabled for GET requests. This parameter controls whether or not we also retry for non-GET
     * requests
     *
     * @param retryOnSocketExceptions should we retry non-GET requests that fail out?
     */
    public void setRetryOnSocketExceptions(boolean retryOnSocketExceptions) {
        this.retryOnSocketExceptions = retryOnSocketExceptions;
    }
    
    public boolean isRetryOn429() {
        return retryOn429;
    }
    
    /**
     * retryOn429 control whether or not we automatically retry non-GET requests that receive ALMA HTTP 429 (rate
     * limit)
     * errors.
     * This is always enabled for GET requests. This parameter controls whether or not we also retry for non-GET
     * requests
     *
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
        
        JacksonJsonProvider jacksonJaxbJsonProvider = new JacksonJaxbJsonProvider();
        jacksonJaxbJsonProvider.disable(DeserializationFeature.UNWRAP_ROOT_VALUE);
        jacksonJaxbJsonProvider.disable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        jacksonJaxbJsonProvider.enable(DeserializationFeature.WRAP_EXCEPTIONS);
        
        jacksonJaxbJsonProvider.enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature());
        jacksonJaxbJsonProvider.enable(JsonParser.Feature.IGNORE_UNDEFINED);
        
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
        
        //client = client
        //        .accept(MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_XML_TYPE)
        //        .type(MediaType.APPLICATION_JSON_TYPE);
    
        client = client
                .accept(MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE)
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
        return invokeDirect(link, type, entity, Operation.PUT);
    }
    
    public <T, E> T post(final WebClient link, Class<T> type, E entity)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return invokeDirect(link, type, entity, Operation.POST);
    }
    
    public <T> T delete(final WebClient link, Class<T> type)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return invokeDirect(link, type, null, Operation.DELETE);
    }
    
    
    //Actual implementation
    
    /**
     * Attempt to fetch the requested resource from the cache. If not found, fetches it from ACTUAL
     *
     * @param uri       the URI to fetch
     * @param type      the Class of the result
     * @param entity    the body. Can be null
     * @param useCache  if false, will bypass cache
     * @param operation the HTTP operation (GET, POST,...)
     * @param <T>       the type of the result
     * @param <E>       the type of the body entity
     * @return the resulting java object, either from cache or from the actual server
     * @throws AlmaConnectionException if we failed on a deeper level, like the connection
     * @throws AlmaKnownException      if we failed on a documented API error code
     * @throws AlmaUnknownException    if we failed on a higher level, but not in a documented way
     */
    protected <T, E> T invokeCache(final WebClient uri, Class<T> type, E entity, boolean useCache, Operation operation)
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
            T value = invokeDirect(uri, type, entity, operation);
            
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
     * @param <T>       the type of the result
     * @param <E>       the type of the entity
     * @param uri       the uri to invoke
     * @param type      the class of the result
     * @param entity    the body entity. Can be null
     * @param operation the operation (GET, POST,...)
     * @return the result
     * @throws AlmaConnectionException if we failed on a deeper level, like the connection
     * @throws AlmaKnownException      if we failed on a documented API error code
     * @throws AlmaUnknownException    if we failed on a higher level, but not in a documented way
     */
    @Nullable
    protected <T, E> T invokeDirect(final WebClient uri, Class<T> type, E entity, Operation operation)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return invokeDirect(new Invocation<>(uri, type, entity, operation, maxRetries));
    }
    
    @Nullable
    protected <T, E> T invokeDirect(Invocation<T, E> invocation)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        
        //Remove the api key from the query string. This is something we handle here, not something you should set
        removeAuth(invocation.getUri());
        
        URI currentURI = invocation.getUri().getCurrentURI();
        log.debug("{}ing on {}", invocation.getOperation(), currentURI);
        T value;
        try (invocation) {
            Invocation<T, E> retryInvocation;
        
            try {
                WebClient webClient = addAuth(invocation.getUri());
                value = webClient.invoke(invocation.getOperation().name(),
                                         invocation.getEntity(),
                                         invocation.getType());
                log.trace("{}ed on {}", invocation.getOperation(), currentURI);
                if (value == null && (invocation.getType() == null || Void.TYPE == invocation.getType())){
                    log.warn("Beware: Returning null from invocation {} but caller expected type {}", invocation, invocation.getType());
                }
                return value;
            } catch (Fault | ProcessingException e) {
                //I am not entirely sure that Fault can reach this far, without being converted to a ProcessingException,
                // but better safe than sorry
            
                //This throws exception if we should NOT retry
                retryInvocation = handleProcessingException(invocation, e);
            } catch (RedirectionException e) {
                //This throws exception if we should NOT retry
                retryInvocation = handleRedirection(invocation, e);
            } catch (WebApplicationException e) {
                //This throws exception if we should NOT retry
                retryInvocation = handleWebApplicationException(invocation, e);
            }
        
            return invokeDirect(retryInvocation);
        }
    }
    
    
    private <T, E> Invocation<T, E> handleProcessingException(Invocation<T, E> invocation, RuntimeException e)
            throws AlmaConnectionException {
        final Operation operation = invocation.getOperation();
        final WebClient uri = invocation.getUri();
        final URI currentURI = uri.getCurrentURI();
        
        //Exception if we should NOT retry
        Supplier<AlmaConnectionException> almaConnectionExceptionSupplier = () -> new AlmaConnectionException(
                "Failed to " + operation.name() + "ing '" + currentURI + "'",
                e);
        
        invocation.decrementRetryCount(almaConnectionExceptionSupplier);
        
        
        //This checks if any exception in the hierachy is a socket timeout exception.
        List<Throwable> causes = HttpUtils.getCauses(e);
        if (shouldRetryOnTimeout(operation) && causes
                .stream()
                .anyMatch(cause -> cause instanceof SocketTimeoutException)) {
            //Multiple things, like SSL and ordinary reads and connects can cause SocketTimeouts, but at
            // different levels of the hierachy
            log.trace("Socket timeout for " + operation.name() + " on " + currentURI, e);
            sleep("Socket timeout exception for '" + currentURI + "'");
            
            HttpUtils.extendTimeouts(operation, uri, currentURI);
            
        } else if (shouldRetryOnSocketException(operation) && causes
                .stream()
                .anyMatch(cause -> cause instanceof SocketException)) {
            
            log.trace("Socket Exception for " + operation.name() + " on " + currentURI, e);
            sleep("Socket exception for '" + currentURI + "'");
            
        } else {
            throw almaConnectionExceptionSupplier.get();
        }
        
        return invocation;
    }
    
    private <T, E> Invocation<T, E> handleWebApplicationException(Invocation<T, E> invocation,
                                                                  WebApplicationException e)
            throws AlmaConnectionException, AlmaUnknownException, AlmaKnownException {
        final Operation operation = invocation.getOperation();
        final URI currentURI = invocation.getUri().getCurrentURI();
        final E entity = invocation.getEntity();
        
        if (shouldRetryOn429(operation) && rateLimitSleep(e, currentURI)) {
            //Do not increment retryCount as 429's should be retried forever. They do not count as errors with
            // a limited number of retries
            return invocation;
        }
        
        String entityMessage = HttpUtils.formatEntityMessage(entity, e);
        
        Response response = HttpUtils.getResponse(e);
        WebServiceResult result = HttpUtils.readWebServiceResult(operation, currentURI, e, entityMessage, response);
        Error firstError = HttpUtils.getFirstError(result);
        
        return handleErrorCodes(invocation, e, entityMessage, response, result, firstError);
    }
    
    
    private <T, E> Invocation<T, E> handleRedirection(Invocation<T, E> invocation, RedirectionException e) {
        URI redirectLocation = e.getLocation();
        log.debug("Redirecting {} to {}", invocation.getOperation(), redirectLocation.getPath());
        if (redirectLocation.isAbsolute()) {
            return invocation.withNewUri(getWebClient(redirectLocation));
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
            return invocation.withNewUri(newLink);
        }
    }
    
    
    private <T, E> Invocation<T, E> handleErrorCodes(Invocation<T, E> invocation,
                                                     WebApplicationException e,
                                                     String entityMessage,
                                                     Response response,
                                                     WebServiceResult result,
                                                     Error error) throws AlmaKnownException {
        
        Supplier<AlmaKnownException> defaultException = () -> new AlmaKnownException(invocation.getOperation().name(),
                                                                                     entityMessage,
                                                                                     invocation
                                                                                             .getUri()
                                                                                             .getCurrentURI(),
                                                                                     response,
                                                                                     result,
                                                                                     e);
        if (error != null) {
            switch (error.getErrorCode()) {
                case "NOT_FOUND":
                    throw new AlmaNotFoundException(invocation.getOperation().name(),
                                                    entityMessage,
                                                    invocation.getUri().getCurrentURI(),
                                                    response,
                                                    result,
                                                    e);
                case "GENERAL_ERROR":
                    // Full message seems to be "503 Service Unavailable - General error with the API Gateway – please retry later."
                    if (error.getErrorMessage().contains("please retry later")) {
                        invocation.decrementRetryCount(defaultException);
                        sleep("ALMA responded '" + error.getErrorMessage() + "'");
                        return invocation;
                    }
                    break;
            }
        }
        throw defaultException.get();
    }
    
    
    private boolean shouldRetryOn429(Operation operation) {
        switch (operation) {
            case GET:
                return true;
            case POST:
            case PUT:
            case DELETE:
                return retryOn429;
            default:
                return false;
        }
    }
    
    private boolean shouldRetryOnTimeout(Operation operation) {
        switch (operation) {
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
        switch (operation) {
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
     * after a DELETE request, you want to invalidate the cache entry for the same resource
     * so that future GET requests will not get the not-deleted cached copy.
     *
     * @param currentURI the URI to invalidate cache for.
     */
    protected void invalidateCacheEntry(URI currentURI) {
        cache.invalidate(currentURI);
    }
    
    
}
