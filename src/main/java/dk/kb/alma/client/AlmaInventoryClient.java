package dk.kb.alma.client;

import com.google.common.collect.Iterables;
import dk.kb.alma.client.exceptions.AlmaConnectionException;
import dk.kb.alma.client.exceptions.AlmaKnownException;
import dk.kb.alma.client.exceptions.AlmaUnknownException;
import dk.kb.alma.client.exceptions.MarcXmlException;
import dk.kb.alma.client.utils.MarcRecordHelper;
import dk.kb.alma.gen.bibs.Bib;
import dk.kb.alma.gen.bibs.Bibs;
import dk.kb.alma.gen.holdings.Holdings;
import dk.kb.alma.gen.items.Item;
import dk.kb.alma.gen.items.ItemData;
import dk.kb.alma.gen.items.Items;
import dk.kb.alma.gen.portfolios.LinkingDetails;
import dk.kb.alma.gen.portfolios.Portfolio;
import dk.kb.alma.gen.user_requests.PickupLocationTypes;
import dk.kb.alma.gen.user_requests.RequestTypes;
import dk.kb.alma.gen.user_requests.UserRequest;
import dk.kb.alma.gen.user_requests.UserRequests;
import dk.kb.util.other.NamedThread;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Corresponds to https://developers.exlibrisgroup.com/console/?url=/wp-content/uploads/alma/openapi/bibs.json
 */
public class AlmaInventoryClient {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    private final AlmaRestClient almaRestClient;
    private final int batchSize;
    
    public AlmaInventoryClient(AlmaRestClient almaRestClient) {
        this(almaRestClient, 100);
    }
    
    public AlmaInventoryClient(AlmaRestClient almaRestClient, int batchSize) {
        this.almaRestClient = almaRestClient;
        this.batchSize = batchSize;
    }
    
    public AlmaRestClient getAlmaRestClient() {
        return almaRestClient;
    }
    
    
    /*CATALOG*/
    
    
    /*BIBS*/
    public Bib getBib(String mmsID) throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return almaRestClient.get(almaRestClient.constructLink()
                                                .path("/bibs/")
                                                .path(mmsID), Bib.class);
    }
    
    public Bib updateBib(Bib record) throws AlmaConnectionException {
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
    
    public Bib deleteBib(String bibId) throws AlmaConnectionException {
        WebClient link = almaRestClient.constructLink().path("/bibs/")
                                       .path(bibId);
        
        return almaRestClient.delete(link, Bib.class);
    }
    
    
    public Bib getBibRecord(String bibId) throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return almaRestClient.get(almaRestClient.constructLink()
                                                .path("/bibs/")
                                                .path(bibId), Bib.class);
    }
    
    public Bib updateBibRecord(Bib record) throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        WebClient link = almaRestClient.constructLink().path("/bibs/")
                                       .path(record.getMmsId());
        
        
        return almaRestClient.put(link, Bib.class, record);
    }
    
    public Set<Bib> getBibs(Set<String> bibIDs)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        String threadName = Thread.currentThread().getName();
        Iterable<List<String>> partition = Iterables.partition(bibIDs, batchSize);
        return StreamSupport.stream(partition.spliterator(), true)
                            .map(NamedThread.function(
                                    (List<String> partion) -> {
                                        String partionBibIDs = String.join(",", partion);
                                        return almaRestClient.get(almaRestClient.constructLink()
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
    
    
    /*HOLDINGS*/

    public Holdings getBibHoldings(String bibId)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return almaRestClient.get(almaRestClient.constructLink()
                                                .path("/bibs/")
                                                .path(bibId)
                                                .path("/holdings"), Holdings.class);
        
    }

    
    /*ITEMS*/
    
    public Items getItems(String bibId, String holdingId)
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
    
    
    public Item getItem(String bibId, String holdingId, String itemId)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        
        Item item = almaRestClient.get(almaRestClient.constructLink().path("/bibs/")
                                                     .path(bibId)
                                                     .path("/holdings/")
                                                     .path(holdingId)
                                                     .path("/items/")
                                                     .path(itemId), Item.class);
        
        
        return item;
    }
    
    public Item getItem(String barcode) throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return almaRestClient.get(almaRestClient.constructLink()
                                                .path("/items")
                                                .query("item_barcode", barcode), Item.class);
    }
    
    public Item createItem(String bibId,
                           String holdingId,
                           String barcode,
                           String description,
                           String pages,
                           String year) throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
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
    
    public Item updateItem(Item item) throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        WebClient link = almaRestClient.constructLink().path("/bibs/")
                                       .path(item.getBibData().getMmsId())
                                       .path("/holdings/")
                                       .path(item.getHoldingData().getHoldingId())
                                       .path("/items/")
                                       .path(item.getItemData().getPid());
        
        return almaRestClient.put(link, Item.class, item);
        
    }
    
    public Item deleteItem(Item item, boolean force, boolean cleanEmptyHolding) {
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
    
    public Portfolio getBibPortfolios(String bibId) throws AlmaConnectionException {
        return almaRestClient.get(almaRestClient.constructLink()
                                                .path("/bibs/")
                                                .path(bibId)
                                                .path("/portfolios/"), Portfolio.class);
        
    }
    
    public Portfolio getPortfolio(String bibId, String portfolioId) throws AlmaConnectionException {
        return almaRestClient.get(almaRestClient.constructLink()
                                                .path("/bibs/")
                                                .path(bibId)
                                                .path("/portfolios/")
                                                .path(portfolioId), Portfolio.class);
        
    }
    
    public Portfolio createPortfolio(String bibId, Portfolio portfolio) throws AlmaConnectionException {
        WebClient link = almaRestClient.constructLink().path("/bibs/")
                                       .path(bibId)
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
    public Portfolio createPortfolio(String bibId, Boolean multiVolume, String pdfLink, String publicNote)
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
    
    
    public Portfolio updatePortfolio(String bibId, Portfolio pf) throws AlmaConnectionException {
        WebClient link = almaRestClient.constructLink()
                                       .path("/bibs/")
                                       .path(bibId)
                                       .path("/portfolios/")
                                       .path(pf.getId());
        
        return almaRestClient.put(link, Portfolio.class, pf);
    }
    
    public Portfolio deletePortfolio(String bibId, String portfolioId) throws AlmaConnectionException {
        WebClient link = almaRestClient.constructLink().path("/bibs/")
                                       .path(bibId)
                                       .path("/portfolios/")
                                       .path(portfolioId);
        
        return almaRestClient.delete(link, Portfolio.class);
    }
    

    /*REQUESTS*/
    
    public UserRequests getRequests(String mmsID)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return almaRestClient.get(almaRestClient.constructLink()
                                                .path("/bibs/")
                                                .path(mmsID)
                                                .path("/requests"), UserRequests.class);
    }
    
    public UserRequests getItemRequests(String mmsId, String holdingId, String itemId)
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
    
    
    public UserRequest getItemRequest(String mmsId, String holdingId, String itemId, String request_id) {
        
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
    public UserRequest createRequest(String userId,
                                     String bibId,
                                     String holdingId,
                                     String itemId,
                                     String pickupLocationCode,
                                     XMLGregorianCalendar lastInterestDate)
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
    
    
}
