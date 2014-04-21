package com.atlassian.jira.bc.issue.util;

import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.issue.Issue;

/**
 * This class is used to check the validity of group or role level visibility restrictions. Examples of where this is
 * used are: Comment visibility and Worklog visibility restrictions.
 * <p/>
 * Use this class to determine if group or project role restrictions are enabled, and to validate that input parameters
 * that contain group or project role information would constitute a valid restriction.
 */
public interface VisibilityValidator
{
    /**
     * This will validate that the passed in group level and project role level id will constitute a valid visibility
     * restriction. You can not specify both a group and role level. The group or role must exist in JIRA and the
     * user performing this operation must be a member of the group or role. All errors will be added in 
     * human-readable form via the I18N key prefixed with the passed in prefix.
     *
     * @param jiraServiceContext containing the user who wishes to apply the restriction and the errorCollection
     * @param i18nPrefix the prefix for the I18N messages that will be added to the error collection.
     * @param issue The {@link Issue} you wish to associate the element that will be restricted with. This can not be null.
     * @param groupLevel The group level visibility of the comment (null if roleLevelId specified)
     * @param roleLevelId The role level visibility id of the comment (null if groupLevel specified)
     * @return true if the visibility data is valid, false otherwise.
     */
    boolean isValidVisibilityData(JiraServiceContext jiraServiceContext, String i18nPrefix, Issue issue, String groupLevel, String roleLevelId);

    /**
     * Determines if group visibility restrictions are currently enabled in JIRA.
     *
     * @return true if enabled, false otherwise.
     */
    boolean isGroupVisiblityEnabled();

    /**
     * Determines if project role visibility restrictions are currently enabled in JIRA.
     *
     * @return true if enabled, false otherwise.
     */
    boolean isProjectRoleVisiblityEnabled();
}
