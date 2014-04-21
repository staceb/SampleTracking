package com.atlassian.jira.web.filters.accesslog;

import com.atlassian.jira.config.properties.JiraSystemProperties;
import com.atlassian.jira.util.collect.LRUMap;
import org.joda.time.DateTime;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import static com.atlassian.jira.web.filters.accesslog.AccessLogBuilder.enc;
import static com.atlassian.jira.web.filters.accesslog.AccessLogBuilder.formatMStoDecimalSecs;
import static com.atlassian.jira.web.filters.accesslog.AccessLogBuilder.getDateString;
import static com.atlassian.jira.web.filters.accesslog.AccessLogRequestInfo.JIRA_REQUEST_ASESSIONID;
import static com.atlassian.jira.web.filters.accesslog.AccessLogRequestInfo.JIRA_REQUEST_ID;
import static com.atlassian.jira.web.filters.accesslog.AccessLogRequestInfo.JIRA_REQUEST_START_MILLIS;
import static com.atlassian.jira.web.filters.accesslog.AccessLogRequestInfo.JIRA_SESSION_LAST_ACCESSED_TIME;
import static com.atlassian.jira.web.filters.accesslog.AccessLogRequestInfo.JIRA_SESSION_MAX_INACTIVE_INTERVAL;
import static com.atlassian.jira.web.filters.accesslog.AccessLogRequestInfo.concurrentRequests;
import static org.apache.commons.lang.StringUtils.leftPad;

import com.atlassian.jira.ofbiz.PerformanceSQLInterceptor;
import com.atlassian.jira.security.JiraAuthenticationContextImpl;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This can imprint output such as web pages with details that will have been placed in the access logs.  This is useful
 * for debugging problems in the wild and also for performance throughput tracking
 *
 * @since v3.13.2
 */
public class AccessLogImprinter
{
    public static final String REQUEST_SQL_CACHE_STORAGE = "request.sql.cache.storage";
    
    private static final int LEFT_PAD = 30;
    private static final int MAX_NUM_REQUESTS = 50;

    private final HttpServletRequest httpServletRequest;

    public AccessLogImprinter(final HttpServletRequest httpServletRequest)
    {
        this.httpServletRequest = httpServletRequest;
    }

    /**
     * Returns an imprint of access log information as a HTML comment
     *
     * @return a HTML comment with access log information
     */
    public String imprintHTMLComment()
    {
        final Long startMS = (Long) httpServletRequest.getAttribute(JIRA_REQUEST_START_MILLIS);
        final Long responseTimeMS = (startMS == null ? null : System.currentTimeMillis() - startMS);
        final DateTime requestTimeStamp = (startMS == null ? null : new DateTime(startMS));

        final String comment = new StringBuilder()
                .append("\n\t").append(leftPad("REQUEST ID : ", LEFT_PAD)).append(enc(httpServletRequest.getAttribute(JIRA_REQUEST_ID)))
                .append("\n\t").append(leftPad("REQUEST TIMESTAMP : ", LEFT_PAD)).append(getDateString(requestTimeStamp))
                .append("\n\t").append(leftPad("REQUEST TIME : ", LEFT_PAD)).append(enc(formatMStoDecimalSecs(responseTimeMS)))
                .append("\n\t").append(leftPad("ASESSIONID : ", LEFT_PAD)).append(enc(httpServletRequest.getAttribute(JIRA_REQUEST_ASESSIONID)))
                .append("\n\t").append(leftPad("CONCURRENT REQUESTS : ", LEFT_PAD)).append(enc(concurrentRequests))
                .toString();
        return new StringBuilder("\n<!--").append(htmlCommentEsc(comment)).append("\n-->").toString();
    }

