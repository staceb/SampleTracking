package com.atlassian.jira.web.action.admin.user;

import com.atlassian.core.user.GroupUtils;
import com.atlassian.core.util.collection.EasyList;
import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.bc.group.GroupService;
import com.atlassian.jira.bc.issue.worklog.TimeTrackingConfiguration;
import com.atlassian.jira.exception.DataAccessException;
import com.atlassian.jira.issue.AttachmentManager;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.issue.subscription.SubscriptionManager;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.user.UserIssueHistoryManager;
import com.atlassian.jira.web.action.IssueActionSupport;
import com.atlassian.sal.api.websudo.WebSudoRequired;
import com.opensymphony.user.Group;
import com.opensymphony.user.User;
import com.opensymphony.user.UserManager;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@WebSudoRequired
public class DeleteGroup extends IssueActionSupport
{
    String name;
    String swapGroup;
    boolean confirm;

    private final SubscriptionManager subscriptionManager;
    private final SearchRequestService searchRequestService;
    private final GroupService groupService;

    public DeleteGroup(IssueManager issueManager, CustomFieldManager customFieldManager, AttachmentManager attachmentManager,
                       ProjectManager projectManager, PermissionManager permissionManager, VersionManager versionManager,
                       SubscriptionManager subscriptionManager, SearchRequestService searchRequestService, GroupService groupService,
                       UserIssueHistoryManager userHistoryManager, TimeTrackingConfiguration timeTrackingConfiguration)
    {
        super(issueManager, customFieldManager, attachmentManager, projectManager, permissionManager, versionManager, userHistoryManager, timeTrackingConfiguration);
        this.subscriptionManager = subscriptionManager;
        this.searchRequestService = searchRequestService;
        this.groupService = groupService;
    }

    public String doDefault() throws Exception
    {
        groupService.isAdminDeletingSysAdminGroup(getJiraServiceContext(), name);
        groupService.areOnlyGroupsGrantingUserAdminPermissions(getJiraServiceContext(), EasyList.build(name));
        return super.doDefault();
    }

    protected void doValidation()
    {
        groupService.validateDelete(getJiraServiceContext(), name, swapGroup);
    }

    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
        if (confirm)
        {
            groupService.delete(getJiraServiceContext(), name, swapGroup);
        }

        if (getHasErrorMessages())
        {
            return ERROR;
        }
        else
        {
            return getRedirect("GroupBrowser.jspa");
        }
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public boolean isConfirm()
    {
        return confirm;
    }

    public void setConfirm(boolean confirm)
    {
        this.confirm = confirm;
    }

    public String getSwapGroup()
    {
        return swapGroup;
    }

    public void setSwapGroup(String swapGroup)
    {
        this.swapGroup = swapGroup;
    }

    public long getMatchingCommentsAndWorklogsCount() throws GenericEntityException
    {
        return groupService.getCommentsAndWorklogsGuardedByGroupCount(name);
    }

    /**
     * @return all other groups except this one
     */
    public Collection getOtherGroups()
    {
        List otherGroups = new ArrayList();

        try
        {
            Collection groups = GroupUtils.getGroups();

            for (Iterator iterator = groups.iterator(); iterator.hasNext();)
            {
                Group group = (Group) iterator.next();

                if (!group.getName().equals(name))
                {
                    otherGroups.add(group.getName());
                }
            }
        }
        catch (Exception e)
        {
            addErrorMessage(getText("admin.errors.groups.error.occured.getting.other.groups"));
        }

        return otherGroups;
    }


    public boolean hasSubscriptions()
    {
        try
        {
            List subList = subscriptionManager.getAllSubscriptions();
            for (Iterator iterator = subList.iterator(); iterator.hasNext();)
            {
                GenericValue gv = (GenericValue) iterator.next();
                if (name.equals(gv.getString("group")))
                {
                    return true;
                }
            }
        }
        catch (DataAccessException e)
        {
            log.error(e, e);
        }
        return false;
    }

    public Collection getSubscriptions()
    {
        try
        {
            final List subList = subscriptionManager.getAllSubscriptions();
            final Collection subscriptions = new ArrayList();
            for (Iterator iterator = subList.iterator(); iterator.hasNext();)
            {
                GenericValue gv = (GenericValue) iterator.next();
                if (name.equals(gv.getString("group")))
                {
                    final String userName = gv.getString("username");
                    final Long filterID = gv.getLong("filterID");

                    final User user = UserManager.getInstance().getUser(userName);
                    final JiraServiceContext ctx = new JiraServiceContextImpl(user);
                    final SearchRequest request = searchRequestService.getFilter(ctx, filterID);

                    if (request != null)
                    {
                        final String filterName = request.getName();
                        final String text = getText("admin.deletegroup.subscriptions.item", filterName, userName);
                        subscriptions.add(text);
                    }
                }
            }
            return subscriptions;
        }
        catch (Exception e)
        {
            log.error(e, e);
            return Collections.EMPTY_LIST;
        }
    }
}
