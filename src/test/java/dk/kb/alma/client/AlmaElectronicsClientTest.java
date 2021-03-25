package dk.kb.alma.client;

import dk.kb.alma.gen.item.electronic.ElectronicCollections;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class AlmaElectronicsClientTest {
    private static AlmaRestClient client;
    
    @BeforeAll
    static void setupAlmaClient() throws IOException {
        client = TestUtils.getAlmaClient();
    }
    
    @Test
    @Disabled("apikey is not allowed...")
    void getElectronicCollections() {
        AlmaElectronicsClient electronicsClient = new AlmaElectronicsClient(client);
        ElectronicCollections collections = electronicsClient.getElectronicCollections(null, null, null);
        System.out.println(collections);
    }
    
   
}