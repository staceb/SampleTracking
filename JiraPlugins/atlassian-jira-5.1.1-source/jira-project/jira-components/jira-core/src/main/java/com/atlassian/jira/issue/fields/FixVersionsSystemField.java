package com.atlassian.jira.issue.fields;

import com.atlassian.core.util.map.EasyMap;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueRelationConstants;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.context.IssueContext;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.rest.FixForVersionsRestFieldOperationsHandler;
import com.atlassian.jira.issue.fields.rest.RestFieldOperations;
import com.atlassian.jira.issue.fields.rest.RestFieldOperationsHandler;
import com.atlassian.jira.issue.fields.rest.json.JsonData;
import com.atlassian.jira.issue.fields.rest.json.beans.JiraBaseUrls;
import com.atlassian.jira.issue.fields.util.VersionHelperBean;
import com.atlassian.jira.issue.search.LuceneFieldSorter;
import com.atlassian.jira.issue.search.handlers.FixForVersionSearchHandlerFactory;
import com.atlassian.jira.issue.statistics.FixForVersionStatisticsMapper;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.template.VelocityTemplatingEngine;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.ErrorCollection.Reason;
import com.atlassian.jira.util.I18nHelper;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Copyright (c) 2002-2004
 * All rights reserved.
 */
public class FixVersionsSystemField extends AbstractVersionsSystemField implements FixVersionsField, RestFieldOperations
{
    private static final String FIX_VERSIONS_NAME_KEY = "issue.field.fixversions";
    private final FixForVersionStatisticsMapper fixForVersionStatisticsMapper;

    public FixVersionsSystemField(VelocityTemplatingEngine templatingEngine, ApplicationProperties applicationProperties, VersionManager versionManager,
                                  PermissionManager permissionManager, JiraAuthenticationContext authenticationContext,
                                  FixForVersionStatisticsMapper fixForVersionStatisticsMapper, VersionHelperBean versionHelperBean,
                                  FixForVersionSearchHandlerFactory searchHandlerFactory, JiraBaseUrls jiraBaseUrls)
    {
        super(IssueFieldConstants.FIX_FOR_VERSIONS, FIX_VERSIONS_NAME_KEY, templatingEngine, applicationProperties, versionManager, permissionManager, authenticationContext, versionHelperBean, searchHandlerFactory, jiraBaseUrls);
        this.fixForVersionStatisticsMapper = fixForVersionStatisticsMapper;
    }

    public boolean isShown(Issue issue)
    {
        return hasPermission(issue, Permissions.RESOLVE_ISSUE);
    }

    protected Collection<Version> getCurrentVersions(Issue issue)
    {
        return issue.getFixVersions();
    }

    protected String getArchivedVersionsFieldTitle()
    {
        return "issue.field.archived.fixversions";
    }

    protected String getArchivedVersionsFieldSearchParam()
    {
        return "fixfor";
    }

    protected boolean getUnreleasedVersionsFirst()
    {
        return true;
    }

    protected void addFieldRequiredErrorMessage(Issue issue, ErrorCollection errorCollection, I18nHelper i18n)
    {
        if (isShown(issue))
        {
            if (getPossibleVersions(issue.getProject(), false).isEmpty())
            {
                errorCollection.addErrorMessage(i18n.getText("createissue.error.versions.required", i18n.getText(getNameKey()), issue.getProject().getString("name")), Reason.VALIDATION_FAILED);
            }
            else
            {
                errorCollection.addError(getId(), i18n.getText("issue.field.required", i18n.getText(getNameKey())), Reason.VALIDATION_FAILED);
            }
        }
        else
        {
            errorCollection.addErrorMessage(i18n.getText("createissue.error.fixfors.required", i18n.getText(getNameKey()), issue.getProject().getString("name")), Reason.VALIDATION_FAILED);
        }
    }

    protected String getModifiedWithoutPermissionErrorMessage(I18nHelper i18n)
    {
        return i18n.getText("issue.field.fixversions.nopermission");
    }

    public void updateIssue(FieldLayoutItem fieldLayoutItem, MutableIssue issue, Map fieldValueHolder)
    {
        if (fieldValueHolder.containsKey(getId()))
        {
            Collection fixVersions = (Collection) getValueFromParams(fieldValueHolder);
            issue.setFixVersions(fixVersions);
        }
    }

    public void removeValueFromIssueObject(MutableIssue issue)
    {
        issue.setFixVersions(Collections.EMPTY_LIST);
    }

    public boolean canRemoveValueFromIssueObject(Issue issue)
    {
        return true;
    }

    protected String getIssueRelationName()
    {
        return IssueRelationConstants.FIX_VERSION;
    }

    protected String getChangeItemFieldName()
    {
        return "Fix Version";
    }

    /////////////////////////////////////////// NavigableField implementation //////////////////////////////////////
    public String getColumnHeadingKey()
    {
        return "issue.column.heading.fixversions";
    }

    public LuceneFieldSorter getSorter()
    {
        return fixForVersionStatisticsMapper;
    }

    protected Map addViewVelocityParams()
    {
        return EasyMap.build("linkToBrowseFixFor", Boolean.TRUE);
    }

    @Override
    public RestFieldOperationsHandler getRestFieldOperation()
    {
        return new FixForVersionsRestFieldOperationsHandler(versionManager, authenticationContext.getI18nHelper());
    }

    @Override
    public JsonData getJsonDefaultValue(IssueContext issueCtx)
    {
        return null;
    }
}
