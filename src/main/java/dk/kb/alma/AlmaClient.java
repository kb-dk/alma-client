package dk.kb.alma;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.Striped;
import dk.kb.alma.gen.Bib;
import dk.kb.alma.gen.CodeTable;
import dk.kb.alma.gen.General;
import dk.kb.alma.gen.Item;
import dk.kb.alma.gen.ItemData;
import dk.kb.alma.gen.Items;
import dk.kb.alma.gen.PickupLocationTypes;
import dk.kb.alma.gen.RequestTypes;
import dk.kb.alma.gen.ResourceSharing;
import dk.kb.alma.gen.User;
import dk.kb.alma.gen.UserRequest;
import dk.kb.alma.gen.UserRequests;
import dk.kb.alma.gen.WebServiceResult;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.ClientProperties;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.RedirectionException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

public class AlmaClient {

    protected final static Logger log = LoggerFactory.getLogger(AlmaClient.class);


    public static final String APIKEY = "apikey";

    private final String alma_apikey;


    //Select the right one based on which part of the world you are in
    private final String almaTarget;

    private final Cache<URI, Object> cache;
    private final Striped<Lock> locks;


    private final String lang;
    private final long minSleepMillis;
    private final long sleepVariationMillis;

    private final String almaEnvType;
    private final String almaHost;

    private final int connectTimeout;
    private final int readTimeout;
    
    public AlmaClient(String almaTarget, String alma_apikey, long minSleep, long sleepVariation, String lang)
            throws AlmaConnectionException {
        this(almaTarget,alma_apikey, minSleep, sleepVariation,lang, 3000,3000);
    }
    
