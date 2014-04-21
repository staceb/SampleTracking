package com.atlassian.jira.issue.views;

import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.issue.views.util.SearchRequestViewBodyWriterUtil;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.web.component.IssueTableLayoutBean;
import com.atlassian.jira.web.component.TableLayoutFactory;
import com.opensymphony.user.User;

public class SearchRequestExcelViewCurrentFields extends AbstractSearchRequestExcelView
{
    public SearchRequestExcelViewCurrentFields(JiraAuthenticationContext authenticationContext, SearchProvider searchProvider, ApplicationProperties appProperties, TableLayoutFactory tableLayoutFactory, SearchRequestViewBodyWriterUtil searchRequestViewBodyWriterUtil)
    {
        super(authenticationContext, searchProvider, appProperties, tableLayoutFactory, searchRequestViewBodyWriterUtil);
    }

    protected IssueTableLayoutBean getColumnLayout(SearchRequest searchRequest, User user)
    {
        return tableLayoutFactory.getStandardExcelLayout(searchRequest, user);
    }
}
