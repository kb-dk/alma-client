package dk.kb.alma.client;

import dk.kb.alma.client.utils.SRUtils;
import dk.kb.alma.gen.requested_resource.RequestedResource;
import dk.kb.alma.gen.sru.Explain;
import dk.kb.alma.gen.sru.ExplainResponse;
import dk.kb.alma.gen.sru.Record;
import dk.kb.alma.gen.sru.SearchRetrieveResponse;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * https://developers.exlibrisgroup.com/alma/integrations/SRU/
 */
public class AlmaSRUClient {
    
    private Logger logger = LoggerFactory.getLogger(AlmaSRUClient.class);
    
    /**
     * The URL for the Alma SRU service.
     */
    //https://kbdk-kgl.alma.exlibrisgroup.com/view/sru/45KBDK_KGL
    protected final URI almaSruUrl;
    /**
     * The default parameters for the Alma SRU service.
     */
    protected final String almaSruDefaultParams;
    /**
     * The number of requests for each call for the Alma SRU service.
     */
    protected final int almaSruRequestCount;
    
    public AlmaSRUClient(URI almaSruUrl, String almaSruDefaultParams, int almaSruRequestCount) {
        
        
        this.almaSruUrl = almaSruUrl;
        this.almaSruDefaultParams = almaSruDefaultParams;
        this.almaSruRequestCount = almaSruRequestCount;
    }
    
    
    public WebClient getWebClient(URI link) {
        URI host = UriBuilder.fromUri(link).replaceQuery(null).replacePath(null).replaceMatrix(null).build();
        
        
        WebClient client = WebClient.create(host);
        HTTPConduit conduit = WebClient.getConfig(client).getHttpConduit();
        
        
        if (link.getPath() != null) {
            client = client.path(link.getPath());
        }
        if (link.getQuery() != null) {
            client = client.replaceQuery(link.getQuery());
        }
        
        client = client.accept(MediaType.APPLICATION_XML_TYPE).type(MediaType.APPLICATION_XML_TYPE);
        
        return client;
    }
    
    /**
     * Search for records through the SRU interface.
     *
     * @param query    The search query.
     * @param startPos The starting position for the interval of search.
     */
    public SearchRetrieveResponse searchRetrieve(String query, int startPos) {
        
        WebClient client = getWebClient(almaSruUrl)
                                   //Mandatory arguments
                                   .query("version", "1.2")
                                   .query("operation", "searchRetrieve")
                                   .query("query", query)
                                   //Optionals
                                   .query("startRecord", startPos)
                                   .query("maximumRecords", almaSruRequestCount)
                                   .query("recordSchema", "marcxml");
        
        
        logger.debug("SRU Search with {}", client.getCurrentURI());
        SearchRetrieveResponse value = client.invoke("GET", null, SearchRetrieveResponse.class);
        return value;
        
    }
    
    /**
     * Search for records through the SRU interface.
     */
    public Explain explain() {
        
        
        WebClient client = getWebClient(almaSruUrl)
                                   .query("version", "1.2")
                                   .query("operation", "explain");
        
        
        ExplainResponse value = client.invoke("GET", null, ExplainResponse.class);
        Optional<Explain> explain = value.getRecord()
                                         .getRecordData()
                                         .getContent()
                                         .stream()
                                         .filter(element -> element instanceof Explain)
                                         .map(element -> (Explain) element)
                                         .findFirst();
        return explain.get();
    }
    
    public Iterator<Element> searchOnPermanentCallNumber(String call_number, int offset) {
        SearchRetrieveResponse result = searchRetrieve(
                "PermanentCallNumber all \"" + call_number.trim() + "\"",
                offset);
        if (Objects.isNull(result.getRecords()) || Objects.isNull(result.getRecords().getRecords())) {
            return Collections.emptyIterator();
        } else {
            if (offset > result.getNumberOfRecords().intValueExact()) {
                return Collections.emptyIterator();
            }
            return result.getRecords().getRecords().stream()
                         .flatMap(record -> record.getRecordData().getContent()
                                                  .stream()
                                                  .filter(element -> element instanceof Element)
                                                  .map(element -> (Element) element))
                         .collect(Collectors.toList()).iterator();
        }
    }
    
    public Iterator<Element> searchOnPermanentCallNumber(String call_number) {
        Function<Integer, Iterator<Element>> nextIteratorFunction =
                offset -> searchOnPermanentCallNumber(call_number, offset+1);
        
        return new AutochainingIterator<>(nextIteratorFunction);
    }
}
