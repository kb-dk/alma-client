package dk.kb.alma.client;

import dk.kb.alma.gen.user_resource_sharing_request.UserResourceSharingRequest;
import dk.kb.alma.gen.user_resource_sharing_request.UserResourceSharingRequests;
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
        this.partnerCode = partnerCode;
    }
    
    public AlmaRestClient getAlmaRestClient() {
        return almaRestClient;
    }
    
    
    public UserResourceSharingRequest getResourceSharingRequest(String lendingRequestID){
        return getResourceSharingRequest(partnerCode, lendingRequestID);
    }
    
    public UserResourceSharingRequest getResourceSharingRequest(String partnerCode, String lendingRequestID){
        WebClient link = almaRestClient.constructLink()
                                       .path("partners")
                                       .path(partnerCode)
                                       .path("lending-requests")
                                       .path(lendingRequestID);
        return almaRestClient.get(link, UserResourceSharingRequest.class);
    }

    /**
     * Currently the only supported action is 'mark_reported'.
     * Optional parameter may be null.
     * @param library The resource sharing library from which lending requests should be retrieved. Mandatory.
     * @param status The status of lending requests to retrieve. Optional.
     * @param printed The 'printed' value of lending requests to retrieve. Optional. Possible values: Y, N.
     * @param reported The 'reported' value of lending requests to retrieve. Optional. Possible values: Y, N.
     * @param partner The partner value. Optional.
     * @param requestedFormat Requested format of the resource. Optional.
     * @param suppliedFormat Supplied Format of the resource. Optional.
     */
    public UserResourceSharingRequests actOnLendingRequests(String library, String status, String printed, String reported, String partner, String requestedFormat, String suppliedFormat) {
        WebClient link = almaRestClient.constructLink().path("task-lists/rs/lending-requests");
        link = tryAddQueryParameter(link, "op", "mark_reported");
        link = tryAddQueryParameter(link, "library", library);
        link = tryAddQueryParameter(link, "status", status);
        link = tryAddQueryParameter(link, "printed", printed);
        link = tryAddQueryParameter(link, "reported", reported);
        link = tryAddQueryParameter(link, "partner", partner);
        link = tryAddQueryParameter(link, "requested_format", requestedFormat);
        link = tryAddQueryParameter(link, "supplied_format", suppliedFormat);
        return almaRestClient.post(link, UserResourceSharingRequests.class, "");
    }

    private WebClient tryAddQueryParameter(WebClient link, String parameterKey, String parameterValue) {
        if(parameterValue != null){
            return link.query(parameterKey, parameterValue);
        } else {
            return link;
        }
    }
}
