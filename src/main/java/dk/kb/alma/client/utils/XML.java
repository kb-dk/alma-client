package dk.kb.alma.client.utils;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class XML {
    public static String toXml(Object object) throws JAXBException {
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
}
