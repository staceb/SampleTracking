package com.atlassian.jira.bc.issue.worklog;

import com.atlassian.jira.issue.Issue;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.Date;

/**
 * Implementation of the {@link WorklogInputParameters}, {@link WorklogNewEstimateInputParameters} and
 * {@link WorklogAdjustmentAmountInputParameters} interfaces. Static builder methods are provided for convenience.
 *
 * @since v4.2
 * @see com.atlassian.jira.bc.issue.worklog.WorklogInputParametersImpl.Builder
 */
public class WorklogInputParametersImpl implements WorklogInputParameters, WorklogNewEstimateInputParameters, WorklogAdjustmentAmountInputParameters
{
    private final Long worklogId;
    private final Issue issue;
    private final String timeSpent;
    private final Date startDate;
    private final String comment;
    private final String groupLevel;
    private final String roleLevelId;
    private final String newEstimate;
    private final String adjustmentAmount;
    private final boolean editableCheckRequired;
    private final String errorFieldPrefix;

    private WorklogInputParametersImpl(final Long worklogId, final Issue issue, final String timeSpent, final Date startDate,
            final String comment, final String groupLevel, final String roleLevelId,
            final String newEstimate, final String adjustmentAmount, final boolean editableCheckRequired, final String errorFieldPrefix)
    {
        this.adjustmentAmount = adjustmentAmount;
        this.comment = comment;
        this.editableCheckRequired = editableCheckRequired;
        this.groupLevel = groupLevel;
        this.issue = issue;
        this.newEstimate = newEstimate;
        this.roleLevelId = roleLevelId;
        this.startDate = startDate;
        this.timeSpent = timeSpent;
        this.worklogId = worklogId;
        this.errorFieldPrefix = errorFieldPrefix;
    }

    public Long getWorklogId()
    {
        return worklogId;
    }

    public Issue getIssue()
    {
        return issue;
    }

    public String getTimeSpent()
    {
        return timeSpent;
    }

    public Date getStartDate()
    {
        return startDate;
    }

    public String getComment()
    {
        return comment;
    }

    public String getGroupLevel()
    {
        return groupLevel;
    }

    public String getRoleLevelId()
    {
        return roleLevelId;
    }

    public String getNewEstimate()
    {
        return newEstimate;
    }

    public String getAdjustmentAmount()
    {
        return adjustmentAmount;
    }

    public boolean isEditableCheckRequired()
    {
        return editableCheckRequired;
    }

    public String getErrorFieldPrefix()
    {
        return errorFieldPrefix;
    }

    /**
     * @return a blank Builder to use for {@link WorklogInputParameters} construction
     */
    public static Builder builder()
    {
        return new Builder();
    }

    /**
     * @param issue the issue
     * @return a Builder with the Issue pre-populated
     */
    public static Builder issue(final Issue issue)
    {
        return new Builder().issue(issue);
    }

    /**
     * @param timeSpent the amount of time spent
     * @return a Builder with the time spent pre-populated
     */
    public static Builder timeSpent(final String timeSpent)
    {
        return new Builder().timeSpent(timeSpent);
    }

    /**
     * A fluent-style Builder for constructing {@link WorklogInputParameters}, {@link WorklogNewEstimateInputParameters} and
     * {@link WorklogAdjustmentAmountInputParameters} objects.
     *
     * @since v4.2
     */
    public static class Builder
    {
        private Long worklogId = null;
        private Issue issue = null;
        private String timeSpent = null;
        private Date startDate = null;
        private String comment = null;
        private String groupLevel = null;
        private String roleLevelId = null;
        private String newEstimate = null;
        private String adjustmentAmount = null;
        private boolean editableCheckRequired = true;
        private String errorFieldPrefix = null;

        public Builder worklogId(Long worklogId)
        {
            this.worklogId = worklogId;
            return this;
        }

        public Builder issue(Issue issue)
        {
            this.issue = issue;
            return this;
        }

        public Builder timeSpent(String timeSpent)
        {
            this.timeSpent = timeSpent;
            return this;
        }

        public Builder startDate(Date startDate)
        {
            this.startDate = startDate;
            return this;
        }

        public Builder comment(String comment)
        {
            this.comment = comment;
            return this;
        }

        public Builder groupLevel(String groupLevel)
        {
            this.groupLevel = groupLevel;
            return this;
        }

