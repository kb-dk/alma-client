package dk.kb.alma.client.utils;

import dk.kb.alma.gen.users.Address;
import dk.kb.alma.gen.users.Addresses;
import dk.kb.alma.gen.users.ContactInfo;
import dk.kb.alma.gen.users.Email;
import dk.kb.alma.gen.items.Item;
import dk.kb.alma.gen.users.User;
import dk.kb.alma.gen.requested_resources.RequestedResource;
import dk.kb.util.other.StringListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AlmaObjectUtils {
    
    protected static final Logger log = LoggerFactory.getLogger(AlmaObjectUtils.class);
    
    public static String getCallNumber(Item item){
        String call_number = StringListUtils.useDefaultIfNullOrEmpty(
                item.getItemData().getAlternativeCallNumber(),
                item.getHoldingData().getCallNumber());
        return call_number;
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
    
    
}
