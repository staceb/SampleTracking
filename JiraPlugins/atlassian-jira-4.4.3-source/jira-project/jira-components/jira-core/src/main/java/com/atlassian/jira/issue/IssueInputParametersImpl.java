package com.atlassian.jira.issue;

import com.atlassian.jira.issue.customfields.view.CustomFieldParams;
import com.atlassian.jira.issue.fields.CommentSystemField;
import com.atlassian.jira.issue.fields.CommentVisibility;
import com.atlassian.jira.issue.search.constants.SystemSearchConstants;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

/**
 * @since v4.1
 */
public class IssueInputParametersImpl implements IssueInputParameters
{
    private final Map<String, String[]> actionParameters;
    private Map<String, Object> fieldValuesHolder;
    private Collection<String> providedFields;
    private boolean retainExistingValuesWhenParameterNotProvided = true;

    public IssueInputParametersImpl()
    {
        this(null);
    }

    /**
     * Can be used to quickly convert some "web-style" parameters (which can be the result of a form submit with the
     * fields rendered create/edit html) to an IssueInputParameters object.
     *
     * @param actionParameters provides the default starting values for this object.
     */
    public IssueInputParametersImpl(Map<String, String[]> actionParameters)
    {
        this.actionParameters = new HashMap<String, String[]>();
        if (actionParameters != null)
        {
            this.actionParameters.putAll(actionParameters);
        }
    }

    public IssueInputParameters setProjectId(Long projectId)
    {
        if (projectId != null)
        {
            // This field is special it uses its url parameter instead of its field name
            this.actionParameters.put(SystemSearchConstants.forProject().getUrlParameter(), new String[] {projectId.toString()});
        }
        return this;
    }

    public Long getProjectId()
    {
        return getSingleLongValueFromParameter(SystemSearchConstants.forProject().getUrlParameter());
    }

    public IssueInputParameters setIssueTypeId(String issueTypeId)
    {
        if (issueTypeId != null)
        {
            this.actionParameters.put(SystemSearchConstants.forIssueType().getFieldId(), new String[]{issueTypeId});
        }
        return this;
    }

    public String getIssueTypeId()
    {
        return getSingleValueFromParameters(SystemSearchConstants.forIssueType().getFieldId());
    }

    public IssueInputParameters setPriorityId(String priorityId)
    {
        if (priorityId != null)
        {
            this.actionParameters.put(SystemSearchConstants.forPriority().getFieldId(), new String[]{priorityId});
        }
        else
        {
            this.actionParameters.put(SystemSearchConstants.forPriority().getFieldId(), null);
        }
        return this;
    }

    public String getPriorityId()
    {
        return getSingleValueFromParameters(SystemSearchConstants.forPriority().getFieldId());
    }

    public IssueInputParameters setResolutionId(String resolutionId)
    {
        if (resolutionId != null)
        {
            this.actionParameters.put(SystemSearchConstants.forResolution().getFieldId(), new String[]{ resolutionId });
        }
        else
        {
            this.actionParameters.put(SystemSearchConstants.forResolution().getFieldId(), null);
        }
        return this;
    }

    public String getResolutionId()
    {
        return getSingleValueFromParameters(SystemSearchConstants.forResolution().getFieldId());
    }

    public IssueInputParameters setStatusId(String statusId)
    {
        if (statusId != null)
        {
            this.actionParameters.put(SystemSearchConstants.forStatus().getFieldId(), new String[]{ statusId });
        }
        else
        {
            this.actionParameters.put(SystemSearchConstants.forStatus().getFieldId(), null);
        }
        return this;
    }

    public String getStatusId()
    {
        return getSingleValueFromParameters(SystemSearchConstants.forStatus().getFieldId());
    }

    public IssueInputParameters setSummary(String summary)
    {
        if (summary != null)
        {
            this.actionParameters.put(SystemSearchConstants.forSummary().getFieldId(), new String[] {summary});
        }
        else
        {
            this.actionParameters.put(SystemSearchConstants.forSummary().getFieldId(), null);
        }
        return this;
    }

