package dk.kb.alma;

import com.google.common.collect.Iterables;
import dk.kb.alma.gen.Bib;
import dk.kb.alma.gen.Bibs;
import dk.kb.alma.gen.CodeTable;
import dk.kb.alma.gen.Item;
import dk.kb.alma.gen.ItemData;
import dk.kb.alma.gen.Items;
import dk.kb.alma.gen.PickupLocationTypes;
import dk.kb.alma.gen.RequestTypes;
import dk.kb.alma.gen.RequestedResource;
import dk.kb.alma.gen.ResourceSharing;
import dk.kb.alma.gen.User;
import dk.kb.alma.gen.UserRequest;
import dk.kb.alma.gen.UserRequests;
import org.apache.cxf.jaxrs.client.WebClient;

import javax.xml.datatype.XMLGregorianCalendar;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class AlmaClient extends AlmaRestClient {
    
    
    private final int batchSize;
    
    public AlmaClient(String almaTarget,
                      String alma_apikey,
                      int batchSize,
                      long minSleep,
                      long sleepVariation,
                      String lang)
            throws AlmaConnectionException {
        super(almaTarget, alma_apikey, minSleep, sleepVariation, lang);
        this.batchSize = batchSize;
    }
    
    public AlmaClient(String almaTarget,
                      String alma_apikey,
                      int batchSize,
                      long minSleep,
                      long sleepVariation,
                      String lang,
                      int connectTimeout,
                      int readTimeout) throws AlmaConnectionException {
        super(almaTarget, alma_apikey, minSleep, sleepVariation, lang, connectTimeout, readTimeout);
        this.batchSize = batchSize;
    }
    
    public <T> T get(final String link, Class<T> type) throws AlmaConnectionException {
        return get(getWebClient(link), type);
    }
    
    public Iterator<RequestedResource> getRequestedResourceIterator(String libraryId, String circulationDeskName,
                                                                    boolean allOrNothing) {
        
        Function<Integer, Iterator<RequestedResource>> nextIteratorFunction =
                offset -> getBatchOfRequestedResources(batchSize, offset, libraryId, circulationDeskName, allOrNothing);
        
        return new AutochainingIterator<>(nextIteratorFunction);
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
    
    
    public Set<Bib> getBibs(Set<String> bibIDs) {
        
        Iterable<List<String>> partition = Iterables.partition(bibIDs, batchSize);
        return StreamSupport.stream(partition.spliterator(), true)
                            .map(partion -> {
                                String partionBibIDs = String.join(",", partion);
                                return get(constructLink()
                                                            .path("/bibs/")
                                                            .query("mms_id", partionBibIDs)
                                                            .query("view", "full")
                                                            .query("expand", "None"),
                                                    Bibs.class);
                            })
                            .filter(Objects::nonNull)
                            .filter(bibs -> bibs.getBibs() != null)
                            .flatMap(bibs -> bibs.getBibs().stream())
                            .collect(Collectors.toSet());
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
        
        invalidateCacheEntry(currentURI);
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
