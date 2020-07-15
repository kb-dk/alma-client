package dk.kb.alma.client;

import dk.kb.alma.client.utils.SRUtils;
import dk.kb.alma.client.utils.StringListUtils;
import dk.kb.alma.client.utils.XML;
import dk.kb.alma.gen.sru.Explain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBException;
import javax.xml.transform.TransformerException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

class AlmaSRUClientTest {
    
    AlmaSRUClient client;
    @BeforeEach
    void setUp() {
        client = new AlmaSRUClient(URI.create(
                "https://kbdk-kgl-psb.alma.exlibrisgroup.com/view/sru/45KBDK_KGL"), null, 10);
    }
    
    //material_type=dvd and (current_Location=SMAG or current_Location=SMAGG or current_Location=SMAGK or current_Location=SMAGP)
    @Test
    void explain() throws JAXBException {
    
        Explain explain = client.explain();
    
        System.out.println(XML.toXmlString(explain));
        
    }
    
    @Test
    void testRetrieve() throws TransformerException {
    
        //
        //return result.getRecords().getRecords().stream()
        //             .filter(record -> SRUtils.getMarcField(record,"015","a").contains(call_number))
        //             .findFirst();
        Iterator<Element> result = client.searchOnPermanentCallNumber("121576");
        List<Element> resultList = StringListUtils.asStream(result).collect(Collectors.toList());
        Element first = resultList.get(0);
        
        String mmsID = SRUtils.extractMMSid(first).get();
        String xml = XML.toXmlString(first);
        System.out.println(xml);
    }
}