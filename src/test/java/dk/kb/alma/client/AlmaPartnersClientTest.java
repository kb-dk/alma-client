package dk.kb.alma.client;

import dk.kb.alma.gen.partners.Partner;
import dk.kb.alma.gen.partners.Partners;
import dk.kb.alma.gen.partners.Status;
import dk.kb.util.json.JSON;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


class AlmaPartnersClientTest {
    
    private static AlmaRestClient client;
    
    @BeforeAll
    static void setUp() throws IOException {
        client = TestUtils.getAlmaClient();
    }
    
    @Test
    void testPartners() {
        Partners partners = new AlmaPartnersClient(client).getPartners(0, 1, Status.ACTIVE);
        
        Partner foundPartner = partners.getPartners()
                                       .stream()
                                       .findFirst()
                                       .orElseThrow(() -> new AssertionError("No partner found"));
        
        assertThat(foundPartner.getPartnerDetails().getName(), is("Emerson College Library"));
        assertThat(foundPartner.getPartnerDetails().getCode(), is("074281"));
        assertThat(foundPartner.getPartnerDetails().getProfileDetails().getProfileType().value(), is("EMAIL"));
        
        partners.getPartners().stream().map(partner -> JSON.toJson(partner, true)).forEach(System.out::println);
    }
    
    @Test
    void testPartner() {
        Partner partner = new AlmaPartnersClient(client).getPartner("775100");
        
        System.out.println(JSON.toJson(partner, true));
        
        assertThat(partner.getPartnerDetails().getName(), is("Aarhus Kommunes Biblioteker, Hovedbiblioteket"));
        assertThat(partner.getPartnerDetails().getCode(), is("775100"));
        assertThat(partner.getPartnerDetails().getProfileDetails().getProfileType().value(), is("ISO_18626"));
    }
    
    
}
