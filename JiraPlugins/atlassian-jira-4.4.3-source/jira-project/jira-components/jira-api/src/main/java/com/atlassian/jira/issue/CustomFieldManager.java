/*
 * Copyright (c) 2002-2004
 * All rights reserved.
 */

package com.atlassian.jira.issue;

import com.atlassian.jira.exception.RemoveException;
import com.atlassian.jira.issue.customfields.CustomFieldSearcher;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.SearchContext;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;

import java.util.Collection;
import java.util.List;

/**
 * Functions for working with {@link CustomField}s.
 */
public interface CustomFieldManager
{
    public static String PLUGIN_KEY = "com.atlassian.jira.plugin.system.customfieldtypes";

    /**
     * Retrieve custom field(s) of a certain name.
     *
     * @param customFieldName
     * @return A collection of {@link CustomField}s.
     */
    Collection<CustomField> getCustomFieldObjectsByName(final String customFieldName);

    /**
     * Retrieve the first custom field object with the specified name.
     * <b>WARNING:</b> Custom Field names are no longer guaranteed to be unique.
     * This method returns the first named custom field. Use {@link #getCustomFieldObjectsByName(String)} to retrieve all custom fields.
     *
     * @param customFieldName
     * @return The first named {@link CustomField}, or null if not found.
     */
    CustomField getCustomFieldObjectByName(final String customFieldName);

    /**
     * Get a CustomField by ID.
     *
     * @param id ID of field, eg. 10000
     * @return The {@link CustomField} or null.
     */
    CustomField getCustomFieldObject(Long id);

    /**
     * Get a CustomField by its text key (eg 'customfield_10000').
     *
     * @param id Eg. 'customfield_10000'
     * @return The {@link CustomField} or null if not found.
     */
    CustomField getCustomFieldObject(String id);

    /**
     * Returns all custom fields.
     *
     * @return A list of all {@link CustomField}s.
     */
    List<CustomField> getCustomFieldObjects();

    /**
     * Returns a list of custom fields where the {@link com.atlassian.jira.issue.fields.CustomField#isGlobal()}
     * is true.
     *
     * @return A list of {@link CustomField}s
     */
    List<CustomField> getGlobalCustomFieldObjects();

    /**
     * Gets a list of custom fields for a particular project and issue type.
     *
     * @param projectId Id of the project
     * @param issueType An issue type. See {@link com.atlassian.jira.config.ConstantsManager#ALL_ISSUE_TYPES},
     *                  {@link com.atlassian.jira.config.ConstantsManager#ALL_STANDARD_ISSUE_TYPES} and  {@link com.atlassian.jira.config.ConstantsManager#ALL_SUB_TASK_ISSUE_TYPES}
     * @return A list of {@link CustomField}s
     */
    List<CustomField> getCustomFieldObjects(Long projectId, String issueType);

    /**
     * Returns the same as {@link #getCustomFieldObjects(Long,String)} but allows
     * to specify a list of issueTypes.
     *
     * @param projectId  Id of the project
     * @param issueTypes A list of issue types. See {@link com.atlassian.jira.config.ConstantsManager#ALL_ISSUE_TYPES},
     *                   {@link com.atlassian.jira.config.ConstantsManager#ALL_STANDARD_ISSUE_TYPES} and  {@link com.atlassian.jira.config.ConstantsManager#ALL_SUB_TASK_ISSUE_TYPES}
     * @return A list of {@link CustomField}s
     */
    List<CustomField> getCustomFieldObjects(Long projectId, List<String> issueTypes);

    /**
     * Returns all customfields in a particular {@link SearchContext}. Also
     * see {@link CustomField#isInScope(com.opensymphony.user.User,com.atlassian.jira.issue.search.SearchContext)}.
     *
     * @param searchContext
     * @return A list of {@link CustomField}s
     */
    List<CustomField> getCustomFieldObjects(SearchContext searchContext);

    /**
     * Returns a list of {@link CustomField}s that apply to a particular issue.
     *
     * @param issue A {@link GenericValue} of the issue
     * @return A list of {@link CustomField}s
     */
    List<CustomField> getCustomFieldObjects(GenericValue issue);

    /**
     * Returns a list of {@link CustomField}s that apply to a particular issue.
     *
     * @param issue A {@link Issue} object
     * @return A list of {@link CustomField}s
     */
    List<CustomField> getCustomFieldObjects(Issue issue);


    /**
     * Retrieve all customfieldtypes registered in the sytem.
     *
     * @return a list og {@link CustomFieldType}s
     */
    public List getCustomFieldTypes();

    /**
     * Retrieve a custom field type by its type key.
     *
     * @param key Type identifier constructed from plugin XML.  Eg. the system types are specified in system-customfiedltypes-plugin.xml:
     *            <pre>
     *                                                                              &lt;atlassian-plugin key="com.atlassian.jira.plugin.system.customfieldtypes" name="Custom Field Types &amp; Searchers">
     *                                                                                ...
     *                                                                                &lt;customfield-type key="float" name="Number Field"
     *                                                                              ...
     *                                                                              </pre>
     *            To access the 'Number Field' type, the key would then be 'com.atlassian.jira.plugin.system.customfieldtypes:float'
     *            (the first part can be referred to via {@link com.atlassian.jira.web.action.admin.customfields.CreateCustomField#FIELD_TYPE_PREFIX}).
     */
    public CustomFieldType getCustomFieldType(String key);

