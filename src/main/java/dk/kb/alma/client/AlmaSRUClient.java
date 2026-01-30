package dk.kb.alma.client;

import dk.kb.alma.client.sru.Query;
import dk.kb.alma.gen.sru.*;
import dk.kb.alma.gen.sru.Record;
import dk.kb.util.xml.XML;
import jakarta.annotation.Nonnull;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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

        int cappedHits = capNumHits(numHits);
        WebClient client = constructLink()
                //Mandatory arguments
                .query("operation", "searchRetrieve")
                .query("query", query.build())
                //Optionals
                .query("startRecord", startPos)
                .query("maximumRecords", cappedHits)
                .query("recordSchema", "marcxml");


        logger.debug("SRU Search with {}", client.getCurrentURI());
        SearchRetrieveResponse value = invokeDirect(client, SearchRetrieveResponse.class, null, Operation.GET);
        if (value == null) {
            throw new RuntimeException("Result for (query '"
                    + query
                    + "', startpos="
                    + startPos
                    + ", numHits="
                    + cappedHits
                    + ") is null");
        }
        return value;

    }

    /**
     * Executes a search and retrieve operation using the SRU (Search/Retrieve via URL) protocol.
     *
     * @param query        The query object containing the search criteria.
     * @param startPos     The starting position of the records to retrieve.
     * @param numHits      The maximum number of records to retrieve. This value will be capped at the
     *                     maximum allowed by the SRU request.
     * @param recordSchema The schema of the records to be returned, which defines the format of the
     *                     retrieved records. Currently, the supported values are:
     *                                        <ul>
     *                                            <li>marcxml</li>
     *                                            <li>dc</li>
     *                                            <li>dcx</li>
     *                                            <li>mods</li>
     *                                            <li>unimarcxml</li>
     *                                            <li>kormarcxml</li>
     *                                            <li>cnmarcxml</li>
     *                                            <li>isohold (ISO 20775 standard)</li>
     *                                            <li>lc_bf_instance</li>
     *                                        </ul>
     *                     @return A {@link SearchRetrieveResponse} object containing the results of the search operation.
     * @throws RuntimeException if the search operation returns null.
     */
    @Nonnull
    public SearchRetrieveResponse searchRetrieve(Query query, int startPos, int numHits, String recordSchema) {
        int cappedHits = capNumHits(numHits);

        WebClient client = buildSearchRetrieveClient(query, startPos, cappedHits, recordSchema);

        logger.debug("SRU Search with {}", client.getCurrentURI());

        return executeRequest(client, query, startPos, cappedHits);
    }

    private int capNumHits(int numHits) {
        return Math.min(almaSruRequestCount, Math.max(0, numHits));
    }

    private WebClient buildSearchRetrieveClient(Query query, int startPos, int numHits, String recordSchema) {
        return constructLink()
                .query("operation", "searchRetrieve")
                .query("query", query.build())
                .query("startRecord", startPos)
                .query("maximumRecords", numHits)
                .query("recordSchema", recordSchema);
    }

    private SearchRetrieveResponse executeRequest(WebClient client, Query query, int startPos, int numHits) {
        SearchRetrieveResponse response = invokeDirect(client, SearchRetrieveResponse.class, null, Operation.GET);

        if (response == null) {
            throw new RuntimeException("Result for (query '"
                    + query
                    + "', startpos=" + startPos
                    + ", numHits=" + numHits
                    + ") is null");
        }
        return response;
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

    private String toLogString(SearchRetrieveResponse result) {
        if (result == null) {
            return "null";
        } else {
            return XML.marshall(result);
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
