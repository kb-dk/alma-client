package dk.kb.alma.client.exceptions;

import javax.ws.rs.core.Response;
import java.net.URI;

public class AlmaUnknownException extends RuntimeException {
    
    private final String operation;
    private final String entityMessage;
    private final URI currentURI;
    private final Response response;
    
    public AlmaUnknownException(String operation,
                                String entityMessage,
                                URI currentURI,
                                Response response,
                                Exception e) {
        super("Failed to " + operation + " " + entityMessage + "on '" + currentURI + "' with response '"
              + response + "'", e);
        this.operation = operation;
        this.entityMessage = entityMessage;
        this.currentURI = currentURI;
        this.response = response;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public String getEntityMessage() {
        return entityMessage;
    }
    
    public URI getCurrentURI() {
        return currentURI;
    }
    
    public Response getResponse() {
        return response;
    }
}
