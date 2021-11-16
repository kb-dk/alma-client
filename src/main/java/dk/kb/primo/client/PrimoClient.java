package dk.kb.primo.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.jakarta.rs.json.JacksonXmlBindJsonProvider;
import dk.kb.util.json.JSON;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.impl.UriBuilderImpl;
import org.apache.cxf.transport.http.HTTPConduit;

import jakarta.ws.rs.core.MediaType;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class PrimoClient {
    
    public URI restBaseURLSuprima;// = URI.create("https://kbdk-kgl-psb.primo.exlibrisgroup.com/primaws/rest");
    //public URI restBaseURLSuprima; = URI.create("https://kbdk-kgl-psb.primo.exlibrisgroup.com/primaws/rest");
    //public URI restBaseURLSuprima = URI.create("https://soeg.kb.dk/primaws/rest");
    
    private final String inst;
    private final String vid;
    private final String lang;
    private Map<String, String> globalParams;
    
    /**
     * @param restBaseURLSuprima
     * @param inst
     * @param vid                The view ID.
     * @param lang               use the two letters format language, i.e. en for english
     */
    public PrimoClient(URI restBaseURLSuprima, String inst, String vid, String lang) {
        this.restBaseURLSuprima = restBaseURLSuprima;
        this.inst               = inst;
        this.vid                = vid;
        this.lang               = lang;
    }
    
    private WebClient getWebClient(URI link) {
        URI host = new UriBuilderImpl(link).replaceQuery(null).replacePath(null).replaceMatrix(null).build();
        
        
        JacksonXmlBindJsonProvider jacksonJaxbJsonProvider = new JacksonXmlBindJsonProvider();
        jacksonJaxbJsonProvider.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);
        jacksonJaxbJsonProvider.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        
        
        final List<?> providers = Arrays.asList(jacksonJaxbJsonProvider);
        WebClient client = WebClient.create(host.toString(), providers);
        
        
        HTTPConduit conduit = WebClient.getConfig(client).getHttpConduit();
        //conduit.getClient().setConnectionTimeout(connectTimeout);
        //conduit.getClient().setConnectionRequestTimeout(connectTimeout);
        //conduit.getClient().setReceiveTimeout(readTimeout);
        
        if (link.getPath() != null) {
            client = client.path(link.getPath());
        }
        if (link.getQuery() != null) {
            client = client.replaceQuery(link.getQuery());
        }
        
        client = client
                         .accept(MediaType.APPLICATION_JSON_TYPE)
                         .type(MediaType.APPLICATION_JSON_TYPE);
        
        if (globalParams != null) {
            for (Map.Entry<String, String> globalParam : globalParams.entrySet()) {
                client = client.replaceQueryParam(globalParam.getKey(), globalParam.getValue());
            }
        }
        
        return client;
    }
    
    
    // webpack:///src/main/webapp/components/appConfig/appConfig.ts
    
    /*
    let restBaseURLSuprima= '/primaws/rest';
    let restBaseURLsSuprima={
            resourceRecommenderUrl: restBaseURLSuprima + '/pub/resourceRecommender',
    deliveryBaseURL: restBaseURLSuprima+'/priv/delivery/representationInfo',
    officeFileURL: restBaseURLSuprima+'/priv/delivery/officeFile',
    updateStatistics:restBaseURLSuprima+'/priv/delivery/updateStatistics',
    referenceEntryUrl: restBaseURLSuprima + '/priv/externalServices/referenceEntry',
    myAccountBaseURL: restBaseURLSuprima+'/priv/myaccount',
    userSettingsBaseURL: restBaseURLSuprima+'/priv/usersettings',
    favoritesBaseURL: restBaseURLSuprima+'/priv/favorites',
    bestOfferURL: restBaseURLSuprima +'/pub/ngrs/bestoffer/physical',
    digitalBestOfferURL: restBaseURLSuprima +'/pub/ngrs/bestoffer/digital',
    ngrsCreateBorrowingRequestURL: restBaseURLSuprima +'/pub/ngrs/bestoffer/borrowingrequest',
    primaPickupLocationBaseUrl: restBaseURLSuprima +'/pub/ngrs/bestoffer/pickupLocation',
    moreOptionBaseUrl: restBaseURLSuprima +'/pub/ngrs/bestoffer/moreResult',
    primaEmailUrl: restBaseURLSuprima +'/pub/ngrs/bestoffer/email',
    primaCopyRightsStatementUrl: restBaseURLSuprima +'/pub/ngrs/bestoffer/copyRightsStatement',
    gtiURL: restBaseURLSuprima +'/pub/ngrs/gti',
    requestStatusMapURL: restBaseURLSuprima +'/pub/ngrs/requestStatusMap',
    ngrsResourceSharingServiceEnabledURL: restBaseURLSuprima +'/pub/ngrs/ngrsResourceSharingServiceEnabled',
    ngrsNoOfferMessageURL: restBaseURLSuprima +'/pub/ngrs/ngrsNoOfferMessage',
    ngrsPickupInformationFromUserPreferenceUrl: restBaseURLSuprima +'/pub/ngrs/ngrsPickupInformationFromUserPreferenceUrl',
    ngrsPickupInformationUrl: restBaseURLSuprima +'/pub/ngrs/ngrsPickupInformationUrl',
    datesMapURL: restBaseURLSuprima +'/pub/ngrs/datesMap',
    requestFormMessageURL: restBaseURLSuprima +'/pub/ngrs/requestFormMessage',
    tagsBaseURL: restBaseURLSuprima+'/priv/tags',
    tagsSearchBaseURL: restBaseURLSuprima+'/priv/tags/searchTags?tagTerm=',
    pnxBaseURL: restBaseURLSuprima +'/pub/pnxs',
    deliveryURL: restBaseURLSuprima +'/pub/delivery',
    getFacetsBaseURL: restBaseURLSuprima +'/pub/facets',
    pushToBaseURL: restBaseURLSuprima +'/pub/pushto',
    actionsBaseURL: restBaseURLSuprima+'/pub/actions',
    opacBaseUrl: restBaseURLSuprima+'/priv/ILSServices/titleServices',
    openUrl: restBaseURLSuprima+'/pub/openurl',
    ILSServicesBaseURL: restBaseURLSuprima+'/priv/ILSServices',
    prepareElectorincRTA: restBaseURLSuprima+'/pub/edelivery',
    calculatePhysicalServiceId: restBaseURLSuprima+'/pub/getPhysicalService',
    translationsUrl: restBaseURLSuprima + '/pub/translations',
    bxUrl: restBaseURLSuprima + '/pub/bx/recId',
    thumbUrl: restBaseURLSuprima + '/v1/syndetics-thumb',
    browse: restBaseURLSuprima +'/pub/browse',
    saveSearchURL : restBaseURLSuprima + '/priv/savedSearches',
    snippetURL: restBaseURLSuprima + '/pub/snippets',
    savedQueries: restBaseURLSuprima + '/v1/savedSearches',
    citationTrails: restBaseURLSuprima + '/pub/citation-trails',
    citationTrailsSeedsInfo: restBaseURLSuprima + '/pub/citation-trails/seeds-info',
    timesCited: restBaseURLSuprima + '/pub/timesCited',
    sourceRecord: restBaseURLSuprima + '/pub/sourceRecord',
    scopeListBaseURL: restBaseURLSuprima+'/v1/configuration',
    calculatePcDelivery : restBaseURLSuprima + '/pub/pcDelivery',
    journalsAutocomplete : restBaseURLSuprima + '/pub/journals/autocomplete',
    loginJwtCache: restBaseURLSuprima + '/pub/loginJwtCache',
    rss: restBaseURLSuprima + '/priv/savedSearches/addRss',
    memberPnx: restBaseURLSuprima + '/priv/nz/pnx',
    showPnxInXmlUrl: restBaseURLSuprima + '/pub/pnxs/xml',
    dbCategoriesBaseURL: restBaseURLSuprima + '/pub/restDlf/categories',
    directLink: restBaseURLSuprima + '/pub/directLink',
    relateditems: restBaseURLSuprima+ '/v1/relatedItems',
    orgListEsploroUrl: restBaseURLSuprima + '/pub/restDlf/orgList',
    esploroUsageDataUrl: '/esplorows/rest/research/usage',
    esploroPortalSearchServices: '/esplorows/rest/research/portalSearchServices',
    recentScopes: restBaseURLSuprima + '/priv/recentScopes',
    clearDeliverySessionAtLogout: restBaseURLSuprima + '/priv/delivery/clearDeliverySessionAtLogout',
    getThumbnail: restBaseURLSuprima + '/priv/delivery/thumbnail',
    registerForDeliveryURL: restBaseURLSuprima + '/priv/delivery/registerForDelivery',
    unpaywallURL: restBaseURLSuprima + '/v1/unpaywall'
};

     */
    
    /**
     *
     *  @see  <a href="https://developers.exlibrisgroup.com/primo/apis/docs/primoSearch/R0VUIC9wcmltby92MS9zZWFyY2g=/">Primo search</a> for more detail.
     *
     * @param query    The query string that you want to use to perform a search.
     *                 The query string uses the following format:
     *
     *                 <pre>q=$field_1,$precision_1,$value_1[[,$operator_1];$field_n,$precision_n,$value_n...]</pre>
     *
     *                 <ul>
     *                  <li> field - The data field that you want to search within. The following fields are valid: any (for any field), title, creator (for author), sub (for subject), and usertag (for tag). </li>
     *                  <li> precision - The precision operation that you want to apply to the field. The following precision operators are valid: exact (value must match the data in the field exactly), begins_with (the value must be found at the beginning of the field), and contains (the value must be found anywhere in the field). </li>
     *                  <li> value - The search terms, which can be a word, phrase, or exact phrase (group of words enclosed by quotes), and can include the following logical operators: AND, OR, and NOT. For more information regarding search terms, see Performing Basic Searches.</li>
     *                  <li> operator - When specifying multiple search fields for advanced searches, this parameter applies the following logical operations between fields: AND (specified values must be found in both fields), OR (specified values must be found in at least one of the fields), NOT (the specified value of the next field must not be found). If no operator is specified, the system defaults to AND.</li>
     *                 </ul>
     *                 Note: Multiple fields are delimited by a semicolon. <br/>
     *                 Limitation: The value must not include a semicolon character.
     *                 <p>
     *                 In the following example, the system searches for all records in which the word home is found anywhere within the record's title</p>
     *                 <pre>q=title,contains,home</pre>
     *
     *                 <p>In the following example, the system searches for all records in which the title field contains the words pop and music and the subject field contains the word korean: </p>
     *                 <pre>q=title,contains,pop music,AND;sub,contains,korean</pre>
     * @param offset   The offset of the results from which to start displaying the results.
     *                 *                 <br/>
     * @param limit    The maximum number of results in the response
     *                 *                 <br/>
     * @param qInclude Filters the results by the facets that you want to include. The logical AND operation is applied
     *                 between the facets.
     *                 This filter uses the following format:
     *                 <pre>qInclude=$facet_category_1,exact,$facet_name_1[|,|$facet_category_n,exact,$facet_name_n...]</pre>
     *
     *                 <p>Note: Multiple categories are delimited by the following string of characters: |,|</p>
     *
     *                 <ul>
     *                  <li> facet_category - The facet category that you want to include. The following categories are valid: facet_rtype (Resources Type), facet_topic (Subject), facet_creator (Author), facet_tlevel (Availability), facet_domain (Collection), facet_library (library name), facet_lang (language), facet_lcc (LCC classification).</li>
     *                   <li> facet_name - The name of the facet to include (such as Journals if facet_rtype was selected).</li>
     *                 </ul>
     *                 <br/>
     * @param qExclude Filters the results by the facets that you want to exclude. The logical AND operation is applied
     *                 between the facets.
     *                 This parameter uses the following format:
     *                 <pre>qExclude=$facet_category_1,exact,$facet_name_1[|,|$facet_category_n,exact,$facet_name_n...]</pre>
     *
     *                 <p>Note: Multiple categories are delimited by the following string of characters: |,|</p>
     *
     *                 <ul>
     *                   <li> facet_category - The facet category that you want to exclude. The following categories are valid: facet_rtype (Resources Type), facet_topic (Subject), facet_creator (Author), facet_tlevel (Availability), facet_domain (Collection), facet_library (library name), facet_lang (language), facet_lcc (LCC classification) </li>
     *                   <li>facet_name - The name of the facet to exclude (such as Journals if facet_rtype was selected).</li>
     *                 </ul>
     *                 <br/>
     *
     * @param pcAvailability Indicates whether PC records that do not have full text are displayed in the results. The valid values are true (display all records even if they do not have full text) or false (display full text records only). Default true
     *
     * @param getMore Relevant for searches in Metalib.
     * Indicates whether to expand the number of results in Metalib searches. The valid values are 0 (false) or 1 (true). Default 0
     *
     * @param sort The type of sort to perform on the results, which can be based on relevance or a specific field: rank, title, author, or date. Default rank
     *
     * @param tab The name of the tab.
     * @param scope The scope name.
     * @param skipDeliveryInfo N to include the delivery info, such as the location and holdingID
     * @param includeNewspapers
     * @return
     */
    public JsonNode search(String query,
                           Integer offset,
                           Integer limit,
                           String qInclude,
                           String qExclude,
                           Boolean pcAvailability,
                           Integer getMore,
                           String sort,
                           String tab,
                           String scope,
                           String skipDeliveryInfo,
                           Boolean includeNewspapers) {
        
        var value = getWebClient(restBaseURLSuprima)
                            .path("pub")
                            .path("pnxs")
                            .query("blendFacetsSeparately", false)
                            .query("disableCache", "false")
                            .query("getMore", getMore)
                            //.query("inst", "45KBDK_KGL")
                            .query("inst", inst)
                            .query("lang", lang)
                            .query("limit", limit)
                            .query("newspapersActive", "true")
                            .query("newspapersSearch", includeNewspapers)
                            .query("offset", offset)
                            .query("pcAvailability", pcAvailability)
                            .query("q", query)
                            .query("qExclude", qExclude)
                            .query("qInclude", qInclude)
                            .query("rapido", "false")
                            .query("refEntryActive", "true")
                            .query("rtaLinks", "false")
                            .query("scope", scope)
                            .query("skipDelivery", skipDeliveryInfo)
                            .query("sort", sort)
                            .query("tab", tab)
                            .query("vid", vid)
                            .get(String.class);
        return JSON.fromJson(value, JsonNode.class);
    }
    
    public JsonNode getTranslations(String targetLanguage) {
        var value = getWebClient(restBaseURLSuprima).path("pub")
                                                    .path("translations")
                                                    .path(vid)
                                                    //.path("45KBDK_KGL:KGL")
                                                    //.query("lang", "da")
                                                    .query("lang", targetLanguage)
                                                    .get(String.class);
        
        return JSON.fromJson(value, JsonNode.class);
    }
    
    public JsonNode getConfig() {
        var value = getWebClient(restBaseURLSuprima).path("pub")
                                                    .path("configuration")
                                                    .path("vid")
                                                    //.path("45KBDK_KGL:KGL")
                                                    .path(vid)
                                                    .get(String.class);
        
        return JSON.fromJson(value, JsonNode.class);
        
    }
}