    public String getSummary()
    {
        return getSingleValueFromParameters(SystemSearchConstants.forSummary().getFieldId());
    }

    public IssueInputParameters setDescription(String description)
    {
        if (description != null)
        {
            this.actionParameters.put(SystemSearchConstants.forDescription().getFieldId(), new String[] {description});
        }
        else
        {
            this.actionParameters.put(SystemSearchConstants.forDescription().getFieldId(), null);
        }
        return this;
    }

    public String getDescription()
    {
        return getSingleValueFromParameters(SystemSearchConstants.forDescription().getFieldId());
    }

    public IssueInputParameters setEnvironment(String environment)
    {
        if (environment != null)
        {
            this.actionParameters.put(SystemSearchConstants.forEnvironment().getFieldId(), new String[] {environment});
        }
        else
        {
            this.actionParameters.put(SystemSearchConstants.forEnvironment().getFieldId(), null);
        }
        return this;
    }

    public String getEnvironment()
    {
        return getSingleValueFromParameters(SystemSearchConstants.forEnvironment().getFieldId());
    }

    public IssueInputParameters setAssigneeId(String assigneeId)
    {
        if (assigneeId != null)
        {
            this.actionParameters.put(SystemSearchConstants.forAssignee().getFieldId(), new String[] {assigneeId});
        }
        else
        {
            this.actionParameters.put(SystemSearchConstants.forAssignee().getFieldId(), null);
        }
        return this;
    }

    public String getAssigneeId()
    {
        return getSingleValueFromParameters(SystemSearchConstants.forAssignee().getFieldId());
    }

    public IssueInputParameters setReporterId(String reporterId)
    {
        if (reporterId != null)
        {
            this.actionParameters.put(SystemSearchConstants.forReporter().getFieldId(), new String[] {reporterId});
        }
        else
        {
            this.actionParameters.put(SystemSearchConstants.forReporter().getFieldId(), null);
        }
        return this;
    }

    public String getReporterId()
    {
        return getSingleValueFromParameters(SystemSearchConstants.forReporter().getFieldId());
    }

    public IssueInputParameters setComponentIds(Long... componentIds)
    {
        this.actionParameters.put(SystemSearchConstants.forComponent().getFieldId(), convertLongArrayToStringArray(componentIds));
        return this;
    }

    public Long [] getComponentIds()
    {
        return getMultipleLongValuesFromParameters(SystemSearchConstants.forComponent().getFieldId());
    }

    public IssueInputParameters setFixVersionIds(Long... fixVersionIds)
    {
        this.actionParameters.put(SystemSearchConstants.forFixForVersion().getFieldId(), convertLongArrayToStringArray(fixVersionIds));
        return this;
    }

    public Long [] getFixVersionIds()
    {
        return getMultipleLongValuesFromParameters(SystemSearchConstants.forFixForVersion().getFieldId());
    }

    public IssueInputParameters setAffectedVersionIds(Long... affectedVersionIds)
    {
        this.actionParameters.put(SystemSearchConstants.forAffectedVersion().getFieldId(), convertLongArrayToStringArray(affectedVersionIds));
        return this;
    }

    public Long [] getAffectedVersionIds()
    {
        return getMultipleLongValuesFromParameters(SystemSearchConstants.forAffectedVersion().getFieldId());
    }

    public IssueInputParameters setDueDate(String dueDate)
    {
        if (dueDate != null)
        {
            this.actionParameters.put(SystemSearchConstants.forDueDate().getFieldId(), new String [] {dueDate});
        }
        else
        {
            this.actionParameters.put(SystemSearchConstants.forDueDate().getFieldId(), null);
        }
        return this;
    }

    public String getDueDate()
    {
        return getSingleValueFromParameters(SystemSearchConstants.forDueDate().getFieldId());
    }

