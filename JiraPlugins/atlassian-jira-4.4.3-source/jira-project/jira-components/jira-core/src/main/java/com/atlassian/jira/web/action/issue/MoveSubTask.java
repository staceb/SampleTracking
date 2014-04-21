/*
 * Copyright (c) 2002-2004
 * All rights reserved.
 */
package com.atlassian.jira.web.action.issue;

import com.atlassian.core.util.map.EasyMap;
import com.atlassian.jira.bc.issue.comment.CommentService;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFactory;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.fields.screen.FieldScreenRendererFactory;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.workflow.WorkflowManager;
import org.ofbiz.core.entity.GenericValue;
import webwork.action.ActionContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MoveSubTask extends MoveIssue
{
    private final IssueTypeSchemeManager issueTypeSchemeManager;

    public MoveSubTask(IssueLinkManager issueLinkManager, SubTaskManager subTaskManager,
                       ConstantsManager constantsManager, WorkflowManager workflowManager, FieldManager fieldManager,
                       FieldLayoutManager fieldLayoutManager, IssueFactory issueFactory,
                       FieldScreenRendererFactory fieldScreenRendererFactory, CommentService commentService, IssueTypeSchemeManager issueTypeSchemeManager)
    {
        super(issueLinkManager, subTaskManager, constantsManager, workflowManager, fieldManager, fieldLayoutManager,
                issueFactory, fieldScreenRendererFactory, commentService);
        this.issueTypeSchemeManager = issueTypeSchemeManager;
    }

    public String doDefault() throws Exception
    {
        if(getMoveIssueBean() == null)
        {
            return redirectToSessionTimeoutPage();
        }

        getMoveIssueBean().setCurrentStep(1);
        getMoveIssueBean().addAvailablePreviousStep(0);
        return INPUT;
    }

    protected void doValidation()
    {
        if(getMoveIssueBean() != null)
        {
            try
            {
                if (!isHasIssuePermission(Permissions.MOVE_ISSUE, getIssue()))
                {
                    addErrorMessage(getText("move.issue.nopermissions"));
                }

                getFieldManager().getIssueTypeField().populateFromParams(getMoveIssueBean().getFieldValuesHolder(), ActionContext.getParameters());

                // Validate as if we are creating a new issue
                Issue issueObject = getIssueObject(null);
                getFieldManager().getIssueTypeField().validateParams(getMoveIssueBean(), this, this, issueObject, null);
            }
            catch (Exception e)
            {
                log.error("Exception: " + e, e);
                addErrorMessage(getText("admin.errors.issues.an.exception.occured", e));
            }
        }
    }

    protected String doExecute() throws Exception
    {
        if(getMoveIssueBean() == null)
        {
            return redirectToSessionTimeoutPage();
        }

        getMoveIssueBean().addAvailablePreviousStep(1);
        getMoveIssueBean().setCurrentStep(2);
        return super.doExecute();
    }

    /**
     * Returns a collection of Sub-task Issue Types for this project
     * @return Sub-task Issue Types for this project
     */
    public Collection getSubTaskTypes()
    {
        return issueTypeSchemeManager.getSubTaskIssueTypesForProject(getIssueObject().getProjectObject());
    }

    // Return collection of sub-task issue types without the current sub-task issue type of the selected issue
    public Collection getOtherSubTaskTypes()
    {
        Collection subTaskTypes = getSubTaskTypes();
        List otherSubTaskTypes = new ArrayList();
        GenericValue currentSubTaskType = getSubTaskManager().getSubTaskIssueTypeById(getIssue().getString("type"));

        // This collection does not support remove
        for (Iterator iterator = subTaskTypes.iterator(); iterator.hasNext();)
        {
            GenericValue subTaskType = (GenericValue) iterator.next();
            if (!currentSubTaskType.equals(subTaskType))
                otherSubTaskTypes.add(subTaskType);
        }

        return otherSubTaskTypes;
    }

    protected Map getViewHtmlParams()
    {
        return EasyMap.build(OrderableField.MOVE_ISSUE_PARAM_KEY, Boolean.TRUE);
    }

    public MutableIssue getIssueObject(GenericValue issue)
    {
        MutableIssue issueObject = super.getIssueObject(issue);
        issueObject.setParentId(getSubTaskManager().getParentIssueId(getIssue()));
        return issueObject;
    }
}
