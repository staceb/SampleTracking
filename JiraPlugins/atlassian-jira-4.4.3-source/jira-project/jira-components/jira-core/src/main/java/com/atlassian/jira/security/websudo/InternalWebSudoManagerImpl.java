package com.atlassian.jira.security.websudo;

import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.config.properties.JiraSystemProperties;
import com.atlassian.jira.web.SessionKeys;
import com.atlassian.sal.api.websudo.WebSudoRequired;
import com.atlassian.util.concurrent.Nullable;
import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;
import webwork.action.Action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 */
public class InternalWebSudoManagerImpl implements InternalWebSudoManager
{

    private static final String REQUEST_ATTRIBUTE = "jira.websudo.request";
    private static final String REQUIRE_AUTHENTICATION = "Require-Authentication";
    private static final String HAS_AUTHENTICATION = "Has-Authentication";
    public static final String WEBSUDO = "X-Atlassian-WebSudo";


    private final ApplicationProperties applicationProperties;

    public InternalWebSudoManagerImpl(final ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @Override
    public boolean isEnabled()
    {
        return !JiraSystemProperties.isWebSudoDisabled() &&
               !applicationProperties.getOption(APKeys.WebSudo.IS_DISABLED) &&
               !InbuiltAuthenticatorCheck.hasCustomAuthenticator();
    }

    @Override
    public boolean matches(Class<? extends Action> actionClass)
    {
            return actionClass.getAnnotation(WebSudoRequired.class) != null;
    }

    @Override
    public boolean hasValidSession(@Nullable HttpSession session)
    {
        if(null == session)
        {
            return false;
        }
        String timeoutString = applicationProperties.getDefaultBackedString(APKeys.WebSudo.TIMEOUT);
        int timeout;
        if (StringUtils.isNotBlank(timeoutString))
        {
            timeout = Integer.valueOf(timeoutString);
        }
        else
        {
            timeout = 10;
        }
        Long timestamp = (Long) session.getAttribute(SessionKeys.WEBSUDO_TIMESTAMP);
        long timeoutMillis = timeout * 60 * 1000; // TimeUnit.MINUTES.toMillis(timeout);
        return timestamp != null && timestamp >= currentTimeMillis() - timeoutMillis;
    }

    @Override
    public boolean isWebSudoRequest(@Nullable HttpServletRequest request)
    {
        return (null != request) && Boolean.TRUE.equals(request.getAttribute(REQUEST_ATTRIBUTE));
    }

    @Override
    public void startSession(HttpServletRequest request, HttpServletResponse response)
    {
        Preconditions.checkNotNull(request);
        Preconditions.checkNotNull(response);
        request.getSession(true).setAttribute(SessionKeys.WEBSUDO_TIMESTAMP, currentTimeMillis());
        markWebSudoRequest(request);
        response.setHeader(WEBSUDO, HAS_AUTHENTICATION);
    }

    @Override
    public void markWebSudoRequest(@Nullable HttpServletRequest request)
    {
        if (null == request)
        {
            return;
        }
        request.setAttribute(REQUEST_ATTRIBUTE, Boolean.TRUE);
    }

    @Override
    public void invalidateSession(HttpServletRequest request, HttpServletResponse response)
    {
        final HttpSession session = null != request ? request.getSession(false) : null;
        if(null != session)
        {
            session.removeAttribute(SessionKeys.WEBSUDO_TIMESTAMP);
        }
        response.setHeader(WEBSUDO, REQUIRE_AUTHENTICATION);
    }

    /*
     * Used for testing.
     */
    long currentTimeMillis()
    {
        return System.currentTimeMillis();
    }
}
