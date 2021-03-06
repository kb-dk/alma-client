package dk.kb.alma.client;

import dk.kb.alma.client.sru.Query;
import dk.kb.alma.client.utils.SRUtils;
import dk.kb.alma.gen.sru.Explain;
import dk.kb.util.other.StringListUtils;
import dk.kb.util.xml.XML;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

class AlmaSRUClientTest {
    
    private static AlmaSRUClient client;
    
    @BeforeAll
    public static void setUp() throws IOException {
        client = TestUtils.getAlmaSruClient();
    }
    
    //material_type=dvd and (current_Location=SMAG or current_Location=SMAGG or current_Location=SMAGK or current_Location=SMAGP)
    @Test
    void explain() throws JAXBException {
    
        Explain explain = client.explain();
    
        System.out.println(XML.marshall(explain));
        
    }
    
    @Test
    void testRetrieve() throws TransformerException {
    
        //
        //return result.getRecords().getRecords().stream()
        //             .filter(record -> SRUtils.getMarcField(record,"015","a").contains(call_number))
        //             .findFirst();
    
        //Function<Integer, Iterator<Element>> nextIteratorFunction =
        //        offset -> searchOnPermanentCallNumber(call_number, offset+1);
        //
        //return new AutochainingIterator<>(nextIteratorFunction);
        Iterator<Element> result = client.search(Query.containsWords(Query.PermanentCallNumber, "121576"));
        List<Element> resultList = StringListUtils.asStream(result).collect(Collectors.toList());
        Element first = resultList.get(0);
        
        String mmsID = SRUtils.extractMMSid(first).get();
        String xml = XML.domToString(first);
        System.out.println(xml);
    }
}