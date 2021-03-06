package com.atlassian.jira.util;

import java.util.List;

/**
 * Component that provides access to project and issue key utilities.
 */
public class JiraKeyUtilsBean
{

    public boolean validProjectKey(String key)
    {
        return JiraKeyUtils.validProjectKey(key);
    }

    public String getProjectKeyFromIssueKey(String key)
    {
        return JiraKeyUtils.getProjectKeyFromIssueKey(key);
    }

    public long getCountFromKey(String key)
    {
        return JiraKeyUtils.getCountFromKey(key);
    }

    public boolean validIssueKey(String key)
    {
        return JiraKeyUtils.validIssueKey(key);
    }

    public boolean isKeyInString(String issueKey, String body)
    {
        return JiraKeyUtils.isKeyInString(issueKey, body);
    }

    public boolean isKeyInString(String s)
    {
        return JiraKeyUtils.isKeyInString(s);
    }

    public String linkBugKeys(String body)
    {
        return JiraKeyUtils.linkBugKeys(body);
    }

    public List getIssueKeysFromString(String body)
    {
        return JiraKeyUtils.getIssueKeysFromString(body);
    }

}
