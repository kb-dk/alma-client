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
    private final boolean finished;
    
    private final List<Map<String, String>> rows;
    
    private final Map<String, String> columns;
    
    
    public Report(String token,
                  boolean finished,
                  List<Map<String, String>> rows,
                  Map<String, String> columns) {
        this.token = token;
        this.finished = finished;
        this.rows = rows;
        this.columns = columns;
    }
    
    public static Report parseFromAlmaReport(dk.kb.alma.gen.analytics.Report almaReport, Report old_report) {
        Element doc = almaReport.getAnies().get(0);
        
        final XPathSelector xPathSelector = XpathUtils.createXPathSelector("rowset",
                                                                           "urn:schemas-microsoft-com:xml-analysis:rowset",
                                                                           "saw-sql",
                                                                           "urn:saw-sql",
                                                                           "xsd",
                                                                           "http://www.w3.org/2001/XMLSchema");
        String token = xPathSelector.selectString(doc, "/QueryResult/ResumptionToken");
        if (token == null || token.isBlank()){
            token = old_report.getToken();
        }
        boolean isFinished = Boolean.parseBoolean(xPathSelector.selectString(doc, "/QueryResult/IsFinished"));
        
        List<Node> columnNodes =
                xPathSelector.selectNodeList(doc,
                                             "/QueryResult/ResultXml/rowset:rowset/xsd:schema/xsd:complexType/xsd:sequence/xsd:element");
    
        Map<String, String> columns;
        if (columnNodes.isEmpty()){
            columns = old_report.getColumns();
        } else {
            columns = new LinkedHashMap<>();
            for (Node columnNode : columnNodes) {
                NamedNodeMap attributes = columnNode.getAttributes();
                String columnId = attributes.getNamedItem("name").getTextContent();
                String type = attributes.getNamedItem("type").getTextContent();
                String name = Optional.ofNullable(attributes.getNamedItemNS("urn:saw-sql", "columnHeading"))
                                      .map(columnHeading -> columnHeading.getTextContent())
                                      .orElse(columnId).trim();
                columns.put(columnId, name);
            }
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
        return new Report(token, isFinished, rows, columns);
    }
    
    public String getToken() {
        return token;
    }
    
    public List<Map<String, String>> getRows() {
        return Collections.unmodifiableList(rows);
    }
    
    public boolean isFinished() {
        return finished;
    }
    
    
    public Map<String, String> getColumns() {
        return columns;
    }
    
    @Override
    public String toString() {
        return "Report{" +
               "token='" + token + '\'' +
               ", finished=" + finished +
               ", rows=" + rows +
               '}';
    }
}
