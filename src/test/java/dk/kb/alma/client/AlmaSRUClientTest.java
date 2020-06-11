package dk.kb.alma.client;

import dk.kb.alma.client.utils.SRUtils;
import dk.kb.alma.client.utils.XML;
import dk.kb.alma.gen.sru.Explain;
import dk.kb.alma.gen.sru.ExplainResponse;
import dk.kb.alma.gen.sru.Record;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AlmaSRUClientTest {
    
    AlmaSRUClient client;
    @BeforeEach
    void setUp() {
        client = new AlmaSRUClient(URI.create(
                "https://kbdk-kgl.alma.exlibrisgroup.com/view/sru/45KBDK_KGL"), null, 10);
    }
    
    //material_type=dvd and (current_Location=SMAG or current_Location=SMAGG or current_Location=SMAGK or current_Location=SMAGP)
    @Test
    void explain() throws JAXBException {
    
        Explain explain = client.explain();
    
        System.out.println(XML.toXml(explain));
        
    }
    
    @Test
    void testRetrieve() throws JAXBException {
    
        //
        //return result.getRecords().getRecords().stream()
        //             .filter(record -> SRUtils.getMarcField(record,"015","a").contains(call_number))
        //             .findFirst();
        List<Record> result = client.retrieveFromPermanentCallNumber("121576");
        String mmsID = SRUtils.extractMMSid(result.get(0)).get();
        String xml = XML.toXml(result.get(0));
        System.out.println(xml);
    }
}