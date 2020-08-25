package dk.kb.alma.client;

import dk.kb.alma.client.analytics.Report;
import dk.kb.alma.client.exceptions.AlmaConnectionException;
import dk.kb.alma.client.exceptions.AlmaKnownException;
import dk.kb.alma.client.exceptions.MarcXmlException;
import dk.kb.alma.client.utils.MarcRecordHelper;
import dk.kb.alma.gen.bibs.Bib;
import dk.kb.alma.gen.code_table.CodeTable;
import dk.kb.alma.gen.items.Item;
import dk.kb.alma.gen.portfolios.Portfolio;
import dk.kb.alma.gen.code_table.Rows;
import dk.kb.alma.gen.user_requests.UserRequest;
import dk.kb.alma.gen.users.User;
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
    
    
    @Test
    @Disabled("Takes forever")
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
    @Disabled("The IDs are no longer valid in ALMA and I cannot find useful replacements")
    public void testGetBibRecord() throws AlmaConnectionException, IOException {
        AlmaClient almaClient = getAlmaClient();
        
        Bib bib = almaClient.getBib("99122993627405763");  // 99123290311205763
        
        assertEquals("99122993627405763", bib.getMmsId());
    }
    
    
    @Test
    public void testGetBibRecordWithFail() throws AlmaConnectionException, IOException {
        AlmaClient almaClient = getAlmaClient();
        
        try {
            almaClient.getBib("fail");
            Assertions.fail("Should have thrown exception");
        } catch (AlmaKnownException e) {
            Assertions.assertEquals(
                    "Failed with code 402203 / Input parameters mmsId fail is not valid. on GET  https://api-eu.hosted.exlibrisgroup.com/almaws/v1/bibs/fail",
                    e.getMessage());
        }
    }
    
    @Test
    public void testGetUser() throws AlmaConnectionException, IOException {
        AlmaClient almaClient = getAlmaClient();
        
        User user = almaClient.getUser("thl");
        
        assertEquals("Thomas", user.getFirstName().trim());
    }
    
    
    @Test
    public void testRestrictLimit() {
        assertEquals(25, AlmaClient.restrictLimit(25));
        
        assertEquals(125, AlmaClient.restrictLimit(105));
        assertEquals(100, AlmaClient.restrictLimit(98));
        
        assertEquals(0, AlmaClient.restrictLimit(-25));
        
        assertEquals(1000, AlmaClient.restrictLimit(Integer.MAX_VALUE));
        
        assertEquals(0, AlmaClient.restrictLimit(Integer.MIN_VALUE));
        
        assertEquals(1000, AlmaClient.restrictLimit(2002));
    }
    
    
    @Test
    @Disabled("The IDs are no longer valid in ALMA and I cannot find useful replacements")
    public void testUpdatePortfolio() throws IOException, AlmaConnectionException {
        AlmaClient almaClient = getAlmaClient();
        String bibId = "99123290311205763";
        
        String portfolioId = "532109932570005763";
        Portfolio portfolio = almaClient.getPortfolio(bibId, portfolioId);
        portfolio.setIsStandalone(false);
        
        Portfolio pf = almaClient.updatePortfolio(bibId, portfolioId);
//        // TODO: Alma API does not work
//        assertFalse(pf.isIsStandalone());
    }
    
    @Test
    @Disabled("The IDs are no longer valid in ALMA and I cannot find useful replacements")
    public void testSetControlfield008() throws IOException, AlmaConnectionException, MarcXmlException {
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
        Item item = almaClient.createItem("99122993627405763",
                                          "222071145220005763",
                                          String.valueOf(barcode),
                                          "test item",
                                          "1",
                                          "2000");
        try {
            String title = item.getBibData().getTitle();
            String itemBarcode = item.getItemData().getBarcode();
            System.out.println("Created new item with barcode: " + itemBarcode + " and title: " + title);
        } finally {
            almaClient.deleteItem(item, true, true);
        }
    }
    
    @Test
    public void testGetItemByBarcode() throws AlmaConnectionException, IOException {
        AlmaClient almaClient = getAlmaClient();
        
        Item item = almaClient.getItem("201000489518");
        
        assertEquals("", item.getItemData().getDescription());
    }
    
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
        try {
            assertNotNull(bib);
        } finally {            // Clean up
            almaClient.deleteBib(bib.getMmsId());
        }
        
        
    }
    
    @Test
    public void testUpdateBib() throws IOException, MarcXmlException, AlmaConnectionException {
        AlmaClient almaClient = getAlmaClient();
        Bib bib = almaClient.createBib();
        try {
            assertNotNull(bib);
            
            
            String bibId = bib.getMmsId();
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
        } finally {            // Clean up
            almaClient.deleteBib(bib.getMmsId());
        }
        
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
        Bib bib = almaClient.createBib();
        Portfolio portfolio = almaClient.createPortfolio(bib.getMmsId(), false, "thePdfLink", "public note");
        try {
            assertNotNull(bib);
        } finally {            // Clean up
            almaClient.deletePortfolio(bib.getMmsId(), portfolio.getId());
            almaClient.deleteBib(bib.getMmsId());
        }

    }

    @Test
    public void testCreateUpdateAndCancelRequest() throws AlmaConnectionException, IOException {
        AlmaClient almaClient = getAlmaClient();

        UserRequest request = almaClient.createRequest("thl", "99122652604305763", "221882066210005763", "231882066200005763", "SBL", null);

        assertTrue(request.getTitle().startsWith("Ja!"));

        String newComment = "integration test comment";
        request.setComment(newComment);
        UserRequest updatedRequest = almaClient.updateRequest(request);

        assertEquals(newComment, updatedRequest.getComment());

        boolean success = almaClient.cancelRequest("thl", request.getRequestId(), "PatronNotInterested", false);

        assertTrue(success);
    }

    @Test
    public void testUpdateRequestWithInvalidRequestId() throws AlmaConnectionException, IOException {
        AlmaClient almaClient = getAlmaClient();

        UserRequest request = almaClient.getRequest("thl", "19899886630005763");

        request.setRequestId("00000000000000");
        Assertions.assertThrows(AlmaKnownException.class, () -> {
            almaClient.updateRequest(request);
        });
    }
    
    
}
