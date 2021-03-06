package com.atlassian.jira.issue.customfields.impl;

import com.atlassian.jira.imports.project.customfield.ProjectCustomFieldImporter;
import com.atlassian.jira.imports.project.customfield.ProjectImportableCustomField;
import com.atlassian.jira.imports.project.customfield.SelectCustomFieldImporter;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.context.IssueContext;
import com.atlassian.jira.issue.context.JiraContextNode;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.CustomFieldUtils;
import com.atlassian.jira.issue.customfields.GroupSelectorField;
import com.atlassian.jira.issue.customfields.MultipleSettableCustomFieldType;
import com.atlassian.jira.issue.customfields.SortableCustomField;
import com.atlassian.jira.issue.customfields.config.item.SettableOptionsConfigItem;
import com.atlassian.jira.issue.customfields.impl.rest.MultiSelectCustomFieldOperationsHandler;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.option.OptionUtils;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.customfields.persistence.PersistenceFieldType;
import com.atlassian.jira.issue.customfields.statistics.SelectStatisticsMapper;
import com.atlassian.jira.issue.customfields.view.CustomFieldParams;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.config.FieldConfigItemType;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.rest.FieldJsonRepresentation;
import com.atlassian.jira.issue.fields.rest.FieldTypeInfo;
import com.atlassian.jira.issue.fields.rest.FieldTypeInfoContext;
import com.atlassian.jira.issue.fields.rest.RestAwareCustomFieldType;
import com.atlassian.jira.issue.fields.rest.RestCustomFieldTypeOperations;
import com.atlassian.jira.issue.fields.rest.RestFieldOperationsHandler;
import com.atlassian.jira.issue.fields.rest.json.JsonData;
import com.atlassian.jira.issue.fields.rest.json.JsonType;
import com.atlassian.jira.issue.fields.rest.json.JsonTypeBuilder;
import com.atlassian.jira.issue.fields.rest.json.beans.CustomFieldOptionJsonBean;
import com.atlassian.jira.issue.fields.rest.json.beans.JiraBaseUrls;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.ErrorCollection.Reason;
import com.atlassian.jira.util.NotNull;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptySet;

/**
 * <p>Multiple Select Type allows selecting of multiple {@link Option}s</p>
 * <dl>
 * <dt><strong>Transport Object Type</strong></dt>
 * <dd>{@link Collection<Option>}</dd>
 * <dt><strong>Singular Object Type</strong></dt>
 * <dd>{@link Option}</dd>
 * <dt><Strong>Database Storage Type</Strong></dt>
 * <dd>{@link String} of option id</dd>
 * </dl>
 */
public class MultiSelectCFType extends AbstractMultiCFType<Option>
        implements MultipleSettableCustomFieldType<Collection<Option>, Option>, SortableCustomField<List<String>>, GroupSelectorField, ProjectImportableCustomField, RestAwareCustomFieldType, RestCustomFieldTypeOperations

