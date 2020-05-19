package dk.kb.alma;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dk.kb.alma.gen.Address;
import dk.kb.alma.gen.Addresses;
import dk.kb.alma.gen.ContactInfo;
import dk.kb.alma.gen.Email;
import dk.kb.alma.gen.RequestedResource;
import dk.kb.alma.gen.User;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Utilities {
    
    protected static final Logger log = LoggerFactory.getLogger(Utilities.class);
    
    public static int MAX_LINE_LENGTH = 65;
    
    /**
     * Turn an iterator into a stream
     *
     * @param iterator the iterator
     * @param <T>      the the type of elements
     * @return A stream of the iterator elements in the same order
     */
    public static <T> Stream<T> asStream(Iterator<T> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false);
    }
    
    
    /**
     * Return the string or "" if null
     *
     * @param string the string
     * @return same or "" if null
     */
    public static String notNull(String string) {
        if (string == null) {
            return "";
        }
        return string;

    }

    /**
     * Normalise spaces and wordwrap the given string with a line-length of MAX_LINE_LENGTH
     *
     * @param string the string to wordwrap
     * @return "" if string is null, otherwise the wrapped string
     * @see #MAX_LINE_LENGTH
     */
    public static List<String> wordwrap(String string) {
        return wordwrap(string, MAX_LINE_LENGTH, "\\s+", " ");
    }
    
    
    /**
     * Normalise spaces and wordwrap the given string with a line-length of MAX_LINE_LENGTH
     *
     * @param string the string to wordwrap
     * @return "" if string is null, otherwise the wrapped string
     * @see #MAX_LINE_LENGTH
     */
    public static List<String> wordwrap(String string, int lineLength, String splitPattern, String replacement) {
        
        
        if (string == null) {
            return Collections.emptyList();
        }
        
        String[] splits = string.split(splitPattern);
        int currentLineLength = 0;
        
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String split : splits) {
            if (currentLineLength == 0) {
                //Empty, but we want the very first split to go directly into the builder
            } else if (currentLineLength + replacement.length() + split.length() > lineLength) {
                //if we would break the length limit, start a new line.
                //Indent this line with 4 spaces
                //And update the currentLineLength
                current.append(replacement.trim());
                result.add(current.toString());
                current = new StringBuilder();
                String indent = "    ";
                current.append(indent);
                currentLineLength = indent.length();
            } else {
                //Otherwise just append the replacement for the split
                current.append(replacement);
                currentLineLength += replacement.length();
            }
            //And then, no matter what happened above we append the split and update the length.
            current.append(split);
            currentLineLength = currentLineLength + split.length();
        }
        if (!current.toString().trim().isEmpty()) {
            result.add(current.toString());
        }
        
        return result;
        
    }
    
    /**
     * If the string contains /, return the part BEFORE the last /. Otherwise return the entire string
     *
     * @param string the string
     * @return see above
     */
    public static String firstPart(String string) {
        string = notNull(string);

        if (string.contains("/")) {
            string = string.substring(0, string.indexOf("/"));
        }
        return string;
    }
    
    public static Path getPathFromClasspath(String s) {
        try {
            return new File(Thread.currentThread().getContextClassLoader().getResource(s).toURI()).toPath();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static String readFileFromClasspath(String name) throws IOException {
        try (InputStream resourceAsStream = Thread.currentThread()
                                                  .getContextClassLoader()
                                                  .getResourceAsStream(name);) {
            if (resourceAsStream == null) {
                log.warn("Failed to find file {}, returning null", name);
                return null;
            } else {
                return IOUtils.toString(resourceAsStream, Charset.defaultCharset());
            }
        }
    }


    public static String extractUserPrimaryID(RequestedResource RestRequestedResource, String userLink) {
        // User link will look like
        // https://api-eu.hosted.exlibrisgroup.com/almaws/v1/users/4099176418
        
        String userID = RestRequestedResource.getRequests().getRequests().get(0).getRequester().getDesc();
        
        if (userLink != null) {
            // Extract the user primary id from the url. Hackish
            try {
                userID = new File(new URL(userLink).getPath()).getName();
            } catch (MalformedURLException e) {
                log.warn("Failed to parse userLink '{}' as URL", userLink, e);
            }
        }
        return userID;
    }
    
    
    public static Email getBestEmailForUser(User user) {
        List<Email> emails = user.getContactInfo()
                                 .getEmails()
                                 .getEmails();
        Optional<Email> firstEmail = emails.stream().findFirst();
        Optional<Email> preferredEmail = emails
                                                 .stream()
                                                 .filter(email -> email.isPreferred())
                                                 .findFirst();
        return preferredEmail.orElse(firstEmail.orElse(null));
    }
    
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
    
    public static String toJson(Object object) {
        return toJson(object, true);
    }
    
    public static <T> String toJson(T object, boolean indent) {
        
        if (object == null) {
            return "";
        }
        JSON json = new JSON();
        ObjectMapper mapper = json.getContext(object.getClass());
        
        
        if (indent) {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
        } else {
            mapper.disable(SerializationFeature.INDENT_OUTPUT);
        }
        
        
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static LocalDate toLocalDate(XMLGregorianCalendar xmlGregorianCalendar) {
        return xmlGregorianCalendar.toGregorianCalendar().toZonedDateTime().toLocalDate();
    }
    
    public static LocalDateTime toLocalDateTime(XMLGregorianCalendar xmlGregorianCalendar) {
        return xmlGregorianCalendar.toGregorianCalendar().toZonedDateTime().toLocalDateTime();
    }
    
    
    public static List<String> getWorkAddress(User user) {
        return getAddress(user, "work");
    }
    
    public static List<String> getHomeAddress(User user) {
        return getAddress(user, "home");
    }
    
    public static List<String> getAddress(User user, String addressType) {
        if (user == null) {
            return Collections.emptyList();
        }
        ContactInfo contactInfo = user.getContactInfo();
        if (contactInfo == null) {
            log.error("User '{}' has no contact info", user);
            return Arrays.asList(user.getFullName(),
                                 "User has no contact info");
        }
        Addresses addresses = contactInfo.getAddresses();
        if (addresses == null) {
            log.error("User '{}' has no addresses", user);
            return Arrays.asList(user.getFullName(),
                                 "User has no addresses");
            
        }
        
        
        LocalDate today = LocalDate.now();
        Optional<Address> addressOptional =
                addresses.getAddresses().stream()
                
                         //Only work addresses
                         .filter(address ->
                                         address.getAddressTypes().getAddressTypes()
                                                .stream()
                                                .anyMatch(type -> addressType.equalsIgnoreCase(type.getValue())))
                
                         //Only if start date not set or after today
                         .filter(address -> address.getStartDate() == null
                                            || today.isAfter(toLocalDate(address.getStartDate())))
                
                         //Only if end date not set or before today
                         .filter(address -> address.getEndDate() == null
                                            || today.isBefore(toLocalDate(address.getEndDate())))
                
                         //Find the first that matches the above filters
                         .findFirst();
        
        
        if (!addressOptional.isPresent()) {
            log.error("User '{}' has no valid {} addresse", user, addressType);
            return Arrays.asList(user.getFullName(),
                                 "User has no valid " + addressType + " address");
            
        }
        Address addressO = addressOptional.get();
        
        return Stream.of(user.getFullName(),
                         addressO.getLine1(),
                         addressO.getLine2(),
                         addressO.getLine3(),
                         addressO.getLine4(),
                         addressO.getLine5(),
                         addressO.getPostalCode() + " " + addressO.getCity(),
                         addressO.getStateProvince(),
                         addressO.getCountry().getDesc()
        )
                     .filter(Objects::nonNull)
                     .map(line -> line.trim())
                     .filter(line -> !line.isEmpty())
                     .filter(line -> !"Denmark".equalsIgnoreCase(line)) //remove country if denmark
                     .collect(Collectors.toList());
        
    }
    
    
    private static List<Node> nodeList(NodeList list) {
        List<Node> result = new ArrayList<>();
        for (int i = 0; i < list.getLength(); i++) {
            result.add(list.item(i));
        }
        return result;
    }
    
    private static String firstOf(List<String> list, String defaultValue) {
        if (list.isEmpty()) {
            return defaultValue;
        } else {
            return list.get(0);
        }
    }
    
    
    public static String getParamOrProps(Properties props, String param, String propKey) {
        if (param == null) {
            
            if (!props.contains(propKey)) {
                param = props.getProperty(propKey);
            } else {
                param = null;
            }
        }
        return param;
    }
    
    
    private static List<String> removeEmpties(List<String> list) {
        return list.stream()
                   .parallel()
                   .filter(Objects::nonNull)
                   .filter(number -> !number.trim().isEmpty())
                   .distinct()
                   .collect(Collectors.toList());
    }
    
    
    public static <T extends Iterable<String>> T removeSubstrings(T list) {
        
        Iterator<String> firstIterator = list.iterator();
        while (firstIterator.hasNext()) {
            String s = firstIterator.next();
            for (String t : list) {
                if (s.equals(t)) {
                    continue;
                }
                if (t.contains(s)) {
                    firstIterator.remove();
                    break;
                }
            }
        }
        
        return list;
    }
    
    @SafeVarargs
    public static <T> Set<T> setOf(T... objects) {
        return new HashSet<T>(Arrays.asList(objects));
    }
    
    //
    //public static Set<String> removeSubstrings(Set<String> list) {
    //
    //    Set<String> result = new HashSet<>();
    //    for (String s : list) {
    //        boolean substring = false;
    //        for (String t : list) {
    //            if (s.equals(t)){
    //                continue;
    //            }
    //            if (t.contains(s)){
    //                substring = true;
    //                break;
    //            }
    //        }
    //        if (!substring){
    //            result.add(s);
    //        }
    //    }
    //    return result;
    //}
    private static List<String> removeEmpties(String... list) {
        return Arrays.stream(list)
                     .filter(Objects::nonNull)
                     .filter(number -> !number.trim().isEmpty())
                     .distinct()
                     .collect(Collectors.toList());
    }
    
    public static String orDefault(String value, String default_value) {
        if (value == null) {
            return default_value;
        } else {
            return value;
        }
    }
}
