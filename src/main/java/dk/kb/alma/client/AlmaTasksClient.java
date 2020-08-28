package dk.kb.alma.client;

import dk.kb.alma.gen.requested_resources.RequestedResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class AlmaTasksClient {
    
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    private final AlmaRestClient almaRestClient;
    private final Integer batchSize;
    
    public AlmaTasksClient(AlmaRestClient almaRestClient, Integer batchSize) {
        this.almaRestClient = almaRestClient;
        this.batchSize = batchSize;
    }
    
    public AlmaRestClient getAlmaRestClient() {
        return almaRestClient;
    }
    
    public Iterator<RequestedResource> getRequestedResourceIterator(String libraryId, String circulationDeskName,
                                                                    boolean allOrNothing) {
        Function<Integer, AutochainingIterator.IteratorOffset<Integer, Iterator<RequestedResource>>>
                nextIteratorFunction
                = offset -> {
            if (offset == null){
                offset = 0;
            }
            List<RequestedResource> batchOfRequestedResources = almaRestClient.getBatchOfRequestedResources(batchSize,
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
    
    
}
