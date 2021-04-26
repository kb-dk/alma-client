package dk.kb.alma.client;

import dk.kb.alma.client.exceptions.AlmaConnectionException;
import dk.kb.alma.client.exceptions.AlmaKnownException;
import dk.kb.alma.client.exceptions.AlmaUnknownException;
import dk.kb.alma.gen.general.General;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AlmaRestClient extends HttpClient {
    
    protected final static Logger log = LoggerFactory.getLogger(AlmaRestClient.class);
    
    
    public static final String APIKEY = "apikey";
    
    private final String alma_apikey;
    
    
    private final String almaEnvType;
    private final String almaHost;
    
    public AlmaRestClient(String almaTarget, String alma_apikey)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        this(almaTarget, alma_apikey,
             2000,
             3000,
             "da",
             30000,
             30000,
             TimeUnit.HOURS.toMillis(1),
             3);
    }
    
    public AlmaRestClient(String almaTarget, String alma_apikey, long minSleep, long sleepVariation, String lang)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        this(almaTarget, alma_apikey, minSleep, sleepVariation, lang, 3000, 3000, TimeUnit.HOURS.toMillis(5),3);
    }
    
    public AlmaRestClient(String almaTarget,
                          String alma_apikey,
                          long minSleep,
                          long sleepVariation,
                          String lang,
                          int connectTimeout,
                          int readTimeout,
                          long cacheTimeMillis,
                         Integer maxRetries)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        super(almaTarget,
              minSleep,
              sleepVariation,
              Stream.of(lang)
                    .filter(Objects::nonNull)
                    .filter(element -> !element.isBlank())
                    .collect(Collectors.toMap(element -> "lang", element -> element)),
              connectTimeout,
              readTimeout,
              cacheTimeMillis,
              maxRetries);
        this.alma_apikey = alma_apikey;
        
        log.debug("Getting ALMA general info to determine alma host");
        General almaGeneral = get(constructLink().path("/conf/general"), General.class);
        this.almaEnvType = almaGeneral.getEnvironmentType();
        try {
            this.almaHost = new URL(almaGeneral.getAlmaUrl()).getHost();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        log.info("Initialized {} with alma ENV={}, alma Host={}",
                 getClass().getName(),
                 this.almaEnvType,
                 this.almaHost);
        
    }
    
    protected WebClient removeAuth(WebClient link) {
        return link.replaceQueryParam(APIKEY);
    }
    
    protected WebClient addAuth(WebClient link) {
        return link.header("Authorization","apikey "+alma_apikey);
        //return link.replaceQueryParam(APIKEY, alma_apikey);
    }
    
    public String getAlmaEnvType() {
        return almaEnvType;
    }
    
    public String getAlmaHost() {
        return almaHost;
    }
}
