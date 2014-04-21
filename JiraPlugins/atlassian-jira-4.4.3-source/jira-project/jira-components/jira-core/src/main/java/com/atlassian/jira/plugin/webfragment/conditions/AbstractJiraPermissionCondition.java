package com.atlassian.jira.plugin.webfragment.conditions;

import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.plugin.PluginParseException;
import com.opensymphony.user.User;

import java.util.Map;

/**
 * Convenient abstraction to initialise conditions that require the {@link PermissionManager} and accept "permission"
 * param.
 * <p/>
 * The permission param is converted using {@link Permissions#getType(String)} and its value is set in {@link
 * #permission}
 */
public abstract class AbstractJiraPermissionCondition extends AbstractJiraCondition
{
    protected PermissionManager permissionManager;
    protected int permission;

    public AbstractJiraPermissionCondition(PermissionManager permissionManager)
    {
        this.permissionManager = permissionManager;
    }

    public void init(Map params) throws PluginParseException
    {
        permission = Permissions.getType((String) params.get("permission"));
        if (permission == -1)
        {
            throw new PluginParseException("Could not determine permission type for: " + params.get("permission"));
        }

        super.init(params);
    }

    public abstract boolean shouldDisplay(User user, JiraHelper jiraHelper);
}
