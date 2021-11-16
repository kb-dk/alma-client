package dk.kb.alma.client.exceptions;

public class AlmaConnectionException extends RuntimeException{
    
    public AlmaConnectionException(String message) {
        super(message);
    }

    public AlmaConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
