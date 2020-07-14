package dk.kb.alma.client.exceptions;

public class MarcXmlException extends Exception {
    public MarcXmlException(String message, Exception e) {
        super(message, e);
    }

    public MarcXmlException(String message) {
        super(message);
    }
}
