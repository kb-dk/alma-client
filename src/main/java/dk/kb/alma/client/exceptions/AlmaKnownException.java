package dk.kb.alma.client.exceptions;

import dk.kb.alma.gen.WebServiceResult;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.stream.Collectors;

import static dk.kb.alma.client.utils.StringListUtils.notNull;

public class AlmaKnownException extends AlmaClientException {
    
    
    private final WebServiceResult result;
    private final String errorCode;
    private final String errorMessage;
    
    
    public AlmaKnownException(String operation,
                              String entityMessage,
                              URI currentURI,
                              Response response,
                              WebServiceResult result,
                              Exception e) {
        super("Failed with code " + parseErrorCode(result) + " / " + parseErrorMessage(result) + " on " + operation + " "
              + notNull(entityMessage) + " " + currentURI,
              operation,
              entityMessage,
              currentURI,
              response,
              e);
        errorCode = parseErrorCode(result);
        errorMessage= parseErrorMessage(result);
        this.result = result;
        
    }
    
    public WebServiceResult getResult() {
        return result;
    }
    
    protected static String parseErrorCode(WebServiceResult result) {
        if (result.isErrorsExist()) {
            String errorCode = result.getErrorList()
                                     .getErrors()
                                     .stream()
                                     .findFirst()
                                     .map(error -> error.getErrorCode())
                                     .orElse("");
            
            return errorCode;
            
        }
        return "";
    }
    
    protected static String parseErrorMessage(WebServiceResult result) {
        if (result.isErrorsExist()) {
            String errorMessage = result.getErrorList()
                                        .getErrors()
                                        .stream()
                                        .map(error -> error.getErrorMessage())
                                        .collect(
                                                Collectors.joining(", "));
            return errorMessage;
        }
        return "";
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
}
