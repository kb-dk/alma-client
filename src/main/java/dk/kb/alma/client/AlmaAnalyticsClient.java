package dk.kb.alma.client;

import dk.kb.alma.client.exceptions.AlmaKnownException;
import dk.kb.alma.gen.analytics.Report;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AlmaAnalyticsClient {
    
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final AlmaRestClient almaRestClient;
    
    public AlmaAnalyticsClient(AlmaRestClient almaRestClient) {
        this.almaRestClient = almaRestClient;
    }
    
    public AlmaRestClient getAlmaRestClient() {
        return almaRestClient;
    }
    
    /*Reports*/
    
    /**
     * @param reportPath Full path to the report
     * @param filter     Optional.	An XML representation of a filter in OBI format ???
     * @param limit      Optional. Default: 25	Maximum number of results to return - between 25 and 1000 (multiples of
     *                   25)
     * @param col_names  Optional. Default: true	Include column heading information. Default: true. To ensure consistent
     *                   sort order it might be required to turn it off (col_names=false).
     * @return
     */
    public dk.kb.alma.client.analytics.Report startReport(
            @Nonnull String reportPath,
            @Nullable String filter,
            @Nullable Integer limit,
            @Nullable Boolean col_names) {
        filter = filter == null ? "" : filter;
        limit = restrictLimit(limit);
        col_names = col_names == null ? true : col_names;
    
        return dk.kb.alma.client.analytics.Report.parseFromAlmaReport(
                almaRestClient.get(almaRestClient.constructLink()
                                                 .path("/analytics/reports")
                                                 .query("path", reportPath)
                                                 .query("filter", filter)
                                                 .query("limit", limit)
                                                 .query("col_names", col_names), Report.class, false),
                null);
    }
    
    
    /**
     * @param report
     * @return
     * @throws IllegalArgumentException when you try to continue a report that is already finished
     */
    public dk.kb.alma.client.analytics.Report continueReport(dk.kb.alma.client.analytics.Report report)
            throws IllegalArgumentException {
        if (report.isFinished()) {
            throw new IllegalArgumentException("The report is finished, there is no more to get here");
        }
        log.debug("Using Token {}",report.getToken());
        final Report rawReport;
        try {
            rawReport = almaRestClient.get(almaRestClient.constructLink().path("/analytics/reports")
                                                         .query("token", report.getToken()), Report.class, false);
        } catch (AlmaKnownException e) {
            //TODO This is a hack, but it seems that sometimes we miss isFinished...?
            if (e.getErrorCode().equals("420033") && e.getErrorMessage().equals("No more rows to fetch")) {
                throw new IllegalArgumentException("The report is finished, there is no more to get here, but somehow we did not see the finished flag???", e);
            } else {
                throw e;
            }
        }
        return dk.kb.alma.client.analytics.Report.parseFromAlmaReport(
                rawReport, //Important that cache is not used, as this is the same url being requested each time
                report);
    }
    
    /* When reports stop
    2021-11-04 10:10:22 [main] ERROR dk.kb.alma.client.utils.HttpUtils(HttpUtils.java:47) - Failed to parse response '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><web_service_result xmlns="http://com/exlibris/urm/general/xmlbeans"><errorsExist>true</errorsExist><errorList><error><errorCode>420033</errorCode><errorMessage>No more rows to fetch</errorMessage><trackingId>E02-0411091022-OVKS5-AWAE948043988</trackingId></error></errorList></web_service_result>' as WebServiceResult, but throwing based on the original exception 'jakarta.ws.rs.InternalServerErrorException: HTTP 500 Internal Server Error'
dk.kb.alma.client.exceptions.AlmaUnknownException: Failed to GET on 'https://api-eu.hosted.exlibrisgroup.com/almaws/v1/analytics/reports?lang=da&token=97BA943266B3B2155DBB9F6B58A0F4C4E65BC6BCBA8C16A872F10530D1BEFB4EFC94D111DA30DE180D54EFF14DA657CA590F866A14B76ED8E14C823BBC4988AF' with response '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><web_service_result xmlns="http://com/exlibris/urm/general/xmlbeans"><errorsExist>true</errorsExist><errorList><error><errorCode>420033</errorCode><errorMessage>No more rows to fetch</errorMessage><trackingId>E02-0411091022-OVKS5-AWAE948043988</trackingId></error></errorList></web_service_result>' with headers '{Access-Control-Allow-Headers=[Origin, X-Requested-With, Content-Type, Accept, Authorization], Access-Control-Allow-Methods=[GET,OPTIONS], Access-Control-Allow-Origin=[*], connection=[close], Content-Length=[352], content-type=[application/xml;charset=UTF-8], Date=[Thu, 04 Nov 2021 09:10:21 GMT], p3p=[CP="IDC DSP COR ADM DEVi TAIi PSA PSD IVAi IVDi CONi HIS OUR IND CNT"], Server=[CA-API-Gateway/9.0], X-Exl-Api-Remaining=[1519628], X-Request-ID=[zkvHCAOFRZ], Content-Type=[application/xml;charset=UTF-8]}'
        at dk.kb.alma.client.utils.HttpUtils.readWebServiceResult(HttpUtils.java:44)
        at dk.kb.alma.client.HttpClient.handleWebApplicationException(HttpClient.java:412)
        at dk.kb.alma.client.HttpClient.invokeDirect(HttpClient.java:348)
        at dk.kb.alma.client.HttpClient.invokeDirect(HttpClient.java:314)
        at dk.kb.alma.client.HttpClient.invokeCache(HttpClient.java:282)
        at dk.kb.alma.client.HttpClient.get(HttpClient.java:226)
        at dk.kb.alma.client.AlmaAnalyticsClient.continueReport(AlmaAnalyticsClient.java:71)
        at dk.statsbiblioteket.cdrip.importer.AlmaImporter.fetchAnalyticsCDs(AlmaImporter.java:268)
        at dk.statsbiblioteket.cdrip.importer.AlmaImporter.main(AlmaImporter.java:125)
Caused by: java.lang.IllegalArgumentException: Invalid return type 'application/xml;charset=UTF-8'
        ... 9 common frames omitted

    
    
     */
    
    /**
     * Transforms the input integer to be * positive * between 25 and 1000 * A multiple of 25
     *
     * @param limit
     * @return
     */
    protected static Integer restrictLimit(@Nullable Integer limit) {
        
        limit = (limit == null ? 25 : limit); //if null, set to 25
        
        limit = Math.max(0, limit); //Make positive
        
        final double multipleOf25 = Math.ceil(limit / 25.0); //Find the multiple of 25
        final long multipliedBack = (long) multipleOf25 * 25;
        limit = (int) Math.min(1000L, multipliedBack);
        
        return limit;
    }
    
}
