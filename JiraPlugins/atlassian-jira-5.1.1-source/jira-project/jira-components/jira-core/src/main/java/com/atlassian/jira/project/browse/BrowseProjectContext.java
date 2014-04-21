package com.atlassian.jira.project.browse;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.query.Query;
import com.atlassian.query.QueryImpl;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.clause.TerminalClauseImpl;
import com.atlassian.query.operator.Operator;

import java.util.HashMap;
import java.util.Map;

/**
 * Context for BrowseProject when viewing a project.
 *
 * @since v4.0
 */
public class BrowseProjectContext implements BrowseContext
{
    protected Project project;
    final private User user;
    protected TerminalClause projectClause;

    public BrowseProjectContext(User user, Project project)
    {
        this.project = project;
        this.user = user;
    }

    public Project getProject()
    {
        return project;
    }

    public User getUser()
    {
        return user;
    }

    public Query createQuery()
    {
        return new QueryImpl(getProjectClause());
    }

    protected TerminalClause getProjectClause()
    {
        if (projectClause == null)
        {
            projectClause = new TerminalClauseImpl(IssueFieldConstants.PROJECT, Operator.EQUALS, getProject().getKey());
        }
        return projectClause;
    }

    protected ProjectManager getProjectManager()
    {
        return ComponentAccessor.getProjectManager();
    }

    protected SearchService getSearchService()
    {
        return ComponentManager.getComponent(SearchService.class);
    }

    ///CLOVER:OFF
    public String getQueryString()
    {
        final Query query = new QueryImpl(getProjectClause());
        return getSearchService().getQueryString(getUser(), query);
    }
    ///CLOVER:ON

    public Map<String, Object> createParameterMap()
    {
        final Map<String, Object> map = new HashMap<String, Object>(2);
        map.put("project", getProject());
        map.put("user", getUser());
        return map;
    }

    public String getContextKey()
    {
        return project.getKey();
    }
}