    public String imprintHiddenHtml()
    {
        final Long startMS = (Long) httpServletRequest.getAttribute(JIRA_REQUEST_START_MILLIS);
        final Long responseTimeMS = (startMS == null ? null : System.currentTimeMillis() - startMS);
        final Long sessionLastAccess = (Long) httpServletRequest.getAttribute(JIRA_SESSION_LAST_ACCESSED_TIME);
        final Integer sessionMaxInactiveInterval = (Integer) httpServletRequest.getAttribute(JIRA_SESSION_MAX_INACTIVE_INTERVAL);
        final Long expiryTime = calculateSessionExpiry(sessionLastAccess, sessionMaxInactiveInterval);
        final Long expiresIn = calculateSessionExpiresIn(expiryTime);


        //
        // At the time I wrote this code, the values output do not need HTML escaping!
        //
        final StringBuilder builder = new StringBuilder("\n<form id=\"jira_request_timing_info\" class=\"dont-default-focus\" >");
        builder.append("\n\t<fieldset class=\"parameters hidden\">")
                .append("\n\t\t").append(hiddenInput(JIRA_REQUEST_START_MILLIS, enc(startMS)))
                .append("\n\t\t").append(hiddenInput("jira.request.server.time", enc(responseTimeMS)))
                .append("\n\t\t").append(hiddenInput("jira.request.id", enc(httpServletRequest.getAttribute(JIRA_REQUEST_ID))))
                .append("\n\t\t").append(hiddenInput("jira.session.expiry.time", enc(expiryTime)))
                .append("\n\t\t").append(hiddenInput("jira.session.expiry.in.mins", enc(expiresIn)))
                .append("\n\t\t").append(hiddenInput("jiraConcurrentRequests", "jira.request.concurrent.requests", enc(concurrentRequests)));

        if(JiraSystemProperties.showPerformanceMonitor())
        {
            final PerformanceSQLInterceptor.SQLPerfCache perfCache = (PerformanceSQLInterceptor.SQLPerfCache)
                    JiraAuthenticationContextImpl.getRequestCache().get(PerformanceSQLInterceptor.SQL_PERF_CACHE);
            if (perfCache != null)
            {
                builder.append("\n\t\t").append(hiddenInput("jiraSQLtime", enc(perfCache.getTotalTimeMs())))
                        .append("\n\t\t").append(hiddenInput("jiraSQLstatements", enc(perfCache.getNumStatements())));
                recordSQLdataInSession(perfCache);
            }
            builder.append("\n\t\t").append(hiddenInput("showmonitor", "true"));                 
        }

        builder.append("\n\t</fieldset>").append("\n</form>");
        return builder.toString();
    }

    private void recordSQLdataInSession(PerformanceSQLInterceptor.SQLPerfCache perfCache)
    {
        final HttpSession session = httpServletRequest.getSession(false);
        if(session != null)
        {
            @SuppressWarnings("unchecked")
            Map<String, PerformanceSQLInterceptor.SQLPerfCache> storage =
                    (Map<String, PerformanceSQLInterceptor.SQLPerfCache>) session.getAttribute(REQUEST_SQL_CACHE_STORAGE);
            if(storage == null)
            {
                //only keep the last 50 requests!
                storage = LRUMap.newLRUMap(MAX_NUM_REQUESTS);
                session.setAttribute(REQUEST_SQL_CACHE_STORAGE, storage);
            }
            storage.put((String) httpServletRequest.getAttribute(JIRA_REQUEST_ID), perfCache);
        }
    }

    private Long calculateSessionExpiry(Long sessionLastAccess, Integer sessionMaxInactiveInterval)
    {
        if (sessionLastAccess == null || sessionMaxInactiveInterval == null)
        {
            return null;
        }
        return sessionLastAccess + (sessionMaxInactiveInterval * 1000);
    }

    private Long calculateSessionExpiresIn(final Long expiryTime)
    {
        return expiryTime == null ? null : (expiryTime - System.currentTimeMillis()) / 1000 / 60;
    }

    private String hiddenInput(final String name, final String value)
    {
        return new StringBuilder("<input type=\"hidden\" title=\"").append(name).append("\" value=\"").append(value).append("\" />").toString();
    }

    private String hiddenInput(final String id, final String name, final String value)
    {
        return new StringBuilder("<input id=\"").append(id).append("\" type=\"hidden\" name=\"").append(name).append("\" value=\"").append(value).append("\" />").toString();
    }

    private String htmlCommentEsc(final String comment)
    {
        if (comment.indexOf("--") != -1)
        {
            return comment.replaceAll("--", "-:");
        }
        return comment;
    }

}
