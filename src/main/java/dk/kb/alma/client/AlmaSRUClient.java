package dk.kb.alma.client;

import dk.kb.alma.client.utils.SRUtils;
import dk.kb.alma.gen.sru.Explain;
import dk.kb.alma.gen.sru.ExplainResponse;
import dk.kb.alma.gen.sru.Record;
import dk.kb.alma.gen.sru.SearchRetrieveResponse;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * https://developers.exlibrisgroup.com/alma/integrations/SRU/
 */
public class AlmaSRUClient {
    
    
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
                                   .query("startRecord", startPos)
                                   .query("maximumRecords", almaSruRequestCount)
                                   .query("version", "1.2")
                                   .query("operation", "searchRetrieve")
                                   .query("query",
                                          query);
        System.out.println(client.getCurrentURI());
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
    
    public List<Record> retrieveFromPermanentCallNumber(String call_number){
        SearchRetrieveResponse result = searchRetrieve("PermanentCallNumber==" + call_number, 0);
        if (Objects.isNull(result.getRecords()) || Objects.isNull(result.getRecords().getRecords())){
            return Collections.emptyList();
        } else {
            return result.getRecords().getRecords();
        }
    }
}