    public IssueInputParameters setResolutionDate(String resolutionDate)
    {
        if (resolutionDate != null)
        {
            this.actionParameters.put(SystemSearchConstants.forResolutionDate().getFieldId(), new String [] { resolutionDate });
        }
        else
        {
            this.actionParameters.put(SystemSearchConstants.forResolutionDate().getFieldId(), null);
        }
        return this;
    }

    public String getResolutionDate()
    {
        return getSingleValueFromParameters(SystemSearchConstants.forResolutionDate().getFieldId());
    }

    public IssueInputParameters setSecurityLevelId(Long securityLevelId)
    {
        if (securityLevelId != null)
        {
            this.actionParameters.put(SystemSearchConstants.forSecurityLevel().getFieldId(), new String [] {securityLevelId.toString()});
        }
        else
        {
            this.actionParameters.put(SystemSearchConstants.forSecurityLevel().getFieldId(), null);
        }
        return this;
    }

    public Long getSecurityLevelId()
    {
        return getSingleLongValueFromParameter(SystemSearchConstants.forSecurityLevel().getFieldId());

    }

    public IssueInputParameters setOriginalEstimate(Long originalEstimate)
    {
        if (originalEstimate != null)
        {
            this.actionParameters.put(SystemSearchConstants.forOriginalEstimate().getFieldId(), new String [] { originalEstimate.toString()});
        }
        else
        {
            this.actionParameters.put(SystemSearchConstants.forOriginalEstimate().getFieldId(), null);
        }
        return this;
    }

    public Long getOriginalEstimate()
    {
        return getSingleLongValueFromParameter(SystemSearchConstants.forOriginalEstimate().getFieldId());

    }

    public IssueInputParameters setTimeSpent(Long timeSpent)
    {
        if (timeSpent != null)
        {
            this.actionParameters.put(SystemSearchConstants.forTimeSpent().getFieldId(), new String [] { timeSpent.toString()});
        }
        else
        {
            this.actionParameters.put(SystemSearchConstants.forTimeSpent().getFieldId(), null);
        }
        return this;
    }

    public Long getTimeSpent()
    {
        return getSingleLongValueFromParameter(SystemSearchConstants.forTimeSpent().getFieldId());

    }

    public IssueInputParameters addCustomFieldValue(Long customFieldId, String... values)
    {
        if (customFieldId != null)
        {
            this.actionParameters.put("customfield_" + customFieldId, values);
        }
        return this;
    }

    public IssueInputParameters addCustomFieldValue(String fullCustomFieldKey, String... values)
    {
        if (fullCustomFieldKey != null)
        {
            this.actionParameters.put(fullCustomFieldKey, values);
        }
        return this;
    }

    public boolean retainExistingValuesWhenParameterNotProvided()
    {
        return retainExistingValuesWhenParameterNotProvided;
    }

    public void setRetainExistingValuesWhenParameterNotProvided(boolean retain)
    {
        this.retainExistingValuesWhenParameterNotProvided = retain;
    }

    public String[] getCustomFieldValue(Long customFieldId)
    {
        notNull("customFieldId", customFieldId);
        return this.actionParameters.get("customfield_" + customFieldId);
    }

    public String[] getCustomFieldValue(final String fullCustomFieldKey)
    {
        return this.actionParameters.get(fullCustomFieldKey);
    }

    public String getCommentValue()
    {
        return getSingleValueFromParameters(SystemSearchConstants.forComments().getFieldId());
    }

    public IssueInputParameters setComment(String comment)
    {
        if (comment != null)
        {
            this.actionParameters.put(SystemSearchConstants.forComments().getFieldId(), new String[] {comment});
        }
        return this;
    }

