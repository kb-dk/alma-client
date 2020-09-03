package dk.kb.alma.client;

import dk.kb.alma.client.exceptions.AlmaConnectionException;
import dk.kb.alma.client.exceptions.AlmaKnownException;
import dk.kb.alma.client.exceptions.AlmaUnknownException;
import dk.kb.alma.gen.code_table.CodeTable;
import dk.kb.alma.gen.general.General;
import dk.kb.alma.gen.libraries.Libraries;
import dk.kb.alma.gen.libraries.Library;
import dk.kb.alma.gen.locations.Location;
import dk.kb.alma.gen.locations.Locations;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Corresponds to https://developers.exlibrisgroup.com/console/?url=/wp-content/uploads/alma/openapi/conf.json
 */
public class AlmaConfClient {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    private final AlmaRestClient almaRestClient;
    
    public AlmaConfClient(AlmaRestClient almaRestClient) {
        this.almaRestClient = almaRestClient;
    }
    
    /*ORG UNITS*/
    
    public Libraries getLibraries() {
        return almaRestClient.get(almaRestClient.constructLink()
                                                .path("/conf/libraries"),
                                  Libraries.class);
    }
    
    public Library getLibrary(String libraryCode) {
        return almaRestClient.get(almaRestClient.constructLink()
                                                .path("/conf/libraries/")
                                                .path(libraryCode),
                                  Library.class);
    }
    
    public Locations getLibraryLocations(String libraryCode) {
        return almaRestClient.get(almaRestClient.constructLink()
                                                .path("/conf/libraries/")
                                                .path(libraryCode)
                                                .path("/locations"),
                                  Locations.class);
    }
    
    public Location getLibraryLocation(String libraryCode, String locationCode) {
        return almaRestClient.get(almaRestClient.constructLink()
                                                .path("/conf/libraries/")
                                                .path(libraryCode)
                                                .path("/locations/")
                                                .path(locationCode),
                                  Location.class);
    }
    
    /*GENERAL*/
    
    public CodeTable getCodeTable(String codeTableName)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return getCodeTable(codeTableName, "da");
    }
    
    public CodeTable getCodeTable(String codeTableName, String lang)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        WebClient link = almaRestClient.constructLink().path("/conf/code-tables/")
                                       .path(codeTableName)
                                       .replaceQueryParam("lang", lang);
        
        return almaRestClient.get(link, CodeTable.class);
    }
    
    public General getGeneralConf() {
        return almaRestClient.get(almaRestClient.constructLink()
                                                .path("/conf/general"),
                                  General.class);
    }
    
    
}
