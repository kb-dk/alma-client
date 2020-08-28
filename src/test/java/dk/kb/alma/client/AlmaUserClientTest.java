package dk.kb.alma.client;

import dk.kb.alma.client.exceptions.AlmaConnectionException;
import dk.kb.alma.client.exceptions.AlmaKnownException;
import dk.kb.alma.gen.user_requests.PickupLocationTypes;
import dk.kb.alma.gen.user_requests.RequestTypes;
import dk.kb.alma.gen.user_requests.UserRequest;
import dk.kb.alma.gen.users.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;

import static dk.kb.alma.client.TestUtils.getAlmaClient;
import static org.junit.jupiter.api.Assertions.*;

class AlmaUserClientTest {
    
    
    @Test
    public void testGetUser() throws AlmaConnectionException, IOException {
        AlmaUserClient almaClient = new AlmaUserClient(getAlmaClient());
        
        User user = almaClient.getUser("thl");
        
        assertEquals("Thomas", user.getFirstName().trim());
    }
    
    
    @Test
    public void testCreateUpdateAndCancelRequest() throws AlmaConnectionException, IOException {
        AlmaUserClient almaClient = new AlmaUserClient(getAlmaClient());
    
        UserRequest newrequest = new UserRequest();
        newrequest.setUserPrimaryId("thl");
        newrequest.setMmsId("99122652604305763");
        newrequest.setItemId("231882066200005763");
        newrequest.setPickupLocationLibrary("SBL");
        newrequest.setRequestType(RequestTypes.HOLD);
        newrequest.setPickupLocationType(PickupLocationTypes.LIBRARY);
        
        UserRequest request = almaClient.createRequest(newrequest);
        
        assertTrue(request.getTitle().startsWith("Ja!"));
        
        String newComment = "integration test comment";
        request.setComment(newComment);
        UserRequest updatedRequest = almaClient.updateRequest(request);
        
        assertEquals(newComment, updatedRequest.getComment());
        
        boolean success = almaClient.cancelRequest("thl", request.getRequestId(), "PatronNotInterested", false);
        
        assertTrue(success);
    }
    
    @Test
    public void testUpdateRequestWithInvalidRequestId() throws AlmaConnectionException, IOException {
        AlmaUserClient almaClient = new AlmaUserClient(getAlmaClient());
        
        UserRequest request = almaClient.getRequest("thl", "22097291510005763");
        
        request.setRequestId("00000000000000");
        Assertions.assertThrows(AlmaKnownException.class, () -> {
            almaClient.updateRequest(request);
        });
    }
    
}