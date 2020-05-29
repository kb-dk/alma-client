package dk.kb.alma.client.exceptions;

import java.net.URI;

import static dk.kb.alma.client.utils.StringListUtils.notNull;

public class AlmaKnownException extends RuntimeException{
    
    private String operation;
    
    private String entityMessage;
    
    private URI currentURI;
    
    private String errorMessage;
    
    private String errorCode;
    
    public AlmaKnownException(String operation,
                              String entityMessage,
                              URI currentURI,
                              String errorMessage,
                              String errorCode,
                              Exception e) {
        super("Failed with code "+errorCode+" / "+errorMessage+" on " + operation + " " + notNull(entityMessage) + " " + currentURI, e);
        this.operation = operation;
        this.entityMessage = entityMessage;
        this.currentURI = currentURI;
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }
}