    public AlmaClient(String almaTarget, String alma_apikey, long minSleep, long sleepVariation, String lang, int connectTimeout, int readTimeout)
        throws AlmaConnectionException {
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
                            .expireAfterAccess(5, TimeUnit.HOURS)
                            .build();

        locks = Striped.lock(cacheSize);


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
        URI host = UriBuilder.fromUri(link).replaceQuery(null).replacePath(null).replaceMatrix(null).build();
    
        
        WebClient client = WebClient.create(host);
        HTTPConduit conduit = WebClient.getConfig(client).getHttpConduit();
        conduit.getClient().setConnectionTimeout(connectTimeout);
        conduit.getClient().setReceiveTimeout(readTimeout);
        
        if (link.getPath() != null) {
            client = client.path(link.getPath());
        }
        if (link.getQuery() != null) {
            client = client.replaceQuery(link.getQuery());
        }

        client = client.accept(MediaType.APPLICATION_XML_TYPE).type(MediaType.APPLICATION_XML_TYPE);
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

    public <T> T get(final WebClient link, Class<T> type) throws AlmaConnectionException {
        return get(link, type, true);
    }

    public <T> T get(final WebClient link, Class<T> type, boolean useCache) throws AlmaConnectionException {
        return invoke(link, type, null, useCache, Operation.GET);
    }

    public <T, E> T put(final WebClient link, Class<T> type, E entity) throws AlmaConnectionException {
        return invoke(link, type, entity, false, Operation.PUT);
    }

    public <T, E> T post(final WebClient link, Class<T> type, E entity) throws AlmaConnectionException {
        return invoke(link, type, entity, false, Operation.POST);
    }

    public <T> T delete(final WebClient link, Class<T> type) throws AlmaConnectionException {
        return invoke(link, type, null, false, Operation.DELETE);
    }

    protected <T, E> T invoke(final WebClient link, Class<T> type, E entity, boolean useCache, Operation operation)
        throws AlmaConnectionException {

        //Remove the api key from the query string. This is something we handle here, not something you should set
        link.replaceQueryParam(APIKEY);

        useCache &= operation == Operation.GET;

        URI currentURI = link.getCurrentURI();
        Lock lock = locks.get(currentURI);
        lock.lock();
        try {
            if (useCache) {
                Object cacheValue = cache.getIfPresent(currentURI);
                if (type.isInstance(cacheValue)) {
                    log.debug("cache hit on {}", currentURI);
                    return (T) cacheValue;
                }
            }
            log.debug("{}ing on {}", operation, currentURI);
            T value;
            try {
                WebClient webClient = link.replaceQueryParam(APIKEY, alma_apikey);
                value = webClient.invoke(operation.name(), entity, type);
                log.trace("{}ed on {}", operation, currentURI);
            } catch (Fault | ProcessingException e){
                //I am not entirely sure that Fault can reach this far, without being converted to a ProcessingException,
                // but better safe than sorry
                
                //This checks if any exception in the hierachy is a socket timeout exception.
                List<Throwable> causes = getCauses(e);
                if (causes.stream().anyMatch(cause -> cause instanceof SocketTimeoutException)) {
                    sleep("Socket timeout exception for '"+currentURI+"'");
                    return invoke(link, type, entity, useCache, operation);
                } else {
                    throw new RuntimeException("Failed to retrieve '" + currentURI + "'", e);
                }
            } catch (RedirectionException e) {
                URI redirectLocation = e.getLocation();
                log.debug("Redirecting {} to {}", operation, redirectLocation.getPath());
                if (redirectLocation.isAbsolute()) {
                    return invoke(getWebClient(redirectLocation), type, entity, useCache, operation);
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
                    return invoke(newLink, type, entity, useCache, operation);
                }
            } catch (WebApplicationException e) {
                if (rateLimitSleep(e, currentURI)) {
                    return invoke(link, type, entity, useCache, operation);
                }

                String entityMessage = "";
                if (entity != null) {
                    try {
                        entityMessage = "with entity '" + Utilities.toXml(entity) + "' ";
                    } catch (JAXBException jaxbException) {
                        throw new RuntimeException(jaxbException);
                    }
                }

                Response response = e.getResponse();
                WebServiceResult result;
                try {
                    result = response.readEntity(WebServiceResult.class);
                } catch (Exception e2) {
                    throw new AlmaConnectionException(
                        "Failed to " + operation + " " + entityMessage + "on '" + currentURI
                        + "', and failed to parse out out webservice result from ALMA reply",
                        e);
                }
                if (result.isErrorsExist()) {
                    String errorMessage = result.getErrorList()
                                                .getErrors()
                                                .stream()
                                                .map(error -> error.getErrorMessage())
                                                .collect(
                                                    Collectors.joining(", "));
                    String errorCode = result.getErrorList()
                                             .getErrors()
                                             .stream()
                                             .findFirst()
                                             .map(error -> error.getErrorCode())
                                             .orElseGet(null);


                    throw new AlmaConnectionException(
                        "Failed to " + operation + " " + entityMessage + "on '" + currentURI + "' with errormessage '"
                        + errorMessage
                        + "' and errorcode '" + errorCode + "'", e);

                } else {
                    throw new AlmaConnectionException(
                        "Failed to " + operation + " " + entityMessage + "on '" + currentURI + "' with response '"
                        + response + "'", e);
                }
            }
            if (useCache) {
                cache.put(currentURI, value);
            }
            return value;
        } finally {
            link.close();
            lock.unlock();
        }
    }
    
    private List<Throwable> getCauses(Throwable throwable) {
        List<Throwable> result = new ArrayList<>();
        Throwable current = throwable;
        while (current != null){
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
            sleep("Received response status 429(rate-limiting) for '"+currentURI+"'");
            return true;
        }
        return false;
    }
    
    
    private void sleep(String s) {
        long sleepTimeMillis = minSleepMillis + Math.round(Math.random() * sleepVariationMillis);
        log.warn(s+", so backing off for {} seconds", Math.round(sleepTimeMillis / 1000.0));
        try {
            Thread.sleep(sleepTimeMillis);
        } catch (InterruptedException ex) {
        }
    }

    public Item getItem(String barcode) throws AlmaConnectionException {
        return get(constructLink()
                       .path("/items/")
                       .query("item_barcode", barcode),
                   Item.class);
    }


    public Bib getBib(String mmsID) throws AlmaConnectionException {
        return get(constructLink()
                       .path("/bibs/")
                       .path(mmsID),
                   Bib.class);
    }


    public User getUser(String userID) throws AlmaConnectionException {
        return get(constructLink()
                       .path("/users/")
                       .path(userID),
                   User.class);
    }

    public UserRequests getRequests(String mmsID) throws AlmaConnectionException {
        return get(constructLink()
                       .path("/bibs/")
                       .path(mmsID)
                       .path("/requests/"),
                   UserRequests.class);
    }

    public UserRequest getRequest(String userId, String requestId) throws AlmaConnectionException {
        return get(constructLink()
                       .path("/users/")
                       .path(userId)
                       .path("/requests/")
                       .path(requestId),
                   UserRequest.class);
    }

    public ResourceSharing getResourceSharingRequest(String userId, String requestId) throws AlmaConnectionException {
        return get(constructLink().path("/users/")
                                  .path(userId)
                                  .path("/resource-sharing-requests/")
                                  .path(requestId),
                   ResourceSharing.class);
    }


    public Items getItems(String bibId, String holdingId) throws AlmaConnectionException {
        int limit = 100;
        Items items = get(constructLink().path("/bibs/")
                                         .path(bibId)
                                         .path("/holdings/")
                                         .path(holdingId)
                                         .path("/items/")
                                         .query("limit", limit),
                          Items.class);


        if (items != null && items.getItems().size() == limit) {
            log.warn("Retrieved max number of items ({}) for record '{}', holding '{}'. There might be more..",
                     limit,
                     bibId,
                     holdingId);
        }
        return items;
    }

    public Bib.Holdings getBibHoldings(String bibId) throws AlmaConnectionException {
        return get(constructLink()
                       .path("/bibs/")
                       .path(bibId)
                       .path("/holdings/"),
                   Bib.Holdings.class);

    }

    public Bib getBibRecord(String bibId) throws AlmaConnectionException {
        return get(constructLink()
                       .path("/bibs/")
                       .path(bibId),
                   Bib.class);
    }

    public Item createItem(String bibId,
                           String holdingId,
                           String barcode,
                           String description,
                           String pages,
                           String year) throws AlmaConnectionException {
        WebClient link = constructLink().path("/bibs/")
                                        .path(bibId)
                                        .path("/holdings/")
                                        .path(holdingId)
                                        .path("/items/");


        Item item = new Item();
        ItemData itemData = new ItemData();
        itemData.setBarcode(barcode);
        itemData.setDescription(description);
        itemData.setPages(pages);
        itemData.setYearOfIssue(year);
        //TODO: set baseStatus
        item.setItemData(itemData);

        return post(link, Item.class, item);
    }

    public Item updateItem(Item item) throws AlmaConnectionException {
        WebClient link = constructLink().path("/bibs/")
                                        .path(item.getBibData().getMmsId())
                                        .path("/holdings/")
                                        .path(item.getHoldingData().getHoldingId())
                                        .path("/items/")
                                        .path(item.getItemData().getPid());

        return put(link, Item.class, item);

    }


    public Bib updateBibRecord(Bib record) throws AlmaConnectionException {
        WebClient link = constructLink().path("/bibs/")
                                        .path(record.getMmsId());


        return put(link, Bib.class, record);
    }


    /**
     * Cancel request in Alma
     *
     * @param userId     Id of the user with the request
     * @param requestId  The request id
     * @param reasonCode Code of the cancel reason. Must be a value from the code table 'RequestCancellationReasons'
     * @param notifyUser Indication of whether the user should be notified
     * @return True if the request is cancelled successfully. False if the request was not found.
     * @throws AlmaConnectionException if something went wrong
     */
    public boolean cancelRequest(String userId, String requestId, String reasonCode, boolean notifyUser)
        throws AlmaConnectionException {
        return cancelRequest(userId, requestId, reasonCode, null, notifyUser);
    }

    /**
     * Cancel request in Alma
     *
     * @param userId     Id of the user with the request
     * @param requestId  The request id
     * @param reasonCode Code of the cancel reason. Must be a value from the code table 'RequestCancellationReasons'
     * @param note       Additional note for the user
     * @param notifyUser Indication of whether the user should be notified
     * @return True if the request is cancelled successfully. False if the request was not found.
     */
    public boolean cancelRequest(String userId, String requestId, String reasonCode, String note, boolean notifyUser)
        throws AlmaConnectionException {
        WebClient link = constructLink().path("/users/")
                                        .path(userId)
                                        .path("/requests/")
                                        .path(requestId);
        URI currentURI = link.getCurrentURI();

        WebClient builder = link.query("reason", reasonCode)
                                .query("notify_user", notifyUser);

        if (note != null) {
            builder = builder.query("note", note);
        }
        delete(builder, Void.class);

        Lock lock = locks.get(currentURI);
        lock.lock();
        try {
            cache.invalidate(currentURI);
        } finally {
            lock.unlock();
        }
        return true;
    }

    /**
     * Create request for an item in alma
     *
     * @param userId             Id of the user
     * @param bibId              RecordId
     * @param holdingId          HoldingId
     * @param itemId             ItemId
     * @param pickupLocationCode A valid Alma pickupLocationCode
     * @param lastInterestDate   Last
     * @return
     */
    public UserRequest createRequest(String userId,
                                     String bibId,
                                     String holdingId,
                                     String itemId,
                                     String pickupLocationCode,
                                     XMLGregorianCalendar lastInterestDate) throws AlmaConnectionException {

        WebClient link = constructLink().path("/bibs/")
                                        .path(bibId)
                                        .path("/holdings/")
                                        .path(holdingId)
                                        .path("/items/")
                                        .path(itemId)
                                        .path("/requests/")
                                        .query("user_id", userId)
                                        .query("user_id_type", "all_unique");

        UserRequest userRequest = new UserRequest();
        userRequest.setRequestType(RequestTypes.HOLD);
        userRequest.setPickupLocationType(PickupLocationTypes.LIBRARY);
        userRequest.setPickupLocationLibrary(pickupLocationCode);
        userRequest.setLastInterestDate(lastInterestDate);
        return post(link, UserRequest.class, userRequest);
    }

    /**
     * Create request in alma
     *
     * @param request The fully populated request
     * @return The request created in Alma
     */
    public UserRequest createRequest(UserRequest request) throws AlmaConnectionException {

        String userId = request.getUserPrimaryId();
        String mmsId = request.getMmsId();
        String itemId = request.getItemId();

        WebClient link = constructLink().path("/users/")
                                        .path(userId)
                                        .path("/requests")
                                        .query("user_id_type", "all_unique");

        if (mmsId != null) {
            link = link.query("mms_id", mmsId);
        }
        if (itemId != null) {
            link = link.query("item_pid", itemId);
        }
        return post(link, UserRequest.class, request);
    }


    public ResourceSharing createResourceSharingRequest(ResourceSharing request, String userId)
        throws AlmaConnectionException {
        WebClient link = constructLink().path("/users/")
                                        .path(userId)
                                        .path("/resource-sharing-requests/");
        return post(link, ResourceSharing.class, request);

    }

    public UserRequests getItemRequests(String recordId, String holdingId, String itemId)
        throws AlmaConnectionException {

        WebClient link = constructLink().path("/bibs/")
                                        .path(recordId)
                                        .path("/holdings/")
                                        .path(holdingId)
                                        .path("/items/")
                                        .path(itemId)
                                        .path("/requests/");

        return get(link, UserRequests.class);
    }

    public CodeTable getCodeTable(String codeTableName) throws AlmaConnectionException {
        return getCodeTable(codeTableName, "da");
    }

    public CodeTable getCodeTable(String codeTableName, String lang) throws AlmaConnectionException {
        WebClient link = constructLink().path("/conf/code-tables/")
                                        .path(codeTableName)
                                        .replaceQueryParam("lang", lang);

        return get(link, CodeTable.class);
    }
    
    
}
