package dk.kb.alma.client;

import com.google.common.base.Charsets;
import dk.kb.alma.client.exceptions.AlmaConnectionException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;

public class TestUtils {
    
    public static AlmaRestClient getAlmaClient() throws IOException, AlmaConnectionException {
        Properties dodpro = new Properties();
        try (Reader propStream = new InputStreamReader(Thread.currentThread()
                                                             .getContextClassLoader()
                                                             .getResourceAsStream("almaclient.properties"),
                                                       Charsets.UTF_8)) {
            dodpro.load(propStream);
        }
        return new AlmaRestClient(dodpro.getProperty("alma.url"),
                                  dodpro.getProperty("alma.apikey"),
                                  Long.parseLong(dodpro.getProperty("alma_rate_limit_min_sleep_millis")),
                                  Long.parseLong(dodpro.getProperty("alma_rate_limit_sleep_variation_millis")),
                                  dodpro.getProperty("lang"),
                                  Integer.parseInt(dodpro.getProperty("connect_timeout")),
                                  Integer.parseInt(dodpro.getProperty("read_timeout")),
                                  Long.parseLong(dodpro.getProperty("cache_timeout", "0")),
                                  Integer.parseInt(dodpro.getProperty("max_retries", "3"))
        );
    }
    
    
    public static AlmaSRUClient getAlmaSruClient() throws IOException, AlmaConnectionException {
        Properties dodpro = new Properties();
        try (Reader propStream = new InputStreamReader(Thread.currentThread()
                                                             .getContextClassLoader()
                                                             .getResourceAsStream("almaclient.properties"),
                                                       Charsets.UTF_8)) {
            dodpro.load(propStream);
        }
        return new AlmaSRUClient(dodpro.getProperty("alma.sru.url"),
                                 100,
                                 Long.parseLong(dodpro.getProperty("alma_rate_limit_min_sleep_millis")),
                                 Long.parseLong(dodpro.getProperty("alma_rate_limit_sleep_variation_millis")),
                                 Integer.parseInt(dodpro.getProperty("connect_timeout")),
                                 Integer.parseInt(dodpro.getProperty("read_timeout")),
                                 Long.parseLong(dodpro.getProperty("cache_timeout", "0")),
                                 Integer.parseInt(dodpro.getProperty("max_retries", "3"))
        );
    }
}
