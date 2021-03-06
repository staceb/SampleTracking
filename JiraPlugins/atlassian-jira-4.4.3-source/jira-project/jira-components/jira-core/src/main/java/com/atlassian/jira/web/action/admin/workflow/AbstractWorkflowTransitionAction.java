package com.atlassian.jira.web.action.admin.workflow;

import com.atlassian.jira.bc.workflow.WorkflowService;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.jira.workflow.JiraWorkflow;
import com.atlassian.jira.workflow.WorkflowUtil;
import com.atlassian.plugin.PluginAccessor;
import com.opensymphony.workflow.loader.ActionDescriptor;
import com.opensymphony.workflow.loader.StepDescriptor;

public class AbstractWorkflowTransitionAction extends JiraWebActionSupport
{
    private final JiraWorkflow workflow;
    private final StepDescriptor step;
    private final ActionDescriptor transition;
    private final PluginAccessor pluginAccessor;
    protected final WorkflowService workflowService;

    public AbstractWorkflowTransitionAction(JiraWorkflow workflow, StepDescriptor step, ActionDescriptor transition,
            PluginAccessor pluginAccessor, WorkflowService workflowService)
    {
        this.step = step;
        this.workflow = workflow;
        this.transition = transition;
        this.pluginAccessor = pluginAccessor;
        this.workflowService = workflowService;
    }

    public AbstractWorkflowTransitionAction(JiraWorkflow workflow, ActionDescriptor transition,
            PluginAccessor pluginAccessor, WorkflowService workflowService)
    {
        // Used for working with global actions where step is null
        this(workflow, null, transition, pluginAccessor, workflowService);
    }

    public JiraWorkflow getWorkflow()
    {
        return workflow;
    }

    public StepDescriptor getStep()
    {
        return step;
    }

    public ActionDescriptor getTransition()
    {
        return transition;
    }

    protected PluginAccessor getPluginAccessor()
    {
        return pluginAccessor;
    }

    public String getWorkflowDisplayName()
    {
        return WorkflowUtil.getWorkflowDisplayName(workflow);
    }
}
