package dk.kb.alma.client;

import dk.kb.alma.client.analytics.Report;
import dk.kb.alma.client.exceptions.AlmaConnectionException;
import dk.kb.alma.client.exceptions.AlmaKnownException;
import dk.kb.alma.client.exceptions.MarcXmlException;
import dk.kb.alma.client.utils.MarcRecordHelper;
import dk.kb.alma.gen.Bib;
import dk.kb.alma.gen.CodeTable;
import dk.kb.alma.gen.Item;
import dk.kb.alma.gen.Portfolio;
import dk.kb.alma.gen.Rows;
import dk.kb.alma.gen.User;
import dk.kb.alma.gen.holdings.Holdings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.marc4j.marc.Record;

import javax.xml.transform.TransformerException;
import java.io.IOException;

import static dk.kb.alma.client.TestUtils.getAlmaClient;
import static dk.kb.alma.client.utils.MarcRecordHelper.DF245_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class AlmaClientTest {




//    @Ignore
//    @Test
//    public void createItem() throws AlmaConnectionException {
//        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);
//        long barcode = (long) (Math.random() * 999999999999L);
//        Item item = almaClient.createItem("99120789920105763", "221199059350005763", String.valueOf(barcode), "test item", "1", "2000");
//
//        String title = item.getBibData().getTitle();
//        String itemBarcode = item.getItemData().getBarcode();
//        System.out.println("Created new item with barcode: " + itemBarcode + " and title: " + title);
//    }
//
//    @Ignore
//    @Test
//    public void updateItem() throws AlmaConnectionException {
//        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);
//
//        Item item = almaClient.getItem("99120789920105763", "221199059350005763", "231615214960005763");
//
//        Assert.assertNotNull(item);
//
//        String newBarcode = String.valueOf((long) (Math.random() * 999999999999L));
//        item.getItemData().setBarcode(String.valueOf(newBarcode));
//        Item updatedItem = almaClient.updateItem(item);
//
//        Assert.assertEquals(newBarcode, updatedItem.getItemData().getBarcode());
//    }
//
//    @Ignore
//    @Test
//    public void testGetItem() throws AlmaConnectionException {
//        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);
//
//        Item item = almaClient.getItem("99123290311205763", "221199059350005763", "231615214960005763");
//
//        Assert.assertEquals("test item", item. getItemData().getDescription());
//    }
//
//    @Ignore
//    @Test
//    public void testGetItemByBarcode() throws AlmaConnectionException {
//        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);
//
//        Item item = almaClient.getItem("v22333");
//
//        Assert.assertEquals("Created via Elba.", item.getItemData().getDescription());
//    }
//
//    @Ignore
//    @Test
//    public void testGetItems() throws AlmaConnectionException {
//        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);
//
//        Items items = almaClient.getItems("99120661858005763", "221157462480005763");
//
//        assertTrue(items.getItem().size() >= 2);
//    }
//
//    @Ignore
//    @Test
//    public void testGetHoldings() throws AlmaConnectionException {
//        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);
//
//        Holdings holdings = almaClient.getBibHoldings("99120789920105763");
//
//        Assert.assertNotNull(holdings);
//        assertTrue(holdings.getHolding().size() >= 3);
//    }
//
    
    
    @Test
    @Disabled
    public void testGetReport() throws AlmaConnectionException, IOException, TransformerException {
        AlmaClient almaClient = getAlmaClient();
    
        Report report = almaClient.startReport(
                "/shared/Royal Danish Library 45KBDK_KGL/Færdige rapporter/Digital kulturarv_DKM/Digitalt materiale i fysisk form/dvd-opstilling-bibnr-location\\/llo",
                null,
                null,
                true);
    
        System.out.println(report);
    }
    
    @Test
    public void testGetBibRecord() throws AlmaConnectionException, IOException {
        AlmaClient almaClient = getAlmaClient();

        Bib bib = almaClient.getBib("99123290311205763");  // 99123290311205763

        assertEquals("99123290311205763", bib.getMmsId());
    }


    @Test
    public void testGetBibRecordWithFail() throws AlmaConnectionException, IOException {
        AlmaClient almaClient = getAlmaClient();

        try {
            almaClient.getBib("fail");
            Assertions.fail("Should have thrown exception");
        } catch (AlmaKnownException e){
            Assertions.assertEquals("Failed with code 402203 / Input parameters mmsId fail is not valid. on GET  https://api-eu.hosted.exlibrisgroup.com/almaws/v1/bibs/fail",e.getMessage());
        }
    }

    @Test
    public void testGetUser() throws AlmaConnectionException, IOException {
        AlmaClient almaClient = getAlmaClient();

        User user = almaClient.getUser("thl");
        
        assertEquals("Thomas", user.getFirstName().trim());
    }
    
    
    @Test
    public void testRestrictLimit(){
        assertEquals(25,AlmaClient.restrictLimit(25));
    
        assertEquals(125,AlmaClient.restrictLimit(105));
        assertEquals(100,AlmaClient.restrictLimit(98));
    
        assertEquals(0,AlmaClient.restrictLimit(-25));
    
        assertEquals(1000,AlmaClient.restrictLimit(Integer.MAX_VALUE));
    
        assertEquals(0,AlmaClient.restrictLimit(Integer.MIN_VALUE));
    
        assertEquals(1000,AlmaClient.restrictLimit(2002));
    }

//    @Ignore
//    @Test
//    public void testGetUserForNonexistingUser() throws AlmaConnectionException {
//        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);
//
//        User user = almaClient.getUser("nonexistinguser");
//
//        assertNull(user);
//    }
//
//    @Ignore
//    @Test
//    public void testCancelRequestWithNonexistingRequest() throws AlmaConnectionException {
//        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);
//
//        boolean success = almaClient.cancelRequest("thl", "999999999999999", "PatronNotInterested", true);
//
//        assertFalse("Cancellation should fail.", success);
//    }
//
//    @Ignore
//    @Test
//    public void testCreateRequestAndCancelRequest() throws AlmaConnectionException {
//        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);
//
//        UserRequest request = almaClient.createRequest("thl", "99120747423805763", "221185306080005763", "231185306070005763", "SBL", null);
//
//        assertTrue(request.getTitle().startsWith("Ja!"));
//
//        boolean success = almaClient.cancelRequest("thl", request.getRequestId(), "PatronNotInterested", false);
//
//        assertTrue(success);
//    }
//
//    @Ignore
//    @Test
//    public void testCreateAndCancelItemRequest() throws AlmaConnectionException {
//        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);
//
//        UserRequest request = new UserRequest();
//        request.setUserPrimaryId("thl");
//        request.setRequestType(RequestTypes.HOLD);
//        request.setMmsId("99120402557905763");
//        request.setItemId("231073573770005763");
//        request.setPickupLocationType(PickupLocationTypes.LIBRARY);
//        request.setPickupLocationLibrary("UMOES");
//
//        request = almaClient.createRequest(request);
//
//        assertTrue(request.getTitle().startsWith("Eine warme Kartoffel ist ein warmes Bett"));
//
//        boolean success = almaClient.cancelRequest("thl", request.getRequestId(), "PatronNotInterested", false);
//
//        assertTrue(success);
//    }
//
//    @Ignore
//    @Test
//    public void testCreateAndCancelDigitizationRequest() throws AlmaConnectionException {
//        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);
//
//        UserRequest request = new UserRequest();
//        request.setUserPrimaryId("thl");
//        request.setRequestType(RequestTypes.DIGITIZATION);
//        UserRequest.RequestSubType subtype = new UserRequest.RequestSubType();
//        subtype.setValue("PHYSICAL_TO_DIGITIZATION");
//        request.setRequestSubType(subtype);
//        request.setMmsId("99120428345305763");
//        UserRequest.TargetDestination targetDestination = new UserRequest.TargetDestination();
//        targetDestination.setValue("DIGI_DEPT_INST");
//        request.setTargetDestination(targetDestination);
////        request.setDescription("Nr. 6 (april 2006)");
//        request.setPartialDigitization(false);
//
//        request = almaClient.createRequest(request);
//
//        assertTrue(request.getTitle().startsWith("Illustreret tidende"));
//
//        boolean success = almaClient.cancelRequest("thl", request.getRequestId(), "PatronNotInterested", false);
//
//        assertTrue(success);
//    }
//
//    @Ignore
//    @Test
//    public void testCreateAndCancelResourceSharingRequest() throws AlmaConnectionException {
//        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);
//
//        UserResourceSharingRequest request = createResourceSharingRequest();
//        UserResourceSharingRequest createdRequest = almaClient.createResourceSharingRequest(request, "thl");
//
//        Assert.assertEquals("Integration testtt", createdRequest.getTitle());
//
//        createdRequest = almaClient.getResourceSharingRequest("thl", createdRequest.getRequestId());
//        String userRequestLink = createdRequest.getUserRequest().getLink();
//        Assert.assertNotNull(userRequestLink);
//        String userRequestId = userRequestLink.substring(userRequestLink.lastIndexOf("/")+1);
//        assertFalse(userRequestId.isEmpty());
//
//        boolean cancelled = almaClient.cancelRequest("thl", userRequestId, "PatronNotInterested", false);
//
//        assertTrue(cancelled);
//    }
//
//    private UserResourceSharingRequest createResourceSharingRequest() {
//        UserResourceSharingRequest request = new UserResourceSharingRequest();
//        request.setTitle("Integration testtt");
//        UserResourceSharingRequest.Format format = new UserResourceSharingRequest.Format();
//        format.setValue("PHYSICAL");
//        request.setFormat(format);
//        UserResourceSharingRequest.CitationType citationType = new UserResourceSharingRequest.CitationType();
//        citationType.setValue("BK");
//        request.setCitationType(citationType);
//        request.setAgreeToCopyrightTerms(true);
//        UserResourceSharingRequest.PickupLocation pickupLocation = new UserResourceSharingRequest.PickupLocation();
//        pickupLocation.setValue("RRRUC");
//        request.setPickupLocation(pickupLocation);
//        return request;
//    }
//
//    @Ignore
//    @Test
//    public void testGetRequest() throws AlmaConnectionException {
//        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);
//
//        UserRequest request = almaClient.getRequest("thl", "12301266660005763");
//
//        Assert.assertEquals("The hitchhiker's guide to the galaxy", request.getTitle());
//    }
//
//    @Ignore
//    @Test
//    public void testGetItemRequests() throws AlmaConnectionException {
//        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);
//
//        List<UserRequest> itemRequests = almaClient.getItemRequests("99120962982505763", "221255954600005763", "231255954590005763");
//
//        assertTrue(itemRequests.size() > 0);
//
//        assertTrue("There should be a request from user 'thl'", itemRequests.stream().anyMatch(request -> request.getUserPrimaryId().equals("thl")));
//    }
//
//    @Ignore
//    @Test
//    public void testGetResourceSharingRequest() throws AlmaConnectionException {
//        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);
//
//        UserResourceSharingRequest request = almaClient.getResourceSharingRequest("thl", "12482165450005763");
//
//        Assert.assertEquals("testtest", request.getTitle());
//    }
//
//    @Ignore
//    @Test
//    public void testGetCodeTable() throws AlmaConnectionException {
//        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);
//
//        CodeTable requestCancellationReasons = almaClient.getCodeTable("RequestCancellationReasons");
//        Rows rows = requestCancellationReasons.getRows();
//        Assert.assertNotNull(rows);
//        assertTrue(rows.getRow().size() > 0);
//    }
    
    
    @Test
    public void testUpdatePortfolio() throws IOException, AlmaConnectionException {
        AlmaClient almaClient = getAlmaClient();
        String bibId = "99123319235105763";
        
        String portfolioId = "532109932570005763";
        Portfolio portfolio = almaClient.getPortfolio(bibId, portfolioId);
        portfolio.setIsStandalone(false);
        
        Portfolio pf = almaClient.updatePortfolio(bibId, portfolioId);
//        // TODO: Alma API does not work
//        assertFalse(pf.isIsStandalone());
    }
    
    @Test
    public void testSetContolfield008 () throws IOException, AlmaConnectionException, MarcXmlException {
        AlmaClient almaClient = getAlmaClient();
        String bibIdAna = "99122993392805763";
        String bibIdDigi = "99123315968505763";
        Bib anaRecord = almaClient.getBib(bibIdAna);
        Bib digiRecord = almaClient.getBib(bibIdDigi);
        String digiYear = "2020";
        Record anaMarcRecord = MarcRecordHelper.getMarcRecordFromAlmaRecord(anaRecord);
        Record digiMarcRecord = MarcRecordHelper.getMarcRecordFromAlmaRecord(digiRecord);
        MarcRecordHelper.setControlField008(anaMarcRecord, digiMarcRecord, digiYear);
        MarcRecordHelper.saveMarcRecordOnAlmaRecord(digiRecord, digiMarcRecord);
        almaClient.updateBib(digiRecord);
    }
    
    @Test
    public void testCreateDigiFromAna() throws IOException, AlmaConnectionException {
        AlmaClient almaClient = getAlmaClient();
//        99123299347505763
//        Bib digiRecord = almaClient.createDigitalRecordFromAnalog("99122993392805763", "pdfLinkValue", "yes");
//        String bibId = digiRecord.getMmsId();

//        // Clean up
//        Portfolio portfolio = almaClient.getBibPortfolios(bibId);
//        String portfolioId = portfolio.getId();
//        almaClient.deletePortfolio(bibId, portfolioId);
//        almaClient.deleteBib(bibId);
    }
    
    
    
    @Test
    public void testGetCodeTable() throws AlmaConnectionException, IOException {
        AlmaClient almaClient = getAlmaClient();
        
        CodeTable requestCancellationReasons = almaClient.getCodeTable("electronicMaterialType");
        Rows rows = requestCancellationReasons.getRows();
        assertNotNull(rows);
        assertTrue(rows.getRows().size() > 0);
    }
    
    
    @Test
    public void createItem() throws AlmaConnectionException, IOException {
        AlmaClient almaClient = getAlmaClient();
        long barcode = (long) (Math.random() * 999999999999L);
        Item item = almaClient.createItem("99122993627405763", "222071145220005763", String.valueOf(barcode), "test item", "1", "2000");
        
        String title = item.getBibData().getTitle();
        String itemBarcode = item.getItemData().getBarcode();
        System.out.println("Created new item with barcode: " + itemBarcode + " and title: " + title);
    }
    //
//    @Ignore
//    @Test
//    public void updateItem() throws AlmaConnectionException {
//        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);
//
//        Item item = almaClient.getItem("99120789920105763", "221199059350005763", "231615214960005763");
//
//        Assert.assertNotNull(item);
//
//        String newBarcode = String.valueOf((long) (Math.random() * 999999999999L));
//        item.getItemData().setBarcode(String.valueOf(newBarcode));
//        Item updatedItem = almaClient.updateItem(item);
//
//        Assert.assertEquals(newBarcode, updatedItem.getItemData().getBarcode());
//    }
//
//    @Ignore
//    @Test
//    public void testGetItem() throws AlmaConnectionException {
//        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);
//
//        Item item = almaClient.getItem("99123290311205763", "221199059350005763", "231615214960005763");
//
//        Assert.assertEquals("test item", item. getItemData().getDescription());
//    }
//
    @Test
    public void testGetItemByBarcode() throws AlmaConnectionException, IOException {
        AlmaClient almaClient = getAlmaClient();
        
        Item item = almaClient.getItem("201000489518");
        
        assertEquals("", item.getItemData().getDescription());
    }
    
    //    @Ignore
//    @Test
//    public void testGetItems() throws AlmaConnectionException {
//        AlmaClient almaClient = new AlmaClient("https://api-eu.hosted.exlibrisgroup.com/almaws/v1/", SANDBOX_APIKEY);
//
//        Items items = almaClient.getItems("99120661858005763", "221157462480005763");
//
//        assertTrue(items.getItem().size() >= 2);
//    }
//
    @Test
    public void testGetHoldings() throws AlmaConnectionException, IOException {
        AlmaClient almaClient = getAlmaClient();

//        List<String> holdingIdList = new ArrayList<>();
        String bibId = "99122030762505763";
        Holdings holdings = almaClient.getBibHoldings(bibId);
//        for (int i = 0; i < holdings.getTotalRecordCount(); i++) {
//            holdingIdList.add(holdings.getHolding().get(i).getHoldingId() );
//
//        }
        assertNotNull(holdings);
        assertTrue(holdings.getHoldings().size() >= 1);
    }
  
  
   
    
    @Test
    public void testCreateBibRecord() throws IOException, AlmaConnectionException {
        AlmaClient almaClient = getAlmaClient();
        Bib bib = almaClient.createBib();
        assertNotNull(bib);
        // Clean up
        almaClient.deleteBib(bib.getMmsId());
        
    }
    
    @Test
    public void testUpdateBib() throws IOException, MarcXmlException, AlmaConnectionException {
        AlmaClient almaClient = getAlmaClient();
        String bibId = "99123315753305763";
        Bib oldRecord = almaClient.getBib(bibId);
        
        Record marcOldRecord = MarcRecordHelper.getMarcRecordFromAlmaRecord(oldRecord);
        String oldTitle = oldRecord.getTitle();
        String newTitle = "AnotherTitle";
        assertTrue(MarcRecordHelper.setDataField(marcOldRecord, DF245_TAG, 'a', newTitle));
        MarcRecordHelper.saveMarcRecordOnAlmaRecord(oldRecord, marcOldRecord);
        Bib updatedRecord = almaClient.updateBib(oldRecord);
        assertEquals(newTitle, updatedRecord.getTitle());
        
        // Clean up
        assertTrue(MarcRecordHelper.setDataField(marcOldRecord, DF245_TAG, 'a', oldTitle));
        MarcRecordHelper.saveMarcRecordOnAlmaRecord(oldRecord, marcOldRecord);
        almaClient.updateBib(oldRecord);
    }
    
    @Test
    public void testSetSuppressFromPublishing() throws IOException, AlmaConnectionException, MarcXmlException {
        //99122993392805763 ana
        //99123315968505763 digi
        AlmaClient almaClient = getAlmaClient();
        String bibId = "99122993392805763";//99123319235105763
        Bib record = almaClient.getBib(bibId);
        Record marcRecord = MarcRecordHelper.getMarcRecordFromAlmaRecord(record);
        record.setSuppressFromPublishing("true");
        MarcRecordHelper.saveMarcRecordOnAlmaRecord(record, marcRecord);
//        almaClient.setSuppressFromPublishing(bibId, "true");
        almaClient.updateBib(record);
        
    }
    
    @Test
    public void testCreatePortfolio() throws IOException, AlmaConnectionException {
        AlmaClient almaClient = getAlmaClient();
        String bibIdDigi = "99123319235105763"; // 99123315968505763
        Portfolio portfolio = almaClient.createPortfolio(bibIdDigi, false, "thePdfLink", "public note");
        
        almaClient.deletePortfolio(bibIdDigi, portfolio.getId());
    }
    

}
