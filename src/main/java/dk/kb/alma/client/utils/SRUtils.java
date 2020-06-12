package dk.kb.alma.client.utils;

import dk.kb.alma.gen.sru.Record;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Optional;

//https://developers.exlibrisgroup.com/alma/apis/docs/bibs/R0VUIC9hbG1hd3MvdjEvYmlicy97bW1zX2lkfQ==/
public class SRUtils {
    
    public static Optional<String> extractBibnr(Element record) {
        List<String> ids = getMarcField(record, "035", "a");
        Optional<String> bibnr = ids.stream()
                                    .filter(id -> id.startsWith("(DK-820010)"))
                                    .map(id -> id.substring("(DK-820010)".length()))
                                    .findFirst();
        return bibnr;
    }
    
    public static Optional<String> extractMMSid(Element record) {
        
        List<String> ids = getMarcField(record, "035", "a");
        
        Optional<String> mmsID = ids.stream()
                                    .filter(id -> id.startsWith("(EXLNZ-45KBDK_NETWORK)"))
                                    .map(id -> id.substring("(EXLNZ-45KBDK_NETWORK)".length()))
                                    .findFirst();
        
        return mmsID;
    }
    
    public static List<String> getMarcField(Element recordXml, String datafield, String subfield) {
        XPathSelector xpath = XpathUtils.createXPathSelector("marc", "http://www.loc.gov/MARC21/slim");
        
        List<String> values = (xpath.selectStringList(recordXml,
                                                      "/marc:record/marc:datafield[@tag='" + datafield
                                                      + "']/marc:subfield[@code='" + subfield + "']/text()"));
        return values;
        
    }
}
