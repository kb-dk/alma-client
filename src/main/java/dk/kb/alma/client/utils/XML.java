package dk.kb.alma.client.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class XML {
    
    public static String toXmlString(Node dom) throws TransformerException {
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        t.setOutputProperty(OutputKeys.METHOD, "xml");
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        
        /* Transformer */
        try (StringWriter sw = new StringWriter();) {
            t.transform(new DOMSource(dom), new StreamResult(sw));
            return sw.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    
    public static <T> String toXmlString(T object) throws JAXBException {
        //TODO does this work?
        JAXBContext jc = JAXBContext.newInstance(object.getClass());
        
        
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            marshaller.marshal(object, out);
            out.flush();
            return out.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T fromXml(String object, Class<T> type) {
        try {
            JAXBContext jc = JAXBContext.newInstance(type.getPackageName(), type.getClassLoader());
            
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            
            try (ByteArrayInputStream in = new ByteArrayInputStream(object.getBytes())) {
                return (T) unmarshaller.unmarshal(in);
            }
        } catch (JAXBException | IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static List<Node> nodeList(NodeList list) {
        List<Node> result = new ArrayList<>();
        for (int i = 0; i < list.getLength(); i++) {
            result.add(list.item(i));
        }
        return result;
    }
    
    /**
     * Parses an XML document from a String to a DOM.
     *
     * @param xmlString      a String containing an XML document.
     * @param namespaceAware if {@code true} the parsed DOM will reflect any
     *                       XML namespaces declared in the document
     * @return The document in a DOM or {@code null} on errors.
     */
    public static Document fromXML(String xmlString,
                                   boolean namespaceAware)
            throws ParserConfigurationException, IOException, SAXException {
        
        InputSource in = new InputSource();
        in.setCharacterStream(new StringReader(xmlString));
        
        DocumentBuilderFactory dbFact = DocumentBuilderFactory.newInstance();
        dbFact.setNamespaceAware(namespaceAware);
        
        return dbFact.newDocumentBuilder().parse(in);
        
    }
    
    /**
     * Parses a XML document from a stream to a DOM or return
     * {@code null} on error.
     *
     * @param xmlStream      a stream containing an XML document.
     * @param namespaceAware if {@code true} the constructed DOM will reflect
     *                       the namespaces declared in the XML document
     * @return The document in a DOM or {@code null} in case of errors
     */
    public static Document fromXML(InputStream xmlStream,
                                   boolean namespaceAware)
            throws ParserConfigurationException, IOException, SAXException {
        
        DocumentBuilderFactory dbFact = DocumentBuilderFactory.newInstance();
        dbFact.setNamespaceAware(namespaceAware);
        
        return dbFact.newDocumentBuilder().parse(xmlStream);
        
    }
}
