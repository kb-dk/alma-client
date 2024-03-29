package dk.kb.alma.client;

import com.google.common.collect.Lists;
import dk.kb.alma.client.exceptions.AlmaKnownException;
import dk.kb.alma.client.utils.Utils;
import dk.kb.alma.gen.requested_resources.RequestedResource;
import dk.kb.alma.gen.requested_resources.RequestedResources;
import dk.kb.alma.gen.user_resource_sharing_request.UserResourceSharingRequests;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import static dk.kb.alma.client.utils.Utils.withDefault;

public class AlmaTasksClient {
    
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    private final AlmaRestClient almaRestClient;
    private final int batchSize;
    
    public AlmaTasksClient(AlmaRestClient almaRestClient) {
        this(almaRestClient, 100);
    }
    
    public AlmaTasksClient(AlmaRestClient almaRestClient, int batchSize) {
        this.almaRestClient = almaRestClient;
        this.batchSize = batchSize;
    }
    
    public AlmaRestClient getAlmaRestClient() {
        return almaRestClient;
    }
    
    
    /*Requested Resources*/
    
    public Iterator<RequestedResource> getRequestedResourceIterator(String libraryId,
                                                                    String circulationDeskName,
                                                                    String location,
                                                                    boolean allOrNothing,
                                                                    Logger... errorLoggers) {
        Function<Integer, AutochainingIterator.IteratorOffset<Integer, Iterator<RequestedResource>>>
                nextIteratorFunction
                = offset -> {
            if (offset == null) {
                offset = 0;
            }
            List<Logger> loggers = Lists.asList(log,errorLoggers);
            List<RequestedResource> batchOfRequestedResources = getBatchOfRequestedResources(batchSize,
                                                                                             offset,
                                                                                             libraryId,
                                                                                             circulationDeskName,
                                                                                             location,
                                                                                             allOrNothing,
                                                                                             loggers);
            log.info("Retrieved requests {} to {} for (libraryID={},circulationDesk={},location={})",
                     offset,
                     offset + batchOfRequestedResources.size(),
                     libraryId,
                     circulationDeskName,
                     location);
            
            if (batchOfRequestedResources.size() < batchSize){
                batchOfRequestedResources = Utils.toModifiableList(batchOfRequestedResources);
                batchOfRequestedResources.add(null);
            }
            
            return AutochainingIterator.IteratorOffset.of(offset + batchOfRequestedResources.size(),
                                                          batchOfRequestedResources.iterator());
        };
        
        return new AutochainingIterator<>(nextIteratorFunction);
    }
    
    
    /**
     * Get a specific batch of requested resources, detailed by limit and offset
     *
     * @param limit               limit
     * @param offset              offset
     * @param libraryID           LibraryID
     * @param circulationDeskName guess
     * @param allOrNothing        If anything fails, do you want the already fetched results or an exception?
     * @param errorLoggers        loggers to use for logging errors. Only used if allOrNothing is false
     * @return an iterator of the requested resources
     */
    protected List<RequestedResource> getBatchOfRequestedResources(Integer limit,
                                                                   Integer offset,
                                                                   String libraryID,
                                                                   String circulationDeskName,
                                                                   String location,
                                                                   boolean allOrNothing,
                                                                   List<Logger> errorLoggers) throws AlmaKnownException {
        //if (Objects.isNull(offset)){
        //    offset = 0;
        //}
        //This does not use getLinkValue, as we do NOT want these things cached
        WebClient link = almaRestClient.constructLink()
                                       .path("task-lists")
                                       .path("requested-resources")
                                       .query("library", libraryID)
                                       .query("circ_desk", circulationDeskName)
                                       .query("location",withDefault(location,""))
                                       .query("order_by", "call_number")
                                       .query("direction", "asc")
                                       .query("pickup_inst")
                                       .query("reported")
                                       .query("printed")
                                       .query("limit", limit)
                                       .query("offset", offset);
        
        RequestedResources result;
        try {
            result = almaRestClient.get(link, RequestedResources.class, false);
        } catch (AlmaKnownException e) {
            
            if (allOrNothing) {
                throw e;
            } else {
                for (Logger logger : errorLoggers) {
                    //Known alma errors, we can be more intelligent here
                    logger.error("Failed to retrieve content [{}-{}] for '{}'/'{}' with error {}. Continuing on",
                              offset,
                              offset + limit,
                              libraryID,
                              circulationDeskName,
                              e.getMessage());
                }
                return Collections.emptyList();
            }
        } catch (Exception e) {
            //Something unknowable failed, we're fragged
            if (allOrNothing) {
                throw new RuntimeException(
                        "Failed to retrieve content [" + offset + "-" + (offset + limit) + "] for '" + libraryID + "'/'"
                        + circulationDeskName + "'", e);
            } else {
                for (Logger logger : errorLoggers) {
                    //Known alma errors, we can be more intelligent here
                    logger.error("Failed to retrieve content [{}-{}] for '{}'/'{}'. Continuing on",
                                 offset,
                                 offset + limit,
                                 libraryID,
                                 circulationDeskName,
                                 e);
                }
                return Collections.emptyList();
            }
        }
        log.debug("Completed fetching requests {}-{} for '{}'/'{}'",
                  offset,
                  offset + limit,
                  libraryID,
                  circulationDeskName);
        
        Integer total_records = result.getTotalRecordCount();
        if (offset >= total_records || result.getRequestedResources() == null) {
            return Collections.emptyList();
        } else {
            return result.getRequestedResources();
        }
    }
    
