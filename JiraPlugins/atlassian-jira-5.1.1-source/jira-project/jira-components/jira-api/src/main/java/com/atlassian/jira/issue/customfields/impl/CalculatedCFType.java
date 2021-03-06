package com.atlassian.jira.issue.customfields.impl;

import com.atlassian.annotations.PublicSpi;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.SortableCustomField;
import com.atlassian.jira.issue.customfields.view.CustomFieldParams;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.config.FieldConfigItemType;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.NotNull;
import org.ofbiz.core.entity.GenericValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A CustomFieldType which is calculated rather than set.  <em>Transport Object</em> is defined by subclass
 * @param <T> Transport Object as defined in {@link com.atlassian.jira.issue.customfields.CustomFieldType}
 * @param <S> Single Object contained within the Transport Object as defined in {@link com.atlassian.jira.issue.customfields.CustomFieldType}
 */
@PublicSpi
public abstract class CalculatedCFType<T, S> extends AbstractCustomFieldType<T, S> implements SortableCustomField<T>
{
    public Set<Long> remove(CustomField field)
    {
        return Collections.emptySet();
    }

    public void validateFromParams(CustomFieldParams relevantParams, ErrorCollection errorCollectionToAddTo, FieldConfig config)
    {
        // Do nothing
    }

    public void createValue(CustomField field, Issue issue, T value)
    {
        // Do nothing
    }

    public void updateValue(CustomField field, Issue issue, T value)
    {
        // Do nothing
    }

    public T getDefaultValue(FieldConfig fieldConfig)
    {
        return null;
    }

    public void setDefaultValue(FieldConfig fieldConfig, T value)
    {
        // Do nothing
    }

    public String getChangelogValue(CustomField field, T value)
    {
        return null;
    }

    public T getValueFromCustomFieldParams(CustomFieldParams parameters) throws FieldValidationException
    {
        Object o = parameters.getFirstValueForKey(null);

        // This method mandates that it cannot be a collection object.  To provide backwards compatability I have assumed T == S.
        // If not it will break at run time. And hopefully plugin developers will see it.  I'm assuming the method doesn't get used though, cause surely it would have broken before now
        // ParticipantsCustomField is an example of one which is actually a collection.
        return (T) getSingularObjectFromString((String) o);
    }

    public Object getStringValueFromCustomFieldParams(CustomFieldParams parameters)
    {
        return parameters.getFirstValueForNullKey();
    }

    public int compare(@NotNull T o1, @NotNull T o2, FieldConfig fieldConfig)
    {
        if (o1 != null && o1 instanceof Comparable && o2 instanceof Comparable)
        {
            Comparable comparable1 = (Comparable) o1;
            return comparable1.compareTo(o2);
        }

        return 0;
    }

    @NotNull
    public List<FieldConfigItemType> getConfigurationItemTypes()
    {
        // No defaults for calculated custom fields and can't be Collections.EMPTY_LIST since it needs to be mutatble
        return new ArrayList<FieldConfigItemType>();
    }

    @Override
    public Object accept(VisitorBase visitor)
    {
        if (visitor instanceof Visitor)
        {
            return ((Visitor) visitor).visitCalculated(this);
        }

        return super.accept(visitor);
    }

    public interface Visitor<X> extends VisitorBase<X>
    {
        X visitCalculated(CalculatedCFType calculatedCustomFieldType);
    }
}

