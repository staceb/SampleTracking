package com.atlassian.jira.issue.fields;

import com.atlassian.jira.bc.issue.issuelink.IssueLinkService;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.ModifiedValue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.customfields.OperationContext;
import com.atlassian.jira.issue.customfields.impl.FieldValidationException;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.screen.FieldScreenRenderLayoutItem;
import com.atlassian.jira.issue.fields.util.MessagedResult;
import com.atlassian.jira.issue.issuelink.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkDisplayHelper;
import com.atlassian.jira.issue.search.LuceneFieldSorter;
import com.atlassian.jira.issue.search.parameters.lucene.sort.MappedSortComparator;
import com.atlassian.jira.issue.util.IssueChangeHolder;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.UserHistoryManager;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.web.bean.BulkEditBean;
import com.atlassian.velocity.VelocityManager;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.lucene.search.SortComparatorSource;
import webwork.action.Action;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class IssueLinksSystemField extends AbstractOrderableField
        implements HideableField, RequirableField, NavigableField
{
    private static final String NAME_KEY = "issue.field.issuelinks";

    private static final String ISCREATEISSUE = "isCreateIssue";
    private final IssueLinkService issueLinkService;
    private final UserHistoryManager userHistoryManager;

    public IssueLinksSystemField(
            final VelocityManager velocityManager,
            final ApplicationProperties applicationProperties,
            final JiraAuthenticationContext authenticationContext,
            final PermissionManager permissionManager,
            final IssueLinkService issueLinkService,
            final UserHistoryManager userHistoryManager)
    {
        super(IssueFieldConstants.ISSUE_LINKS, NAME_KEY, velocityManager, applicationProperties, authenticationContext, permissionManager, null);
        this.issueLinkService = issueLinkService;
        this.userHistoryManager = userHistoryManager;
    }

    public LuceneFieldSorter getSorter()
    {
        return null;
    }

    public String getColumnViewHtml(FieldLayoutItem fieldLayoutItem, Map displayParams, Issue issue)
    {
        Map<String, Object> velocityParams = getVelocityParams(fieldLayoutItem, null, issue, displayParams);
        final IssueLinkService.IssueLinkResult result = issueLinkService.getIssueLinks(authenticationContext.getLoggedInUser(), issue);
        List<IssueLink> allIssues = Lists.newArrayList(result.getIssueLinks().getAllIssues());
        Collection<Issue> allLinkedIssues = Lists.transform(allIssues, new Function<IssueLink, Issue>()
        {
            @Override
            public Issue apply(@Nullable IssueLink from)
            {
                return from.getDestinationIssue();
            }
        });
        velocityParams.put("linkedIssues", Lists.newArrayList(allLinkedIssues));
        velocityParams.put("applicationProperties", getApplicationProperties());
        return renderTemplate("issuelinks-columnview.vm", velocityParams);
    }

    public String getCreateHtml(FieldLayoutItem fieldLayoutItem, OperationContext operationContext, Action action, Issue issue, Map displayParameters)
    {
        return getCreateOrEditHtml(fieldLayoutItem, operationContext, action, issue, displayParameters, Boolean.TRUE);
    }

    public String getEditHtml(FieldLayoutItem fieldLayoutItem, OperationContext operationContext, Action action, Issue issue, Map displayParameters)
    {
        return getCreateOrEditHtml(fieldLayoutItem, operationContext, action, issue, displayParameters, Boolean.FALSE);
    }

    private String getCreateOrEditHtml(FieldLayoutItem fieldLayoutItem, OperationContext operationContext, Action action, Issue issue, Map displayParameters, final Boolean create)
    {
        IssueLinkDisplayHelper issueLinkDisplayHelper = new IssueLinkDisplayHelper(userHistoryManager, authenticationContext.getLoggedInUser());

        Map<String, Object> velocityParams = getVelocityParams(fieldLayoutItem, action, issue, displayParameters);
        velocityParams.put(ISCREATEISSUE, create);
        velocityParams.put("value", operationContext.getFieldValuesHolder().get(getId()));
        velocityParams.put("linkTypes", issueLinkDisplayHelper.getSortedIssueLinkTypes(issueLinkService.getIssueLinkTypes()));
        velocityParams.put("selectedLinkType", issueLinkDisplayHelper.getLastUsedLinkType());

        return renderTemplate("issuelinks-edit.vm", velocityParams);
    }

    public String getViewHtml(FieldLayoutItem fieldLayoutItem, Action action, Issue issue, Map displayParameters)
    {
        Map<String, Object> velocityParams = getVelocityParams(fieldLayoutItem, action, issue, displayParameters);
        populateFromIssue(velocityParams, issue);
        return getViewVelocityTemplate(velocityParams);
    }

    public String getViewHtml(FieldLayoutItem fieldLayoutItem, Action action, Issue issue, Object value, Map displayParameters)
    {
        Map<String, Object> velocityParams = getVelocityParams(fieldLayoutItem, action, null, displayParameters);
        velocityParams.put(getId(), value);
        return getViewVelocityTemplate(velocityParams);
    }

    private String getViewVelocityTemplate(Map velocityParams)
    {
        return renderTemplate("issuelinking-view.vm", velocityParams);
    }

    @Override
    public String getBulkEditHtml(final OperationContext operationContext, final Action action, final BulkEditBean bulkEditBean, final Map displayParameters)
    {
        return "TODO";
    }

    public void createValue(Issue issue, Object value)
    {
        if (isIssueLinkingEnabled())
        {
            IssueLinkingValue issueLinkingValue = (IssueLinkingValue) value;
            IssueLinkService.AddIssueLinkValidationResult validationResult = issueLinkingValue.getValidationResult();
            if (validationResult != null && validationResult.isValid())
            {
                issueLinkService.addIssueLinks(authenticationContext.getLoggedInUser(), validationResult);
            }
        }
    }


    public Object getDefaultValue(Issue issue)
    {
        return null;
    }

    /**
     * We don't return any default for the field.
     *
     * @param fieldValuesHolder The fieldValuesHolder Map to be populated.
     * @param issue The issue in play.
     */
    public void populateDefaults(Map fieldValuesHolder, Issue issue)
    {
        //noinspection unchecked
        fieldValuesHolder.put(getId(), new IssueLinkingValue.Builder().setIssueLinkingEnabled(isIssueLinkingEnabled()).build());
    }


    /**
     * This is called by Jelly code to map a value into a field values holder.
     *
     * @param fieldValuesHolder Map of field Values.
     * @param stringValue user friendly string value.
     * @param issue the issue in play.
     */
    public void populateParamsFromString(Map fieldValuesHolder, String stringValue, Issue issue)
            throws FieldValidationException
    {
    }


    public void populateForMove(Map fieldValuesHolder, Issue originalIssue, Issue targetIssue)
    {
        // If the field need to be moved then it does not have a current value, so populate the default value
        // which is null in our case
    }

    /**
     * This will populate the field values holder map with the time tracking value contained within the map of
     * (typically but not always web) parameters.
     * <p/>
     * We override this so we can see this happen.  This helps for debugging reasons.  Damn you class hierarchies, you
     * are confusing me!
     * <p/>
     * This will end up calling {@link #getRelevantParams(java.util.Map)} by the way.
     *
     * @param fieldValuesHolder the writable map of field values in play.
     * @param inputParameters the input parameters.
     */
    public void populateFromParams(final Map fieldValuesHolder, final Map inputParameters)
    {
        super.populateFromParams(fieldValuesHolder, inputParameters);
    }

    /**
     * This is implemented in response to use being an AbstractOrderableField.  It is actually called via
     * populateFromParams so that we can place our relevant value object into the field values holder map.  See above
     * for the code entry point.
     *
     * @param inputParameters the input parameters.
     * @return the object to be placed into a field values holder map under our id.
     */
    protected Object getRelevantParams(Map inputParameters)
    {
        IssueLinkingValue.Builder builder = new IssueLinkingValue.Builder();
        builder.setIssueLinkingEnabled(isIssueLinkingEnabled());
        builder.setCreateIssue(asArray(inputParameters, ISCREATEISSUE));
        builder.setLinkDescription(asArray(inputParameters, "issuelinks-linktype"));
        builder.setLinkedIssues(asArray(inputParameters, "issuelinks-issues"));
        return builder.build();
    }

    /**
     * This is called to populate the field values holder with the current state of the Issue object.  For example this
     * will be called when the issue is edited.
     *
     * @param fieldValuesHolder The fieldValuesHolder Map to be populated.
     * @param issue The issue in play.
     */
    public void populateFromIssue(Map fieldValuesHolder, Issue issue)
    {
        IssueLinkingValue.Builder valueBuilder = new IssueLinkingValue.Builder();
        valueBuilder.setIssueLinkingEnabled(isIssueLinkingEnabled());
        //noinspection unchecked
        fieldValuesHolder.put(getId(), valueBuilder.build());
    }

    private String[] asArray(final Map inputParameters, final String key)
    {
        return (String[]) inputParameters.get(key);
    }

    public void validateParams(OperationContext operationContext, ErrorCollection errorCollection, I18nHelper i18n, Issue issue, FieldScreenRenderLayoutItem fieldScreenRenderLayoutItem)
    {
        IssueLinkingValue value = (IssueLinkingValue) operationContext.getFieldValuesHolder().get(getId());

        if (isIssueLinkingEnabled() && value != null && !value.getLinkedIssues().isEmpty())
        {
            IssueLinkService.AddIssueLinkValidationResult issueLinkValidationResult = issueLinkService.validateAddIssueLinks(authenticationContext.getLoggedInUser(), issue, value.getLinkDescription(), value.getLinkedIssues());

            // if we were returned an updated value, that signifies that we must update the TimeTrackingValue in the FieldValuesHolder
            if (issueLinkValidationResult.isValid())
            {
                //noinspection unchecked
                operationContext.getFieldValuesHolder().put(getId(), new IssueLinkingValue.Builder(issueLinkValidationResult).build());
            }
            else
            {
                transferErrorMessages(errorCollection, issueLinkValidationResult.getErrorCollection().getErrorMessages());
                transferErrorMessages(errorCollection, issueLinkValidationResult.getErrorCollection().getErrors().values());
            }
        }
    }

    private void transferErrorMessages(ErrorCollection errorCollection, Collection<String> errorMessages)
    {
        for (String errMsg : errorMessages)
        {
            errorCollection.addError(getId(), errMsg);
        }
    }


    /**
     * This is called from BulkEdit/BulkWorkflowTransition to get an value object from a input set of values.
     *
     * @param fieldValueHolder the map of parameters.
     * @return a parsed long or null if not in the input.
     */
    public Object getValueFromParams(Map fieldValueHolder)
    {
        return fieldValueHolder.get(getId());
    }

    /**
     * <p> This is called to back update the MutableIssue with the value object we previously stuffed into the field
     * values holder. <p/> <p>This is called prior to the issue being stored on disk.</p>
     *
     * @param fieldLayoutItem FieldLayoutItem in play.
     * @param issue MutableIssue in play.
     * @param fieldValueHolder Field Value Holder Map.
     */
    public void updateIssue(FieldLayoutItem fieldLayoutItem, MutableIssue issue, Map fieldValueHolder)
    {
        IssueLinkingValue newValue = (IssueLinkingValue) getValueFromParams(fieldValueHolder);
        if (newValue == null)
        {
            return; // belts and braces.  We don't ever expect this
        }
        if (isIssueLinkingEnabled())
        {
            issue.setExternalFieldValue(getId(), null, newValue);
        }
    }


    /**
     * This is called after the issue has been stored on disk and allows us a chance to create change records for the
     * update.
     *
     * @param fieldLayoutItem for this field within this context.
     * @param issue Issue this field is part of.
     * @param modifiedValue new value to set field to. Cannot be null.
     * @param issueChangeHolder an object to record any changes made to the issue by this method.
     */
    public void updateValue(FieldLayoutItem fieldLayoutItem, Issue issue, ModifiedValue modifiedValue, IssueChangeHolder issueChangeHolder)
    {
        Object newValue = modifiedValue.getNewValue();
        if (newValue != null)
        {
            if (isIssueLinkingEnabled())
            {
                IssueLinkingValue issueLinkingValue = (IssueLinkingValue) newValue;
                IssueLinkService.AddIssueLinkValidationResult validationResult = issueLinkingValue.getValidationResult();
                if (validationResult != null && validationResult.isValid())
                {
                    issueLinkService.addIssueLinks(authenticationContext.getLoggedInUser(), validationResult);
                }
            }
        }
    }


    public MessagedResult needsMove(Collection originalIssues, Issue targetIssue, FieldLayoutItem targetFieldLayoutItem)
    {
        return new MessagedResult(false);
    }

    public void removeValueFromIssueObject(MutableIssue issue)
    {
    }

    public boolean isShown(Issue issue)
    {
        return isIssueLinkingEnabled();
    }

    public boolean canRemoveValueFromIssueObject(Issue issue)
    {
        return true;
    }

    public boolean hasValue(Issue issue)
    {
        return false;
    }

    public String availableForBulkEdit(BulkEditBean bulkEditBean)
    {
        return "bulk.edit.unavailable";
    }


    /* ===========================
     * Simple implemenation methods of NavigableField since we are no longer derived from NavigableFieldImpl
     * =========================== */

    @Override
    public String getColumnHeadingKey()
    {
        return "issue.column.heading.issuelinks";
    }

    @Override
    public String getColumnCssClass()
    {
        return getId();
    }

    @Override
    public String getDefaultSortOrder()
    {
        return null;
    }

    @Override
    public SortComparatorSource getSortComparatorSource()
    {
        final LuceneFieldSorter sorter = getSorter();
        if (sorter == null)
        {
            return null;
        }
        else
        {
            return new MappedSortComparator(sorter);
        }
    }

    @Override
    public String getHiddenFieldId()
    {
        return null;
    }

    @Override
    public String prettyPrintChangeHistory(final String changeHistory)
    {
        return changeHistory;
    }

    @Override
    public String prettyPrintChangeHistory(final String changeHistory, final I18nHelper i18nHelper)
    {
        return changeHistory;
    }

    private boolean isIssueLinkingEnabled()
    {
        return getApplicationProperties().getOption(APKeys.JIRA_OPTION_ISSUELINKING);
    }


    /**
     * <p>This interface is used as a value object for IssueLinking information.<p/> <p> It lurks around inside the
     * field values holder maps while JIRA does its thang.  It's referenced by the velocity views and also by the
     * IssueLinksSystemField itself. <p/> <p> While the class is PUBLIC, it is only so that the Velocity template can
     * get to it.  Please do not consider this part of the JIRA API.  It's for the IssueLinksSystemField only.  You have
     * been warned :) <p/>
     */
    public static interface IssueLinkingValue
    {
        boolean isCreateIssue();

        boolean isIssueLinkingActivated();

        public String getLinkDescription();

        public List<String> getLinkedIssues();

        public IssueLinkService.AddIssueLinkValidationResult getValidationResult();

        public static class Builder
        {
            private String linkDesc = null;
            private List<String> linkedIssues;
            private boolean isIssueLinkingEnabled;
            private boolean isCreateIssue = false;
            private IssueLinkService.AddIssueLinkValidationResult validationResult;

            Builder()
            {
            }

            Builder(IssueLinkService.AddIssueLinkValidationResult validationResult)
            {
                this.validationResult = validationResult;
            }

            Builder setCreateIssue(String[] createIssue)
            {
                final String s = fromArray(createIssue);
                this.isCreateIssue = StringUtils.isNotBlank(s) ? Boolean.valueOf(s) : false;
                return this;
            }

            Builder setIssueLinkingEnabled(boolean enabled)
            {
                this.isIssueLinkingEnabled = enabled;
                return this;
            }

            Builder setLinkDescription(String[] value)
            {
                linkDesc = fromArray(value);
                return this;
            }


            Builder setLinkedIssues(String[] value)
            {
                if (value != null)
                {
                    linkedIssues = new ArrayList<String>();
                    linkedIssues.addAll(Arrays.asList(value));
                }
                return this;
            }


            private String fromArray(final String[] value)
            {
                return value != null && value.length > 0 ? value[0] : null;
            }

            IssueLinkingValue build()
            {
                final boolean isCreateIssue = this.isCreateIssue;
                final boolean isIssueLinkingEnabled = this.isIssueLinkingEnabled;
                final String linkDesc = this.linkDesc;
                final List<String> linkedIssues = this.linkedIssues == null ? Collections.<String>emptyList() : this.linkedIssues;

                return new IssueLinkingValue()
                {
                    public boolean isCreateIssue()
                    {
                        return isCreateIssue;
                    }

                    public boolean isIssueLinkingActivated()
                    {
                        return isIssueLinkingEnabled;
                    }

                    public String getLinkDescription()
                    {
                        return linkDesc;
                    }

                    public List<String> getLinkedIssues()
                    {
                        return linkedIssues;
                    }

                    @Override
                    public IssueLinkService.AddIssueLinkValidationResult getValidationResult()
                    {
                        return validationResult;
                    }

                    @Override
                    public boolean equals(final Object obj)
                    {
                        if (this == obj)
                        {
                            return true;
                        }

                        if (!(obj instanceof IssueLinkingValue))
                        {
                            return false;
                        }

                        IssueLinkingValue rhs = (IssueLinkingValue) obj;

                        return new EqualsBuilder().
                                append(isCreateIssue, rhs.isCreateIssue()).
                                append(isIssueLinkingEnabled, rhs.isIssueLinkingActivated()).
                                append(linkDesc, rhs.getLinkDescription()).
                                append(linkedIssues, rhs.getLinkDescription()).
                                isEquals();
                    }

                    @Override
                    public int hashCode()
                    {
                        return new HashCodeBuilder(17, 31).
                                append(isIssueLinkingEnabled).
                                append(isCreateIssue).
                                append(linkDesc).
                                append(linkedIssues).
                                toHashCode();
                    }

                    @Override
                    public String toString()
                    {
                        return new ToStringBuilder(this).
                                append("isIssueLinkingEnabled", isIssueLinkingEnabled).
                                append("isCreateIssue", isCreateIssue).
                                append("linkDesc", linkDesc).
                                append("linkedIssues", linkedIssues).
                                toString();
                    }
                };
            }
        }

    }

}
