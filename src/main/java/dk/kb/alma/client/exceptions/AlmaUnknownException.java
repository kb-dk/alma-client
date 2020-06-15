package dk.kb.alma.client.exceptions;

import dk.kb.alma.client.utils.StringListUtils;

import javax.ws.rs.core.Response;
import java.net.URI;

public class AlmaUnknownException extends AlmaClientException {
  
    public AlmaUnknownException(String operation,
                                String entityMessage,
                                URI currentURI,
                                Response response,
                                Exception e) {
        super("Failed to " + operation + " " + entityMessage + "on '" + currentURI + "' with response '"
              + response.readEntity(String.class) + "' with headers '"+response.getStringHeaders()+"'",
              operation,
              entityMessage,
              currentURI,
              response,
              e);
     
    }
    
}
