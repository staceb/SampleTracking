/*
 * Copyright (c) 2002-2004
 * All rights reserved.
 */

package com.atlassian.jira.issue.fields.layout.column;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.ManagerFactory;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.NavigableField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayout;
import com.atlassian.jira.issue.search.managers.SearchHandlerManager;
import com.atlassian.util.profiling.UtilTimerStack;
import com.opensymphony.user.User;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.Map;

public class ColumnLayoutItemImpl implements ColumnLayoutItem
{
    private static final Logger log = Logger.getLogger(ColumnLayoutItemImpl.class);

    private final NavigableField navigableField;
    private final int position;

    public ColumnLayoutItemImpl(final NavigableField navigableField, final int position)
    {
        this.navigableField = navigableField;
        this.position = position;
    }

    public NavigableField getNavigableField()
    {
        return navigableField;
    }

    public int getPosition()
    {
        return position;
    }

    public int compareTo(final Object o)
    {
        if (o instanceof ColumnLayoutItem)
        {
            return (getPosition() - ((ColumnLayoutItem) o).getPosition());
        }
        else
        {
            throw new IllegalArgumentException(o + " is not of type ColumnLayoutItem.");
        }
    }

    public String getSafeSortOrder(final String specifiedSortOrder)
    {
        if (specifiedSortOrder == null)
        {
            return navigableField.getDefaultSortOrder();
        }
        else
        {
            return specifiedSortOrder;
        }
    }

    public boolean isAliasForField(final User user, final String jqlName)
    {
        final SearchHandlerManager searchHandlerManager = ComponentManager.getComponentInstanceOfType(SearchHandlerManager.class);
        final Collection<String> fieldIds = searchHandlerManager.getFieldIds(user, jqlName);
        return fieldIds.contains(navigableField.getId());
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof ColumnLayoutItemImpl))
        {
            return false;
        }

        final ColumnLayoutItemImpl columnLayoutItem = (ColumnLayoutItemImpl) o;

        if (position != columnLayoutItem.position)
        {
            return false;
        }
        if (navigableField != null ? !navigableField.equals(columnLayoutItem.navigableField) : columnLayoutItem.navigableField != null)
        {
            return false;
        }

        return true;
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int hashCode()
    {
        int result;
        result = (navigableField != null ? navigableField.hashCode() : 0);
        result = 29 * result + position;
        return result;
    }

    public String getHtml(final Map displayParams, final Issue issue)
    {
        final FieldLayout fieldLayout = ManagerFactory.getFieldManager().getFieldLayoutManager().getFieldLayout(issue);
        final String hiddenFieldId = getNavigableField().getHiddenFieldId();
        if ((fieldLayout == null) || ((hiddenFieldId != null) && fieldLayout.isFieldHidden(hiddenFieldId)))
        {
            return "";
        }
        else
        {
            final String timerName = !UtilTimerStack.isActive() ? "" : "Rendering navigable field '" + getNavigableField().getId() + "' for issue: " + (issue == null ? "null" : issue.getKey());
            try
            {
                UtilTimerStack.push(timerName);
                return getNavigableField().getColumnViewHtml(fieldLayout.getFieldLayoutItem(getNavigableField().getId()), displayParams, issue);
            }
            finally
            {
                UtilTimerStack.pop(timerName);
            }
        }
    }

    public String getColumnHeadingKey()
    {
        return getNavigableField().getColumnHeadingKey();
    }
}
