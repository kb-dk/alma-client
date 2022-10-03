package dk.kb.alma.client;

import dk.kb.alma.client.exceptions.AlmaKnownException;
import dk.kb.alma.client.exceptions.AlmaNotFoundException;
import dk.kb.alma.gen.vendor.Account;
import dk.kb.alma.gen.vendor.Note;
import dk.kb.alma.gen.vendor.Vendor;
import dk.kb.alma.gen.vendor.Vendors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AlmaAcquisitionsClientTest {
    
    private static AlmaRestClient client;
    
    @BeforeAll
    static void setupAlmaClient() throws IOException {
        client = TestUtils.getAlmaClient();
        AlmaAcquisitionsClient almaClient = new AlmaAcquisitionsClient(client);
        try {
            almaClient.getVendor("pligt_test_vendor_manuel_oprettelse");
        } catch (AlmaNotFoundException e) {
            Vendor vendor = almaClient.newMaterialSupplierVendorObject("pligt_test_vendor_manuel_oprettelse",
                    "pligt_test_vendor_manuel_oprettelse",
                    "pligt_test_vendor_manuel_oprettelse",
                    "pligt_test_vendor_account_manuel_code",
                    "ACCOUNTINGDEPARTMENT");
            Vendor newVendor = almaClient.createVendor(vendor);
        } catch (Exception e) {
            System.out.println("mvn e.getMessage() = " + e.getMessage());
            if (e.getMessage().contains("402880")) {
                Vendor vendor = almaClient.newMaterialSupplierVendorObject("pligt_test_vendor_manuel_oprettelse",
                        "pligt_test_vendor_manuel_oprettelse",
                        "pligt_test_vendor_manuel_oprettelse",
                        "pligt_test_vendor_account_manuel_code",
                        "ACCOUNTINGDEPARTMENT");
                Vendor newVendor = almaClient.createVendor(vendor);
            }
        }
    }
    
    @BeforeEach
    void setUp() {
    }
    
    @Test
    @Disabled("There are way to many vendors for this test to finish in reasonable time")
    void getVendors() {
        AlmaAcquisitionsClient almaClient = new AlmaAcquisitionsClient(client);
        Vendors vendors = almaClient.getVendors();
        assertEquals(vendors.getVendors().size(), (int) vendors.getTotalRecordCount());
        assertTrue(vendors.getVendors().size() > 0);
    }
    
    @Test
    void getVendor() {
        AlmaAcquisitionsClient almaClient = new AlmaAcquisitionsClient(client);
        Vendor vendor = almaClient.getVendor("pligt_test_vendor_manuel_oprettelse");
    
        assertEquals("pligt_test_vendor_manuel_oprettelse",vendor.getName());
        assertEquals("ACTIVE",vendor.getStatus().getValue());
        assertEquals(true,vendor.isMaterialSupplier());
        assertEquals("pligt_test_vendor_account_manuel_code",vendor.getAccounts().getAccounts().get(0).getCode());
    }
    
    @Test
    void updateVendor() {
        AlmaAcquisitionsClient almaClient = new AlmaAcquisitionsClient(client);
        Vendor vendor = almaClient.getVendor("pligt_test_vendor_manuel_oprettelse");
        List<Note> notes = vendor.getNotes().getNotes();
        notes.clear();
        Note note = new Note();
        notes.add(note);
        String content = "Test af update vendor: " + new Date().getTime();
        note.setContent(content);
    
        Vendor updatedVendor = almaClient.updateVendor(vendor);
        
        assertEquals(content, updatedVendor.getNotes().getNotes().get(0).getContent());
    }
     @Test
    void updateVendorWithNewVendorCode() {
        AlmaAcquisitionsClient almaClient = new AlmaAcquisitionsClient(client);
         final String oldVendorCode = "pligt_test_vendor_manuel_oprettelse";
         final String newVendorCode = "newVendorCode";
         Vendor vendor = almaClient.getVendor(oldVendorCode);
         vendor.setCode(newVendorCode);
         almaClient.updateVendorAndChangeVendorCode(vendor, oldVendorCode);
         Vendor updatedVendor = almaClient.getVendor(newVendorCode);
         assertEquals(newVendorCode, updatedVendor.getCode());
//       Rolling back
         vendor = almaClient.getVendor(newVendorCode);
         vendor.setCode(oldVendorCode);
         almaClient.updateVendorAndChangeVendorCode(vendor, newVendorCode);
         updatedVendor = almaClient.getVendor(oldVendorCode);
         assertEquals(oldVendorCode, updatedVendor.getCode());
    }

    @Test
    void cloneVendor() {
        AlmaAcquisitionsClient almaClient = new AlmaAcquisitionsClient(client);
        Vendor vendor = almaClient.getVendor("pligt_test_vendor_manuel_oprettelse");
        String cloneCode = "pligt_test_vendor_manuel_oprettelse_clone";

        try {
            almaClient.deleteVendor(cloneCode);
        } catch (AlmaKnownException e){
            if (!e.getErrorCode().equals("402880")) { //Not found is ok for delete
                throw e;
            }
        }
        
        vendor.setCode(cloneCode);
        List<Account> accountList = vendor.getAccounts().getAccounts();
        Account account = accountList.get(0);
        account.setCode("pligt_test_vendor_manuel_account_code_clone");
        account.setAccountId(null);
        
        almaClient.createVendor(vendor);
    
        Vendor clonedVendor = almaClient.getVendor(cloneCode);
        
        almaClient.deleteVendor(cloneCode);
    }
    
    
    @Test
    void createVendor() {
        AlmaAcquisitionsClient almaClient = new AlmaAcquisitionsClient(client);
        Vendor vendor = almaClient.newMaterialSupplierVendorObject("Alma Client Unit Test Vendor",
                                                                   "ALMA_CLIENT_TEST_VENDOR",
                                                                   "Alma Client Unit Test Vendor Account",
                                                                   "ALMA_CLIENT_TEST_VENDOR_ACCOUNT",
                                                                   "ACCOUNTINGDEPARTMENT");
        try {
            almaClient.deleteVendor(vendor);
        } catch (AlmaKnownException e){
            if (!e.getErrorCode().equals("402880")) { //Not found is ok for delete
                throw e;
            }
        }
        
        
        Vendor newVendor = almaClient.createVendor(vendor);
        
        almaClient.deleteVendor(newVendor);
    }
    
   
}
