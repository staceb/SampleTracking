package com.atlassian.jira.issue.tabpanels;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.action.IssueActionComparator;
import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueTabPanel;
import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.util.collect.CollectionBuilder;
import com.atlassian.jira.vcs.Repository;
import com.atlassian.jira.vcs.RepositoryManager;
import com.atlassian.jira.vcs.cvsimpl.CVSCommit;
import com.opensymphony.user.User;
import org.apache.log4j.Logger;
import org.ofbiz.core.entity.GenericEntityException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CVSTabPanel extends AbstractIssueTabPanel
{
    private static final Logger log = Logger.getLogger(CVSTabPanel.class);
    private RepositoryManager repositoryManager;
    private PermissionManager permissionManager;

    public CVSTabPanel(RepositoryManager repositoryManager, PermissionManager permissionManager)
    {
        this.repositoryManager = repositoryManager;
        this.permissionManager = permissionManager;
    }

    /**
     * Retrieves all of the commits for this issue from ALL of the repositories associated with the issue's project
     *
     * @param issue the issue to find the comments for.
     * @param remoteUser the user asking for the commits.
     */
    public List<IssueAction> getActions(Issue issue, User remoteUser)
    {
        if (issue == null)
            throw new IllegalArgumentException("Issue cannot be null.");

        List<IssueAction> commitActions = new ArrayList<IssueAction>();

        Map<Long, Set<CVSCommit>> repositoryCommits = repositoryManager.getCommits(issue, remoteUser);
        for (Map.Entry<Long, Set<CVSCommit>> entry : repositoryCommits.entrySet())
        {
            Long repositoryId = entry.getKey();
            Set<CVSCommit> coms = entry.getValue();
            Repository repository;
            try
            {
                repository = repositoryManager.getRepository(repositoryId);
                if (coms == null)
                {
                    commitActions.add(new GenericMessageAction(descriptor.getI18nBean().getText("admin.cvsmodules.no.index.error.message", repository.getName())));
                }
                else
                {
                    for (CVSCommit cvsCommit : coms)
                    {
                        commitActions.add(new CVSAction(descriptor, cvsCommit));
                    }
                }
            }
            catch (GenericEntityException e)
            {
                log.error("Error retrieving project repository with id: " + repositoryId, e);
            }
        }

        // This is a bit of a hack to indicate that there are no commits to display
        if (commitActions.isEmpty())
        {
            GenericMessageAction action = new GenericMessageAction(descriptor.getI18nBean().getText("viewissue.nocommits"));
            return CollectionBuilder.<IssueAction>newBuilder(action).asMutableList();
        }

        // Sort by date
        Collections.sort(commitActions, IssueActionComparator.COMPARATOR);
        return commitActions;
    }

    public boolean showPanel(Issue issue, User remoteUser)
    {
        // Check that the user can see the Version Control Page AND
        // Check that the project has at least one associated repository
        // Fixes: JRA-3404
        try
        {
            return hasPermission(issue, remoteUser) &&
                   !repositoryManager.getRepositoriesForProject(issue.getProject()).isEmpty();
        }
        catch (GenericEntityException e)
        {
            log.error("Error occurred while retrieving information from the datastore.", e);
            return false;
        }
    }

    private boolean hasPermission(Issue issue, User remoteUser)
    {
        return permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, issue, remoteUser);
    }
}
