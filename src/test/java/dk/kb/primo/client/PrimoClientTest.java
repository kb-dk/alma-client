package dk.kb.primo.client;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrimoClientTest {
    
    public URI psbURI = URI.create("https://kbdk-kgl-psb.primo.exlibrisgroup.com/primaws/rest");
    public URI prodURI = URI.create("https://soeg.kb.dk/primaws/rest");
    private PrimoClient primoClient = new PrimoClient(psbURI, "45KBDK_KGL", "45KBDK_KGL:KGL", "en");
    
    @Test
    void primoSearch() {
        //Search for specific mmsID
        String mmsID = "99121922824305763";
        var search = primoClient.search("any,exact," + mmsID,
                                        0,
                                        1,
                                        "",
                                        "",
                                        true,
                                        0,
                                        "rank",
                                        "Everything",
                                        "MyInst_and_CI", "N", false);
        String mmsIDofFoundItem = search.get("docs").get(0).get("pnx").get("display").get("mms").get(0).asText();
        assertEquals(mmsID, mmsIDofFoundItem);
    }
    
    @Test
    void primoTranslations() {
        var trans = primoClient.getTranslations("da");
        final String text = trans.get("fulldisplay.lds01").asText();
        assertEquals("Værk ID", text);
    }
    
    @Test
    void getPrimoConfig() {
        var config = primoClient.getConfig();
        assertEquals("45KBDK_KGL",
                     config.get("primo-view").get("institution").get("org-fields").get("customer-code").asText());
    }
    
}