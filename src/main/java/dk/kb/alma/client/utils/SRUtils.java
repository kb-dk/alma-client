package dk.kb.alma.client.utils;

import dk.kb.alma.gen.sru.Record;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Optional;

//https://developers.exlibrisgroup.com/alma/apis/docs/bibs/R0VUIC9hbG1hd3MvdjEvYmlicy97bW1zX2lkfQ==/
public class SRUtils {
    
    public static Optional<String> extractBibnr(Record record) {
        List<String> ids = getMarcField(record, "035", "a");
        Optional<String> bibnr = ids.stream()
                                    .filter(id -> id.startsWith("(DK-820010)"))
                                    .map(id -> id.substring("(DK-820010)".length()))
                                    .findFirst();
        return bibnr;
    }
    
    public static Optional<String> extractMMSid(Record record) {
        
        List<String> ids = getMarcField(record, "035", "a");
        
        Optional<String> mmsID = ids.stream()
                                    .filter(id -> id.startsWith("(EXLNZ-45KBDK_NETWORK)"))
                                    .map(id -> id.substring("(EXLNZ-45KBDK_NETWORK)".length()))
                                    .findFirst();
        
        return mmsID;
    }
    
    public static List<String> getMarcField(Record record, String datafield, String subfield) {
        Element recordXml = record.getRecordData().getContent().stream()
                                  .filter(element -> element instanceof Element)
                                  .map(element -> (Element) element)
                                  .findFirst().get();
        
        XPathSelector xpath = XpathUtils.createXPathSelector("marc", "http://www.loc.gov/MARC21/slim");
        
        List<String> values = (xpath.selectStringList(recordXml,
                                                      "/marc:record/marc:datafield[@tag='" + datafield
                                                      + "']/marc:subfield[@code='" + subfield + "']/text()"));
        return values;
        
    }
}
