package dk.kb.alma.client.utils;

import dk.kb.alma.client.HttpClient;
import dk.kb.alma.client.exceptions.AlmaConnectionException;
import dk.kb.alma.client.exceptions.AlmaUnknownException;
import dk.kb.alma.gen.web_service_result.Error;
import dk.kb.alma.gen.web_service_result.ErrorList;
import dk.kb.alma.gen.web_service_result.WebServiceResult;
import dk.kb.util.xml.XML;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class HttpUtils {
    
    protected final static Logger log = LoggerFactory.getLogger(HttpUtils.class);
    
    public static WebServiceResult readWebServiceResult(HttpClient.Operation operation,
                                                        URI currentURI,
                                                        WebApplicationException e,
                                                        String entityMessage,
                                                        Response response) {
        WebServiceResult result;
        try {
            result = response.readEntity(WebServiceResult.class);
        } catch (Exception e2) {
            log.error(
                    "Failed to parse response {} as WebServiceResult, but throwing based on the original exception {}",
                    response.readEntity(String.class),
                    e,
                    e2);
            throw new AlmaUnknownException(operation.name(), entityMessage, currentURI, response, e);
        }
        return result;
    }
    
    public static <E> String formatEntityMessage(E entity, WebApplicationException e) {
        String entityMessage = "";
        if (entity != null) {
            try {
                entityMessage = "with entity '" + XML.marshall(entity) + "' ";
            } catch (JAXBException jaxbException) {
                throw new AlmaConnectionException(jaxbException
                                                  + ": Failed to parse entity '"
                                                  + entity
                                                  + "' as xml, but throwing the original WebApplicationException", e);
            }
        }
        return entityMessage;
    }
    
    public static Response getResponse(WebApplicationException e) {
        Response response = e.getResponse();
        //Buffer entity so we can read the response multiple times
        response.bufferEntity();
        return response;
    }
    
    @Nullable
    public static Error getFirstError(WebServiceResult result) {
        return Optional
                .ofNullable(result)
                .stream()
                .map(WebServiceResult::getErrorList)
                .filter(Objects::nonNull)
                .map(ErrorList::getErrors)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
    
    public static void extendTimeouts(HttpClient.Operation operation, WebClient uri, URI currentURI) {
        HTTPClientPolicy clientPolicy = WebClient.getConfig(uri).getHttpConduit().getClient();
        clientPolicy.setConnectionTimeout(clientPolicy.getConnectionTimeout() * 2);
        clientPolicy.setReceiveTimeout(clientPolicy.getReceiveTimeout() * 2);
        clientPolicy.setConnectionRequestTimeout(clientPolicy.getConnectionRequestTimeout() * 2);
        log.debug("Increased timeouts to connect={}ms and receive={}ms for the {}ing of {}",
                             clientPolicy.getConnectionTimeout(),
                             clientPolicy.getReceiveTimeout(),
                             operation.name(),
                             currentURI);
    }
    
    /**
     * Walk through the Throwable cause-tree and return it as a list
     *
     * @param throwable the throwable
     * @return a list of all the throwables that led to this throwables
     */
    public static List<Throwable> getCauses(Throwable throwable) {
        List<Throwable> result = new ArrayList<>();
        Throwable current = throwable;
        while (current != null) {
            result.add(current);
            current = current.getCause();
        }
        return result;
    }
}
