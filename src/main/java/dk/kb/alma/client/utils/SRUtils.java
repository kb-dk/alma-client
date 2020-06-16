package dk.kb.alma.client.utils;

import dk.kb.alma.gen.sru.Record;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Optional;

//https://developers.exlibrisgroup.com/alma/apis/docs/bibs/R0VUIC9hbG1hd3MvdjEvYmlicy97bW1zX2lkfQ==/
public class SRUtils {
    
    public static Optional<String> extractBibnr(Element record) {
        List<String> ids = getMarcDataField(record, "035", "a");
        Optional<String> bibnr = ids.stream()
                                    .filter(id -> id.startsWith("(DK-820010)"))
                                    .map(id -> id.substring("(DK-820010)".length()))
                                    .findFirst();
        return bibnr;
    }
    
    public static Optional<String> extractMMSid(Element record) {
        
        List<String> ids = getMarcControlField(record, "001");
        
        Optional<String> mmsID = ids.stream()
                                    .findFirst();
        
        return mmsID;
    }
    
    public static List<String> extractHoldingID(Element record) {
        List<String> ids = getMarcDataField(record, "AVA","8");
        return ids;
    }
    
    public static Optional<String> extractCallNumber(Element record, String holdingID) {
        List<String> ids = getMarcDataField(record, "AVA","d", "8", holdingID);
        Optional<String> result = ids.stream().findFirst();
        return result;
    }
    
    
    protected static List<String> getMarcDataField(Element recordXml, String datafield, String subfield) {
        XPathSelector xpath = XpathUtils.createXPathSelector("marc", "http://www.loc.gov/MARC21/slim");
        
        List<String> values = (xpath.selectStringList(recordXml,
                                                      "/marc:record/marc:datafield[@tag='" + datafield
                                                      + "']/marc:subfield[@code='" + subfield + "']/text()"));
        return values;
        
    }
    
    protected static List<String> getMarcDataField(Element recordXml, String datafield, String subfield, String testField, String testvalue) {
        XPathSelector xpath = XpathUtils.createXPathSelector("marc", "http://www.loc.gov/MARC21/slim");
        
        List<String> values = (xpath.selectStringList(recordXml,
                                                      "/marc:record/marc:datafield[@tag='" + datafield
                                                      + "'][marc:subfield[@code='"+testField+"']='"+testvalue+"']/marc:subfield[@code='" + subfield + "']/text()"));
        return values;
        
    }
    
    public static List<String> getMarcControlField(Element recordXml, String datafield) {
        XPathSelector xpath = XpathUtils.createXPathSelector("marc", "http://www.loc.gov/MARC21/slim");
        
        List<String> values = (xpath.selectStringList(recordXml,
                                                      "/marc:record/marc:controlfield[@tag='" + datafield
                                                      + "']/text()"));
        return values;
        
    }
}
