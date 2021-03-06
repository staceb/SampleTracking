package com.atlassian.jira.web.action.issue;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.util.ErrorCollection;
import com.opensymphony.util.TextUtils;

public class DeleteIssue extends AbstractViewIssue
{
    boolean confirm;
    private Integer numberOfSubTasks;
    private final IssueService issueService;
    private IssueService.DeleteValidationResult issueValidationResult;

    public DeleteIssue(IssueLinkManager issueLinkManager, SubTaskManager subTaskManager, IssueService issueService)
    {
        super(issueLinkManager, subTaskManager);
        this.issueService = issueService;
    }

    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
        if (confirm)
        {
            final ErrorCollection errors = issueService.delete(getRemoteUser(), issueValidationResult);
            if (errors.hasAnyErrors())
            {
                addErrorCollection(errors);
            }
        }
        
        return returnComplete(getViewUrl());
    }

    private boolean isCurrentDeletedIssue(final String returnUrl)
    {
        final String key = getIssueObject().getKey();
        return returnUrl.endsWith("browse/" + key) || returnUrl.contains("browse/" + key + "?");
    }

    public String getViewUrl()
    {

        final String returnUrl = getReturnUrl();
        if(TextUtils.stringSet(returnUrl) && ! isCurrentDeletedIssue(returnUrl))
        {
            return returnUrl;
        }
        else if (TextUtils.stringSet(getViewIssueKey()))
        {
            return "/browse/" + getViewIssueKey();
        }
        else
        {
            return "/secure/IssueNavigator.jspa";
        }
    }


    public String getTargetUrl()
    {
        return getViewUrl();
    }


    protected void doValidation()
    {
        issueValidationResult = issueService.validateDelete(getRemoteUser(), getIssueObject().getId());
        if (!issueValidationResult.isValid())
        {
            addErrorCollection(issueValidationResult.getErrorCollection());
        }
    }

    public String doDefault() throws Exception
    {
        final IssueService.IssueResult issueResult = issueService.getIssue(getRemoteUser(), getId());
        this.addErrorCollection(issueResult.getErrorCollection());
        return INPUT;
     }

    public boolean isConfirm()
    {
        return confirm;
    }

    public void setConfirm(boolean confirm)
    {
        this.confirm = confirm;
    }

    public int getNumberOfSubTasks()
    {
        if (numberOfSubTasks == null)
        {
            numberOfSubTasks = new Integer(getSubTaskManager().getSubTaskIssueLinks(getIssue().getLong("id")).size());
        }

        return numberOfSubTasks.intValue();
    }
}