    /*Lending requests*/
    
    /**
     * Get Lending Requests. Optional parameters may be null.
     *
     * @param library         The resource sharing library from which lending requests should be retrieved. Mandatory.
     * @param status          The status of lending requests to retrieve. Optional.
     * @param printed         The 'printed' value of lending requests to retrieve. Optional. Possible values: Y, N.
     * @param reported        The 'reported' value of lending requests to retrieve. Optional. Possible values: Y, N.
     * @param partner         The partner value. Optional.
     * @param requestedFormat Requested format of the resource. Optional.
     * @param suppliedFormat  Supplied Format of the resource. Optional.
     */
    public UserResourceSharingRequests getLendingRequests(String library,
                                                          String status,
                                                          String printed,
                                                          String reported,
                                                          String partner,
                                                          String requestedFormat,
                                                          String suppliedFormat) {
        WebClient link = almaRestClient.constructLink().path("task-lists/rs/lending-requests");
        link = tryAddQueryParameter(link, "library", library);
        link = tryAddQueryParameter(link, "status", status);
        link = tryAddQueryParameter(link, "printed", printed);
        link = tryAddQueryParameter(link, "reported", reported);
        link = tryAddQueryParameter(link, "partner", partner);
        link = tryAddQueryParameter(link, "requested_format", requestedFormat);
        link = tryAddQueryParameter(link, "supplied_format", suppliedFormat);
        return almaRestClient.get(link, UserResourceSharingRequests.class);
    }
    
    /**
     * Currently the only supported action is 'mark_reported'. Optional parameters may be null.
     *
     * @param library         The resource sharing library from which lending requests should be retrieved. Mandatory.
     * @param status          The status of lending requests to retrieve. Optional.
     * @param printed         The 'printed' value of lending requests to retrieve. Optional. Possible values: Y, N.
     * @param reported        The 'reported' value of lending requests to retrieve. Optional. Possible values: Y, N.
     * @param partner         The partner value. Optional.
     * @param requestedFormat Requested format of the resource. Optional.
     * @param suppliedFormat  Supplied Format of the resource. Optional.
     */
    public UserResourceSharingRequests actOnLendingRequests(String library,
                                                            String status,
                                                            String printed,
                                                            String reported,
                                                            String partner,
                                                            String requestedFormat,
                                                            String suppliedFormat) {
        WebClient link = almaRestClient.constructLink().path("task-lists/rs/lending-requests");
        link = tryAddQueryParameter(link, "op", "mark_reported");
        link = tryAddQueryParameter(link, "library", library);
        link = tryAddQueryParameter(link, "status", status);
        link = tryAddQueryParameter(link, "printed", printed);
        link = tryAddQueryParameter(link, "reported", reported);
        link = tryAddQueryParameter(link, "partner", partner);
        link = tryAddQueryParameter(link, "requested_format", requestedFormat);
        link = tryAddQueryParameter(link, "supplied_format", suppliedFormat);
        return almaRestClient.post(link, UserResourceSharingRequests.class, "");
    }
    
    private WebClient tryAddQueryParameter(WebClient link, String parameterKey, String parameterValue) {
        if (parameterValue != null) {
            return link.query(parameterKey, parameterValue);
        } else {
            return link;
        }
    }
    
    /*Printouts*/
    
}
