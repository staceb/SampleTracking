package com.atlassian.jira.web.action.admin.issuefields.screens;

import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.screen.FieldScreen;
import com.atlassian.jira.issue.fields.screen.FieldScreenManager;
import com.atlassian.jira.issue.fields.screen.FieldScreenTab;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.websudo.WebSudoRequired;
import org.apache.commons.collections.ListUtils;
import webwork.action.ActionContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@WebSudoRequired
public class AssociateFieldToScreens extends JiraWebActionSupport
{
    // ------------------------------------------------------------------------------------------------------- Constants


    // ------------------------------------------------------------------------------------------------- Type Properties
    private String fieldId;
    private Long[] associatedTabs;
    private Long[] associatedScreens;

    // ---------------------------------------------------------------------------------------------------- Dependencies
    private final FieldScreenManager fieldScreenManager;
    private final FieldManager fieldManager;

    // ---------------------------------------------------------------------------------------------------- Constructors
    public AssociateFieldToScreens(FieldManager fieldManager, FieldScreenManager fieldScreenManager)
    {
        this.fieldManager = fieldManager;
        this.fieldScreenManager = fieldScreenManager;
    }

    // -------------------------------------------------------------------------------------------------- Action Methods
    protected String doExecute() throws Exception
    {
        List newScreens = getAssociatedScreens() != null ? Arrays.asList(getAssociatedScreens()) : Collections.EMPTY_LIST;
        List newTabs = new ArrayList();
        Map actionParams = ActionContext.getParameters();
        if (associatedScreens != null)
        {
            for (int i = 0; i < associatedScreens.length; i++)
            {
                Long screenId = associatedScreens[i];
                final Object o = actionParams.get(screenId.toString());
                final String tabIdString = ((String[]) o)[0];
                newTabs.add(new Long(tabIdString));
            }
        }

        List oldTabs = getStoredTabIdsForField() != null ? Arrays.asList(getStoredTabIdsForField()) : Collections.EMPTY_LIST;

        // Remove from tabs
        final List tabsToRemove = ListUtils.subtract(oldTabs, newTabs);
        for (Iterator iterator = tabsToRemove.iterator(); iterator.hasNext();)
        {
            Long tabId = (Long) iterator.next();
            final FieldScreenTab fieldScreenTab = fieldScreenManager.getFieldScreenTab(tabId);
            fieldScreenTab.getFieldScreen().removeFieldScreenLayoutItem(getFieldId());
        }

        // Add to new tabs
        final List tabsToAdd = ListUtils.subtract(newTabs, oldTabs);
        for (Iterator iterator = tabsToAdd.iterator(); iterator.hasNext();)
        {
            Long tabId = (Long) iterator.next();
            final FieldScreenTab fieldScreenTab = fieldScreenManager.getFieldScreenTab(tabId);
            fieldScreenTab.addFieldScreenLayoutItem(getFieldId());
        }

        fieldManager.refresh();
        fieldScreenManager.refresh();

        return getRedirect("ViewFieldScreens.jspa");
    }

    // -------------------------------------------------------------------------------------------------- View Helpers
    public Collection getScreens()
    {
        return fieldScreenManager.getFieldScreens();
    }

    // -------------------------------------------------------------------------------------------------- Helper Methods
    /**
     * Gets the tab Ids of the tabs with the particular field
     *
     */
    private Long[] getStoredTabIdsForField()
    {
        List selectedTabs = new ArrayList(fieldScreenManager.getFieldScreenTabs(getField().getId()));

        Long[] newAssociatedTabs = null;
        if (selectedTabs != null && !selectedTabs.isEmpty())
        {
            newAssociatedTabs = new Long[selectedTabs.size()];
            for (int i = 0; i < newAssociatedTabs.length; i++)
            {
                final FieldScreenTab tab = (FieldScreenTab) selectedTabs.get(i);
                newAssociatedTabs[i] = tab.getId();
            }
        }

        return newAssociatedTabs;
    }


    public FieldScreenTab selectedTabForScreen(FieldScreen screen)
    {
        if (screen.containsField(getFieldId()))
        {
            for (Iterator iterator = screen.getTabs().iterator(); iterator.hasNext();)
            {
                FieldScreenTab tab = (FieldScreenTab) iterator.next();
                if (tab.getFieldScreenLayoutItem(getFieldId()) != null)
                {
                    return tab;
                }
            }
        }

        return null;
    }

    // -------------------------------------------------------------------------------------- Basic accessors & mutators
    public String getFieldId()
    {
        return fieldId;
    }

    public void setFieldId(String fieldId)
    {
        this.fieldId = fieldId;
    }

    public Long[] getAssociatedTabs()
    {
        return associatedTabs;
    }

    public void setAssociatedTabs(Long[] associatedTabs)
    {
        this.associatedTabs = associatedTabs;
    }

    public Long[] getAssociatedScreens()
    {
        return associatedScreens;
    }

    public void setAssociatedScreens(Long[] associatedScreens)
    {
        this.associatedScreens = associatedScreens;
    }

    public Field getField()
    {
        if (getFieldId() != null)
        {
            return fieldManager.getField(getFieldId());
        }
        else
        {
            return null;
        }
    }
}