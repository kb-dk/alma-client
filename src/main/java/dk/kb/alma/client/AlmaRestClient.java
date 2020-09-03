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
import dk.kb.alma.gen.general.General;
import dk.kb.alma.gen.web_service_result.WebServiceResult;
import dk.kb.alma.gen.requested_resources.RequestedResource;
import dk.kb.alma.gen.requested_resources.RequestedResources;
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
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AlmaRestClient {
    
    protected final static Logger log = LoggerFactory.getLogger(AlmaRestClient.class);
    
    
    public static final String APIKEY = "apikey";
    
    private final String alma_apikey;
    
    
    //Select the right one based on which part of the world you are in
    private final String almaTarget;
    
    private final Cache<URI, Object> cache;
    
    
    private final String lang;
    private final long minSleepMillis;
    private final long sleepVariationMillis;
    
    private final String almaEnvType;
    private final String almaHost;
    
    private final int connectTimeout;
    private final int readTimeout;

    private boolean cachingEnabled = true;

    public AlmaRestClient(String almaTarget, String alma_apikey)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        this(almaTarget, alma_apikey,
                2000,
                3000,
                "da",
                30000,
                30000,
                TimeUnit.HOURS.toMillis(1));
    }

    public AlmaRestClient(String almaTarget, String alma_apikey, long minSleep, long sleepVariation, String lang)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        this(almaTarget, alma_apikey, minSleep, sleepVariation, lang, 3000, 3000, TimeUnit.HOURS.toMillis(5));
    }
    
    public AlmaRestClient(String almaTarget,
                          String alma_apikey,
                          long minSleep,
                          long sleepVariation,
                          String lang,
                          int connectTimeout,
                          int readTimeout,
                          long cacheTimeMillis)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        this.almaTarget = almaTarget;
        this.alma_apikey = alma_apikey;
        this.minSleepMillis = minSleep;
        this.sleepVariationMillis = sleepVariation;
        this.lang = lang;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        
        int cacheSize = 1000;
        cache = CacheBuilder.newBuilder()
                            .maximumSize(cacheSize)
                            .expireAfterAccess(cacheTimeMillis, TimeUnit.MILLISECONDS)
                            .build();
        
        
        
        log.debug("Getting ALMA general info to determine alma host");
        General almaGeneral = get(constructLink().path("/conf/general"), General.class);
        this.almaEnvType = almaGeneral.getEnvironmentType();
        try {
            this.almaHost = new URL(almaGeneral.getAlmaUrl()).getHost();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        log.info("Initialized {} with alma ENV={}, alma Host={}",
                 getClass().getName(),
                 this.almaEnvType,
                 this.almaHost);
        
    }
    
    public WebClient constructLink() {
        return getWebClient(almaTarget);
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
        if (lang != null) {
            client = client.replaceQueryParam("lang", lang);
        }
        
        
        return client;
    }
    
    public String getAlmaEnvType() {
        return almaEnvType;
    }
    
    public String getAlmaHost() {
        return almaHost;
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
    
    protected <T, E> T invokeCache(final WebClient link,
                                   Class<T> type,
                                   E entity,
                                   boolean useCache,
                                   Operation operation)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        
        //Only use cache on getRequests
        useCache &= operation == Operation.GET;
    
        //Remove the api key from the query string. This is something we handle here, not something you should set
        link.replaceQueryParam(APIKEY);
        URI currentURI = link.getCurrentURI();
    
        try {
            if (useCache) {
                Object cacheValue = cache.getIfPresent(currentURI);
                if (type.isInstance(cacheValue)) {
                    log.debug("cache hit on {}", currentURI);
                    return (T) cacheValue;
                }
            }
    
            //It is possible for multiple threads to run here, potentially getting the same URI concurrently
            T value = invokeDirect(link, type, entity, operation);
    
            if (useCache) {
                cache.put(currentURI, value);
            }
            return value;
        } finally {
            link.close();
        }
    }
    
    
    protected <T,E> T invokeDirect(final WebClient link, Class<T> type, E entity, Operation operation) {
    
        //Remove the api key from the query string. This is something we handle here, not something you should set
        link.replaceQueryParam(APIKEY);
    
        URI currentURI = link.getCurrentURI();
        log.debug("{}ing on {}", operation, currentURI);
        T value;
        try {
            WebClient webClient = addAPIKey(link);
            value = webClient.invoke(operation.name(), entity, type);
            log.trace("{}ed on {}", operation, currentURI);
        } catch (Fault | ProcessingException e) {
            //I am not entirely sure that Fault can reach this far, without being converted to a ProcessingException,
            // but better safe than sorry
        
            //This checks if any exception in the hierachy is a socket timeout exception.
            List<Throwable> causes = getCauses(e);
            if (causes.stream().anyMatch(cause -> cause instanceof SocketTimeoutException)) {
                //Multiple things, like SSL and ordinary reads and connects can cause SocketTimeouts, but at
                // different levels of the hierachy
                //TODO should we run a counter to avoid eternal retries?
                log.trace("Socket timeout for " + operation.name() + " on " + currentURI, e);
                sleep("Socket timeout exception for '" + currentURI + "'");
                HTTPClientPolicy clientPolicy = WebClient.getConfig(link).getHttpConduit().getClient();
                clientPolicy.setConnectionTimeout(clientPolicy.getConnectionTimeout() * 2);
                clientPolicy.setReceiveTimeout(clientPolicy.getReceiveTimeout() * 2);
                clientPolicy.setConnectionRequestTimeout(clientPolicy.getConnectionRequestTimeout() * 2);
                log.debug("Increased timeouts to connect={}ms and receive={}ms for the {}ing of {}",
                          clientPolicy.getConnectionTimeout(),
                          clientPolicy.getReceiveTimeout(),
                          operation.name(),
                          currentURI);
            
            
                return invokeDirect(link, type, entity, operation);
            } else {
                throw new AlmaConnectionException("Failed to " + operation.name() + "ing '" + currentURI + "'", e);
            }
        } catch (RedirectionException e) {
            URI redirectLocation = e.getLocation();
            log.debug("Redirecting {} to {}", operation, redirectLocation.getPath());
            if (redirectLocation.isAbsolute()) {
                return invokeDirect(getWebClient(redirectLocation), type, entity, operation);
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
                return invokeDirect(newLink, type, entity, operation);
            }
        } catch (WebApplicationException e) {
            if (rateLimitSleep(e, currentURI)) {
                return invokeDirect(link, type, entity, operation);
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
            link.close();
        }
        return value;
    
    }
    
    private WebClient addAPIKey(WebClient link) {
        return link.replaceQueryParam(APIKEY, alma_apikey);
    }
    
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
    
    
    
    protected void invalidateCacheEntry(URI currentURI) {
        cache.invalidate(currentURI);
    }


    public boolean isCachingEnabled() {
        return cachingEnabled;
    }

    public void setCachingEnabled(boolean cachingEnabled) {
        this.cachingEnabled = cachingEnabled;
    }
}
