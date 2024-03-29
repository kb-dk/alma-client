package dk.kb.alma.client;

import dk.kb.alma.client.sru.Query;
import dk.kb.alma.gen.sru.Explain;
import dk.kb.alma.gen.sru.ExplainResponse;
import dk.kb.alma.gen.sru.Record;
import dk.kb.alma.gen.sru.Records;
import dk.kb.alma.gen.sru.SearchRetrieveResponse;
import dk.kb.util.xml.XML;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import javax.annotation.Nonnull;
import javax.xml.bind.JAXBException;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Math.max;

/**
 * https://developers.exlibrisgroup.com/alma/integrations/SRU/
 */
public class AlmaSRUClient extends HttpClient {
    
    private Logger logger = LoggerFactory.getLogger(AlmaSRUClient.class);
    
    
    /**
     * The number of requests for each call for the Alma SRU service.
     */
    protected final int almaSruRequestCount;
    
    
    public AlmaSRUClient(String almaSruUrl,
                         int almaSruRequestCount,
                         long minSleep,
                         long sleepVariation,
                         int connectTimeout,
                         int readTimeout,
                         long cacheTimeMillis,
                         Integer maxRetries) {
        super(almaSruUrl,
              minSleep,
              sleepVariation,
              Map.of("version", "1.2"),
              connectTimeout,
              readTimeout,
              cacheTimeMillis,
              maxRetries);
        
        
        this.almaSruRequestCount = almaSruRequestCount;
    }
    
    
    /**
     * Search for records through the SRU interface.
     *
     * @param query    The search query.
     * @param startPos The starting position for the interval of search.
     * @throws RuntimeException if the search returned null
     */
    @Nonnull
    public SearchRetrieveResponse searchRetrieve(Query query, int startPos, int numHits) {
        
        numHits = Math.min(almaSruRequestCount, max(0, numHits));
        WebClient client = constructLink()
                //Mandatory arguments
                .query("operation", "searchRetrieve")
                .query("query", query.build())
                //Optionals
                .query("startRecord", startPos)
                .query("maximumRecords", numHits)
                .query("recordSchema", "marcxml");
        
        
        logger.debug("SRU Search with {}", client.getCurrentURI());
        SearchRetrieveResponse value = invokeDirect(client, SearchRetrieveResponse.class, null, Operation.GET);
        if (value == null) {
            throw new RuntimeException("Result for (query '"
                                       + query
                                       + "', startpos="
                                       + startPos
                                       + ", numHits="
                                       + numHits
                                       + ") is null");
        }
        return value;
        
    }
    
    /**
     * Search for records through the SRU interface.
     */
    public Explain explain() {
        
        
        WebClient client = constructLink()
                .query("operation", "explain");
        
        ExplainResponse value = invokeDirect(client, ExplainResponse.class, null, Operation.GET);
        if (value == null) {
            throw new RuntimeException("Result for explain is null");
        }
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
        
        SearchRetrieveResponse result = searchRetrieve(query, 0, 10);
    
        int numHits = Optional.ofNullable(result.getNumberOfRecords())
                              .map(BigInteger::intValueExact)
                              .orElseThrow(() -> new RuntimeException(
                                      "Failed to retrieve basic value 'numberOfRecords' from SRU result '\n"
                                      + toLogString(result)));
        
        if (result.getRecords() != null &&
            result.getRecords().getRecords() != null &&
            numHits == result.getRecords()
                             .getRecords()
                             .size()) { //If we got all the hits in the first go, just return them
            return result.getRecords().getRecords().stream()
                         .flatMap(this::getElementStream)
                         .collect(Collectors.toList())
                         .iterator();
        }
        //else, get all the batches in parallel
        List<Element> elements = IntStream
                .range(0, Math.min(numHits, 10_000))//The results are limited to the first 10k objects
                .filter(x -> x % almaSruRequestCount == 0)
                .parallel()
                .mapToObj(offset -> searchRetrieve(query,
                                                   offset,
                                                   almaSruRequestCount))
                .flatMap(searchResult -> {
                    //Todo this can be done better with Optionals...
                    final Records records = searchResult.getRecords();
                    if (records == null) {
                        return Stream.empty();
                    }
                    final List<Record> records2 = records.getRecords();
                    if (records2 == null) {
                        return Stream.empty();
                    }
                    return records2.stream();
                })
                .flatMap(this::getElementStream)
                .collect(Collectors.toList());
        return elements.iterator();
        
    }
    
    private String toLogString(SearchRetrieveResponse result)  {
        if (result == null){
            return "null";
        } else {
            try {
                return XML.marshall(result);
            } catch (JAXBException e) {
                log.error("Failed to remarshal result object as xml for log message. "
                          + "Not escalating this exception as it will block out whatever called this",e);
                return e.getMessage();
            }
        }
    }
    
    
    private Stream<Element> getElementStream(Record record) {
        return record.getRecordData()
                     .getContent()
                     .stream()
                     .filter(element -> element instanceof Element)
                     .map(element -> (Element) element);
    }
    
    @Override
    protected WebClient removeAuth(WebClient uri) {
        return uri;
    }
    
    @Override
    protected WebClient addAuth(WebClient uri) {
        return uri;
    }
}
