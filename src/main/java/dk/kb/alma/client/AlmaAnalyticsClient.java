package dk.kb.alma.client;

import dk.kb.alma.client.exceptions.AlmaConnectionException;
import dk.kb.alma.client.exceptions.AlmaKnownException;
import dk.kb.alma.client.exceptions.AlmaUnknownException;
import dk.kb.alma.gen.analytics.Report;
import org.apache.cxf.jaxrs.client.WebClient;
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
    
    public WebClient constructLink() {
        return almaRestClient.constructLink();
    }
    
    public <T> T get(WebClient link, Class<T> type)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return almaRestClient.get(link, type);
    }
    
    public <T> T get(WebClient link, Class<T> type, boolean useCache)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return almaRestClient.get(link, type, useCache);
    }
    
    public <T, E> T put(WebClient link, Class<T> type, E entity)
            throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
        return almaRestClient.put(link, type, entity);
    }
    
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
                get(constructLink()
                            .path("/analytics/reports")
                            .query("path", reportPath)
                            .query("filter", filter)
                            .query("limit", limit)
                            .query("col_names", col_names),
                    Report.class,
                    false),
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
        final Report rawReport;
        try {
            rawReport = get(constructLink().path("/analytics/reports")
                                           .query("token", report.getToken()),
                            Report.class,
                            false);
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
