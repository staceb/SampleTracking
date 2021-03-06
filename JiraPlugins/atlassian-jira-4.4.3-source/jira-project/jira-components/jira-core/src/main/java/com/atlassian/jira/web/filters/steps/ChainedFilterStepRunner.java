package com.atlassian.jira.web.filters.steps;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

/**
 * A {@link Filter} that consists of a chain of filter steps to run.  A top level filter can become one of these guys
 * and name the steps he wants to run.
 * <p/>
 * This will only run the steps once per request.  Internal redirects will not have the filters re-run.  This is the
 * standard JIRA pattern.
 */
public abstract class ChainedFilterStepRunner implements Filter
{
    private final String filterName;

    protected ChainedFilterStepRunner()
    {
        filterName = this.getClass().getCanonicalName() + "_alreadyfiltered";
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
    }

    @Override
    public void destroy()
    {
    }

    /**
     * @return the list of {@link FilterStep}s to run
     */
    protected abstract List<FilterStep> getFilterSteps();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException
    {
        final HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        final HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

        // Only apply this filter once per request
        if (servletRequest.getAttribute(filterName) != null)
        {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
            return;
        }
        else
        {
            httpServletRequest.setAttribute(filterName, Boolean.TRUE);
        }


        List<FilterStep> filterSteps = notNull("filterSteps", getFilterSteps());
        FilterCallContext callContext = new FilterCallContextImpl(httpServletRequest, httpServletResponse, filterChain);
        try
        {
            for (final FilterStep filterStep : filterSteps)
            {
                callContext = notNull("callContext", filterStep.beforeDoFilter(callContext));
            }
            filterChain.doFilter(servletRequest, servletResponse);
        }
        finally
        {
            for (final FilterStep filterStep : filterSteps)
            {
                callContext = notNull("callContext", filterStep.finallyAfterDoFilter(callContext));
            }
        }

    }

}