{
    public static final String COMMA_REPLACEMENT = "&#44;";
    private static final Logger log = Logger.getLogger(MultiSelectCFType.class);

    private final ProjectCustomFieldImporter projectCustomFieldImporter;
    private final OptionsManager optionsManager;
    private final JiraBaseUrls jiraBaseUrls;

    public MultiSelectCFType(final OptionsManager optionsManager, final CustomFieldValuePersister valuePersister, final GenericConfigManager genericConfigManager, JiraBaseUrls jiraBaseUrls)
    {
        super(valuePersister, genericConfigManager);
        this.optionsManager = optionsManager;
        this.jiraBaseUrls = jiraBaseUrls;
        projectCustomFieldImporter = new SelectCustomFieldImporter();
    }

    public Set<Long> remove(final CustomField field)
    {
        optionsManager.removeCustomFieldOptions(field);
        return customFieldValuePersister.removeAllValues(field.getId());
    }

    public Options getOptions(final FieldConfig fieldConfig, final JiraContextNode jiraContextNode)
    {
        return optionsManager.getOptions(fieldConfig);
    }

    public void removeValue(final CustomField field, final Issue issue, final Option option)
    {
        if (option != null)
        {
            customFieldValuePersister.removeValue(field, issue.getId(), PersistenceFieldType.TYPE_LIMITED_TEXT, option.getOptionId().toString());
        }
    }

    public Set<Long> getIssueIdsWithValue(final CustomField field, final Option option)
    {
        if (option != null)
        {
            return customFieldValuePersister.getIssueIdsWithValue(field, PersistenceFieldType.TYPE_LIMITED_TEXT, option.getOptionId().toString());
        }
        else
        {
            return emptySet();
        }
    }

    @NotNull
    @Override
    protected PersistenceFieldType getDatabaseType()
    {
        return PersistenceFieldType.TYPE_LIMITED_TEXT;
    }

    @Override
    protected Option convertDbValueToType(Object dbValue)
    {
        return getSingularObjectFromString((String) dbValue);
    }

    @Override
    protected Object convertTypeToDbValue(Option value)
    {
        return getStringFromSingularObject(value);
    }

    @Override
    protected Comparator<Option> getTypeComparator()
    {
        return null; // can't really compare without knowing the full list of possibilities (FieldConfig)
    }

    public void setDefaultValue(final FieldConfig fieldConfig, final Collection<Option> value)
    {
        List<Long> defaultIds = new ArrayList<Long>();
        if (value != null)
        {
            for (Option o : value)
            {
                defaultIds.add(o.getOptionId());
            }
        }
        genericConfigManager.update(CustomFieldType.DEFAULT_VALUE_TYPE, fieldConfig.getId().toString(), defaultIds);
    }

    public Collection<Option> getDefaultValue(final FieldConfig fieldConfig)
    {
        // why do we store the default value as a Long, but yet we store the actual value as Strings? Leaving for now, changes would require upgrade tasks.
        List<Option> options = new ArrayList<Option>();
        Collection<Long> optionIds = (Collection<Long>) genericConfigManager.retrieve(CustomFieldType.DEFAULT_VALUE_TYPE, fieldConfig.getId().toString());
        if (optionIds != null)
        {
            for (Long optionId : optionIds)
            {
                Option option = optionsManager.findByOptionId(optionId);
                if (option != null)
                {
                    options.add(option);
                }
            }
        }
        return options;
    }

    public Collection<Option> getValueFromCustomFieldParams(final CustomFieldParams parameters) throws FieldValidationException
    {
        final Collection<?> values = parameters.getAllValues();
        if (CustomFieldUtils.isCollectionNotEmpty(values))
        {
            List<Option> options = new ArrayList<Option>();
            for (Object value : values)
            {
                options.add(getSingularObjectFromString((String) value));
            }
            return options;
        }
        else
        {
            return null;
        }
    }

    public Object getStringValueFromCustomFieldParams(final CustomFieldParams parameters)
    {
        return parameters.getValuesForNullKey();
    }

    public void validateFromParams(final CustomFieldParams relevantParams, final ErrorCollection errorCollectionToAddTo, final FieldConfig config)
    {
        @SuppressWarnings ("unchecked")
        final Collection<String> params = relevantParams.getAllValues();
        if ((params == null) || params.isEmpty())
        {
            return;
        }

        final CustomField customField = config.getCustomField();

        for (final String paramValue : params)
        {
            if ("-1".equals(paramValue))
            {
                if (params.size() > 1)
                {
                    errorCollectionToAddTo.addError(customField.getId(), getI18nBean().getText("admin.errors.cannot.specify.none"), Reason.VALIDATION_FAILED);
                }
            }
            else
            {
                // Validate options
                final Options options = optionsManager.getOptions(config);
                Long optionId = null;
                try
                {
                    optionId = Long.valueOf(paramValue);
                }
                catch (NumberFormatException e)
                {
                    errorCollectionToAddTo.addError(customField.getId(), getI18nBean().getText("admin.errors.invalid.value.passed.for.customfield",
                            "'" + paramValue + "'", "'" + customField + "'", createValidOptionsString(options)), Reason.VALIDATION_FAILED);
                }
                if (options.getOptionById(optionId) == null)
                {
                    errorCollectionToAddTo.addError(customField.getId(), getI18nBean().getText("admin.errors.invalid.value.passed.for.customfield",
                            "'" + paramValue + "'", "'" + customField + "'", createValidOptionsString(options)), Reason.VALIDATION_FAILED);
                }
            }
        }
    }

    private String createValidOptionsString(final Options options)
    {
        final List<Option> rootOptions = options.getRootOptions();
        final StringBuilder validOptions = new StringBuilder();

        for (Iterator<Option> optionIterator = rootOptions.iterator(); optionIterator.hasNext();)
        {
            Option option = optionIterator.next();
            validOptions.append(option.getOptionId()).append("[").append(option.getValue()).append("]");

            if (optionIterator.hasNext())
            {
                validOptions.append(", ");
            }
        }
        validOptions.append(", -1");
        return validOptions.toString();
    }

    @Override
    public String getChangelogString(CustomField field, Collection<Option> value)
    {
        ArrayList<String> stringValues = new ArrayList<String>();
        if (value != null)
        {
            for (Option option : value)
            {
                stringValues.add(option.getValue());
            }
            return StringUtils.join(stringValues, ",");
        }
        return "";
    }

    public Option getSingularObjectFromString(final String string) throws FieldValidationException
    {
        if ("-1".equals(string))
        {
            return null;
        }
        return getOptionFromStringValue(string);
    }

    private Option getOptionFromStringValue(String selectValue)
            throws FieldValidationException
    {
        final Long aLong = OptionUtils.safeParseLong(selectValue);
        if (aLong != null)
        {
            final Option option = optionsManager.findByOptionId(aLong);
            if (option != null)
            {
                return option;
            }
            else
            {
                return null;
            }
        }
        else
        {
            return null;
        }
    }

    public String getStringFromSingularObject(final Option optionObject)
    {
        if (optionObject == null)
        {
            return null;
        }
        return optionObject.getOptionId().toString();
    }

    @NotNull
    @Override
    public List<FieldConfigItemType> getConfigurationItemTypes()
    {
        final List<FieldConfigItemType> configurationItemTypes = super.getConfigurationItemTypes();
        configurationItemTypes.add(new SettableOptionsConfigItem(this, optionsManager));
        return configurationItemTypes;
    }

    public int compare(@NotNull final List<String> customFieldObjectValue1, @NotNull final List<String> customFieldObjectValue2, final FieldConfig fieldConfig)
    {
        final Options options = getOptions(fieldConfig, null);

        if (options != null)
        {
            final Long i1 = getLowestIndex(customFieldObjectValue1, options);
            final Long i2 = getLowestIndex(customFieldObjectValue2, options);

            return i1.compareTo(i2);
        }

        log.info("No options were found.");
        return 0;
    }

    public ProjectCustomFieldImporter getProjectImporter()
    {
        return projectCustomFieldImporter;
    }

    private Long getLowestIndex(final List<String> l, final Options options)
    {
        Long lowest = new Long(Long.MAX_VALUE);

        for (final String name : l)
        {
            final Option o = options.getOptionById(Long.valueOf(name));
            if ((o != null) && (o.getSequence() != null) && (o.getSequence().compareTo(lowest) < 0))
            {
                lowest = o.getSequence();
            }
        }

        return lowest;
    }

    /**
     * Parses the given comma-separated String value into a Collection. Whitespace is trimmed and blank fields are
     * discarded. If literal commas are required, then they can be escaped with a backslash. Therefore the input String
     * <code>"red, white\, and blue"</code> would produce two tokens in its list: <ul> <li>red</li> <li>white, and
     * blue</li> </ul>
     *
     * @param value The comma-separated input String.
     * @return Collection of tokens parsed from the input value.
     * @see #getStringFromTransferObject(java.util.Collection)
     */
    public static Collection<String> extractTransferObjectFromString(final String value)
    {
        if (value == null)
        {
            return null;
        }
        final Collection<String> valuesToAdd = new ArrayList<String>();
        // Commas can be escaped with a backslash if we actually want it in our value text.
        // So we replace instances of "\," with "&#44;"
        final String[] a = StringUtils.split(StringUtils.replace(value, "\\,", COMMA_REPLACEMENT), ",");

        for (final String s : a)
        {
            // put a comma back wherever we have the "replacement" text.
            final String s2 = StringUtils.replace(s, COMMA_REPLACEMENT, ",");
            // Now trim whitespace
            final String s3 = StringUtils.trimToNull(s2);
            if (s3 != null)
            {
                // We only add non-blank values to our list.
                valuesToAdd.add(s3);
            }
        }

        return valuesToAdd;
    }

    /**
     * Takes a Collection of values and creates a comma-separated String that represents this Collection. <p> If any
     * items in the collection include literal commas, then these commas are escaped with a prepended backslash. eg a
     * list that looks like: <ul> <li>All sorts</li> <li>Tom, Dick, and Harry</li> </ul> Will be turned into a string
     * that looks like "All sorts,Tom\, Dick\, and Harry" </p>
     *
     * @param collection a collection of Strings to be comma separated
     */
    public static String getStringFromTransferObject(final Collection<String> collection)
    {
        if (collection != null)
        {
            final StringBuilder sb = new StringBuilder();
            for (final Iterator<String> iterator = collection.iterator(); iterator.hasNext();)
            {
                String s = iterator.next();
                s = StringUtils.replace(s, ",", "\\,");
                sb.append(s);
                if (iterator.hasNext())
                {
                    sb.append(",");
                }
            }
            return sb.toString();
        }
        else
        {
            return null;
        }
    }

    // override
    @Override
    public boolean valuesEqual(final Collection<Option> v1, final Collection<Option> v2)
    {
        if (v1 == v2)
        {
            return true;
        }

        if ((v1 == null) || (v2 == null))
        {
            return false;
        }

        // we want the equality test to not be order-dependant. see JRA-15105
        return CollectionUtils.isEqualCollection(v1, v2);
    }

    @Override
    public Object accept(VisitorBase visitor)
    {
        if (visitor instanceof Visitor)
        {
            return ((Visitor) visitor).visitMultiSelect(this);
        }

        return super.accept(visitor);
    }

    public Query getQueryForGroup(final String fieldName, final String groupName)
    {
        return new TermQuery(new Term(fieldName + SelectStatisticsMapper.RAW_VALUE_SUFFIX, groupName));
    }

    public interface Visitor<T> extends VisitorBase<T>
    {
        T visitMultiSelect(MultiSelectCFType multiSelectCustomFieldType);
    }

    @Override
    public FieldTypeInfo getFieldTypeInfo(FieldTypeInfoContext fieldTypeInfoContext)
    {
        // Get the allowed options
        FieldConfig config = ((CustomField) fieldTypeInfoContext.getOderableField()).getRelevantConfig(fieldTypeInfoContext.getIssueContext());
        Options options = optionsManager.getOptions(config);
        Collection<CustomFieldOptionJsonBean> optionBeans = CustomFieldOptionJsonBean.shortBeans(options, jiraBaseUrls);

        return new FieldTypeInfo(optionBeans, null);
    }

    @Override
    public JsonType getJsonSchema(CustomField customField)
    {
        return JsonTypeBuilder.customArray(JsonType.STRING_TYPE, getKey(), customField.getIdAsLong());
    }

    @Override
    public FieldJsonRepresentation getJsonFromIssue(CustomField field, Issue issue, boolean renderedVersionRequested, @Nullable FieldLayoutItem fieldLayoutItem)
    {
        Collection<Option> valueFromIssue = getValueFromIssue(field, issue);
        if (valueFromIssue == null)
        {
            return new FieldJsonRepresentation(new JsonData(null));
        }
        return new FieldJsonRepresentation(new JsonData(CustomFieldOptionJsonBean.shortBeans(valueFromIssue, jiraBaseUrls)));
    }

    @Override
    public JsonData getJsonDefaultValue(IssueContext issueCtx, CustomField field)
    {
        FieldConfig config = field.getRelevantConfig(issueCtx);
        Collection<Option> defaultValue = (Collection<Option>) field.getCustomFieldType().getDefaultValue(config);
        return defaultValue == null ? null : new JsonData(CustomFieldOptionJsonBean.shortBeans(defaultValue, jiraBaseUrls));
    }

    @Override
    public RestFieldOperationsHandler getRestFieldOperation(CustomField field)
    {
        return new MultiSelectCustomFieldOperationsHandler(optionsManager, field, getI18nBean());
    }
}
