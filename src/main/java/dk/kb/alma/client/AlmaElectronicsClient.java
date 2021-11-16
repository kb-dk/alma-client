package dk.kb.alma.client;

import dk.kb.alma.client.exceptions.AlmaConnectionException;
import dk.kb.alma.client.exceptions.AlmaKnownException;
import dk.kb.alma.client.exceptions.AlmaUnknownException;
import dk.kb.alma.client.utils.Utils;
import dk.kb.alma.gen.item.electronic.ElectronicCollection;
import dk.kb.alma.gen.item.electronic.ElectronicCollections;
import dk.kb.alma.gen.item.electronic.ElectronicService;
import dk.kb.alma.gen.item.electronic.ElectronicServices;
import dk.kb.alma.gen.portfolios.Portfolio;
import dk.kb.alma.gen.portfolios.Portfolios;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

public class AlmaElectronicsClient {
    
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    private final AlmaRestClient almaRestClient;
    
    public AlmaElectronicsClient(@NotNull AlmaRestClient almaRestClient) {
        this.almaRestClient = almaRestClient;
    }
    
    public AlmaRestClient getAlmaRestClient() {
        return almaRestClient;
    }
    
    
    public ElectronicCollections getElectronicCollections(@Nullable String query,
                                                          @Nullable Integer offset,
                                                          @Nullable Integer limit)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        WebClient link = almaRestClient.constructLink().path("/electronic/e-collections/");
        Utils.nullable(query).ifPresent(value -> link.query("q", value));
        Utils.nullable(limit).ifPresent(value -> link.query("limit", value));
        Utils.nullable(offset).ifPresent(value -> link.query("offset", value));
        
        return almaRestClient.get(link, ElectronicCollections.class);
    }
    
    public ElectronicCollection getElectronicCollection(@NotNull String collectionID)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        WebClient link = almaRestClient.constructLink().path("/electronic/e-collections/").path(collectionID);
        return almaRestClient.get(link, ElectronicCollection.class);
    }
    
    public ElectronicServices getElectronicCollectionServices(@NotNull String collectionID)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        WebClient link = almaRestClient
                .constructLink()
                .path("/electronic/e-collections/")
                .path(collectionID)
                .path("/e-services");
        return almaRestClient.get(link, ElectronicServices.class);
    }
    
    
    public ElectronicService getElectronicCollectionService(@NotNull String collectionID, @NotNull String serviceID)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        WebClient link = almaRestClient
                .constructLink()
                .path("/electronic/e-collections/")
                .path(collectionID)
                .path("/e-services/")
                .path(serviceID);
        return almaRestClient.get(link, ElectronicService.class);
    }
    
    public Portfolios getElectronicCollectionServicePortfolios(@NotNull String collectionID,
                                                               @NotNull String serviceID,
                                                               @Nullable Integer offset,
                                                               @Nullable Integer limit)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        WebClient link = almaRestClient
                .constructLink()
                .path("/electronic/e-collections/")
                .path(collectionID)
                .path("/e-services/")
                .path(serviceID)
                .path("/portfolios");
        Utils.nullable(limit).ifPresent(value -> link.query("limit", value));
        Utils.nullable(offset).ifPresent(value -> link.query("offset", value));
        return almaRestClient.get(link, Portfolios.class);
    }
    
    public Portfolio getElectronicCollectionServicePortfolio(@NotNull String collectionID,
                                                             @NotNull String serviceID,
                                                             @NotNull String portfolioID)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        WebClient link = almaRestClient
                .constructLink()
                .path("/electronic/e-collections/")
                .path(collectionID)
                .path("/e-services/")
                .path(serviceID)
                .path("/portfolios/")
                .path(portfolioID);
    
        return almaRestClient.get(link, Portfolio.class);
    }
}
