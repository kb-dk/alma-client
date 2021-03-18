package dk.kb.alma.client;

import dk.kb.alma.client.exceptions.AlmaConnectionException;
import dk.kb.alma.gen.code_table.CodeTable;
import dk.kb.alma.gen.code_table.Rows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static dk.kb.alma.client.TestUtils.getAlmaClient;
import static org.junit.jupiter.api.Assertions.*;

class AlmaConfClientTest {
    
    private static AlmaRestClient client;
    
    @BeforeAll
    static void setupAlmaClient() throws IOException {
        client = TestUtils.getAlmaClient();
    }
    @Test
    public void testGetCodeTable() throws AlmaConnectionException, IOException {
        AlmaConfClient almaClient = new AlmaConfClient(client);
        
        CodeTable requestCancellationReasons = almaClient.getCodeTable("electronicMaterialType");
        Rows rows = requestCancellationReasons.getRows();
        assertNotNull(rows);
        assertTrue(rows.getRows().size() > 0);
    }
    
    
}