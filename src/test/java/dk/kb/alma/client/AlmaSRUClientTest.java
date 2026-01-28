package dk.kb.alma.client;

import dk.kb.alma.client.sru.Query;
import dk.kb.alma.client.utils.SRUtils;
import dk.kb.alma.gen.sru.Explain;
import dk.kb.alma.gen.sru.SearchRetrieveResponse;
import dk.kb.util.other.StringListUtils;
import dk.kb.util.xml.XML;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.w3c.dom.Element;

import jakarta.xml.bind.JAXBException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlmaSRUClientTest {

    private WebClient mockClient;
    private AlmaSRUClient client;

    @BeforeEach
    void setUp() throws IOException {
        client = TestUtils.getAlmaSruClient();
        mockClient = mock(WebClient.class);
    }

    /**
     * Hjælpefunktion til at lave en spy med mockClient injiceret.
     */
    private AlmaSRUClient createSpyClientWithMock() {
        AlmaSRUClient spyClient = spy(client);
        doReturn(mockClient).when(spyClient).constructLink();
        when(mockClient.query(anyString(), any())).thenReturn(mockClient);
        when(mockClient.getCurrentURI()).thenReturn(URI.create("http://mock-sru"));
        return spyClient;
    }

    @Test
    void explainRunsWithoutErrors() throws JAXBException {
        Explain explain = client.explain();
        assertNotNull(explain);
        System.out.println(XML.marshall(explain));
    }

    @Test
    void searchReturnsResults() throws TransformerException {
        Iterator<Element> result = client.search(
                Query.containsWords(Query.PermanentCallNumber, "121576")
        );

        List<Element> resultList = StringListUtils.asStream(result).collect(Collectors.toList());
        assertFalse(resultList.isEmpty());

        Element first = resultList.get(0);
        assertTrue(SRUtils.extractMMSid(first).isPresent());

        System.out.println(XML.domToString(first));
    }

    @Test
    void searchRetrieveReturnsResponse() {
        AlmaSRUClient spyClient = createSpyClientWithMock();

        Query query = mock(Query.class);
        when(query.build()).thenReturn("title=Test");

        SearchRetrieveResponse expectedResponse = new SearchRetrieveResponse();
        doReturn(expectedResponse).when(spyClient).invokeDirect(
                eq(mockClient),
                eq(SearchRetrieveResponse.class),
                isNull(),
                eq(HttpClient.Operation.GET)
        );

        SearchRetrieveResponse result = spyClient.searchRetrieve(query, 1, 5, "marcxml");

        assertNotNull(result);
        assertEquals(expectedResponse, result);
    }

    @Test
    void searchRetrieveThrowsWhenNullResponse() {
        AlmaSRUClient spyClient = createSpyClientWithMock();

        Query query = mock(Query.class);
        when(query.build()).thenReturn("title=Fail");

        doReturn(null).when(spyClient).invokeDirect(
                eq(mockClient),
                eq(SearchRetrieveResponse.class),
                isNull(),
                eq(HttpClient.Operation.GET)
        );

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> spyClient.searchRetrieve(query, 1, 5, "dc"));

        assertTrue(ex.getMessage().contains("is null"));
    }

    @Test
    void searchRetrieveCapsNumHits() {
        AlmaSRUClient spyClient = createSpyClientWithMock();

        Query query = mock(Query.class);
        when(query.build()).thenReturn("title=CapTest");

        SearchRetrieveResponse expectedResponse = new SearchRetrieveResponse();
        doReturn(expectedResponse).when(spyClient).invokeDirect(
                eq(mockClient),
                eq(SearchRetrieveResponse.class),
                isNull(),
                eq(HttpClient.Operation.GET)
        );

        int tooHighHits = 9999;
        SearchRetrieveResponse result = spyClient.searchRetrieve(query, 1, tooHighHits, "mods");

        assertNotNull(result);
        verify(mockClient).query("maximumRecords", spyClient.almaSruRequestCount);
    }
}