    public IssueInputParameters setComment(String comment, Long projectRoleId)
    {
        if (comment != null)
        {
            this.actionParameters.put(SystemSearchConstants.forComments().getFieldId(), new String[] {comment});
        }
        if (projectRoleId != null)
        {
            this.actionParameters.put(CommentSystemField.PARAM_COMMENT_LEVEL, new String[] { CommentVisibility.getCommentLevelFromLevels(null, projectRoleId)});
        }
        return this;
    }

    public IssueInputParameters setComment(String comment, String groupId)
    {
        if (comment != null)
        {
            this.actionParameters.put(SystemSearchConstants.forComments().getFieldId(), new String[] {comment});
        }
        if (groupId != null)
        {
            this.actionParameters.put(CommentSystemField.PARAM_COMMENT_LEVEL, new String[] { CommentVisibility.getCommentLevelFromLevels(groupId, null)});
        }
        return this;
    }

    public Map<String, String[]> getActionParameters()
    {
        return actionParameters;
    }

    public void setFieldValuesHolder(final Map<String, Object> fieldValuesHolder)
    {
        this.fieldValuesHolder = fieldValuesHolder;
    }

    public Map<String, Object> getFieldValuesHolder()
    {
        if (fieldValuesHolder == null)
        {
            return Collections.emptyMap();
        }
        return fieldValuesHolder;
    }

    public Collection<String> getProvidedFields()
    {
        return providedFields;
    }

    public void setProvidedFields(final Collection<String> providedFields)
    {
        this.providedFields = providedFields;
    }

    public boolean isFieldSet(String fieldId)
    {
        return this.actionParameters.containsKey(fieldId) && !isObjectEmpty(this.actionParameters.get(fieldId));
    }

    public boolean isFieldPresent(String fieldId)
    {
        return this.actionParameters.containsKey(fieldId);
    }

    private Long [] getMultipleLongValuesFromParameters(String key)
    {
        final String[] idStrs = this.actionParameters.get(key);
        if (idStrs != null)
        {
            try
            {
                final Long [] ids = new Long[idStrs.length];
                for (int i = 0; i < idStrs.length; i++)
                {
                    String idStr = idStrs[i];
                    ids[i] = new Long(idStr);
                }
                return ids;
            }
            catch (NumberFormatException e)
            {
                // Bum data, oh well
            }
        }
        return null;
    }

    private Long getSingleLongValueFromParameter(String key)
    {
        try
        {
            final String isStr = getSingleValueFromParameters(key);
            if (isStr != null)
            {
                return new Long(isStr);
            }
        }
        catch (NumberFormatException e)
        {
            // Must be a crappy value
        }
        return null;
    }

    private String getSingleValueFromParameters(String key)
    {
        final String[] strings = this.actionParameters.get(key);
        if (strings != null && strings.length == 1)
        {
            return strings[0];
        }
        return null;
    }

    private String [] convertLongArrayToStringArray(Long [] longs)
    {
        if (longs == null)
        {
            return null;
        }
        final String [] strings = new String[longs.length];
        for (int i = 0; i < longs.length; i++)
        {
            Long id = longs[i];
            strings[i] = id.toString();
        }
        return strings;
    }

    /**
     * Checks for "emptiness" of the specified value. Knows about Arrays, {@link java.util.Collection}s and
     * {@link com.atlassian.jira.issue.customfields.view.CustomFieldParams}. Null values are empty.
     *
     * @param value the value to test
     * @return true if null or "empty"; false otherwise.
     */
    boolean isObjectEmpty(final Object value)
    {
        if (value == null)
        {
            return true;
        }

        // sometimes we encounter arrays with one null element - these are deemed empty
        if (value instanceof Object[])
        {
            final Object[] objects = (Object[]) value;
            if (objects.length == 0 || (objects.length == 1 && objects[0] == null))
            {
                return true;
            }
        }

        if (value instanceof Collection)
        {
            return ((Collection) value).isEmpty();
        }

        if (value instanceof CustomFieldParams)
        {
            return ((CustomFieldParams) value).isEmpty();
        }

        return false;
    }

}
