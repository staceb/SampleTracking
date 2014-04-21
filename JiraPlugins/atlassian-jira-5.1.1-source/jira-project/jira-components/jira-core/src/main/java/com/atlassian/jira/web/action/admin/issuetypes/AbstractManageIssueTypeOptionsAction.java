package com.atlassian.jira.web.action.admin.issuetypes;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bulkedit.operation.BulkMoveOperation;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.fields.ConfigurableField;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.config.FieldConfigScheme;
import com.atlassian.jira.issue.fields.config.manager.FieldConfigSchemeManager;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.fields.option.OptionSetManager;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.search.managers.IssueSearcherManager;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.jira.web.bean.BulkEditBean;
import com.atlassian.jira.web.bean.BulkEditBeanImpl;
import com.atlassian.jira.web.bean.BulkEditBeanSessionHelper;
import com.atlassian.jira.web.bean.MultiBulkMoveBean;
import com.atlassian.query.Query;
import com.atlassian.query.order.SortOrder;
import org.apache.commons.collections.Predicate;

import java.util.Collection;

public class AbstractManageIssueTypeOptionsAction extends JiraWebActionSupport
{
    // ------------------------------------------------------------------------------------------------- Type Properties
    protected String fieldId;
    protected FieldConfigScheme configScheme;
    protected Long schemeId;

    // ---------------------------------------------------------------------------------------------------- Dependencies
    protected final FieldConfigSchemeManager configSchemeManager;
    protected final IssueTypeSchemeManager issueTypeSchemeManager;
    protected final FieldManager fieldManager;
    protected final OptionSetManager optionSetManager;
    protected final ManageableOptionType manageableOptionType;
    protected final BulkMoveOperation bulkMoveOperation;
    protected final SearchProvider searchProvider;
    protected final IssueManager issueManager;
    protected final IssueSearcherManager searcherManager;

    // ---------------------------------------------------------------------------------------------------- Constructors
    public AbstractManageIssueTypeOptionsAction(FieldConfigSchemeManager configSchemeManager,
                                                IssueTypeSchemeManager issueTypeSchemeManager,
                                                FieldManager fieldManager,
                                                OptionSetManager optionSetManager,
                                                IssueTypeManageableOption manageableOptionType,
                                                BulkMoveOperation bulkMoveOperation,
                                                SearchProvider searchProvider,
                                                IssueManager issueManager)
    {
        this.configSchemeManager = configSchemeManager;
        this.issueTypeSchemeManager = issueTypeSchemeManager;
        this.fieldManager = fieldManager;
        this.optionSetManager = optionSetManager;
        this.manageableOptionType = manageableOptionType;
        this.bulkMoveOperation = bulkMoveOperation;
        this.searchProvider = searchProvider;
        this.issueManager = issueManager;
        this.searcherManager = ComponentManager.getComponent(IssueSearcherManager.class);
    }

    // ---------------------------------------------------------------------------------------------------- View Helpers
    public ManageableOptionType getManageableOption()
    {
        return manageableOptionType;
    }

    // -------------------------------------------------------------------------------------------- Other common methods
    protected ConfigurableField getConfigurableField()
    {
        return fieldManager.getConfigurableField(getManageableOption().getFieldId());
    }

    // -------------------------------------------------------------------------------------------------- Protected Helper
    protected String getRedirect(FieldConfigScheme configScheme)
    {
        return getRedirect("ManageIssueTypeSchemes!default.jspa?actionedSchemeId=" + configScheme.getId());
    }

    // -------------------------------------------------------------------------------------- Basic accessors & mutators
    public FieldConfigScheme getConfigScheme()
    {
        if (configScheme == null && schemeId != null)
        {
            configScheme = configSchemeManager.getFieldConfigScheme(schemeId);
        }

        return configScheme;
    }

    protected void setConfigScheme(FieldConfigScheme configScheme)
    {
        this.configScheme = configScheme;
    }

    public String getFieldId()
    {
        return fieldId;
    }

    public void setFieldId(String fieldId)
    {
        this.fieldId = fieldId;
    }

    public Long getSchemeId()
    {
        return schemeId;
    }

    public void setSchemeId(Long schemeId)
    {
        this.schemeId = schemeId;
    }

    // -------------------------------------------------------------------------------------------------- Helper Methods
    protected Query getQuery(Collection<Long> projectIds, Collection<String> obseleteIssueTypeIds)
    {
        User remoteUser = getLoggedInUser();
        if (remoteUser == null)
        {
            return null;
        }
        final JqlQueryBuilder builder = JqlQueryBuilder.newBuilder();
        final JqlClauseBuilder whereBuilder = builder.where().defaultAnd();
        if (obseleteIssueTypeIds != null && !obseleteIssueTypeIds.isEmpty())
        {
            whereBuilder.issueType().inStrings(obseleteIssueTypeIds);
        }
        if (projectIds != null && !projectIds.isEmpty())
        {
            whereBuilder.project().inNumbers(projectIds);
        }
        builder.orderBy().project(SortOrder.ASC).issueType(SortOrder.ASC);
        return builder.buildQuery();
    }

    protected String migrateIssues(ExecutableAction executableAction, Collection affectedIssues, Collection targetOptionIds)
    {
        FieldConfigScheme configScheme = getConfigScheme();

        // Prepare for Update
        BulkEditBean bulkEditBean = new BulkEditBeanImpl(issueManager);
        bulkEditBean.initSelectedIssues(affectedIssues);
        bulkEditBean.initMultiBulkBean();

        MultiBulkMoveBean multiBulkMoveBean = bulkEditBean.getRelatedMultiBulkMoveBean();
        multiBulkMoveBean.initOptionIds(targetOptionIds);

        // Validate first
        multiBulkMoveBean.validate(this, bulkMoveOperation, getLoggedInUser());
        if (hasAnyErrors())
        {
            return INPUT;
        }

        multiBulkMoveBean.setExecutableAction(executableAction);
        multiBulkMoveBean.setFinalLocation("ManageIssueTypeSchemes!default.jspa?actionedSchemeId=" + configScheme.getId());

        BulkEditBeanSessionHelper.storeToSession(bulkEditBean);

        return forceRedirect("MigrateIssueTypes!default.jspa");
    }

    protected static class FieldConfigPredicate implements Predicate
    {
        private final Long id;
        private final String name;

        public FieldConfigPredicate(Long id, String name)
        {
            this.id = id;
            this.name = name;
        }

        public boolean evaluate(Object object)
        {
            FieldConfigScheme configScheme = ((FieldConfigScheme) object);
            return configScheme.getName().equalsIgnoreCase(name) && !configScheme.getId().equals(id);
        }
    }
}
