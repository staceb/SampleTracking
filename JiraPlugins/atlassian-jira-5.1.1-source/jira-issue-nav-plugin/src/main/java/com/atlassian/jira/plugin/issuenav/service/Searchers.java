package com.atlassian.jira.plugin.issuenav.service;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
* Collection of searcher groups
* @since v5.1
*/
@XmlRootElement
public class Searchers
{
    @XmlElement
    private List<FilteredSearcherGroup> groups;

    public Searchers()
    {
        this.groups = new ArrayList<FilteredSearcherGroup>();
    }

    public void addGroup(FilteredSearcherGroup group)
    {
        groups.add(group);
    }

    public List<FilteredSearcherGroup> getGroups()
    {
        return this.groups;
    }
}
