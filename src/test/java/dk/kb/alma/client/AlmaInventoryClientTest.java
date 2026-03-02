package dk.kb.alma.client;

import dk.kb.alma.client.exceptions.AlmaConnectionException;
import dk.kb.alma.client.exceptions.AlmaKnownException;
import dk.kb.alma.client.exceptions.MarcXmlException;
import dk.kb.alma.client.utils.MarcRecordHelper;
import dk.kb.alma.gen.bibs.Bib;
import dk.kb.alma.gen.holding.Holding;
import dk.kb.alma.gen.holdings.Holdings;
import dk.kb.alma.gen.items.Item;
import dk.kb.alma.gen.portfolios.Portfolio;
import dk.kb.util.xml.XML;
import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.VariableField;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.List;

import static dk.kb.alma.client.utils.MarcRecordHelper.DF245_TAG;
import static dk.kb.alma.client.utils.MarcRecordHelper.saveMarcRecordOnHolding;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlmaInventoryClientTest {

    private static AlmaRestClient client;

    @BeforeAll
    static void setupAlmaClient() throws IOException {
        client = TestUtils.getAlmaClient();
    }

    @Test
    @Disabled("Still figuring out what to actually test in MARC")
    public void testGetBibRecordMarc() throws AlmaConnectionException, TransformerException {
        AlmaInventoryClient almaClient = new AlmaInventoryClient(client, 100);

        Bib bib = almaClient.getBib("99121999521205763");  // 99123290311205763
        System.out.println(XML.domToString(bib.getAnies().get(0)));
    }


    @Test
    public void testMarcRecordHelper() throws AlmaConnectionException, MarcXmlException {
        AlmaInventoryClient almaClient = new AlmaInventoryClient(client, 100);

        Bib bib = almaClient.getBib("99122217581905763");  // 99123290311205763
        final Record marcRecord = MarcRecordHelper.getMarcRecordFromAlmaRecord(bib);
        final List<String> subfieldValues = MarcRecordHelper.getSubfieldValuesNew(marcRecord, "300", 'a');
        assertEquals(2, subfieldValues.size());
    }

    @Test
    public void testGetBibRecord() throws AlmaConnectionException {
        AlmaInventoryClient almaClient = new AlmaInventoryClient(client, 100);
        Bib bib = almaClient.getBib("99121999521205763");
        assertEquals("99121999521205763", bib.getMmsId());
    }


    @Test
    public void testGetBibRecordWithFail() throws AlmaConnectionException, IOException {
        AlmaInventoryClient almaClient = new AlmaInventoryClient(client, 100);

        try {
            almaClient.getBib("fail");
            Assertions.fail("Should have thrown exception");
        } catch (AlmaKnownException e) {
            Assertions.assertEquals(
                    "Failed with code '402203' / 'Input parameters mmsId fail is not valid.' on operation 'GET'  on URI 'https://api-eu.hosted.exlibrisgroup.com/almaws/v1/bibs/fail'",
                    e.getMessage());
        }
    }


    @Test
    @Disabled("The IDs are no longer valid in ALMA and I cannot find useful replacements")
    public void testUpdatePortfolio() throws IOException, AlmaConnectionException {
        AlmaInventoryClient almaClient = new AlmaInventoryClient(client, 100);
        String bibId = "99123290311205763";

        String portfolioId = "532109932570005763";
        Portfolio portfolio = almaClient.getPortfolio(bibId, portfolioId);
        portfolio.setIsStandalone(false);

        Portfolio pf = almaClient.updatePortfolio(bibId, portfolio);
//        // TODO: Alma API does not work
//        assertFalse(pf.isIsStandalone());
    }

    @Test
    @Disabled("The IDs are no longer valid in ALMA and I cannot find useful replacements")
    public void testSetControlfield008() throws IOException, AlmaConnectionException, MarcXmlException {
        AlmaInventoryClient almaClient = new AlmaInventoryClient(client, 100);
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
    public void createItem() throws AlmaConnectionException, IOException {
        AlmaInventoryClient almaClient = new AlmaInventoryClient(client, 100);

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
        AlmaInventoryClient almaClient = new AlmaInventoryClient(client, 100);

        Item item = almaClient.getItem("201000489518");

        assertEquals("", item.getItemData().getDescription());
    }

    @Test
    public void testGetHoldings() throws AlmaConnectionException {
        AlmaInventoryClient almaClient = new AlmaInventoryClient(client, 100);

        String bibId = "99122030762505763";
        Holdings holdings = almaClient.getBibHoldings(bibId);
        assertNotNull(holdings);
        assertFalse(holdings.getHoldings().isEmpty());
    }

    @Test
    public void testUpdateHolding() throws MarcXmlException {
        AlmaInventoryClient almaClient = new AlmaInventoryClient(client, 100);
        String bibId = "99122030762505763";
        String holdId ="222088096330005763";
        final Holding holding = almaClient.getHolding(bibId, holdId);
        Record holdingMarcRec = MarcRecordHelper.getMarcRecordFromHolding(holding);

        MarcRecordHelper.addDataField(holdingMarcRec,  "866", '3', '0', 'a', "TestData");
        MarcRecordHelper.saveMarcRecordOnHolding(holding, holdingMarcRec);
        almaClient.updateHolding("99122030762505763", holding);

        final Holding holdingUpd = almaClient.getHolding(bibId, holdId);
        Record holdingUpdMarcRec = MarcRecordHelper.getMarcRecordFromHolding(holdingUpd);
        List<VariableField> variableFieldsBeforeCleanup = holdingUpdMarcRec.find("866", "TestData");
        assertFalse(variableFieldsBeforeCleanup.isEmpty(), "The added '866' field with 'TestData' should be present in the holding after update");

        for (VariableField vf : variableFieldsBeforeCleanup){
            holdingUpdMarcRec.removeVariableField(vf);
        }

        MarcRecordHelper.saveMarcRecordOnHolding(holdingUpd, holdingUpdMarcRec);
        almaClient.updateHolding(bibId, holdingUpd);

        final Holding holdingAfterCleanup = almaClient.getHolding(bibId, holdId);
        Record holdingAfterCleanupMarcRec = MarcRecordHelper.getMarcRecordFromHolding(holdingAfterCleanup);
        List<VariableField> variableFieldsAfterCleanup = holdingAfterCleanupMarcRec.find("866", "TestData");
        assertTrue(variableFieldsAfterCleanup.isEmpty(), "The '866' field with 'TestData' should be removed from the holding after cleanup");
    }

    @Test
    public void testCreateBibRecord() throws IOException, AlmaConnectionException {
        AlmaInventoryClient almaClient = new AlmaInventoryClient(client, 100);
        Bib bib = almaClient.createBib();
        try {
            assertNotNull(bib);
        } finally {            // Clean up
            almaClient.deleteBib(bib.getMmsId());
        }


    }

    @Test
    public void testUpdateBib() throws IOException, MarcXmlException, AlmaConnectionException {
        AlmaInventoryClient almaClient = new AlmaInventoryClient(client, 100);
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
        AlmaInventoryClient almaClient = new AlmaInventoryClient(client, 100);
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
        AlmaInventoryClient almaClient = new AlmaInventoryClient(client, 100);
        Bib bib = almaClient.createBib();
        Portfolio portfolio = almaClient.createPortfolio(bib.getMmsId(), false, "thePdfLink", "public note");
        try {
            assertNotNull(bib);
        } finally {            // Clean up
            almaClient.deletePortfolio(bib.getMmsId(), portfolio.getId());
            almaClient.deleteBib(bib.getMmsId());
        }

    }

}