    /**
     * Retrieves all the searchers registered for a particular custom Field.  Searchers may be registred via:
     * <pre>
     *   &lt;customfield-searcher key="daterange" name="Date Range picker"
     *                     i18n-name-key="admin.customfield.searcher.daterange.name"
     *                     class="com.atlassian.jira.issue.customfields.searchers.DateRangeSearcher"&gt
     * </pre>
     *
     * @param customFieldType
     * @return A list of {@link CustomFieldSearcher}s
     */
    public List getCustomFieldSearchers(CustomFieldType customFieldType);

    /**
     * Retrieve a custom field searcher by its type key.
     *
     * @param key Type identifier constructed from plugin XML.  Eg. the system types are specified in system-customfiedltypes-plugin.xml:
     *            <pre>
     *                                                                              &lt;atlassian-plugin key="com.atlassian.jira.plugin.system.customfieldtypes" name="Custom Field Types &amp; Searchers">
     *                                                                                ...
     *                                                                                 &lt;customfield-searcher key="exactnumber" name="Number Searcher"
     *                       <p/>
     *                                                                              ...
     *                                                                              </pre>
     *            To access the 'Number Searcher' searcher, the key would then be 'com.atlassian.jira.plugin.system.customfieldtypes:exactnumber'
     *            (the first part can be referred to via {@link com.atlassian.jira.web.action.admin.customfields.CreateCustomField#FIELD_TYPE_PREFIX}).
     */
    public CustomFieldSearcher getCustomFieldSearcher(String key);

    /**
     * Creates a custom field with the given name and description of the given CustomFieldType
     * using the given CustomFieldSearcher that is displayed in the given list of contexts available to the given list of issueTypes.
     *
     * @return the newly created CustomField.
     * @throws GenericEntityException if the CustomField could not be created.
     */
    CustomField createCustomField(String fieldName, String description, CustomFieldType fieldType, CustomFieldSearcher customFieldSearcher, List contexts, List issueTypes)
            throws GenericEntityException;

    /**
     * Removes the customfield supplied. This method will try to lookup the customfield object via the manager first.
     * If the customfield object can be retrieved this way, it simply delegates the removal to the
     * {@link #removeCustomField(com.atlassian.jira.issue.fields.CustomField)} method, which will not leave any orphaned
     * data behind.
     * <p/>
     * <b>NOTE:</b> Generally you should use the {@link #removeCustomField(com.atlassian.jira.issue.fields.CustomField)}
     * method to remove a custom field, as it is guaranteed to remove all data.  This method should only be used if
     * a customfield needs to be removed when the customfieldtype for that field is no longer available!
     * <p/>
     * If however the customfield object cannot be retrieved via the manager, which may be the case if the custom field
     * type is no longer available (a plugin may have been removed), then this method will try to lookup the custom
     * field directly in the database.  If it doesn't exist in the database, an IllegalArgumentException will
     * be thrown.  Otherwise, the customfield and all associated configurations will be removed.  This method
     * will also call to the {@link com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister#removeAllValues(String)}
     * method to delete any values.  Please note however, that if your custom field stores any other values (such
     * as options for example), they will have to be removed by the caller of this method, as the custom field type
     * is not accessible (which is usually responsible for removing such values).
     *
     * @param customFieldId The id of the customField to be removed.
     * @throws RemoveException On any error removing the custom field
     * @throws IllegalArgumentException If no customfield matching the customFieldId can be found
     */
    void removeCustomFieldPossiblyLeavingOrphanedData(Long customFieldId) throws RemoveException, IllegalArgumentException;

    /**
     * Removes the customfield supplied including all associations and values.
     *
     * @param customField The {@link CustomField} to be removed.
     * @throws RemoveException On any error removing the custom field
     */
    void removeCustomField(CustomField customField) throws RemoveException;

    /**
     * Removes the values stored by customfields for a particular Issue.
     *
     * @param issue The issue {@link GenericValue}
     * @throws GenericEntityException
     */
    void removeCustomFieldValues(GenericValue issue) throws GenericEntityException;

    /**
     * Used if a project is deleted to remove the project field associations.
     *
     * @param project The project being deleted.
     */
    void removeProjectAssociations(GenericValue project);

    /**
     * Used if a project category is deleted to remove the field associations.
     *
     * @param projectCategory The project category being deleted.
     */
    void removeProjectCategoryAssociations(GenericValue projectCategory);

    /**
     * Converts a customfield {@link GenericValue} to a {@link CustomField} instance.
     *
     * @param customFieldGv
     * @return a {@link CustomField} instance
     */
    CustomField getCustomFieldInstance(GenericValue customFieldGv);

    /**
     * reloads all customfields into the cache from the DB.
     */
    void refresh();

    /**
     *  clear the cache
     */
    void clear();

    /**
     * Retrieve a custom field searcher by its type key.
     *
     * @param key Type identifier constructed from plugin XML.  Eg. the system types are specified in system-customfiedltypes-plugin.xml:
     *            <pre>
     *                                                                              &lt;atlassian-plugin key="com.atlassian.jira.plugin.system.customfieldtypes" name="Custom Field Types &amp; Searchers">
     *                                                                                ...
     *                                                                                &lt;customfield-type key="float" name="Number Field"
     *                                                                              ...
     *                                                                              </pre>
     *            To access the 'Number Field' type, the key would then be 'com.atlassian.jira.plugin.system.customfieldtypes:float'
     *            (the first part can be referred to via {@link com.atlassian.jira.web.action.admin.customfields.CreateCustomField#FIELD_TYPE_PREFIX}).
     */
    Class getCustomFieldSearcherClass(String key);
}
