package dk.kb.alma.client;

import dk.kb.alma.client.exceptions.AlmaKnownException;
import dk.kb.alma.gen.requested_resources.RequestedResource;
import dk.kb.alma.gen.requested_resources.RequestedResources;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class AlmaTasksClient {
    
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    private final AlmaRestClient almaRestClient;
    private final Integer batchSize;

    public AlmaTasksClient(AlmaRestClient almaRestClient) {
        this(almaRestClient, 100);
    }

    public AlmaTasksClient(AlmaRestClient almaRestClient, Integer batchSize) {
        this.almaRestClient = almaRestClient;
        this.batchSize = batchSize;
    }
    
    public AlmaRestClient getAlmaRestClient() {
        return almaRestClient;
    }
    
    
    /*Requested Resources*/
    
    public Iterator<RequestedResource> getRequestedResourceIterator(String libraryId, String circulationDeskName,
                                                                    boolean allOrNothing) {
        Function<Integer, AutochainingIterator.IteratorOffset<Integer, Iterator<RequestedResource>>>
                nextIteratorFunction
                = offset -> {
            if (offset == null){
                offset = 0;
            }
            List<RequestedResource> batchOfRequestedResources = getBatchOfRequestedResources(batchSize,
                                                                                             offset,
                                                                                             libraryId,
                                                                                             circulationDeskName,
                                                                                             allOrNothing);
            log.info("Retrieved requests {} to {} for (libraryID={},circulationDesk={})",offset,offset+batchOfRequestedResources.size(),libraryId, circulationDeskName);
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
     * @return an iterator of the requested resources
     */
    protected List<RequestedResource> getBatchOfRequestedResources(Integer limit,
                                                                   Integer offset,
                                                                   String libraryID,
                                                                   String circulationDeskName,
                                                                   boolean allOrNothing) {
        //if (Objects.isNull(offset)){
        //    offset = 0;
        //}
        //This does not use getLinkValue, as we do NOT want these things cached
        WebClient link = almaRestClient.constructLink()
                                 .path("task-lists")
                                 .path("requested-resources")
                                 .query("library", libraryID)
                                 .query("circ_desk", circulationDeskName)
                                 .query("location")
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
            //Known alma errors, we can be more intelligent here
            log.error("Failed to retrieve content [{}-{}] for '{}'/'{}' with error {}. Continuing on",
                      offset,
                      offset + limit,
                      libraryID,
                      circulationDeskName,
                      e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            //Something unknowable failed, we're fragged
            if (allOrNothing) {
                throw new RuntimeException(
                        "Failed to retrieve content [" + offset + "-" + (offset + limit) + "] for '" + libraryID + "'/'"
                        + circulationDeskName + "'", e);
            } else {
                log.error(
                        "Failed to retrieve content [" + offset + "-" + (offset + limit) + "] for '" + libraryID + "'/'"
                        + circulationDeskName + "' but continuing on", e);
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
    
    /*Printouts*/
    
}
