package com.atlassian.jira.dev.reference.plugin.tabpanels;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.tabpanels.GenericMessageAction;
import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueTabPanel;
import com.opensymphony.user.User;

import java.util.Collections;
import java.util.List;

/**
 * Describes a simple reference {@link com.atlassian.jira.plugin.issuetabpanel.IssueTabPanel} that displays a
 * short text message on a tab in the view issue page.
 *
 * @since v4.3
 */
public class ReferenceIssueTabPanel extends AbstractIssueTabPanel
{
    public List getActions(Issue issue, User remoteUser)
    {
        return Collections.singletonList(new GenericMessageAction("This is a message brought to you by the JIRA "
                + "Reference Plugin"));
    }

    /**
     * This panel will be shown to all users for all issues.
     *
     * @param issue The Issue.
     * @param remoteUser The viewing user.
     * @return true; for all issues and users.
     */
    public boolean showPanel(Issue issue, User remoteUser)
    {
        return true;
    }
}
