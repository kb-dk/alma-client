package dk.kb.alma.client.exceptions;

import jakarta.ws.rs.core.Response;
import java.net.URI;

public class AlmaClientException extends RuntimeException {
    
    
    private final String operation;
    private final String entityMessage;
    private final URI currentURI;
    private final Response response;
    
    public AlmaClientException(String operation, String entityMessage, URI currentURI, Response response) {
        this.operation = operation;
        this.entityMessage = entityMessage;
        this.currentURI = currentURI;
        this.response = response;
    }
    
    public AlmaClientException(String message,
                               String operation,
                               String entityMessage,
                               URI currentURI,
                               Response response) {
        super(message);
        this.operation = operation;
        this.entityMessage = entityMessage;
        this.currentURI = currentURI;
        this.response = response;
    }
    
    public AlmaClientException(String message,
    
                               String operation,
                               String entityMessage,
                               URI currentURI,
                               Response response,
                               Throwable cause) {
        super(message, cause);
        this.operation = operation;
        this.entityMessage = entityMessage;
        this.currentURI = currentURI;
        this.response = response;
    }
    
    
    public AlmaClientException(String message,
    
                               boolean enableSuppression,
                               boolean writableStackTrace,
                               String operation, String entityMessage, URI currentURI, Response response,
                               Throwable cause) {
        super(message, cause, enableSuppression, writableStackTrace);
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
    
    @Override
    public String toString() {
        return this.getClass().getName() +"{" +
               "operation='" + operation + '\'' +
               ", entityMessage='" + entityMessage + '\'' +
               ", currentURI=" + currentURI +
               ", response=" + (response==null?null:response.readEntity(String.class)) +
               '}';
    }
}