        public Builder roleLevelId(String roleLevelId)
        {
            this.roleLevelId = roleLevelId;
            return this;
        }

        /**
         * <p>Set the new estimate value when building a {@link WorklogNewEstimateInputParameters} object.
         * <p>Note: the value you set here will be <strong>ignored</strong> if you build a {@link WorklogInputParameters} object using
         * {@link #build()} or a {@link WorklogAdjustmentAmountInputParameters} object using {@link #buildAdjustmentAmount()}.
         *
         * @param newEstimate the new estimate value
         * @return the builder
         * @see #buildNewEstimate()
         * @see WorklogNewEstimateInputParameters
         */
        public Builder newEstimate(String newEstimate)
        {
            this.newEstimate = newEstimate;
            return this;
        }

        /**
         * <p>Set the adjustment amount value when building a {@link WorklogAdjustmentAmountInputParameters} object.
         * <p>Note: the value you set here will be <strong>ignored</strong> if you build a {@link WorklogInputParameters} object using
         * {@link #build()} or a {@link WorklogNewEstimateInputParameters} object using {@link #buildNewEstimate()}.
         *
         * @param adjustmentAmount the adjustment amount value
         * @return the builder
         * @see #buildAdjustmentAmount()
         * @see WorklogAdjustmentAmountInputParameters
         */
        public Builder adjustmentAmount(String adjustmentAmount)
        {
            this.adjustmentAmount = adjustmentAmount;
            return this;
        }

        public Builder editableCheckRequired(boolean editableCheckRequired)
        {
            this.editableCheckRequired = editableCheckRequired;
            return this;
        }

        public Builder errorFieldPrefix(String errorFieldPrefix)
        {
            this.errorFieldPrefix = errorFieldPrefix;
            return this;
        }

        /**
         * Use this method to build the base {@link WorklogInputParameters} object.
         *
         * @return a {@link WorklogInputParameters} object with the built parameters.
         */
        public WorklogInputParameters build()
        {
            return new WorklogInputParametersImpl(worklogId, issue, timeSpent, startDate, comment, groupLevel,
                    roleLevelId, null, null, editableCheckRequired, errorFieldPrefix);
        }

        /**
         * Use this method to build the {@link WorklogNewEstimateInputParameters} object required for the "new estimate"
         * service calls.
         *
         * @return a {@link WorklogNewEstimateInputParameters} object with the built parameters.
         */
        public WorklogNewEstimateInputParameters buildNewEstimate()
        {
            return new WorklogInputParametersImpl(worklogId, issue, timeSpent, startDate, comment, groupLevel,
                    roleLevelId, newEstimate, null, editableCheckRequired, errorFieldPrefix);
        }

        /**
         * Use this method to build the {@link WorklogAdjustmentAmountInputParameters} object required for the
         * "manual adjustment" (or "adjustment amount") service calls.
         *
         * @return a {@link WorklogAdjustmentAmountInputParameters} object with the built parameters.
         */
        public WorklogAdjustmentAmountInputParameters buildAdjustmentAmount()
        {
            return new WorklogInputParametersImpl(worklogId, issue, timeSpent, startDate, comment, groupLevel,
                    roleLevelId, null, adjustmentAmount, editableCheckRequired, errorFieldPrefix);
        }
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        final WorklogInputParametersImpl rhs = (WorklogInputParametersImpl) o;

        return new EqualsBuilder()
                .append(worklogId, rhs.worklogId)
                .append(issue, rhs.issue)
                .append(timeSpent, rhs.timeSpent)
                .append(startDate, rhs.startDate)
                .append(comment, rhs.comment)
                .append(groupLevel, rhs.groupLevel)
                .append(roleLevelId, rhs.roleLevelId)
                .append(newEstimate, rhs.newEstimate)
                .append(adjustmentAmount, rhs.adjustmentAmount)
                .append(editableCheckRequired, rhs.editableCheckRequired)
                .append(errorFieldPrefix, rhs.errorFieldPrefix)
                .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(11, 29)
                .append(worklogId)
                .append(issue)
                .append(timeSpent)
                .append(startDate)
                .append(comment)
                .append(groupLevel)
                .append(roleLevelId)
                .append(newEstimate)
                .append(adjustmentAmount)
                .append(editableCheckRequired)
                .append(errorFieldPrefix)
                .toHashCode();
    }
}
