package dk.kb.alma.client;

import com.google.common.collect.Iterables;
import dk.kb.alma.client.AutochainingIterator.IteratorOffset;
import dk.kb.alma.client.exceptions.AlmaConnectionException;
import dk.kb.alma.client.exceptions.AlmaKnownException;
import dk.kb.alma.client.exceptions.AlmaUnknownException;
import dk.kb.alma.client.exceptions.MarcXmlException;
import dk.kb.alma.client.utils.MarcRecordHelper;
import dk.kb.alma.client.utils.NamedThread;
import dk.kb.alma.gen.Bib;
import dk.kb.alma.gen.Bibs;
import dk.kb.alma.gen.CodeTable;
import dk.kb.alma.gen.Item;
import dk.kb.alma.gen.ItemData;
import dk.kb.alma.gen.Items;
import dk.kb.alma.gen.Libraries;
import dk.kb.alma.gen.LinkingDetails;
import dk.kb.alma.gen.Portfolio;
import dk.kb.alma.gen.User;
import dk.kb.alma.gen.analytics.Report;
import dk.kb.alma.gen.holdings.Holdings;
import dk.kb.alma.gen.requested_resource.RequestedResource;
import dk.kb.alma.gen.user_request.PickupLocationTypes;
import dk.kb.alma.gen.user_request.RequestTypes;
import dk.kb.alma.gen.user_request.ResourceSharing;
import dk.kb.alma.gen.user_request.UserRequest;
import dk.kb.alma.gen.user_request.UserRequests;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
    
    private final Logger log = LoggerFactory.getLogger(AlmaClient.class);
    
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
                      int readTimeout,
                      long cacheTimeout) throws AlmaConnectionException {
        super(almaTarget, alma_apikey, minSleep, sleepVariation, lang, connectTimeout, readTimeout, cacheTimeout);
        this.batchSize = batchSize;
    }
    
    public <T> T get(final String link, Class<T> type) throws AlmaConnectionException,
                                                                      AlmaKnownException,
                                                                      AlmaUnknownException {
        return get(getWebClient(link), type);
    }
    
    public Iterator<RequestedResource> getRequestedResourceIterator(String libraryId, String circulationDeskName,
                                                                    boolean allOrNothing) {
        
        Function<Integer, Iterator<RequestedResource>> nextIteratorFunction =
                offset -> getBatchOfRequestedResources(batchSize, offset, libraryId, circulationDeskName, allOrNothing);
        
        return new AutochainingIterator<>(nextIteratorFunction);
    }
    
    
    public Item getItem(String barcode) throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return get(constructLink()
                           .path("/items")
                           .query("item_barcode", barcode),
                   Item.class);
    }
    
    
    public Bib getBib(String mmsID) throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return get(constructLink()
                           .path("/bibs/")
                           .path(mmsID),
                   Bib.class);
    }
    
    
    public Bib updateBib(Bib record) throws AlmaConnectionException {
        WebClient link = constructLink().path("/bibs/")
                                        .path(record.getMmsId());
        
        
        return put(link, Bib.class, record);
    }
    
    
    public Set<Bib> getBibs(Set<String> bibIDs)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        String threadName = Thread.currentThread().getName();
        Iterable<List<String>> partition = Iterables.partition(bibIDs, batchSize);
        return StreamSupport.stream(partition.spliterator(), true)
                            .map(NamedThread.namedThread(
                                    (List<String> partion) -> {
                                        String partionBibIDs = String.join(",", partion);
                                        return get(constructLink()
                                                           .path("/bibs/")
                                                           .query("mms_id", partionBibIDs)
                                                           .query("view", "full")
                                                           .query("expand", "None"),
                                                   Bibs.class);
                                    },
                                    partion -> threadName + "->" + "Bibs-from-" + partion.get(0) + "-to-" + partion.get(
                                            partion.size() - 1)))
                            .filter(Objects::nonNull)
                            .filter(bibs -> bibs.getBibs() != null)
                            .flatMap(bibs -> bibs.getBibs().stream())
                            .collect(Collectors.toSet());
    }
    
    
    public User getUser(String userID) throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return get(constructLink()
                           .path("/users/")
                           .path(userID),
                   User.class);
    }
    
    public UserRequests getRequests(String mmsID)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return get(constructLink()
                           .path("/bibs/")
                           .path(mmsID)
                           .path("/requests"),
                   UserRequests.class);
    }
    
    public UserRequest getRequest(String userId, String requestId)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return get(constructLink()
                           .path("/users/")
                           .path(userId)
                           .path("/requests/")
                           .path(requestId),
                   UserRequest.class);
    }
    
    public ResourceSharing getResourceSharingRequest(String userId, String requestId)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return get(constructLink().path("/users/")
                                  .path(userId)
                                  .path("/resource-sharing-requests/")
                                  .path(requestId),
                   ResourceSharing.class);
    }
    
    
    public Items getItems(String bibId, String holdingId)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        Items result = new Items();
        
        int limit = 100;
        int offset = 0;
        int hitCount = 1;
        while (true) {
            Items items = get(constructLink().path("/bibs/")
                                             .path(bibId)
                                             .path("/holdings/")
                                             .path(holdingId)
                                             .path("/items")
                                             .query("limit", limit)
                                             .query("offset", offset),
            
                              Items.class);
            result.getItems().addAll(items.getItems());
            offset += items.getItems().size();
            
            if (items.getItems().size() != limit || offset >= items.getTotalRecordCount()) {
                break;
            }
        }
        result.setTotalRecordCount(result.getItems().size());
        
        return result;
    }
    
    public Item getItem(String bibId, String holdingId, String itemId)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        
        Item item = get(constructLink().path("/bibs/")
                                       .path(bibId)
                                       .path("/holdings/")
                                       .path(holdingId)
                                       .path("/items/")
                                       .path(itemId),
                        Item.class);
        
        
        return item;
    }
    
    public Holdings getBibHoldings(String bibId)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return get(constructLink()
                           .path("/bibs/")
                           .path(bibId)
                           .path("/holdings"),
                   Holdings.class);
        
    }
    
    public Bib getBibRecord(String bibId) throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
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
                           String year) throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        WebClient link = constructLink().path("/bibs/")
                                        .path(bibId)
                                        .path("/holdings/")
                                        .path(holdingId)
                                        .path("/items");
        
        
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
    
    public Item updateItem(Item item) throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        WebClient link = constructLink().path("/bibs/")
                                        .path(item.getBibData().getMmsId())
                                        .path("/holdings/")
                                        .path(item.getHoldingData().getHoldingId())
                                        .path("/items/")
                                        .path(item.getItemData().getPid());
        
        return put(link, Item.class, item);
        
    }
    
    
    public Bib updateBibRecord(Bib record) throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
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
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
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
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
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
                                     XMLGregorianCalendar lastInterestDate)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        
        WebClient link = constructLink().path("/bibs/")
                                        .path(bibId)
                                        .path("/holdings/")
                                        .path(holdingId)
                                        .path("/items/")
                                        .path(itemId)
                                        .path("/requests")
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
    public UserRequest createRequest(UserRequest request)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        
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
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        WebClient link = constructLink().path("/users/")
                                        .path(userId)
                                        .path("/resource-sharing-requests");
        return post(link, ResourceSharing.class, request);
        
    }
    
    public UserRequests getItemRequests(String mmsId, String holdingId, String itemId)
            throws AlmaConnectionException {
        
        WebClient link = constructLink().path("/bibs/")
                                        .path(mmsId)
                                        .path("/holdings/")
                                        .path(holdingId)
                                        .path("/items/")
                                        .path(itemId)
                                        .path("/requests");
        
        return get(link, UserRequests.class);
    }
    
    public CodeTable getCodeTable(String codeTableName)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return getCodeTable(codeTableName, "da");
    }
    
    public CodeTable getCodeTable(String codeTableName, String lang)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        WebClient link = constructLink().path("/conf/code-tables/")
                                        .path(codeTableName)
                                        .replaceQueryParam("lang", lang);
        
        return get(link, CodeTable.class);
    }
    
    public UserRequest getItemRequest(String mmsId, String holdingId, String itemId, String request_id) {
        
        WebClient link = constructLink().path("/bibs/")
                                        .path(mmsId)
                                        .path("/holdings/")
                                        .path(holdingId)
                                        .path("/items/")
                                        .path(itemId)
                                        .path("/requests/")
                                        .path(request_id);
        
        return get(link, UserRequest.class);
    }
    
    
    public Portfolio getBibPortfolios(String bibId) throws AlmaConnectionException {
        return get(constructLink()
                           .path("/bibs/")
                           .path(bibId)
                           .path("/portfolios/"),
                   Portfolio.class);
        
    }
    
    
    //TODO do not use hardcoded values, use parameters
    public Portfolio createPortfolio(String bibId, Boolean multiVolume, String pdfLink, String publicNote)
            throws AlmaConnectionException {
        WebClient link = constructLink().path("/bibs/")
                                        .path(bibId)
                                        .path("/portfolios/");
        
        Portfolio portfolio = new Portfolio();
        if (multiVolume) {
            portfolio.setIsStandalone(false);
        }
        
        LinkingDetails ld = new LinkingDetails();
        ld.setUrl(pdfLink);
        portfolio.setLinkingDetails(ld);
        
        Portfolio.Availability avail = new Portfolio.Availability();
        avail.setDesc("Available");
        avail.setValue("11");
        portfolio.setAvailability(avail);
        
        portfolio.setPublicNote(publicNote);
        
        Portfolio.MaterialType materialType = new Portfolio.MaterialType();
        materialType.setValue("BOOK");
        materialType.setDesc("Book");
        portfolio.setMaterialType(materialType);
        
        return post(link, Portfolio.class, portfolio);
    }
    
    public Portfolio getPortfolio(String bibId, String portfolioId) throws AlmaConnectionException {
        return get(constructLink()
                           .path("/bibs/")
                           .path(bibId)
                           .path("/portfolios/")
                           .path(portfolioId),
                   Portfolio.class);
        
    }
    
    public Portfolio updatePortfolio(String bibId, String portfolioId) throws AlmaConnectionException {
        Portfolio pf = getPortfolio(bibId, portfolioId);
        
        WebClient link = constructLink()
                                 .path("/bibs/")
                                 .path(bibId)
                                 .path("/portfolios/")
                                 .path(portfolioId);
        
        return put(link, Portfolio.class, pf);
    }
    
    
    public Portfolio deletePortfolio(String bibId, String portfolioId) throws AlmaConnectionException {
        WebClient link = constructLink().path("/bibs/")
                                        .path(bibId)
                                        .path("/portfolios/")
                                        .path(portfolioId);
        
        return delete(link, Portfolio.class);
    }
    
    
    /**
     * Create a new basic Bib record with only 'Title' set (NewTitle). The record should be updated {@link
     * #updateBib(Bib)} with relevant values after creation, e.g. Leader, ControlFields and relevant DataFields
     *
     * @return The newly created record
     */
    public Bib createBib() throws AlmaConnectionException {
        Bib bib = new Bib();
        MarcRecordHelper.createRecordWithTitle(bib);
        WebClient link = constructLink().path("/bibs/");
        
        return post(link, Bib.class, bib);
        
    }
    
    public Bib deleteBib(String bibId) throws AlmaConnectionException {
        WebClient link = constructLink().path("/bibs/")
                                        .path(bibId);
        
        return delete(link, Bib.class);
    }
    
    public Item deleteItem(Item item, boolean force, boolean cleanEmptyHolding) {
        WebClient link = constructLink().path("/bibs/")
                .path(item.getBibData().getMmsId())
                .path("/holdings/")
                .path(item.getHoldingData().getHoldingId())
                .path("/items/")
                .path(item.getItemData().getPid())
                .query("override",force)
                .query("holdings",cleanEmptyHolding);
    
        return delete(link,Item.class);
    }
    
    
    
    /**
     * Set whether the record should be published to Primo or not. "true" as suppressValue means that the record will
     * NOT be published. The subfield 'u' of datafield 096 will be set to: "Kan ikke hjemlånes"
     *
     * @param bibId         The record Id of the record to suppress
     * @param suppressValue String value "true" means to suppress and "false" not to suppress
     * @return the Bib record
     */
    public Bib setSuppressFromPublishing(String bibId, String suppressValue) throws AlmaConnectionException {
        Bib record = getBib(bibId);
        try {
            org.marc4j.marc.Record marcRecord = MarcRecordHelper.getMarcRecordFromAlmaRecord(record);
            String suppress;
            if (suppressValue.equalsIgnoreCase("true")) {
                suppress = "true";
            } else if (suppressValue.equalsIgnoreCase("false")) {
                suppress = "false";
            } else {
                suppress = "false";
                log.warn("Suppress value must be 'true' or 'false'. It was: {}. Default set to 'false'", suppressValue);
            }
            
            record.setSuppressFromPublishing(suppress);
            
            if (suppress.equals("true")) {
                if (MarcRecordHelper.isSubfieldPresent(marcRecord, "DF096_TAG", 'u')) {
                    MarcRecordHelper.addSubfield(marcRecord, "DF096_TAG", 'u', "Kan ikke hjemlånes");
                }
            }
            MarcRecordHelper.saveMarcRecordOnAlmaRecord(record, marcRecord);
        } catch (MarcXmlException e) {
            log.info("Set supress failed ", e);
        }
        return updateBib(record);
    }
    
    
    public Libraries getLibraries() {
        return get(constructLink()
                           .path("/conf/libraries"),
                   Libraries.class);
    }
    
    /**
     * @param reportPath Full path to the report
     * @param filter     Optional.	An XML representation of a filter in OBI format ???
     * @param limit      Optional. Default: 25	Maximum number of results to return - between 25 and 1000 (multiples of
     *                   25)
     * @param col_names  Optional. Default: true	Include column heading information. Default: true. To ensure consistent
     *                   sort order it might be required to turn it off (col_names=false).
     * @return
     */
    public dk.kb.alma.client.analytics.Report startReport(
            @Nonnull String reportPath,
            @Nullable String filter,
            @Nullable Integer limit,
            @Nullable Boolean col_names) {
        filter = filter == null ? "" : filter;
        limit = restrictLimit(limit);
        col_names = col_names == null ? true : col_names;
        
        return dk.kb.alma.client.analytics.Report.parseFromAlmaReport(
                get(constructLink()
                            .path("/analytics/reports")
                            .query("path", reportPath)
                            .query("filter", filter)
                            .query("limit", limit)
                            .query("col_names", col_names),
                    Report.class,
                    false),
                null);
    }
    
    
    /**
     * @param report
     * @return
     * @throws IllegalArgumentException when you try to continue a report that is already finished
     */
    public dk.kb.alma.client.analytics.Report continueReport(dk.kb.alma.client.analytics.Report report)
            throws IllegalArgumentException {
        if (report.isFinished()) {
            throw new IllegalArgumentException("The report is finished, there is no more to get here");
        }
        final Report rawReport;
        try {
            rawReport = get(constructLink().path("/analytics/reports")
                                           .query("token", report.getToken()),
                            Report.class,
                            false);
        } catch (AlmaKnownException e) {
            //TODO This is a hack, but it seems that sometimes we miss isFinished...?
            if (e.getErrorCode().equals("420033") && e.getErrorMessage().equals("No more rows to fetch")) {
                throw new IllegalArgumentException("The report is finished, there is no more to get here, but somehow we did not see the finished flag???", e);
            } else {
                throw e;
            }
        }
        return dk.kb.alma.client.analytics.Report.parseFromAlmaReport(
                rawReport, //Important that cache is not used, as this is the same url being requested each time
                report);
    }
    
    /**
     * Transforms the input integer to be * positive * between 25 and 1000 * A multiple of 25
     *
     * @param limit
     * @return
     */
    protected static Integer restrictLimit(@Nullable Integer limit) {
        
        limit = (limit == null ? 25 : limit); //if null, set to 25
        
        limit = Math.max(0, limit); //Make positive
        
        final double multipleOf25 = Math.ceil(limit / 25.0); //Find the multiple of 25
        final long multipliedBack = (long) multipleOf25 * 25;
        limit = (int) Math.min(1000L, multipliedBack);
        
        return limit;
    }
    
    
}
