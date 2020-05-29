package dk.kb.alma.client.exceptions;

import javax.ws.rs.core.Response;
import java.net.URI;

public class AlmaUnknownException extends RuntimeException {
    public AlmaUnknownException(String operation,
                                String entityMessage,
                                URI currentURI,
                                Response response,
                                Exception e) {
        super("Failed to " + operation + " " + entityMessage + "on '" + currentURI + "' with response '"
              + response + "'", e);
    }
    
}
