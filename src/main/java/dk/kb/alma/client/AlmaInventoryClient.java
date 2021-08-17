package dk.kb.alma.client;

import com.google.common.collect.Iterables;
import dk.kb.alma.client.exceptions.AlmaConnectionException;
import dk.kb.alma.client.exceptions.AlmaKnownException;
import dk.kb.alma.client.exceptions.AlmaUnknownException;
import dk.kb.alma.client.utils.MarcRecordHelper;
import dk.kb.alma.gen.bibs.Bib;
import dk.kb.alma.gen.bibs.Bibs;
import dk.kb.alma.gen.holding.Holding;
import dk.kb.alma.gen.holdings.Holdings;
import dk.kb.alma.gen.item.loans.ItemLoan;
import dk.kb.alma.gen.item.loans.ItemLoans;
import dk.kb.alma.gen.items.Item;
import dk.kb.alma.gen.items.ItemData;
import dk.kb.alma.gen.items.Items;
import dk.kb.alma.gen.portfolios.LinkingDetails;
import dk.kb.alma.gen.portfolios.Portfolio;
import dk.kb.alma.gen.portfolios.Portfolios;
import dk.kb.alma.gen.user_requests.PickupLocationTypes;
import dk.kb.alma.gen.user_requests.RequestTypes;
import dk.kb.alma.gen.user_requests.UserRequest;
import dk.kb.alma.gen.user_requests.UserRequests;
import dk.kb.util.other.NamedThread;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static dk.kb.alma.client.utils.Utils.nullable;
import static dk.kb.alma.client.utils.Utils.withDefault;

/**
 * Corresponds to https://developers.exlibrisgroup.com/console/?url=/wp-content/uploads/alma/openapi/bibs.json
 */
public class AlmaInventoryClient {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    private final AlmaRestClient almaRestClient;
    private final int batchSize;
    
    public AlmaInventoryClient(@NotNull AlmaRestClient almaRestClient) {
        this(almaRestClient, 100);
    }
    
    public AlmaInventoryClient(@NotNull AlmaRestClient almaRestClient, int batchSize) {
        this.almaRestClient = almaRestClient;
        this.batchSize = Integer.min(Integer.max(batchSize,1),100);
    }
    
