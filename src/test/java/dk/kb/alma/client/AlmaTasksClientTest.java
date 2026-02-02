package dk.kb.alma.client;

import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;

class AlmaTasksClientTest {
    private static AlmaRestClient client;

    @BeforeAll
    static void setupAlmaClient() throws IOException {
        client = TestUtils.getAlmaClient();
    }
}