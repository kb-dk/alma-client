package dk.kb.alma.client.analytics;

import dk.kb.util.xml.XML;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ReportTest {
    
    @Test
    void parseFromAlmaReport() throws ParserConfigurationException, IOException, SAXException {
    
        Element reportDoc = XML.fromXML(Thread.currentThread()
                                              .getContextClassLoader()
                                              .getResourceAsStream("sample_report_value.xml"), true)
                               .getDocumentElement();
    
        Report result = Report.parseFromAlmaReport(reportDoc, null);
        assertEquals("400030235159",result.getRows().get(10).get("Barcode"));
        
    }
}
