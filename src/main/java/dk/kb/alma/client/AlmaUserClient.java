package dk.kb.alma.client;

import dk.kb.alma.client.exceptions.AlmaConnectionException;
import dk.kb.alma.client.exceptions.AlmaKnownException;
import dk.kb.alma.client.exceptions.AlmaUnknownException;
import dk.kb.alma.gen.fees.Fee;
import dk.kb.alma.gen.fees.Fees;
import dk.kb.alma.gen.requested_resources.RequestedResource;
import dk.kb.alma.gen.user_requests.RequestTypes;
import dk.kb.alma.gen.user_requests.UserRequest;
import dk.kb.alma.gen.user_requests.UserRequests;
import dk.kb.alma.gen.user_resource_sharing_request.UserResourceSharingRequest;
import dk.kb.alma.gen.users.User;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class AlmaUserClient {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    private final AlmaRestClient almaRestClient;
    private final int batchSize;
    public enum FeeStatus {ACTIVE , INDISPUTE, EXPORTED, CLOSED};
    
    
    
    public AlmaUserClient(AlmaRestClient almaRestClient, int batchSize) {
        this.almaRestClient = almaRestClient;
        this.batchSize = batchSize;
    }
    
    public AlmaUserClient(AlmaRestClient almaRestClient) {
        this(almaRestClient, 100);
    }
    
    public AlmaRestClient getAlmaRestClient() {
        return almaRestClient;
    }
    
    /*USERS*/
    
    public User getUser(String userID) throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return getUser(userID, "all_unique");
    }
    
    public User getUser(String userID, String userIDType)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return almaRestClient.get(almaRestClient.constructLink()
                                                .path("/users/")
                                                .path(userID)
                                                .query("user_id_type", userIDType), User.class);
    }
    
    /*LOANS*/
    
    /*REQUESTS*/
    public Iterator<UserRequest> getRequests(@NotNull String userId, RequestTypes requestType, String status)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        
        Function<Integer, AutochainingIterator.IteratorOffset<Integer, Iterator<UserRequest>>>
                nextIteratorFunction
                = offset -> {
            if (offset == null) {
                offset = 0;
            }
            WebClient query = almaRestClient.constructLink()
                                            .path("/users/")
                                            .path(userId)
                                            .path("/requests/")
                                            .query("user_id_type", "all_unique")
                                            .query("offset", offset)
                                            .query("limit", batchSize);
            if (requestType != null){
                query = query.query("request_type", requestType);
            }
            if (status != null){
                query = query.query("status", status);
            }
            List<UserRequest> batchOfRequestedResources =
                    almaRestClient.get(query,UserRequests.class).getUserRequests();
            return AutochainingIterator.IteratorOffset.of(offset + batchOfRequestedResources.size(),
                                                          batchOfRequestedResources.iterator());
        };
        
        return new AutochainingIterator<>(nextIteratorFunction);
    }
    
    public UserRequest getRequest(String userId, String requestId)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return almaRestClient.get(almaRestClient.constructLink()
                                                .path("/users/")
                                                .path(userId)
                                                .path("/requests/")
                                                .path(requestId), UserRequest.class);
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
    
    
    
    /*Resource sharing requests*/
    
    public UserResourceSharingRequest getResourceSharingRequest(String userId, String requestId)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return almaRestClient.get(almaRestClient.constructLink().path("/users/")
                                                .path(userId)
                                                .path("/resource-sharing-requests/")
                                                .path(requestId), UserResourceSharingRequest.class);
    }
    
    
    public UserResourceSharingRequest createResourceSharingRequest(UserResourceSharingRequest request, String userId)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        WebClient link = almaRestClient.constructLink().path("/users/")
                                       .path(userId)
                                       .path("/resource-sharing-requests");
        return almaRestClient.post(link, UserResourceSharingRequest.class, request);
        
    }
    
    
    public UserResourceSharingRequest createResourceSharingRequest(UserResourceSharingRequest request,
                                                                   String userId,
                                                                   boolean overrideBlocks)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        WebClient link = almaRestClient.constructLink()
                                       .path("/users/")
                                       .path(userId)
                                       .path("/resource-sharing-requests")
                                       .query("override_blocks", String.valueOf(overrideBlocks));
        return almaRestClient.post(link, UserResourceSharingRequest.class, request);
        
    }
    
    
    
    /*******************
     * Fees
     ********************/
   
    public Fee getUserFee(String userId, String feeId) {
        WebClient link = almaRestClient.constructLink()
                                       .path("/users/")
                                       .path(userId)
                                       .path("/fees/")
                                       .path(feeId)
                                       .query("user_id_type", "all_unique");
        return almaRestClient.get(link, Fee.class);
    }
    
    public Fees getAllFees(String userId, FeeStatus status) {
        WebClient link = almaRestClient.constructLink()
                                       .path("/users/")
                                       .path(userId)
                                       .path("/fees")
                                       .query("user_id_type", "all_unique")
                                       .query("status", status.toString());
    
        return almaRestClient.get(link, Fees.class);
    }
    
    public Fees getAllActiveFees(String userId) {
        return getAllFees(userId, FeeStatus.ACTIVE);
    }
    
    public Fees getAllClosedFees(String userId) {
        return getAllFees(userId, FeeStatus.CLOSED);
    }
    
    
    public Fee payUserFee(String userId, String feeId, String amount, String method, String comment, String external_transaction_id) {
        WebClient link = almaRestClient.constructLink()
                                       .path("/users/")
                                       .path(userId)
                                       .path("/fees/")
                                       .path(feeId)
                                       .query("user_id_type", "all_unique")
                                       .query("op", "pay")
                                       .query("amount", amount)
                                       .query("method", method)
                                       .query("comment", comment)
                                       .query("external_transaction_id", external_transaction_id);
        return almaRestClient.post(link, Fee.class, null);
    
    }
    
    public Fees payAllFees(String userId, String method, String comment, String external_transaction_id){
        WebClient link = almaRestClient.constructLink()
                                       .path("/users/")
                                       .path(userId)
                                       .path("/fees/")
                                       .path("all")
                                       .query("user_id_type", "all_unique")
                                       .query("op", "pay")
                                       .query("amount", "ALL")
                                       .query("method", method)
                                       .query("comment", comment)
                                       .query("external_transaction_id", external_transaction_id);
        return almaRestClient.post(link, Fees.class, null);
    }
    
}
