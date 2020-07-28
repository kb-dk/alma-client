package dk.kb.alma.client.analytics;

import dk.kb.alma.client.utils.XPathSelector;
import dk.kb.alma.client.utils.XpathUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Report {
    
    private final String token;
    
    private final List<Map<String, String>> rows;
    
    public Report(String token, List<Map<String, String>> rows) {
        this.token = token;
        this.rows = rows;
    }
    
    public static Report parseFromAlmaReport(dk.kb.alma.gen.analytics.Report almaReport) {
        Element doc = almaReport.getAnies().get(0);
        
        final XPathSelector xPathSelector = XpathUtils.createXPathSelector("rowset",
                                                                           "urn:schemas-microsoft-com:xml-analysis:rowset",
                                                                           "saw-sql",
                                                                           "urn:saw-sql",
                                                                           "xsd",
                                                                           "http://www.w3.org/2001/XMLSchema");
        String token = xPathSelector.selectString(doc, "/QueryResult/ResumptionToken");
        
        Map<String, String> columns = new LinkedHashMap<>();
        List<Node> columnNodes =
                xPathSelector.selectNodeList(doc,
                                             "/QueryResult/ResultXml/rowset:rowset/xsd:schema/xsd:complexType/xsd:sequence/xsd:element");
        
        for (Node columnNode : columnNodes) {
            NamedNodeMap attributes = columnNode.getAttributes();
            String columnId = attributes.getNamedItem("name").getTextContent();
            String type = attributes.getNamedItem("type").getTextContent();
            String name = Optional.ofNullable(attributes.getNamedItemNS("urn:saw-sql", "columnHeading"))
                                  .map(columnHeading -> columnHeading.getTextContent())
                                  .orElse(columnId);
            columns.put(columnId, name);
        }
        
        
        List<Node> rowNodes =
                xPathSelector.selectNodeList(doc,
                                             "/QueryResult/ResultXml/rowset:rowset/rowset:Row");
        List<Map<String, String>> rows = new ArrayList<>(rowNodes.size());
        
        for (Node rowNode : rowNodes) {
            HashMap<String, String> row = new LinkedHashMap<>();
            for (Map.Entry<String, String> column : columns.entrySet()) {
                final String xpath = "rowset:" + column.getKey();
                final String value = xPathSelector.selectString(rowNode, xpath);
                row.put(column.getValue(), value);
            }
            rows.add(Collections.unmodifiableMap(row));
        }
        //TODO type
        return new Report(token, rows);
    }
    
    public String getToken() {
        return token;
    }
    
    public List<Map<String, String>> getRows() {
        return Collections.unmodifiableList(rows);
    }
    
    
    @Override
    public String toString() {
        return "Report{" +
               "token='" + token + '\'' +
               ", rows=" + rows +
               '}';
    }
}
