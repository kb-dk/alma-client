package dk.kb.alma.client;

import dk.kb.alma.gen.user_resource_sharing_request.UserResourceSharingRequest;

public class AlmaPartnerClient {

    private final AlmaRestClient almaRestClient;

    public AlmaPartnerClient(AlmaRestClient almaRestClient) {
        this.almaRestClient = almaRestClient;
    }

    public AlmaRestClient getAlmaRestClient() {
        return almaRestClient;
    }

    public UserResourceSharingRequest getLendingRequest(String partnerCode, String requestId) {
        return almaRestClient.get(almaRestClient.constructLink()
                .path("/partners/")
                .path(partnerCode)
                .path("lending-requests")
                .path(requestId), UserResourceSharingRequest.class);
    }


}
