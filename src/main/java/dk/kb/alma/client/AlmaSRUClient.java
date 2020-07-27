package dk.kb.alma.client;

import dk.kb.alma.client.sru.Query;
import dk.kb.alma.client.sru.Restriction;
import dk.kb.alma.gen.sru.Explain;
import dk.kb.alma.gen.sru.ExplainResponse;
import dk.kb.alma.gen.sru.Record;
import dk.kb.alma.gen.sru.Records;
import dk.kb.alma.gen.sru.SearchRetrieveResponse;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Math.max;

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
    public SearchRetrieveResponse searchRetrieve(Query query, int startPos, int numHits) {
        
        numHits = Math.min(almaSruRequestCount,max(0,numHits));
        WebClient client = getWebClient(almaSruUrl)
                                   //Mandatory arguments
                                   .query("version", "1.2")
                                   .query("operation", "searchRetrieve")
                                   .query("query", query.build())
                                   //Optionals
                                   .query("startRecord", startPos)
                                   .query("maximumRecords", numHits)
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
    
    public Iterator<Element> search(Query query) {
        
        SearchRetrieveResponse result = searchRetrieve(query,0,10);
        
        int numHits = result.getNumberOfRecords().intValueExact();
        if (result.getRecords() != null &&
            result.getRecords().getRecords() != null &&
            numHits == result.getRecords().getRecords().size()){ //If we got all the hits in the first go, just return them
            return result.getRecords().getRecords().stream()
                         .flatMap(record -> getElementStream(record))
                         .collect(Collectors.toList())
                         .iterator();
        }
        //else, get all the batches in parallel
        List<Element> elements = IntStream.range(0, Math.min(numHits,10000))//The results are limited to the first 10k objects
                                                  .filter(x -> x % almaSruRequestCount == 0)
                                                  .parallel()
                                                  .mapToObj(offset -> searchRetrieve(query,
                                                                                     offset,
                                                                                     almaSruRequestCount))
                                                  .flatMap(searchResult -> {
                                                      final Records records = searchResult.getRecords();
                                                      if (records == null){
                                                          return Stream.empty();
                                                      }
                                                      final List<Record> records2 = records.getRecords();
                                                      if (records2 == null){
                                                          return Stream.empty();
                                                      }
                                                      return records2.stream();
                                                  })
                                                  .flatMap((Record record) -> getElementStream(record))
                                                  .collect(Collectors.toList());
        return elements.iterator();
        
    }
    
    
    private Stream<Element> getElementStream(Record record) {
        return record.getRecordData()
                                      .getContent()
                                      .stream()
                                      .filter(element -> element instanceof Element)
                                      .map(element -> (Element) element);
    }
    
}
