package dk.kb.alma.client.analytics;

import com.sun.istack.Nullable;
import dk.kb.util.xml.XPathSelector;
import dk.kb.util.xml.XpathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Report {
    private final static Logger log = LoggerFactory.getLogger(Report.class);
    
    private final String token;
    
    private final List<Map<String, String>> rows;
    private final Map<String, String> columns;
    
    private final boolean finished;
    
    public Report(String token,
                  List<Map<String, String>> rows,
                  Map<String, String> columns, boolean finished) {
        this.token    = token;
        this.rows     = rows;
        this.columns  = columns;
        this.finished = finished;
    }
    
    public static Report parseFromAlmaReport(@Nonnull Element doc,
                                             @Nullable Report previousReport) {
        
        
        final XPathSelector xPathSelector = XpathUtils.createXPathSelector(
                "rowset", "urn:schemas-microsoft-com:xml-analysis:rowset",
                "saw-sql", "urn:saw-sql",
                "xsd", "http://www.w3.org/2001/XMLSchema");
        Boolean isFinished = xPathSelector.selectBoolean(doc, "/QueryResult/IsFinished", null);
        
        
        String token;
        Map<String, String> columns;
        if (previousReport == null) {
            columns = new LinkedHashMap<>();
            List<Node> columnNodes =
                    xPathSelector.selectNodeList(doc,
                                                 "/QueryResult/ResultXml/rowset:rowset/xsd:schema/xsd:complexType/xsd:sequence/xsd:element");
            
            for (Node columnNode : columnNodes) {
                NamedNodeMap attributes = columnNode.getAttributes();
                String columnId = attributes.getNamedItem("name").getTextContent();
                String type = attributes.getNamedItem("type").getTextContent();
                String name = Optional.ofNullable(attributes.getNamedItemNS("urn:saw-sql", "columnHeading"))
                                      .map(columnHeading -> columnHeading.getTextContent())
                                      .orElse(columnId)
                                      .trim();
                columns.put(columnId, name);
            }
            
            token = xPathSelector.selectString(doc, "/QueryResult/ResumptionToken");
            log.info("Report token is '{}'", token);
            
        } else {
            columns = previousReport.getColumns();
            
            token = previousReport.getToken();
        }
        
        
        List<Node> rowNodes =
                xPathSelector.selectNodeList(doc,
                                             "/QueryResult/ResultXml/rowset:rowset/rowset:Row");
        List<Map<String, String>> rows = new ArrayList<>(rowNodes.size());
        
        for (Node rowNode : rowNodes) {
            
            //<Row xmlns="urn:schemas-microsoft-com:xml-analysis:rowset">
            //    <Column0>0</Column0>
            //    <Column1>400030280499</Column1>
            //    <Column2>2021-01-07</Column2>
            //    <Column3>213095</Column3>
            //
            //</Row>
            HashMap<String, String> row = new LinkedHashMap<>();
            
            List<Node> columnNodes = xPathSelector.selectNodeList(rowNode, "rowset:*");
            for (Node columnNode : columnNodes) {
                String name = columns.get(columnNode.getLocalName());
                String value = columnNode.getTextContent();
                row.put(name, value);
            }
         
            rows.add(Collections.unmodifiableMap(row));
        }
        return new Report(token, rows, columns, isFinished);
    }
    
    public String getToken() {
        return token;
    }
    
    public List<Map<String, String>> getRows() {
        return Collections.unmodifiableList(rows);
    }
    
    protected Map<String, String> getColumns() {
        return columns;
    }
    
    
    public boolean isFinished() {
        return finished;
    }
    
    @Override
    public String toString() {
        return "Report{" +
               "token='" + token + '\'' +
               ", rows=" + rows +
               ", columns=" + columns +
               ", finished=" + finished +
               '}';
    }
}
