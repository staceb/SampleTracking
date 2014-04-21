package com.atlassian.jira.issue.fields;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.context.IssueContext;
import com.atlassian.jira.issue.context.JiraContextNode;
import com.atlassian.jira.issue.customfields.CustomFieldSearcher;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.customfields.view.CustomFieldParams;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.config.FieldConfigScheme;
import com.atlassian.jira.issue.fields.renderer.RenderableField;
import com.atlassian.jira.issue.search.ClauseNames;
import com.atlassian.jira.issue.search.SearchContext;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.util.ErrorCollection;
import org.ofbiz.core.entity.GenericValue;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Custom Field interface.
 * <p/>
 * Typically one obtains a CustomField using {@link com.atlassian.jira.issue.CustomFieldManager},
 * eg. {@link com.atlassian.jira.issue.CustomFieldManager#getCustomFieldObjectByName(String)}
 * <p/>
 * To create or update an instance of a CustomField for an issue (a {@link com.atlassian.jira.issue.customfields.customfieldvalue.CustomFieldValue}),
 * use {@link #createValue(com.atlassian.jira.issue.Issue,Object)} or {@link OrderableField#updateValue(com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem,com.atlassian.jira.issue.Issue,com.atlassian.jira.issue.ModifiedValue,com.atlassian.jira.issue.util.IssueChangeHolder)}.
 *
 * @see com.atlassian.jira.issue.customfields.customfieldvalue.CustomFieldValue CustomFieldValue - a value of this field for a specific issue
 * @see CustomFieldType CustomFieldType - The type of custom field (text, number, user picker etc).
 */
public interface CustomField extends NavigableField, HideableField, ConfigurableField, RenderableField, RequirableField, OrderableField
{
    /**
     * Determines if this custom field is within the scope of the given project, and list of Issue Types.
     *
     * <p> If the issueTypeIds list is null or an empty list, then it is treated as a wildcard.
     *
     * @param project The project.
     * @param issueTypeIds List of IssueType ID's.
     * @return {@code true} if this custom field is within the given scope.
     */
    boolean isInScope(Project project, List<String> issueTypeIds);

    /**
     * This is used for determining whether we can view a custom field in view issue.
     *
     * @param project      project generic value
     * @param issueTypeIds issue type IDs, e.g. ["1", "2"] for Bugs and New Features
     * @return true if the field is to be shown, false if hidden
     * @deprecated Please use {@link #isInScope(com.atlassian.jira.project.Project, java.util.List)}. This method was created in v4.3 as a temporary compatibility measure and has been always deprecated.
     */
    boolean isInScope(GenericValue project, List<String> issueTypeIds);

    /**
     * Determines whether this custom field is in scope.
     *
     * @param searchContext search context
     * @return true if this field is in scope
     */
    boolean isInScope(SearchContext searchContext);

    /**
     * Determines whether this custom field is in scope.
     *
     * @param user          current user
     * @param searchContext search context
     * @return true if this field is in scope
     *
     * @deprecated Use {@link #isInScope(com.atlassian.jira.issue.search.SearchContext)} instead. Since v4.3
     */
    boolean isInScope(User user, SearchContext searchContext);

    /**
     * Returns a generic value that represents this custom field
     *
     * @return generic value of this custom field
     * @deprecated
     */
    GenericValue getGenericValue();

    /**
     * This method compares the values of this custom field in two given issues.
     * <p/>
     * Returns a negative integer, zero, or a positive integer as the value of first issue is less than, equal to,
     * or greater than the value of the second issue.
     * <p/>
     * If either of given issues is null a IllegalArgumentException is thrown.
     *
     * @param issue1 issue to compare
     * @param issue2 issue to compare
     * @return a negative integer, zero, or a positive integer as the value of first issue is less than, equal to, or
     *         greater than the value of the second issue
     * @throws IllegalArgumentException if any of given issues is null
     */
    public int compare(Issue issue1, Issue issue2) throws IllegalArgumentException;

    //------------------------------------------------- methods used for create / edit, which use Collections of Strings

    /**
     * Get the custom field string values that are relevant to this particular custom field
     *
     * @param customFieldValuesHolder containing all params
     * @return a {@link CustomFieldParams} of {@link String} objects
     */
    public CustomFieldParams getCustomFieldValues(Map customFieldValuesHolder);

    /**
     * Retrieves and returns the Object representing the this CustomField value for the given issue.
     * See {@link CustomFieldType#getValueFromIssue(CustomField,Issue)}.
     * This is only used to communicate with the 'view' JSP. Multiselects will return a list, dates a date, etc.
     *
     * @param issue issue to retrieve the value from
     * @return Object representing the this CustomField value for the given issue
     * @see #getValueFromParams(java.util.Map)
     */
    public Object getValue(Issue issue);

    /**
     * Removes this custom field and returns a set of issue IDs of all issues that are affected by removal of this
     * custom field.
     *
     * @return a set of issue IDs of affected issues
     */
    public Set<Long> remove();

    /**
     * Returns options for this custom field if it is
     * of {@link com.atlassian.jira.issue.customfields.MultipleCustomFieldType} type. Otherwise returns null.
     * <p/>
     * As this is just used by the view layer, it can be a list of objects
     *
     * @param key             not used
     * @param jiraContextNode JIRA context node
     * @return options for this custom field if it is of {@link com.atlassian.jira.issue.customfields.MultipleCustomFieldType} type, null otherwise
     */
    public Options getOptions(String key, JiraContextNode jiraContextNode);

    /**
     * Sets the name of this custom field.
     *
     * @param name name to set
     */
    void setName(String name);

    /**
     * Returns the description of this custom field.
     *
     * @return the description of this custom field
     */
    String getDescription();

    /**
     * Sets the description of this custom field.
     *
     * @param description description to set
     */
    void setDescription(String description);

    /**
     * Retrieves the {@link CustomFieldSearcher} for this custom field.
     * The seracher, if found is initialized with this custom field before it is returned.
     *
     * @return found custom field searcher or null, if none found
     */
    CustomFieldSearcher getCustomFieldSearcher();

    /**
     * Sets the {@link CustomFieldSearcher} for this custom field.
     *
     * @param searcher custom field searcher to associate with this custom field
     */
    void setCustomFieldSearcher(CustomFieldSearcher searcher);

    /**
     * Stores this custom field.
     */
    void store();

    /**
     * Returns true if this custom field can be edited, false otherwise.
     *
     * @return true if this custom field can be edited, false otherwise
     */
    boolean isEditable();

    /**
     * Returns ID of this custom field.
     *
     * @return ID of this custom field
     */
    Long getIdAsLong();

    /**
     * Returns a list of configuration schemes.
     *
     * @return a list of {@link FieldConfigScheme} objects.
     */
    List<FieldConfigScheme> getConfigurationSchemes();

    /**
     * Returns options for this custom field if it is
     * of {@link com.atlassian.jira.issue.customfields.MultipleCustomFieldType} type. Otherwise returns null.
     * <p/>
     * As this is just used by the view layer, it can be a list of objects
     *
     * @param key         not used
     * @param config      relevant field config
     * @param contextNode JIRA context node
     * @return options for this custom field if it is of {@link com.atlassian.jira.issue.customfields.MultipleCustomFieldType} type, null otherwise
     */
    Options getOptions(String key, FieldConfig config, JiraContextNode contextNode);

    /**
     * Returns a relevant {@link FieldConfig} for the given issue. If the field <strong>has</strong>
     * a config for the issue then one will be returned, otherwise null is returned.
     * <p/>
     * For example, if we have 2 projects: project A and project B, and a custom field is configured to be only
     * applicable to project A, calling getRelevantConfig with an issue from project A should return the config
     * (i.e. <strong>not</strong> null). Calling this method with an issue from project B <strong>should</strong>
     * rerurn null.
     *
     * @param issue issue whose project and issue type will be used to check if the field has a config
     * @return an instance of {@link FieldConfig} representing the configuration of the field for issue's
     *         project/issue type. If the field does not have a config for issue's project/issue type, null is returned.
     */
    FieldConfig getRelevantConfig(Issue issue);

    /**
     * Validates relevant parameters on custom field type of this custom field. Any errors found are added to the given
     * errorCollection.
     * See {@link CustomFieldType#validateFromParams(CustomFieldParams,ErrorCollection,FieldConfig)}
     *
     * @param actionParameters action parameters
     * @param errorCollection  error collection to add errors to
     * @param config           field config
     */
    void validateFromActionParams(Map actionParameters, ErrorCollection errorCollection, FieldConfig config);

    /**
     * Returns a list of associated project categories for this custom field.
     * It returns null if {@link #getConfigurationSchemes()} returns null.
     * It returns an empty list if the {@link #getConfigurationSchemes()} returns an empty list.
     * The returned list should be sorted by project category name.
     *
     * @return a list of {@link org.ofbiz.core.entity.GenericValue} objects that represent associated project categories
     *         as {@link com.atlassian.jira.issue.context.ProjectCategoryContext} objects
     */
    List<GenericValue> getAssociatedProjectCategories();

    /**
     * Returns a list of projects associated with this project. Will be null if the field is global
     *
     * @return List of project generic values
     */
    List<GenericValue> getAssociatedProjects();

    /**
     * Returns a list of issue types associated with this project. Will be null if the field is global
     *
     * @return List of issue type generic values
     */
    List<GenericValue> getAssociatedIssueTypes();

    /**
     * Returns true if this custom field applies for all projects and all issue types.
     *
     * @return true if it is in all projects and all issue types, false otherwise.
     */
    boolean isGlobal();

    /**
     * Checks whether this custom field applies for all projects. It returns true if it applies for all projects
     * for any field configuration scheme, false otherwise.
     *
     * @return true if it applies for all projects for any field configuration scheme, false otherwise.
     */
    boolean isAllProjects();

    /**
     * Returns true if it applies for all issue types, false otherwise.
     *
     * @return true if it applies for all issue types, false otherwise.
     */
    boolean isAllIssueTypes();

    /**
     * Returns true if all configuration schemes returned by {@link #getConfigurationSchemes()} are enabled.
     *
     * @return true if all configuration schemes are enabled, false otherwise
     */
    boolean isEnabled();

    /**
     * Looks up the {@link com.atlassian.jira.issue.customfields.CustomFieldType}. It can return null if the custom
     * field type cannot be found in the {@link com.atlassian.jira.issue.CustomFieldManager}.
     *
     * @return custom field type
     */
    CustomFieldType getCustomFieldType();

    /**
     * Returns the relevant field config of this custom field for the give issue context
     *
     * @param issueContext issue context to find the relevant field config for
     * @return the relevant field config of this custom field for the give issue context
     */
    FieldConfig getRelevantConfig(IssueContext issueContext);

    /**
     * The {@link FieldConfig} that is relevent to <strong>all</strong> issue contexts in the search context
     * Checks that all configs within search context are the same - i.e. all null or all the same config.
     * <p/>
     * Returns null if any two configs are different.
     * <p/>
     * Note: null config is not equal to non-null config. Previously, a non-null config was returned even if the first
     * config(s) was null.
     *
     * @param searchContext search context
     * @return null if any two configs are different
     */
    FieldConfig getReleventConfig(SearchContext searchContext);

    /**
     * Return the JQL clause names that this custom field should be recognized by.
     *
     * @return the clause names this custom field should be recognized by.
     */
    ClauseNames getClauseNames();
}
