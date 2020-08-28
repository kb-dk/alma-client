package dk.kb.alma.client;

import dk.kb.alma.client.analytics.Report;
import dk.kb.alma.client.exceptions.AlmaConnectionException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.xml.transform.TransformerException;
import java.io.IOException;

import static dk.kb.alma.client.TestUtils.getAlmaClient;
import static org.junit.jupiter.api.Assertions.*;

class AlmaAnalyticsClientTest {
    
    
    @Test
    public void testRestrictLimit() {
        assertEquals(25, AlmaAnalyticsClient.restrictLimit(25));
        
        assertEquals(125, AlmaAnalyticsClient.restrictLimit(105));
        assertEquals(100, AlmaAnalyticsClient.restrictLimit(98));
        
        assertEquals(0, AlmaAnalyticsClient.restrictLimit(-25));
        
        assertEquals(1000, AlmaAnalyticsClient.restrictLimit(Integer.MAX_VALUE));
        
        assertEquals(0, AlmaAnalyticsClient.restrictLimit(Integer.MIN_VALUE));
        
        assertEquals(1000, AlmaAnalyticsClient.restrictLimit(2002));
    }
    
    @Test
    @Disabled("Takes forever")
    public void testGetReport() throws AlmaConnectionException, IOException, TransformerException {
        AlmaAnalyticsClient almaClient = new AlmaAnalyticsClient(getAlmaClient());
        
        Report report = almaClient.startReport(
                "/shared/Royal Danish Library 45KBDK_KGL/Færdige rapporter/Digital kulturarv_DKM/Digitalt materiale i fysisk form/dvd-opstilling-bibnr-location\\/llo",
                null,
                null,
                true);
        
        System.out.println(report);
    }
    
}