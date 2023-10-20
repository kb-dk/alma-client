package dk.kb.alma.client;

import dk.kb.alma.client.exceptions.AlmaConnectionException;
import dk.kb.alma.client.exceptions.AlmaKnownException;
import dk.kb.alma.gen.holdings.Holdings;
import dk.kb.alma.gen.items.Item;
import dk.kb.alma.gen.items.Items;
import dk.kb.alma.gen.purchase_requests.Amount;
import dk.kb.alma.gen.purchase_requests.PurchaseRequest;
import dk.kb.alma.gen.purchase_requests.PurchaseRequests;
import dk.kb.alma.gen.purchase_requests.ResourceMetadata;
import dk.kb.alma.gen.user_requests.PickupLocationTypes;
import dk.kb.alma.gen.user_requests.RequestTypes;
import dk.kb.alma.gen.user_requests.UserRequest;
import dk.kb.alma.gen.user_requests.UserRequests;
import dk.kb.alma.gen.user_resource_sharing_request.UserResourceSharingRequest;
import dk.kb.alma.gen.users.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class AlmaUserClientTest {
    
    
    private static AlmaRestClient client;
    private final String TEST_USER_IN_PSB = "jjeg";

    @BeforeAll
    static void setupAlmaClient() throws IOException {
        client = TestUtils.getAlmaClient();
    }
    
    @Test
    public synchronized void testGetUser() throws AlmaConnectionException, IOException {
        AlmaUserClient almaClient = new AlmaUserClient(client);
        //This user must be present in PSB!!
        User user = almaClient.getUser(TEST_USER_IN_PSB);
        final String TEST_USER_FIRST_NAME = "Jørgen";
        assertEquals(TEST_USER_FIRST_NAME, user.getFirstName().trim());
    }
    
    
    @Test
    public synchronized void testGetInvalidUser() throws AlmaConnectionException, IOException {
        AlmaUserClient almaClient = new AlmaUserClient(client);
        String invalidUser = "dsfdksdfsddfs";
        AlmaKnownException exceptionThrown = Assertions.assertThrows(AlmaKnownException.class, () -> {
            User user = almaClient.getUser(invalidUser);
        });
        assertEquals(exceptionThrown.getErrorCode(),"401861");
        assertEquals(exceptionThrown.getErrorMessage(), "User with identifier "+invalidUser+" was not found.");
    }
    
    
    @Test
    public synchronized void testCreateUpdateAndCancelUserRequest() throws AlmaConnectionException, IOException {
        AlmaInventoryClient almaInventoryClient = new AlmaInventoryClient(client);

        AlmaUserClient almaUserClient = new AlmaUserClient(client);

        final String mmsID = "99122652604305763";
        final String itemID = "231882066200005763";
        final String userID = TEST_USER_IN_PSB;

        cancelExistingUserRequestIfPresent(almaInventoryClient, almaUserClient, mmsID, itemID, userID);
    
        UserRequest newrequest = new UserRequest();

        newrequest.setUserPrimaryId(userID);
        newrequest.setMmsId(mmsID);

        newrequest.setItemId(itemID);
        newrequest.setPickupLocationLibrary("SBMAG");
        newrequest.setRequestType(RequestTypes.HOLD);
        newrequest.setPickupLocationType(PickupLocationTypes.LIBRARY);
        
        UserRequest request = almaUserClient.createRequest(newrequest);
        
        assertTrue(request.getTitle().startsWith("Ja!"));
        
        String newComment = "integration test comment";
        request.setComment(newComment);
        request.setAdditionalId("new Additional-id value");
        UserRequest updatedRequest = almaUserClient.updateRequest(request);
        
        assertEquals(newComment, updatedRequest.getComment());
        //Test that the additional id is NOT settable
        assertNotEquals("new Additional-id value",updatedRequest.getAdditionalId());
        
        boolean success = almaUserClient.cancelRequest(userID, request.getRequestId(), "PatronNotInterested", false);
        
        assertTrue(success);
    }
    
    private void cancelExistingUserRequestIfPresent(AlmaInventoryClient almaInventoryClient,
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
    public synchronized void testUpdateUserRequestWithInvalidRequestId() throws AlmaConnectionException, IOException {
        AlmaInventoryClient almaInventoryClient = new AlmaInventoryClient(client);
    
        AlmaUserClient almaUserClient = new AlmaUserClient(client);
    
        final String mmsID = "99122652604305763";
        final String itemID = "231882066200005763";
        final String userID = TEST_USER_IN_PSB;
    
        cancelExistingUserRequestIfPresent(almaInventoryClient, almaUserClient, mmsID, itemID, userID);
    
        UserRequest newrequest = new UserRequest();
    
        newrequest.setUserPrimaryId(userID);
        newrequest.setMmsId(mmsID);
    
        newrequest.setItemId(itemID);
        newrequest.setPickupLocationLibrary("SBMAG");
        newrequest.setRequestType(RequestTypes.HOLD);
        newrequest.setPickupLocationType(PickupLocationTypes.LIBRARY);
    
        UserRequest request = almaUserClient.createRequest(newrequest);
    
        assertTrue(request.getTitle().startsWith("Ja!"));
    
        String oldRequestId = request.getRequestId();
        request.setRequestId("00000000000000");
        Assertions.assertThrows(AlmaKnownException.class, () -> {
            try {
                almaUserClient.updateRequest(request);
            } finally {
                boolean success = almaUserClient.cancelRequest(userID, oldRequestId, "PatronNotInterested", false);
                assertTrue(success);
            }
        });
        
        
    }

    @Test
    public synchronized void testCreateAndCancelResourceSharingRequest() throws IOException {
        AlmaUserClient almaClient = new AlmaUserClient(client);
        String requester = TEST_USER_IN_PSB;
        
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

        String userRequestId = getUserRequestIdFromResourceSharingRequest(createdRequest);

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
        request.setPickupLocationType("LIBRARY");
        UserResourceSharingRequest.PickupLocation pickupLocation = new UserResourceSharingRequest.PickupLocation();
        pickupLocation.setValue("SBMAG");
        request.setPickupLocation(pickupLocation);
        return request;
    }

    public String getUserRequestIdFromResourceSharingRequest(UserResourceSharingRequest resourceSharingRequest) throws AlmaConnectionException {
        String userRequestLink = resourceSharingRequest.getUserRequest().getLink();
        return userRequestLink.substring(userRequestLink.lastIndexOf("/")+1);

    }

    @Test
    public void testCreateGetAndCancelPurchaseRequest(){
        final String mmsID = "99122652604305763";
        final String userID = TEST_USER_IN_PSB;
        client.setCachingEnabled(false);
        AlmaUserClient almaUserClient = new AlmaUserClient(client);

        //pretest. Delete existing purchase requests for user.
        PurchaseRequests purchaseRequests = almaUserClient.getPurchaseRequests(TEST_USER_IN_PSB);
        if (purchaseRequests.getTotalRecordCount()>0) {
            deleteAllPurchaseRequestsForUser(purchaseRequests.getPurchaseRequests());
            purchaseRequests = almaUserClient.getPurchaseRequests(TEST_USER_IN_PSB);
            assertEquals(0, (int) purchaseRequests.getTotalRecordCount(), "Efter sletning er der stadig purchase requests! Antal: " + purchaseRequests.getTotalRecordCount());
        }

        //create
        PurchaseRequest purchaseRequest = initPurchaseRequest(mmsID);
        PurchaseRequest result = almaUserClient.createPurchaseRequest(userID, purchaseRequest);
        final String purchaseRequestRequestId = result.getRequestId();
        // alma needs time to recocignize the new purchase request for some reason!!!
        justWait();

        //get all
        purchaseRequests = almaUserClient.getPurchaseRequests(TEST_USER_IN_PSB);
        assertEquals(1, (int) purchaseRequests.getTotalRecordCount(), "Forventet 1, antal fundet: " + purchaseRequests.getTotalRecordCount());
        final List<PurchaseRequest> purchaseRequestList = purchaseRequests.getPurchaseRequests();
        boolean idFound = purchaseRequestList.stream().anyMatch(
                tmpPurchaseRequest -> tmpPurchaseRequest.getRequestId().equalsIgnoreCase(purchaseRequestRequestId)
        );
        Assertions.assertTrue(idFound, "Kunne ikke finde id " + purchaseRequestRequestId);

        //cancel
        deleteAllPurchaseRequestsForUser(purchaseRequestList);
        purchaseRequests = almaUserClient.getPurchaseRequests(TEST_USER_IN_PSB);
        assertEquals(0, (int) purchaseRequests.getTotalRecordCount(), "Forventet 0, fundet " + purchaseRequests.getTotalRecordCount());

    }

    private static void justWait() {
        System.out.println("ventetid starter");
        try {
            Thread.sleep(3000);
            System.out.println("ventetid forbi");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testInvalidUserPurchaseRequest(){
        final String mmsID = "findes_ikke";
        final String userID = "findes heller ikke";
        AlmaUserClient almaUserClient = new AlmaUserClient(client);

        //create
        PurchaseRequest purchaseRequest = initPurchaseRequest(mmsID);

        //test exception type
        Exception exception = Assertions.assertThrows(
                AlmaKnownException.class, () -> almaUserClient.createPurchaseRequest(userID, purchaseRequest)
        );
        // test message
        final String expectedErrorMessage = "User with identifier " + userID + " of type  was not found.";
        Assertions.assertTrue(exception.getMessage().contains(expectedErrorMessage));
    }

    private static PurchaseRequest initPurchaseRequest(String mmsID) {
        PurchaseRequest purchaseRequest = new PurchaseRequest();
        PurchaseRequest.Format format = new PurchaseRequest.Format();
        format.setValue("E");
        purchaseRequest.setFormat(format);

        PurchaseRequest.OwningLibrary owningLibrary = new PurchaseRequest.OwningLibrary();
        owningLibrary.setValue("SBMAG");
        purchaseRequest.setOwningLibrary(owningLibrary);

        Amount amount = new Amount();
        Amount.Currency currency = new Amount.Currency();
        currency.setValue("DKK");
        amount.setCurrency(currency);
        amount.setSum("500");
        purchaseRequest.setEstimatedCost(amount);

        ResourceMetadata resourceMetadata = new ResourceMetadata();
        ResourceMetadata.MmsId resourceMetadateMmsId = new ResourceMetadata.MmsId();
        resourceMetadateMmsId.setValue(mmsID);
        resourceMetadata.setMmsId(resourceMetadateMmsId);
        purchaseRequest.setResourceMetadata(resourceMetadata);

        PurchaseRequest.Fund fund= new PurchaseRequest.Fund();
        fund.setValue("AUL_MONO");
        purchaseRequest.setFund(fund);
        return purchaseRequest;
    }

    private void deleteAllPurchaseRequestsForUser(List<PurchaseRequest> purchaseRequestList) {
        AlmaAcquisitionsClient almaAcquisitionsClient = new AlmaAcquisitionsClient(client);
        purchaseRequestList.forEach(tmpPurchaseRequest -> almaAcquisitionsClient.deletePurchaseRequest(tmpPurchaseRequest.getRequestId()));
    }

}
