package dk.kb.alma.client;

import dk.kb.alma.client.utils.Utils;
import dk.kb.alma.gen.partners.Partner;
import dk.kb.alma.gen.partners.Partners;
import dk.kb.alma.gen.partners.Status;
import dk.kb.alma.gen.user_resource_sharing_request.UserResourceSharingRequest;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlmaPartnersClient {
    
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    private final AlmaRestClient almaRestClient;
    private final String partnerCode;
    
    public AlmaPartnersClient(AlmaRestClient almaRestClient) {
        this(almaRestClient, "1234");
    }
    
    public AlmaPartnersClient(AlmaRestClient almaRestClient, String partnerCode) {
        this.almaRestClient = almaRestClient;
        this.partnerCode    = partnerCode;
    }
    
    public AlmaRestClient getAlmaRestClient() {
        return almaRestClient;
    }
    
    
    public UserResourceSharingRequest getResourceSharingRequest(String lendingRequestID) {
        return getResourceSharingRequest(partnerCode, lendingRequestID);
    }
    
    public UserResourceSharingRequest getResourceSharingRequest(String partnerCode, String lendingRequestID) {
        WebClient link = almaRestClient.constructLink()
                                       .path("partners")
                                       .path(partnerCode)
                                       .path("lending-requests")
                                       .path(lendingRequestID);
        return almaRestClient.get(link, UserResourceSharingRequest.class);
    }
    
    public Partners getPartners(Integer offset, Integer limit, Status status) {
        WebClient link = almaRestClient.constructLink()
                                       .path("partners");
        link.query("offset", Utils.nullable(offset).orElse(0));
        link.query("limit", Utils.nullable(limit).orElse(10));
        Utils.nullable(status).ifPresent(value -> link.query("status", value.value()));
        
        return almaRestClient.get(link, Partners.class);
    }
    
    public Partner getPartner(String partnerCode) {
        WebClient link = almaRestClient.constructLink()
                                       .path("partners")
                                       .path(partnerCode);
        return almaRestClient.get(link, Partner.class);
    }
}
