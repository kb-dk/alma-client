package dk.kb.alma.client.exceptions;

import dk.kb.alma.gen.web_service_result.WebServiceResult;

import jakarta.ws.rs.core.Response;
import java.net.URI;

public class AlmaNotFoundException extends AlmaKnownException {
    public AlmaNotFoundException(String operation,
                                 String entityMessage,
                                 URI currentURI,
                                 Response response,
                                 WebServiceResult result, Exception e) {
        super(operation, entityMessage, currentURI, response, result, e);
    }
}
