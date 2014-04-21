/*
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: Mar 23, 2004
 * Time: 4:02:21 PM
 */
package com.atlassian.jira.web.action.admin.workflow;

import com.atlassian.jira.bc.workflow.WorkflowService;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.jira.workflow.JiraWorkflow;
import com.atlassian.jira.workflow.WorkflowUtil;
import com.atlassian.sal.api.websudo.WebSudoRequired;
import com.opensymphony.util.TextUtils;

@WebSudoRequired
public class CloneWorkflow extends JiraWebActionSupport
{
    private final JiraWorkflow workflow;
    private final WorkflowService workflowService;

    private String description;
    private String newWorkflowName;

    public CloneWorkflow(JiraWorkflow workflow, WorkflowService workflowService)
    {
        this.workflow = workflow;
        this.workflowService = workflowService;
    }

    public String doDefault() throws Exception
    {
        newWorkflowName = WorkflowUtil.cloneWorkflowName(getWorkflow().getName());
        if (TextUtils.stringSet(getWorkflow().getDescription()))
        {
            setDescription(getWorkflow().getDescription());
        }

        return super.doDefault();
    }

    protected void doValidation()
    {
        workflowService.validateCopyWorkflow(getJiraServiceContext(), newWorkflowName);

        super.doValidation();
    }

    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
        workflowService.copyWorkflow(getJiraServiceContext(), newWorkflowName, getDescription(), getWorkflow());
        return getRedirect("ListWorkflows.jspa");
    }

    public String getNewWorkflowName()
    {
        return newWorkflowName;
    }

    public void setNewWorkflowName(String newWorkflowName)
    {
        this.newWorkflowName = newWorkflowName;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public JiraWorkflow getWorkflow()
    {
        return workflow;
    }
}