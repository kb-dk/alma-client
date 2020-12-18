package dk.kb.alma.client;

import dk.kb.alma.client.exceptions.AlmaConnectionException;
import dk.kb.alma.client.exceptions.AlmaKnownException;
import dk.kb.alma.gen.fees.Fee;
import dk.kb.alma.gen.fees.Fees;
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
import org.junit.jupiter.api.Disabled;
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
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
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
    
    
    
    //FEES
    //TODO fix these tests
   /*
    @Test
    void testGetUser() {
        User user = getNewUser();
        when(restUtil.get(anyString(), anyMap(), Mockito.eq(User.class))).thenReturn(user);
        
        User result = almaClient.getUser();
        Assertions.assertEquals(user.getPrimaryId(), result.getPrimaryId());
    }
    
    @Test
    void testGetUserById() {
        User user = getNewUser();
        when(restUtil.get(anyString(), anyMap(), Mockito.eq(User.class))).thenReturn(user);
        
        User result = almaClient.getUser(userId);
        Assertions.assertEquals(user.getPrimaryId(), result.getPrimaryId());
    }
    
    @Test
    void testGetAllActiveFees() {
        Fees fees = getNewFees();
        when(restUtil.get(anyString(), anyMap(), Mockito.eq(Fees.class))).thenReturn(fees);
        
        Fees result = almaClient.getAllActiveFees();
        Assertions.assertEquals(fees.getFees().get(0).getId(), result.getFees().get(0).getId());
    }
    
    @Test
    void testGetUserFee() {
        Fee fee = getNewFee();
        when(restUtil.get(anyString(), anyMap(), Mockito.eq(Fee.class))).thenReturn(fee);
        
        Fee result = almaClient.getUserFee(feeId);
        Assertions.assertEquals(fee.getId(), result.getId());
    }
    
    @Test
    void testGetAllClosedFees() {
        Fees fees = getNewFees();
        when(restUtil.get(anyString(), anyMap(), Mockito.eq(Fees.class))).thenReturn(fees);
        
        Fees result = almaClient.getAllClosedFees();
        Assertions.assertEquals(fees.getFees().get(0).getId(), result.getFees().get(0).getId());
    }
    
    @Test
    void testGetAllFees() {
        Fees fees = getNewFees();
        when(restUtil.get(anyString(), anyMap(), Mockito.eq(Fees.class))).thenReturn(fees);
        
        Fees result = almaClient.getAllFees(userId, AlmaClient.FeeStatus.ACTIVE.toString());
        Assertions.assertEquals(fees.getFees().get(0).getId(), result.getFees().get(0).getId());
    }
    
    @Test
    void testPayUserFee() {
        Fee fee = getNewFee();
        when(restUtil.post(anyString(), anyMap(), anyString(), Mockito.eq(Fee.class))).thenReturn(fee);
        
        Fee result = almaClient.payUserFee(userId, feeId, "0", METHOD, COMMENT, EXTERNAL_TRANSACTION_ID);
        Assertions.assertEquals(fee.getId(), result.getId());
    }
    
    @Test
    void testPayAllFees() {
        Fees fees = getNewFees();
        when(restUtil.post(anyString(), anyMap(), anyString(), Mockito.eq(Fees.class))).thenReturn(fees);
        
        Fees result = almaClient.payAllFees(userId, METHOD, COMMENT, EXTERNAL_TRANSACTION_ID);
        Assertions.assertEquals(fees.getFees().get(0).getId(), result.getFees().get(0).getId());
    }
    
    private User getNewUser() {
        User user = new User();
        user.setPrimaryId("userId");
        user.setFirstName("Test");
        user.setLastName("Unit");
        
        return user;
    }
    
    private Fee getNewFee() {
        
        Fee fee = new Fee();
        fee.setId(feeId);
        fee.setOwner(new Fee.Owner());
        fee.setStatus(new Fee.Status());
        Fee.UserPrimaryId userId = new Fee.UserPrimaryId();
        userId.setValue("userId");
        fee.setUserPrimaryId(userId);
        
        return fee;
    }
    
    private Fees getNewFees() {
        Fees fees = new Fees();
        
        fees.setTotalRecordCount(new Long(1));
        fees.setTotalSum(new Float(10));
        
        Fee fee = getNewFee();
        
        fees.getFees().add(new Fee());
        return fees;
    }*/
}