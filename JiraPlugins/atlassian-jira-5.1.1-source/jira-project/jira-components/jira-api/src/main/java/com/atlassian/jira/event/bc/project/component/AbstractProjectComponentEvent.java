package com.atlassian.jira.event.bc.project.component;

import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.project.AssigneeTypes;

/**
 * Abstract event that captures the data relevant to project component events
 *
 * @since v5.1
 */
public class AbstractProjectComponentEvent
{
    private Long id;
    private Long assigneeType;

    public AbstractProjectComponentEvent(ProjectComponent projectComponent)
    {
        this.id = projectComponent.getId();
        this.assigneeType = projectComponent.getAssigneeType();
    }

    public Long getId()
    {
        return id;
    }

    public Long getAssigneeType()
    {
        return assigneeType;
    }

    public boolean isDefaultAssigneeSetToProjectDefault()
    {
        return AssigneeTypes.PROJECT_DEFAULT == assigneeType;
    }

    public boolean isDefaultAssigneeSetToComponentLead()
    {
        return AssigneeTypes.COMPONENT_LEAD == assigneeType;
    }

    public boolean isDefaultAssigneeSetToProjectLead()
    {
        return AssigneeTypes.PROJECT_LEAD == assigneeType;
    }

    public boolean isDefaultAssigneeSetToUnassigned()
    {
        return AssigneeTypes.UNASSIGNED == assigneeType;
    }
}
