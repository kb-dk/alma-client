package dk.kb.alma.client;

import dk.kb.alma.client.exceptions.AlmaConnectionException;
import dk.kb.alma.client.exceptions.AlmaKnownException;
import dk.kb.alma.client.exceptions.AlmaUnknownException;
import dk.kb.alma.gen.user_requests.UserRequest;
import dk.kb.alma.gen.user_resource_sharing_request.UserResourceSharingRequest;
import dk.kb.alma.gen.users.User;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class AlmaUserClient {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    private final AlmaRestClient almaRestClient;
    
    public AlmaUserClient(AlmaRestClient almaRestClient) {
        this.almaRestClient = almaRestClient;
    }
    
    public AlmaRestClient getAlmaRestClient() {
        return almaRestClient;
    }
    
    public User getUser(String userID) throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return almaRestClient.get(almaRestClient.constructLink()
                                                .path("/users/")
                                                .path(userID), User.class);
    }
    
    public UserRequest getRequest(String userId, String requestId)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return almaRestClient.get(almaRestClient.constructLink()
                                                .path("/users/")
                                                .path(userId)
                                                .path("/requests/")
                                                .path(requestId), UserRequest.class);
    }
    
    public UserResourceSharingRequest getResourceSharingRequest(String userId, String requestId)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return almaRestClient.get(almaRestClient.constructLink().path("/users/")
                                                .path(userId)
                                                .path("/resource-sharing-requests/")
                                                .path(requestId), UserResourceSharingRequest.class);
    }
    
    
    
    /**
     * Cancel request in Alma
     *
     * @param userId     Id of the user with the request
     * @param requestId  The request id
     * @param reasonCode Code of the cancel reason. Must be a value from the code table 'RequestCancellationReasons'
     * @param notifyUser Indication of whether the user should be notified
     * @return True if the request is cancelled successfully. False if the request was not found.
     * @throws AlmaConnectionException if something went wrong
     */
    public boolean cancelRequest(String userId, String requestId, String reasonCode, boolean notifyUser)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return cancelRequest(userId, requestId, reasonCode, null, notifyUser);
    }
    
    /**
     * Cancel request in Alma
     *
     * @param userId     Id of the user with the request
     * @param requestId  The request id
     * @param reasonCode Code of the cancel reason. Must be a value from the code table 'RequestCancellationReasons'
     * @param note       Additional note for the user
     * @param notifyUser Indication of whether the user should be notified
     * @return True if the request is cancelled successfully. False if the request was not found.
     */
    public boolean cancelRequest(String userId, String requestId, String reasonCode, String note, boolean notifyUser)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        WebClient link = almaRestClient.constructLink().path("/users/")
                                       .path(userId)
                                       .path("/requests/")
                                       .path(requestId);
        URI currentURI = link.getCurrentURI();
        
        WebClient builder = link.query("reason", reasonCode)
                                .query("notify_user", notifyUser);
        
        if (note != null) {
            builder = builder.query("note", note);
        }
        almaRestClient.delete(builder, Void.class);
    
        almaRestClient.invalidateCacheEntry(currentURI);
        return true;
    }
    
    
    /**
     * Create request in alma
     *
     * @param request The fully populated request
     * @return The request created in Alma
     */
    public UserRequest createRequest(UserRequest request)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        
        String userId = request.getUserPrimaryId();
        String mmsId = request.getMmsId();
        String itemId = request.getItemId();
    
        WebClient link = almaRestClient.constructLink().path("/users/")
                                       .path(userId)
                                       .path("/requests")
                                       .query("user_id_type", "all_unique");
        
        if (mmsId != null) {
            link = link.query("mms_id", mmsId);
        }
        if (itemId != null) {
            link = link.query("item_pid", itemId);
        }
        return almaRestClient.post(link, UserRequest.class, request);
    }
    
    /**
     * Update request in alma
     *
     * @param request The fully populated request
     * @return The request updated in Alma
     */
    public UserRequest updateRequest(UserRequest request)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        
        String userId = request.getUserPrimaryId();
    
        WebClient link = almaRestClient.constructLink().path("/users/")
                                       .path(userId)
                                       .path("/requests")
                                       .path(request.getRequestId());
    
        return almaRestClient.put(link, UserRequest.class, request);
    }
    
    
    public UserResourceSharingRequest createResourceSharingRequest(UserResourceSharingRequest request, String userId)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        WebClient link = almaRestClient.constructLink().path("/users/")
                                       .path(userId)
                                       .path("/resource-sharing-requests");
        return almaRestClient.post(link, UserResourceSharingRequest.class, request);
        
    }
    
}
