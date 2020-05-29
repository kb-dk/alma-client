package dk.kb.alma.client;

import com.google.common.base.Charsets;
import dk.kb.alma.client.exceptions.AlmaConnectionException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;

public class TestUtils {

    public static AlmaClient getAlmaClient() throws IOException, AlmaConnectionException {
        Properties dodpro = new Properties();
        try (Reader propStream = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("almaclient.properties"),
                                                       Charsets.UTF_8)) {
            dodpro.load(propStream);
        }
        return new AlmaClient(dodpro.getProperty("alma.url"),
                              dodpro.getProperty("alma.apikey"),
                              100, Long.parseLong(dodpro.getProperty("alma_rate_limit_min_sleep_millis")),
                              Long.parseLong(dodpro.getProperty("alma_rate_limit_sleep_variation_millis")),
                              dodpro.getProperty("lang"),
                              Integer.parseInt(dodpro.getProperty("connect_timeout")), Integer.parseInt(dodpro.getProperty("read_timeout"))
        );
    }
}
