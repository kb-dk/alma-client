package dk.kb.alma.client;

import dk.kb.alma.client.exceptions.AlmaConnectionException;
import dk.kb.alma.client.exceptions.AlmaKnownException;
import dk.kb.alma.gen.holdings.Holdings;
import dk.kb.alma.gen.items.Item;
import dk.kb.alma.gen.items.Items;
import dk.kb.alma.gen.user_requests.PickupLocationTypes;
import dk.kb.alma.gen.user_requests.RequestTypes;
import dk.kb.alma.gen.user_requests.UserRequest;
import dk.kb.alma.gen.user_requests.UserRequests;
import dk.kb.alma.gen.user_resource_sharing_request.UserResourceSharingRequest;
import dk.kb.alma.gen.users.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static dk.kb.alma.client.TestUtils.getAlmaClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AlmaUserClientTest {
    
    
    @Test
    public synchronized void testGetUser() throws AlmaConnectionException, IOException {
        AlmaUserClient almaClient = new AlmaUserClient(getAlmaClient());
        
        User user = almaClient.getUser("thl");
        
        assertEquals("Thomas", user.getFirstName().trim());
    }
    
    
    @Test
    public synchronized void testCreateUpdateAndCancelRequest() throws AlmaConnectionException, IOException {
        AlmaInventoryClient almaInventoryClient = new AlmaInventoryClient(getAlmaClient());

        AlmaUserClient almaUserClient = new AlmaUserClient(getAlmaClient());

        final String mmsID = "99122652604305763";
        final String itemID = "231882066200005763";
        final String userID = "thl";

        cancelExistingRequestIfPresent(almaInventoryClient, almaUserClient, mmsID, itemID, userID);
    
        UserRequest newrequest = new UserRequest();

        newrequest.setUserPrimaryId(userID);
        newrequest.setMmsId(mmsID);

        newrequest.setItemId(itemID);
        newrequest.setPickupLocationLibrary("SBL");
        newrequest.setRequestType(RequestTypes.HOLD);
        newrequest.setPickupLocationType(PickupLocationTypes.LIBRARY);
        
        UserRequest request = almaUserClient.createRequest(newrequest);
        
        assertTrue(request.getTitle().startsWith("Ja!"));
        
        String newComment = "integration test comment";
        request.setComment(newComment);
        UserRequest updatedRequest = almaUserClient.updateRequest(request);
        
        assertEquals(newComment, updatedRequest.getComment());
        
        boolean success = almaUserClient.cancelRequest(userID, request.getRequestId(), "PatronNotInterested", false);
        
        assertTrue(success);
    }
    
    private void cancelExistingRequestIfPresent(AlmaInventoryClient almaInventoryClient,
                                                AlmaUserClient almaUserClient,
                                                String mmsID,
                                                String itemID,
                                                String userID) {
        Holdings holdings = almaInventoryClient.getBibHoldings(mmsID);
        List<String> holdingIDs = holdings.getHoldings()
                                          .stream()
                                          .map(holding -> holding.getHoldingId())
                                          .collect(Collectors.toList());
        for (String holdingID : holdingIDs) {
            Items items = almaInventoryClient.getItems(mmsID, holdingID);
            for (Item item : items.getItems()) {
                if (item.getItemData().getPid().equals(itemID)) {
                    UserRequests requests = almaInventoryClient.getItemRequests(mmsID,
                                                                                holdingID,
                                                                                item.getItemData()
                                                                                    .getPid());
                    for (UserRequest userRequest : requests.getUserRequests()) {
                        if (userRequest.getUserPrimaryId().equals(userID)) {
                            almaUserClient.cancelRequest(userRequest.getUserPrimaryId(),
                                                         userRequest.getRequestId(),
                                                         "reason",
                                                         false);
                        }
                    }
                }

            }
        }
    }

    @Test
    public synchronized void testUpdateRequestWithInvalidRequestId() throws AlmaConnectionException, IOException {
        AlmaUserClient almaClient = new AlmaUserClient(getAlmaClient());
        
        UserRequest request = almaClient.getRequest("thl", "22097291510005763");
        
        request.setRequestId("00000000000000");
        Assertions.assertThrows(AlmaKnownException.class, () -> {
            almaClient.updateRequest(request);
        });
    }

    @Test
    public synchronized void testCreateAndCancelRSRequest() throws IOException {
        AlmaUserClient almaClient = new AlmaUserClient(getAlmaClient());
        String requester = "thl";
        
        UserResourceSharingRequest resourceSharingRequest = createResourceSharingRequest();
        Iterator<UserRequest> requests = almaClient.getRequests(requester, null, null);
        while (requests.hasNext()) {
            UserRequest request = requests.next();
            if (request.getTitle().equals(resourceSharingRequest.getTitle())){
                almaClient.cancelRequest(requester,
                                         request.getRequestId(),
                                         "reason",
                                         false);
            }
        }
        
        
        UserResourceSharingRequest createdRequest = almaClient.createResourceSharingRequest(resourceSharingRequest, requester);

        createdRequest = almaClient.getResourceSharingRequest(requester, createdRequest.getRequestId());

        String userRequestId = getUserRequestIdFromRSRequest(createdRequest);

        boolean cancelled = almaClient.cancelRequest(requester, userRequestId, "AnotherReason", false);

        assertTrue(cancelled);

    }

    private UserResourceSharingRequest createResourceSharingRequest() {
        UserResourceSharingRequest request = new UserResourceSharingRequest();
        request.setTitle("AlmaUserClient integration test-" + new Random().nextInt());
        UserResourceSharingRequest.Format format = new UserResourceSharingRequest.Format();
        format.setValue("PHYSICAL");
        request.setFormat(format);
        UserResourceSharingRequest.CitationType citationType = new UserResourceSharingRequest.CitationType();
        citationType.setValue("BK");
        request.setCitationType(citationType);
        request.setAgreeToCopyrightTerms(true);
        UserResourceSharingRequest.PickupLocation pickupLocation = new UserResourceSharingRequest.PickupLocation();
        pickupLocation.setValue("SBSVB");
        request.setPickupLocation(pickupLocation);
        return request;
    }

    public String getUserRequestIdFromRSRequest(UserResourceSharingRequest resourceSharingRequest) throws AlmaConnectionException {
        String userRequestLink = resourceSharingRequest.getUserRequest().getLink();
        return userRequestLink.substring(userRequestLink.lastIndexOf("/")+1);

    }

}