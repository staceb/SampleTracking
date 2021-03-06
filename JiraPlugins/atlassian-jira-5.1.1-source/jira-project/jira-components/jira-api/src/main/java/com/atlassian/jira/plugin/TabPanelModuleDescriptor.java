package com.atlassian.jira.plugin;

import com.atlassian.jira.plugin.browsepanel.TabPanel;

/**
 * Copyright 2007 Atlassian Software.
 * All rights reserved.
 */
public interface TabPanelModuleDescriptor<T extends TabPanel> extends JiraResourcedModuleDescriptor<T>, OrderableModuleDescriptor, Comparable
{
    public String getLabel();

    public String getLabelKey();
}
