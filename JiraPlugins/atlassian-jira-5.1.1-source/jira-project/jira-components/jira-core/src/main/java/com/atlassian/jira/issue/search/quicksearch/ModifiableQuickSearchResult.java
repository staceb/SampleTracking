package com.atlassian.jira.issue.search.quicksearch;

import org.apache.commons.collections.MultiHashMap;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class ModifiableQuickSearchResult implements QuickSearchResult
{
    private final Map searchParams = new MultiHashMap();
    private String searchInput;

    public ModifiableQuickSearchResult(String searchInput)
    {
        this.searchInput = searchInput;
    }

    public String getQueryString()
    {
        StringBuilder queryString = new StringBuilder();
        for (Iterator iterator = searchParams.entrySet().iterator(); iterator.hasNext();)
        {
            Map.Entry entry = (Map.Entry) iterator.next();
            String key = (String) entry.getKey();
            Collection values = (Collection) entry.getValue();
            for (Iterator iterator1 = values.iterator(); iterator1.hasNext();)
            {
                String value = (String) iterator1.next();
                queryString.append("&" + key + "=" + value);

            }
        }
        return queryString.toString();
    }

    public void addSearchParameter(String key, String value)
    {
        searchParams.put(key, value);
    }

    public Collection getSearchParameters(String key)
    {
        return (Collection) searchParams.get(key);
    }

    public String getSearchInput()
    {
        return searchInput;
    }

    public void setSearchInput(String searchInput)
    {
        this.searchInput = searchInput;
    }
}