    public AlmaRestClient getAlmaRestClient() {
        return almaRestClient;
    }
    
    
    /*CATALOG*/
    
    
    /*BIBS*/
    public Bib getBib(@NotBlank String mmsID) throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return almaRestClient.get(almaRestClient.constructLink()
                                                .path("/bibs/")
                                                .path(mmsID), Bib.class);
    }

    public Bib updateBib(@NotNull Bib record) throws AlmaConnectionException {
        WebClient link = almaRestClient.constructLink().path("/bibs/")
                                       .path(record.getMmsId());
        
        
        return almaRestClient.put(link, Bib.class, record);
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
        WebClient link = almaRestClient.constructLink().path("/bibs/");
        
        return almaRestClient.post(link, Bib.class, bib);
        
    }

    public Bib createBibFromNZ(@NotNull Bib bib, @NotBlank String mmsID) throws AlmaConnectionException {
        WebClient link = almaRestClient.constructLink().path("/bibs/")
                                                       .query("from_nz_mms_id", mmsID);

        return almaRestClient.post(link, Bib.class, bib);
    }
    
    public Bib deleteBib(@NotBlank String bibId) throws AlmaConnectionException {
        WebClient link = almaRestClient.constructLink().path("/bibs/")
                                       .path(bibId);
        
        return almaRestClient.delete(link, Bib.class);
    }
    
    
    public Bib getBibRecord(@NotBlank String bibId) throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return almaRestClient.get(almaRestClient.constructLink()
                                                .path("/bibs/")
                                                .path(bibId), Bib.class);
    }
    
    public Bib updateBibRecord(@NotNull Bib record) throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        WebClient link = almaRestClient.constructLink().path("/bibs/")
                                       .path(record.getMmsId());
        
        
        return almaRestClient.put(link, Bib.class, record);
    }
    
    public Set<Bib> getBibs(@NotNull Set<String> bibIDs)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        
        Pattern pattern = Pattern.compile(
                "Input parameters mmsId (\\d+) is not valid\\.");
        
        String threadName = Thread.currentThread().getName();
        Iterable<List<String>> partition = Iterables.partition(bibIDs, batchSize);
        return StreamSupport.stream(partition.spliterator(), true)
                            .map(NamedThread.function(
                                    (List<String> partion) -> {
                    
                                        List<String> modifiablePartion = new ArrayList<>(partion);
                                        Bibs bibs = null;
                                        while (!partion.isEmpty()) {
                                            try {
                                                String partionBibIDs = String.join(",", modifiablePartion);
                                                bibs = almaRestClient.get(almaRestClient
                                                                                  .constructLink()
                                                                                  .path("/bibs/")
                                                                                  .query("mms_id", partionBibIDs)
                                                                                  .query("view", "full")
                                                                                  .query("expand", "None"),
                                                                          Bibs.class);
                                                break;
                                            } catch (AlmaKnownException e) {
                                                //Check if this is due to one of the mmsIDs being invalid. If so, try again without this mmsid
                                                if (e.getErrorCode().equals("402203")) {
                                                    Matcher matcher = pattern.matcher(e.getErrorMessage());
                                                    if (matcher.matches()) {
                                                        String badMMSId = matcher.group(1);
                                                        log.warn("Requested bib data for mmsID {}. ALMA says this mmsID is invalid so disregarding",badMMSId,e);
                                                        modifiablePartion.remove(badMMSId);
                                                        continue;
                                                    }
                                                }
                                                throw e; //If we did not hit the continue above, just throw the exception and fail normally
                                            }
                                        }
                                        log.debug("Retrieved bibs range {} - ...{}... - {}",
                                                  partion.get(0),
                                                  partion.size(),
                                                  partion.get(partion.size() - 1));
                                        return bibs;
                                    },
                                    partion -> threadName + "->" + "Bibs-from-" + partion.get(0) + "-to-" + partion.get(
                                            partion.size() - 1)))
                            .filter(Objects::nonNull)
                            .filter(bibs -> bibs.getBibs() != null)
                            .flatMap(bibs -> bibs.getBibs().stream())
                            .collect(Collectors.toSet());
    }
    
    
    /**
     * Set whether the record should be published to Primo or not. "true" as suppressValue means that the record will
     * NOT be published.
     *
     * @param bibId         The record Id of the record to suppress
     * @param suppressValue String value "true" means to suppress and "false" not to suppress
     * @return the Bib record
     */
    public Bib setSuppressFromPublishing(@NotBlank String bibId, @NotNull String suppressValue) throws AlmaConnectionException {
        Bib record = getBib(bibId);
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

        return record;
    }
    
    
    /*HOLDINGS*/

    public Holdings getBibHoldings(@NotBlank String bibId)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return almaRestClient.get(almaRestClient.constructLink()
                                                .path("/bibs/")
                                                .path(bibId)
                                                .path("/holdings"), Holdings.class);
        
    }
    
    public Holding getHolding(@NotBlank String bibId, @NotBlank String holdingId) throws AlmaConnectionException {
        return almaRestClient.get(almaRestClient.constructLink()
                                                .path("/bibs/")
                                                .path(bibId)
                                                .path("/holdings/")
                                                .path(holdingId), Holding.class);
        
    }
    
    public Holding updateHolding(@NotBlank String bibId, @NotNull Holding holding) throws AlmaConnectionException {
        WebClient link = almaRestClient.constructLink()
                                       .path("/bibs/")
                                       .path(bibId)
                                       .path("/holdings/")
                                       .path(holding.getHoldingId());
        
        return almaRestClient.put(link, Holding.class, holding);
    }
    
    /*ITEMS*/
    
    public Items getItems(@NotBlank String bibId, @NotBlank String holdingId)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        Items result = new Items();
        
        int limit = 100;
        int offset = 0;
        int hitCount = 1;
        while (true) {
            Items items = almaRestClient.get(almaRestClient.constructLink().path("/bibs/")
                                                           .path(bibId)
                                                           .path("/holdings/")
                                                           .path(holdingId)
                                                           .path("/items")
                                                           .query("limit", limit)
                                                           .query("offset", offset), Items.class);
            result.getItems().addAll(items.getItems());
            offset += items.getItems().size();
            
            if (items.getItems().size() != limit || offset >= items.getTotalRecordCount()) {
                break;
            }
        }
        result.setTotalRecordCount(result.getItems().size());
        
        return result;
    }
    
    
    public Item getItem(@NotBlank String bibId, @NotBlank String holdingId, @NotBlank String itemId)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        
        Item item = almaRestClient.get(almaRestClient.constructLink().path("/bibs/")
                                                     .path(bibId)
                                                     .path("/holdings/")
                                                     .path(holdingId)
                                                     .path("/items/")
                                                     .path(itemId), Item.class);
        
        
        return item;
    }
    
    public Item getItem(@NotBlank String bibId,
                        @NotBlank String holdingId,
                        @NotBlank String itemId,
                        @Nullable String view,
                        @Nullable String expand,
                        @Nullable String user_id)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
    
        WebClient link = almaRestClient.constructLink()
                                       .path("/bibs/")
                                       .path(bibId)
                                       .path("/holdings/")
                                       .path(holdingId)
                                       .path("/items/")
                                       .path(itemId);
        nullable(view).ifPresent(viewValue -> link.query("view", viewValue));
        nullable(expand).ifPresent(expandValue -> link.query("expand", expandValue));
        nullable(user_id).ifPresent(user_idValue -> link.query("user_id", user_idValue));
    
        Item item = almaRestClient.get(link, Item.class);
        
        return item;
    }
    public Item getItem(String barcode) throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return almaRestClient.get(almaRestClient.constructLink()
                                                .path("/items")
                                                .query("item_barcode", barcode), Item.class);
    }
    
    public Item getItem(@NotNull String barcode, @Nullable String view, @Nullable String expand, @Nullable String user_id) throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        WebClient link = almaRestClient.constructLink()
                                        .path("/items")
                                        .query("item_barcode", barcode);
        nullable(view).ifPresent(viewValue -> link.query("view", viewValue));
        nullable(expand).ifPresent(expandValue -> link.query("expand", expandValue));
        nullable(user_id).ifPresent(user_idValue -> link.query("user_id", user_idValue));
        return almaRestClient.get(link, Item.class);
    }
    
    
    public Item createItem(@NotBlank String bibId,
                           @NotBlank String holdingId,
                           @Nullable String barcode,
                           @Nullable String description,
                           @Nullable String pages,
                           @Nullable String year) throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        WebClient link = almaRestClient.constructLink().path("/bibs/")
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
        
        return almaRestClient.post(link, Item.class, item);
    }
    
    public Item updateItem(@NotNull Item item) throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        WebClient link = almaRestClient.constructLink().path("/bibs/")
                                       .path(item.getBibData().getMmsId())
                                       .path("/holdings/")
                                       .path(item.getHoldingData().getHoldingId())
                                       .path("/items/")
                                       .path(item.getItemData().getPid());
        
        return almaRestClient.put(link, Item.class, item);
        
    }
    
    public Item deleteItem(@NotNull Item item, boolean force, boolean cleanEmptyHolding) {
        WebClient link = almaRestClient.constructLink().path("/bibs/")
                                       .path(item.getBibData().getMmsId())
                                       .path("/holdings/")
                                       .path(item.getHoldingData().getHoldingId())
                                       .path("/items/")
                                       .path(item.getItemData().getPid())
                                       .query("override", force)
                                       .query("holdings", cleanEmptyHolding);
        
        return almaRestClient.delete(link, Item.class);
    }
    
    
    /*PORTFOLIOS*/
    
    public Portfolios getBibPortfolios(@NotBlank String bibId) throws AlmaConnectionException {
        return almaRestClient.get(almaRestClient.constructLink()
                                                .path("/bibs/")
                                                .path(bibId)
                                                .path("/portfolios/"), Portfolios.class);
        
    }
    
    public Portfolio getPortfolio(@NotBlank String bibId, String portfolioId) throws AlmaConnectionException {
        return almaRestClient.get(almaRestClient.constructLink()
                                                .path("/bibs/")
                                                .path(bibId)
                                                .path("/portfolios/")
                                                .path(portfolioId), Portfolio.class);
        
    }
    
    public Portfolio createPortfolio(@NotBlank String bibId, Portfolio portfolio) throws AlmaConnectionException {
        WebClient link = almaRestClient.constructLink().path("/bibs/")
                                       .path(bibId)
                                       .path("/portfolios/");
        
        return almaRestClient.post(link, Portfolio.class, portfolio);
    }

    public Portfolio createPortfolioECollection(@NotBlank String collectionId, @NotBlank String serviceId, Portfolio portfolio) throws AlmaConnectionException {
        WebClient link = almaRestClient.constructLink().path("/electronic/e-collections/")
                                       .path(collectionId)
                                       .path("/e-services/")
                                       .path(serviceId)
                                       .path("/portfolios/");

        return almaRestClient.post(link, Portfolio.class, portfolio);
    }
    
    /**
     * @param bibId
     * @param multiVolume
     * @param pdfLink
     * @param publicNote
     * @return
     * @throws AlmaConnectionException
     * @see #createPortfolio(String, Portfolio) Use linked method instead
     * @deprecated
     */
    @Deprecated()
    public Portfolio createPortfolio(@NotBlank String bibId, Boolean multiVolume, String pdfLink, String publicNote)
            throws AlmaConnectionException {
        WebClient link = almaRestClient.constructLink().path("/bibs/")
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
        
        return almaRestClient.post(link, Portfolio.class, portfolio);
    }
    
    
    public Portfolio updatePortfolio(@NotBlank String bibId, Portfolio pf) throws AlmaConnectionException {
        WebClient link = almaRestClient.constructLink()
                                       .path("/bibs/")
                                       .path(bibId)
                                       .path("/portfolios/")
                                       .path(pf.getId());
        
        return almaRestClient.put(link, Portfolio.class, pf);
    }

    public Portfolio updatePortfolioECollection(@NotBlank String collectionId, @NotBlank String serviceId, Portfolio portfolio) throws AlmaConnectionException {
        WebClient link = almaRestClient.constructLink().path("/electronic/e-collections/")
                                       .path(collectionId)
                                       .path("/e-services/")
                                       .path(serviceId)
                                       .path("/portfolios/")
                                       .path(portfolio.getId());

        return almaRestClient.put(link, Portfolio.class, portfolio);
    }

    public Portfolio deletePortfolio(@NotBlank String bibId, @NotBlank String portfolioId) throws AlmaConnectionException {
        WebClient link = almaRestClient.constructLink().path("/bibs/")
                                       .path(bibId)
                                       .path("/portfolios/")
                                       .path(portfolioId);
        
        return almaRestClient.delete(link, Portfolio.class);
    }
    

    /*REQUESTS*/
    
    public UserRequests getRequests(@NotBlank String mmsID)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return almaRestClient.get(almaRestClient.constructLink()
                                                .path("/bibs/")
                                                .path(mmsID)
                                                .path("/requests"), UserRequests.class);
    }
    
    public UserRequests getItemRequests(@NotBlank String mmsId, @NotBlank String holdingId, @NotBlank String itemId)
            throws AlmaConnectionException {
        
        WebClient link = almaRestClient.constructLink().path("/bibs/")
                                       .path(mmsId)
                                       .path("/holdings/")
                                       .path(holdingId)
                                       .path("/items/")
                                       .path(itemId)
                                       .path("/requests");
        
        return almaRestClient.get(link, UserRequests.class);
    }
    
    
    public UserRequest getItemRequest(String mmsId, String holdingId, String itemId, @NotBlank String request_id) {
        
        WebClient link = almaRestClient.constructLink().path("/bibs/")
                                       .path(mmsId)
                                       .path("/holdings/")
                                       .path(holdingId)
                                       .path("/items/")
                                       .path(itemId)
                                       .path("/requests/")
                                       .path(request_id);
        
        return almaRestClient.get(link, UserRequest.class);
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
    public UserRequest createRequest(@NotBlank String userId,
                                     @NotBlank String bibId,
                                     @NotBlank String holdingId,
                                     @NotBlank String itemId,
                                     @NotBlank String pickupLocationCode,
                                     @Nullable XMLGregorianCalendar lastInterestDate)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        
        WebClient link = almaRestClient.constructLink().path("/bibs/")
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
        return almaRestClient.post(link, UserRequest.class, userRequest);
    }
    
    /*LOANS*/
    
    public ItemLoan createLoan(@NotBlank String userId,
                               @NotBlank String bibId,
                               @NotBlank String holdingId,
                               @NotBlank String itemId,
                               @NotNull ItemLoan itemLoan)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        WebClient link = almaRestClient.constructLink().path("/bibs/")
                                       .path(bibId)
                                       .path("/holdings/")
                                       .path(holdingId)
                                       .path("/items/")
                                       .path(itemId)
                                       .path("/loans")
                                       .query("user_id", userId)
                                       .query("user_id_type", "all_unique");
    
        return almaRestClient.post(link, ItemLoan.class, itemLoan);
    }
    
    public Item scanIn(@NotBlank String mmsId, @NotBlank String holdingId, @NotBlank String itemId, @NotBlank String library, @NotBlank String circulationDesk){
        //https://developers.exlibrisgroup.com/alma/apis/docs/bibs/UE9TVCAvYWxtYXdzL3YxL2JpYnMve21tc19pZH0vaG9sZGluZ3Mve2hvbGRpbmdfaWR9L2l0ZW1zL3tpdGVtX3BpZH0=/
        //Scan-in operation on item.
        WebClient link = almaRestClient.constructLink().path("/bibs/")
                                       .path(mmsId)
                                       .path("/holdings/")
                                       .path(holdingId)
                                       .path("/items/")
                                       .path(itemId)
                                       .query("op", "scan")
                                       .query("library", library)
                                       .query("circ_desk", circulationDesk);
        
        return almaRestClient.post(link, Item.class, null);
    }
    
    public ItemLoans getLoans(@NotBlank String mmsId, @NotBlank String holdingId, @NotBlank String itemId){
        WebClient link = almaRestClient.constructLink().path("/bibs/")
                                       .path(mmsId)
                                       .path("/holdings/")
                                       .path(holdingId)
                                       .path("/items/")
                                       .path(itemId)
                                       .path("/loans");
    
        return almaRestClient.get(link, ItemLoans.class);
    }
    
    public ItemLoan getLoan(@Nullable String mmsId, @Nullable String holdingId, @Nullable String itemId, @NotBlank String loanId){
        //It seems that you do not NEED the mmsID, holdingId or itemId values, but you must input SOMETHING in those fields
        WebClient link = almaRestClient.constructLink().path("/bibs/")
                                       .path(withDefault(mmsId, "0"))
                                       .path("/holdings/")
                                       .path(withDefault(holdingId,"0"))
                                       .path("/items/")
                                       .path(withDefault(itemId, "0"))
                                       .path("/loans/")
                                       .path(loanId);
        
        return almaRestClient.get(link, ItemLoan.class);
    }
}
